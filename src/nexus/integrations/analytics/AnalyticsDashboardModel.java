package nexus.integrations.analytics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Data model for Metabase dashboards in NEXUS V26.
 *
 * Explicitly distinguishes real persisted metrics from simulated or projected data.
 */
public class AnalyticsDashboardModel {
    public enum MetricType {
        REAL_PERSISTED,
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
        return metricType == MetricType.REAL_PERSISTED;
    }

    /**
     * Factory generating the 5 mandatory V26 Metabase dashboards.
     */
    public static List<AnalyticsDashboardModel> getDefaultDashboards() {
        return Arrays.asList(
            // 1. Financial Overview
            new AnalyticsDashboardModel(
                1,
                "Financial Overview",
                "Executive breakdown of total budget, expenditures, remaining liquidity, and category splits.",
                "Financial",
                MetricType.REAL_PERSISTED,
                "financial-overview",
                "SELECT category, SUM(amount) AS total_spent, COUNT(*) AS tx_count FROM transactions GROUP BY category ORDER BY total_spent DESC;"
            ),
            // 2. Monthly Spending
            new AnalyticsDashboardModel(
                2,
                "Monthly Spending",
                "Time-series spending trajectory across monthly calendar cycles.",
                "Financial",
                MetricType.REAL_PERSISTED,
                "monthly-spending",
                "SELECT strftime('%Y-%m', date / 1000, 'unixepoch') AS month, SUM(amount) AS total FROM transactions WHERE amount > 0 GROUP BY month ORDER BY month ASC;"
            ),
            // 3. Task Completion Trends
            new AnalyticsDashboardModel(
                3,
                "Task Completion Trends",
                "Cumulative productivity velocity, task completion velocity by category and priority.",
                "Productivity",
                MetricType.REAL_PERSISTED,
                "task-completion-trends",
                "SELECT priority, completed, COUNT(*) as count FROM tasks GROUP BY priority, completed;"
            ),
            // 4. Goal Progression
            new AnalyticsDashboardModel(
                4,
                "Goal Progression",
                "Milestone achievement tracking, strategic objective health, and deadlines.",
                "Productivity",
                MetricType.REAL_PERSISTED,
                "goal-progression",
                "SELECT g.title, g.category, COUNT(m.id) AS total_milestones, SUM(m.completed) AS completed_milestones FROM goals g LEFT JOIN milestones m ON g.id = m.goal_id GROUP BY g.id;"
            ),
            // 5. Productivity Overview
            new AnalyticsDashboardModel(
                5,
                "Productivity Overview",
                "Composite executive index comparing real task velocity against projected target velocity.",
                "Productivity",
                MetricType.REAL_PERSISTED,
                "productivity-overview",
                "SELECT (SELECT COUNT(*) FROM tasks WHERE completed = 1) AS tasks_done, (SELECT COUNT(*) FROM goals) AS goals_active, (SELECT COUNT(*) FROM media_items WHERE status = 'COMPLETED') AS media_finished;"
            )
        );
    }
}
