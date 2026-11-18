package nexus.test;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import nexus.auth.AccountService;
import nexus.auth.UserProfile;
import nexus.common.EventBus;
import nexus.common.NexusEvent;
import nexus.common.exceptions.AuthenticationFailedException;
import nexus.database.DatabaseService;
import nexus.finance.FinancialCalculator;
import nexus.finance.Transaction;
import nexus.finance.TransactionCategory;
import nexus.goals.Goal;
import nexus.market.Product;
import nexus.market.ShoppingCart;
import nexus.media.CinemaBooking;
import nexus.media.TicketQrGenerator;
import nexus.tasks.Task;
import nexus.tasks.TaskPriority;

/**
 * Production Verification & Automated Regression Test Suite for NEXUS.
 * Validates core business logic, database persistence, financial precision,
 * authentication security boundaries, and event propagation.
 */
public class NexusProductionTestSuite {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  NEXUS PRODUCTION REGRESSION TEST SUITE (JDK 21)");
        System.out.println("=================================================");

        runTest("Authentication: Valid Admin Credentials", NexusProductionTestSuite::testAuthSuccess);
        runTest("Authentication: Rejection of Bad Credentials", NexusProductionTestSuite::testAuthFailure);
        runTest("Financial Engine: EMI Mathematical Precision", NexusProductionTestSuite::testFinanceEmiCalculation);
        runTest("Financial Engine: Compound Interest Formula", NexusProductionTestSuite::testFinanceCompoundInterest);
        runTest("Marketplace: Cart Addition & Dynamic Subtotal", NexusProductionTestSuite::testMarketCartCalculations);
        runTest("Media: Dynamic 2D Ticket QR Matrix Rendering", NexusProductionTestSuite::testTicketQrGeneration);
        runTest("Database Service: SQLite ACID Persistence Roundtrip", NexusProductionTestSuite::testDatabasePersistence);
        runTest("EventBus Hub: Reactive Publish / Subscribe Delivery", NexusProductionTestSuite::testEventBusPubSub);

        System.out.println("=================================================");
        System.out.printf("  RESULTS: %d PASSED, %d FAILED\n", testsPassed, testsFailed);
        System.out.println("=================================================");

        if (testsFailed > 0) {
            System.exit(1);
        }
    }

    private static void runTest(String testName, Runnable test) {
        System.out.printf("[RUNNING] %-55s ", testName);
        try {
            test.run();
            System.out.println(" [PASS] ✓");
            testsPassed++;
        } catch (Throwable t) {
            System.out.println(" [FAIL] ✗");
            System.err.println("   Error: " + t.getMessage());
            testsFailed++;
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Assertion Failed: " + message);
        }
    }

    private static void assertEquals(double expected, double actual, double delta, String message) {
        if (Math.abs(expected - actual) > delta) {
            throw new AssertionError(String.format("%s (Expected: %.4f, Got: %.4f)", message, expected, actual));
        }
    }

    private static void testAuthSuccess() {
        AccountService auth = AccountService.getInstance();
        try {
            UserProfile p = auth.authenticate("ramcharan.vanga", "admin");
            assertTrue(p != null, "User profile should not be null");
            assertTrue(p.getId().equals("USER-01"), "Expected USER-01");
        } catch (AuthenticationFailedException e) {
            throw new AssertionError("Valid authentication threw unexpected exception: " + e.getMessage());
        }
    }

    private static void testAuthFailure() {
        AccountService auth = AccountService.getInstance();
        boolean rejected = false;
        try {
            auth.authenticate("ramcharan.vanga", "wrong_password_123");
        } catch (AuthenticationFailedException e) {
            rejected = true;
        }
        assertTrue(rejected, "Invalid password must throw AuthenticationFailedException");
    }

    private static void testFinanceEmiCalculation() {
        // Principal = 100,000, Annual Rate = 10%, Tenure = 12 months
        FinancialCalculator.EmiResult result = FinancialCalculator.calculateEmi(100000.0, 10.0, 12);
        // Standard formula gives ~8791.59
        assertEquals(8791.59, result.monthlyEmi, 1.0, "Monthly EMI calculation outside acceptable tolerance");
    }

    private static void testFinanceCompoundInterest() {
        // Principal = 10,000, Rate = 5%, Time = 2 years, Compounded Annually (1)
        FinancialCalculator.CompoundInterestResult result = FinancialCalculator.calculateCompoundInterest(10000.0, 5.0, 1, 2);
        // Formula: 10000 * (1 + 0.05)^2 = 11025.0
        assertEquals(11025.0, result.futureValue, 0.1, "Compound interest outside precision tolerance");
    }

    private static void testMarketCartCalculations() {
        ShoppingCart cart = new ShoppingCart();
        Product p1 = new Product("Mechanical Keyboard", 2500.0, "Electronics", null);
        Product p2 = new Product("Wireless Mouse", 800.0, "Electronics", null);

        cart.addItem(p1, 1);
        cart.addItem(p2, 2);

        assertEquals(4100.0, cart.getTotal(), 0.01, "Shopping cart total mismatch");
        assertTrue(cart.getItems().size() == 2, "Expected 2 cart items");
    }

    private static void testTicketQrGeneration() {
        String payload = "NEXUS-PASS|BMS-TEST01|Dune Part Two|A1, A2|2026-11-18";
        BufferedImage img = TicketQrGenerator.generateQrImage(payload, 150);

        assertTrue(img != null, "Generated QR image must not be null");
        assertTrue(img.getWidth() == 150, "QR width should be 150");
        assertTrue(img.getHeight() == 150, "QR height should be 150");
    }

    private static void testDatabasePersistence() {
        DatabaseService db = DatabaseService.getInstance();
        assertTrue(db != null, "DatabaseService instance must not be null");

        // Insert and read back a test task
        Task testTask = new Task("Test Verification Task", TaskPriority.HIGH);
        db.persistTask(testTask);

        List<Task> tasks = db.loadTasks();
        assertTrue(!tasks.isEmpty(), "Loaded tasks should not be empty");

        // Insert and verify a test transaction
        Transaction testTx = new Transaction("QA Verification Expense", 150.0, TransactionCategory.OTHER);
        db.persistTransaction(testTx);

        List<Transaction> txList = db.loadTransactions();
        assertTrue(!txList.isEmpty(), "Loaded transactions should not be empty");
    }

    private static void testEventBusPubSub() {
        EventBus bus = EventBus.getInstance();
        AtomicBoolean received = new AtomicBoolean(false);

        bus.subscribe("TEST_EVENT_TOPIC", event -> {
            if ("QA_PAYLOAD".equals(event.getPayload())) {
                received.set(true);
            }
        });

        bus.publish(new NexusEvent("TEST_EVENT_TOPIC", "QA_PAYLOAD", "TestSuite"));

        assertTrue(received.get(), "EventBus should synchronously deliver published events to subscribers");
    }
}
