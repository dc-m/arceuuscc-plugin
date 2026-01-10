package com.arceuuscc.plugin.ui;

import com.arceuuscc.plugin.ArceuusCCPlugin;
import com.arceuuscc.plugin.models.Event;
import com.arceuuscc.plugin.util.DateTimeUtils;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Panel containing the events list.
 */
public class EventsTabPanel extends JPanel
{
	private final ArceuusCCPlugin plugin;
	private final EventPanelBuilder eventBuilder;

	public EventsTabPanel(ArceuusCCPlugin plugin)
	{
		this.plugin = plugin;
		this.eventBuilder = new EventPanelBuilder(plugin);

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(PanelColors.getDarkGray());
		setBorder(new EmptyBorder(10, 10, 10, 10));
	}

	public void refresh()
	{
		removeAll();

		if (!plugin.hasPluginAccess())
		{
			add(new NoAccessPanel(plugin.getPluginSettings().getClanName()));
			revalidate();
			repaint();
			return;
		}

		List<Event> events = plugin.getEvents();

		if (events == null || events.isEmpty())
		{
			addNoEventsLabel();
		}
		else
		{
			renderEventsList(events);
		}

		revalidate();
		repaint();
	}

	private void addNoEventsLabel()
	{
		JLabel label = new JLabel("No events");
		label.setForeground(PanelColors.getLightGray());
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		add(label);
	}

	private void renderEventsList(List<Event> events)
	{
		if (plugin.hasUnseenEvents())
		{
			JButton markAllReadBtn = new JButton("Mark all as read");
			markAllReadBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
			markAllReadBtn.addActionListener(e -> plugin.markAllEventsAsRead());
			add(markAllReadBtn);
			add(Box.createVerticalStrut(10));
		}

		List<Event> live = new ArrayList<>();
		List<Event> upcoming = new ArrayList<>();
		List<Event> finished = new ArrayList<>();
		categorizeEvents(events, live, upcoming, finished);

		upcoming.sort(Comparator.comparing(e -> DateTimeUtils.parseDateTime(e.getStartTime())));
		finished.sort((a, b) -> DateTimeUtils.parseDateTime(b.getStartTime())
			.compareTo(DateTimeUtils.parseDateTime(a.getStartTime())));

		renderSection("LIVE", PanelColors.ACTIVE_TITLE, live);
		renderSection("UPCOMING", PanelColors.UPCOMING_TITLE, upcoming);
		renderSection("FINISHED", Color.GRAY, finished);

		if (live.isEmpty() && upcoming.isEmpty() && finished.isEmpty())
		{
			addNoEventsLabel();
		}
	}

	private void categorizeEvents(List<Event> events, List<Event> live, List<Event> upcoming, List<Event> finished)
	{
		for (Event event : events)
		{
			String status = event.getStatus();
			if ("ACTIVE".equals(status))
			{
				live.add(event);
			}
			else if ("UPCOMING".equals(status))
			{
				upcoming.add(event);
			}
			else if ("COMPLETED".equals(status) || "CANCELLED".equals(status))
			{
				finished.add(event);
			}
		}
	}

	private void renderSection(String title, Color color, List<Event> events)
	{
		if (events.isEmpty())
		{
			return;
		}

		addSectionHeader(title, color);
		for (Event event : events)
		{
			add(eventBuilder.createEventPanel(event));
			add(Box.createVerticalStrut(8));
		}
		add(Box.createVerticalStrut(10));
	}

	private void addSectionHeader(String title, Color color)
	{
		JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		header.setBackground(PanelColors.getDarkGray());
		header.setAlignmentX(Component.LEFT_ALIGNMENT);
		header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

		JLabel label = new JLabel(title);
		label.setFont(new Font("Arial", Font.BOLD, 12));
		label.setForeground(color);
		header.add(label);

		add(header);
		add(Box.createVerticalStrut(5));
	}
}
