package com.arceuuscc.plugin.ui;

import com.arceuuscc.plugin.ArceuusCCPlugin;
import com.arceuuscc.plugin.models.AuthorizationState;
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
	private PendingApprovalPanel pendingApprovalPanel;
	private RequirementsPanel requirementsPanel;

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
		updateAuthorizationState();
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
		updateAuthorizationState();
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

	/**
	 * Update the panel based on authorization state.
	 * Shows the main tabbed content ONLY when all conditions are met:
	 * 1. User is logged in
	 * 2. User is in the Arceuus clan
	 * 3. User has ACCEPTED authorization status
	 */
	public void updateAuthorizationState()
	{
		// Clear any existing overlay panels
		clearOverlayPanels();

		// Check condition 1: User must be logged in
		if (plugin.getPlayerName() == null)
		{
			remove(tabbedPane);
			requirementsPanel = new RequirementsPanel(RequirementsPanel.RequirementType.NOT_LOGGED_IN);
			add(requirementsPanel, BorderLayout.CENTER);
			revalidate();
			repaint();
			return;
		}

		// Check condition 2: User must be in the Arceuus clan
		if (!plugin.isInClan())
		{
			remove(tabbedPane);
			requirementsPanel = new RequirementsPanel(RequirementsPanel.RequirementType.NOT_IN_CLAN);
			add(requirementsPanel, BorderLayout.CENTER);
			revalidate();
			repaint();
			return;
		}

		// Check condition 3: User must have ACCEPTED authorization
		AuthorizationState state = plugin.getAuthState();
		if (!state.hasAccess())
		{
			remove(tabbedPane);
			pendingApprovalPanel = new PendingApprovalPanel(
				state,
				plugin.getAuthToken(),
				plugin.getAuthReason(),
				() -> plugin.requestAccess()
			);
			add(pendingApprovalPanel, BorderLayout.CENTER);

			// Start polling for auth status changes (PENDING, REJECTED, or REVOKED)
			// This allows the plugin to detect when an admin approves/changes status
			if (plugin.getAuthToken() != null && plugin.getHttpClient() != null)
			{
				plugin.getHttpClient().startAuthPolling();
			}

			revalidate();
			repaint();
			return;
		}

		// All conditions met - show the main content
		add(tabbedPane, BorderLayout.CENTER);
		revalidate();
		repaint();
	}

	/**
	 * Remove any overlay panels (requirements or pending approval).
	 */
	private void clearOverlayPanels()
	{
		if (pendingApprovalPanel != null)
		{
			remove(pendingApprovalPanel);
			pendingApprovalPanel = null;
		}
		if (requirementsPanel != null)
		{
			remove(requirementsPanel);
			requirementsPanel = null;
		}
	}
}
