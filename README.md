# NEXUS V26 — Enterprise External Integration & Core Reliability Platform

![Java Version](https://img.shields.io/badge/Java-21%20LTS-orange)
![UI](https://img.shields.io/badge/UI-FlatDarkLaf%203.5-blue)
![Database](https://img.shields.io/badge/Database-SQLite%20WAL-green)
![Integrations](https://img.shields.io/badge/Integrations-ntfy%20%7C%20Metabase-purple)
![Architecture](https://img.shields.io/badge/Architecture-Decoupled%20EventBus%20Pub%2FSub-critical)

NEXUS V26 is an enterprise-grade autonomous operating environment and campus management platform built in pure Java 21 LTS with FlatLaf dark desktop ergonomics. V26 introduces enterprise external integrations with **ntfy** (asynchronous push notifications) and **Metabase** (self-hosted read-only business intelligence), coupled with comprehensive core reliability hardenings across event processing, financial checkout validation, and database persistence.

---

## 🌟 What's New in V26

### 1. External Integration A — ntfy Push Notifications (`nexus.integrations.notifications`)
- **Native Standard HTTP Client**: Powered by Java 21 `java.net.http.HttpClient` with zero third-party networking bloat.
- **Asynchronous Non-Blocking Dispatch**: Fully isolated from the Swing Event Dispatch Thread (EDT) via a dedicated background daemon worker pool.
- **Exponential Backoff Retry**: Automatic resilient retry engine (3 attempts, doubling backoff) for transient network or HTTP 5xx errors.
- **Automatic PII & Credential Sanitization**: Regex scrubbing removes credit cards, API keys, and sensitive tokens before publication.
- **Dynamic Event Subscriptions**: Subscribes natively to:
  - `TASK_COMPLETED`
  - `GOAL_COMPLETED`
  - `BUDGET_UPDATED`
  - `PRODUCT_PURCHASED`
  - `TICKET_BOOKED`
- **User Preference Service**: Granular event filtering, muting, and priority thresholds.

### 2. External Integration B — Metabase Self-Hosted Analytics (`nexus.integrations.analytics`)
- **Zero-Mutation Read-Only Architecture**: Enforced at the SQLite engine level using `PRAGMA query_only = ON`. Metabase cannot alter or delete business data.
- **5 Configured Executive Dashboards**:
  1. *Financial Overview*: Liquidity splits, expenditures, category breakdown.
  2. *Monthly Spending*: Time-series spending trajectory across monthly calendar cycles.
  3. *Task Completion Trends*: Velocity breakdown by priority and category.
  4. *Goal Progression*: Milestone completion rates and deadline health.
  5. *Productivity Overview*: Multi-module progress and execution index.
- **Local Fallback & Graceful Degradation**: Metabase is 100% optional. If Metabase is offline or unreachable, NEXUS logs the state cleanly and operates normally with built-in native analytics.

### 3. Native NEXUS Automation Engine (`nexus.rules`)
- **Strict n8n Exclusion**: All event handling, trigger evaluation, and action execution remain 100% native to NEXUS.
- **Typed Predicate Conditions**: Rules evaluate `Predicate<NexusEvent>` conditions before triggering actions.
- **Execution Audit History**: Thread-safe bounded history log recording execution durations, success states, and failure stacks.
- **Per-Rule Failure Tracking**: Diagnostic counters for pinpointing intermittent execution faults.

### 4. Core Reliability & Data Integrity Hardening
- **EventBus**: Upgraded with `ConcurrentHashMap` and `CopyOnWriteArrayList`, strict deduplication, listener lifecycle management (`unsubscribe`), and per-listener exception isolation.
- **DatabaseService**: Event pre-filtering whitelist rejects non-persistable events before queue submission; multi-statement operations run in atomic transactions with rollback protection; graceful shutdown ensures write queues are drained prior to connection closure.
- **TaskService**: Sample task duplication on restart is completely eliminated. Tasks are seeded only when the database is completely empty, preserving user tasks intact.
- **FinanceService & MarketService**: Financial checkout validation strictly prevents purchases exceeding available budgets; checkout rollbacks retain cart contents on failure; atomic check-and-debit prevents race conditions and deduplication guards prevent double-billing while preserving the XP discount system (1,000 XP = 5%, 5,000 XP = 15%).
- **AnalyticsService**: Stripped hardcoded misleading values; derived metrics clearly distinguish verified measurements (`[REAL METRIC]`) from projections (`[SIMULATED]`).

---

## 🏗️ System Architecture

```
                                  +-----------------------------+
                                  |      NEXUS Application      |
                                  |   (Java 21 / FlatDarkLaf)   |
                                  +--------------+--------------+
                                                 |
                                     Pub/Sub Event Bus (Thread-Safe)
                     +---------------------------+---------------------------+
                     |                           |                           |
         +-----------v-----------+   +-----------v-----------+   +-----------v-----------+
         |    Core Subsystems    |   |   Native Automation   |   | External Integrations |
         | Tasks, Finance, Goals |   |   RuleEngineService   |   |   ntfy & Metabase     |
         | Market, Media, Auth   |   |  (No n8n Dependency)  |   | (Async / Non-Blocking)|
         +-----------+-----------+   +-----------+-----------+   +-----------+-----------+
                     |                           |                           |
                     +---------------------------+---------------------------+
                                                 |
                                  +--------------v--------------+
                                  |     DatabaseService (WAL)   |
                                  |  (Pre-Filtered / ACID Tx)   |
                                  +--------------+--------------+
                                                 |
                                         [ nexus.db ]
```

---

## 🚀 Getting Started

### Prerequisites
- **Java SE Development Kit 21** or later (`javac`, `java` available on PATH).
- **SQLite JDBC Driver** (bundled in `lib/`).
- **FlatLaf 3.5 Look & Feel** (bundled in `lib/`).

### Launching the Application
Run the one-click launcher batch script:
```cmd
V26_Launch.bat
```
Or run directly via command line:
```cmd
java -cp "bin;lib/*" nexus.NexusApplication
```

### Running the 12-Factor Verification Suite
NEXUS V26 includes an automated test runner validating all 12 core reliability and integration criteria:
```cmd
V26_Verify.bat
```
Or execute directly:
```cmd
java -cp "bin;lib/*" nexus.V26IntegrationVerificationRunner
```

---

## ⚙️ Configuration

Integrations can be customized via JVM system properties, environment variables, or `nexus.properties`:

| Configuration Key | Env Variable | Default | Purpose |
|-------------------|--------------|---------|---------|
| `nexus.ntfy.server` | `NEXUS_NTFY_SERVER` | `https://ntfy.sh` | ntfy server URL |
| `nexus.ntfy.topic` | `NEXUS_NTFY_TOPIC` | `nexus-alerts` | ntfy push notification topic |
| `nexus.ntfy.token` | `NEXUS_NTFY_TOKEN` | *(empty)* | Optional Bearer auth token for private ntfy instances |
| `nexus.ntfy.enabled` | `NEXUS_NTFY_ENABLED` | `true` | Enable/disable ntfy push notifications |
| `nexus.metabase.baseUrl` | `NEXUS_METABASE_BASEURL` | `http://localhost:3000` | Metabase server URL |
| `nexus.metabase.apiKey` | `NEXUS_METABASE_APIKEY` | *(empty)* | Optional Metabase API token |
| `nexus.metabase.enabled` | `NEXUS_METABASE_ENABLED` | `true` | Enable/disable Metabase analytics integration |

---

## 🧪 Verification & Acceptance Results

All 12 automated verification tests passed with 100% success:

```
[PASS] Test 1: EventBus Concurrent Publishing           : 1000 events / 10 concurrent threads (Zero concurrency exceptions)
[PASS] Test 2: Duplicate Listener Registration          : 5 registrations deduped to 1 invocation
[PASS] Test 3: Database Persistence Across Restarts     : Persisted & reloaded across simulated JVM restart
[PASS] Test 4: Task Seeding Behavior                    : Empty DB seeded; existing user records preserved without duplicates
[PASS] Test 5: Budget Enforcement During Checkout       : Overdraft rejected, cart retained, balance preserved
[PASS] Test 6: Duplicate Transaction Prevention         : Duplicate transaction IDs ignored, balance protected
[PASS] Test 7: Notification Delivery (ntfy)             : Validated HTTP delivery with sanitized payload and tags
[PASS] Test 8: Notification Retry After Failure         : 2 initial 503 failures recovered via exponential backoff
[PASS] Test 9: Metabase Unavailable Scenario            : Offline service handled gracefully with zero crashes
[PASS] Test 10: Application Startup Without External Services: 100% autonomous local-first startup verified
[PASS] Test 11: Existing Module Regression Tests        : FinTech, Circuit Breakers, RBAC, Merkle Chain, and Rules verified
[PASS] Test 12: Graceful Application Shutdown           : Background thread pools and queues cleanly drained
```

---

## 📄 License
Academic & Open Source Project for SRM Institute of Science and Technology. All rights reserved.
