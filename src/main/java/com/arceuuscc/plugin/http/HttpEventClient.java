package com.arceuuscc.plugin.http;

import com.arceuuscc.plugin.ArceuusCCPlugin;
import com.arceuuscc.plugin.models.Event;
import com.arceuuscc.plugin.models.Newsletter;
import com.arceuuscc.plugin.models.PluginSettings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HttpEventClient
{
	private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
	private static final String API_KEY = "arceuus-cc-runelite-2026";
	private static final int DEFAULT_POLLING_INTERVAL = 30;

	private final String apiUrl;
	private final ArceuusCCPlugin plugin;
	private final OkHttpClient httpClient;
	private final Gson gson;
	private final ScheduledExecutorService executor;

	private ScheduledFuture<?> pollingTask;
	private boolean connected = false;
	private int pollingInterval = DEFAULT_POLLING_INTERVAL;

	public HttpEventClient(String apiUrl, ArceuusCCPlugin plugin, OkHttpClient httpClient, Gson gson, ScheduledExecutorService executor)
	{
		this.apiUrl = apiUrl;
		this.plugin = plugin;
		this.httpClient = httpClient;
		this.gson = gson;
		this.executor = executor;
	}

	public void start()
	{
		if (pollingTask != null)
		{
			return;
		}

		// Fetch settings first, then start polling
		requestSettings();

		// Initial fetch
		requestEvents();

		// Poll at configured interval (default 30 seconds, 0 = disabled)
		startPolling();
		log.info("HTTP Event Client started, polling {}", apiUrl);
	}

	private void startPolling()
	{
		// Cancel existing polling task if any
		if (pollingTask != null)
		{
			pollingTask.cancel(false);
			pollingTask = null;
		}

		// If polling interval is 0, polling is disabled
		if (pollingInterval <= 0)
		{
			log.info("Event polling disabled (interval = 0)");
			return;
		}

		pollingTask = executor.scheduleAtFixedRate(() -> {
			requestEvents();
			requestLatestNewsletter();
			requestNewsletters(10);
		}, pollingInterval, pollingInterval, TimeUnit.SECONDS);
		log.info("Event and newsletter polling started with interval: {} seconds", pollingInterval);
	}

	public void updatePollingInterval(int newInterval)
	{
		if (newInterval != pollingInterval)
		{
			pollingInterval = newInterval;
			startPolling();
		}
	}

	public void stop()
	{
		if (pollingTask != null)
		{
			pollingTask.cancel(false);
			pollingTask = null;
		}
		log.info("HTTP Event Client stopped");
	}

	public boolean isConnected()
	{
		return connected;
	}

	public void requestEvents()
	{
		Request request = new Request.Builder()
			.url(apiUrl)
			.get()
			.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.error("Failed to fetch events", e);
				if (connected)
				{
					connected = false;
					plugin.onConnectionStatusChanged(false);
				}
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (ResponseBody body = response.body())
				{
					if (!response.isSuccessful() || body == null)
					{
						log.error("Error response: {}", response.code());
						if (connected)
						{
							connected = false;
							plugin.onConnectionStatusChanged(false);
						}
						return;
					}

					String json = body.string();
					JsonObject jsonObj = new JsonParser().parse(json).getAsJsonObject();
					JsonArray eventsArray = jsonObj.getAsJsonArray("events");
					List<Event> events = gson.fromJson(eventsArray, new TypeToken<List<Event>>(){}.getType());

					if (!connected)
					{
						connected = true;
						plugin.onConnectionStatusChanged(true);
					}

					plugin.onEventsReceived(events);
					log.debug("Fetched {} events", events.size());
				}
				catch (Exception e)
				{
					log.error("Error parsing events response", e);
				}
			}
		});
	}

	public void sendSignup(String eventId, String osrsName)
	{
		JsonObject payload = new JsonObject();
		payload.addProperty("eventId", eventId);
		payload.addProperty("osrsName", osrsName);

		Request request = new Request.Builder()
			.url(apiUrl + "?action=signup")
			.header("X-API-Key", API_KEY)
			.post(RequestBody.create(JSON_MEDIA_TYPE, gson.toJson(payload)))
			.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.error("Failed to sign up for event {}", eventId, e);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (ResponseBody body = response.body())
				{
					if (response.isSuccessful())
					{
						log.info("Successfully signed up for event {}", eventId);
						// Refresh events to get updated signups
						requestEvents();
					}
					else
					{
						String errorBody = body != null ? body.string() : "Unknown error";
						log.error("Signup failed: {}", errorBody);
					}
				}
			}
		});
	}

	public void sendUnsignup(String eventId, String osrsName)
	{
		JsonObject payload = new JsonObject();
		payload.addProperty("eventId", eventId);
		payload.addProperty("osrsName", osrsName);

		Request request = new Request.Builder()
			.url(apiUrl + "?action=unsignup")
			.header("X-API-Key", API_KEY)
			.post(RequestBody.create(JSON_MEDIA_TYPE, gson.toJson(payload)))
			.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.error("Failed to unsign from event {}", eventId, e);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (ResponseBody body = response.body())
				{
					if (response.isSuccessful())
					{
						log.info("Successfully unsigned from event {}", eventId);
						// Refresh events to get updated signups
						requestEvents();
					}
					else
					{
						String errorBody = body != null ? body.string() : "Unknown error";
						log.error("Unsignup failed: {}", errorBody);
					}
				}
			}
		});
	}

	// ==================== NEWSLETTER METHODS ====================

	/**
	 * Request the latest newsletter from the API.
	 */
	public void requestLatestNewsletter()
	{
		String newsletterUrl = apiUrl.replace("events.php", "newsletters.php") + "?action=latest";

		Request request = new Request.Builder()
			.url(newsletterUrl)
			.get()
			.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.error("Failed to fetch latest newsletter", e);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (ResponseBody body = response.body())
				{
					if (!response.isSuccessful() || body == null)
					{
						log.error("Error fetching newsletter: {}", response.code());
						return;
					}

					String json = body.string();
					JsonObject jsonObj = new JsonParser().parse(json).getAsJsonObject();

					if (jsonObj.has("newsletter") && !jsonObj.get("newsletter").isJsonNull())
					{
						Newsletter newsletter = gson.fromJson(jsonObj.get("newsletter"), Newsletter.class);
						plugin.onNewsletterReceived(newsletter);
						log.debug("Fetched newsletter: {}", newsletter.getTitle());
					}
					else
					{
						// No newsletter exists - clear the latest
						plugin.onNewsletterReceived(null);
						log.debug("No newsletter available");
					}
				}
				catch (Exception e)
				{
					log.error("Error parsing newsletter response", e);
				}
			}
		});
	}

	/**
	 * Request list of recent newsletters.
	 */
	public void requestNewsletters(int limit)
	{
		String newsletterUrl = apiUrl.replace("events.php", "newsletters.php") + "?limit=" + limit;

		Request request = new Request.Builder()
			.url(newsletterUrl)
			.get()
			.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.error("Failed to fetch newsletters", e);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (ResponseBody body = response.body())
				{
					if (!response.isSuccessful() || body == null)
					{
						log.error("Error fetching newsletters: {}", response.code());
						return;
					}

					String json = body.string();
					JsonObject jsonObj = new JsonParser().parse(json).getAsJsonObject();
					JsonArray newslettersArray = jsonObj.getAsJsonArray("newsletters");
					List<Newsletter> newsletters = gson.fromJson(newslettersArray, new TypeToken<List<Newsletter>>(){}.getType());

					plugin.onNewslettersReceived(newsletters);
					log.debug("Fetched {} newsletters", newsletters.size());
				}
				catch (Exception e)
				{
					log.error("Error parsing newsletters response", e);
				}
			}
		});
	}

	/**
	 * Get newsletter image URL.
	 */
	public String getNewsletterImageUrl(int newsletterId)
	{
		return apiUrl.replace("events.php", "newsletters.php") + "?id=" + newsletterId + "&image=1";
	}

	// ==================== SETTINGS METHODS ====================

	/**
	 * Request plugin settings from the API.
	 */
	public void requestSettings()
	{
		String settingsUrl = apiUrl.replace("events.php", "settings.php");

		Request request = new Request.Builder()
			.url(settingsUrl)
			.get()
			.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.error("Failed to fetch plugin settings", e);
				// Use defaults if settings fetch fails
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (ResponseBody body = response.body())
				{
					if (!response.isSuccessful() || body == null)
					{
						log.error("Error fetching settings: {}", response.code());
						return;
					}

					String json = body.string();
					JsonObject jsonObj = new JsonParser().parse(json).getAsJsonObject();

					if (jsonObj.has("settings") && !jsonObj.get("settings").isJsonNull())
					{
						JsonObject settingsObj = jsonObj.getAsJsonObject("settings");

						PluginSettings settings = PluginSettings.builder()
							.eventPollingInterval(settingsObj.has("event_polling_interval") ?
								settingsObj.get("event_polling_interval").getAsInt() : 30)
							.requireClanMembership(settingsObj.has("require_clan_membership") ?
								settingsObj.get("require_clan_membership").getAsBoolean() : true)
							.clanName(settingsObj.has("clan_name") ?
								settingsObj.get("clan_name").getAsString() : "Arceuus")
							.showNewsletterNotifications(settingsObj.has("show_newsletter_notifications") ?
								settingsObj.get("show_newsletter_notifications").getAsBoolean() : true)
							.build();

						// Update polling interval
						int newInterval = settings.getEventPollingInterval();
						if (newInterval != pollingInterval)
						{
							pollingInterval = newInterval;
							// Don't restart polling here - it will be started in start()
						}

						plugin.onSettingsReceived(settings);
						log.info("Loaded plugin settings: polling={}s, requireClan={}, clanName={}, showNewsletterNotifications={}",
							settings.getEventPollingInterval(),
							settings.isRequireClanMembership(),
							settings.getClanName(),
							settings.isShowNewsletterNotifications());
					}
				}
				catch (Exception e)
				{
					log.error("Error parsing settings response", e);
				}
			}
		});
	}
}
