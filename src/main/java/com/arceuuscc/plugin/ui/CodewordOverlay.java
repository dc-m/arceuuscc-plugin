package com.arceuuscc.plugin.ui;

import com.arceuuscc.plugin.ArceuusCCConfig;
import com.arceuuscc.plugin.ArceuusCCPlugin;
import com.arceuuscc.plugin.models.Event;
import com.arceuuscc.plugin.util.DateTimeUtils;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Single-line overlay showing an active event's codeword with the current UTC date and time.
 * Separate from the event overlay so it can be positioned on its own in any overlay mode.
 */
public class CodewordOverlay extends Overlay
{
	private static final Color CODEWORD_YELLOW = new Color(255, 220, 0);
	private static final int PANEL_PADDING = 10;
	private static final String GAP = "  ";

	private final ArceuusCCPlugin plugin;
	private final ArceuusCCConfig config;

	private final PanelComponent panelComponent = new PanelComponent();

	public CodewordOverlay(ArceuusCCPlugin plugin, ArceuusCCConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;

		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showCodewordOverlay() || !plugin.hasPluginAccess())
		{
			return null;
		}

		List<Event> events = plugin.getEvents();
		if (events == null)
		{
			return null;
		}

		List<Event> codewordEvents = new ArrayList<>();
		for (Event event : events)
		{
			if (plugin.isCodewordOnOverlay(event))
			{
				codewordEvents.add(event);
			}
		}

		if (codewordEvents.isEmpty())
		{
			return null;
		}

		codewordEvents.sort(Comparator.comparing(e -> DateTimeUtils.parseDateTime(e.getStartTime())));

		String timestamp = DateTimeUtils.currentUtcTimestamp();
		FontMetrics fontMetrics = graphics.getFontMetrics();

		// Size the panel to the longest codeword so each event stays on one line
		int width = 0;
		for (Event event : codewordEvents)
		{
			width = Math.max(width, fontMetrics.stringWidth(event.getCodeword() + GAP + timestamp) + PANEL_PADDING);
		}

		panelComponent.getChildren().clear();
		panelComponent.setPreferredSize(new Dimension(width, 0));

		for (Event event : codewordEvents)
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left(event.getCodeword())
				.leftColor(CODEWORD_YELLOW)
				.right(timestamp)
				.rightColor(Color.WHITE)
				.build());
		}

		return panelComponent.render(graphics);
	}
}
