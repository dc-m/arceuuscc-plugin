package com.arceuuscc.plugin.service;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

import java.util.HashSet;
import java.util.Set;

/**
 * Service for managing the read/seen state of events and newsletters.
 * Persists state using RuneLite's ConfigManager.
 */
@Slf4j
public class ReadStateService
{
	private static final String CONFIG_GROUP = "arceuuscc";
	private static final String SEEN_EVENTS_KEY = "seenEventIds";
	private static final String LAST_SEEN_NEWSLETTER_KEY = "lastSeenNewsletterId";

	private final ConfigManager configManager;
	private final Set<String> seenEventIds = new HashSet<>();
	private int lastSeenNewsletterId = -1;
	private int lastKnownNewsletterId = -1;

	public ReadStateService(ConfigManager configManager)
	{
		this.configManager = configManager;
	}

	/**
	 * Load persisted seen state from config.
	 */
	public void load()
	{
		loadSeenEventIds();
		loadLastSeenNewsletterId();
	}

	private void loadSeenEventIds()
	{
		String seenEventsStr = configManager.getConfiguration(CONFIG_GROUP, SEEN_EVENTS_KEY);
		if (seenEventsStr != null && !seenEventsStr.isEmpty())
		{
			seenEventIds.clear();
			String[] ids = seenEventsStr.split(",");
			for (String id : ids)
			{
				if (!id.isEmpty())
				{
					seenEventIds.add(id);
				}
			}
			log.debug("Loaded {} seen event IDs from config", seenEventIds.size());
		}
	}

	private void loadLastSeenNewsletterId()
	{
		String lastNewsletterStr = configManager.getConfiguration(CONFIG_GROUP, LAST_SEEN_NEWSLETTER_KEY);
		if (lastNewsletterStr != null && !lastNewsletterStr.isEmpty())
		{
			try
			{
				lastSeenNewsletterId = Integer.parseInt(lastNewsletterStr);
				log.debug("Loaded last seen newsletter ID: {}", lastSeenNewsletterId);
			}
			catch (NumberFormatException e)
			{
				log.warn("Invalid last seen newsletter ID in config: {}", lastNewsletterStr);
			}
		}
	}

	// ==================== EVENT METHODS ====================

	/**
	 * Mark an event as seen.
	 */
	public void markEventSeen(String eventId)
	{
		seenEventIds.add(eventId);
		saveSeenEventIds();
	}

	/**
	 * Mark multiple events as seen.
	 */
	public void markEventsSeen(Iterable<String> eventIds)
	{
		for (String id : eventIds)
		{
			seenEventIds.add(id);
		}
		saveSeenEventIds();
	}

	/**
	 * Check if an event has been seen.
	 */
	public boolean isEventSeen(String eventId)
	{
		return seenEventIds.contains(eventId);
	}

	private void saveSeenEventIds()
	{
		String seenEventsStr = String.join(",", seenEventIds);
		configManager.setConfiguration(CONFIG_GROUP, SEEN_EVENTS_KEY, seenEventsStr);
		log.debug("Saved {} seen event IDs to config", seenEventIds.size());
	}

	// ==================== NEWSLETTER METHODS ====================

	/**
	 * Mark a newsletter as seen.
	 */
	public void markNewsletterSeen(int newsletterId)
	{
		if (newsletterId > lastSeenNewsletterId)
		{
			lastSeenNewsletterId = newsletterId;
			saveLastSeenNewsletterId();
		}
	}

	/**
	 * Check if a newsletter has been seen.
	 */
	public boolean isNewsletterSeen(int newsletterId)
	{
		return newsletterId <= lastSeenNewsletterId;
	}

	/**
	 * Check if a newsletter is unread.
	 */
	public boolean isNewsletterUnread(int newsletterId)
	{
		return newsletterId > lastSeenNewsletterId;
	}

	/**
	 * Get the last seen newsletter ID.
	 */
	public int getLastSeenNewsletterId()
	{
		return lastSeenNewsletterId;
	}

	private void saveLastSeenNewsletterId()
	{
		configManager.setConfiguration(CONFIG_GROUP, LAST_SEEN_NEWSLETTER_KEY, String.valueOf(lastSeenNewsletterId));
		log.debug("Saved last seen newsletter ID: {}", lastSeenNewsletterId);
	}

	// ==================== POLLING TRACKING ====================

	/**
	 * Get the last known newsletter ID (for polling detection).
	 */
	public int getLastKnownNewsletterId()
	{
		return lastKnownNewsletterId;
	}

	/**
	 * Update the last known newsletter ID.
	 */
	public void updateLastKnownNewsletterId(int newsletterId)
	{
		if (newsletterId > lastKnownNewsletterId)
		{
			lastKnownNewsletterId = newsletterId;
		}
	}

	/**
	 * Check if a newsletter is new since the last poll.
	 */
	public boolean isNewSinceLastPoll(int newsletterId)
	{
		return newsletterId > lastKnownNewsletterId;
	}
}
