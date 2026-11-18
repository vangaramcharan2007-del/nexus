# NEXUS Platform (Production Core Edition)

**NEXUS** is an integrated desktop productivity, personal finance, and campus utility platform engineered in Java 21 and Swing, utilizing a reactive Pub/Sub EventBus architecture with SQLite ACID persistence.

---

## 🚀 Core Production Modules

NEXUS focuses on 6 end-to-end, fully functional core modules without simulated bloat:

1. **🏠 Executive Dashboard**
   - Live telemetry ribbon: Cognitive XP index, real-time weather & sensor metrics, active tasks/goals count, and liquidity runway.
   - Interactive system canvas with animated particle feedback responding to application events.
   - Global HUD stats widget row.

2. **💳 Smart Finance & Wealth Engine**
   - Full transaction ledger with category breakdowns (Food, Transport, Entertainment, Shopping, Education).
   - High-precision mathematical engines:
     - **Loan EMI Calculator**: $E = P \cdot r \cdot \frac{(1+r)^n}{(1+r)^n - 1}$
     - **Compound Interest**: $A = P \cdot (1 + \frac{r}{n})^{nt}$
     - **SIP & Wealth Growth**
     - **Discount & GST/Tax Net Engine**
   - Live cryptocurrency ticker via CoinGecko gateway.

3. **🛒 Campus Marketplace**
   - Product catalog with real-time E-Commerce search and curated campus inventory.
   - Shopping cart with quantity management, dynamic subtotals, and balance validation.
   - ACID-compliant purchase ledger entries saved to SQLite.

4. **🎬 Cinema & Entertainment Hub**
   - Interactive cinema booking matrix (Executive, Club, and VIP tiers).
   - Dynamic 2D barcode ticket generator (`TicketQrGenerator`) producing verified, scannable digital boarding passes.
   - In-app cinema trailer playback and IMDb curated entertainment picks.

5. **🎯 Strategic Goals & Tasks**
   - Milestone tracking with completion percentages and visual progress bars.
   - Full SQLite task management with priority tagging (High, Medium, Low) and deadline tracking.

6. **⚡ Cognitive XP & Analytics**
   - Gamified experience system tracking student productivity, velocity, and capital preservation ratio.
   - Animated SVG/Java2D bar charts and category distribution pie charts.
   - Synchronous diagnostic telemetry scan.

---

## 🛠️ Build and Execution

### Prerequisites
- **JDK 21** or later installed and configured on your `PATH`.
- Windows 10/11 (or cross-platform Java runtime).

### Commands

- **Build Application**:
  ```bat
  build.bat
  ```
- **Run Application**:
  ```bat
  start.bat
  ```
- **Run Automated Regression Tests**:
  ```bat
  test.bat
  ```

---

## 🔒 Security & Hardening Highlights
- **Real Password Authentication**: Replaced dummy bypass with authenticated credential validation in `AccountService`.
- **Zero Simulated Modules**: Stripped non-functional simulations (fake Web3 tokens, fake Kafka streams, fake AI fallbacks).
- **Safe Database Layer**: Thread-safe SQLite access in WAL (Write-Ahead Logging) mode using parameterized `PreparedStatement` queries.
- **Privacy Hardened**: Scrubbed hardcoded personal identification, hostel room addresses, and student registration numbers from source code.
