package com.arceuuscc.plugin;

import com.arceuuscc.plugin.http.HttpEventClient;
import com.arceuuscc.plugin.models.Event;
import com.arceuuscc.plugin.models.Newsletter;
import com.arceuuscc.plugin.models.PluginSettings;
import com.arceuuscc.plugin.ui.ArceuusCCOverlay;
import com.arceuuscc.plugin.ui.ArceuusCCPanel;
import com.google.gson.Gson;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanID;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import okhttp3.OkHttpClient;

import javax.inject.Inject;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
@PluginDescriptor(
	name = "Arceuus CC",
	description = "Clan event management for Arceuus CC - view events, sign up, and coordinate with your clan. This plugin submits your username to a 3rd party server not controlled or verified by the RuneLite Developers.",
	tags = {"clan", "events", "arceuus", "cc", "signup"}
)
public class ArceuusCCPlugin extends Plugin
{
	private static final String API_URL = "https://pixelperfectdigital.co.uk/arceuus/events.php";

	@Inject
	private Client client;

	@Inject
	private ArceuusCCConfig config;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private Notifier notifier;

	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private Gson gson;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private ConfigManager configManager;

	private static final String CONFIG_GROUP = "arceuuscc";
	private static final String SEEN_EVENTS_KEY = "seenEventIds";
	private static final String LAST_SEEN_NEWSLETTER_KEY = "lastSeenNewsletterId";

	@Getter
	private HttpEventClient httpClient;

	private ArceuusCCPanel panel;
	private ArceuusCCOverlay overlay;
	private NavigationButton navButton;

	@Getter
	private List<Event> events = new ArrayList<>();

	@Getter
	private List<Newsletter> newsletters = new ArrayList<>();

	@Getter
	private Newsletter latestNewsletter = null;

	private int lastSeenNewsletterId = -1;
	private int lastKnownNewsletterId = -1; // For detecting new newsletters during polling

	private final Map<String, String> lastEventStatus = new HashMap<>();

	// Track which events the user has seen (by event ID)
	private final java.util.Set<String> seenEventIds = new java.util.HashSet<>();
	private boolean initialEventsLoaded = false;
	private boolean loginNotificationSent = false;

	@Getter
	private boolean inClan = false;

	@Getter
	private String playerName = null;

	@Getter
	private PluginSettings pluginSettings = PluginSettings.builder().build();

