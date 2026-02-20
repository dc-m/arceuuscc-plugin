package com.arceuuscc.plugin;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("arceuuscc")
public interface ArceuusCCConfig extends Config
{
	enum OverlayMode
	{
		DETAILED("Detailed"),
		MINIMAL("Minimal"),
		ICON_ONLY("Icon");

		private final String name;

		OverlayMode(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

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
		keyName = "overlayMode",
		name = "Overlay Mode",
		description = "Detailed shows full info, Minimal shows compact view, Icon shows small event icons",
		position = 1,
		section = overlaySection
	)
	default OverlayMode overlayMode()
	{
		return OverlayMode.ICON_ONLY;
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
		keyName = "notifyNewNewsletter",
		name = "New Newsletter",
		description = "Notify when a new newsletter is published",
		position = 5,
		section = notificationSection
	)
	default boolean notifyNewNewsletter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "notifyUnreadOnLogin",
		name = "Unread Notifications on Login",
		description = "Show notifications for unread events/newsletters when you log in",
		position = 6,
		section = notificationSection
	)
	default boolean notifyUnreadOnLogin()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showNewsletterOverlay",
		name = "Show Newsletter Alert",
		description = "Show an overlay alert when a new newsletter is available",
		position = 5,
		section = overlaySection
	)
	default boolean showNewsletterOverlay()
	{
		return true;
	}

}
