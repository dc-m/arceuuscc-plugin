package com.arceuuscc.plugin.ui;

import com.arceuuscc.plugin.ArceuusCCPlugin;
import com.arceuuscc.plugin.models.Newsletter;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 * Builds newsletter panel UI components.
 */
@Slf4j
public class NewsletterPanelBuilder
{
	private static final String FONT_ARIAL = "Arial";
	private static final String FONT_SERIF = "Serif";

	private final ArceuusCCPlugin plugin;

	public NewsletterPanelBuilder(ArceuusCCPlugin plugin)
	{
		this.plugin = plugin;
	}

	/**
	 * Creates a panel for displaying a newsletter.
	 */
	public JPanel createNewsletterPanel(Newsletter newsletter, boolean isLatest)
	{
		boolean isUnseen = !plugin.isNewsletterSeen(newsletter.getId());
		Color bgColor = isLatest ? PanelColors.NEWSLETTER_LATEST_BG : PanelColors.getDarkerGray();
		Color borderColor = isLatest ? PanelColors.NEWSLETTER_LATEST_BORDER : PanelColors.getMediumGray();

		// Wrapper panel with BorderLayout for mail icon positioning
		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(bgColor);
		wrapper.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(borderColor, isLatest ? 2 : 1),
			new EmptyBorder(10, 10, 10, 10)
		));
		wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
		wrapper.setMaximumSize(new Dimension(225, 160));

		// Mail icon in top-right corner
		JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		topBar.setOpaque(false);
		JLabel mailIcon = new JLabel(isUnseen ? MailIcons.UNREAD : MailIcons.READ);
		mailIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
		mailIcon.setForeground(isUnseen ? PanelColors.GOLD : PanelColors.getLightGray());
		mailIcon.setToolTipText(isUnseen ? "Unread" : "Read");
		topBar.add(mailIcon);
		wrapper.add(topBar, BorderLayout.NORTH);

		// Content panel
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setOpaque(false);

		// Title
		JLabel titleLabel = new JLabel(newsletter.getTitle());
		titleLabel.setFont(new Font(FONT_SERIF, Font.BOLD, 14));
		titleLabel.setForeground(isLatest ? PanelColors.GOLD : Color.WHITE);
		titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(titleLabel);

		// Month/Year
		if (newsletter.getMonthYear() != null)
		{
			JLabel monthLabel = new JLabel(newsletter.getMonthYear());
			monthLabel.setFont(new Font(FONT_ARIAL, Font.ITALIC, 11));
			monthLabel.setForeground(PanelColors.getLightGray());
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
			subtitleLabel.setFont(new Font(FONT_ARIAL, Font.PLAIN, 10));
			subtitleLabel.setForeground(PanelColors.getLightGray());
			subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			panel.add(subtitleLabel);
		}

		// Editor
		if (newsletter.getEditorName() != null)
		{
			JLabel editorLabel = new JLabel(newsletter.getEditorName());
			editorLabel.setFont(new Font(FONT_ARIAL, Font.PLAIN, 9));
			editorLabel.setForeground(Color.GRAY);
			editorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			panel.add(editorLabel);
		}

		// Page count indicator
		if (newsletter.getPageCount() > 1)
		{
			JLabel pagesLabel = new JLabel(newsletter.getPageCount() + " pages");
			pagesLabel.setFont(new Font(FONT_ARIAL, Font.PLAIN, 9));
			pagesLabel.setForeground(PanelColors.GOLD);
			pagesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			panel.add(pagesLabel);
		}

		panel.add(Box.createVerticalStrut(8));

		// View button
		JButton viewButton = new JButton("View Newsletter");
		viewButton.setFont(new Font(FONT_ARIAL, Font.BOLD, 11));
		viewButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		viewButton.setBackground(isLatest ? PanelColors.NEWSLETTER_LATEST_BORDER : PanelColors.getMediumGray());
		viewButton.setForeground(Color.WHITE);
		viewButton.setPreferredSize(new Dimension(130, 24));
		viewButton.setMaximumSize(new Dimension(130, 24));
		viewButton.addActionListener(e -> showNewsletterDialog(newsletter));
		panel.add(viewButton);

		wrapper.add(panel, BorderLayout.CENTER);

		// Mark as read when clicked
		wrapper.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				plugin.markNewsletterAsSeen(newsletter);
			}
		});
		wrapper.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		return wrapper;
	}

	/**
	 * Shows a dialog with the full newsletter image and page navigation.
	 */
	public void showNewsletterDialog(Newsletter newsletter)
	{
		// Mark as seen
		plugin.markNewsletterAsSeen(newsletter);

		// Get screen size to cap dialog dimensions
		java.awt.Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
			.getMaximumWindowBounds();
		int maxDialogWidth = Math.min(1200, screenBounds.width - 100);
		int maxDialogHeight = Math.min(900, screenBounds.height - 100);

		// Create dialog
		JDialog dialog = new JDialog();
		dialog.setTitle(newsletter.getTitle() + " - " + newsletter.getMonthYear());
		dialog.setModal(true);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.setLayout(new BorderLayout());

		// Image content area
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBackground(PanelColors.DIALOG_BG);
		contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

		JLabel loadingLabel = new JLabel("Loading newsletter...");
		loadingLabel.setFont(new Font(FONT_ARIAL, Font.PLAIN, 14));
		loadingLabel.setForeground(Color.WHITE);
		loadingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		contentPanel.add(loadingLabel);

		// Scroll pane
		JScrollPane scrollPane = new JScrollPane(contentPanel);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setBorder(null);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
		dialog.add(scrollPane, BorderLayout.CENTER);

		int totalPages = Math.max(1, newsletter.getPageCount());
		final int[] currentPage = {1};

		// Navigation bar (only shown if multiple pages)
		if (totalPages > 1)
		{
			JPanel navBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
			navBar.setBackground(PanelColors.DIALOG_BG);

			JButton prevButton = new JButton("< Prev");
			JLabel pageLabel = new JLabel("Page 1 of " + totalPages);
			JButton nextButton = new JButton("Next >");

			prevButton.setEnabled(false);
			pageLabel.setForeground(Color.WHITE);
			pageLabel.setFont(new Font(FONT_ARIAL, Font.BOLD, 12));

			for (JButton btn : new JButton[]{prevButton, nextButton})
			{
				btn.setFont(new Font(FONT_ARIAL, Font.BOLD, 11));
				btn.setBackground(PanelColors.getMediumGray());
				btn.setForeground(Color.WHITE);
				btn.setPreferredSize(new Dimension(80, 28));
			}

			final int pages = totalPages;

			prevButton.addActionListener(e -> {
				if (currentPage[0] > 1)
				{
					currentPage[0]--;
					pageLabel.setText("Page " + currentPage[0] + " of " + pages);
					prevButton.setEnabled(currentPage[0] > 1);
					nextButton.setEnabled(currentPage[0] < pages);
					loadPage(newsletter.getId(), currentPage[0], contentPanel, scrollPane,
						prevButton, nextButton);
				}
			});

			nextButton.addActionListener(e -> {
				if (currentPage[0] < pages)
				{
					currentPage[0]++;
					pageLabel.setText("Page " + currentPage[0] + " of " + pages);
					prevButton.setEnabled(currentPage[0] > 1);
					nextButton.setEnabled(currentPage[0] < pages);
					loadPage(newsletter.getId(), currentPage[0], contentPanel, scrollPane,
						prevButton, nextButton);
				}
			});

			navBar.add(prevButton);
			navBar.add(pageLabel);
			navBar.add(nextButton);
			dialog.add(navBar, BorderLayout.SOUTH);
		}

		dialog.setSize(Math.min(900, maxDialogWidth), Math.min(800, maxDialogHeight));
		dialog.setLocationRelativeTo(null);

		// Load page 1
		loadPage(newsletter.getId(), 1, contentPanel, scrollPane, null, null);

		dialog.setVisible(true);
	}

	private void loadPage(int newsletterId, int page, JPanel contentPanel, JScrollPane scrollPane,
		JButton prevButton, JButton nextButton)
	{
		// Disable nav buttons while loading
		if (prevButton != null)
		{
			prevButton.setEnabled(false);
		}
		if (nextButton != null)
		{
			nextButton.setEnabled(false);
		}

		javax.swing.SwingUtilities.invokeLater(() -> {
			contentPanel.removeAll();
			JLabel loading = new JLabel("Loading page " + page + "...");
			loading.setFont(new Font(FONT_ARIAL, Font.PLAIN, 14));
			loading.setForeground(Color.WHITE);
			loading.setAlignmentX(Component.CENTER_ALIGNMENT);
			contentPanel.add(loading);
			contentPanel.revalidate();
			contentPanel.repaint();
		});

		String imageUrl = plugin.getNewsletterImageUrl(newsletterId, page);
		if (imageUrl == null)
		{
			return;
		}

		new Thread(() -> {
			try
			{
				BufferedImage image = ImageIO.read(new URL(imageUrl));
				if (image != null)
				{
					ImageIcon icon = new ImageIcon(image);
					javax.swing.SwingUtilities.invokeLater(() -> {
						contentPanel.removeAll();
						JLabel imageLabel = new JLabel(icon);
						imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
						contentPanel.add(imageLabel);
						contentPanel.revalidate();
						contentPanel.repaint();
						// Scroll to top on page change
						scrollPane.getVerticalScrollBar().setValue(0);
						// Re-enable nav buttons
						if (prevButton != null)
						{
							prevButton.setEnabled(page > 1);
						}
						if (nextButton != null)
						{
							nextButton.setEnabled(true);
						}
					});
				}
			}
			catch (Exception ex)
			{
				log.error("Failed to load newsletter page {} for newsletter {}", page, newsletterId, ex);
				javax.swing.SwingUtilities.invokeLater(() -> {
					contentPanel.removeAll();
					JLabel error = new JLabel("Failed to load page " + page);
					error.setFont(new Font(FONT_ARIAL, Font.PLAIN, 14));
					error.setForeground(Color.RED);
					error.setAlignmentX(Component.CENTER_ALIGNMENT);
					contentPanel.add(error);
					contentPanel.revalidate();
					contentPanel.repaint();
					// Re-enable nav buttons so user can try another page
					if (prevButton != null)
					{
						prevButton.setEnabled(page > 1);
					}
					if (nextButton != null)
					{
						nextButton.setEnabled(true);
					}
				});
			}
		}).start();
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
}
