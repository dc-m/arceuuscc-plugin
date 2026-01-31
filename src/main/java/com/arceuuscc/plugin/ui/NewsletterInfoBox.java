package com.arceuuscc.plugin.ui;

import com.arceuuscc.plugin.ArceuusCCPlugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.util.ColorUtil;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class NewsletterInfoBox extends InfoBox
{
	private static final Color NEWSLETTER_GOLD = new Color(218, 165, 32);

	public NewsletterInfoBox(BufferedImage image, ArceuusCCPlugin plugin)
	{
		super(image, plugin);
	}

	@Override
	public String getText()
	{
		return "NEW!";
	}

	@Override
	public Color getTextColor()
	{
		return NEWSLETTER_GOLD;
	}

	@Override
	public String getTooltip()
	{
		return ColorUtil.wrapWithColorTag("New Newsletter Available", NEWSLETTER_GOLD)
			+ "</br>" + ColorUtil.wrapWithColorTag("Check the sidebar panel to read", Color.WHITE);
	}
}
