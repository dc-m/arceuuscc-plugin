package com.arceuuscc.plugin;

import com.arceuuscc.plugin.http.HttpEventClient;
import com.arceuuscc.plugin.models.Event;
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

	@Getter
	private HttpEventClient httpClient;

	private ArceuusCCPanel panel;
	private ArceuusCCOverlay overlay;
	private NavigationButton navButton;

	@Getter
	private List<Event> events = new ArrayList<>();

	private final Map<String, String> lastEventStatus = new HashMap<>();

	@Getter
	private boolean inClan = false;

	@Getter
	private String playerName = null;

	@Provides
	ArceuusCCConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ArceuusCCConfig.class);
	}

	@Override
	protected void startUp()
	{
		log.info("Arceuus CC plugin started");

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
			SwingUtilities.invokeLater(() ->
			{
				panel.updatePlayerInfo();
				panel.updateEvents();
			});
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

		if (!inClan)
		{
			checkClanMembership();
			if (inClan)
			{
				SwingUtilities.invokeLater(() -> panel.updatePlayerInfo());
			}
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
		if (config.showNotifications() && config.notifyNewEvent() && !events.isEmpty())
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
}
