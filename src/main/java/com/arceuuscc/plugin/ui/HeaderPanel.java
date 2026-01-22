package com.arceuuscc.plugin.ui;

import com.arceuuscc.plugin.ArceuusCCPlugin;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Header panel with title, connection status, and refresh button.
 */
public class HeaderPanel extends JPanel
{
	private final ArceuusCCPlugin plugin;
	private final JLabel connectionStatus;
	private final JLabel playerInfoLabel;
	private final JLabel clanStatusLabel;
	private final JLabel authCodeLabel;

	public HeaderPanel(ArceuusCCPlugin plugin)
	{
		this.plugin = plugin;
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

		authCodeLabel = new JLabel("");
		authCodeLabel.setFont(new Font("Monospaced", Font.PLAIN, 10));
		authCodeLabel.setForeground(PanelColors.getLightGray());
		authCodeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		authCodeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		authCodeLabel.setToolTipText("Click to copy auth code");
		authCodeLabel.setVisible(false);
		authCodeLabel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				String token = plugin.getAuthToken();
				if (token != null)
				{
					StringSelection selection = new StringSelection(token);
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
					authCodeLabel.setText("Code: copied!");
					Timer resetTimer = new Timer(2000, evt -> updateAuthCode(token));
					resetTimer.setRepeats(false);
					resetTimer.start();
				}
			}
		});
		add(authCodeLabel);
	}

	public void updateConnectionStatus(boolean connected)
	{
		if (connected)
		{
			connectionStatus.setText("Connected");
			connectionStatus.setForeground(PanelColors.CONNECTED);
		}
		else
		{
			connectionStatus.setText("Disconnected");
			connectionStatus.setForeground(PanelColors.DISCONNECTED);
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

			String token = plugin.getAuthToken();
			if (token != null)
			{
				updateAuthCode(token);
				authCodeLabel.setVisible(true);
			}
			else
			{
				authCodeLabel.setVisible(false);
			}
		}
		else
		{
			playerInfoLabel.setVisible(false);
			clanStatusLabel.setVisible(false);
			authCodeLabel.setVisible(false);
		}
	}

	private void updateAuthCode(String token)
	{
		String displayToken = token.length() > 8
			? token.substring(0, 8) + "..."
			: token;
		authCodeLabel.setText("Code: " + displayToken + " (click to copy)");
	}
}
