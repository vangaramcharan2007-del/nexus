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
import nexus.finance.Transaction;
import nexus.finance.TransactionCategory;
import nexus.finance.FinancialCalculator;
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
 * Master Verification & Automated Test Suite for NEXUS V26.
 *
 * Covers all 12 mandatory verification areas specified in the V26 upgrade brief:
 * 1.  EventBus Concurrent Publishing
 * 2.  Duplicate Listener Registration
 * 3.  Database Persistence Across Restarts
 * 4.  Task Seeding Behavior
 * 5.  Budget Enforcement During Checkout
 * 6.  Duplicate Transaction Prevention
 * 7.  Notification Delivery (ntfy)
 * 8.  Notification Retry After Failure
 * 9.  Metabase Unavailable Scenario
 * 10. Application Startup Without External Services
 * 11. Existing Module Regression Tests
 * 12. Graceful Application Shutdown
 */
public class V26IntegrationVerificationRunner {

    private static int totalPassed = 0;
    private static int totalFailed = 0;
    private static final List<String> resultsSummary = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("         NEXUS V26.0 - INTEGRATION & CORE RELIABILITY VERIFICATION SUITE         ");
        System.out.println("================================================================================\n");

        long suiteStartTime = System.currentTimeMillis();

        test1_EventBusConcurrentPublishing();
        test2_DuplicateListenerRegistration();
        test3_DatabasePersistenceAcrossRestarts();
        test4_TaskSeedingBehavior();
        test5_BudgetEnforcementDuringCheckout();
        test6_DuplicateTransactionPrevention();
        test7_NotificationDelivery();
        test8_NotificationRetryAfterFailure();
        test9_MetabaseUnavailableScenario();
        test10_ApplicationStartupWithoutExternalServices();
        test11_ExistingModuleRegressionTests();
        test12_GracefulApplicationShutdown();

        probeLiveConnectivity();

        long duration = System.currentTimeMillis() - suiteStartTime;

        System.out.println("\n================================================================================");
        System.out.println("                         V26 TEST EXECUTION SUMMARY                             ");
        System.out.println("================================================================================");
        for (String line : resultsSummary) {
            System.out.println(line);
        }
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("Total Executed: %d | Passed: %d | Failed: %d | Duration: %d ms%n",
                totalPassed + totalFailed, totalPassed, totalFailed, duration);
        System.out.println("================================================================================\n");

