package com.arceuuscc.plugin.ui;

import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Panel shown when user doesn't meet basic requirements (not logged in or not in clan).
 */
public class RequirementsPanel extends JPanel
{
	public enum RequirementType
	{
		NOT_LOGGED_IN,
		NOT_IN_CLAN
	}

	public RequirementsPanel(RequirementType type)
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setMaximumSize(new Dimension(PluginPanel.PANEL_WIDTH - 10, Integer.MAX_VALUE));
		setBackground(PanelColors.getDarkGray());
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(80, 80, 100), 2),
			new EmptyBorder(15, 15, 15, 15)
		));

		switch (type)
		{
			case NOT_LOGGED_IN:
				buildNotLoggedInPanel();
				break;
			case NOT_IN_CLAN:
				buildNotInClanPanel();
				break;
		}
	}

	private void buildNotLoggedInPanel()
	{
		JLabel titleLabel = new JLabel("Login Required");
		titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
		titleLabel.setForeground(new Color(150, 150, 200));
		titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(titleLabel);

		add(Box.createVerticalStrut(15));

		JLabel messageLabel = new JLabel("<html><div style='width:150px; text-align:center'>" +
			"Please log in to your OSRS account to use the Arceuus CC Plugin." +
			"</div></html>");
		messageLabel.setFont(new Font("Arial", Font.PLAIN, 11));
		messageLabel.setForeground(Color.WHITE);
		messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(messageLabel);

		addDiscordLink();
	}

	private void buildNotInClanPanel()
	{
		JLabel titleLabel = new JLabel("Clan Required");
		titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
		titleLabel.setForeground(new Color(150, 150, 200));
		titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(titleLabel);

		add(Box.createVerticalStrut(15));

		JLabel messageLabel = new JLabel("<html><div style='width:150px; text-align:center'>" +
			"To use this plugin, please join the <b>Arceuus</b> Clan." +
			"</div></html>");
		messageLabel.setFont(new Font("Arial", Font.PLAIN, 11));
		messageLabel.setForeground(Color.WHITE);
		messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(messageLabel);

		add(Box.createVerticalStrut(15));

		JLabel instructionLabel = new JLabel("<html><div style='width:150px; text-align:center'>" +
			"Open the Clan tab and join our clan to get started." +
			"</div></html>");
		instructionLabel.setFont(new Font("Arial", Font.PLAIN, 10));
		instructionLabel.setForeground(PanelColors.getLightGray());
		instructionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(instructionLabel);

		addDiscordLink();
	}

	private void addDiscordLink()
	{
		add(Box.createVerticalStrut(20));

		JLabel discordLabel = new JLabel("<html><div style='width:150px; text-align:center'>Join us on Discord:</div></html>");
		discordLabel.setFont(new Font("Arial", Font.BOLD, 11));
		discordLabel.setForeground(PanelColors.getLightGray());
		discordLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(discordLabel);

		add(Box.createVerticalStrut(5));

		JLabel linkLabel = new JLabel("<html><div style='width:150px; text-align:center'><u>discord.gg/Ka3bVn6nkW</u></div></html>");
		linkLabel.setFont(new Font("Arial", Font.PLAIN, 11));
		linkLabel.setForeground(PanelColors.UPCOMING_TITLE);
		linkLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		linkLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		linkLabel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				LinkBrowser.browse("https://discord.gg/Ka3bVn6nkW");
			}
		});
		add(linkLabel);
	}
}
