package com.arceuuscc.plugin.service;

import com.arceuuscc.plugin.ArceuusCCConfig;
import com.arceuuscc.plugin.models.Event;
import com.arceuuscc.plugin.models.Newsletter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.Notifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for handling plugin notifications.
 */
@Slf4j
public class NotificationService
{
	private static final String NOTIFICATION_PREFIX = "Arceuus CC - ";

	private final Notifier notifier;
	private final ArceuusCCConfig config;
	private final ReadStateService readStateService;

	private final Map<String, String> lastEventStatus = new HashMap<>();
	private boolean initialEventsLoaded = false;
	private boolean initialNewsletterLoaded = false;

	public NotificationService(Notifier notifier, ArceuusCCConfig config, ReadStateService readStateService)
	{
		this.notifier = notifier;
		this.config = config;
		this.readStateService = readStateService;
	}

	// ==================== EVENT NOTIFICATIONS ====================

	/**
	 * Process new events and send appropriate notifications.
	 */
	public void processEvents(List<Event> newEvents, List<Event> existingEvents)
	{
		if (!config.showNotifications())
		{
			updateEventStatusCache(newEvents);
			initialEventsLoaded = true;
			return;
		}

		// Notify for truly new events (not seen before in this session) - after initial load
		if (initialEventsLoaded && config.notifyNewEvent())
		{
			notifyNewEvents(newEvents, existingEvents);
		}

		// Notify for status changes
		notifyStatusChanges(newEvents);

		updateEventStatusCache(newEvents);
		initialEventsLoaded = true;
	}

	private void notifyNewEvents(List<Event> newEvents, List<Event> existingEvents)
	{
		for (Event newEvent : newEvents)
		{
			// Check if event is new (not in previous poll AND not seen before)
			boolean isNewInPoll = existingEvents.stream()
				.noneMatch(e -> e.getEventId().equals(newEvent.getEventId()));
			boolean isUnseen = !readStateService.isEventSeen(newEvent.getEventId());

			if (isNewInPoll && isUnseen && "UPCOMING".equals(newEvent.getStatus()))
			{
				notifier.notify(NOTIFICATION_PREFIX + "New Event: " + newEvent.getTitle());
			}
		}
	}

	private void notifyStatusChanges(List<Event> newEvents)
	{
		for (Event newEvent : newEvents)
		{
			String oldStatus = lastEventStatus.get(newEvent.getEventId());
			String newStatus = newEvent.getStatus();

			if (oldStatus != null && !oldStatus.equals(newStatus))
			{
				notifyEventStatusChange(newEvent, newStatus);
			}
		}
	}

	private void notifyEventStatusChange(Event event, String newStatus)
	{
		switch (newStatus)
		{
			case "ACTIVE":
				if (config.notifyEventStarting())
				{
					notifier.notify(NOTIFICATION_PREFIX + "Event Starting: " + event.getTitle());
				}
				break;
			case "COMPLETED":
				if (config.notifyEventEnding())
				{
					notifier.notify(NOTIFICATION_PREFIX + "Event Ended: " + event.getTitle());
				}
				break;
			case "CANCELLED":
				if (config.notifyEventCancelled())
				{
					notifier.notify(NOTIFICATION_PREFIX + "Event Cancelled: " + event.getTitle());
				}
				break;
			default:
				break;
		}
	}

	private void updateEventStatusCache(List<Event> events)
	{
		for (Event event : events)
		{
			lastEventStatus.put(event.getEventId(), event.getStatus());
		}
	}

	// ==================== NEWSLETTER NOTIFICATIONS ====================

	/**
	 * Process a new newsletter and send notification if appropriate.
	 */
	public void processNewsletter(Newsletter newsletter)
	{
		if (newsletter == null)
		{
			return;
		}

		boolean isNewSinceLastPoll = readStateService.isNewSinceLastPoll(newsletter.getId());

		if (initialNewsletterLoaded && isNewSinceLastPoll && config.showNotifications() && config.notifyNewNewsletter())
		{
			log.debug("New newsletter detected: {} (id={}), notifying user", newsletter.getTitle(), newsletter.getId());
			notifier.notify(NOTIFICATION_PREFIX + "New Newsletter: " + newsletter.getTitle());
		}

		readStateService.updateLastKnownNewsletterId(newsletter.getId());
		initialNewsletterLoaded = true;
	}

	/**
	 * Mark newsletter subsystem as initialized (for list-based polling).
	 */
	public void markNewsletterInitialized()
	{
		initialNewsletterLoaded = true;
	}

	/**
	 * Check if initial newsletter load has happened.
	 */
	public boolean isNewsletterInitialized()
	{
		return initialNewsletterLoaded;
	}

	// ==================== LOGIN NOTIFICATIONS ====================

	/**
	 * Send notifications for unseen content on login.
	 */
	public void sendLoginNotifications(List<Event> events, Newsletter latestNewsletter)
	{
		if (!config.showNotifications() || !config.notifyUnreadOnLogin())
		{
			return;
		}

		// Notify for unseen events
		if (config.notifyNewEvent())
		{
			notifyUnseenEvents(events);
		}

		// Notify for unread newsletter
		if (config.notifyNewNewsletter())
		{
			notifyUnreadNewsletter(latestNewsletter);
		}
	}

	private void notifyUnseenEvents(List<Event> events)
	{
		int unseenCount = 0;
		String firstUnseenTitle = null;

		for (Event event : events)
		{
			if ("UPCOMING".equals(event.getStatus()) && !readStateService.isEventSeen(event.getEventId()))
			{
				if (firstUnseenTitle == null)
				{
					firstUnseenTitle = event.getTitle();
				}
				unseenCount++;
			}
		}

		if (unseenCount == 1)
		{
			notifier.notify(NOTIFICATION_PREFIX + "New Event: " + firstUnseenTitle);
		}
		else if (unseenCount > 1)
		{
			notifier.notify(NOTIFICATION_PREFIX + unseenCount + " new events available!");
		}
	}

	private void notifyUnreadNewsletter(Newsletter latestNewsletter)
	{
		if (latestNewsletter != null && readStateService.isNewsletterUnread(latestNewsletter.getId()))
		{
			notifier.notify(NOTIFICATION_PREFIX + "New Newsletter: " + latestNewsletter.getTitle());
		}
	}
}
