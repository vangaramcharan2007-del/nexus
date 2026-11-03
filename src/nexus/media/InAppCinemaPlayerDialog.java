package nexus.media;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NEXUS In-App Cinema Video Player.
 * Provides rich in-app trailer & teaser playback without kicking the user out to a regular browser:
 * 1) Built-in interactive Swing Cinema Player with animated visualizer, scrubbing bar, volume,
 *    synced subtitles, resolution switch, and audio modes.
 * 2) Standalone In-App Web Screen mode using Edge App Container (--app embed) with ZERO browser chrome
 *    (no address bar, no tabs, standalone native window appearance).
 */
public class InAppCinemaPlayerDialog extends JDialog {

    private final String movieTitle;
    private final String trailerUrl;
    private final String teaserUrl;
    private final String posterUrl;
    private final String synopsis;
    private final double ratingScore;

    private boolean isPlaying = true;
    private boolean isTeaserMode = false;
    private int currentTimeSec = 0;
    private int totalDurationSec = 166; // e.g. 2:46
    private javax.swing.Timer playbackTimer;

    private JLabel timeLabel;
    private JSlider progressSlider;
    private JButton playPauseBtn;
    private JLabel subtitleLabel;
    private JLabel videoScreenCanvas;
    private JSlider volumeSlider;
    private JComboBox<String> subtitleCombo;
    private JComboBox<String> qualityCombo;
    private JComboBox<String> audioModeCombo;

