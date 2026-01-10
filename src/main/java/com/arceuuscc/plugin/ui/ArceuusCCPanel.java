package com.arceuuscc.plugin.ui;

import com.arceuuscc.plugin.ArceuusCCPlugin;
import com.arceuuscc.plugin.models.Event;
import com.arceuuscc.plugin.models.Newsletter;
import com.arceuuscc.plugin.util.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URL;
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
	private final JPanel newslettersContainer;
	private final JButton refreshButton;
	private final JTabbedPane tabbedPane;

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

		refreshButton = new JButton("Refresh");
		refreshButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		refreshButton.addActionListener(e -> {
			plugin.refreshEvents();
			plugin.refreshNewsletters();
		});
		headerPanel.add(refreshButton);

		add(headerPanel, BorderLayout.NORTH);

		// Create tabbed pane for Events and Newsletters
		tabbedPane = new JTabbedPane();
		tabbedPane.setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Events tab
		eventsContainer = new JPanel();
		eventsContainer.setLayout(new BoxLayout(eventsContainer, BoxLayout.Y_AXIS));
		eventsContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		eventsContainer.setBorder(new EmptyBorder(10, 10, 10, 10));

		JScrollPane eventsScrollPane = new JScrollPane(eventsContainer);
		eventsScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		eventsScrollPane.setBorder(null);
		eventsScrollPane.getVerticalScrollBar().setUnitIncrement(16);

		tabbedPane.addTab("Events", eventsScrollPane);

		// Newsletters tab
		newslettersContainer = new JPanel();
		newslettersContainer.setLayout(new BoxLayout(newslettersContainer, BoxLayout.Y_AXIS));
		newslettersContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		newslettersContainer.setBorder(new EmptyBorder(10, 10, 10, 10));

		JScrollPane newslettersScrollPane = new JScrollPane(newslettersContainer);
		newslettersScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		newslettersScrollPane.setBorder(null);
		newslettersScrollPane.getVerticalScrollBar().setUnitIncrement(16);

		tabbedPane.addTab("Newsletters", newslettersScrollPane);

		add(tabbedPane, BorderLayout.CENTER);

		updateEvents();
		updateNewsletters();
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
				// Refresh tabs when clan status is confirmed
				updateEvents();
				updateNewsletters();
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

		// Check if user has plugin access
		if (!plugin.hasPluginAccess())
		{
			renderNoAccessPanel(eventsContainer);
			eventsContainer.revalidate();
			eventsContainer.repaint();
			return;
		}

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
			// Add "Mark all as read" button if there are unseen events
			if (plugin.hasUnseenEvents())
			{
				JButton markAllReadBtn = new JButton("Mark all as read");
				markAllReadBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
				markAllReadBtn.addActionListener(e -> plugin.markAllEventsAsRead());
				eventsContainer.add(markAllReadBtn);
				eventsContainer.add(Box.createVerticalStrut(10));
			}

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

		// Update tab title if there are unseen events
		if (plugin.hasUnseenEvents())
		{
			tabbedPane.setTitleAt(0, "Events *");
			tabbedPane.setForegroundAt(0, new Color(218, 165, 32));
		}
		else
		{
			tabbedPane.setTitleAt(0, "Events");
			tabbedPane.setForegroundAt(0, null);
		}
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
		boolean isUnseen = isUpcoming && !plugin.isEventSeen(event.getEventId());

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
		else if (isUnseen)
		{
			// Highlight unseen events with a golden border
			bgColor = new Color(50, 45, 30);
			borderColor = new Color(218, 165, 32);
		}
		else
		{
			bgColor = ColorScheme.DARKER_GRAY_COLOR;
			borderColor = ColorScheme.MEDIUM_GRAY_COLOR;
		}

		panel.setBackground(bgColor);
		panel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(borderColor, isUnseen ? 2 : 1),
			new EmptyBorder(8, 8, 8, 8)
		));
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.setMaximumSize(new Dimension(225, Integer.MAX_VALUE));

		// Show "NEW" badge for unseen events
		if (isUnseen)
		{
			JLabel newBadge = new JLabel("NEW");
			newBadge.setFont(new Font("Arial", Font.BOLD, 9));
			newBadge.setForeground(new Color(218, 165, 32));
			newBadge.setAlignmentX(Component.LEFT_ALIGNMENT);
			panel.add(newBadge);
			panel.add(Box.createVerticalStrut(2));
		}

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
		}

		// Always show View Details button - it marks the event as seen
		panel.add(Box.createVerticalStrut(4));
		JButton detailsButton = new JButton("View Details");
		detailsButton.setFont(new Font("Arial", Font.PLAIN, 10));
		detailsButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		detailsButton.setPreferredSize(new Dimension(90, 20));
		detailsButton.setMaximumSize(new Dimension(90, 20));
		detailsButton.addActionListener(e -> {
			plugin.markEventAsSeen(event.getEventId());
			showEventDetailsDialog(event);
		});
		panel.add(detailsButton);

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

	// ==================== NEWSLETTER METHODS ====================

	public void updateNewsletters()
	{
		newslettersContainer.removeAll();

		// Check if user has plugin access
		if (!plugin.hasPluginAccess())
		{
			renderNoAccessPanel(newslettersContainer);
			newslettersContainer.revalidate();
			newslettersContainer.repaint();
			return;
		}

		List<Newsletter> newsletters = plugin.getNewsletters();
		Newsletter latest = plugin.getLatestNewsletter();

		if ((newsletters == null || newsletters.isEmpty()) && latest == null)
		{
			JLabel noNewslettersLabel = new JLabel("No newsletters available");
			noNewslettersLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			noNewslettersLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			newslettersContainer.add(noNewslettersLabel);
		}
		else
		{
			// Always show "Mark all as read" button if there are any newsletters
			JButton markAllReadBtn = new JButton("Mark all as read");
			markAllReadBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
			markAllReadBtn.addActionListener(e -> plugin.markAllNewslettersAsRead());
			newslettersContainer.add(markAllReadBtn);
			newslettersContainer.add(Box.createVerticalStrut(10));

			// Show latest newsletter prominently
			if (latest != null)
			{
				addSectionHeaderToContainer(newslettersContainer, "LATEST NEWSLETTER", new Color(139, 69, 19));
				newslettersContainer.add(createNewsletterPanel(latest, true));
				newslettersContainer.add(Box.createVerticalStrut(15));
			}

			// Show newsletter history
			if (newsletters != null && !newsletters.isEmpty())
			{
				addSectionHeaderToContainer(newslettersContainer, "NEWSLETTER HISTORY", Color.GRAY);

				for (Newsletter newsletter : newsletters)
				{
					// Skip if it's the same as latest
					if (latest != null && newsletter.getId() == latest.getId())
					{
						continue;
					}
					newslettersContainer.add(createNewsletterPanel(newsletter, false));
					newslettersContainer.add(Box.createVerticalStrut(8));
				}
			}

			// Add refresh button at bottom
			newslettersContainer.add(Box.createVerticalStrut(10));
			JButton refreshNewslettersBtn = new JButton("Refresh");
			refreshNewslettersBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
			refreshNewslettersBtn.addActionListener(e -> plugin.refreshNewsletters());
			newslettersContainer.add(refreshNewslettersBtn);
		}

		newslettersContainer.revalidate();
		newslettersContainer.repaint();

		// Update tab title if there's an unread newsletter
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

	private void addSectionHeaderToContainer(JPanel container, String title, Color color)
	{
		JPanel sectionHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		sectionHeaderPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		sectionHeaderPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		sectionHeaderPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

		JLabel label = new JLabel(title);
		label.setFont(new Font("Arial", Font.BOLD, 12));
		label.setForeground(color);
		sectionHeaderPanel.add(label);

		container.add(sectionHeaderPanel);
		container.add(Box.createVerticalStrut(5));
	}

	private JPanel createNewsletterPanel(Newsletter newsletter, boolean isLatest)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		Color bgColor = isLatest ? new Color(60, 50, 30) : ColorScheme.DARKER_GRAY_COLOR;
		Color borderColor = isLatest ? new Color(139, 69, 19) : ColorScheme.MEDIUM_GRAY_COLOR;

		panel.setBackground(bgColor);
		panel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(borderColor, isLatest ? 2 : 1),
			new EmptyBorder(10, 10, 10, 10)
		));
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		// Limit height so panel doesn't expand to fill available space
		panel.setMaximumSize(new Dimension(225, 160));

		// Title
		JLabel titleLabel = new JLabel(newsletter.getTitle());
		titleLabel.setFont(new Font("Serif", Font.BOLD, 14));
		titleLabel.setForeground(isLatest ? new Color(218, 165, 32) : Color.WHITE);
		titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(titleLabel);

		// Month/Year
		if (newsletter.getMonthYear() != null)
		{
			JLabel monthLabel = new JLabel(newsletter.getMonthYear());
			monthLabel.setFont(new Font("Arial", Font.ITALIC, 11));
			monthLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			monthLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			panel.add(monthLabel);
		}

		panel.add(Box.createVerticalStrut(5));

		// Subtitle
		if (newsletter.getSubtitle() != null && !newsletter.getSubtitle().isEmpty())
		{
			String subtitle = newsletter.getSubtitle();
			if (subtitle.length() > 50)
			{
				subtitle = subtitle.substring(0, 47) + "...";
			}
			JLabel subtitleLabel = new JLabel("<html><i>" + escapeHtml(subtitle) + "</i></html>");
			subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 10));
			subtitleLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			panel.add(subtitleLabel);
		}

		// Editor
		if (newsletter.getEditorName() != null)
		{
			JLabel editorLabel = new JLabel(newsletter.getEditorName());
			editorLabel.setFont(new Font("Arial", Font.PLAIN, 9));
			editorLabel.setForeground(Color.GRAY);
			editorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			panel.add(editorLabel);
		}

		panel.add(Box.createVerticalStrut(8));

		// View button
		JButton viewButton = new JButton("View Newsletter");
		viewButton.setFont(new Font("Arial", Font.BOLD, 11));
		viewButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		viewButton.setBackground(isLatest ? new Color(139, 69, 19) : ColorScheme.MEDIUM_GRAY_COLOR);
		viewButton.setForeground(Color.WHITE);
		viewButton.setPreferredSize(new Dimension(130, 24));
		viewButton.setMaximumSize(new Dimension(130, 24));
		viewButton.addActionListener(e -> showNewsletterDialog(newsletter));
		panel.add(viewButton);

		// Mark as read when clicked
		panel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				plugin.markNewsletterAsSeen(newsletter);
				updateNewsletters();
			}
		});
		panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		return panel;
	}

	private void showNewsletterDialog(Newsletter newsletter)
	{
		// Mark as seen
		plugin.markNewsletterAsSeen(newsletter);
		updateNewsletters();

		// Get screen size to cap dialog dimensions for small screens
		java.awt.Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
			.getMaximumWindowBounds();
		int maxDialogWidth = Math.min(1200, screenBounds.width - 100);
		int maxDialogHeight = Math.min(900, screenBounds.height - 100);

		// Create a large dialog
		JDialog dialog = new JDialog();
		dialog.setTitle(newsletter.getTitle() + " - " + newsletter.getMonthYear());
		dialog.setModal(true);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBackground(new Color(30, 30, 30));
		contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

		// Loading label
		JLabel loadingLabel = new JLabel("Loading newsletter...");
		loadingLabel.setFont(new Font("Arial", Font.PLAIN, 14));
		loadingLabel.setForeground(Color.WHITE);
		loadingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		contentPanel.add(loadingLabel);

		// Scroll pane for the content - always show scrollbars when needed
		JScrollPane scrollPane = new JScrollPane(contentPanel);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setBorder(null);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

		dialog.add(scrollPane);
		dialog.setSize(Math.min(900, maxDialogWidth), Math.min(800, maxDialogHeight));
		dialog.setLocationRelativeTo(null);

		// Load image in background
		String imageUrl = plugin.getNewsletterImageUrl(newsletter.getId());
		if (imageUrl != null)
		{
			new Thread(() -> {
				try
				{
					BufferedImage image = ImageIO.read(new URL(imageUrl));
					if (image != null)
					{
						// Use full image size, no scaling
						ImageIcon icon = new ImageIcon(image);

						javax.swing.SwingUtilities.invokeLater(() -> {
							contentPanel.removeAll();

							JLabel imageLabel = new JLabel(icon);
							imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
							contentPanel.add(imageLabel);

							contentPanel.revalidate();
							contentPanel.repaint();

							// Resize dialog to fit image, capped by screen size
							int dialogWidth = Math.min(image.getWidth() + 60, maxDialogWidth);
							int dialogHeight = Math.min(image.getHeight() + 80, maxDialogHeight);
							dialog.setSize(dialogWidth, dialogHeight);
							dialog.setLocationRelativeTo(null);
						});
					}
				}
				catch (Exception ex)
				{
					log.error("Failed to load newsletter image", ex);
					javax.swing.SwingUtilities.invokeLater(() -> {
						loadingLabel.setText("Failed to load newsletter image");
						loadingLabel.setForeground(Color.RED);
					});
				}
			}).start();
		}

		dialog.setVisible(true);
	}

	// ==================== NO ACCESS MESSAGE ====================

	private void renderNoAccessPanel(JPanel container)
	{
		JPanel noAccessPanel = new JPanel();
		noAccessPanel.setLayout(new BoxLayout(noAccessPanel, BoxLayout.Y_AXIS));
		noAccessPanel.setBackground(new Color(60, 40, 30));
		noAccessPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(139, 69, 19), 2),
			new EmptyBorder(10, 10, 10, 10)
		));
		noAccessPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		noAccessPanel.setMaximumSize(new Dimension(PluginPanel.PANEL_WIDTH - 10, Integer.MAX_VALUE));

		// Title
		JLabel titleLabel = new JLabel("<html><div style='width:170px'>Clan Membership Required</div></html>");
		titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
		titleLabel.setForeground(new Color(255, 140, 0));
		titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		noAccessPanel.add(titleLabel);

		noAccessPanel.add(Box.createVerticalStrut(8));

		// Message
		String clanName = plugin.getPluginSettings().getClanName();
		JLabel messageLabel = new JLabel("<html><div style='width:150px'>To use the Arceuus CC Plugin, please join the \"" +
			clanName + "\" Clan Chat.</div></html>");
		messageLabel.setFont(new Font("Arial", Font.PLAIN, 11));
		messageLabel.setForeground(Color.WHITE);
		messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		noAccessPanel.add(messageLabel);

		noAccessPanel.add(Box.createVerticalStrut(15));

		// Discord section
		JLabel discordLabel = new JLabel("Join us on Discord:");
		discordLabel.setFont(new Font("Arial", Font.BOLD, 11));
		discordLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		discordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		noAccessPanel.add(discordLabel);

		noAccessPanel.add(Box.createVerticalStrut(5));

		// Discord link
		JLabel linkLabel = new JLabel("<html><u>discord.gg/Ka3bVn6nkW</u></html>");
		linkLabel.setFont(new Font("Arial", Font.PLAIN, 11));
		linkLabel.setForeground(new Color(114, 137, 218));
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
		noAccessPanel.add(linkLabel);

		container.add(noAccessPanel);
	}
}
