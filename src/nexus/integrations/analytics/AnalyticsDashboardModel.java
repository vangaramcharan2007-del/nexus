package nexus.integrations.analytics;

import java.util.Arrays;
import java.util.List;

/**
 * Data model for Metabase dashboards in NEXUS V26.1.
 *
 * Explicitly distinguishes real persisted metrics computed locally in NEXUS
 * from external Metabase dashboards and projections.
 */
public class AnalyticsDashboardModel {
    public enum MetricType {
        NEXUS_LOCAL_PERSISTED,
        EXTERNAL_METABASE_SYNC,
        SIMULATED_PROJECTION
    }

    private final int id;
    private final String name;
    private final String description;
    private final String category; // "Financial" or "Productivity"
    private final MetricType metricType;
    private final String slug;
    private final String primaryQuery;

    public AnalyticsDashboardModel(int id, String name, String description, String category, MetricType metricType, String slug, String primaryQuery) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.metricType = metricType;
        this.slug = slug;
        this.primaryQuery = primaryQuery;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public MetricType getMetricType() { return metricType; }
    public String getSlug() { return slug; }
    public String getPrimaryQuery() { return primaryQuery; }

    public boolean isRealMetric() {
        return metricType == MetricType.NEXUS_LOCAL_PERSISTED;
    }

    /**
     * Factory generating the 5 mandatory V26.1 analytical dashboard specifications.
     * Accurately labeled as NEXUS Local Analytics specifications configured for Metabase export.
     */
    public static List<AnalyticsDashboardModel> getDefaultDashboards() {
        return Arrays.asList(
            // 1. Financial Overview
            new AnalyticsDashboardModel(
                1,
                "Financial Overview (Local Analytics)",
                "Executive breakdown of total budget, expenditures, remaining liquidity, and category splits computed from SQLite.",
                "Financial",
                MetricType.NEXUS_LOCAL_PERSISTED,
                "financial-overview",
                "SELECT category, SUM(amount) AS total_spent, COUNT(*) AS tx_count FROM transactions GROUP BY category ORDER BY total_spent DESC;"
            ),
            // 2. Monthly Spending
            new AnalyticsDashboardModel(
                2,
                "Monthly Spending Trajectory",
                "Time-series spending trajectory across monthly calendar cycles derived from recorded transactions.",
                "Financial",
                MetricType.NEXUS_LOCAL_PERSISTED,
                "monthly-spending",
                "SELECT strftime('%Y-%m', date / 1000, 'unixepoch') AS month, SUM(amount) AS total FROM transactions WHERE amount > 0 GROUP BY month ORDER BY month ASC;"
            ),
            // 3. Task Completion Trends
            new AnalyticsDashboardModel(
                3,
                "Task Completion Velocity",
                "Cumulative productivity velocity and completion ratios partitioned by priority and category.",
                "Productivity",
                MetricType.NEXUS_LOCAL_PERSISTED,
                "task-completion-trends",
                "SELECT priority, completed, COUNT(*) as count FROM tasks GROUP BY priority, completed;"
            ),
            // 4. Goal Progression
            new AnalyticsDashboardModel(
                4,
                "Goal & Milestone Progression",
                "Strategic objective health and milestone completion rates tracked from database goals.",
                "Productivity",
                MetricType.NEXUS_LOCAL_PERSISTED,
                "goal-progression",
                "SELECT g.title, g.category, COUNT(m.id) AS total_milestones, SUM(m.completed) AS completed_milestones FROM goals g LEFT JOIN milestones m ON g.id = m.goal_id GROUP BY g.id;"
            ),
            // 5. Productivity Overview
            new AnalyticsDashboardModel(
                5,
                "Productivity & Operations Index",
                "Composite executive index comparing verified task completions against strategic goals and media finishes.",
                "Productivity",
                MetricType.NEXUS_LOCAL_PERSISTED,
                "productivity-overview",
                "SELECT (SELECT COUNT(*) FROM tasks WHERE completed = 1) AS tasks_done, (SELECT COUNT(*) FROM goals) AS goals_active, (SELECT COUNT(*) FROM media_items WHERE status = 'COMPLETED') AS media_finished;"
            )
        );
    }
}
