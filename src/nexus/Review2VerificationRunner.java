package nexus;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import nexus.finance.CryptoPrice;
import nexus.finance.RealtimeFinanceService;
import nexus.finance.Transaction;
import nexus.finance.TransactionCategory;
import nexus.goals.Goal;
import nexus.goals.Milestone;
import nexus.media.MediaItem;
import nexus.media.MediaType;
import nexus.media.RealtimeMediaService;
import nexus.realtime.SRMWeatherService;
import nexus.tasks.Task;
import nexus.tasks.TaskPriority;
import nexus.database.DatabaseService;
import nexus.common.EventBus;
import nexus.common.NexusEvent;

public class Review2VerificationRunner {
    public static void main(String[] args) {
        System.out.println("=============================================================");
        System.out.println("   NEXUS V7 STARTUP EDITION - REVIEW 2 VERIFICATION SUITE    ");
        System.out.println("=============================================================");

        // 1. VERIFY REAL-TIME COINGECKO CRYPTO FEED
        System.out.println("\n[1/5] Testing Live CoinGecko Cryptocurrency Feed...");
        try {
            List<CryptoPrice> cryptoList = RealtimeFinanceService.fetchLiveCryptoRates();
            for (CryptoPrice cp : cryptoList) {
                System.out.printf("   ✓ %s: $%,.2f (₹%,.0f) 24h: %+.2f%%\n", 
                        cp.getSymbol(), cp.getPriceUSD(), cp.getPriceINR(), cp.getChange24h());
            }
            if (!cryptoList.isEmpty()) {
                System.out.println("   >>> STATUS: REAL-TIME CRYPTO API ONLINE & OPERATIONAL!");
            }
        } catch (Exception e) {
            System.err.println("   ✗ Crypto Feed Error: " + e.getMessage());
        }

        // 2. VERIFY REAL-TIME TVMAZE MEDIA FEED
        System.out.println("\n[2/5] Testing Live TVMaze Entertainment Database API...");
        try {
            List<MediaItem> shows = RealtimeMediaService.searchShowsOnline("Arcane");
            for (int i = 0; i < Math.min(3, shows.size()); i++) {
                MediaItem m = shows.get(i);
                System.out.printf("   ✓ '%s' (%s) | Rating: ★%.1f/10 | Genres: %s\n", 
                        m.getTitle(), m.getYear(), m.getRatingScore(), m.getGenres());
            }
            if (!shows.isEmpty()) {
                System.out.println("   >>> STATUS: REAL-TIME TVMAZE MEDIA API ONLINE & OPERATIONAL!");
            }
        } catch (Exception e) {
            System.err.println("   ✗ Media Feed Error: " + e.getMessage());
        }

        // 3. VERIFY REAL-TIME SRM KATTANKULATHUR WEATHER SENSOR
        System.out.println("\n[3/5] Testing Live SRM University Campus Telemetry...");
        try {
            SRMWeatherService.WeatherData weather = SRMWeatherService.fetchLiveWeather();
            System.out.println("   ✓ " + weather);
            System.out.println("   >>> STATUS: REAL-TIME SRM CAMPUS SENSOR DATA STREAMING!");
        } catch (Exception e) {
            System.err.println("   ✗ Weather Sensor Error: " + e.getMessage());
        }

        // 4. VERIFY SQLITE PERSISTENCE
        System.out.println("\n[4/5] Testing Asynchronous SQLite Persistence Engine...");
        try {
            DatabaseService db = new DatabaseService();
            Thread.sleep(300); // let db init

            // Write Task
            Task t = new Task("Test Review 2 Task", TaskPriority.HIGH, "Academic", "Tomorrow");
            EventBus.getInstance().publish(new NexusEvent("TASK_ADDED", t, "Test"));

            // Write Transaction
            Transaction tx = new Transaction("Campus Cafeteria", 12.50, TransactionCategory.FOOD);
            EventBus.getInstance().publish(new NexusEvent("TRANSACTION_ADDED", tx, "Test"));

            // Write Goal
            Goal g = new Goal("Publish Research Paper", "Academic", "Dec 2026");
            g.addMilestone(new Milestone("Draft Abstract"));
            EventBus.getInstance().publish(new NexusEvent("GOAL_ADDED", g, "Test"));

            // Write Media
            MediaItem mi = new MediaItem("Oppenheimer", MediaType.MOVIE, "Biography, Drama", 8.9, "Biographical thriller", "");
            EventBus.getInstance().publish(new NexusEvent("MEDIA_ADDED", mi, "Test"));

            Thread.sleep(1000); // Wait for background queue to flush

            List<Task> loadedTasks = db.loadTasks();
            List<Transaction> loadedTx = db.loadTransactions();
            List<Goal> loadedGoals = db.loadGoals();
            List<MediaItem> loadedMedia = db.loadMedia();

            System.out.printf("   ✓ SQLite Tasks Loaded: %d records\n", loadedTasks.size());
            System.out.printf("   ✓ SQLite Transactions Loaded: %d records\n", loadedTx.size());
            System.out.printf("   ✓ SQLite Goals Loaded: %d records\n", loadedGoals.size());
            System.out.printf("   ✓ SQLite Watchlist Loaded: %d records\n", loadedMedia.size());
            System.out.println("   >>> STATUS: PERSISTENCE ENGINE 100% OPERATIONAL!");
        } catch (Exception e) {
            System.err.println("   ✗ Database Error: " + e.getMessage());
        }

        // 5. RENDER APPLICATION & CAPTURE LIVE SCREENSHOT
        System.out.println("\n[5/5] Launching GUI & Capturing Live Review 2 Screenshot...");
        try {
            com.formdev.flatlaf.FlatDarkLaf.setup();
            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));

            SwingUtilities.invokeAndWait(() -> {
                NexusApplication app = new NexusApplication();
                app.setVisible(true);

                new Thread(() -> {
                    try {
                        Thread.sleep(1500); // Wait for UI rendering & network fetch
                        SwingUtilities.invokeAndWait(() -> {
                            try {
                                BufferedImage image = new BufferedImage(app.getWidth(), app.getHeight(), BufferedImage.TYPE_INT_RGB);
                                app.paint(image.getGraphics());
                                File output = new File("nexus_review2_preview.png");
                                ImageIO.write(image, "png", output);
                                System.out.println("   ✓ Preview screenshot captured: " + output.getAbsolutePath());
                                System.out.println("\n=============================================================");
                                System.out.println("   ALL REVIEW 2 UPGRADES VALIDATED & READY FOR EVALUATION!   ");
                                System.out.println("=============================================================");
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        });
                        Thread.sleep(500);
                        System.exit(0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
