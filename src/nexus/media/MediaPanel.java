package nexus.media;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import nexus.common.EventBus;
import nexus.common.NexusEvent;
import nexus.common.NexusListener;

public class MediaPanel extends JPanel implements NexusListener {
    private final MediaService service;
    private final ConcurrentHashMap<String, ImageIcon> posterCache = new ConcurrentHashMap<>();

    // UI Components
    private JTextField searchField;
    private JPanel liveSearchGrid;
    private JPanel watchlistGrid;
    private JLabel watchlistSummaryLabel;
    private JTabbedPane mainTabs;
    private JComboBox<String> statusFilterCombo;

    public MediaPanel(MediaService service) {
        this.service = service;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setBackground(new Color(20, 24, 33));

        // 1. TOP CONTROL BAR (Search + Manual Add)
        add(createTopControlBar(), BorderLayout.NORTH);

        // 2. MAIN TABS: [My Watchlist] and [Live Online Discovery]
        mainTabs = new JTabbedPane();
        mainTabs.setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Tab 1: My Watchlist
        JPanel watchlistTab = new JPanel(new BorderLayout(10, 10));
        watchlistTab.setOpaque(false);

        JPanel watchlistTopBar = new JPanel(new BorderLayout(10, 0));
        watchlistTopBar.setOpaque(false);
        watchlistTopBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        watchlistSummaryLabel = new JLabel("Watchlist: 0 items");
        watchlistSummaryLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        watchlistSummaryLabel.setForeground(new Color(147, 197, 253));

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        filterPanel.setOpaque(false);
        filterPanel.add(new JLabel("Filter Status:"));
        statusFilterCombo = new JComboBox<>(new String[]{"All", "Watching", "Plan to Watch", "Completed"});
        statusFilterCombo.addActionListener(e -> renderWatchlist(service.getMediaItems()));
        filterPanel.add(statusFilterCombo);

        watchlistTopBar.add(watchlistSummaryLabel, BorderLayout.WEST);
        watchlistTopBar.add(filterPanel, BorderLayout.EAST);
        watchlistTab.add(watchlistTopBar, BorderLayout.NORTH);

        watchlistGrid = new JPanel();
        watchlistGrid.setLayout(new BoxLayout(watchlistGrid, BoxLayout.Y_AXIS));
        watchlistGrid.setBackground(new Color(20, 24, 33));
        JScrollPane watchlistScroll = new JScrollPane(watchlistGrid);
        watchlistScroll.setBorder(BorderFactory.createEmptyBorder());
        watchlistScroll.getVerticalScrollBar().setUnitIncrement(16);
        watchlistTab.add(watchlistScroll, BorderLayout.CENTER);

        mainTabs.addTab("🎬 My Watchlist", watchlistTab);

        // Tab 2: Live Online Search Results
        JPanel liveSearchTab = new JPanel(new BorderLayout(10, 10));
        liveSearchTab.setOpaque(false);

        JLabel liveHeader = new JLabel("● REAL-TIME TV & MOVIE METADATA ENGINE (TVMAZE API)");
        liveHeader.setFont(new Font("Segoe UI", Font.BOLD, 12));
        liveHeader.setForeground(new Color(52, 211, 153));
        liveHeader.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        liveSearchTab.add(liveHeader, BorderLayout.NORTH);

        liveSearchGrid = new JPanel();
        liveSearchGrid.setLayout(new BoxLayout(liveSearchGrid, BoxLayout.Y_AXIS));
        liveSearchGrid.setBackground(new Color(20, 24, 33));
        JScrollPane liveScroll = new JScrollPane(liveSearchGrid);
        liveScroll.setBorder(BorderFactory.createEmptyBorder());
        liveScroll.getVerticalScrollBar().setUnitIncrement(16);
        liveSearchTab.add(liveScroll, BorderLayout.CENTER);

        mainTabs.addTab("🌐 Live Online Discovery", liveSearchTab);

        add(mainTabs, BorderLayout.CENTER);

        // EventBus Subscriptions
        EventBus.getInstance().subscribe("MEDIA_UPDATED", this);
        EventBus.getInstance().subscribe("MEDIA_SEARCH_RESULTS", this);

        // Initial Renders
        renderWatchlist(service.getMediaItems());
        renderLiveSearchResults(service.getSearchResults());
    }

