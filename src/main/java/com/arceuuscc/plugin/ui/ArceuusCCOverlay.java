package com.arceuuscc.plugin.ui;

import com.arceuuscc.plugin.ArceuusCCConfig;
import com.arceuuscc.plugin.ArceuusCCPlugin;
import com.arceuuscc.plugin.models.Event;
import com.arceuuscc.plugin.util.DateTimeUtils;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ArceuusCCOverlay extends Overlay
{
	private static final Color ARCEUUS_PURPLE = new Color(128, 0, 128);
	private static final Color LIVE_GREEN = new Color(50, 200, 50);
	private static final Color ENDING_SOON_RED = new Color(255, 100, 100);
	private static final Color STARTING_SOON_YELLOW = new Color(255, 220, 0);
	private static final Color UPCOMING_BLUE = new Color(114, 137, 218);
	private static final Color NEWSLETTER_GOLD = new Color(218, 165, 32);
	private static final Color NO_ACCESS_ORANGE = new Color(255, 140, 0);

	private final ArceuusCCPlugin plugin;
	private final ArceuusCCConfig config;
	private final PanelComponent panelComponent = new PanelComponent();

	@Inject
	public ArceuusCCOverlay(ArceuusCCPlugin plugin, ArceuusCCConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;

		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showOverlay())
		{
			return null;
		}

		// Check if user has plugin access (is in clan if required)
		if (!plugin.hasPluginAccess())
		{
			return renderNoAccessMessage(graphics);
		}

		List<Event> events = plugin.getEvents();
		LocalDateTime now = LocalDateTime.now();

		Event activeEvent = null;
		Event upcomingEvent = null;

		if (events != null)
		{
			for (Event event : events)
			{
				if ("ACTIVE".equals(event.getStatus()))
				{
					activeEvent = event;
				}
				else if ("UPCOMING".equals(event.getStatus()) && upcomingEvent == null)
				{
					LocalDateTime startTime = parseDateTime(event.getStartTime());
					long minutesUntil = ChronoUnit.MINUTES.between(now, startTime);
					if (minutesUntil <= 180 && minutesUntil >= 0)
					{
						upcomingEvent = event;
					}
				}
			}
		}

		boolean showActive = activeEvent != null && config.showActiveEvent();
		boolean showUpcoming = upcomingEvent != null && config.showUpcoming();

		boolean isStartingSoon = false;
		if (upcomingEvent != null)
		{
			LocalDateTime startTime = parseDateTime(upcomingEvent.getStartTime());
			long minutesUntil = ChronoUnit.MINUTES.between(now, startTime);
			isStartingSoon = minutesUntil <= 30 && minutesUntil >= 0;

			if (isStartingSoon && !config.showStartingSoon())
			{
				showUpcoming = false;
			}
		}

		boolean isEndingSoon = false;
		if (activeEvent != null)
		{
			LocalDateTime endTime = parseDateTime(activeEvent.getStartTime())
				.plusMinutes(activeEvent.getDurationMinutes());
			long minutesLeft = ChronoUnit.MINUTES.between(now, endTime);
			isEndingSoon = minutesLeft <= 30 && minutesLeft >= 0;

			if (isEndingSoon && !config.showEndingSoon())
			{
				isEndingSoon = false;
			}
		}

		// Check if we should show newsletter notification
		boolean showNewsletter = config.showNewsletterOverlay()
			&& plugin.getPluginSettings().isShowNewsletterNotifications()
			&& plugin.hasUnreadNewsletter();

		// If nothing to show, return null
		if (!showActive && !showUpcoming && !showNewsletter)
		{
			return null;
		}

		panelComponent.getChildren().clear();

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("ARCEUUS")
			.color(ARCEUUS_PURPLE)
			.build());

		if (showActive)
		{
			renderActiveEvent(activeEvent, now, isEndingSoon);
		}

		if (showUpcoming)
		{
			if (showActive)
			{
				panelComponent.getChildren().add(LineComponent.builder().build());
			}
			renderUpcomingEvent(upcomingEvent, now, isStartingSoon);
		}

		// Show new newsletter notification
		if (showNewsletter)
		{
			if (showActive || showUpcoming)
			{
				panelComponent.getChildren().add(LineComponent.builder().build());
			}
			renderNewsletterNotification();
		}

		return panelComponent.render(graphics);
	}

	private void renderNewsletterNotification()
	{
		var newsletter = plugin.getLatestNewsletter();
		if (newsletter == null)
		{
			return;
		}

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("NEW NEWSLETTER")
			.color(NEWSLETTER_GOLD)
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left(newsletter.getTitle())
			.leftColor(Color.WHITE)
			.build());

		if (newsletter.getMonthYear() != null)
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left(newsletter.getMonthYear())
				.leftColor(Color.GRAY)
				.build());
		}

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Check panel to read")
			.leftColor(NEWSLETTER_GOLD)
			.build());
	}

	private void renderActiveEvent(Event event, LocalDateTime now, boolean isEndingSoon)
	{
		LocalDateTime endTime = parseDateTime(event.getStartTime())
			.plusMinutes(event.getDurationMinutes());
		long secondsLeft = ChronoUnit.SECONDS.between(now, endTime);

		String title = isEndingSoon ? "ENDING SOON" : "LIVE EVENT";
		Color titleColor = isEndingSoon ? ENDING_SOON_RED : LIVE_GREEN;

		panelComponent.getChildren().add(TitleComponent.builder()
			.text(title)
			.color(titleColor)
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left(event.getTitle())
			.leftColor(Color.WHITE)
			.build());

		if (secondsLeft > 0)
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Ends in:")
				.leftColor(Color.GRAY)
				.right(formatCountdown(secondsLeft))
				.rightColor(isEndingSoon ? ENDING_SOON_RED : Color.ORANGE)
				.build());
		}

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Signups:")
			.leftColor(Color.GRAY)
			.right(String.valueOf(event.getSignups() != null ? event.getSignups().size() : 0))
			.rightColor(Color.WHITE)
			.build());
	}

	private void renderUpcomingEvent(Event event, LocalDateTime now, boolean isStartingSoon)
	{
		LocalDateTime startTime = parseDateTime(event.getStartTime());
		long secondsUntil = ChronoUnit.SECONDS.between(now, startTime);

		String title = isStartingSoon ? "STARTING SOON" : "NEXT EVENT";
		Color titleColor = isStartingSoon ? STARTING_SOON_YELLOW : UPCOMING_BLUE;

		panelComponent.getChildren().add(TitleComponent.builder()
			.text(title)
			.color(titleColor)
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left(event.getTitle())
			.leftColor(Color.WHITE)
			.build());

		if (secondsUntil > 0)
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Starts in:")
				.leftColor(Color.GRAY)
				.right(formatCountdown(secondsUntil))
				.rightColor(isStartingSoon ? STARTING_SOON_YELLOW : Color.WHITE)
				.build());
		}
		else
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Starting now!")
				.leftColor(STARTING_SOON_YELLOW)
				.build());
		}

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Signups:")
			.leftColor(Color.GRAY)
			.right(String.valueOf(event.getSignups() != null ? event.getSignups().size() : 0))
			.rightColor(Color.WHITE)
			.build());

		if (plugin.isSignedUp(event.getEventId()))
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("You are signed up!")
				.leftColor(LIVE_GREEN)
				.build());
		}
	}

	private LocalDateTime parseDateTime(String isoTime)
	{
		return DateTimeUtils.parseDateTime(isoTime);
	}

	private String formatCountdown(long totalSeconds)
	{
		if (totalSeconds < 0)
		{
			return "0s";
		}

		if (totalSeconds < 60)
		{
			return totalSeconds + "s";
		}

		long totalMinutes = totalSeconds / 60;

		if (totalMinutes < 60)
		{
			return totalMinutes + "m";
		}

		long hours = totalMinutes / 60;
		long minutes = totalMinutes % 60;
		if (hours < 24)
		{
			return hours + "h " + minutes + "m";
		}

		long days = hours / 24;
		hours = hours % 24;
		return days + "d " + hours + "h";
	}

	private Dimension renderNoAccessMessage(Graphics2D graphics)
	{
		panelComponent.getChildren().clear();

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("ARCEUUS CC")
			.color(ARCEUUS_PURPLE)
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Join the clan to use")
			.leftColor(NO_ACCESS_ORANGE)
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("this plugin")
			.leftColor(NO_ACCESS_ORANGE)
			.build());

		panelComponent.getChildren().add(LineComponent.builder().build());

		String clanName = plugin.getPluginSettings().getClanName();
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Clan Chat:")
			.leftColor(Color.GRAY)
			.right(clanName)
			.rightColor(Color.WHITE)
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Discord:")
			.leftColor(Color.GRAY)
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("discord.gg/Ka3bVn6nkW")
			.leftColor(UPCOMING_BLUE)
			.build());

		return panelComponent.render(graphics);
	}
}
