package com.arceuuscc.plugin.ui;

import com.arceuuscc.plugin.ArceuusCCPlugin;
import net.runelite.client.ui.PluginPanel;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Color;

/**
 * Main plugin panel displayed in the RuneLite sidebar.
 */
public class ArceuusCCPanel extends PluginPanel
{
	private final ArceuusCCPlugin plugin;
	private final HeaderPanel headerPanel;
	private final EventsTabPanel eventsTab;
	private final NewslettersTabPanel newslettersTab;
	private final JTabbedPane tabbedPane;

	public ArceuusCCPanel(ArceuusCCPlugin plugin)
	{
		super(false);
		this.plugin = plugin;

		setLayout(new BorderLayout());
		setBackground(PanelColors.getDarkGray());

		headerPanel = new HeaderPanel(plugin);
		add(headerPanel, BorderLayout.NORTH);

		tabbedPane = new JTabbedPane();
		tabbedPane.setBackground(PanelColors.getDarkGray());

		eventsTab = new EventsTabPanel(plugin);
		JScrollPane eventsScroll = new JScrollPane(eventsTab);
		eventsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		eventsScroll.setBorder(null);
		eventsScroll.getVerticalScrollBar().setUnitIncrement(16);
		tabbedPane.addTab("Events", eventsScroll);

		newslettersTab = new NewslettersTabPanel(plugin);
		JScrollPane newslettersScroll = new JScrollPane(newslettersTab);
		newslettersScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		newslettersScroll.setBorder(null);
		newslettersScroll.getVerticalScrollBar().setUnitIncrement(16);
		tabbedPane.addTab("Newsletters", newslettersScroll);

		add(tabbedPane, BorderLayout.CENTER);

		updateEvents();
		updateNewsletters();
	}

	public void updateConnectionStatus(boolean connected)
	{
		headerPanel.updateConnectionStatus(connected);
	}

	public void updatePlayerInfo()
	{
		headerPanel.updatePlayerInfo(plugin.getPlayerName(), plugin.isInClan());
		if (plugin.isInClan())
		{
			updateEvents();
			updateNewsletters();
		}
	}

	public void updateEvents()
	{
		eventsTab.refresh();
		updateEventTabTitle();
	}

	public void updateNewsletters()
	{
		newslettersTab.refresh();
		updateNewsletterTabTitle();
	}

	private void updateEventTabTitle()
	{
		if (plugin.hasUnseenEvents())
		{
			tabbedPane.setTitleAt(0, "Events *");
			tabbedPane.setForegroundAt(0, PanelColors.GOLD);
		}
		else
		{
			tabbedPane.setTitleAt(0, "Events");
			tabbedPane.setForegroundAt(0, null);
		}
	}

	private void updateNewsletterTabTitle()
	{
		if (plugin.hasUnreadNewsletter())
		{
			tabbedPane.setTitleAt(1, "Newsletters *");
			tabbedPane.setForegroundAt(1, new Color(255, 200, 0));
		}
		else
		{
			tabbedPane.setTitleAt(1, "Newsletters");
			tabbedPane.setForegroundAt(1, null);
		}
	}
}
