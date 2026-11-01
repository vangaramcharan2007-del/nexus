package nexus.media;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import nexus.common.EventBus;
import nexus.common.NexusEvent;
import nexus.common.NexusListener;

public class MediaPanel extends JPanel implements NexusListener {
    private final MediaService service;
    private final ConcurrentHashMap<String, ImageIcon> posterCache = new ConcurrentHashMap<>();

    // Seat Allocation State
    private final Map<String, CinemaSeat> seatMap = new LinkedHashMap<>();
    private final Map<String, JButton> seatButtons = new HashMap<>();
    private final List<String> selectedSeatCodes = new ArrayList<>();

    // Booking Summary Components
    private JLabel selectedSeatsLabel;
    private JLabel subtotalLabel;
    private JLabel discountLabel;
    private JLabel totalPayableLabel;
    private JTextField promoCodeField;
    private double currentDiscount = 0.0;
    private String currentPromoCode = "";

    // Cinema Form Pickers
    private JComboBox<String> movieCombo;
    private JComboBox<String> theatreCombo;
    private JComboBox<String> showtimeCombo;

    // Tabs
    private JTabbedPane mainTabs;
    private JPanel bookingsGrid;
    private JPanel watchlistGrid;
    private JPanel liveSearchGrid;
    private JTextField searchField;

    public MediaPanel(MediaService service) {
        this.service = service;
        setLayout(new BorderLayout());
        setBackground(new Color(15, 23, 42));

        mainTabs = new JTabbedPane();
        mainTabs.setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Tab 1: BookMyShow Cinema Box Office
        mainTabs.addTab("🎟️ Cinema Box Office & Seat Booking", createBoxOfficePanel());

        // Tab 2: My Bookings
        mainTabs.addTab("🎫 My Movie Passes (Active Bookings)", createMyBookingsPanel());

        // Tab 3: IMDb Explorer & Watchlist
        mainTabs.addTab("🎬 IMDb Online Explorer & Watchlist", createWatchlistAndSearchPanel());

        add(mainTabs, BorderLayout.CENTER);

        // EventBus Subscriptions
        EventBus.getInstance().subscribe("BOOKINGS_UPDATED", this);
        EventBus.getInstance().subscribe("MEDIA_UPDATED", this);
        EventBus.getInstance().subscribe("MEDIA_SEARCH_RESULTS", this);

        renderBookings(service.getBookings());
        renderWatchlist(service.getMediaItems());
    }