	@Provides
	ArceuusCCConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ArceuusCCConfig.class);
	}

	@Override
	protected void startUp()
	{
		log.info("Arceuus CC plugin started");

		// Load persisted seen state
		loadSeenState();

		try
		{
			panel = new ArceuusCCPanel(this);

			overlay = new ArceuusCCOverlay(this, config);
			overlayManager.add(overlay);

			BufferedImage icon = null;
			try
			{
				icon = ImageUtil.loadImageResource(getClass(), "icon.png");
			}
			catch (Exception iconEx)
			{
				log.debug("Could not load icon, using fallback");
			}

			navButton = NavigationButton.builder()
				.tooltip("Arceuus CC Events")
				.icon(icon != null ? icon : new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB))
				.priority(5)
				.panel(panel)
				.build();

			clientToolbar.addNavigation(navButton);

			httpClient = new HttpEventClient(API_URL, this, okHttpClient, gson, executor);
			httpClient.start();

			// Fetch newsletters on startup
			httpClient.requestLatestNewsletter();
			httpClient.requestNewsletters(10);
		}
		catch (Exception e)
		{
			log.error("Error during startup", e);
		}
	}

	@Override
	protected void shutDown()
	{
		log.info("Arceuus CC plugin stopped");

		clientToolbar.removeNavigation(navButton);
		overlayManager.remove(overlay);

		if (httpClient != null)
		{
			httpClient.stop();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			playerName = client.getLocalPlayer() != null ? client.getLocalPlayer().getName() : null;
			log.debug("Player logged in: {}", playerName);
			checkClanMembership();

			// Send login notifications for unseen events/newsletters
			if (!loginNotificationSent && config.showNotifications() && config.notifyUnreadOnLogin())
			{
				loginNotificationSent = true;
				sendLoginNotifications();
			}

			SwingUtilities.invokeLater(() ->
			{
				panel.updatePlayerInfo();
				panel.updateEvents();
			});
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			playerName = null;
			inClan = false;
			loginNotificationSent = false; // Reset so we notify again on next login
			SwingUtilities.invokeLater(() ->
			{
				panel.updatePlayerInfo();
				panel.updateEvents();
			});
		}
	}

	private void sendLoginNotifications()
	{
		// Notify for unseen events
		if (config.notifyNewEvent())
		{
			int unseenCount = 0;
			String firstUnseenTitle = null;
			for (Event event : events)
			{
				if ("UPCOMING".equals(event.getStatus()) && !seenEventIds.contains(event.getEventId()))
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
				notifier.notify("Arceuus CC - New Event: " + firstUnseenTitle);
			}
			else if (unseenCount > 1)
			{
				notifier.notify("Arceuus CC - " + unseenCount + " new events available!");
			}
		}

		// Notify for unread newsletter
		if (config.notifyNewNewsletter() && latestNewsletter != null)
		{
			boolean isUnread = latestNewsletter.getId() > lastSeenNewsletterId;
			if (isUnread)
			{
				notifier.notify("Arceuus CC - New Newsletter: " + latestNewsletter.getTitle());
			}
		}
	}

	@Subscribe
	public void onClanChannelChanged(ClanChannelChanged event)
	{
		checkClanMembership();
		SwingUtilities.invokeLater(() ->
		{
			panel.updatePlayerInfo();
			panel.updateEvents();
		});
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (playerName == null && client.getLocalPlayer() != null)
		{
			playerName = client.getLocalPlayer().getName();
			log.info("Player name detected: {}", playerName);
			SwingUtilities.invokeLater(() -> panel.updatePlayerInfo());
		}

		// Always check clan membership status
		boolean wasInClan = inClan;
		checkClanMembership();

		// If clan status changed, update the UI
		if (inClan != wasInClan)
		{
			SwingUtilities.invokeLater(() -> {
				panel.updatePlayerInfo();
				panel.updateEvents();
				panel.updateNewsletters();
			});
		}
	}

	private void checkClanMembership()
	{
		ClanChannel yourClan = client.getClanChannel(ClanID.CLAN);

		if (yourClan != null)
		{
			String clanName = yourClan.getName();
			boolean wasInClan = inClan;

			if ("Arceuus".equalsIgnoreCase(clanName))
			{
				inClan = true;
				if (!wasInClan)
				{
					log.info("Joined Arceuus clan");
				}
			}
			else
			{
				inClan = false;
				if (wasInClan)
				{
					log.info("Left Arceuus clan (now in: {})", clanName);
				}
			}
		}
		else
		{
			if (inClan)
			{
				log.info("Left clan");
			}
			inClan = false;
		}
	}

	public void onEventsReceived(List<Event> newEvents)
	{
		// Notify for truly new events (not seen before in this session) - after initial load
		if (initialEventsLoaded && config.showNotifications() && config.notifyNewEvent())
		{
			for (Event newEvent : newEvents)
			{
				boolean isNew = events.stream()
					.noneMatch(e -> e.getEventId().equals(newEvent.getEventId()));
				if (isNew && "UPCOMING".equals(newEvent.getStatus()))
				{
					notifier.notify("Arceuus CC - New Event: " + newEvent.getTitle());
				}
			}
		}
		initialEventsLoaded = true;

		if (config.showNotifications())
		{
			for (Event newEvent : newEvents)
			{
				String oldStatus = lastEventStatus.get(newEvent.getEventId());
				String newStatus = newEvent.getStatus();

				if (oldStatus != null && !oldStatus.equals(newStatus))
				{
					switch (newStatus)
					{
						case "ACTIVE":
							if (config.notifyEventStarting())
							{
								notifier.notify("Arceuus CC - Event Starting: " + newEvent.getTitle());
							}
							break;
						case "COMPLETED":
							if (config.notifyEventEnding())
							{
								notifier.notify("Arceuus CC - Event Ended: " + newEvent.getTitle());
							}
							break;
						case "CANCELLED":
							if (config.notifyEventCancelled())
							{
								notifier.notify("Arceuus CC - Event Cancelled: " + newEvent.getTitle());
							}
							break;
						default:
							break;
					}
				}
			}
		}

		for (Event event : newEvents)
		{
			lastEventStatus.put(event.getEventId(), event.getStatus());
		}

		this.events = newEvents;
		SwingUtilities.invokeLater(() -> panel.updateEvents());
	}

	public void onConnectionStatusChanged(boolean connected)
	{
		SwingUtilities.invokeLater(() -> panel.updateConnectionStatus(connected));
	}

	public void signUp(String eventId)
	{
		log.info("signUp called for eventId: {}, playerName: {}, inClan: {}", eventId, playerName, inClan);

		if (playerName == null)
		{
			log.warn("Cannot sign up - player name not known");
			return;
		}
		if (!inClan)
		{
			log.warn("Cannot sign up - not in Arceuus clan chat");
			return;
		}
		if (httpClient != null && httpClient.isConnected())
		{
			log.info("Sending signup request via HTTP");
			httpClient.sendSignup(eventId, playerName);
		}
		else
		{
			log.warn("HTTP client not connected, cannot sign up");
		}
	}

	public void unSignUp(String eventId)
	{
		log.info("unSignUp called for eventId: {}, playerName: {}", eventId, playerName);

		if (playerName == null)
		{
			return;
		}
		if (httpClient != null && httpClient.isConnected())
		{
			log.info("Sending unsignup request via HTTP");
			httpClient.sendUnsignup(eventId, playerName);
		}
		else
		{
			log.warn("HTTP client not connected, cannot unsign up");
		}
	}

	public void refreshEvents()
	{
		if (httpClient != null)
		{
			httpClient.requestEvents();
		}
	}

	public boolean isSignedUp(String eventId)
	{
		if (playerName == null)
		{
			return false;
		}
		for (Event event : events)
		{
			if (event.getEventId().equals(eventId))
			{
				return event.getSignups() != null &&
					event.getSignups().stream()
						.anyMatch(s -> s.getOsrsName().equalsIgnoreCase(playerName));
			}
		}
		return false;
	}

	// ==================== NEWSLETTER METHODS ====================

	private boolean initialNewsletterLoaded = false;

	public void onNewsletterReceived(Newsletter newsletter)
	{
		if (newsletter == null)
		{
			return;
		}

		// Check if this is a new newsletter compared to what we last knew about
		boolean isNewSinceLastPoll = newsletter.getId() > lastKnownNewsletterId;

		// After initial load, notify for newly published newsletters during polling
		if (initialNewsletterLoaded && isNewSinceLastPoll && config.showNotifications() && config.notifyNewNewsletter())
		{
			log.info("New newsletter detected: {} (id={}), notifying user", newsletter.getTitle(), newsletter.getId());
			notifier.notify("Arceuus CC - New Newsletter: " + newsletter.getTitle());
		}

		// Update tracking
		if (newsletter.getId() > lastKnownNewsletterId)
		{
			lastKnownNewsletterId = newsletter.getId();
		}
		initialNewsletterLoaded = true;

		this.latestNewsletter = newsletter;
		SwingUtilities.invokeLater(() -> panel.updateNewsletters());
	}

	public void onNewslettersReceived(List<Newsletter> newNewsletters)
	{
		this.newsletters = newNewsletters;
		if (!newNewsletters.isEmpty())
		{
			Newsletter latest = newNewsletters.get(0);

			// Check if this is a new newsletter compared to what we last knew about
			boolean isNewSinceLastPoll = latest.getId() > lastKnownNewsletterId;

			// After initial load, notify for newly published newsletters during polling
			if (initialNewsletterLoaded && isNewSinceLastPoll && config.showNotifications() && config.notifyNewNewsletter())
			{
				log.info("New newsletter detected from list: {} (id={}), notifying user", latest.getTitle(), latest.getId());
				notifier.notify("Arceuus CC - New Newsletter: " + latest.getTitle());
			}

			// Update tracking
			if (latest.getId() > lastKnownNewsletterId)
			{
				lastKnownNewsletterId = latest.getId();
			}

			this.latestNewsletter = latest;
		}
		initialNewsletterLoaded = true;
		SwingUtilities.invokeLater(() -> panel.updateNewsletters());
	}

	public void refreshNewsletters()
	{
		if (httpClient != null)
		{
			httpClient.requestNewsletters(10);
		}
	}

	public void markNewsletterAsSeen(Newsletter newsletter)
	{
		if (newsletter != null && newsletter.getId() > lastSeenNewsletterId)
		{
			lastSeenNewsletterId = newsletter.getId();
			saveLastSeenNewsletterId();
		}
	}

	public void markAllNewslettersAsRead()
	{
		if (latestNewsletter != null && latestNewsletter.getId() > lastSeenNewsletterId)
		{
			lastSeenNewsletterId = latestNewsletter.getId();
			saveLastSeenNewsletterId();
		}
		SwingUtilities.invokeLater(() -> panel.updateNewsletters());
	}

	public boolean hasUnreadNewsletter()
	{
		if (latestNewsletter == null)
		{
			return false;
		}
		return latestNewsletter.getId() > lastSeenNewsletterId;
	}

	public String getNewsletterImageUrl(int newsletterId)
	{
		if (httpClient != null)
		{
			return httpClient.getNewsletterImageUrl(newsletterId);
		}
		return null;
	}

	// ==================== EVENT READ STATUS METHODS ====================

	public void markEventAsSeen(String eventId)
	{
		seenEventIds.add(eventId);
		saveSeenEventIds();
		SwingUtilities.invokeLater(() -> panel.updateEvents());
	}

	public void markAllEventsAsRead()
	{
		for (Event event : events)
		{
			seenEventIds.add(event.getEventId());
		}
		saveSeenEventIds();
		SwingUtilities.invokeLater(() -> panel.updateEvents());
	}

	public boolean isEventSeen(String eventId)
	{
		return seenEventIds.contains(eventId);
	}

	public boolean hasUnseenEvents()
	{
		for (Event event : events)
		{
			if ("UPCOMING".equals(event.getStatus()) && !seenEventIds.contains(event.getEventId()))
			{
				return true;
			}
		}
		return false;
	}

	// ==================== SETTINGS METHODS ====================

	public void onSettingsReceived(PluginSettings settings)
	{
		this.pluginSettings = settings;
		log.info("Plugin settings updated: polling={}s, requireClan={}, clanName={}",
			settings.getEventPollingInterval(),
			settings.isRequireClanMembership(),
			settings.getClanName());

		// Update UI to reflect any settings changes
		SwingUtilities.invokeLater(() -> {
			panel.updatePlayerInfo();
			panel.updateEvents();
		});
	}

	/**
	 * Check if the user should have access to plugin features.
	 * Returns true if clan membership is not required OR user is in the correct clan.
	 */
	public boolean hasPluginAccess()
	{
		if (!pluginSettings.isRequireClanMembership())
		{
			return true;
		}
		return inClan;
	}

	/**
	 * Get the message to display when user doesn't have plugin access.
	 */
	public String getNoAccessMessage()
	{
		return "To use the Arceuus CC Plugin please join the \"" +
			pluginSettings.getClanName() +
			"\" Clan Chat. You can also join us on Discord: https://discord.gg/Ka3bVn6nkW";
	}

	// ==================== PERSISTENCE METHODS ====================

	private void loadSeenState()
	{
		// Load seen event IDs
		String seenEventsStr = configManager.getConfiguration(CONFIG_GROUP, SEEN_EVENTS_KEY);
		if (seenEventsStr != null && !seenEventsStr.isEmpty())
		{
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

		// Load last seen newsletter ID
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

	private void saveSeenEventIds()
	{
		String seenEventsStr = String.join(",", seenEventIds);
		configManager.setConfiguration(CONFIG_GROUP, SEEN_EVENTS_KEY, seenEventsStr);
		log.debug("Saved {} seen event IDs to config", seenEventIds.size());
	}

	private void saveLastSeenNewsletterId()
	{
		configManager.setConfiguration(CONFIG_GROUP, LAST_SEEN_NEWSLETTER_KEY, String.valueOf(lastSeenNewsletterId));
		log.debug("Saved last seen newsletter ID: {}", lastSeenNewsletterId);
	}
}
