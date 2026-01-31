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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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

	public ArceuusCCOverlay(ArceuusCCPlugin plugin, ArceuusCCConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;

		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showOverlay())
		{
			return null;
		}

		if (!plugin.hasPluginAccess())
		{
			return renderNoAccessMessage(graphics);
		}

		List<Event> events = plugin.getEvents();
		LocalDateTime now = LocalDateTime.now();

		// Collect filtered events
		List<Event> activeEvents = new ArrayList<>();
		List<Event> upcomingEvents = new ArrayList<>();

		if (events != null)
		{
			for (Event event : events)
			{
				String eventId = event.getEventId();

				if ("ACTIVE".equals(event.getStatus()))
				{
					// Active events only show if user is signed up and not hidden
					if (plugin.isSignedUp(eventId) && !plugin.isOverlayHidden(eventId))
					{
						activeEvents.add(event);
					}
				}
				else if ("UPCOMING".equals(event.getStatus()))
				{
					LocalDateTime startTime = parseDateTime(event.getStartTime());
					long minutesUntil = ChronoUnit.MINUTES.between(now, startTime);
					if (minutesUntil <= 180 && minutesUntil >= 0
						&& !plugin.isNotInterested(eventId)
						&& !plugin.isOverlayHidden(eventId))
					{
						upcomingEvents.add(event);
					}
				}
			}
		}

		boolean showActive = !activeEvents.isEmpty() && config.showActiveEvent();

		// For DETAILED/MINIMAL, use only the first upcoming event
		Event upcomingEvent = upcomingEvents.isEmpty() ? null : upcomingEvents.get(0);

		boolean isStartingSoon = false;
		boolean showUpcoming = upcomingEvent != null && config.showUpcoming();
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

		boolean showNewsletter = config.showNewsletterOverlay()
			&& plugin.getPluginSettings().isShowNewsletterNotifications()
			&& plugin.hasUnreadNewsletter();

		if (!showActive && !showUpcoming && !showNewsletter)
		{
			return null;
		}

		ArceuusCCConfig.OverlayMode mode = config.overlayMode();

		if (mode == ArceuusCCConfig.OverlayMode.ICON_ONLY)
		{
			// InfoBoxManager handles ICON_ONLY mode rendering
			return null;
		}

		// Panel-based modes (DETAILED / MINIMAL)
		panelComponent.getChildren().clear();
		panelComponent.setPreferredSize(new Dimension(130, 0));

		if (mode == ArceuusCCConfig.OverlayMode.MINIMAL)
		{
			if (showActive)
			{
				for (Event active : activeEvents)
				{
					boolean isEndingSoon = isEventEndingSoon(active, now);
					renderActiveEventMinimal(active, now, isEndingSoon);
				}
			}
			if (showUpcoming)
			{
				renderUpcomingEventMinimal(upcomingEvent, now, isStartingSoon);
			}
		}
		else
		{
			panelComponent.getChildren().add(TitleComponent.builder()
				.text("ARCEUUS")
				.color(ARCEUUS_PURPLE)
				.build());
			if (showActive)
			{
				for (int i = 0; i < activeEvents.size(); i++)
				{
					if (i > 0)
					{
						panelComponent.getChildren().add(LineComponent.builder().build());
					}
					Event active = activeEvents.get(i);
					boolean isEndingSoon = isEventEndingSoon(active, now);
					renderActiveEvent(active, now, isEndingSoon);
				}
			}

			if (showUpcoming)
			{
				if (showActive)
				{
					panelComponent.getChildren().add(LineComponent.builder().build());
				}
				renderUpcomingEvent(upcomingEvent, now, isStartingSoon);
			}
		}

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

	// ==================== PANEL MODE RENDERERS ====================

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

		if (event.getCodeword() != null && !event.getCodeword().isEmpty())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Codeword:")
				.leftColor(Color.GRAY)
				.right(event.getCodeword())
				.rightColor(STARTING_SOON_YELLOW)
				.build());
		}
	}

	private void renderActiveEventMinimal(Event event, LocalDateTime now, boolean isEndingSoon)
	{
		LocalDateTime endTime = parseDateTime(event.getStartTime())
			.plusMinutes(event.getDurationMinutes());
		long secondsLeft = ChronoUnit.SECONDS.between(now, endTime);

		Color labelColor = isEndingSoon ? ENDING_SOON_RED : LIVE_GREEN;
		boolean hasCodeword = event.getCodeword() != null && !event.getCodeword().isEmpty();

		panelComponent.getChildren().add(TitleComponent.builder()
			.text(event.getTitle())
			.color(Color.WHITE)
			.build());

		if (hasCodeword)
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Ends In")
				.leftColor(labelColor)
				.right("Codeword")
				.rightColor(labelColor)
				.build());

			panelComponent.getChildren().add(LineComponent.builder()
				.left(formatCountdown(secondsLeft))
				.leftColor(STARTING_SOON_YELLOW)
				.right(event.getCodeword())
				.rightColor(STARTING_SOON_YELLOW)
				.build());
		}
		else
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Ends In:")
				.leftColor(labelColor)
				.right(formatCountdown(secondsLeft))
				.rightColor(STARTING_SOON_YELLOW)
				.build());
		}
	}

	private void renderUpcomingEventMinimal(Event event, LocalDateTime now, boolean isStartingSoon)
	{
		LocalDateTime startTime = parseDateTime(event.getStartTime());
		long secondsUntil = ChronoUnit.SECONDS.between(now, startTime);

		Color labelColor = isStartingSoon ? STARTING_SOON_YELLOW : UPCOMING_BLUE;

		panelComponent.getChildren().add(TitleComponent.builder()
			.text(event.getTitle())
			.color(Color.WHITE)
			.build());

		if (secondsUntil > 0)
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Starts In:")
				.leftColor(labelColor)
				.right(formatCountdown(secondsUntil))
				.rightColor(STARTING_SOON_YELLOW)
				.build());
		}
		else
		{
			panelComponent.getChildren().add(TitleComponent.builder()
				.text("Starting now!")
				.color(STARTING_SOON_YELLOW)
				.build());
		}
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

	// ==================== UTILITY METHODS ====================

	private boolean isEventEndingSoon(Event event, LocalDateTime now)
	{
		if (event.getDurationMinutes() <= 0)
		{
			return false;
		}
		LocalDateTime endTime = parseDateTime(event.getStartTime())
			.plusMinutes(event.getDurationMinutes());
		long minutesLeft = ChronoUnit.MINUTES.between(now, endTime);
		return minutesLeft <= 30 && minutesLeft >= 0 && config.showEndingSoon();
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
		panelComponent.setPreferredSize(new Dimension(180, 0));

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
