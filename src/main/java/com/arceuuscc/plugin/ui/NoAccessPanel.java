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
 * Panel shown when user doesn't have access (not in clan).
 */
public class NoAccessPanel extends JPanel
{
	public NoAccessPanel(String clanName)
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(PanelColors.NO_ACCESS_BG);
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(PanelColors.NO_ACCESS_BORDER, 2),
			new EmptyBorder(10, 10, 10, 10)
		));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setMaximumSize(new Dimension(PluginPanel.PANEL_WIDTH - 10, Integer.MAX_VALUE));

		JLabel titleLabel = new JLabel("<html><div style='width:170px'>Clan Membership Required</div></html>");
		titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
		titleLabel.setForeground(PanelColors.NO_ACCESS_TITLE);
		titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		add(titleLabel);

		add(Box.createVerticalStrut(8));

		JLabel messageLabel = new JLabel("<html><div style='width:150px'>To use the Arceuus CC Plugin, please join the \"" +
			clanName + "\" Clan Chat.</div></html>");
		messageLabel.setFont(new Font("Arial", Font.PLAIN, 11));
		messageLabel.setForeground(Color.WHITE);
		messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		add(messageLabel);

		add(Box.createVerticalStrut(15));

		JLabel discordLabel = new JLabel("Join us on Discord:");
		discordLabel.setFont(new Font("Arial", Font.BOLD, 11));
		discordLabel.setForeground(PanelColors.getLightGray());
		discordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		add(discordLabel);

		add(Box.createVerticalStrut(5));

		JLabel linkLabel = new JLabel("<html><u>discord.gg/Ka3bVn6nkW</u></html>");
		linkLabel.setFont(new Font("Arial", Font.PLAIN, 11));
		linkLabel.setForeground(PanelColors.UPCOMING_TITLE);
		linkLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
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
