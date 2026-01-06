package com.arceuuscc.plugin.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateTimeUtils
{
	public static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

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
