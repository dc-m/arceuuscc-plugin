package com.arceuuscc.plugin;

import com.arceuuscc.plugin.http.HttpEventClient;
import com.arceuuscc.plugin.models.AuthorizationState;
import com.arceuuscc.plugin.models.Event;
import com.arceuuscc.plugin.models.Newsletter;
import com.arceuuscc.plugin.models.PluginSettings;
import com.arceuuscc.plugin.ui.ArceuusCCOverlay;
import com.arceuuscc.plugin.ui.ArceuusCCPanel;
import com.arceuuscc.plugin.ui.EventInfoBox;
import com.arceuuscc.plugin.ui.NewsletterInfoBox;
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
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ImageUtil;
import okhttp3.OkHttpClient;

import javax.inject.Inject;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
@PluginDescriptor(
	name = "Arceuus CC",
	description = "Clan event management for Arceuus CC - view events, sign up, and coordinate with your clan. This plugin submits your username to a 3rd party server not controlled or verified by the RuneLite Developers.",
	tags = {"clan", "events", "arceuus", "cc", "signup"}
)
public class ArceuusCCPlugin extends Plugin
{

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

	@Inject
	private InfoBoxManager infoBoxManager;

	private static final String CONFIG_GROUP = "arceuuscc";
	private static final String SEEN_EVENTS_KEY = "seenEventIds";
	private static final String LAST_SEEN_NEWSLETTER_KEY = "lastSeenNewsletterId";
	private static final String AUTH_TOKEN_KEY = "authToken";
	private static final String AUTH_STATUS_KEY = "authStatus";
	private static final String HIDDEN_OVERLAY_EVENTS_KEY = "hiddenOverlayEventIds";
	private static final String NOT_INTERESTED_EVENTS_KEY = "notInterestedEventIds";

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
	// Track which events the user has hidden from the overlay
	private final java.util.Set<String> hiddenOverlayEventIds = new java.util.HashSet<>();
	// Track which upcoming events the user marked "Not Interested"
	private final java.util.Set<String> notInterestedEventIds = new java.util.HashSet<>();
	// InfoBox tracking for ICON_ONLY mode
	private final List<EventInfoBox> activeInfoBoxes = new ArrayList<>();
	private NewsletterInfoBox newsletterInfoBox = null;
	private BufferedImage infoBoxIcon = null;
	private boolean initialEventsLoaded = false;
	private boolean loginNotificationSent = false;
	// Counter for consecutive "not found" auth responses before deleting the token
	private int authNotFoundCounter = 0;
	private static final int AUTH_NOT_FOUND_THRESHOLD = 3;

	@Getter
	private boolean inClan = false;

	// Counter for consecutive null clan channel readings - prevents false negatives during loading/teleports
	private int clanNullCounter = 0;
	private static final int CLAN_NULL_THRESHOLD = 5; // Require 5 consecutive null readings (~3 seconds)

	@Getter
	private String playerName = null;

	@Getter
	private PluginSettings pluginSettings = PluginSettings.builder().build();

	@Getter
	private String authToken = null;

	@Getter
	private AuthorizationState authState = AuthorizationState.UNKNOWN;

	private String authReason = null;

