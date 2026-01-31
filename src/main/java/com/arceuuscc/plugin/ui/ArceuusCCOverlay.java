package com.arceuuscc.plugin.ui;

import com.arceuuscc.plugin.ArceuusCCConfig;
import com.arceuuscc.plugin.ArceuusCCPlugin;
import com.arceuuscc.plugin.models.Event;
import com.arceuuscc.plugin.util.DateTimeUtils;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
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

	private static final int ICON_SIZE = 32;
	private static final int BOX_PAD = 1;
	private static final int BOX_SIZE = ICON_SIZE + BOX_PAD * 2;
	private static final int ICON_GAP = 2;
	private static final Color BOX_BG = new Color(45, 45, 45);
	private static final Color BOX_BORDER = new Color(90, 90, 90);

	private final ArceuusCCPlugin plugin;
	private final ArceuusCCConfig config;
	private final PanelComponent panelComponent = new PanelComponent();
	private BufferedImage arceuusIcon;

	@Inject
	private Client client;

	@Inject
	private TooltipManager tooltipManager;

	// Icon mode hit-testing
	private final List<Rectangle> iconBounds = new ArrayList<>();
	private final List<Event> iconEvents = new ArrayList<>();

	@Inject
	public ArceuusCCOverlay(ArceuusCCPlugin plugin, ArceuusCCConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;

		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);

		try
		{
			arceuusIcon = ImageUtil.loadImageResource(getClass(), "/com/arceuuscc/plugin/icon.png");
		}
		catch (Exception e)
		{
			arceuusIcon = null;
		}
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
			return renderIconMode(graphics, activeEvents, upcomingEvents, now, showNewsletter);
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

	// ==================== ICON MODE ====================

	private Dimension renderIconMode(Graphics2D graphics, List<Event> activeEvents,
		List<Event> upcomingEvents, LocalDateTime now, boolean showNewsletter)
	{
		iconBounds.clear();
		iconEvents.clear();

		List<Event> allEvents = new ArrayList<>();
		if (config.showActiveEvent())
		{
			allEvents.addAll(activeEvents);
		}
		if (config.showUpcoming())
		{
			allEvents.addAll(upcomingEvents);
		}

		if (allEvents.isEmpty() && !showNewsletter)
		{
			return null;
		}

		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		int x = 0;
		int maxHeight = 0;

		for (Event event : allEvents)
		{
			int h = renderIconBox(graphics, event, now, x);
			Rectangle bounds = new Rectangle(x, 0, BOX_SIZE, h);
			iconBounds.add(bounds);
			iconEvents.add(event);
			if (h > maxHeight)
			{
				maxHeight = h;
			}
			x += BOX_SIZE + ICON_GAP;
		}

		// Newsletter icon
		if (showNewsletter)
		{
			int h = renderNewsletterIconBox(graphics, x);
			if (h > maxHeight)
			{
				maxHeight = h;
			}
			x += BOX_SIZE + ICON_GAP;
		}

		handleIconTooltip(graphics, now);

		int totalWidth = x > 0 ? x - ICON_GAP : 0;
		return new Dimension(totalWidth, maxHeight);
	}

	private int renderIconBox(Graphics2D graphics, Event event, LocalDateTime now, int x)
	{
		boolean isActive = "ACTIVE".equals(event.getStatus());
		boolean hasCodeword = isActive && event.getCodeword() != null && !event.getCodeword().isEmpty();
		Color statusColor = getIconColor(event, now);

		// Gray background box (like RuneLite InfoBox)
		graphics.setColor(BOX_BG);
		graphics.fillRect(x, 0, BOX_SIZE, BOX_SIZE);
		graphics.setColor(BOX_BORDER);
		graphics.drawRect(x, 0, BOX_SIZE - 1, BOX_SIZE - 1);

		// Colored status bar at top
		graphics.setColor(statusColor);
		graphics.fillRect(x + 1, 1, BOX_SIZE - 2, 2);

		// Draw icon centered inside box
		if (arceuusIcon != null)
		{
			graphics.drawImage(arceuusIcon, x + BOX_PAD, BOX_PAD, ICON_SIZE, ICON_SIZE, null);
		}

		// Abbreviated event name overlaid on bottom-right of icon (like item quantities)
		String abbrev = event.getTitle().length() > 4
			? event.getTitle().substring(0, 4)
			: event.getTitle();
		Font overlayFont = new Font("Arial", Font.BOLD, 10);
		graphics.setFont(overlayFont);
		FontMetrics fm = graphics.getFontMetrics();
		int textW = fm.stringWidth(abbrev);
		int textX = x + BOX_SIZE - textW - 3;
		int textY = BOX_SIZE - 4;

		// Text shadow
		graphics.setColor(Color.BLACK);
		graphics.drawString(abbrev, textX + 1, textY + 1);
		// Text
		graphics.setColor(Color.WHITE);
		graphics.drawString(abbrev, textX, textY);

		int y = BOX_SIZE;

		// Codeword below box
		if (hasCodeword)
		{
			String cw = event.getCodeword();
			Font cwFont = new Font("Arial", Font.BOLD, 9);
			graphics.setFont(cwFont);
			fm = graphics.getFontMetrics();
			int cwX = x + (BOX_SIZE - fm.stringWidth(cw)) / 2;
			graphics.setColor(Color.BLACK);
			graphics.drawString(cw, cwX, y + fm.getAscent());
			y += fm.getHeight() + 1;
		}

		return y;
	}

	private int renderNewsletterIconBox(Graphics2D graphics, int x)
	{
		// Gray background box
		graphics.setColor(BOX_BG);
		graphics.fillRect(x, 0, BOX_SIZE, BOX_SIZE);
		graphics.setColor(BOX_BORDER);
		graphics.drawRect(x, 0, BOX_SIZE - 1, BOX_SIZE - 1);

		// Gold status bar
		graphics.setColor(NEWSLETTER_GOLD);
		graphics.fillRect(x + 1, 1, BOX_SIZE - 2, 2);

		// Draw icon
		if (arceuusIcon != null)
		{
			graphics.drawImage(arceuusIcon, x + BOX_PAD, BOX_PAD, ICON_SIZE, ICON_SIZE, null);
		}

		// "NEW!" overlaid on bottom-right
		Font overlayFont = new Font("Arial", Font.BOLD, 10);
		graphics.setFont(overlayFont);
		FontMetrics fm = graphics.getFontMetrics();
		String label = "NEW!";
		int textX = x + BOX_SIZE - fm.stringWidth(label) - 3;
		int textY = BOX_SIZE - 4;
		graphics.setColor(Color.BLACK);
		graphics.drawString(label, textX + 1, textY + 1);
		graphics.setColor(NEWSLETTER_GOLD);
		graphics.drawString(label, textX, textY);

		return BOX_SIZE;
	}

	private void handleIconTooltip(Graphics2D graphics, LocalDateTime now)
	{
		if (client == null)
		{
			return;
		}

		net.runelite.api.Point mouse = client.getMouseCanvasPosition();
		if (mouse == null)
		{
			return;
		}

		// Use the graphics transform to get the overlay's actual screen position
		int overlayX = (int) graphics.getTransform().getTranslateX();
		int overlayY = (int) graphics.getTransform().getTranslateY();

		int relX = mouse.getX() - overlayX;
		int relY = mouse.getY() - overlayY;

		for (int i = 0; i < iconBounds.size(); i++)
		{
			if (iconBounds.get(i).contains(relX, relY))
			{
				Event event = iconEvents.get(i);
				String tooltip = buildEventTooltip(event, now);
				tooltipManager.add(new Tooltip(tooltip));
				return;
			}
		}
	}

	private String buildEventTooltip(Event event, LocalDateTime now)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(event.getTitle());

		boolean isActive = "ACTIVE".equals(event.getStatus());

		if (isActive)
		{
			LocalDateTime endTime = parseDateTime(event.getStartTime())
				.plusMinutes(event.getDurationMinutes());
			long secondsLeft = ChronoUnit.SECONDS.between(now, endTime);
			sb.append("</br>Ends in: ").append(formatCountdown(secondsLeft));
		}
		else
		{
			LocalDateTime startTime = parseDateTime(event.getStartTime());
			long secondsUntil = ChronoUnit.SECONDS.between(now, startTime);
			sb.append("</br>Starts in: ").append(formatCountdown(secondsUntil));
		}

		int signups = event.getSignups() != null ? event.getSignups().size() : 0;
		sb.append("</br>Signups: ").append(signups);

		if (isActive && event.getCodeword() != null && !event.getCodeword().isEmpty())
		{
			sb.append("</br>Codeword: ").append(event.getCodeword());
		}

		return sb.toString();
	}

	private Color getIconColor(Event event, LocalDateTime now)
	{
		if ("ACTIVE".equals(event.getStatus()))
		{
			return isEventEndingSoon(event, now) ? ENDING_SOON_RED : LIVE_GREEN;
		}
		LocalDateTime startTime = parseDateTime(event.getStartTime());
		long minutesUntil = ChronoUnit.MINUTES.between(now, startTime);
		return (minutesUntil <= 30 && minutesUntil >= 0) ? STARTING_SOON_YELLOW : UPCOMING_BLUE;
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
