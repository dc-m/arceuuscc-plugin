package com.arceuuscc.plugin.ui;

import com.arceuuscc.plugin.models.AuthorizationState;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
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
 * Panel shown when user needs authorization approval.
 */
public class PendingApprovalPanel extends JPanel
{
	// Panel colors for different states
	private static final Color PENDING_BG = new Color(50, 50, 60);
	private static final Color PENDING_BORDER = new Color(100, 100, 150);
	private static final Color PENDING_TITLE = new Color(150, 150, 200);

	private static final Color REJECTED_BG = new Color(60, 40, 40);
	private static final Color REJECTED_BORDER = new Color(150, 50, 50);
	private static final Color REJECTED_TITLE = new Color(255, 100, 100);

	private static final Color REVOKED_BG = new Color(60, 50, 40);
	private static final Color REVOKED_BORDER = new Color(150, 100, 50);
	private static final Color REVOKED_TITLE = new Color(255, 150, 50);

	private final Runnable onRequestAccess;

	public PendingApprovalPanel(AuthorizationState state, String authToken, String reason, Runnable onRequestAccess)
	{
		this.onRequestAccess = onRequestAccess;

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setMaximumSize(new Dimension(PluginPanel.PANEL_WIDTH - 10, Integer.MAX_VALUE));

		switch (state)
		{
			case NO_TOKEN:
				buildNoTokenPanel();
				break;
			case UNKNOWN:
				buildVerifyingPanel();
				break;
			case PENDING:
				buildPendingPanel(authToken);
				break;
			case REJECTED:
				buildRejectedPanel(reason);
				break;
			case REVOKED:
				buildRevokedPanel(reason);
				break;
			default:
				buildNoTokenPanel();
		}
	}

