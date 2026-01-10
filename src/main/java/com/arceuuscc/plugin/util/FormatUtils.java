package com.arceuuscc.plugin.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

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
		if (mins == 0)
		{
			return hours + "h";
		}
		return hours + "h " + mins + "m";
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