    private JPanel createTopControlBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setBackground(new Color(30, 41, 59));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(71, 85, 105), 1),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));

        // Left: Search Field & Search Button
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchBox.setOpaque(false);

        JLabel searchIcon = new JLabel("Search Movies & TV:");
        searchIcon.setFont(new Font("Segoe UI", Font.BOLD, 12));
        searchIcon.setForeground(Color.WHITE);

        searchField = new JTextField(18);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setText("Batman");

        JButton searchBtn = new JButton("🔍 Search Online");
        searchBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        searchBtn.setBackground(new Color(59, 130, 246));
        searchBtn.setForeground(Color.WHITE);
        searchBtn.setFocusPainted(false);
        searchBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        searchBtn.addActionListener(e -> executeSearch());
        searchField.addActionListener(e -> executeSearch());

        searchBox.add(searchIcon);
        searchBox.add(searchField);
        searchBox.add(searchBtn);

        // Right: Manual Quick Add
        JButton manualAddBtn = new JButton("+ Add Custom Item");
        manualAddBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        manualAddBtn.setBackground(new Color(16, 185, 129));
        manualAddBtn.setForeground(Color.WHITE);
        manualAddBtn.setFocusPainted(false);
        manualAddBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        manualAddBtn.addActionListener(e -> openCustomAddDialog());

        bar.add(searchBox, BorderLayout.WEST);
        bar.add(manualAddBtn, BorderLayout.EAST);
        return bar;
    }

    private void executeSearch() {
        String q = searchField.getText().trim();
        if (!q.isEmpty()) {
            mainTabs.setSelectedIndex(1); // Switch to discovery tab
            liveSearchGrid.removeAll();
            JLabel loading = new JLabel("Connecting to TVMaze real-time API for '" + q + "'...");
            loading.setFont(new Font("Segoe UI", Font.ITALIC, 14));
            loading.setForeground(Color.LIGHT_GRAY);
            loading.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            liveSearchGrid.add(loading);
            liveSearchGrid.revalidate();
            liveSearchGrid.repaint();

            service.searchOnline(q);
        }
    }

    private void openCustomAddDialog() {
        JTextField titleField = new JTextField();
        JComboBox<MediaType> typeCombo = new JComboBox<>(MediaType.values());
        JTextField genreField = new JTextField("Action, Drama");

        Object[] message = {
                "Title:", titleField,
                "Media Type:", typeCombo,
                "Genre:", genreField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Add Custom Media", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String title = titleField.getText().trim();
            if (!title.isEmpty()) {
                MediaItem item = new MediaItem(title, (MediaType) typeCombo.getSelectedItem());
                item.setGenres(genreField.getText().trim());
                service.addMediaItem(item);
                mainTabs.setSelectedIndex(0);
            }
        }
    }

    private void renderWatchlist(List<MediaItem> items) {
        watchlistGrid.removeAll();

        int total = items.size();
        long watching = items.stream().filter(i -> i.getStatus() == MediaStatus.IN_PROGRESS).count();
        long completed = items.stream().filter(i -> i.getStatus() == MediaStatus.COMPLETED).count();
        watchlistSummaryLabel.setText(String.format("Watchlist: %d Total  •  %d In Progress  •  %d Completed", total, watching, completed));

        String filter = (String) statusFilterCombo.getSelectedItem();

        boolean hasItems = false;
        for (MediaItem item : items) {
            if ("Watching".equals(filter) && item.getStatus() != MediaStatus.IN_PROGRESS) continue;
            if ("Plan to Watch".equals(filter) && item.getStatus() != MediaStatus.WANT_TO_CONSUME) continue;
            if ("Completed".equals(filter) && item.getStatus() != MediaStatus.COMPLETED) continue;

            watchlistGrid.add(createWatchlistCard(item));
            watchlistGrid.add(Box.createVerticalStrut(10));
            hasItems = true;
        }

        if (!hasItems) {
            JLabel empty = new JLabel("No media items found in this filter. Use the Live Online Discovery tab to search and add!");
            empty.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            empty.setForeground(new Color(156, 163, 175));
            empty.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
            watchlistGrid.add(empty);
        }

        watchlistGrid.revalidate();
        watchlistGrid.repaint();
    }

    private JPanel createWatchlistCard(MediaItem item) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(new Color(30, 41, 59));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        card.setMaximumSize(new Dimension(2000, 100));

        // Left Poster Thumbnail
        JLabel posterLabel = new JLabel();
        posterLabel.setPreferredSize(new Dimension(50, 75));
        posterLabel.setOpaque(true);
        posterLabel.setBackground(new Color(15, 23, 42));
        posterLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadPosterAsync(item.getPosterUrl(), posterLabel, 50, 75);
        card.add(posterLabel, BorderLayout.WEST);

        // Center Info
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(item.getTitle() + (item.getYear().isEmpty() ? "" : " (" + item.getYear() + ")"));
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(Color.WHITE);

        JLabel metaLabel = new JLabel(String.format("[%s] • %s • Rating: ★ %.1f/10", item.getType(), item.getGenres(), item.getRatingScore()));
        metaLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        metaLabel.setForeground(new Color(147, 197, 253));

        JLabel summaryLabel = new JLabel(item.getSummary().isEmpty() ? "No description available." : item.getSummary());
        summaryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        summaryLabel.setForeground(new Color(209, 213, 219));

        infoPanel.add(titleLabel);
        infoPanel.add(Box.createVerticalStrut(3));
        infoPanel.add(metaLabel);
        infoPanel.add(Box.createVerticalStrut(3));
        infoPanel.add(summaryLabel);

        card.add(infoPanel, BorderLayout.CENTER);

        // Right Controls: Status Dropdown + Star Rating + Delete
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 20));
        controls.setOpaque(false);

        JComboBox<MediaStatus> statusCombo = new JComboBox<>(MediaStatus.values());
        statusCombo.setSelectedItem(item.getStatus());
        statusCombo.addActionListener(e -> {
            MediaStatus newSt = (MediaStatus) statusCombo.getSelectedItem();
            if (newSt != item.getStatus()) {
                service.updateStatus(item.getId(), newSt);
            }
        });

        // Rating Combo (1 to 5 stars)
        String[] stars = {"No Rating", "★ 1", "★★ 2", "★★★ 3", "★★★★ 4", "★★★★★ 5"};
        JComboBox<String> ratingCombo = new JComboBox<>(stars);
        ratingCombo.setSelectedIndex(Math.min(5, Math.max(0, item.getRating())));
        ratingCombo.addActionListener(e -> {
            int selected = ratingCombo.getSelectedIndex();
            service.rateMedia(item.getId(), selected);
        });

        JButton deleteBtn = new JButton("🗑️");
        deleteBtn.setToolTipText("Remove from Watchlist");
        deleteBtn.setBackground(new Color(239, 68, 68));
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setFocusPainted(false);
        deleteBtn.addActionListener(e -> service.deleteMedia(item.getId()));

        controls.add(new JLabel("Status:"));
        controls.add(statusCombo);
        controls.add(ratingCombo);
        controls.add(deleteBtn);

        card.add(controls, BorderLayout.EAST);
        return card;
    }

    private void renderLiveSearchResults(List<MediaItem> items) {
        liveSearchGrid.removeAll();
        if (items == null || items.isEmpty()) {
            JLabel empty = new JLabel("No search results yet. Type a title above and click Search Online!");
            empty.setFont(new Font("Segoe UI", Font.ITALIC, 14));
            empty.setForeground(new Color(156, 163, 175));
            empty.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            liveSearchGrid.add(empty);
        } else {
            for (MediaItem item : items) {
                liveSearchGrid.add(createLiveSearchCard(item));
                liveSearchGrid.add(Box.createVerticalStrut(10));
            }
        }
        liveSearchGrid.revalidate();
        liveSearchGrid.repaint();
    }

    private JPanel createLiveSearchCard(MediaItem item) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(new Color(30, 41, 59));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(71, 85, 105), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        card.setMaximumSize(new Dimension(2000, 110));

        // Poster
        JLabel posterLabel = new JLabel();
        posterLabel.setPreferredSize(new Dimension(55, 80));
        posterLabel.setOpaque(true);
        posterLabel.setBackground(new Color(15, 23, 42));
        posterLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadPosterAsync(item.getPosterUrl(), posterLabel, 55, 80);
        card.add(posterLabel, BorderLayout.WEST);

        // Center Info
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(item.getTitle() + " (" + item.getYear() + ")");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLabel.setForeground(Color.WHITE);

        JLabel meta = new JLabel(String.format("⭐ %.1f/10  |  Genres: %s", item.getRatingScore(), item.getGenres()));
        meta.setFont(new Font("Segoe UI", Font.BOLD, 12));
        meta.setForeground(new Color(251, 191, 36)); // Gold

        JLabel summary = new JLabel("<html><body style='width: 450px;'>" + item.getSummary() + "</body></html>");
        summary.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        summary.setForeground(new Color(209, 213, 219));

        infoPanel.add(titleLabel);
        infoPanel.add(Box.createVerticalStrut(3));
        infoPanel.add(meta);
        infoPanel.add(Box.createVerticalStrut(4));
        infoPanel.add(summary);

        card.add(infoPanel, BorderLayout.CENTER);

        // Right: "Add to Watchlist" Button
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 30));
        actionPanel.setOpaque(false);

        JButton addBtn = new JButton("+ Add to Watchlist");
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addBtn.setBackground(new Color(16, 185, 129));
        addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false);
        addBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        addBtn.addActionListener(e -> {
            service.addMediaItem(item);
            JOptionPane.showMessageDialog(this, "'" + item.getTitle() + "' added to your Watchlist!", "Added to Watchlist", JOptionPane.INFORMATION_MESSAGE);
            mainTabs.setSelectedIndex(0);
        });

        actionPanel.add(addBtn);
        card.add(actionPanel, BorderLayout.EAST);
        return card;
    }

    private void loadPosterAsync(String url, JLabel label, int width, int height) {
        if (url == null || url.isEmpty()) {
            label.setText("🎬");
            label.setForeground(Color.GRAY);
            return;
        }
        if (posterCache.containsKey(url)) {
            label.setIcon(posterCache.get(url));
            label.setText("");
            return;
        }
        label.setText("⌛");
        new Thread(() -> {
            ImageIcon icon = RealtimeMediaService.loadScaledPoster(url, width, height);
            SwingUtilities.invokeLater(() -> {
                if (icon != null) {
                    posterCache.put(url, icon);
                    label.setIcon(icon);
                    label.setText("");
                } else {
                    label.setText("🎬");
                }
            });
        }).start();
    }

    @Override
    public void onEvent(NexusEvent event) {
        SwingUtilities.invokeLater(() -> {
            if ("MEDIA_UPDATED".equals(event.getType())) {
                @SuppressWarnings("unchecked")
                List<MediaItem> updated = (List<MediaItem>) event.getPayload();
                renderWatchlist(updated);
            } else if ("MEDIA_SEARCH_RESULTS".equals(event.getType())) {
                @SuppressWarnings("unchecked")
                List<MediaItem> results = (List<MediaItem>) event.getPayload();
                renderLiveSearchResults(results);
            }
        });
    }
}
