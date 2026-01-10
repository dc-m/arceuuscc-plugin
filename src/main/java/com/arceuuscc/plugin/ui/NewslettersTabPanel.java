package com.arceuuscc.plugin.ui;

import com.arceuuscc.plugin.ArceuusCCPlugin;
import com.arceuuscc.plugin.models.Newsletter;

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
import java.util.List;

/**
 * Panel containing the newsletters list.
 */
public class NewslettersTabPanel extends JPanel
{
	private final ArceuusCCPlugin plugin;
	private final NewsletterPanelBuilder newsletterBuilder;

	public NewslettersTabPanel(ArceuusCCPlugin plugin)
	{
		this.plugin = plugin;
		this.newsletterBuilder = new NewsletterPanelBuilder(plugin);

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

		List<Newsletter> newsletters = plugin.getNewsletters();
		Newsletter latest = plugin.getLatestNewsletter();

		if ((newsletters == null || newsletters.isEmpty()) && latest == null)
		{
			JLabel label = new JLabel("No newsletters available");
			label.setForeground(PanelColors.getLightGray());
			label.setAlignmentX(Component.LEFT_ALIGNMENT);
			add(label);
		}
		else
		{
			renderNewslettersList(newsletters, latest);
		}

		revalidate();
		repaint();
	}

	private void renderNewslettersList(List<Newsletter> newsletters, Newsletter latest)
	{
		JButton markAllReadBtn = new JButton("Mark all as read");
		markAllReadBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
		markAllReadBtn.addActionListener(e -> plugin.markAllNewslettersAsRead());
		add(markAllReadBtn);
		add(Box.createVerticalStrut(10));

		if (latest != null)
		{
			addSectionHeader("LATEST NEWSLETTER", PanelColors.NEWSLETTER_LATEST_BORDER);
			add(newsletterBuilder.createNewsletterPanel(latest, true));
			add(Box.createVerticalStrut(15));
		}

		if (newsletters != null && !newsletters.isEmpty())
		{
			addSectionHeader("NEWSLETTER HISTORY", Color.GRAY);

			for (Newsletter newsletter : newsletters)
			{
				if (latest != null && newsletter.getId() == latest.getId())
				{
					continue;
				}
				add(newsletterBuilder.createNewsletterPanel(newsletter, false));
				add(Box.createVerticalStrut(8));
			}
		}

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
