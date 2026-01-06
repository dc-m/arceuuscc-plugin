package com.arceuuscc.plugin.ui;

import com.arceuuscc.plugin.ArceuusCCConfig;
import com.arceuuscc.plugin.ArceuusCCPlugin;
import com.arceuuscc.plugin.models.Event;
import com.arceuuscc.plugin.util.DateTimeUtils;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ArceuusCCOverlay extends Overlay {
    private static final Color ARCEUUS_PURPLE = new Color(128, 0, 128);
    private static final Color LIVE_GREEN = new Color(50, 200, 50);
    private static final Color ENDING_SOON_RED = new Color(255, 100, 100);
    private static final Color STARTING_SOON_YELLOW = new Color(255, 220, 0);
    private static final Color UPCOMING_BLUE = new Color(114, 137, 218);

    private final ArceuusCCPlugin plugin;
    private final ArceuusCCConfig config;
    private final PanelComponent panelComponent = new PanelComponent();

    @Inject
    public ArceuusCCOverlay(ArceuusCCPlugin plugin, ArceuusCCConfig config) {
        super(plugin);
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.LOW);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.showOverlay()) {
            return null;
        }

        List<Event> events = plugin.getEvents();
        if (events == null || events.isEmpty()) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();

        // Find active event and next upcoming event
        Event activeEvent = null;
        Event upcomingEvent = null;

        for (Event event : events) {
            if ("ACTIVE".equals(event.getStatus())) {
                activeEvent = event;
            } else if ("UPCOMING".equals(event.getStatus()) && upcomingEvent == null) {
                // Check if within 3 hours
                LocalDateTime startTime = parseDateTime(event.getStartTime());
                long minutesUntil = ChronoUnit.MINUTES.between(now, startTime);
                if (minutesUntil <= 180 && minutesUntil >= 0) { // Within 3 hours
                    upcomingEvent = event;
                }
            }
        }

        // Determine what to show based on config and event states
        boolean showActive = activeEvent != null && config.showActiveEvent();
        boolean showUpcoming = upcomingEvent != null && config.showUpcoming();

        // Check for "starting soon" (within 30 minutes)
        boolean isStartingSoon = false;
        if (upcomingEvent != null) {
            LocalDateTime startTime = parseDateTime(upcomingEvent.getStartTime());
            long minutesUntil = ChronoUnit.MINUTES.between(now, startTime);
            isStartingSoon = minutesUntil <= 30 && minutesUntil >= 0;

            // If starting soon but config disabled, don't show
            if (isStartingSoon && !config.showStartingSoon()) {
                showUpcoming = false;
            }
        }

        // Check for "ending soon" (within 30 minutes)
        boolean isEndingSoon = false;
        if (activeEvent != null) {
            LocalDateTime endTime = parseDateTime(activeEvent.getStartTime())
                    .plusMinutes(activeEvent.getDurationMinutes());
            long minutesLeft = ChronoUnit.MINUTES.between(now, endTime);
            isEndingSoon = minutesLeft <= 30 && minutesLeft >= 0;

            // If ending soon but config disabled, check if we should still show active
            if (isEndingSoon && !config.showEndingSoon()) {
                // Still show active, just don't highlight as ending soon
                isEndingSoon = false;
            }
        }

        // If nothing to show, return null
        if (!showActive && !showUpcoming) {
            return null;
        }

        panelComponent.getChildren().clear();

        // Always show ARCEUUS header
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("ARCEUUS")
                .color(ARCEUUS_PURPLE)
                .build());

        // Show active event
        if (showActive) {
            renderActiveEvent(activeEvent, now, isEndingSoon);
        }

        // Show upcoming event
        if (showUpcoming) {
            if (showActive) {
                // Add separator
                panelComponent.getChildren().add(LineComponent.builder().build());
            }
            renderUpcomingEvent(upcomingEvent, now, isStartingSoon);
        }

        return panelComponent.render(graphics);
    }

    private void renderActiveEvent(Event event, LocalDateTime now, boolean isEndingSoon) {
        // Calculate time remaining
        LocalDateTime endTime = parseDateTime(event.getStartTime())
                .plusMinutes(event.getDurationMinutes());
        long secondsLeft = ChronoUnit.SECONDS.between(now, endTime);

        // Title with appropriate color
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

        // Time remaining countdown
        if (secondsLeft > 0) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Ends in:")
                    .leftColor(Color.GRAY)
                    .right(formatCountdown(secondsLeft))
                    .rightColor(isEndingSoon ? ENDING_SOON_RED : Color.ORANGE)
                    .build());
        }

        // Signups count
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Signups:")
                .leftColor(Color.GRAY)
                .right(String.valueOf(event.getSignups() != null ? event.getSignups().size() : 0))
                .rightColor(Color.WHITE)
                .build());
    }

    private void renderUpcomingEvent(Event event, LocalDateTime now, boolean isStartingSoon) {
        // Calculate time until start
        LocalDateTime startTime = parseDateTime(event.getStartTime());
        long secondsUntil = ChronoUnit.SECONDS.between(now, startTime);

        // Title with appropriate color
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

        // Time until start countdown
        if (secondsUntil > 0) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Starts in:")
                    .leftColor(Color.GRAY)
                    .right(formatCountdown(secondsUntil))
                    .rightColor(isStartingSoon ? STARTING_SOON_YELLOW : Color.WHITE)
                    .build());
        } else {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Starting now!")
                    .leftColor(STARTING_SOON_YELLOW)
                    .build());
        }

        // Signups count
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Signups:")
                .leftColor(Color.GRAY)
                .right(String.valueOf(event.getSignups() != null ? event.getSignups().size() : 0))
                .rightColor(Color.WHITE)
                .build());

        // Show if player is signed up
        if (plugin.isSignedUp(event.getEventId())) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("You are signed up!")
                    .leftColor(LIVE_GREEN)
                    .build());
        }
    }

    private LocalDateTime parseDateTime(String isoTime) {
        return DateTimeUtils.parseDateTime(isoTime);
    }

    /**
     * Format countdown: minutes until < 1 minute, then seconds
     */
    private String formatCountdown(long totalSeconds) {
        if (totalSeconds < 0) {
            return "0s";
        }

        // Less than 1 minute - show seconds
        if (totalSeconds < 60) {
            return totalSeconds + "s";
        }

        long totalMinutes = totalSeconds / 60;

        // Less than 1 hour - show minutes
        if (totalMinutes < 60) {
            return totalMinutes + "m";
        }

        // Less than 24 hours - show hours and minutes
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        if (hours < 24) {
            return hours + "h " + minutes + "m";
        }

        // More than 24 hours - show days and hours
        long days = hours / 24;
        hours = hours % 24;
        return days + "d " + hours + "h";
    }
}