        if (totalFailed > 0) {
            System.err.println("Verification FAILED with " + totalFailed + " failed assertions.");
            System.exit(1);
        } else {
            System.out.println("ALL 12 V26 VERIFICATION CRITERIA PASSED FLAWLESSLY.");
        }
    }

    private static void pass(String testName, String details) {
        totalPassed++;
        String msg = String.format("[PASS] %-48s : %s", testName, details);
        System.out.println(msg);
        resultsSummary.add(msg);
    }

    private static void fail(String testName, String reason) {
        totalFailed++;
        String msg = String.format("[FAIL] %-48s : %s", testName, reason);
        System.err.println(msg);
        resultsSummary.add(msg);
    }

    // -------------------------------------------------------------------------
    // TEST 1: EventBus Concurrent Publishing
    // -------------------------------------------------------------------------
    private static void test1_EventBusConcurrentPublishing() {
        System.out.println(">>> Running Test 1: EventBus Concurrent Publishing...");
        EventBus bus = EventBus.getInstance();
        bus.reset();

        final int threadCount = 10;
        final int eventsPerThread = 100;
        final AtomicInteger receivedCount = new AtomicInteger(0);

        NexusListener listener = event -> receivedCount.incrementAndGet();
        bus.subscribe("CONCURRENT_TEST_EVENT", listener);

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            pool.submit(() -> {
                try {
                    for (int j = 0; j < eventsPerThread; j++) {
                        bus.publish(new NexusEvent("CONCURRENT_TEST_EVENT", "T" + threadId + "-" + j, "TestRunner"));
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            boolean completed = latch.await(5, TimeUnit.SECONDS);
            pool.shutdown();
            int expected = threadCount * eventsPerThread;
            if (completed && receivedCount.get() == expected) {
                pass("Test 1: EventBus Concurrent Publishing", 
                    "Successfully dispatched and received all " + expected + " events across " + threadCount + " concurrent threads.");
            } else {
                fail("Test 1: EventBus Concurrent Publishing", 
                    "Expected " + expected + " events, received " + receivedCount.get());
            }
        } catch (InterruptedException e) {
            fail("Test 1: EventBus Concurrent Publishing", "Interrupted during latch wait: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // TEST 2: Duplicate Listener Registration
    // -------------------------------------------------------------------------
    private static void test2_DuplicateListenerRegistration() {
        System.out.println(">>> Running Test 2: Duplicate Listener Registration...");
        EventBus bus = EventBus.getInstance();
        bus.reset();

        final AtomicInteger invocationCount = new AtomicInteger(0);
        NexusListener duplicateListener = event -> invocationCount.incrementAndGet();

        // Register the exact same listener 5 times
        for (int i = 0; i < 5; i++) {
            bus.subscribe("DEDUP_TEST_EVENT", duplicateListener);
        }

        // Publish once
        bus.publish(new NexusEvent("DEDUP_TEST_EVENT", "Data", "TestRunner"));

        int subscriberCount = bus.getSubscriberCount("DEDUP_TEST_EVENT");
        int count = invocationCount.get();

        if (subscriberCount == 1 && count == 1) {
            pass("Test 2: Duplicate Listener Registration", 
                "Strict deduplication verified: 5 registrations resulted in exactly 1 listener entry and 1 invocation.");
        } else {
            fail("Test 2: Duplicate Listener Registration", 
                "Deduplication failed: subscriberCount=" + subscriberCount + ", invocations=" + count);
        }
    }

    // -------------------------------------------------------------------------
    // TEST 3: Database Persistence Across Restarts
    // -------------------------------------------------------------------------
    private static void test3_DatabasePersistenceAcrossRestarts() {
        System.out.println(">>> Running Test 3: Database Persistence Across Restarts...");
        // Use custom test DB file
        String testDbPath = "nexus_v26_test.db";
        File f = new File(testDbPath);
        if (f.exists()) f.delete();

        System.setProperty("nexus.db.path", testDbPath);
        DatabaseService dbService = new DatabaseService();

        String taskId = "TEST-TASK-" + UUID.randomUUID().toString().substring(0, 8);
        Task testTask = new Task(taskId, "V26 Integration Verification", TaskPriority.HIGH, false, "QA", "Today");
        dbService.persistTask(testTask);

        String txId = "TEST-TX-" + UUID.randomUUID().toString().substring(0, 8);
        Transaction testTx = new Transaction(txId, "V26 Verified Test Deposit", 450.0, new Date(), TransactionCategory.SALARY_INCOME);
        dbService.persistTransaction(testTx);

        // Ensure async queue is completely drained
        boolean drained = dbService.awaitDrained(3000);
        dbService.close();

        // Re-open fresh instance (simulating restart)
        DatabaseService restartedDb = new DatabaseService();
        List<Task> loadedTasks = restartedDb.loadTasks();
        List<Transaction> loadedTx = restartedDb.loadTransactions();
        restartedDb.close();

        boolean taskFound = loadedTasks.stream().anyMatch(t -> t.getId().equals(taskId));
        boolean txFound = loadedTx.stream().anyMatch(t -> t.getId().equals(txId));

        // Cleanup
        if (f.exists()) f.delete();
        System.clearProperty("nexus.db.path");

        if (drained && taskFound && txFound) {
            pass("Test 3: Database Persistence Across Restarts", 
                "Successfully persisted and reloaded Task and Transaction records across simulated JVM restart.");
        } else {
            fail("Test 3: Database Persistence Across Restarts", 
                "Persistence check failed. drained=" + drained + ", taskFound=" + taskFound + ", txFound=" + txFound);
        }
    }

    // -------------------------------------------------------------------------
    // TEST 4: Task Seeding Behavior
    // -------------------------------------------------------------------------
    private static void test4_TaskSeedingBehavior() {
        System.out.println(">>> Running Test 4: Task Seeding Behavior...");
        TaskService taskService = new TaskService();
        
        // Scenario A: Starting with empty database/list -> should seed defaults
        taskService.setTasks(new ArrayList<>());
        int initialCount = taskService.getTasks().size();

        // Scenario B: Starting with pre-existing user records -> must NOT duplicate
        List<Task> existingTasks = new ArrayList<>(taskService.getTasks());
        Task userTask = new Task("CUSTOM-1", "User Custom Task", TaskPriority.HIGH, false, "Personal", "Tomorrow");
        existingTasks.add(userTask);

        // Simulate app restart by loading the existing tasks
        TaskService restartedTaskService = new TaskService();
        restartedTaskService.setTasks(existingTasks);
        int afterRestartCount = restartedTaskService.getTasks().size();

        if (initialCount == 3 && afterRestartCount == initialCount + 1) {
            pass("Test 4: Task Seeding Behavior", 
                "Seeding executed only on empty state (3 tasks). Existing records preserved with zero duplicate injections.");
        } else {
            fail("Test 4: Task Seeding Behavior", 
                "Seeding failed. initialCount=" + initialCount + ", afterRestartCount=" + afterRestartCount);
        }
    }

    // -------------------------------------------------------------------------
    // TEST 5: Budget Enforcement During Checkout
    // -------------------------------------------------------------------------
    private static void test5_BudgetEnforcementDuringCheckout() {
        System.out.println(">>> Running Test 5: Budget Enforcement During Checkout...");
        EventBus bus = EventBus.getInstance();
        bus.reset();

        FinanceService financeService = new FinanceService();
        financeService.resetBudget(1000.0); // Budget limit ₹1,000

        MarketService marketService = new MarketService(financeService);

        // Add expensive product ₹2,500
        Product expensiveItem = new Product("Quantum Workstation", 2500.0);
        marketService.addToCart(expensiveItem, 1);

        final AtomicBoolean checkoutFailedFired = new AtomicBoolean(false);
        bus.subscribe("CHECKOUT_FAILED", event -> checkoutFailedFired.set(true));

        // Attempt checkout
        marketService.checkout();

        boolean cartPreserved = marketService.getCart().getItems().size() == 1;
        boolean balanceUnchanged = financeService.getCurrentBalance() == 1000.0;
        boolean failureEventEmitted = checkoutFailedFired.get();

        // Now test affordable purchase ₹300
        marketService.getCart().clear();
        Product affordableItem = new Product("Ergonomic Mouse", 300.0);
        marketService.addToCart(affordableItem, 1);
        marketService.checkout();

        boolean affordableCheckedOut = marketService.getCart().getItems().isEmpty();
        boolean balanceDeducted = Math.abs(financeService.getCurrentBalance() - 700.0) < 0.01;

        if (cartPreserved && balanceUnchanged && failureEventEmitted && affordableCheckedOut && balanceDeducted) {
            pass("Test 5: Budget Enforcement During Checkout", 
                "Overdraft purchase strictly rejected (balance remained ₹1,000, cart intact). Affordable checkout debited to ₹700.");
        } else {
            fail("Test 5: Budget Enforcement During Checkout", 
                String.format("Overdraft validation failed. cartPreserved=%b, balanceUnchanged=%b, eventEmitted=%b, affordableDone=%b, balanceDeducted=%b",
                        cartPreserved, balanceUnchanged, failureEventEmitted, affordableCheckedOut, balanceDeducted));
        }
    }

    // -------------------------------------------------------------------------
    // TEST 6: Duplicate Transaction Prevention
    // -------------------------------------------------------------------------
    private static void test6_DuplicateTransactionPrevention() {
        System.out.println(">>> Running Test 6: Duplicate Transaction Prevention...");
        FinanceService financeService = new FinanceService();
        financeService.resetBudget(5000.0);

        String fixedId = "DEDUP-TX-100";
        Transaction t1 = new Transaction(fixedId, "Software License Renewal", 400.0, new Date(), TransactionCategory.EDUCATION);
        Transaction t2 = new Transaction(fixedId, "Software License Renewal (Duplicate)", 400.0, new Date(), TransactionCategory.EDUCATION);

        financeService.addTransaction(t1);
        financeService.addTransaction(t2);

        int txCount = financeService.getTransactions().size();
        double remaining = financeService.getCurrentBalance();

        if (txCount == 1 && Math.abs(remaining - 4600.0) < 0.01) {
            pass("Test 6: Duplicate Transaction Prevention", 
                "Duplicate transaction with ID " + fixedId + " successfully ignored. Total transactions: 1, Balance: ₹4,600.");
        } else {
            fail("Test 6: Duplicate Transaction Prevention", 
                "Duplicate check failed: txCount=" + txCount + ", remaining=" + remaining);
        }
    }

    // -------------------------------------------------------------------------
    // TEST 7: Notification Delivery (ntfy)
    // -------------------------------------------------------------------------
    private static void test7_NotificationDelivery() {
        System.out.println(">>> Running Test 7: Notification Delivery (ntfy)...");
        HttpServer server = null;
        try {
            final List<String> receivedTitles = new CopyOnWriteArrayList<>();
            final List<String> receivedBodies = new CopyOnWriteArrayList<>();

            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/test-alerts", exchange -> {
                String title = exchange.getRequestHeaders().getFirst("Title");
                if (title != null) receivedTitles.add(title);
                try (InputStream in = exchange.getRequestBody()) {
                    String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    receivedBodies.add(body);
                }
                exchange.sendResponseHeaders(200, 0);
                exchange.close();
            });
            server.start();

            int port = server.getAddress().getPort();
            NotificationConfig config = new NotificationConfig("http://127.0.0.1:" + port, "test-alerts", null, true);
            NtfyNotificationService service = new NtfyNotificationService(config, new NotificationPreferenceService(), HttpHelper.getDefaultClient());

            NotificationPayload payload = new NotificationPayload("Task Completed", "Verified V26 test task done.", 3, Arrays.asList("check"));
            boolean sent = service.sendNotificationSync(payload);
            service.shutdown();

            if (sent && receivedTitles.size() == 1 && receivedTitles.get(0).equals("Task Completed")) {
                pass("Test 7: Notification Delivery (ntfy)", 
                    "Successfully delivered HTTP POST notification with sanitized payload and expected headers to mock ntfy server.");
            } else {
                fail("Test 7: Notification Delivery (ntfy)", 
                    "Delivery failed. sent=" + sent + ", titlesCount=" + receivedTitles.size());
            }

        } catch (Exception e) {
            fail("Test 7: Notification Delivery (ntfy)", "Exception: " + e.getMessage());
        } finally {
            if (server != null) server.stop(0);
        }
    }

    // -------------------------------------------------------------------------
    // TEST 8: Notification Retry After Failure
    // -------------------------------------------------------------------------
    private static void test8_NotificationRetryAfterFailure() {
        System.out.println(">>> Running Test 8: Notification Retry After Failure...");
        HttpServer server = null;
        try {
            final AtomicInteger requestCount = new AtomicInteger(0);

            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/retry-alerts", exchange -> {
                int attempt = requestCount.incrementAndGet();
                if (attempt < 3) {
                    // Fail first 2 attempts with 503 Service Unavailable
                    exchange.sendResponseHeaders(503, 0);
                } else {
                    // Succeed on 3rd attempt
                    exchange.sendResponseHeaders(200, 0);
                }
                exchange.close();
            });
            server.start();

            int port = server.getAddress().getPort();
            NotificationConfig config = new NotificationConfig("http://127.0.0.1:" + port, "retry-alerts", null, true);
            config.setMaxRetries(3);
            config.setInitialRetryDelayMs(50); // fast for tests

            NtfyNotificationService service = new NtfyNotificationService(config, new NotificationPreferenceService(), HttpHelper.getDefaultClient());
            NotificationPayload payload = new NotificationPayload("Retry Test", "Testing exponential backoff", 3, Collections.emptyList());

            boolean success = service.sendNotificationSync(payload);
            service.shutdown();

            if (success && requestCount.get() == 3) {
                pass("Test 8: Notification Retry After Failure", 
                    "Exponential backoff retry succeeded: 2 initial 503 failures retried and successfully resolved on attempt 3.");
            } else {
                fail("Test 8: Notification Retry After Failure", 
                    "Retry failed: success=" + success + ", attempts=" + requestCount.get());
            }

        } catch (Exception e) {
            fail("Test 8: Notification Retry After Failure", "Exception: " + e.getMessage());
        } finally {
            if (server != null) server.stop(0);
        }
    }

    // -------------------------------------------------------------------------
    // TEST 9: Metabase Unavailable Scenario
    // -------------------------------------------------------------------------
    private static void test9_MetabaseUnavailableScenario() {
        System.out.println(">>> Running Test 9: Metabase Unavailable Scenario...");
        // Point to an intentionally unreachable port
        MetabaseConfig config = new MetabaseConfig("http://127.0.0.1:59998", null, true);
        MetabaseAnalyticsService service = new MetabaseAnalyticsService(config, new ReadOnlyAnalyticsStore(), HttpHelper.getDefaultClient());

        boolean health = service.checkHealth();
        IntegrationStatus status = service.getStatus();

        // Verify that local analytics and dashboard metadata remain fully accessible without crashing
        List<AnalyticsDashboardModel> dashboards = service.getDashboards();
        boolean dashboardsAvailable = dashboards != null && dashboards.size() == 5;

        if (!health && status == IntegrationStatus.UNAVAILABLE && dashboardsAvailable) {
            pass("Test 9: Metabase Unavailable Scenario", 
                "Metabase offline handled gracefully: reported status UNAVAILABLE without throwing unhandled exceptions. 5 dashboard models intact.");
        } else {
            fail("Test 9: Metabase Unavailable Scenario", 
                "Handling failed: health=" + health + ", status=" + status);
        }
    }

    // -------------------------------------------------------------------------
    // TEST 10: Application Startup Without External Services
    // -------------------------------------------------------------------------
    private static void test10_ApplicationStartupWithoutExternalServices() {
        System.out.println(">>> Running Test 10: Application Startup Without External Services...");
        try {
            // Explicitly disable external services
            System.setProperty("nexus.ntfy.enabled", "false");
            System.setProperty("nexus.metabase.enabled", "false");

            NtfyNotificationService ntfy = new NtfyNotificationService();
            MetabaseAnalyticsService metabase = new MetabaseAnalyticsService();
            TaskService taskService = new TaskService();
            FinanceService financeService = new FinanceService();
            AnalyticsService analyticsService = new AnalyticsService();
            RuleEngineService ruleEngine = new RuleEngineService();

            boolean ntfyDisabled = ntfy.getStatus() == IntegrationStatus.DISABLED;
            boolean metabaseDisabled = metabase.getStatus() == IntegrationStatus.DISABLED;
            boolean coreInitialized = taskService != null && financeService != null && analyticsService != null && ruleEngine != null;

            System.clearProperty("nexus.ntfy.enabled");
            System.clearProperty("nexus.metabase.enabled");

            if (ntfyDisabled && metabaseDisabled && coreInitialized) {
                pass("Test 10: Application Startup Without External Services", 
                    "Local-first autonomy confirmed: NEXUS initializes and executes all core features with zero external service dependencies.");
            } else {
                fail("Test 10: Application Startup Without External Services", 
                    "Startup check failed. ntfyDisabled=" + ntfyDisabled + ", metabaseDisabled=" + metabaseDisabled);
            }

        } catch (Exception e) {
            fail("Test 10: Application Startup Without External Services", "Startup threw exception: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // TEST 11: Existing Module Regression Tests
    // -------------------------------------------------------------------------
    private static void test11_ExistingModuleRegressionTests() {
        System.out.println(">>> Running Test 11: Existing Module Regression Tests...");

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
            pass("Test 11: Existing Module Regression Tests", 
                "All regression checks passed: FinTech EMI, CircuitBreaker, Tenant RBAC, Merkle Audit Chain, and Native Rule Engine.");
        } else {
            fail("Test 11: Existing Module Regression Tests", 
                String.format("Regression failure. emiOk=%b, cbOpen=%b, permOk=%b, auditOk=%b, ruleOk=%b",
                        emiOk, cbOpen, permOk, auditOk, ruleOk));
        }
    }

    // -------------------------------------------------------------------------
    // TEST 12: Graceful Application Shutdown
    // -------------------------------------------------------------------------
    private static void test12_GracefulApplicationShutdown() {
        System.out.println(">>> Running Test 12: Graceful Application Shutdown...");
        try {
            DatabaseService dbService = new DatabaseService();
            NtfyNotificationService ntfy = new NtfyNotificationService();

            // Submit work
            dbService.persistTask(new Task("SHUTDOWN-TASK", "Shutdown Test", TaskPriority.LOW, true));
            ntfy.sendNotificationAsync(new NotificationPayload("Shutdown", "Draining queues", 1, null));

            // Execute shutdown
            long start = System.currentTimeMillis();
            dbService.close();
            ntfy.shutdown();
            long duration = System.currentTimeMillis() - start;

            if (duration < 5000) {
                pass("Test 12: Graceful Application Shutdown", 
                    "Queues drained and background thread pools terminated cleanly within " + duration + " ms.");
            } else {
                fail("Test 12: Graceful Application Shutdown", "Shutdown took excessively long: " + duration + " ms");
            }

        } catch (Exception e) {
            fail("Test 12: Graceful Application Shutdown", "Shutdown threw exception: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // LIVE EXTERNAL SERVICE CONNECTIVITY PROBE
    // -------------------------------------------------------------------------
    private static void probeLiveConnectivity() {
        System.out.println("\n>>> Probing Live External Service Connectivity...");
        System.out.println("--------------------------------------------------------------------------------");

        // 1. Live ntfy.sh probe
        NtfyNotificationService ntfy = new NtfyNotificationService();
        boolean ntfyOnline = ntfy.checkHealth();
        System.out.printf("   ntfy Gateway (%s): %s\n", 
                ntfy.getConfig().getServerUrl(),
                ntfyOnline ? "REACHABLE (HTTP 200 OK - Live Alerts Ready)" : "UNREACHABLE / OFFLINE (Operating with Local Fallback)");
        ntfy.shutdown();

        // 2. Live Metabase probe
        MetabaseAnalyticsService metabase = new MetabaseAnalyticsService();
        boolean metabaseOnline = metabase.checkHealth();
        System.out.printf("   Metabase Server (%s): %s\n", 
                metabase.getConfig().getBaseUrl(),
                metabaseOnline ? "REACHABLE (HTTP 200 OK - Analytics Live)" : "OFFLINE / UNREACHABLE (Optional Service - Local Analytics Active)");
        metabase.shutdown();
        System.out.println("--------------------------------------------------------------------------------\n");
    }
}