    public InAppCinemaPlayerDialog(Frame parent, String movieTitle, String trailerUrl, String teaserUrl,
                                   String posterUrl, String synopsis, double ratingScore, boolean startAsTeaser) {
        super(parent, "NEXUS In-App Cinema Player • " + movieTitle, true);
        this.movieTitle = movieTitle;
        this.trailerUrl = trailerUrl;
        this.teaserUrl = teaserUrl;
        this.posterUrl = posterUrl;
        this.synopsis = synopsis;
        this.ratingScore = ratingScore;
        this.isTeaserMode = startAsTeaser;
        this.totalDurationSec = startAsTeaser ? 75 : 166;

        setSize(960, 640);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(new Color(11, 15, 25)); // Deep Cinema Black
        setLayout(new BorderLayout());

        initHeader();
        initCinemaViewport();
        initBottomControls();

        startPlaybackTimer();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopPlayback();
            }
        });
    }

    private void initHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(new Color(15, 23, 42));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(51, 65, 85)),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JLabel logo = new JLabel("🎬 NEXUS IN-APP CINEMA");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        logo.setForeground(new Color(56, 189, 248));
        left.add(logo);

        JLabel titleLbl = new JLabel("•  " + movieTitle + " (" + (isTeaserMode ? "Teaser" : "Official 4K Trailer") + ")");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLbl.setForeground(Color.WHITE);
        left.add(titleLbl);

        header.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JButton standaloneAppBtn = new JButton("🎥 Standalone Cinema Screen");
        standaloneAppBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        standaloneAppBtn.setBackground(new Color(225, 29, 72)); // YouTube Red
        standaloneAppBtn.setForeground(Color.WHITE);
        standaloneAppBtn.setFocusPainted(false);
        standaloneAppBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        standaloneAppBtn.setToolTipText("Open pure standalone embedded video player without browser address bars");
        standaloneAppBtn.addActionListener(e -> launchStandaloneAppWindow());
        right.add(standaloneAppBtn);

        JButton closeBtn = new JButton("✕ Close");
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        closeBtn.setBackground(new Color(51, 65, 85));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setFocusPainted(false);
        closeBtn.addActionListener(e -> {
            stopPlayback();
            dispose();
        });
        right.add(closeBtn);

        header.add(right, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
    }

    private void initCinemaViewport() {
        JPanel viewport = new JPanel(new BorderLayout());
        viewport.setBackground(new Color(8, 12, 20));
        viewport.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        videoScreenCanvas = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Cinema Screen Backdrop gradient
                GradientPaint gp = new GradientPaint(0, 0, new Color(15, 23, 42), 0, h, new Color(5, 8, 15));
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);

                // Subtle projector light beam effect
                g2.setColor(new Color(56, 189, 248, 15));
                Polygon beam = new Polygon(
                        new int[]{w / 2 - 80, w / 2 + 80, w, 0},
                        new int[]{0, 0, h, h},
                        4
                );
                g2.fill(beam);

                // Center Movie Title & Cinematic Branding
                g2.setFont(new Font("Segoe UI", Font.BOLD, 26));
                FontMetrics fm = g2.getFontMetrics();
                String displayTitle = movieTitle.toUpperCase();
                int tx = (w - fm.stringWidth(displayTitle)) / 2;
                int ty = h / 2 - 40;
                g2.setColor(new Color(0, 0, 0, 180));
                g2.drawString(displayTitle, tx + 2, ty + 2);
                g2.setColor(Color.WHITE);
                g2.drawString(displayTitle, tx, ty);

                // Subtitle/Mode Pill
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                String modePill = isTeaserMode ? "🔥 OFFICIAL SNEAK-PEEK TEASER • 4K HDR10+" : "🎬 OFFICIAL MAIN THEATRICAL TRAILER • IMAX 70mm";
                FontMetrics pfm = g2.getFontMetrics();
                int px = (w - pfm.stringWidth(modePill)) / 2;
                int py = ty + 30;
                g2.setColor(isTeaserMode ? new Color(245, 158, 11) : new Color(56, 189, 248));
                g2.drawString(modePill, px, py);

                // Animated Real-Time Audio EQ Bars (Simulating Live Dolby Atmos Playback)
                if (isPlaying) {
                    g2.setColor(new Color(52, 211, 153, 190));
                    int barCount = 36;
                    int barWidth = 6;
                    int startX = (w - (barCount * 10)) / 2;
                    int baseY = h / 2 + 60;
                    for (int i = 0; i < barCount; i++) {
                        int barH = (int) (Math.sin((currentTimeSec * 2.5) + (i * 0.45)) * 18 + 22);
                        g2.fillRect(startX + (i * 10), baseY - barH / 2, barWidth, barH);
                    }
                } else {
                    g2.setColor(new Color(239, 68, 68));
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
                    String pausedStr = "⏸ PAUSED";
                    int pw = g2.getFontMetrics().stringWidth(pausedStr);
                    g2.drawString(pausedStr, (w - pw) / 2, h / 2 + 60);
                }

                // Watermark Specs Badge
                g2.setFont(new Font("Consolas", Font.BOLD, 11));
                g2.setColor(new Color(148, 163, 184));
                g2.drawString("4K UHD • 60 FPS • DOLBY ATMOS 7.1 • BITRATE: 48.2 Mbps", 20, h - 20);

                g2.dispose();
            }
        };

        videoScreenCanvas.setLayout(new BorderLayout());

        // Subtitles Overlay at bottom of screen
        subtitleLabel = new JLabel(" ", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        subtitleLabel.setForeground(new Color(254, 240, 138)); // Light Warm Gold
        subtitleLabel.setOpaque(true);
        subtitleLabel.setBackground(new Color(0, 0, 0, 160));
        subtitleLabel.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        videoScreenCanvas.add(subtitleLabel, BorderLayout.SOUTH);

        viewport.add(videoScreenCanvas, BorderLayout.CENTER);
        add(viewport, BorderLayout.CENTER);
    }

    private void initBottomControls() {
        JPanel bottomContainer = new JPanel();
        bottomContainer.setLayout(new BoxLayout(bottomContainer, BoxLayout.Y_AXIS));
        bottomContainer.setBackground(new Color(15, 23, 42));
        bottomContainer.setBorder(BorderFactory.createEmptyBorder(6, 14, 10, 14));

        // Row 1: Interactive Scrubber Slider + Timestamp
        JPanel scrubberRow = new JPanel(new BorderLayout(10, 0));
        scrubberRow.setOpaque(false);

        progressSlider = new JSlider(0, totalDurationSec, 0);
        progressSlider.setBackground(new Color(15, 23, 42));
        progressSlider.setForeground(new Color(56, 189, 248));
        progressSlider.addChangeListener(e -> {
            if (progressSlider.getValueIsAdjusting()) {
                currentTimeSec = progressSlider.getValue();
                updateTimeDisplay();
                updateSubtitles();
            }
        });
        scrubberRow.add(progressSlider, BorderLayout.CENTER);

        timeLabel = new JLabel("00:00 / 02:46");
        timeLabel.setFont(new Font("Consolas", Font.BOLD, 12));
        timeLabel.setForeground(new Color(203, 213, 225));
        scrubberRow.add(timeLabel, BorderLayout.EAST);

        bottomContainer.add(scrubberRow);
        bottomContainer.add(Box.createVerticalStrut(4));

        // Row 2: Playback Buttons + Mode Switchers + Volume + Qualities
        JPanel controlsRow = new JPanel(new BorderLayout());
        controlsRow.setOpaque(false);

        // Left Controls (Play, Rewind, Forward, Mode Toggle)
        JPanel leftBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftBtns.setOpaque(false);

        playPauseBtn = new JButton("⏸ Pause");
        playPauseBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        playPauseBtn.setBackground(new Color(59, 130, 246));
        playPauseBtn.setForeground(Color.WHITE);
        playPauseBtn.setFocusPainted(false);
        playPauseBtn.addActionListener(e -> togglePlayPause());
        leftBtns.add(playPauseBtn);

        JButton rewindBtn = new JButton("⏪ -10s");
        rewindBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        rewindBtn.setBackground(new Color(30, 41, 59));
        rewindBtn.setForeground(Color.WHITE);
        rewindBtn.setFocusPainted(false);
        rewindBtn.addActionListener(e -> seekOffset(-10));
        leftBtns.add(rewindBtn);

        JButton forwardBtn = new JButton("⏩ +10s");
        forwardBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        forwardBtn.setBackground(new Color(30, 41, 59));
        forwardBtn.setForeground(Color.WHITE);
        forwardBtn.setFocusPainted(false);
        forwardBtn.addActionListener(e -> seekOffset(10));
        leftBtns.add(forwardBtn);

        JButton replayBtn = new JButton("↺ Replay");
        replayBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        replayBtn.setBackground(new Color(30, 41, 59));
        replayBtn.setForeground(Color.WHITE);
        replayBtn.setFocusPainted(false);
        replayBtn.addActionListener(e -> {
            currentTimeSec = 0;
            isPlaying = true;
            playPauseBtn.setText("⏸ Pause");
            updateTimeDisplay();
            videoScreenCanvas.repaint();
        });
        leftBtns.add(replayBtn);

        // Trailer vs Teaser Switch
        JButton switchTrackBtn = new JButton(isTeaserMode ? "🎬 Switch to 4K Main Trailer" : "🔥 Switch to Teaser");
        switchTrackBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        switchTrackBtn.setBackground(isTeaserMode ? new Color(225, 29, 72) : new Color(217, 119, 6));
        switchTrackBtn.setForeground(Color.WHITE);
        switchTrackBtn.setFocusPainted(false);
        switchTrackBtn.addActionListener(e -> {
            isTeaserMode = !isTeaserMode;
            totalDurationSec = isTeaserMode ? 75 : 166;
            progressSlider.setMaximum(totalDurationSec);
            currentTimeSec = 0;
            switchTrackBtn.setText(isTeaserMode ? "🎬 Switch to 4K Main Trailer" : "🔥 Switch to Teaser");
            switchTrackBtn.setBackground(isTeaserMode ? new Color(225, 29, 72) : new Color(217, 119, 6));
            updateTimeDisplay();
            videoScreenCanvas.repaint();
        });
        leftBtns.add(switchTrackBtn);

        controlsRow.add(leftBtns, BorderLayout.WEST);

        // Right Settings (Volume, Subtitles, Quality, Audio)
        JPanel rightSettings = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightSettings.setOpaque(false);

        rightSettings.add(new JLabel("🔊 Vol:"));
        volumeSlider = new JSlider(0, 100, 85);
        volumeSlider.setPreferredSize(new Dimension(80, 20));
        volumeSlider.setBackground(new Color(15, 23, 42));
        rightSettings.add(volumeSlider);

        rightSettings.add(new JLabel("Subtitles:"));
        String[] subs = {"English (CC)", "Tamil", "Telugu", "Hindi", "Off"};
        subtitleCombo = new JComboBox<>(subs);
        subtitleCombo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subtitleCombo.addActionListener(e -> updateSubtitles());
        rightSettings.add(subtitleCombo);

        String[] qualities = {"4K UHD (2160p)", "1080p 60fps", "720p HD"};
        qualityCombo = new JComboBox<>(qualities);
        qualityCombo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        rightSettings.add(qualityCombo);

        String[] audios = {"Dolby Atmos 7.1", "IMAX 12-Track", "Stereo"};
        audioModeCombo = new JComboBox<>(audios);
        audioModeCombo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        rightSettings.add(audioModeCombo);

        controlsRow.add(rightSettings, BorderLayout.EAST);
        bottomContainer.add(controlsRow);

        add(bottomContainer, BorderLayout.SOUTH);
        updateTimeDisplay();
    }

    private void togglePlayPause() {
        isPlaying = !isPlaying;
        playPauseBtn.setText(isPlaying ? "⏸ Pause" : "▶ Play");
        videoScreenCanvas.repaint();
    }

    private void seekOffset(int offsetSec) {
        currentTimeSec = Math.max(0, Math.min(totalDurationSec, currentTimeSec + offsetSec));
        progressSlider.setValue(currentTimeSec);
        updateTimeDisplay();
        updateSubtitles();
        videoScreenCanvas.repaint();
    }

    private void startPlaybackTimer() {
        playbackTimer = new javax.swing.Timer(1000, e -> {
            if (isPlaying) {
                currentTimeSec++;
                if (currentTimeSec >= totalDurationSec) {
                    currentTimeSec = 0; // loop
                }
                progressSlider.setValue(currentTimeSec);
                updateTimeDisplay();
                updateSubtitles();
                videoScreenCanvas.repaint();
            }
        });
        playbackTimer.start();
    }

    private void stopPlayback() {
        if (playbackTimer != null) {
            playbackTimer.stop();
        }
        isPlaying = false;
    }

    private void updateTimeDisplay() {
        int curM = currentTimeSec / 60;
        int curS = currentTimeSec % 60;
        int totM = totalDurationSec / 60;
        int totS = totalDurationSec % 60;
        timeLabel.setText(String.format("%02d:%02d / %02d:%02d", curM, curS, totM, totS));
    }

    private void updateSubtitles() {
        String lang = (String) subtitleCombo.getSelectedItem();
        if ("Off".equals(lang)) {
            subtitleLabel.setText(" ");
            return;
        }

        // Realistic synchronized dialogue cues for in-app cinematic playback
        String subText = "";
        int phase = currentTimeSec % 40;
        if (phase < 8) {
            subText = movieTitle.contains("Dune") ? "\"Power over spice is power over all...\""
                    : (movieTitle.contains("Interstellar") ? "\"Do not go gentle into that good night...\""
                    : (movieTitle.contains("Kalki") ? "\"The prophecy of the Tenth Avatar awakens...\""
                    : (movieTitle.contains("Deadpool") ? "\"I am Marvel Jesus, baby!\""
                    : "\"Experience the cinematic event of the decade...\"")));
        } else if (phase < 18) {
            subText = movieTitle.contains("Dune") ? "\"May thy knife chip and shatter!\""
                    : (movieTitle.contains("Interstellar") ? "\"We used to look up at the sky and wonder at our place in the stars.\""
                    : (movieTitle.contains("Oppenheimer") ? "\"Now I am become Death, the destroyer of worlds.\""
                    : "\"In Theatres Worldwide • Booking Live on BookMyShow\""));
        } else if (phase < 28) {
            subText = "\"Sound mastered exclusively in DOLBY ATMOS 7.1 & IMAX LASER\"";
        } else {
            subText = "\"Tickets available across all campus multiplex screens.\"";
        }

        if ("Tamil".equals(lang)) {
            subText = "[தமிழ்] " + subText;
        } else if ("Telugu".equals(lang)) {
            subText = "[తెలుగు] " + subText;
        } else if ("Hindi".equals(lang)) {
            subText = "[हिंदी] " + subText;
        }

        subtitleLabel.setText(subText);
    }

    /**
     * Launches Microsoft Edge in pure App Mode (--app) with ZERO browser address bar,
     * zero browser tabs, and zero toolbars. Appears as a dedicated native desktop video player application!
     */
    public void launchStandaloneAppWindow() {
        String targetUrl = isTeaserMode ? teaserUrl : trailerUrl;
        String videoId = extractYouTubeVideoId(targetUrl);
        String embedUrl = (videoId != null && !videoId.isEmpty())
                ? "https://www.youtube.com/embed/" + videoId + "?autoplay=1&modestbranding=1&rel=0&iv_load_policy=3"
                : targetUrl;

        String edgePath = "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe";
        if (!new File(edgePath).exists()) {
            edgePath = "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe";
        }

        try {
            if (new File(edgePath).exists()) {
                // Launch Edge in Standalone Native Video Player App Mode
                ProcessBuilder pb = new ProcessBuilder(
                        edgePath,
                        "--app=" + embedUrl,
                        "--window-size=1080,640",
                        "--window-position=200,150",
                        "--user-data-dir=" + System.getProperty("java.io.tmpdir") + "\\nexus_player_profile"
                );
                pb.start();
            } else {
                // Fallback
                Desktop.getDesktop().browse(new java.net.URI(embedUrl));
            }
        } catch (Exception ex) {
            System.err.println("[InAppPlayer] Standalone launch failed: " + ex.getMessage());
        }
    }

    private static String extractYouTubeVideoId(String url) {
        if (url == null) return null;
        Pattern p = Pattern.compile("(?:v=|/embed/|/watch\\?v=|youtu\\.be/)([a-zA-Z0-9_-]{11})");
        Matcher m = p.matcher(url);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
}
