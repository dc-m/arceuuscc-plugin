package com.arceuuscc.plugin.util;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class DateTimeUtils
{
	public static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	private static final DateTimeFormatter UTC_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);
	private static final DateTimeFormatter UTC_MINUTE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm 'UTC'");

	public static String currentUtcDate()
	{
		return UTC_DATE_FORMATTER.format(ZonedDateTime.now(ZoneOffset.UTC)).toUpperCase(Locale.ENGLISH);
	}

	public static String currentUtcTime()
	{
		return UTC_MINUTE_FORMATTER.format(ZonedDateTime.now(ZoneOffset.UTC));
	}

	public static String currentUtcTimestamp()
	{
		return currentUtcDate() + " " + currentUtcTime();
	}

	public static LocalDateTime parseDateTime(String isoTime)
	{
		if (isoTime == null)
		{
			return LocalDateTime.MIN;
		}
		try
		{
			return LocalDateTime.parse(isoTime, ISO_FORMATTER);
		}
		catch (DateTimeParseException e)
		{
			return LocalDateTime.MIN;
		}
	}
}