	@Provides
	ArceuusCCConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ArceuusCCConfig.class);
	}

	@Override
	protected void startUp()
	{
		log.debug("Arceuus CC plugin started");

		loadSeenState();
		loadAuthToken();

		try
		{
			panel = new ArceuusCCPanel(this);

			overlay = new ArceuusCCOverlay(this, config);
			overlayManager.add(overlay);

			try
			{
				infoBoxIcon = ImageUtil.loadImageResource(getClass(), "icon.png");
			}
			catch (Exception e)
			{
				log.debug("Could not load infobox icon");
			}

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

			httpClient = new HttpEventClient(ApiConfig.getApiUrl(), this, okHttpClient, gson, executor);
			log.debug("Using API: {}", ApiConfig.getApiUrl());
			httpClient.start();

			httpClient.requestLatestNewsletter();
			httpClient.requestNewsletters(10);

			if (client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null)
			{
				playerName = client.getLocalPlayer().getName();
				checkClanMembership();

				// Load the player-specific auth token (now that we have playerName)
				loadAuthToken();

				if (authToken != null)
				{
					checkAuthorizationStatus();
				}

				SwingUtilities.invokeLater(() -> panel.updatePlayerInfo());
			}
		}
		catch (Exception e)
		{
			log.error("Error during startup", e);
		}
	}

	@Override
	protected void shutDown()
	{
		log.debug("Arceuus CC plugin stopped");

		clientToolbar.removeNavigation(navButton);
		overlayManager.remove(overlay);
		clearInfoBoxes();

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

			// Load the player-specific auth token
			loadAuthToken();

			if (authToken != null && httpClient != null)
			{
				checkAuthorizationStatus();
			}

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
			clanNullCounter = 0;
			loginNotificationSent = false;
			// Reset auth state - will be reloaded when player logs in
			authToken = null;
			authState = AuthorizationState.NO_TOKEN;
			SwingUtilities.invokeLater(() ->
			{
				panel.resetDisplayState();
				panel.updatePlayerInfo();
				panel.updateEvents();
				panel.updateNewsletters();
			});
		}
	}

	private void sendLoginNotifications()
	{
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
			panel.updateNewsletters();
		});
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if ((playerName == null || playerName.isEmpty()) && client.getLocalPlayer() != null)
		{
			String name = client.getLocalPlayer().getName();
			if (name != null && !name.isEmpty())
			{
				playerName = name;
				log.debug("Player name detected: {}", playerName);

				// Player name wasn't available when LOGGED_IN fired - reload auth token now
				if (authToken == null)
				{
					loadAuthToken();

					if (authToken != null && httpClient != null)
					{
						checkAuthorizationStatus();
					}
				}

				SwingUtilities.invokeLater(() ->
				{
					panel.updatePlayerInfo();
					panel.updateEvents();
				});
			}
		}

		boolean wasInClan = inClan;
		checkClanMembership();

		if (inClan != wasInClan)
		{
			SwingUtilities.invokeLater(() -> {
				panel.updatePlayerInfo();
				panel.updateEvents();
				panel.updateNewsletters();
			});
		}

		updateInfoBoxes();
	}

	private void checkClanMembership()
	{
		ClanChannel yourClan = client.getClanChannel(ClanID.CLAN);

		if (yourClan != null)
		{
			// Reset null counter when we get valid clan data
			clanNullCounter = 0;

			String clanName = yourClan.getName();
			boolean wasInClan = inClan;

			if ("Arceuus".equalsIgnoreCase(clanName))
			{
				inClan = true;
				if (!wasInClan)
				{
					log.debug("Joined Arceuus clan");
				}
			}
			else
			{
				inClan = false;
				if (wasInClan)
				{
					log.debug("Left Arceuus clan (now in: {})", clanName);
				}
			}
		}
		else
		{
			// Only set inClan to false after multiple consecutive null readings
			// This prevents false negatives during teleports/loading when clan data is briefly unavailable
			if (inClan)
			{
				clanNullCounter++;
				if (clanNullCounter >= CLAN_NULL_THRESHOLD)
				{
					log.debug("Left clan (after {} consecutive null readings)", clanNullCounter);
					inClan = false;
					clanNullCounter = 0;
				}
			}
		}
	}

	public void onEventsReceived(List<Event> newEvents)
	{
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
		updateInfoBoxes();
		SwingUtilities.invokeLater(() -> panel.updateEvents());
	}

	public void onConnectionStatusChanged(boolean connected)
	{
		SwingUtilities.invokeLater(() -> panel.updateConnectionStatus(connected));
	}

	public void signUp(String eventId)
	{
		log.debug("signUp called for eventId: {}, playerName: {}, inClan: {}", eventId, playerName, inClan);

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
		clearNotInterested(eventId);

		if (httpClient != null && httpClient.isConnected())
		{
			log.debug("Sending signup request via HTTP");
			httpClient.sendSignup(eventId, playerName);
		}
		else
		{
			log.warn("HTTP client not connected, cannot sign up");
		}
	}

	public void unSignUp(String eventId)
	{
		log.debug("unSignUp called for eventId: {}, playerName: {}", eventId, playerName);

		if (playerName == null)
		{
			return;
		}
		if (httpClient != null && httpClient.isConnected())
		{
			log.debug("Sending unsignup request via HTTP");
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
			this.latestNewsletter = null;
			initialNewsletterLoaded = true;
			SwingUtilities.invokeLater(() -> panel.updateNewsletters());
			return;
		}

		boolean isNewSinceLastPoll = newsletter.getId() > lastKnownNewsletterId;

		if (initialNewsletterLoaded && isNewSinceLastPoll && config.showNotifications() && config.notifyNewNewsletter())
		{
			log.debug("New newsletter detected: {} (id={}), notifying user", newsletter.getTitle(), newsletter.getId());
			notifier.notify("Arceuus CC - New Newsletter: " + newsletter.getTitle());
		}

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
		if (newNewsletters.isEmpty())
		{
			this.latestNewsletter = null;
		}
		else
		{
			Newsletter latest = newNewsletters.get(0);
			boolean isNewSinceLastPoll = latest.getId() > lastKnownNewsletterId;

			if (initialNewsletterLoaded && isNewSinceLastPoll && config.showNotifications() && config.notifyNewNewsletter())
			{
				log.debug("New newsletter detected from list: {} (id={}), notifying user", latest.getTitle(), latest.getId());
				notifier.notify("Arceuus CC - New Newsletter: " + latest.getTitle());
			}

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
			SwingUtilities.invokeLater(() -> panel.updateNewsletters());
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

	public boolean isNewsletterSeen(int newsletterId)
	{
		return newsletterId <= lastSeenNewsletterId;
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

	// ==================== OVERLAY VISIBILITY METHODS ====================

	public void toggleOverlayVisibility(String eventId)
	{
		if (hiddenOverlayEventIds.contains(eventId))
		{
			hiddenOverlayEventIds.remove(eventId);
		}
		else
		{
			hiddenOverlayEventIds.add(eventId);
		}
		saveHiddenOverlayEventIds();
		SwingUtilities.invokeLater(() -> panel.updateEvents());
	}

	public boolean isOverlayHidden(String eventId)
	{
		return hiddenOverlayEventIds.contains(eventId);
	}

	public void markNotInterested(String eventId)
	{
		notInterestedEventIds.add(eventId);
		saveNotInterestedEventIds();
		SwingUtilities.invokeLater(() -> panel.updateEvents());
	}

	public void clearNotInterested(String eventId)
	{
		notInterestedEventIds.remove(eventId);
		saveNotInterestedEventIds();
		SwingUtilities.invokeLater(() -> panel.updateEvents());
	}

	public boolean isNotInterested(String eventId)
	{
		return notInterestedEventIds.contains(eventId);
	}

	// ==================== INFOBOX METHODS ====================

	public void updateInfoBoxes()
	{
		if (config.overlayMode() != ArceuusCCConfig.OverlayMode.ICON_ONLY || !config.showOverlay())
		{
			clearInfoBoxes();
			return;
		}

		java.time.LocalDateTime now = java.time.LocalDateTime.now();

		// Collect events that should show as InfoBoxes (same filtering as overlay)
		List<Event> infoBoxEvents = new ArrayList<>();
		if (events != null)
		{
			for (Event event : events)
			{
				String eventId = event.getEventId();
				if ("ACTIVE".equals(event.getStatus()))
				{
					if (config.showActiveEvent() && isSignedUp(eventId) && !isOverlayHidden(eventId))
					{
						infoBoxEvents.add(event);
					}
				}
				else if ("UPCOMING".equals(event.getStatus()))
				{
					java.time.LocalDateTime startTime = com.arceuuscc.plugin.util.DateTimeUtils.parseDateTime(event.getStartTime());
					long minutesUntil = java.time.temporal.ChronoUnit.MINUTES.between(now, startTime);
					if (config.showUpcoming() && minutesUntil <= 180 && minutesUntil >= 0
						&& !isNotInterested(eventId) && !isOverlayHidden(eventId))
					{
						infoBoxEvents.add(event);
					}
				}
			}
		}

		// Check if the current InfoBoxes match what we need
		java.util.Set<String> currentIds = new java.util.HashSet<>();
		for (EventInfoBox box : activeInfoBoxes)
		{
			currentIds.add(box.getEvent().getEventId());
		}
		java.util.Set<String> neededIds = new java.util.HashSet<>();
		for (Event event : infoBoxEvents)
		{
			neededIds.add(event.getEventId());
		}

		if (!currentIds.equals(neededIds))
		{
			// Remove old InfoBoxes
			for (EventInfoBox box : activeInfoBoxes)
			{
				infoBoxManager.removeInfoBox(box);
			}
			activeInfoBoxes.clear();

			// Add new InfoBoxes
			for (Event event : infoBoxEvents)
			{
				EventInfoBox box = new EventInfoBox(infoBoxIcon, this, event);
				infoBoxManager.addInfoBox(box);
				activeInfoBoxes.add(box);
			}
		}

		// Newsletter InfoBox
		boolean showNewsletter = config.showNewsletterOverlay()
			&& pluginSettings != null && pluginSettings.isShowNewsletterNotifications()
			&& hasUnreadNewsletter();

		if (showNewsletter && newsletterInfoBox == null)
		{
			newsletterInfoBox = new NewsletterInfoBox(infoBoxIcon, this);
			infoBoxManager.addInfoBox(newsletterInfoBox);
		}
		else if (!showNewsletter && newsletterInfoBox != null)
		{
			infoBoxManager.removeInfoBox(newsletterInfoBox);
			newsletterInfoBox = null;
		}
	}

	private void clearInfoBoxes()
	{
		for (EventInfoBox box : activeInfoBoxes)
		{
			infoBoxManager.removeInfoBox(box);
		}
		activeInfoBoxes.clear();
		if (newsletterInfoBox != null)
		{
			infoBoxManager.removeInfoBox(newsletterInfoBox);
			newsletterInfoBox = null;
		}
	}

	// ==================== SETTINGS METHODS ====================

	public void onSettingsReceived(PluginSettings settings)
	{
		this.pluginSettings = settings;
		log.debug("Plugin settings updated: polling={}s, requireClan={}, clanName={}",
			settings.getEventPollingInterval(),
			settings.isRequireClanMembership(),
			settings.getClanName());

		SwingUtilities.invokeLater(() -> {
			panel.updatePlayerInfo();
			panel.updateEvents();
		});
	}

	/**
	 * Check if the user should have access to plugin features.
	 * Requires ACCEPTED authorization status AND (clan membership not required OR user is in the correct clan).
	 */
	public boolean hasPluginAccess()
	{
		if (!authState.hasAccess())
		{
			return false;
		}

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

		String hiddenStr = configManager.getConfiguration(CONFIG_GROUP, HIDDEN_OVERLAY_EVENTS_KEY);
		if (hiddenStr != null && !hiddenStr.isEmpty())
		{
			for (String id : hiddenStr.split(","))
			{
				if (!id.isEmpty())
				{
					hiddenOverlayEventIds.add(id);
				}
			}
		}

		String notInterestedStr = configManager.getConfiguration(CONFIG_GROUP, NOT_INTERESTED_EVENTS_KEY);
		if (notInterestedStr != null && !notInterestedStr.isEmpty())
		{
			for (String id : notInterestedStr.split(","))
			{
				if (!id.isEmpty())
				{
					notInterestedEventIds.add(id);
				}
			}
		}

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

	private void saveHiddenOverlayEventIds()
	{
		String joined = String.join(",", hiddenOverlayEventIds);
		configManager.setConfiguration(CONFIG_GROUP, HIDDEN_OVERLAY_EVENTS_KEY, joined);
	}

	private void saveNotInterestedEventIds()
	{
		String joined = String.join(",", notInterestedEventIds);
		configManager.setConfiguration(CONFIG_GROUP, NOT_INTERESTED_EVENTS_KEY, joined);
	}

	private void saveLastSeenNewsletterId()
	{
		configManager.setConfiguration(CONFIG_GROUP, LAST_SEEN_NEWSLETTER_KEY, String.valueOf(lastSeenNewsletterId));
		log.debug("Saved last seen newsletter ID: {}", lastSeenNewsletterId);
	}

	// ==================== AUTHORIZATION METHODS ====================

	/**
	 * Get the player-specific config key for auth token.
	 * Returns null if no player is logged in.
	 */
	private String getAuthTokenKey()
	{
		if (playerName == null || playerName.isEmpty())
		{
			return null;
		}
		return AUTH_TOKEN_KEY + "_" + playerName.toLowerCase();
	}

	/**
	 * Get the player-specific config key for auth status.
	 * Returns null if no player is logged in.
	 */
	private String getAuthStatusKey()
	{
		if (playerName == null || playerName.isEmpty())
		{
			return null;
		}
		return AUTH_STATUS_KEY + "_" + playerName.toLowerCase();
	}

	private void loadAuthToken()
	{
		String tokenKey = getAuthTokenKey();

		if (tokenKey == null)
		{
			// No player logged in, can't load player-specific token
			authToken = null;
			authState = AuthorizationState.NO_TOKEN;
			log.debug("No player logged in - cannot load auth token");
			return;
		}

		authToken = configManager.getConfiguration(CONFIG_GROUP, tokenKey);

		if (authToken == null || authToken.isEmpty())
		{
			authState = AuthorizationState.NO_TOKEN;
			log.debug("No auth token found for player {}", playerName);
		}
		else
		{
			// Always set to UNKNOWN when we have a token - requires API verification
			// This ensures revoked users can't see content before the check completes
			authState = AuthorizationState.UNKNOWN;
			log.debug("Loaded auth token for player {}, needs API verification", playerName);
		}
	}

	private void saveAuthToken()
	{
		String tokenKey = getAuthTokenKey();
		String statusKey = getAuthStatusKey();

		if (tokenKey == null || statusKey == null)
		{
			log.warn("Cannot save auth token - no player logged in");
			return;
		}

		if (authToken != null)
		{
			configManager.setConfiguration(CONFIG_GROUP, tokenKey, authToken);
		}
		else
		{
			configManager.unsetConfiguration(CONFIG_GROUP, tokenKey);
		}
		configManager.setConfiguration(CONFIG_GROUP, statusKey, authState.name());
		log.debug("Saved auth token and status for player {}: {}", playerName, authState);
	}

	/**
	 * Request access to the plugin. Generates a new token and submits it for approval.
	 */
	public void requestAccess()
	{
		if (playerName == null)
		{
			log.warn("Cannot request access - player name not known");
			return;
		}

		authToken = UUID.randomUUID().toString();
		authState = AuthorizationState.PENDING;
		saveAuthToken();

		log.debug("Requesting access for {}", playerName);

		if (httpClient != null)
		{
			httpClient.submitAuthorizationRequest(playerName, authToken);
		}

		SwingUtilities.invokeLater(() -> panel.updateAuthorizationState());
	}

	/**
	 * Called when authorization status is received from the API.
	 */
	public void onAuthorizationStatusReceived(AuthorizationState newState, String reason)
	{
		AuthorizationState oldState = authState;
		authReason = reason;

		if (newState == AuthorizationState.NO_TOKEN)
		{
			authNotFoundCounter++;
			log.debug("Auth not found for player {} ({}/{})", playerName, authNotFoundCounter, AUTH_NOT_FOUND_THRESHOLD);

			if (authNotFoundCounter >= AUTH_NOT_FOUND_THRESHOLD)
			{
				authState = newState;
				authToken = null;
				String tokenKey = getAuthTokenKey();
				if (tokenKey != null)
				{
					configManager.unsetConfiguration(CONFIG_GROUP, tokenKey);
				}
				log.debug("Cleared auth token for player {} - not found after {} consecutive checks", playerName, authNotFoundCounter);
				authNotFoundCounter = 0;
			}
			else
			{
				// Don't clear the token yet - could be a transient failure
				return;
			}
		}
		else
		{
			authNotFoundCounter = 0;
			authState = newState;
		}

		saveAuthToken();

		log.debug("Authorization status: {} -> {}", oldState, newState);

		if (newState == AuthorizationState.ACCEPTED && oldState != AuthorizationState.ACCEPTED)
		{
			log.debug("Authorization accepted - fetching events and newsletters immediately");
			if (httpClient != null)
			{
				httpClient.requestEvents();
				httpClient.requestLatestNewsletter();
				httpClient.requestNewsletters(10);
			}
		}

		// Only update UI if the state actually changed to prevent flashing
		if (oldState != newState)
		{
			SwingUtilities.invokeLater(() -> panel.updateAuthorizationState());
		}
	}

	public String getAuthReason()
	{
		return authReason;
	}

	public void checkAuthorizationStatus()
	{
		if (authToken != null && playerName != null && httpClient != null)
		{
			httpClient.checkAuthorizationStatus(playerName, authToken);
		}
	}
}
