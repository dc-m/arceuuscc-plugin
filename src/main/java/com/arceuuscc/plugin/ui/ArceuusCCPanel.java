package com.arceuuscc.plugin.ui;

import com.arceuuscc.plugin.ArceuusCCPlugin;
import com.arceuuscc.plugin.models.Event;
import com.arceuuscc.plugin.util.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class ArceuusCCPanel extends PluginPanel
{
	private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("EEE, MMM d 'at' HH:mm");
	private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
	private static final String EMOJI_FONT_NAME = getEmojiFontName();

	private final ArceuusCCPlugin plugin;
	private final JPanel headerPanel;
	private final JLabel connectionStatus;
	private final JLabel playerInfoLabel;
	private final JLabel clanStatusLabel;
	private final JPanel eventsContainer;
	private final JButton refreshButton;

	private static String getEmojiFontName()
	{
		String[] emojiFonts = {"Segoe UI Emoji", "Apple Color Emoji", "Noto Color Emoji", "Segoe UI Symbol", "Arial"};
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		String[] availableFonts = ge.getAvailableFontFamilyNames();

		for (String emojiFont : emojiFonts)
		{
			for (String available : availableFonts)
			{
				if (available.equalsIgnoreCase(emojiFont))
				{
					return emojiFont;
				}
			}
		}
		return "Arial";
	}

	public ArceuusCCPanel(ArceuusCCPlugin plugin)
	{
		super(false);
		this.plugin = plugin;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		headerPanel = new JPanel();
		headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
		headerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

		JLabel titleLabel = new JLabel("Arceuus CC Events");
		titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
		titleLabel.setForeground(Color.WHITE);
		titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		headerPanel.add(titleLabel);

		headerPanel.add(Box.createVerticalStrut(5));

		connectionStatus = new JLabel("Connecting...");
		connectionStatus.setForeground(Color.ORANGE);
		connectionStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
		headerPanel.add(connectionStatus);

		headerPanel.add(Box.createVerticalStrut(5));

		playerInfoLabel = new JLabel("");
		playerInfoLabel.setForeground(Color.WHITE);
		playerInfoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		playerInfoLabel.setVisible(false);
		headerPanel.add(playerInfoLabel);

		clanStatusLabel = new JLabel("");
		clanStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		headerPanel.add(clanStatusLabel);

		headerPanel.add(Box.createVerticalStrut(10));

		refreshButton = new JButton("Refresh Events");
		refreshButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		refreshButton.addActionListener(e -> plugin.refreshEvents());
		headerPanel.add(refreshButton);

		add(headerPanel, BorderLayout.NORTH);

		eventsContainer = new JPanel();
		eventsContainer.setLayout(new BoxLayout(eventsContainer, BoxLayout.Y_AXIS));
		eventsContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		eventsContainer.setBorder(new EmptyBorder(10, 10, 10, 10));

		JScrollPane scrollPane = new JScrollPane(eventsContainer);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(null);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);

		add(scrollPane, BorderLayout.CENTER);

		updateEvents();
	}

	public void updateConnectionStatus(boolean connected)
	{
		if (connected)
		{
			connectionStatus.setText("Connected");
			connectionStatus.setForeground(new Color(0, 200, 83));
			refreshButton.setEnabled(true);
		}
		else
		{
			connectionStatus.setText("Disconnected");
			connectionStatus.setForeground(Color.RED);
			refreshButton.setEnabled(false);
		}
	}

	public void updatePlayerInfo()
	{
		String playerName = plugin.getPlayerName();
		boolean inClan = plugin.isInClan();

		if (playerName != null)
		{
			playerInfoLabel.setText("Player: " + playerName);
			playerInfoLabel.setVisible(true);

			if (inClan)
			{
				clanStatusLabel.setText("In Arceuus clan");
				clanStatusLabel.setForeground(new Color(0, 200, 83));
			}
			else
			{
				clanStatusLabel.setText("Not in Arceuus clan");
				clanStatusLabel.setForeground(Color.ORANGE);
			}
			clanStatusLabel.setVisible(true);
		}
		else
		{
			playerInfoLabel.setVisible(false);
			clanStatusLabel.setVisible(false);
		}
	}

	public void updateEvents()
	{
		eventsContainer.removeAll();

		List<Event> events = plugin.getEvents();

		if (events == null || events.isEmpty())
		{
			JLabel noEventsLabel = new JLabel("No events");
			noEventsLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			noEventsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			eventsContainer.add(noEventsLabel);
		}
		else
		{
			List<Event> liveEvents = new ArrayList<>();
			List<Event> upcomingEvents = new ArrayList<>();
			List<Event> finishedEvents = new ArrayList<>();

			for (Event event : events)
			{
				String status = event.getStatus();
				if ("ACTIVE".equals(status))
				{
					liveEvents.add(event);
				}
				else if ("UPCOMING".equals(status))
				{
					upcomingEvents.add(event);
				}
				else if ("COMPLETED".equals(status) || "CANCELLED".equals(status))
				{
					finishedEvents.add(event);
				}
			}

			upcomingEvents.sort(Comparator.comparing(e -> parseDateTime(e.getStartTime())));
			finishedEvents.sort((a, b) -> parseDateTime(b.getStartTime()).compareTo(parseDateTime(a.getStartTime())));

			if (!liveEvents.isEmpty())
			{
				addSectionHeader("LIVE", new Color(50, 200, 50));
				for (Event event : liveEvents)
				{
					eventsContainer.add(createEventPanel(event));
					eventsContainer.add(Box.createVerticalStrut(8));
				}
				eventsContainer.add(Box.createVerticalStrut(10));
			}

			if (!upcomingEvents.isEmpty())
			{
				addSectionHeader("UPCOMING", new Color(114, 137, 218));
				for (Event event : upcomingEvents)
				{
					eventsContainer.add(createEventPanel(event));
					eventsContainer.add(Box.createVerticalStrut(8));
				}
				eventsContainer.add(Box.createVerticalStrut(10));
			}

			if (!finishedEvents.isEmpty())
			{
				addSectionHeader("FINISHED", Color.GRAY);
				for (Event event : finishedEvents)
				{
					eventsContainer.add(createEventPanel(event));
					eventsContainer.add(Box.createVerticalStrut(8));
				}
			}

			if (liveEvents.isEmpty() && upcomingEvents.isEmpty() && finishedEvents.isEmpty())
			{
				JLabel noEventsLabel = new JLabel("No events");
				noEventsLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
				noEventsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
				eventsContainer.add(noEventsLabel);
			}
		}

		eventsContainer.revalidate();
		eventsContainer.repaint();
	}

	private void addSectionHeader(String title, Color color)
	{
		JPanel sectionHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		sectionHeaderPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		sectionHeaderPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		sectionHeaderPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

		JLabel label = new JLabel(title);
		label.setFont(new Font("Arial", Font.BOLD, 12));
		label.setForeground(color);
		sectionHeaderPanel.add(label);

		eventsContainer.add(sectionHeaderPanel);
		eventsContainer.add(Box.createVerticalStrut(5));
	}

	private LocalDateTime parseDateTime(String isoTime)
	{
		return DateTimeUtils.parseDateTime(isoTime);
	}

	private JPanel createEventPanel(Event event)
	{
		boolean isCancelled = "CANCELLED".equals(event.getStatus());
		boolean isActive = "ACTIVE".equals(event.getStatus());
		boolean isUpcoming = "UPCOMING".equals(event.getStatus());
		boolean isCompleted = "COMPLETED".equals(event.getStatus());

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		Color bgColor;
		Color borderColor;
		if (isCancelled)
		{
			bgColor = new Color(60, 40, 40);
			borderColor = new Color(150, 50, 50);
		}
		else if (isActive)
		{
			bgColor = new Color(30, 50, 30);
			borderColor = new Color(50, 150, 50);
		}
		else if (isCompleted)
		{
			bgColor = new Color(45, 45, 45);
			borderColor = new Color(80, 80, 80);
		}
		else
		{
			bgColor = ColorScheme.DARKER_GRAY_COLOR;
			borderColor = ColorScheme.MEDIUM_GRAY_COLOR;
		}

		panel.setBackground(bgColor);
		panel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(borderColor, 1),
			new EmptyBorder(8, 8, 8, 8)
		));
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.setMaximumSize(new Dimension(225, Integer.MAX_VALUE));

		String titleText;
		Color titleColor;
		if (isCancelled)
		{
			titleText = "<html><s>" + event.getTitle() + "</s></html>";
			titleColor = Color.GRAY;
		}
		else if (isCompleted)
		{
			titleText = event.getTitle();
			titleColor = Color.GRAY;
		}
		else if (isActive)
		{
			titleText = event.getTitle();
			titleColor = new Color(50, 200, 50);
		}
		else
		{
			titleText = event.getTitle();
			titleColor = new Color(114, 137, 218);
		}
		JLabel titleLabel = new JLabel(titleText);
		titleLabel.setFont(new Font("Arial", Font.BOLD, 13));
		titleLabel.setForeground(titleColor);
		titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(titleLabel);

		panel.add(Box.createVerticalStrut(4));

		String timeDisplay = formatEventTime(event.getStartTime());
		JLabel timeLabel = new JLabel(timeDisplay + " UTC");
		timeLabel.setFont(new Font("Arial", Font.PLAIN, 11));
		timeLabel.setForeground(Color.WHITE);
		timeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(timeLabel);

		JLabel durationLabel = new JLabel("Duration: " + formatDuration(event.getDurationMinutes()));
		durationLabel.setFont(new Font("Arial", Font.PLAIN, 10));
		durationLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		durationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(durationLabel);

		panel.add(Box.createVerticalStrut(4));

		int signupCount = event.getSignups() != null ? event.getSignups().size() : 0;
		JLabel signupsLabel = new JLabel("Signups: " + signupCount);
		signupsLabel.setFont(new Font("Arial", Font.PLAIN, 10));
		signupsLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		signupsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(signupsLabel);

		String description = event.getDescription();
		if (description != null && !description.isEmpty())
		{
			panel.add(Box.createVerticalStrut(4));

			String preview = description.length() > 60
				? description.substring(0, 57) + "..."
				: description;
			JLabel descPreview = new JLabel("<html><i>" + escapeHtml(preview) + "</i></html>");
			descPreview.setFont(new Font(EMOJI_FONT_NAME, Font.PLAIN, 10));
			descPreview.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			descPreview.setAlignmentX(Component.LEFT_ALIGNMENT);
			panel.add(descPreview);

			if (description.length() > 60 || description.contains("\n"))
			{
				panel.add(Box.createVerticalStrut(4));
				JButton detailsButton = new JButton("Show Details");
				detailsButton.setFont(new Font("Arial", Font.PLAIN, 10));
				detailsButton.setAlignmentX(Component.LEFT_ALIGNMENT);
				detailsButton.setPreferredSize(new Dimension(90, 20));
				detailsButton.setMaximumSize(new Dimension(90, 20));
				detailsButton.addActionListener(e -> showEventDetailsDialog(event));
				panel.add(detailsButton);
			}
		}

		if (isUpcoming)
		{
			boolean eventPassed = false;
			if (event.getStartTime() != null)
			{
				try
				{
					LocalDateTime startTime = LocalDateTime.parse(event.getStartTime());
					eventPassed = startTime.isBefore(LocalDateTime.now());
				}
				catch (Exception e)
				{
					log.warn("Could not parse event time: {}", event.getStartTime());
				}
			}

			panel.add(Box.createVerticalStrut(6));

			boolean isSignedUp = plugin.isSignedUp(event.getEventId());
			boolean canSignUp = plugin.getPlayerName() != null && plugin.isInClan() && !eventPassed;

			JButton actionButton;
			if (isSignedUp)
			{
				actionButton = new JButton("Leave Event");
				actionButton.setBackground(new Color(150, 50, 50));
				actionButton.addActionListener(e ->
				{
					log.info("Leave button clicked for event: {}", event.getEventId());
					plugin.unSignUp(event.getEventId());
				});
			}
			else
			{
				actionButton = new JButton("Sign Up");
				actionButton.setBackground(new Color(50, 150, 50));
				actionButton.addActionListener(e ->
				{
					log.info("Sign up button clicked for event: {}", event.getEventId());
					plugin.signUp(event.getEventId());
				});

				if (!canSignUp)
				{
					actionButton.setEnabled(false);
					if (eventPassed)
					{
						actionButton.setToolTipText("Event time has passed");
					}
					else
					{
						actionButton.setToolTipText("Join Arceuus clan to sign up");
					}
				}
			}
			actionButton.setAlignmentX(Component.LEFT_ALIGNMENT);
			actionButton.setForeground(Color.WHITE);
			actionButton.setFont(new Font("Arial", Font.BOLD, 11));
			actionButton.setPreferredSize(new Dimension(100, 24));
			actionButton.setMaximumSize(new Dimension(100, 24));
			panel.add(actionButton);
		}

		return panel;
	}

	private String formatEventTime(String isoTime)
	{
		if (isoTime == null)
		{
			return "Unknown";
		}
		try
		{
			LocalDateTime dateTime = LocalDateTime.parse(isoTime, INPUT_FORMATTER);
			return dateTime.format(DISPLAY_FORMATTER);
		}
		catch (DateTimeParseException e)
		{
			return isoTime;
		}
	}

	private String formatDuration(int minutes)
	{
		if (minutes == 0)
		{
			return "Open-ended";
		}
		if (minutes < 60)
		{
			return minutes + " min";
		}
		int hours = minutes / 60;
		int mins = minutes % 60;
		if (mins == 0)
		{
			return hours + "h";
		}
		return hours + "h " + mins + "m";
	}

	private String escapeHtml(String text)
	{
		if (text == null)
		{
			return "";
		}
		return text.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;")
			.replace("\n", "<br>");
	}

	private void showEventDetailsDialog(Event event)
	{
		JPanel dialogPanel = new JPanel();
		dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.Y_AXIS));
		dialogPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		dialogPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

		JLabel titleLabel = new JLabel(event.getTitle());
		titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
		titleLabel.setForeground(new Color(114, 137, 218));
		titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		dialogPanel.add(titleLabel);

		dialogPanel.add(Box.createVerticalStrut(10));

		JLabel timeLabel = new JLabel("When: " + formatEventTime(event.getStartTime()) + " UTC");
		timeLabel.setFont(new Font("Arial", Font.PLAIN, 12));
		timeLabel.setForeground(Color.WHITE);
		timeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		dialogPanel.add(timeLabel);

		JLabel durationLabel = new JLabel("Duration: " + formatDuration(event.getDurationMinutes()));
		durationLabel.setFont(new Font("Arial", Font.PLAIN, 12));
		durationLabel.setForeground(Color.WHITE);
		durationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		dialogPanel.add(durationLabel);

		JLabel statusLabel = new JLabel("Status: " + event.getStatus());
		statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
		statusLabel.setForeground(Color.WHITE);
		statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		dialogPanel.add(statusLabel);

		int signupCount = event.getSignups() != null ? event.getSignups().size() : 0;
		JLabel signupsLabel = new JLabel("Signups: " + signupCount);
		signupsLabel.setFont(new Font("Arial", Font.PLAIN, 12));
		signupsLabel.setForeground(Color.WHITE);
		signupsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		dialogPanel.add(signupsLabel);

		dialogPanel.add(Box.createVerticalStrut(15));

		JLabel descHeader = new JLabel("Description:");
		descHeader.setFont(new Font("Arial", Font.BOLD, 12));
		descHeader.setForeground(Color.WHITE);
		descHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
		dialogPanel.add(descHeader);

		dialogPanel.add(Box.createVerticalStrut(5));

		JTextArea descArea = new JTextArea(event.getDescription());
		descArea.setFont(new Font(EMOJI_FONT_NAME, Font.PLAIN, 12));
		descArea.setForeground(Color.WHITE);
		descArea.setBackground(ColorScheme.DARK_GRAY_COLOR);
		descArea.setEditable(false);
		descArea.setLineWrap(true);
		descArea.setWrapStyleWord(true);
		descArea.setBorder(new EmptyBorder(8, 8, 8, 8));

		JScrollPane descScroll = new JScrollPane(descArea);
		descScroll.setPreferredSize(new Dimension(350, 150));
		descScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
		descScroll.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
		dialogPanel.add(descScroll);

		if (event.getSignups() != null && !event.getSignups().isEmpty())
		{
			dialogPanel.add(Box.createVerticalStrut(15));

			JLabel signupsHeader = new JLabel("Signed Up Players:");
			signupsHeader.setFont(new Font("Arial", Font.BOLD, 12));
			signupsHeader.setForeground(Color.WHITE);
			signupsHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
			dialogPanel.add(signupsHeader);

			dialogPanel.add(Box.createVerticalStrut(5));

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
			signupsArea.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			signupsArea.setBackground(ColorScheme.DARK_GRAY_COLOR);
			signupsArea.setEditable(false);
			signupsArea.setLineWrap(true);
			signupsArea.setWrapStyleWord(true);
			signupsArea.setBorder(new EmptyBorder(8, 8, 8, 8));

			JScrollPane signupsScroll = new JScrollPane(signupsArea);
			signupsScroll.setPreferredSize(new Dimension(350, 60));
			signupsScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
			signupsScroll.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
			dialogPanel.add(signupsScroll);
		}

		JOptionPane.showMessageDialog(
			this,
			dialogPanel,
			"Event Details",
			JOptionPane.PLAIN_MESSAGE
		);
	}
}
