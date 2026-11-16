package nexus;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * High-definition visual capture engine for NEXUS V26.
 * Captures live GUI execution across tabs for visual verification.
 */
public class CaptureV26Screenshots {

    private static <T extends Component> T findComponent(Container root, Class<T> clazz) {
        for (Component c : root.getComponents()) {
            if (clazz.isInstance(c)) return clazz.cast(c);
            if (c instanceof Container child) {
                T found = findComponent(child, clazz);
                if (found != null) return found;
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Starting NEXUS V26 Visual Verification Capture Engine...");
        
        File outDir = new File("C:\\Users\\vanga\\Downloads\\nexus_v26_screenshots");
        outDir.mkdirs();

        SwingUtilities.invokeAndWait(() -> {
            try {
                UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
            } catch (Exception ignored) {}
        });

        NexusApplication app = new NexusApplication();
        app.setSize(1320, 820);
        app.setLocation(50, 50);
        app.setVisible(true);

        Thread.sleep(1500);

        // 1. Capture Login
        captureWindow(app, new File(outDir, "00_v26_login.png"));
        System.out.println("Captured: 00_v26_login.png");

        // Find CardLayout and show MainApp
        JPanel mainContent = (JPanel) app.getContentPane().getComponent(0);
        CardLayout cardLayout = (CardLayout) mainContent.getLayout();
        
        SwingUtilities.invokeAndWait(() -> {
            cardLayout.show(mainContent, "MainApp");
            app.revalidate();
            app.repaint();
        });

        Thread.sleep(1200);

        JTabbedPane tabbedPane = findComponent(mainContent, JTabbedPane.class);
        if (tabbedPane == null) {
            System.err.println("Could not find JTabbedPane!");
            System.exit(1);
        }

        int tabCount = tabbedPane.getTabCount();
        System.out.println("Found " + tabCount + " production tabs in NEXUS V26.");

        for (int i = 0; i < tabCount; i++) {
            final int tabIdx = i;
            String rawTitle = tabbedPane.getTitleAt(tabIdx);
            String cleanTitle = rawTitle.replaceAll("[^a-zA-Z0-9_-]", "_").replaceAll("_+", "_");

            SwingUtilities.invokeAndWait(() -> {
                tabbedPane.setSelectedIndex(tabIdx);
                app.revalidate();
                app.repaint();
            });

            Thread.sleep(800);

            String fileName = String.format("%02d_v26_%s.png", tabIdx + 1, cleanTitle);
            captureWindow(app, new File(outDir, fileName));
            System.out.println("Captured Tab " + (tabIdx + 1) + "/" + tabCount + ": " + fileName + " (" + rawTitle + ")");
        }

        System.out.println("\nAll " + tabCount + " V26 tab screenshots captured successfully into: " + outDir.getAbsolutePath());
        app.dispose();
        System.exit(0);
    }

    private static void captureWindow(Window window, File outFile) throws Exception {
        BufferedImage img = new BufferedImage(window.getWidth(), window.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        window.paint(g2);
        g2.dispose();
        ImageIO.write(img, "png", outFile);
    }
}
