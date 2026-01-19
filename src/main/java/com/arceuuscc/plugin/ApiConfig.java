package com.arceuuscc.plugin;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads API configuration injected at build time.
 * Use gradle build -Penv=test or gradle build -Penv=prod to switch environments.
 */
@Slf4j
public final class ApiConfig
{
	private static final String CONFIG_FILE = "/api-config.properties";

	// Default fallback URLs (prod)
	private static final String DEFAULT_API_URL = "https://pixelperfectdigital.co.uk/arceuus/events.php";
	private static final String DEFAULT_ENVIRONMENT = "prod";

	private static String apiUrl;
	private static String environment;
	private static boolean loaded = false;

	private ApiConfig()
	{
		// Utility class
	}

	/**
	 * Load configuration from properties file.
	 */
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
				environment = props.getProperty("api.environment", DEFAULT_ENVIRONMENT);

				// Check if properties were properly substituted by Gradle
				if (apiUrl.contains("${"))
				{
					log.warn("API URL not substituted by Gradle, using default");
					apiUrl = DEFAULT_API_URL;
					environment = DEFAULT_ENVIRONMENT;
				}

				log.debug("Loaded API config: environment={}", environment);
			}
			else
			{
				log.warn("api-config.properties not found, using defaults");
				apiUrl = DEFAULT_API_URL;
				environment = DEFAULT_ENVIRONMENT;
			}
		}
		catch (IOException e)
		{
			log.error("Failed to load api-config.properties", e);
			apiUrl = DEFAULT_API_URL;
			environment = DEFAULT_ENVIRONMENT;
		}

		loaded = true;
	}

	/**
	 * Get the API URL for the current build environment.
	 */
	public static String getApiUrl()
	{
		load();
		return apiUrl;
	}

	/**
	 * Get the current environment name (test/prod).
	 */
	public static String getEnvironment()
	{
		load();
		return environment;
	}

	/**
	 * Check if running in test environment.
	 */
	public static boolean isTestEnvironment()
	{
		load();
		return "test".equalsIgnoreCase(environment);
	}
}