    // --- TAB 1: BOOKMYSHOW BOX OFFICE ---
    private JPanel createBoxOfficePanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 10));
        panel.setBackground(new Color(15, 23, 42));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Top Cinema Selector
        JPanel selectorBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        selectorBar.setBackground(new Color(30, 41, 59));
        selectorBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        String[] movies = {
                "Dune: Part Two (IMAX 3D)",
                "Interstellar (10th Anniv Re-Release)",
                "Avengers: Secret Wars (Atmos 7.1)",
                "Deadpool & Wolverine (4DX)",
                "Oppenheimer (70mm IMAX)"
        };
        movieCombo = new JComboBox<>(movies);

        String[] theatres = {
                "PVR Grand Galada, Chennai (Audi 1 - IMAX 3D)",
                "INOX National, Chennai (Laser Atmos)",
                "SPI Cinemas Palazzo, Vadapalani (Screen 4)",
                "AGS Cinemas, OMR Navalur (4DX)",
                "SRM Kattankulathur Campus Multiplex"
        };
        theatreCombo = new JComboBox<>(theatres);

        String[] showtimes = {
                "Today • 11:00 AM (Morning Special)",
                "Today • 02:30 PM (Matinee)",
                "Today • 06:45 PM (Prime IMAX 3D)",
                "Today • 10:15 PM (Night Laser)"
        };
        showtimeCombo = new JComboBox<>(showtimes);

        selectorBar.add(new JLabel("Movie:"));
        selectorBar.add(movieCombo);
        selectorBar.add(new JLabel("Cinema:"));
        selectorBar.add(theatreCombo);
        selectorBar.add(new JLabel("Show:"));
        selectorBar.add(showtimeCombo);

        panel.add(selectorBar, BorderLayout.NORTH);

        // Center Split: Left = Seat Matrix | Right = Checkout & Promo
        JPanel centerGrid = new JPanel(new BorderLayout(15, 0));
        centerGrid.setOpaque(false);

        // SEAT MATRIX PANEL
        JPanel seatArea = new JPanel(new BorderLayout(10, 10));
        seatArea.setBackground(new Color(24, 30, 42));
        seatArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85)),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));

        // Screen Graphic
        JLabel screenLabel = new JLabel("--- 🎬 CINEMA SCREEN THIS WAY (ALL EYES FRONT) 🎬 ---", SwingConstants.CENTER);
        screenLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        screenLabel.setForeground(new Color(56, 189, 248));
        screenLabel.setOpaque(true);
        screenLabel.setBackground(new Color(15, 23, 42));
        screenLabel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        seatArea.add(screenLabel, BorderLayout.NORTH);

        // 60-Seat Interactive Grid (6 rows × 10 cols)
        JPanel grid = new JPanel(new GridLayout(6, 10, 6, 6));
        grid.setOpaque(false);
        initSeatMatrix(grid);
        seatArea.add(grid, BorderLayout.CENTER);

        // Seat Legend
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 4));
        legend.setOpaque(false);
        legend.add(createLegendChip("Available", new Color(51, 65, 85)));
        legend.add(createLegendChip("Selected", new Color(16, 185, 129)));
        legend.add(createLegendChip("Booked", new Color(220, 38, 38)));
        legend.add(new JLabel("• VIP (A-B): $350 | Club (C-D): $250 | Exec (E-F): $150"));
        seatArea.add(legend, BorderLayout.SOUTH);

        centerGrid.add(seatArea, BorderLayout.CENTER);

        // RIGHT CHECKOUT & PROMO PANEL
        JPanel checkoutPanel = createCheckoutPanel();
        checkoutPanel.setPreferredSize(new Dimension(320, 400));
        centerGrid.add(checkoutPanel, BorderLayout.EAST);

        panel.add(centerGrid, BorderLayout.CENTER);
        return panel;
    }

    private void initSeatMatrix(JPanel grid) {
        seatMap.clear();
        seatButtons.clear();
        selectedSeatCodes.clear();

        char[] rows = {'A', 'B', 'C', 'D', 'E', 'F'};
        Random rnd = new Random(42); // Deterministic booked seats for demo

        for (char row : rows) {
            String tier;
            double price;
            if (row == 'A' || row == 'B') {
                tier = "VIP Recliner";
                price = 350.0;
            } else if (row == 'C' || row == 'D') {
                tier = "Club Premium";
                price = 250.0;
            } else {
                tier = "Executive";
                price = 150.0;
            }

            for (int col = 1; col <= 10; col++) {
                String code = "" + row + col;
                CinemaSeat seat = new CinemaSeat(code, tier, price);
                if (rnd.nextDouble() < 0.22) { // 22% pre-booked
                    seat.setBooked(true);
                }
                seatMap.put(code, seat);

                JButton btn = new JButton(code);
                btn.setFont(new Font("Consolas", Font.BOLD, 10));
                btn.setFocusPainted(false);
                btn.setMargin(new Insets(2, 2, 2, 2));

                if (seat.isBooked()) {
                    btn.setBackground(new Color(220, 38, 38));
                    btn.setForeground(Color.WHITE);
                    btn.setEnabled(false);
                    btn.setToolTipText("Occupied / Sold Out");
                } else {
                    btn.setBackground(new Color(51, 65, 85));
                    btn.setForeground(Color.WHITE);
                    btn.setToolTipText(tier + " - $" + price);
                    btn.addActionListener(e -> toggleSeat(code, btn));
                }

                seatButtons.put(code, btn);
                grid.add(btn);
            }
        }
    }

    private void toggleSeat(String code, JButton btn) {
        CinemaSeat seat = seatMap.get(code);
        if (seat == null || seat.isBooked()) return;

        if (selectedSeatCodes.contains(code)) {
            selectedSeatCodes.remove(code);
            seat.setSelected(false);
            btn.setBackground(new Color(51, 65, 85));
        } else {
            selectedSeatCodes.add(code);
            seat.setSelected(true);
            btn.setBackground(new Color(16, 185, 129)); // Selected Green
        }

        updatePricingSummary();
    }

    private void updatePricingSummary() {
        if (selectedSeatCodes.isEmpty()) {
            selectedSeatsLabel.setText("None Selected");
            subtotalLabel.setText("$0.00");
            discountLabel.setText("-$0.00");
            totalPayableLabel.setText("$0.00");
            currentDiscount = 0.0;
            return;
        }

        double subtotal = 0;
        for (String code : selectedSeatCodes) {
            CinemaSeat seat = seatMap.get(code);
            if (seat != null) {
                subtotal += seat.getPrice();
            }
        }

        selectedSeatsLabel.setText(String.join(", ", selectedSeatCodes));
        subtotalLabel.setText(String.format("$%,.2f", subtotal));

        // Apply discount if any
        currentDiscount = service.calculateDiscount(subtotal, currentPromoCode);
        discountLabel.setText(String.format("-$%,.2f (%s)", currentDiscount, currentPromoCode.isEmpty() ? "None" : currentPromoCode));

        double payable = Math.max(0, subtotal - currentDiscount);
        totalPayableLabel.setText(String.format("$%,.2f", payable));
    }

    private JPanel createCheckoutPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(30, 41, 59));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85)),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));

        JLabel title = new JLabel("BOOKING & PROMO SUMMARY");
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setForeground(new Color(56, 189, 248));
        panel.add(title, BorderLayout.NORTH);

        // Center Details
        JPanel details = new JPanel(new GridLayout(6, 1, 4, 4));
        details.setOpaque(false);

        selectedSeatsLabel = new JLabel("None Selected");
        selectedSeatsLabel.setFont(new Font("Consolas", Font.BOLD, 12));
        selectedSeatsLabel.setForeground(new Color(52, 211, 153));

        subtotalLabel = new JLabel("$0.00");
        subtotalLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        subtotalLabel.setForeground(Color.WHITE);

        discountLabel = new JLabel("-$0.00 (None)");
        discountLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        discountLabel.setForeground(new Color(248, 113, 113));

        totalPayableLabel = new JLabel("$0.00");
        totalPayableLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        totalPayableLabel.setForeground(new Color(52, 211, 153));

        details.add(createRow("Allocated Seats:", selectedSeatsLabel));
        details.add(createRow("Base Subtotal:", subtotalLabel));
        details.add(createRow("Applied Discount:", discountLabel));
        details.add(createRow("Total Amount:", totalPayableLabel));

        // Promo Code Row
        JPanel promoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        promoRow.setOpaque(false);
        promoCodeField = new JTextField(8);
        promoCodeField.setFont(new Font("Consolas", Font.BOLD, 11));
        promoCodeField.setText("SRMSTUDENT");

        JButton applyPromoBtn = new JButton("Apply");
        applyPromoBtn.setFont(new Font("Segoe UI", Font.BOLD, 10));
        applyPromoBtn.setBackground(new Color(59, 130, 246));
        applyPromoBtn.setForeground(Color.WHITE);
        applyPromoBtn.addActionListener(e -> {
            currentPromoCode = promoCodeField.getText().trim();
            updatePricingSummary();
            JOptionPane.showMessageDialog(this, "Promo Code '" + currentPromoCode + "' validated! Discount applied.", 
                    "Coupon Applied", JOptionPane.INFORMATION_MESSAGE);
        });

        promoRow.add(new JLabel("Code:"));
        promoRow.add(promoCodeField);
        promoRow.add(applyPromoBtn);
        details.add(promoRow);

        JLabel hints = new JLabel("Coupons: SRMSTUDENT (25%) | NEXUS50 ($50 off)");
        hints.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        hints.setForeground(new Color(156, 163, 175));
        details.add(hints);

        panel.add(details, BorderLayout.CENTER);

        // Bottom Confirm Button
        JButton bookBtn = new JButton("🎟️ Confirm & Generate Pass");
        bookBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        bookBtn.setBackground(new Color(16, 185, 129));
        bookBtn.setForeground(Color.WHITE);
        bookBtn.setPreferredSize(new Dimension(280, 42));
        bookBtn.setFocusPainted(false);
        bookBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        bookBtn.addActionListener(e -> confirmBooking());
        panel.add(bookBtn, BorderLayout.SOUTH);

        return panel;
    }

    private void confirmBooking() {
        if (selectedSeatCodes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select at least one seat from the matrix.", "No Seats Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String movie = (String) movieCombo.getSelectedItem();
        String theatre = (String) theatreCombo.getSelectedItem();
        String show = (String) showtimeCombo.getSelectedItem();

        double subtotal = 0;
        for (String code : selectedSeatCodes) {
            CinemaSeat seat = seatMap.get(code);
            if (seat != null) subtotal += seat.getPrice();
        }
        double payable = Math.max(0, subtotal - currentDiscount);

        CinemaBooking booking = service.createBooking(movie, theatre, show, new ArrayList<>(selectedSeatCodes), 
                subtotal, currentDiscount, currentPromoCode, payable);

        // Mark seats booked in local matrix
        for (String code : selectedSeatCodes) {
            CinemaSeat seat = seatMap.get(code);
            if (seat != null) seat.setBooked(true);
            JButton btn = seatButtons.get(code);
            if (btn != null) {
                btn.setBackground(new Color(220, 38, 38));
                btn.setEnabled(false);
            }
        }
        selectedSeatCodes.clear();
        updatePricingSummary();

        // Show Digital Boarding Ticket Pass Modal
        showDigitalTicketDialog(booking);

        mainTabs.setSelectedIndex(1); // Switch to My Bookings tab
    }

    private void showDigitalTicketDialog(CinemaBooking b) {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Digital Cinema Pass - Confirmed", true);
        dlg.setSize(440, 380);
        dlg.setLocationRelativeTo(this);

        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(new Color(24, 30, 42));
        p.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel header = new JLabel("🎟️ NEXUS CINEMA BOARDING PASS", SwingConstants.CENTER);
        header.setFont(new Font("Segoe UI", Font.BOLD, 16));
        header.setForeground(new Color(56, 189, 248));
        p.add(header, BorderLayout.NORTH);

        JTextArea info = new JTextArea();
        info.setFont(new Font("Consolas", Font.PLAIN, 12));
        info.setEditable(false);
        info.setBackground(new Color(15, 23, 42));
        info.setForeground(Color.WHITE);
        info.setText(String.format("""
                ========================================
                BOOKING REF : %s
                STATUS      : CONFIRMED (PAID)
                ========================================
                MOVIE       : %s
                THEATRE     : %s
                SHOWTIME    : %s
                SEATS       : %s
                ----------------------------------------
                SUBTOTAL    : $%.2f
                DISCOUNT    : -$%.2f (%s)
                TOTAL PAID  : $%.2f
                ========================================
                [|||  ||||| ||  ||||||||| | ||||| |||  ]
                SCAN QR/BARCODE AT CINEMA ADMISSION GATE
                """,
                b.getId(), b.getMovieTitle(), b.getTheatre(), b.getShowTime(),
                b.getSeatsFormatted(), b.getBasePrice(), b.getDiscount(), b.getPromoCode(), b.getFinalTotal()));

        p.add(new JScrollPane(info), BorderLayout.CENTER);

        JButton close = new JButton("Dismiss Pass");
        close.addActionListener(e -> dlg.dispose());
        p.add(close, BorderLayout.SOUTH);

        dlg.setContentPane(p);
        dlg.setVisible(true);
    }

    private JPanel createRow(String labelText, JLabel valLabel) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        JLabel l = new JLabel(labelText);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        l.setForeground(new Color(203, 213, 225));
        row.add(l, BorderLayout.WEST);
        row.add(valLabel, BorderLayout.EAST);
        return row;
    }

    private JLabel createLegendChip(String text, Color bg) {
        JLabel l = new JLabel(" " + text + " ");
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setOpaque(true);
        l.setBackground(bg);
        l.setForeground(Color.WHITE);
        return l;
    }

    // --- TAB 2: MY BOOKINGS ---
    private JPanel createMyBookingsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(15, 23, 42));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("🎫 MY ACTIVE CINEMA PASSES & RESERVATIONS");
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setForeground(new Color(56, 189, 248));
        panel.add(title, BorderLayout.NORTH);

        bookingsGrid = new JPanel();
        bookingsGrid.setLayout(new BoxLayout(bookingsGrid, BoxLayout.Y_AXIS));
        bookingsGrid.setBackground(new Color(15, 23, 42));

        JScrollPane scroll = new JScrollPane(bookingsGrid);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private void renderBookings(List<CinemaBooking> bookings) {
        bookingsGrid.removeAll();
        if (bookings == null || bookings.isEmpty()) {
            JLabel empty = new JLabel("No cinema tickets booked yet. Use the Box Office tab to reserve seats!");
            empty.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            empty.setForeground(new Color(156, 163, 175));
            empty.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
            bookingsGrid.add(empty);
        } else {
            for (CinemaBooking b : bookings) {
                bookingsGrid.add(createBookingCard(b));
                bookingsGrid.add(Box.createVerticalStrut(10));
            }
        }
        bookingsGrid.revalidate();
        bookingsGrid.repaint();
    }

    private JPanel createBookingCard(CinemaBooking b) {
        JPanel card = new JPanel(new BorderLayout(15, 6));
        card.setBackground(new Color(30, 41, 59));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, new Color(16, 185, 129)),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        card.setMaximumSize(new Dimension(2000, 95));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel ref = new JLabel(b.getId() + " • " + b.getStatus());
        ref.setFont(new Font("Consolas", Font.BOLD, 12));
        ref.setForeground(new Color(52, 211, 153));

        JLabel title = new JLabel(b.getMovieTitle());
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(Color.WHITE);

        JLabel venue = new JLabel(b.getTheatre() + "  |  " + b.getShowTime());
        venue.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        venue.setForeground(new Color(147, 197, 253));

        left.add(ref);
        left.add(Box.createVerticalStrut(2));
        left.add(title);
        left.add(Box.createVerticalStrut(2));
        left.add(venue);

        card.add(left, BorderLayout.CENTER);

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setOpaque(false);

        JLabel seats = new JLabel("Seats: " + b.getSeatsFormatted());
        seats.setFont(new Font("Consolas", Font.BOLD, 13));
        seats.setForeground(Color.YELLOW);

        JLabel price = new JLabel(String.format("Paid: $%,.2f", b.getFinalTotal()));
        price.setFont(new Font("Segoe UI", Font.BOLD, 14));
        price.setForeground(Color.WHITE);

        JButton viewPassBtn = new JButton("View Ticket");
        viewPassBtn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        viewPassBtn.setBackground(new Color(59, 130, 246));
        viewPassBtn.setForeground(Color.WHITE);
        viewPassBtn.addActionListener(e -> showDigitalTicketDialog(b));

        right.add(seats);
        right.add(price);
        right.add(Box.createVerticalStrut(4));
        right.add(viewPassBtn);

        card.add(right, BorderLayout.EAST);
        return card;
    }

    // --- TAB 3: IMDB WATCHLIST & DISCOVERY ---
    private JPanel createWatchlistAndSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(15, 23, 42));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Search Bar
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        searchBar.setBackground(new Color(30, 41, 59));
        searchBar.setBorder(BorderFactory.createLineBorder(new Color(51, 65, 85)));

        searchField = new JTextField("Batman", 18);
        JButton searchBtn = new JButton("🔍 Search IMDb / TVMaze");
        searchBtn.setBackground(new Color(59, 130, 246));
        searchBtn.setForeground(Color.WHITE);
        searchBtn.addActionListener(e -> {
            String q = searchField.getText().trim();
            if (!q.isEmpty()) service.searchOnline(q);
        });

        searchBar.add(new JLabel("IMDb Search:"));
        searchBar.add(searchField);
        searchBar.add(searchBtn);
        panel.add(searchBar, BorderLayout.NORTH);

        // Center split: Watchlist (top) and Search Results (bottom)
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.5);
        split.setOpaque(false);
        split.setBorder(null);

        // Watchlist
        watchlistGrid = new JPanel();
        watchlistGrid.setLayout(new BoxLayout(watchlistGrid, BoxLayout.Y_AXIS));
        watchlistGrid.setBackground(new Color(20, 24, 33));
        JScrollPane ws = new JScrollPane(watchlistGrid);
        ws.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(51, 65, 85)), "Personal Watchlist", 0, 0, new Font("Segoe UI", Font.BOLD, 12), Color.CYAN));
        split.setTopComponent(ws);

        // Search Results
        liveSearchGrid = new JPanel();
        liveSearchGrid.setLayout(new BoxLayout(liveSearchGrid, BoxLayout.Y_AXIS));
        liveSearchGrid.setBackground(new Color(20, 24, 33));
        JScrollPane ss = new JScrollPane(liveSearchGrid);
        ss.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(51, 65, 85)), "Live Online Search Results", 0, 0, new Font("Segoe UI", Font.BOLD, 12), Color.GREEN));
        split.setBottomComponent(ss);

        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    private void renderWatchlist(List<MediaItem> items) {
        watchlistGrid.removeAll();
        for (MediaItem item : items) {
            JPanel card = new JPanel(new BorderLayout(10, 0));
            card.setBackground(new Color(30, 41, 59));
            card.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            card.setMaximumSize(new Dimension(2000, 45));

            JLabel title = new JLabel(item.getTitle() + " (" + item.getType() + ") - " + item.getStatus() + " ★" + item.getRating());
            title.setForeground(Color.WHITE);
            title.setFont(new Font("Segoe UI", Font.BOLD, 12));

            JButton del = new JButton("🗑️");
            del.addActionListener(e -> service.deleteMedia(item.getId()));

            card.add(title, BorderLayout.CENTER);
            card.add(del, BorderLayout.EAST);

            watchlistGrid.add(card);
            watchlistGrid.add(Box.createVerticalStrut(4));
        }
        watchlistGrid.revalidate();
        watchlistGrid.repaint();
    }

    private void renderLiveSearchResults(List<MediaItem> items) {
        liveSearchGrid.removeAll();
        for (MediaItem item : items) {
            JPanel card = new JPanel(new BorderLayout(10, 0));
            card.setBackground(new Color(30, 41, 59));
            card.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            card.setMaximumSize(new Dimension(2000, 55));

            JLabel title = new JLabel(item.getTitle() + " (" + item.getYear() + ") ⭐ " + item.getRatingScore() + "/10 - " + item.getGenres());
            title.setForeground(Color.WHITE);
            title.setFont(new Font("Segoe UI", Font.BOLD, 12));

            JButton addWatchlistBtn = new JButton("+ Watchlist");
            addWatchlistBtn.setBackground(new Color(16, 185, 129));
            addWatchlistBtn.setForeground(Color.WHITE);
            addWatchlistBtn.addActionListener(e -> {
                service.addMediaItem(item);
                JOptionPane.showMessageDialog(this, "Added '" + item.getTitle() + "' to watchlist!", "Watchlist", JOptionPane.INFORMATION_MESSAGE);
            });

            card.add(title, BorderLayout.CENTER);
            card.add(addWatchlistBtn, BorderLayout.EAST);

            liveSearchGrid.add(card);
            liveSearchGrid.add(Box.createVerticalStrut(4));
        }
        liveSearchGrid.revalidate();
        liveSearchGrid.repaint();
    }

    @Override
    public void onEvent(NexusEvent event) {
        SwingUtilities.invokeLater(() -> {
            if ("BOOKINGS_UPDATED".equals(event.getType())) {
                @SuppressWarnings("unchecked")
                List<CinemaBooking> updated = (List<CinemaBooking>) event.getPayload();
                renderBookings(updated);
            } else if ("MEDIA_UPDATED".equals(event.getType())) {
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
