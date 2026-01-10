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
	 * Shows a dialog with the full newsletter image.
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

		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBackground(PanelColors.DIALOG_BG);
		contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

		// Loading label
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

		dialog.add(scrollPane);
		dialog.setSize(Math.min(900, maxDialogWidth), Math.min(800, maxDialogHeight));
		dialog.setLocationRelativeTo(null);

		// Load image in background
		String imageUrl = plugin.getNewsletterImageUrl(newsletter.getId());
		if (imageUrl != null)
		{
			loadNewsletterImage(imageUrl, contentPanel, loadingLabel, dialog, maxDialogWidth, maxDialogHeight);
		}

		dialog.setVisible(true);
	}

	private void loadNewsletterImage(String imageUrl, JPanel contentPanel, JLabel loadingLabel,
		JDialog dialog, int maxDialogWidth, int maxDialogHeight)
	{
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

						// Resize dialog to fit image
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
