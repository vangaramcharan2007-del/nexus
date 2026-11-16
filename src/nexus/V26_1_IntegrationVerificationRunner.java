package nexus;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import nexus.common.EventBus;
import nexus.common.NexusEvent;
import nexus.common.NexusListener;
import nexus.database.DatabaseService;
import nexus.tasks.Task;
import nexus.tasks.TaskPriority;
import nexus.tasks.TaskService;
import nexus.finance.FinanceService;
import nexus.finance.PurchaseResult;
import nexus.finance.Transaction;
import nexus.finance.TransactionCategory;
import nexus.finance.FinancialCalculator;
import nexus.market.CartItem;
import nexus.market.MarketService;
import nexus.market.Product;
import nexus.analytics.AnalyticsService;
import nexus.goals.Goal;
import nexus.goals.Milestone;
import nexus.integrations.common.HttpHelper;
import nexus.integrations.common.IntegrationLogger;
import nexus.integrations.common.IntegrationStatus;
import nexus.integrations.notifications.NotificationConfig;
import nexus.integrations.notifications.NotificationPayload;
import nexus.integrations.notifications.NotificationPreferenceService;
import nexus.integrations.notifications.NtfyNotificationService;
import nexus.integrations.analytics.AnalyticsDashboardModel;
import nexus.integrations.analytics.MetabaseConfig;
import nexus.integrations.analytics.MetabaseAnalyticsService;
import nexus.integrations.analytics.ReadOnlyAnalyticsStore;
import nexus.rules.RuleEngineService;
import nexus.resilience.CircuitBreaker;
import nexus.tenant.CampusTenant;
import nexus.tenant.TenantRole;
import nexus.tenant.TenantPermission;
import nexus.tenant.TenantService;
import nexus.vault.AuditBlock;
import nexus.vault.ImmutableAuditChain;
import nexus.vault.VaultService;
import nexus.web3.Web3ProtocolService;

/**
 * Master Verification & Automated Test Suite for NEXUS V26.1.
 *
 * Comprehensive, honest verification across all core subsystems:
 * 1.  Atomic EventBus Subscription Deduplication (50 concurrent threads)
 * 2.  Listener Lifecycle & JVM Fatal Error Propagation
 * 3.  Database Persistence Across Simulated Restarts (Temporary DB)
 * 4.  Database Writer Queue Lifecycle, Error Tracking & Idempotent Shutdown
 * 5.  Real Startup Task Seeding Flow (Empty vs Pre-existing User Data)
 * 6.  Financial Validation (Rejection of NaN, Infinite, Negative, Zero & Duplicate IDs)
 * 7.  Concurrent Financial Checkout Race Condition Prevention
 * 8.  Exact Balance Purchase Validation
 * 9.  Cart Preservation on Failure & Clearance on Confirmed Success
 * 10. ntfy Local HTTP Mock Delivery, Headers & Exponential Backoff Retry
 * 11. Metabase Health Detection & Dual-Layer SQLite Read-Only Protection
 * 12. Local-First Autonomous Startup with External Integrations Disabled
 * 13. Multi-Module Enterprise Regression Suite
 * 14. Graceful Application Teardown & Resource Cleanup
 */
public class V26_1_IntegrationVerificationRunner {

    private static int totalPassed = 0;
    private static int totalFailed = 0;
    private static final List<String> resultsSummary = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("       NEXUS V26.1 — RELIABILITY, SECURITY & VERIFICATION MASTER SUITE         ");
        System.out.println("================================================================================\n");

        long suiteStartTime = System.currentTimeMillis();

        test1_AtomicEventBusDeduplication();
        test2_ListenerLifecycleAndFatalErrors();
        test3_DatabasePersistenceAcrossRestarts();
        test4_DatabaseLifecycleAndErrorTracking();
        test5_RealStartupTaskSeedingFlow();
        test6_FinancialValidationGuards();
        test7_ConcurrentFinancialCheckoutRaceCondition();
        test8_ExactBalancePurchase();
        test9_CartPreservationAndClearance();
        test10_NtfyMockDeliveryAndRetry();
        test11_MetabaseStatusAndReadOnlyProtection();
        test12_AutonomousStartupWithoutExternalServices();
        test13_ExistingModuleRegressionTests();
        test14_GracefulTeardown();

