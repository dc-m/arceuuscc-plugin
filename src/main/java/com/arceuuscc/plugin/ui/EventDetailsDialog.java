package com.arceuuscc.plugin.ui;

import com.arceuuscc.plugin.models.Event;
import com.arceuuscc.plugin.util.FormatUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Dialog showing full event details.
 */
public class EventDetailsDialog
{
	public static void show(Component parent, Event event)
	{
		JPanel dialogPanel = new JPanel();
		dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.Y_AXIS));
		dialogPanel.setBackground(PanelColors.getDarkerGray());
		dialogPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

		addTitle(dialogPanel, event);
		addEventInfo(dialogPanel, event);
		addDescription(dialogPanel, event);
		addSignupsList(dialogPanel, event);

		JOptionPane.showMessageDialog(parent, dialogPanel, "Event Details", JOptionPane.PLAIN_MESSAGE);
	}

	private static void addTitle(JPanel panel, Event event)
	{
		JLabel titleLabel = new JLabel(event.getTitle());
		titleLabel.setFont(new Font(PanelColors.getEmojiFont(), Font.BOLD, 16));
		titleLabel.setForeground(PanelColors.UPCOMING_TITLE);
		titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(titleLabel);
		panel.add(Box.createVerticalStrut(10));
	}

	private static void addEventInfo(JPanel panel, Event event)
	{
		addInfoLabel(panel, "When: " + FormatUtils.formatEventTime(event.getStartTime()) + " UTC");
		addInfoLabel(panel, "Duration: " + FormatUtils.formatDuration(event.getDurationMinutes()));
		addInfoLabel(panel, "Status: " + event.getStatus());

		int signupCount = event.getSignups() != null ? event.getSignups().size() : 0;
		addInfoLabel(panel, "Signups: " + signupCount);

		panel.add(Box.createVerticalStrut(15));
	}

	private static void addInfoLabel(JPanel panel, String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(new Font("Arial", Font.PLAIN, 12));
		label.setForeground(Color.WHITE);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(label);
	}

	private static void addDescription(JPanel panel, Event event)
	{
		JLabel descHeader = new JLabel("Description:");
		descHeader.setFont(new Font("Arial", Font.BOLD, 12));
		descHeader.setForeground(Color.WHITE);
		descHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(descHeader);
		panel.add(Box.createVerticalStrut(5));

		JTextArea descArea = new JTextArea(event.getDescription());
		descArea.setFont(new Font(PanelColors.getEmojiFont(), Font.PLAIN, 12));
		descArea.setForeground(Color.WHITE);
		descArea.setBackground(PanelColors.getDarkGray());
		descArea.setEditable(false);
		descArea.setLineWrap(true);
		descArea.setWrapStyleWord(true);
		descArea.setBorder(new EmptyBorder(8, 8, 8, 8));

		JScrollPane descScroll = new JScrollPane(descArea);
		descScroll.setPreferredSize(new Dimension(350, 150));
		descScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
		descScroll.setBorder(BorderFactory.createLineBorder(PanelColors.getMediumGray()));
		panel.add(descScroll);
	}

	private static void addSignupsList(JPanel panel, Event event)
	{
		if (event.getSignups() == null || event.getSignups().isEmpty())
		{
			return;
		}

		panel.add(Box.createVerticalStrut(15));

		JLabel signupsHeader = new JLabel("Signed Up Players:");
		signupsHeader.setFont(new Font("Arial", Font.BOLD, 12));
		signupsHeader.setForeground(Color.WHITE);
		signupsHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(signupsHeader);
		panel.add(Box.createVerticalStrut(5));

		StringBuilder signupsList = new StringBuilder();
		for (int i = 0; i < event.getSignups().size(); i++)
		{
			if (i > 0)
			{
				signupsList.append(", ");
			}
			signupsList.append(event.getSignups().get(i).getOsrsName());
		}

		JTextArea signupsArea = new JTextArea(signupsList.toString());
		signupsArea.setFont(new Font("Arial", Font.PLAIN, 11));
		signupsArea.setForeground(PanelColors.getLightGray());
		signupsArea.setBackground(PanelColors.getDarkGray());
		signupsArea.setEditable(false);
		signupsArea.setLineWrap(true);
		signupsArea.setWrapStyleWord(true);
		signupsArea.setBorder(new EmptyBorder(8, 8, 8, 8));

		JScrollPane signupsScroll = new JScrollPane(signupsArea);
		signupsScroll.setPreferredSize(new Dimension(350, 60));
		signupsScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
		signupsScroll.setBorder(BorderFactory.createLineBorder(PanelColors.getMediumGray()));
		panel.add(signupsScroll);
	}
}
