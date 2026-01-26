package com.arceuuscc.plugin.ui;

import com.arceuuscc.plugin.ArceuusCCPlugin;
import com.arceuuscc.plugin.models.Event;
import com.arceuuscc.plugin.util.FormatUtils;
import lombok.extern.slf4j.Slf4j;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDateTime;

/**
 * Builds event card panels.
 */
@Slf4j
public class EventPanelBuilder
{
	private final ArceuusCCPlugin plugin;

	public EventPanelBuilder(ArceuusCCPlugin plugin)
	{
		this.plugin = plugin;
	}

	public JPanel createEventPanel(Event event)
	{
		EventStatus status = new EventStatus(event, plugin);

		// Create wrapper with BorderLayout to position mail icon in top-right
		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(status.bgColor);
		wrapper.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(status.borderColor, status.isUnseen ? 2 : 1),
			new EmptyBorder(8, 8, 8, 8)
		));
		wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
		wrapper.setMaximumSize(new Dimension(225, Integer.MAX_VALUE));

		// Mail icon in top-right corner
		JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		topBar.setOpaque(false);
		JLabel mailIcon = new JLabel(status.isUnseen ? MailIcons.UNREAD : MailIcons.READ);
		mailIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
		mailIcon.setForeground(status.isUnseen ? PanelColors.GOLD : PanelColors.getLightGray());
		mailIcon.setToolTipText(status.isUnseen ? "Unread" : "Read");
		topBar.add(mailIcon);
		wrapper.add(topBar, BorderLayout.NORTH);

		// Content panel
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setOpaque(false);

		addTitle(panel, event, status);
		addTimeInfo(panel, event);
		addSignupCount(panel, event);
		addDescriptionPreview(panel, event);
		addDetailsButton(panel, event);

		// Allow signups for both UPCOMING and ACTIVE events
		if (status.isUpcoming || status.isActive)
		{
			addSignupButton(panel, event, status.isActive);
		}

		wrapper.add(panel, BorderLayout.CENTER);
		return wrapper;
	}

	private void addTitle(JPanel panel, Event event, EventStatus status)
	{
		String text = status.isCancelled
			? "<html><s>" + event.getTitle() + "</s></html>"
			: event.getTitle();

		JLabel label = new JLabel(text);
		label.setFont(new Font("Arial", Font.BOLD, 13));
		label.setForeground(status.titleColor);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(label);
		panel.add(Box.createVerticalStrut(4));
	}

	private void addTimeInfo(JPanel panel, Event event)
	{
		JLabel timeLabel = new JLabel(FormatUtils.formatEventTime(event.getStartTime()) + " UTC");
		timeLabel.setFont(new Font("Arial", Font.PLAIN, 11));
		timeLabel.setForeground(Color.WHITE);
		timeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(timeLabel);

		JLabel durationLabel = new JLabel("Duration: " + FormatUtils.formatDuration(event.getDurationMinutes()));
		durationLabel.setFont(new Font("Arial", Font.PLAIN, 10));
		durationLabel.setForeground(PanelColors.getLightGray());
		durationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(durationLabel);
		panel.add(Box.createVerticalStrut(4));
	}

	private void addSignupCount(JPanel panel, Event event)
	{
		int count = event.getSignups() != null ? event.getSignups().size() : 0;
		JLabel label = new JLabel("Signups: " + count);
		label.setFont(new Font("Arial", Font.PLAIN, 10));
		label.setForeground(PanelColors.getLightGray());
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(label);
	}

	private void addDescriptionPreview(JPanel panel, Event event)
	{
		String desc = event.getDescription();
		if (desc == null || desc.isEmpty())
		{
			return;
		}

		panel.add(Box.createVerticalStrut(4));
		String preview = desc.length() > 60 ? desc.substring(0, 57) + "..." : desc;
		JLabel label = new JLabel("<html><i>" + FormatUtils.escapeHtml(preview) + "</i></html>");
		label.setFont(new Font("Arial", Font.PLAIN, 10));
		label.setForeground(PanelColors.getLightGray());
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(label);
	}

	private void addDetailsButton(JPanel panel, Event event)
	{
		panel.add(Box.createVerticalStrut(4));
		JButton button = new JButton("View Details");
		button.setFont(new Font("Arial", Font.PLAIN, 10));
		button.setAlignmentX(Component.LEFT_ALIGNMENT);
		button.setPreferredSize(new Dimension(90, 20));
		button.setMaximumSize(new Dimension(90, 20));
		button.addActionListener(e -> {
			plugin.markEventAsSeen(event.getEventId());
			EventDetailsDialog.show(panel, event);
		});
		panel.add(button);
	}

	private void addSignupButton(JPanel panel, Event event, boolean isActive)
	{
		// For active events, the event hasn't "passed" - it's still ongoing
		boolean eventPassed = !isActive && hasEventPassed(event);
		boolean isSignedUp = plugin.isSignedUp(event.getEventId());
		boolean canSignUp = plugin.getPlayerName() != null && plugin.isInClan() && !eventPassed;

		panel.add(Box.createVerticalStrut(6));

		JButton button = isSignedUp
			? createLeaveButton(event)
			: createSignUpButton(event, canSignUp, eventPassed);

		button.setAlignmentX(Component.LEFT_ALIGNMENT);
		button.setForeground(Color.WHITE);
		button.setFont(new Font("Arial", Font.BOLD, 11));
		button.setPreferredSize(new Dimension(100, 24));
		button.setMaximumSize(new Dimension(100, 24));
		panel.add(button);
	}

	private boolean hasEventPassed(Event event)
	{
		if (event.getStartTime() == null)
		{
			return false;
		}
		try
		{
			LocalDateTime startTime = LocalDateTime.parse(event.getStartTime());
			return startTime.isBefore(LocalDateTime.now());
		}
		catch (Exception e)
		{
			log.warn("Could not parse event time: {}", event.getStartTime());
			return false;
		}
	}

	private JButton createLeaveButton(Event event)
	{
		JButton button = new JButton("Leave Event");
		button.setBackground(PanelColors.LEAVE_BUTTON);
		button.addActionListener(e -> plugin.unSignUp(event.getEventId()));
		return button;
	}

	private JButton createSignUpButton(Event event, boolean canSignUp, boolean eventPassed)
	{
		JButton button = new JButton("Sign Up");
		button.setBackground(PanelColors.SIGNUP_BUTTON);
		button.addActionListener(e -> plugin.signUp(event.getEventId()));

		if (!canSignUp)
		{
			button.setEnabled(false);
			button.setToolTipText(eventPassed ? "Event time has passed" : "Join Arceuus clan to sign up");
		}
		return button;
	}

	/**
	 * Helper to compute event status colors.
	 */
	private static class EventStatus
	{
		final boolean isCancelled;
		final boolean isActive;
		final boolean isUpcoming;
		final boolean isUnseen;
		final Color bgColor;
		final Color borderColor;
		final Color titleColor;

		EventStatus(Event event, ArceuusCCPlugin plugin)
		{
			isCancelled = "CANCELLED".equals(event.getStatus());
			isActive = "ACTIVE".equals(event.getStatus());
			isUpcoming = "UPCOMING".equals(event.getStatus());
			boolean isCompleted = "COMPLETED".equals(event.getStatus());
			isUnseen = isUpcoming && !plugin.isEventSeen(event.getEventId());

			if (isCancelled)
			{
				bgColor = PanelColors.CANCELLED_BG;
				borderColor = PanelColors.CANCELLED_BORDER;
				titleColor = Color.GRAY;
			}
			else if (isActive)
			{
				bgColor = PanelColors.ACTIVE_BG;
				borderColor = PanelColors.ACTIVE_BORDER;
				titleColor = PanelColors.ACTIVE_TITLE;
			}
			else if (isCompleted)
			{
				bgColor = PanelColors.COMPLETED_BG;
				borderColor = PanelColors.COMPLETED_BORDER;
				titleColor = Color.GRAY;
			}
			else if (isUnseen)
			{
				bgColor = PanelColors.UNSEEN_BG;
				borderColor = PanelColors.UNSEEN_BORDER;
				titleColor = PanelColors.UPCOMING_TITLE;
			}
			else
			{
				bgColor = PanelColors.getDarkerGray();
				borderColor = PanelColors.getMediumGray();
				titleColor = PanelColors.UPCOMING_TITLE;
			}
		}
	}
}