	private void buildNoTokenPanel()
	{
		setBackground(PENDING_BG);
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(PENDING_BORDER, 2),
			new EmptyBorder(15, 15, 15, 15)
		));

		JLabel titleLabel = new JLabel("Authorization Required");
		titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
		titleLabel.setForeground(PENDING_TITLE);
		titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(titleLabel);

		add(Box.createVerticalStrut(15));

		JLabel messageLabel = new JLabel("<html><div style='width:150px; text-align:center'>" +
			"To use the Arceuus CC Plugin, you need approval from a clan admin." +
			"</div></html>");
		messageLabel.setFont(new Font("Arial", Font.PLAIN, 11));
		messageLabel.setForeground(Color.WHITE);
		messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(messageLabel);

		add(Box.createVerticalStrut(20));

		JButton requestButton = new JButton("Request Access");
		requestButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		requestButton.setBackground(PanelColors.SIGNUP_BUTTON);
		requestButton.setForeground(Color.WHITE);
		requestButton.setFocusPainted(false);
		requestButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		requestButton.addActionListener(e -> {
			if (onRequestAccess != null)
			{
				onRequestAccess.run();
			}
		});
		add(requestButton);

		addDiscordLink();
	}

	private void buildVerifyingPanel()
	{
		setBackground(PENDING_BG);
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(PENDING_BORDER, 2),
			new EmptyBorder(15, 15, 15, 15)
		));

		JLabel titleLabel = new JLabel("Verifying Access");
		titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
		titleLabel.setForeground(PENDING_TITLE);
		titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(titleLabel);

		add(Box.createVerticalStrut(15));

		JLabel messageLabel = new JLabel("<html><div style='width:150px; text-align:center'>" +
			"Checking your authorization status with the server..." +
			"</div></html>");
		messageLabel.setFont(new Font("Arial", Font.PLAIN, 11));
		messageLabel.setForeground(Color.WHITE);
		messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(messageLabel);

		add(Box.createVerticalStrut(10));

		JLabel statusLabel = new JLabel("Please wait...");
		statusLabel.setFont(new Font("Arial", Font.ITALIC, 10));
		statusLabel.setForeground(PanelColors.getLightGray());
		statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(statusLabel);

		addDiscordLink();
	}

	private void buildPendingPanel(String authToken)
	{
		setBackground(PENDING_BG);
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(PENDING_BORDER, 2),
			new EmptyBorder(15, 15, 15, 15)
		));

		JLabel titleLabel = new JLabel("Awaiting Approval");
		titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
		titleLabel.setForeground(PENDING_TITLE);
		titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(titleLabel);

		add(Box.createVerticalStrut(15));

		JLabel messageLabel = new JLabel("<html><div style='width:150px; text-align:center'>" +
			"A request has been sent to the Arceuus Admins. Once an admin has approved your request, " +
			"you'll have full access to use the plugin." +
			"</div></html>");
		messageLabel.setFont(new Font("Arial", Font.PLAIN, 11));
		messageLabel.setForeground(Color.WHITE);
		messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(messageLabel);

		add(Box.createVerticalStrut(15));

		JLabel codeLabel = new JLabel("Your request code is:");
		codeLabel.setFont(new Font("Arial", Font.BOLD, 11));
		codeLabel.setForeground(PanelColors.getLightGray());
		codeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(codeLabel);

		add(Box.createVerticalStrut(5));

		// Show truncated token (first 12 chars)
		String displayToken = authToken != null && authToken.length() > 12
			? authToken.substring(0, 12) + "..."
			: authToken;

		JLabel tokenLabel = new JLabel(displayToken);
		tokenLabel.setFont(new Font("Monospaced", Font.BOLD, 12));
		tokenLabel.setForeground(PanelColors.GOLD);
		tokenLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(tokenLabel);

		add(Box.createVerticalStrut(10));

		JLabel statusLabel = new JLabel("Checking status automatically...");
		statusLabel.setFont(new Font("Arial", Font.ITALIC, 10));
		statusLabel.setForeground(PanelColors.getLightGray());
		statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(statusLabel);

		addDiscordLink();
	}

	private void buildRejectedPanel(String reason)
	{
		setBackground(REJECTED_BG);
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(REJECTED_BORDER, 2),
			new EmptyBorder(15, 15, 15, 15)
		));

		JLabel titleLabel = new JLabel("Access Denied");
		titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
		titleLabel.setForeground(REJECTED_TITLE);
		titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(titleLabel);

		add(Box.createVerticalStrut(15));

		JLabel messageLabel = new JLabel("<html><div style='width:150px; text-align:center'>" +
			"Your request for access to the Arceuus CC Plugin has been denied." +
			"</div></html>");
		messageLabel.setFont(new Font("Arial", Font.PLAIN, 11));
		messageLabel.setForeground(Color.WHITE);
		messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(messageLabel);

		if (reason != null && !reason.isEmpty())
		{
			add(Box.createVerticalStrut(10));

			JLabel reasonTitleLabel = new JLabel("Reason:");
			reasonTitleLabel.setFont(new Font("Arial", Font.BOLD, 11));
			reasonTitleLabel.setForeground(PanelColors.getLightGray());
			reasonTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			add(reasonTitleLabel);

			add(Box.createVerticalStrut(5));

			JLabel reasonLabel = new JLabel("<html><div style='width:150px; text-align:center'>" +
				reason + "</div></html>");
			reasonLabel.setFont(new Font("Arial", Font.ITALIC, 11));
			reasonLabel.setForeground(Color.WHITE);
			reasonLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			add(reasonLabel);
		}

		add(Box.createVerticalStrut(15));

		JLabel contactLabel = new JLabel("<html><div style='width:150px; text-align:center'>" +
			"Please contact a clan admin on Discord if you believe this was a mistake." +
			"</div></html>");
		contactLabel.setFont(new Font("Arial", Font.PLAIN, 10));
		contactLabel.setForeground(PanelColors.getLightGray());
		contactLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(contactLabel);

		addDiscordLink();
	}

	private void buildRevokedPanel(String reason)
	{
		setBackground(REVOKED_BG);
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(REVOKED_BORDER, 2),
			new EmptyBorder(15, 15, 15, 15)
		));

		JLabel titleLabel = new JLabel("Access Revoked");
		titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
		titleLabel.setForeground(REVOKED_TITLE);
		titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(titleLabel);

		add(Box.createVerticalStrut(15));

		JLabel messageLabel = new JLabel("<html><div style='width:150px; text-align:center'>" +
			"Your access to the Arceuus CC Plugin has been revoked." +
			"</div></html>");
		messageLabel.setFont(new Font("Arial", Font.PLAIN, 11));
		messageLabel.setForeground(Color.WHITE);
		messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(messageLabel);

		if (reason != null && !reason.isEmpty())
		{
			add(Box.createVerticalStrut(10));

			JLabel reasonTitleLabel = new JLabel("Reason:");
			reasonTitleLabel.setFont(new Font("Arial", Font.BOLD, 11));
			reasonTitleLabel.setForeground(PanelColors.getLightGray());
			reasonTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			add(reasonTitleLabel);

			add(Box.createVerticalStrut(5));

			JLabel reasonLabel = new JLabel("<html><div style='width:150px; text-align:center'>" +
				reason + "</div></html>");
			reasonLabel.setFont(new Font("Arial", Font.ITALIC, 11));
			reasonLabel.setForeground(Color.WHITE);
			reasonLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			add(reasonLabel);
		}

		add(Box.createVerticalStrut(15));

		JLabel contactLabel = new JLabel("<html><div style='width:150px; text-align:center'>" +
			"Please contact a clan admin on Discord to appeal." +
			"</div></html>");
		contactLabel.setFont(new Font("Arial", Font.PLAIN, 10));
		contactLabel.setForeground(PanelColors.getLightGray());
		contactLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(contactLabel);

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