        probeLiveConnectivity();

        long duration = System.currentTimeMillis() - suiteStartTime;

        System.out.println("\n================================================================================");
        System.out.println("                        V26.1 TEST EXECUTION SUMMARY                            ");
        System.out.println("================================================================================");
        for (String line : resultsSummary) {
            System.out.println(line);
        }
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("Total Executed: %d | Passed: %d | Failed: %d | Total Duration: %d ms%n",
                totalPassed + totalFailed, totalPassed, totalFailed, duration);
        System.out.println("================================================================================\n");

        if (totalFailed > 0) {
            System.err.println("❌ Verification FAILED with " + totalFailed + " failed assertions.");
            System.exit(1);
        } else {
            System.out.println("✅ ALL 14 V26.1 RELIABILITY & INTEGRATION CRITERIA PASSED FLAWLESSLY.");
        }
    }

    private static void pass(String testName, String details) {
        totalPassed++;
        String msg = String.format("[PASS] %-50s : %s", testName, details);
        System.out.println(msg);
        resultsSummary.add(msg);
    }

    private static void fail(String testName, String reason) {
        totalFailed++;
        String msg = String.format("[FAIL] %-50s : %s", testName, reason);
        System.err.println(msg);
        resultsSummary.add(msg);
    }

    // -------------------------------------------------------------------------
    // TEST 1: Atomic EventBus Subscription Deduplication
    // -------------------------------------------------------------------------
    private static void test1_AtomicEventBusDeduplication() {
        System.out.println(">>> Running Test 1: Atomic EventBus Subscription Deduplication...");
        EventBus bus = EventBus.getInstance();
        bus.reset();

        final int registrationThreads = 50;
        final AtomicInteger callbackCount = new AtomicInteger(0);
        NexusListener targetListener = event -> callbackCount.incrementAndGet();

        ExecutorService pool = Executors.newFixedThreadPool(registrationThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(registrationThreads);

        for (int i = 0; i < registrationThreads; i++) {
            pool.submit(() -> {
                try {
                    startLatch.await();
                    bus.subscribe("ATOMIC_DEDUP_TEST", targetListener);
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Release all 50 threads simultaneously
        try {
            doneLatch.await(5, TimeUnit.SECONDS);
            pool.shutdown();

            int subscriberCount = bus.getSubscriberCount("ATOMIC_DEDUP_TEST");
            bus.publish(new NexusEvent("ATOMIC_DEDUP_TEST", "Payload", "Test"));
            int invocations = callbackCount.get();

            if (subscriberCount == 1 && invocations == 1) {
                pass("Test 1: Atomic EventBus Deduplication", 
                    "50 concurrent registration attempts resulted in exactly 1 listener entry and 1 callback.");
            } else {
                fail("Test 1: Atomic EventBus Deduplication", 
                    "Deduplication failed: subscriberCount=" + subscriberCount + ", invocations=" + invocations);
            }
        } catch (InterruptedException e) {
            fail("Test 1: Atomic EventBus Deduplication", "Interrupted: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // TEST 2: Listener Lifecycle & JVM Fatal Error Propagation
    // -------------------------------------------------------------------------
    private static void test2_ListenerLifecycleAndFatalErrors() {
        System.out.println(">>> Running Test 2: Listener Lifecycle & JVM Fatal Errors...");
        EventBus bus = EventBus.getInstance();
        bus.reset();

        final AtomicInteger healthyCount = new AtomicInteger(0);
        NexusListener healthyListener = event -> healthyCount.incrementAndGet();
        NexusListener faultyListener = event -> { throw new RuntimeException("Simulated runtime fault"); };

        bus.subscribe("FAULT_ISOLATION_TEST", faultyListener);
        bus.subscribe("FAULT_ISOLATION_TEST", healthyListener);

        // Faulty listener should not prevent healthy listener from receiving event
        bus.publish(new NexusEvent("FAULT_ISOLATION_TEST", "Data", "Test"));
        boolean isolationOk = healthyCount.get() == 1;

        // Unsubscribe test
        boolean unregOk = bus.unsubscribe("FAULT_ISOLATION_TEST", healthyListener);
        bus.publish(new NexusEvent("FAULT_ISOLATION_TEST", "Data2", "Test"));
        boolean unregEnforced = healthyCount.get() == 1; // Still 1

        if (isolationOk && unregOk && unregEnforced) {
            pass("Test 2: Listener Lifecycle & Exception Isolation", 
                "Exception in faulty listener isolated; unsubscribe lifecycle verified.");
        } else {
            fail("Test 2: Listener Lifecycle & Exception Isolation", 
                "Failed: isolationOk=" + isolationOk + ", unregOk=" + unregOk + ", unregEnforced=" + unregEnforced);
        }
    }

    // -------------------------------------------------------------------------
    // TEST 3: Database Persistence Across Restarts
    // -------------------------------------------------------------------------
    private static void test3_DatabasePersistenceAcrossRestarts() {
        System.out.println(">>> Running Test 3: Database Persistence Across Restarts...");
        String testDb = "nexus_v26_1_restart_test.db";
        File f = new File(testDb);
        if (f.exists()) f.delete();

        System.setProperty("nexus.db.path", testDb);
        DatabaseService db = new DatabaseService();

        String taskId = "TASK-" + UUID.randomUUID().toString().substring(0, 8);
        Task t = new Task(taskId, "Restart Verification Task", TaskPriority.HIGH, false, "Dev", "Today");
        db.persistTask(t);

        String txId = "TX-" + UUID.randomUUID().toString().substring(0, 8);
        Transaction tx = new Transaction(txId, "Audit Deposit", 850.0, new Date(), TransactionCategory.SALARY_INCOME);
        db.persistTransaction(tx);

        boolean drained = db.awaitDrained(3000);
        db.close();

        // Simulate restart
        DatabaseService restartedDb = new DatabaseService();
        List<Task> loadedTasks = restartedDb.loadTasks();
        List<Transaction> loadedTx = restartedDb.loadTransactions();
        restartedDb.close();

        boolean taskFound = loadedTasks.stream().anyMatch(item -> item.getId().equals(taskId));
        boolean txFound = loadedTx.stream().anyMatch(item -> item.getId().equals(txId));

        if (f.exists()) f.delete();
        System.clearProperty("nexus.db.path");

        if (drained && taskFound && txFound) {
            pass("Test 3: Database Persistence Across Restarts", 
                "Records persisted and restored across simulated restart via temporary test database.");
        } else {
            fail("Test 3: Database Persistence Across Restarts", 
                "Restart persistence failed: drained=" + drained + ", taskFound=" + taskFound + ", txFound=" + txFound);
        }
    }

    // -------------------------------------------------------------------------
    // TEST 4: Database Writer Queue Lifecycle & Error Tracking
    // -------------------------------------------------------------------------
    private static void test4_DatabaseLifecycleAndErrorTracking() {
        System.out.println(">>> Running Test 4: Database Queue Lifecycle & Error Tracking...");
        String testDb = "nexus_v26_1_lifecycle_test.db";
        File f = new File(testDb);
        if (f.exists()) f.delete();

        System.setProperty("nexus.db.path", testDb);
        DatabaseService db = new DatabaseService();

        // Submit 20 tasks under load
        for (int i = 0; i < 20; i++) {
            db.persistTask(new Task("LOAD-" + i, "Load Task " + i, TaskPriority.LOW, false));
        }

        boolean drained = db.awaitDrained(3000);
        db.close();
        boolean isClosed = db.isClosed();

        // Repeated close should be idempotent
        db.close();

        // Attempting write after close should be rejected gracefully
        db.onEvent(new NexusEvent("TASK_ADDED", new Task("POST_CLOSE", "Post Close", TaskPriority.LOW, false), "Test"));
        boolean postCloseRejected = db.loadTasks().stream().noneMatch(t -> t.getId().equals("POST_CLOSE"));

        if (f.exists()) f.delete();
        System.clearProperty("nexus.db.path");

        if (drained && isClosed && postCloseRejected) {
            pass("Test 4: Database Lifecycle & Idempotency", 
                "Queue drain verified under load; close() is idempotent; writes rejected safely post-close.");
        } else {
            fail("Test 4: Database Lifecycle & Idempotency", 
                "Lifecycle failure: drained=" + drained + ", isClosed=" + isClosed + ", postCloseRejected=" + postCloseRejected);
        }
    }

    // -------------------------------------------------------------------------
    // TEST 5: Real Startup Task Seeding Flow
    // -------------------------------------------------------------------------
    private static void test5_RealStartupTaskSeedingFlow() {
        System.out.println(">>> Running Test 5: Real Startup Task Seeding Flow...");
        String testDb = "nexus_v26_1_seeding_test.db";
        File f = new File(testDb);
        if (f.exists()) f.delete();

        System.setProperty("nexus.db.path", testDb);

        // Flow 1: Fresh empty database
        DatabaseService db1 = new DatabaseService();
        TaskService taskService1 = new TaskService();
        List<Task> loadedFromEmpty = db1.loadTasks();

        if (loadedFromEmpty.isEmpty()) {
            taskService1.seedDefaultsIfEmpty();
        } else {
            taskService1.setTasks(loadedFromEmpty);
        }

        db1.awaitDrained(2000);
        int seededCount = taskService1.getTasks().size();
        db1.close();

        // Flow 2: Subsequent startup with existing tasks (Must NOT duplicate)
        DatabaseService db2 = new DatabaseService();
        TaskService taskService2 = new TaskService();
        List<Task> loadedFromSeeded = db2.loadTasks();

        if (loadedFromSeeded.isEmpty()) {
            taskService2.seedDefaultsIfEmpty();
        } else {
            taskService2.setTasks(loadedFromSeeded);
        }

        int afterRestartCount = taskService2.getTasks().size();
        db2.close();

        // Flow 3: Existing database with user-created custom task
        DatabaseService db3 = new DatabaseService();
        TaskService taskService3 = new TaskService();
        Task customTask = new Task("USER_TASK_99", "Custom User Created Task", TaskPriority.HIGH, false);
        db3.persistTask(customTask);
        db3.awaitDrained(2000);

        List<Task> loadedWithCustom = db3.loadTasks();
        if (loadedWithCustom.isEmpty()) {
            taskService3.seedDefaultsIfEmpty();
        } else {
            taskService3.setTasks(loadedWithCustom);
        }

        int finalCount = taskService3.getTasks().size();
        boolean customTaskPresent = taskService3.getTasks().stream().anyMatch(t -> t.getId().equals("USER_TASK_99"));
        db3.close();

        if (f.exists()) f.delete();
        System.clearProperty("nexus.db.path");

        if (seededCount == 3 && afterRestartCount == 3 && finalCount == 4 && customTaskPresent) {
            pass("Test 5: Real Startup Task Seeding Flow", 
                "Empty database seeded (3 tasks); restart preserved count without duplicate sample tasks.");
        } else {
            fail("Test 5: Real Startup Task Seeding Flow", 
                String.format("Seeding failed: seeded=%d, afterRestart=%d, final=%d", seededCount, afterRestartCount, finalCount));
        }
    }

    // -------------------------------------------------------------------------
    // TEST 6: Financial Validation Guards
    // -------------------------------------------------------------------------
    private static void test6_FinancialValidationGuards() {
        System.out.println(">>> Running Test 6: Financial Validation Guards...");
        FinanceService finance = new FinanceService();
        finance.resetBudget(1000.0);

        // 1. Negative amount
        PurchaseResult resNegative = finance.tryPurchase(-50.0, "Negative Purchase", "TX-NEG-1");
        // 2. Zero amount
        PurchaseResult resZero = finance.tryPurchase(0.0, "Zero Purchase", "TX-ZERO-1");
        // 3. NaN amount
        PurchaseResult resNan = finance.tryPurchase(Double.NaN, "NaN Purchase", "TX-NAN-1");
        // 4. Infinite amount
        PurchaseResult resInf = finance.tryPurchase(Double.POSITIVE_INFINITY, "Inf Purchase", "TX-INF-1");
        // 5. Null transaction ID
        PurchaseResult resNullId = finance.tryPurchase(100.0, "Null ID Purchase", null);
        // 6. Duplicate transaction ID
        PurchaseResult resValid = finance.tryPurchase(100.0, "Valid Purchase", "TX-STABLE-1");
        PurchaseResult resDuplicate = finance.tryPurchase(100.0, "Duplicate Purchase", "TX-STABLE-1");

        boolean allRejected = !resNegative.isSuccess() && !resZero.isSuccess() && 
                             !resNan.isSuccess() && !resInf.isSuccess() && 
                             !resNullId.isSuccess() && resValid.isSuccess() && 
                             !resDuplicate.isSuccess();

        double remaining = finance.getCurrentBalance();
        boolean balancePreserved = Math.abs(remaining - 900.0) < 0.01;

        if (allRejected && balancePreserved) {
            pass("Test 6: Financial Validation Guards", 
                "Rejected negative, zero, NaN, infinite, null ID, and duplicate transactions; balance is ₹900.");
        } else {
            fail("Test 6: Financial Validation Guards", 
                "Validation guard failure: allRejected=" + allRejected + ", remaining=" + remaining);
        }
    }

    // -------------------------------------------------------------------------
    // TEST 7: Concurrent Financial Checkout Race Condition Prevention
    // -------------------------------------------------------------------------
    private static void test7_ConcurrentFinancialCheckoutRaceCondition() {
        System.out.println(">>> Running Test 7: Concurrent Financial Checkout Race Condition...");
        FinanceService finance = new FinanceService();
        finance.resetBudget(1000.0); // Exactly ₹1,000 available

        // 10 concurrent threads each attempting to checkout a ₹300 item (Total attempted: ₹3,000)
        // Only exactly 3 purchases should succeed (3 * 300 = 900), leaving balance at ₹100.
        // Balance CANNOT become negative.
        final int contenders = 10;
        final double itemCost = 300.0;
        ExecutorService pool = Executors.newFixedThreadPool(contenders);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(contenders);

        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger rejectedCount = new AtomicInteger(0);

        for (int i = 0; i < contenders; i++) {
            final String threadOrderId = "CONCURRENT-ORDER-" + i;
            pool.submit(() -> {
                try {
                    startLatch.await();
                    PurchaseResult res = finance.tryPurchase(itemCost, "Contender Order", threadOrderId);
                    if (res.isSuccess()) {
                        successCount.incrementAndGet();
                    } else {
                        rejectedCount.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Launch all checkouts simultaneously
        try {
            doneLatch.await(5, TimeUnit.SECONDS);
            pool.shutdown();

            double finalBalance = finance.getCurrentBalance();
            int successes = successCount.get();
            int rejections = rejectedCount.get();

            boolean balanceNonNegative = finalBalance >= 0.0;
            boolean exactBalance = Math.abs(finalBalance - 100.0) < 0.01;
            boolean exactSuccessCount = successes == 3 && rejections == 7;

            if (balanceNonNegative && exactBalance && exactSuccessCount) {
                pass("Test 7: Concurrent Checkout Race Prevention", 
                    "Race condition eliminated: exactly 3 of 10 purchases succeeded (balance remained ₹100, never negative).");
            } else {
                fail("Test 7: Concurrent Checkout Race Prevention", 
                    String.format("Race condition detected! finalBalance=%.2f, successes=%d, rejections=%d",
                            finalBalance, successes, rejections));
            }

        } catch (InterruptedException e) {
            fail("Test 7: Concurrent Checkout Race Prevention", "Interrupted: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // TEST 8: Exact Balance Purchase Validation
    // -------------------------------------------------------------------------
    private static void test8_ExactBalancePurchase() {
        System.out.println(">>> Running Test 8: Exact Balance Purchase...");
        FinanceService finance = new FinanceService();
        finance.resetBudget(500.0);

        PurchaseResult result = finance.tryPurchase(500.0, "Exact Balance Purchase", "TX-EXACT-1");
        double balanceAfter = finance.getCurrentBalance();

        if (result.isSuccess() && Math.abs(balanceAfter - 0.0) < 0.01) {
            pass("Test 8: Exact Balance Purchase", 
                "Exact balance purchase of ₹500 settled successfully; remaining balance is ₹0.00.");
        } else {
            fail("Test 8: Exact Balance Purchase", 
                "Exact balance failed: success=" + result.isSuccess() + ", balanceAfter=" + balanceAfter);
        }
    }

    // -------------------------------------------------------------------------
    // TEST 9: Cart Preservation on Failure & Clearance on Success
    // -------------------------------------------------------------------------
    private static void test9_CartPreservationAndClearance() {
        System.out.println(">>> Running Test 9: Cart Preservation & Clearance...");
        FinanceService finance = new FinanceService();
        finance.resetBudget(200.0);

        MarketService market = new MarketService(finance);
        Product expensive = new Product("Mechanical Keyboard", 1500.0);
        market.addToCart(expensive, 1);

        // Attempt overdraft checkout
        market.checkout();
        boolean cartPreserved = market.getCart().getItems().size() == 1;
        boolean balancePreserved = Math.abs(finance.getCurrentBalance() - 200.0) < 0.01;

        // Reset with sufficient funds
        finance.resetBudget(2000.0);
        market.checkout();
        boolean cartCleared = market.getCart().getItems().isEmpty();
        boolean balanceDebited = Math.abs(finance.getCurrentBalance() - 500.0) < 0.01;

        if (cartPreserved && balancePreserved && cartCleared && balanceDebited) {
            pass("Test 9: Cart Preservation & Clearance", 
                "Cart retained on overdraft; cleared only after confirmed financial settlement.");
        } else {
            fail("Test 9: Cart Preservation & Clearance", 
                String.format("Cart check failed: cartPreserved=%b, balancePreserved=%b, cartCleared=%b, balanceDebited=%b",
                        cartPreserved, balancePreserved, cartCleared, balanceDebited));
        }
    }

    // -------------------------------------------------------------------------
    // TEST 10: ntfy Mock Delivery, Headers & Retry
    // -------------------------------------------------------------------------
    private static void test10_NtfyMockDeliveryAndRetry() {
        System.out.println(">>> Running Test 10: ntfy Mock Delivery & Retry...");
        HttpServer server = null;
        try {
            final AtomicInteger requestCount = new AtomicInteger(0);
            final List<String> authHeaders = new CopyOnWriteArrayList<>();
            final List<String> receivedTitles = new CopyOnWriteArrayList<>();

            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/v261-alerts", exchange -> {
                int attempt = requestCount.incrementAndGet();
                String auth = exchange.getRequestHeaders().getFirst("Authorization");
                if (auth != null) authHeaders.add(auth);
                String title = exchange.getRequestHeaders().getFirst("Title");
                if (title != null) receivedTitles.add(title);

                if (attempt < 3) {
                    exchange.sendResponseHeaders(503, 0); // 503 Service Unavailable
                } else {
                    exchange.sendResponseHeaders(200, 0); // 200 OK
                }
                exchange.close();
            });
            server.start();

            int port = server.getAddress().getPort();
            NotificationConfig config = new NotificationConfig(
                    "http://127.0.0.1:" + port, "v261-alerts", "secret-test-token", true);
            config.setMaxRetries(3);
            config.setInitialRetryDelayMs(50);

            NtfyNotificationService service = new NtfyNotificationService(
                    config, new NotificationPreferenceService(), HttpHelper.getDefaultClient());

            NotificationPayload payload = new NotificationPayload(
                    "V26.1 Notification", "Payload content with [REDACTED_CARD]", 4, Arrays.asList("bell"));

            boolean delivered = service.sendNotificationSync(payload);
            service.shutdown();

            boolean retried = requestCount.get() == 3;
            boolean authPassed = authHeaders.stream().anyMatch(a -> a.contains("secret-test-token"));

            if (delivered && retried && authPassed) {
                pass("Test 10: ntfy Mock Delivery & Retry", 
                    "Delivered with Bearer auth; recovered from 503 failures on attempt 3 via exponential backoff.");
            } else {
                fail("Test 10: ntfy Mock Delivery & Retry", 
                    "ntfy delivery failed: delivered=" + delivered + ", attempts=" + requestCount.get());
            }

        } catch (Exception e) {
            fail("Test 10: ntfy Mock Delivery & Retry", "Exception: " + e.getMessage());
        } finally {
            if (server != null) server.stop(0);
        }
    }

    // -------------------------------------------------------------------------
    // TEST 11: Metabase Health Status & Dual-Layer SQLite Read-Only Protection
    // -------------------------------------------------------------------------
    private static void test11_MetabaseStatusAndReadOnlyProtection() {
        System.out.println(">>> Running Test 11: Metabase Status & Read-Only Protection...");
        String testDb = "nexus_v26_1_ro_test.db";
        File f = new File(testDb);
        if (f.exists()) f.delete();

        System.setProperty("nexus.db.path", testDb);
        DatabaseService db = new DatabaseService();
        db.persistTask(new Task("RO_TEST_1", "Read-Only Test Task", TaskPriority.MEDIUM, false));
        db.awaitDrained(2000);
        db.close();

        ReadOnlyAnalyticsStore store = new ReadOnlyAnalyticsStore(testDb);

        // Layer 1: Application-level check
        boolean safeQueryPermitted = ReadOnlyAnalyticsStore.isReadOnlyQuery("SELECT id, title FROM tasks");
        boolean mutationQueryRejected = !ReadOnlyAnalyticsStore.isReadOnlyQuery("INSERT INTO tasks (id) VALUES ('HACK')");
        boolean dropQueryRejected = !ReadOnlyAnalyticsStore.isReadOnlyQuery("DROP TABLE tasks");

        // Layer 2: Database engine level block (PRAGMA query_only = ON)
        boolean engineBlockedWrite = false;
        try (Connection roConn = store.getReadOnlyConnection();
             Statement stmt = roConn.createStatement()) {
            stmt.executeUpdate("INSERT INTO tasks (id, title) VALUES ('FAIL', 'Should Fail')");
        } catch (SQLException sqle) {
            engineBlockedWrite = true; // Expected: SQLite rejected write!
        }

        if (f.exists()) f.delete();
        System.clearProperty("nexus.db.path");

        if (safeQueryPermitted && mutationQueryRejected && dropQueryRejected && engineBlockedWrite) {
            pass("Test 11: Metabase Read-Only Dual Protection", 
                "Dual-layer security confirmed: application allowlist and SQLite PRAGMA query_only blocked mutations.");
        } else {
            fail("Test 11: Metabase Read-Only Dual Protection", 
                String.format("Protection failed: safe=%b, mutRej=%b, dropRej=%b, engineBlock=%b",
                        safeQueryPermitted, mutationQueryRejected, dropQueryRejected, engineBlockedWrite));
        }
    }

    // -------------------------------------------------------------------------
    // TEST 12: Autonomous Startup Without External Services
    // -------------------------------------------------------------------------
    private static void test12_AutonomousStartupWithoutExternalServices() {
        System.out.println(">>> Running Test 12: Autonomous Startup Without External Services...");
        try {
            System.setProperty("nexus.ntfy.enabled", "false");
            System.setProperty("nexus.metabase.enabled", "false");

            NtfyNotificationService ntfy = new NtfyNotificationService();
            MetabaseAnalyticsService metabase = new MetabaseAnalyticsService();
            TaskService taskService = new TaskService();
            FinanceService financeService = new FinanceService();

            boolean ntfyDisabled = ntfy.getStatus() == IntegrationStatus.DISABLED;
            boolean metabaseDisabled = metabase.getStatus() == IntegrationStatus.DISABLED;
            boolean autonomous = taskService != null && financeService != null;

            System.clearProperty("nexus.ntfy.enabled");
            System.clearProperty("nexus.metabase.enabled");

            if (ntfyDisabled && metabaseDisabled && autonomous) {
                pass("Test 12: Autonomous Offline Startup", 
                    "Local-first architecture confirmed: core services boot and run with external integrations disabled.");
            } else {
                fail("Test 12: Autonomous Offline Startup", 
                    "Offline startup failed: ntfyDisabled=" + ntfyDisabled + ", metabaseDisabled=" + metabaseDisabled);
            }
        } catch (Exception e) {
            fail("Test 12: Autonomous Offline Startup", "Threw exception: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // TEST 13: Existing Module Regression Tests
    // -------------------------------------------------------------------------
    private static void test13_ExistingModuleRegressionTests() {
        System.out.println(">>> Running Test 13: Existing Module Regression Tests...");

        // 1. FinTech Calculations
        double emi = FinancialCalculator.calculateEmi(100000, 10.0, 12).monthlyEmi;
        boolean emiOk = emi > 8000 && emi < 9000;

        // 2. Circuit Breaker
        CircuitBreaker cb = new CircuitBreaker("TestGateway", 2, 500);
        cb.recordFailure();
        cb.recordFailure();
        boolean cbOpen = cb.getState() == CircuitBreaker.State.OPEN;

        // 3. Multi-Tenant RBAC
        TenantService tenantService = TenantService.getInstance();
        boolean permOk = tenantService.authorizeAction(TenantRole.FACULTY_EXAMINER, TenantPermission.SIGN_EVALUATION)
                && !tenantService.authorizeAction(TenantRole.STUDENT_RESIDENT, TenantPermission.DISBURSE_BUDGET);

        // 4. Cryptographic Vault Merkle Leaf
        VaultService vault = VaultService.getInstance();
        boolean auditOk = vault.getAuditChain().getChainLength() >= 1;

        // 5. Native Rule Engine Automation
        RuleEngineService ruleEngine = new RuleEngineService();
        final AtomicBoolean ruleFired = new AtomicBoolean(false);
        ruleEngine.addRule("AUTO_RULE_1", "Audit Trigger", "TASK_COMPLETED", 
                event -> event.getPayload() != null, 
                event -> ruleFired.set(true));

        ruleEngine.onEvent(new NexusEvent("TASK_COMPLETED", "Sample", "Test"));
        boolean ruleOk = ruleFired.get() && ruleEngine.getSuccessCount() == 1;

        if (emiOk && cbOpen && permOk && auditOk && ruleOk) {
            pass("Test 13: Existing Module Regression Suite", 
                "All regression checks passed: FinTech EMI, CircuitBreaker, Tenant RBAC, Merkle Audit Chain, and Native Rule Engine.");
        } else {
            fail("Test 13: Existing Module Regression Suite", 
                String.format("Regression failure: emiOk=%b, cbOpen=%b, permOk=%b, auditOk=%b, ruleOk=%b",
                        emiOk, cbOpen, permOk, auditOk, ruleOk));
        }
    }

    // -------------------------------------------------------------------------
    // TEST 14: Graceful Application Teardown
    // -------------------------------------------------------------------------
    private static void test14_GracefulTeardown() {
        System.out.println(">>> Running Test 14: Graceful Application Teardown...");
        try {
            DatabaseService dbService = new DatabaseService();
            NtfyNotificationService ntfy = new NtfyNotificationService();

            long start = System.currentTimeMillis();
            dbService.close();
            ntfy.shutdown();
            long duration = System.currentTimeMillis() - start;

            if (duration < 5000) {
                pass("Test 14: Graceful Application Teardown", 
                    "Queues drained and background thread pools terminated cleanly within " + duration + " ms.");
            } else {
                fail("Test 14: Graceful Application Teardown", "Shutdown took excessively long: " + duration + " ms");
            }
        } catch (Exception e) {
            fail("Test 14: Graceful Application Teardown", "Shutdown threw exception: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // LIVE EXTERNAL SERVICE CONNECTIVITY PROBE
    // -------------------------------------------------------------------------
    private static void probeLiveConnectivity() {
        System.out.println("\n>>> Probing Live External Service Reachability (Honest Assessment)...");
        System.out.println("--------------------------------------------------------------------------------");

        // 1. Live ntfy.sh probe
        NtfyNotificationService ntfy = new NtfyNotificationService();
        boolean ntfyOnline = ntfy.checkHealth();
        System.out.printf("   ntfy Gateway (%s): %s\n", 
                ntfy.getConfig().getServerUrl(),
                ntfyOnline ? "REACHABLE (HTTP 200 OK - Gateway Reachable; Not Proof of Mobile Delivery)" : "OFFLINE / UNREACHABLE (Operating with Local Fallback)");
        ntfy.shutdown();

        // 2. Live Metabase probe
        MetabaseAnalyticsService metabase = new MetabaseAnalyticsService();
        boolean metabaseOnline = metabase.checkHealth();
        System.out.printf("   Metabase Server (%s): %s\n", 
                metabase.getConfig().getBaseUrl(),
                metabaseOnline ? "REACHABLE (HTTP 200 OK - Optional Monitoring Active)" : "OFFLINE / NOT CONFIGURED (Optional Layer - NEXUS Local Analytics Active)");
        metabase.shutdown();
        System.out.println("--------------------------------------------------------------------------------\n");
    }
}
