package com.arceuuscc.plugin.ui;

import net.runelite.client.ui.ColorScheme;

import java.awt.Color;
import java.awt.GraphicsEnvironment;

/**
 * Color constants used throughout the plugin UI.
 */
public final class PanelColors
{
	// Event status colors
	public static final Color ACTIVE_BG = new Color(30, 50, 30);
	public static final Color ACTIVE_BORDER = new Color(50, 150, 50);
	public static final Color ACTIVE_TITLE = new Color(50, 200, 50);

	public static final Color UPCOMING_TITLE = new Color(114, 137, 218);

	public static final Color COMPLETED_BG = new Color(45, 45, 45);
	public static final Color COMPLETED_BORDER = new Color(80, 80, 80);

	public static final Color CANCELLED_BG = new Color(60, 40, 40);
	public static final Color CANCELLED_BORDER = new Color(150, 50, 50);

	public static final Color UNSEEN_BG = new Color(50, 45, 30);
	public static final Color UNSEEN_BORDER = new Color(218, 165, 32);
	public static final Color GOLD = new Color(218, 165, 32);

	// Newsletter colors
	public static final Color NEWSLETTER_LATEST_BG = new Color(60, 50, 30);
	public static final Color NEWSLETTER_LATEST_BORDER = new Color(139, 69, 19);

	// Status colors
	public static final Color CONNECTED = new Color(0, 200, 83);
	public static final Color DISCONNECTED = Color.RED;
	public static final Color WARNING = Color.ORANGE;

	// Button colors
	public static final Color SIGNUP_BUTTON = new Color(50, 150, 50);
	public static final Color LEAVE_BUTTON = new Color(150, 50, 50);

	// No access panel colors
	public static final Color NO_ACCESS_BG = new Color(60, 40, 30);
	public static final Color NO_ACCESS_BORDER = new Color(139, 69, 19);
	public static final Color NO_ACCESS_TITLE = new Color(255, 140, 0);

	// Dialog colors
	public static final Color DIALOG_BG = new Color(30, 30, 30);

	// Delegate to ColorScheme for standard colors
	public static Color getDarkGray()
	{
		return ColorScheme.DARK_GRAY_COLOR;
	}

	public static Color getDarkerGray()
	{
		return ColorScheme.DARKER_GRAY_COLOR;
	}

	public static Color getMediumGray()
	{
		return ColorScheme.MEDIUM_GRAY_COLOR;
	}

	public static Color getLightGray()
	{
		return ColorScheme.LIGHT_GRAY_COLOR;
	}

	private PanelColors()
	{
		// Utility class
	}

	// Emoji font support
	private static final String EMOJI_FONT = detectEmojiFont();

	private static String detectEmojiFont()
	{
		String[] emojiFonts = {"Segoe UI Emoji", "Apple Color Emoji", "Noto Color Emoji", "Segoe UI Symbol", "Arial"};
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		String[] availableFonts = ge.getAvailableFontFamilyNames();

		for (String emojiFont : emojiFonts)
		{
			for (String available : availableFonts)
			{
				if (available.equalsIgnoreCase(emojiFont))
				{
					return emojiFont;
				}
			}
		}
		return "Arial";
	}

	public static String getEmojiFont()
	{
		return EMOJI_FONT;
	}
}
