package com.arceuuscc.plugin.ui;

import com.arceuuscc.plugin.ArceuusCCPlugin;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

/**
 * Header panel with title, connection status, and refresh button.
 */
public class HeaderPanel extends JPanel
{
	private final JLabel connectionStatus;
	private final JLabel playerInfoLabel;
	private final JLabel clanStatusLabel;
	private final JButton refreshButton;

	public HeaderPanel(ArceuusCCPlugin plugin)
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(PanelColors.getDarkerGray());
		setBorder(new EmptyBorder(10, 10, 10, 10));

		JLabel titleLabel = new JLabel("Arceuus CC Events");
		titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
		titleLabel.setForeground(Color.WHITE);
		titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		add(titleLabel);

		add(Box.createVerticalStrut(5));

		connectionStatus = new JLabel("Connecting...");
		connectionStatus.setForeground(Color.ORANGE);
		connectionStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
		add(connectionStatus);

		add(Box.createVerticalStrut(5));

		playerInfoLabel = new JLabel("");
		playerInfoLabel.setForeground(Color.WHITE);
		playerInfoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		playerInfoLabel.setVisible(false);
		add(playerInfoLabel);

		clanStatusLabel = new JLabel("");
		clanStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		add(clanStatusLabel);

		add(Box.createVerticalStrut(10));

		refreshButton = new JButton("Refresh");
		refreshButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		refreshButton.addActionListener(e -> {
			plugin.refreshEvents();
			plugin.refreshNewsletters();
		});
		add(refreshButton);
	}

	public void updateConnectionStatus(boolean connected)
	{
		if (connected)
		{
			connectionStatus.setText("Connected");
			connectionStatus.setForeground(PanelColors.CONNECTED);
			refreshButton.setEnabled(true);
		}
		else
		{
			connectionStatus.setText("Disconnected");
			connectionStatus.setForeground(PanelColors.DISCONNECTED);
			refreshButton.setEnabled(false);
		}
	}

	public void updatePlayerInfo(String playerName, boolean inClan)
	{
		if (playerName != null)
		{
			playerInfoLabel.setText("Player: " + playerName);
			playerInfoLabel.setVisible(true);

			if (inClan)
			{
				clanStatusLabel.setText("In Arceuus clan");
				clanStatusLabel.setForeground(PanelColors.CONNECTED);
			}
			else
			{
				clanStatusLabel.setText("Not in Arceuus clan");
				clanStatusLabel.setForeground(PanelColors.WARNING);
			}
			clanStatusLabel.setVisible(true);
		}
		else
		{
			playerInfoLabel.setVisible(false);
			clanStatusLabel.setVisible(false);
		}
	}
}
