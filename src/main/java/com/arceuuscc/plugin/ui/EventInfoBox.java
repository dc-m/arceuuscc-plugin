package com.arceuuscc.plugin.ui;

import com.arceuuscc.plugin.ArceuusCCPlugin;
import com.arceuuscc.plugin.models.Event;
import com.arceuuscc.plugin.util.DateTimeUtils;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.util.ColorUtil;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class EventInfoBox extends InfoBox
{
	private static final Color LIVE_GREEN = new Color(50, 200, 50);
	private static final Color ENDING_SOON_RED = new Color(255, 100, 100);
	private static final Color STARTING_SOON_YELLOW = new Color(255, 220, 0);
	private static final Color UPCOMING_BLUE = new Color(114, 137, 218);

	private final Event event;

	public EventInfoBox(BufferedImage image, ArceuusCCPlugin plugin, Event event)
	{
		super(image, plugin);
		this.event = event;
	}

	public Event getEvent()
	{
		return event;
	}

	@Override
	public String getText()
	{
		String title = event.getTitle();
		return title.length() > 4 ? title.substring(0, 4) : title;
	}

	@Override
	public Color getTextColor()
	{
		boolean isActive = "ACTIVE".equals(event.getStatus());
		if (isActive)
		{
			if (event.getCodeword() != null && !event.getCodeword().isEmpty())
			{
				return Color.BLACK;
			}
			return Color.WHITE;
		}
		return Color.WHITE;
	}

	@Override
	public String getTooltip()
	{
		LocalDateTime now = LocalDateTime.now();
		StringBuilder sb = new StringBuilder();

		boolean isActive = "ACTIVE".equals(event.getStatus());

		sb.append(ColorUtil.wrapWithColorTag(event.getTitle(), Color.WHITE));

		if (isActive)
		{
			LocalDateTime endTime = DateTimeUtils.parseDateTime(event.getStartTime())
				.plusMinutes(event.getDurationMinutes());
			long secondsLeft = ChronoUnit.SECONDS.between(now, endTime);
			Color color = secondsLeft <= 1800 ? ENDING_SOON_RED : LIVE_GREEN;
			sb.append("</br>").append(ColorUtil.wrapWithColorTag("Ends in: " + formatCountdown(secondsLeft), color));
		}
		else
		{
			LocalDateTime startTime = DateTimeUtils.parseDateTime(event.getStartTime());
			long secondsUntil = ChronoUnit.SECONDS.between(now, startTime);
			Color color = secondsUntil <= 1800 ? STARTING_SOON_YELLOW : UPCOMING_BLUE;
			sb.append("</br>").append(ColorUtil.wrapWithColorTag("Starts in: " + formatCountdown(secondsUntil), color));
		}

		int signups = event.getSignups() != null ? event.getSignups().size() : 0;
		sb.append("</br>").append(ColorUtil.wrapWithColorTag("Signups: " + signups, Color.GRAY));

		if (isActive && event.getCodeword() != null && !event.getCodeword().isEmpty())
		{
			sb.append("</br>").append(ColorUtil.wrapWithColorTag("Codeword: " + event.getCodeword(), STARTING_SOON_YELLOW));
		}

		return sb.toString();
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
}
