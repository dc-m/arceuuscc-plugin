package com.arceuuscc.plugin;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("arceuuscc")
public interface ArceuusCCConfig extends Config
{
	@ConfigSection(
		name = "Overlay Settings",
		description = "Configure the in-game overlay",
		position = 0
	)
	String overlaySection = "overlay";

	@ConfigSection(
		name = "Notification Settings",
		description = "Configure event notifications",
		position = 1
	)
	String notificationSection = "notifications";

	@ConfigItem(
		keyName = "showOverlay",
		name = "Show Overlay",
		description = "Show the Arceuus event overlay in-game",
		position = 0,
		section = overlaySection
	)
	default boolean showOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showActiveEvent",
		name = "Show Active Event",
		description = "Show currently active events in the overlay",
		position = 1,
		section = overlaySection
	)
	default boolean showActiveEvent()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showStartingSoon",
		name = "Show Starting Soon",
		description = "Show events starting within 30 minutes",
		position = 2,
		section = overlaySection
	)
	default boolean showStartingSoon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showUpcoming",
		name = "Show Upcoming Events",
		description = "Show events starting within 3 hours",
		position = 3,
		section = overlaySection
	)
	default boolean showUpcoming()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showEndingSoon",
		name = "Show Ending Soon",
		description = "Show when active events are ending within 30 minutes",
		position = 4,
		section = overlaySection
	)
	default boolean showEndingSoon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showNotifications",
		name = "Enable Notifications",
		description = "Master toggle for all notifications",
		position = 0,
		section = notificationSection
	)
	default boolean showNotifications()
	{
		return true;
	}

	@ConfigItem(
		keyName = "notifyNewEvent",
		name = "New Event",
		description = "Notify when a new event is created",
		position = 1,
		section = notificationSection
	)
	default boolean notifyNewEvent()
	{
		return true;
	}

	@ConfigItem(
		keyName = "notifyEventStarting",
		name = "Event Starting",
		description = "Notify when an event starts",
		position = 2,
		section = notificationSection
	)
	default boolean notifyEventStarting()
	{
		return true;
	}

	@ConfigItem(
		keyName = "notifyEventEnding",
		name = "Event Ending",
		description = "Notify when an event ends",
		position = 3,
		section = notificationSection
	)
	default boolean notifyEventEnding()
	{
		return true;
	}

	@ConfigItem(
		keyName = "notifyEventCancelled",
		name = "Event Cancelled",
		description = "Notify when an event is cancelled",
		position = 4,
		section = notificationSection
	)
	default boolean notifyEventCancelled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "autoRefresh",
		name = "Auto Refresh",
		description = "Automatically refresh events list periodically",
		position = 10
	)
	default boolean autoRefresh()
	{
		return true;
	}
}
