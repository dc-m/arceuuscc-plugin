package com.arceuuscc.plugin;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads API configuration from properties file.
 */
@Slf4j
public final class ApiConfig
{
	private static final String CONFIG_FILE = "/api-config.properties";
	private static final String DEFAULT_API_URL = "https://pixelperfectdigital.co.uk/arceuus/events.php";

	private static String apiUrl;
	private static boolean loaded = false;

	private ApiConfig()
	{
		// Utility class
	}

	private static synchronized void load()
	{
		if (loaded)
		{
			return;
		}

		Properties props = new Properties();
		try (InputStream is = ApiConfig.class.getResourceAsStream(CONFIG_FILE))
		{
			if (is != null)
			{
				props.load(is);
				apiUrl = props.getProperty("api.url", DEFAULT_API_URL);
			}
			else
			{
				log.warn("api-config.properties not found, using defaults");
				apiUrl = DEFAULT_API_URL;
			}
		}
		catch (IOException e)
		{
			log.error("Failed to load api-config.properties", e);
			apiUrl = DEFAULT_API_URL;
		}

		loaded = true;
	}

	/**
	 * Get the API URL.
	 */
	public static String getApiUrl()
	{
		load();
		return apiUrl;
	}
}
