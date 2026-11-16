package nexus;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class CaptureV25Screenshots {

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
        System.out.println("Starting V25 Screenshot Capture Engine...");
        
        File outDir = new File("C:\\Users\\vanga\\Downloads\\nexus_v25_screenshots");
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

        Thread.sleep(1200);

        // 1. Capture Login
        captureWindow(app, new File(outDir, "00_v25_login.png"));
        System.out.println("Captured: 00_v25_login.png");

        // Find CardLayout and show MainApp
        JPanel mainContent = (JPanel) app.getContentPane().getComponent(0);
        CardLayout cardLayout = (CardLayout) mainContent.getLayout();
        
        SwingUtilities.invokeAndWait(() -> {
            cardLayout.show(mainContent, "MainApp");
            app.revalidate();
            app.repaint();
        });

        Thread.sleep(1000);

        JTabbedPane tabbedPane = findComponent(mainContent, JTabbedPane.class);
        if (tabbedPane == null) {
            System.err.println("Could not find JTabbedPane!");
            System.exit(1);
        }

        int tabCount = tabbedPane.getTabCount();
        System.out.println("Found tabs: " + tabCount);

        for (int i = 0; i < tabCount; i++) {
            final int tabIdx = i;
            String rawTitle = tabbedPane.getTitleAt(tabIdx);
            String safeTitle = rawTitle.replaceAll("[^a-zA-Z0-9_-]", "_").replaceAll("_+", "_");
            String filename = String.format("%02d_v25%s.png", tabIdx + 1, safeTitle);

            SwingUtilities.invokeAndWait(() -> {
                tabbedPane.setSelectedIndex(tabIdx);
                app.revalidate();
                app.repaint();
            });

            Thread.sleep(700);

            captureWindow(app, new File(outDir, filename));
            System.out.println("Captured Tab " + tabIdx + ": " + filename);
        }

        app.dispose();
        System.out.println("ALL V25 SCREENSHOTS CAPTURED SUCCESSFULLY!");
        System.exit(0);
    }

    private static void captureWindow(Window window, File target) {
        try {
            int w = window.getWidth();
            int h = window.getHeight();
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = img.createGraphics();
            window.paint(g2d);
            g2d.dispose();
            ImageIO.write(img, "png", target);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
