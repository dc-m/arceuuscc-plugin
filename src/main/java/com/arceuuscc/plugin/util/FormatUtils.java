package com.arceuuscc.plugin.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * Utility methods for formatting display text.
 */
public final class FormatUtils
{
	private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("EEE, MMM d 'at' HH:mm");
	private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	private FormatUtils() {}

	public static String formatEventTime(String isoTime)
	{
		if (isoTime == null)
		{
			return "Unknown";
		}
		try
		{
			LocalDateTime dateTime = LocalDateTime.parse(isoTime, INPUT_FORMATTER);
			return dateTime.format(DISPLAY_FORMATTER);
		}
		catch (DateTimeParseException e)
		{
			return isoTime;
		}
	}

	public static String formatDuration(int minutes)
	{
		if (minutes == 0)
		{
			return "Open-ended";
		}
		if (minutes < 60)
		{
			return minutes + " min";
		}
		int hours = minutes / 60;
		int mins = minutes % 60;
		if (hours < 24)
		{
			if (mins == 0)
			{
				return hours + "h";
			}
			return hours + "h " + mins + "m";
		}
		int days = hours / 24;
		hours = hours % 24;
		if (hours == 0)
		{
			return days + "d";
		}
		return days + "d " + hours + "h";
	}

	public static String formatCountdown(long totalSeconds)
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

	public static String formatEventCountdown(String startTimeIso, int durationMinutes, String status)
	{
		if (startTimeIso == null)
		{
			return null;
		}
		try
		{
			LocalDateTime startTime = LocalDateTime.parse(startTimeIso);
			LocalDateTime now = LocalDateTime.now();

			if ("ACTIVE".equals(status))
			{
				if (durationMinutes <= 0)
				{
					return null;
				}
				LocalDateTime endTime = startTime.plusMinutes(durationMinutes);
				long seconds = ChronoUnit.SECONDS.between(now, endTime);
				return "Ends in: " + formatCountdown(seconds);
			}
			else if ("UPCOMING".equals(status))
			{
				long seconds = ChronoUnit.SECONDS.between(now, startTime);
				return "Starts in: " + formatCountdown(seconds);
			}
		}
		catch (DateTimeParseException e)
		{
			return null;
		}
		return null;
	}

	public static String escapeHtml(String text)
	{
		if (text == null)
		{
			return "";
		}
		return text.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;")
			.replace("\n", "<br>");
	}
}
