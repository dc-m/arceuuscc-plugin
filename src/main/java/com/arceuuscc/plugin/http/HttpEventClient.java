package com.arceuuscc.plugin.http;

import com.arceuuscc.plugin.ArceuusCCPlugin;
import com.arceuuscc.plugin.models.Event;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HttpEventClient {
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final String API_KEY = "arceuus-cc-runelite-2026";

    private final String apiUrl;
    private final ArceuusCCPlugin plugin;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private boolean connected = false;
    private boolean running = false;

    public HttpEventClient(String apiUrl, ArceuusCCPlugin plugin, OkHttpClient httpClient, Gson gson) {
        this.apiUrl = apiUrl;
        this.plugin = plugin;
        this.httpClient = httpClient;
        this.gson = gson;
    }

    public void start() {
        if (running) return;
        running = true;

        // Initial fetch
        requestEvents();

        // Poll every 30 seconds
        scheduler.scheduleAtFixedRate(this::requestEvents, 30, 30, TimeUnit.SECONDS);
        log.info("HTTP Event Client started, polling {}", apiUrl);
    }

    public void stop() {
        running = false;
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("HTTP Event Client stopped");
    }

    public boolean isConnected() {
        return connected;
    }

    public void requestEvents() {
        Request request = new Request.Builder()
                .url(apiUrl)
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.error("Failed to fetch events", e);
                if (connected) {
                    connected = false;
                    plugin.onConnectionStatusChanged(false);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) {
                        log.error("Error response: {}", response.code());
                        if (connected) {
                            connected = false;
                            plugin.onConnectionStatusChanged(false);
                        }
                        return;
                    }

                    String json = body.string();
                    JsonObject jsonObj = new JsonParser().parse(json).getAsJsonObject();
                    JsonArray eventsArray = jsonObj.getAsJsonArray("events");
                    List<Event> events = gson.fromJson(eventsArray, new TypeToken<List<Event>>(){}.getType());

                    if (!connected) {
                        connected = true;
                        plugin.onConnectionStatusChanged(true);
                    }

                    plugin.onEventsReceived(events);
                    log.debug("Fetched {} events", events.size());
                } catch (Exception e) {
                    log.error("Error parsing events response", e);
                }
            }
        });
    }

    public void sendSignup(String eventId, String osrsName) {
        JsonObject payload = new JsonObject();
        payload.addProperty("eventId", eventId);
        payload.addProperty("osrsName", osrsName);

        Request request = new Request.Builder()
                .url(apiUrl + "?action=signup")
                .header("X-API-Key", API_KEY)
                .post(RequestBody.create(JSON_MEDIA_TYPE, gson.toJson(payload)))
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.error("Failed to sign up for event {}", eventId, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (response.isSuccessful()) {
                        log.info("Successfully signed up for event {}", eventId);
                        // Refresh events to get updated signups
                        requestEvents();
                    } else {
                        String errorBody = body != null ? body.string() : "Unknown error";
                        log.error("Signup failed: {}", errorBody);
                    }
                }
            }
        });
    }

    public void sendUnsignup(String eventId, String osrsName) {
        JsonObject payload = new JsonObject();
        payload.addProperty("eventId", eventId);
        payload.addProperty("osrsName", osrsName);

        Request request = new Request.Builder()
                .url(apiUrl + "?action=unsignup")
                .header("X-API-Key", API_KEY)
                .post(RequestBody.create(JSON_MEDIA_TYPE, gson.toJson(payload)))
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.error("Failed to unsign from event {}", eventId, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (response.isSuccessful()) {
                        log.info("Successfully unsigned from event {}", eventId);
                        // Refresh events to get updated signups
                        requestEvents();
                    } else {
                        String errorBody = body != null ? body.string() : "Unknown error";
                        log.error("Unsignup failed: {}", errorBody);
                    }
                }
            }
        });
    }
}
