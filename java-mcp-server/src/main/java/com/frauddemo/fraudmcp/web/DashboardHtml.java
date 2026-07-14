package com.frauddemo.fraudmcp.web;

import com.frauddemo.fraudmcp.model.EvaluationEntity;
import com.frauddemo.fraudmcp.model.Rule;
import java.util.List;
import java.util.Map;
import org.springframework.web.util.HtmlUtils;

/** Minimal server-rendered HTML dashboard for "/" -- a Java port of
 * python-mcp-server/dashboard.py, same styling for visual parity between
 * the two implementations. */
final class DashboardHtml {

    private static final Map<String, String> DECISION_COLORS = Map.of(
            "DECLINE", "#dc2626", "REVIEW", "#d97706", "APPROVE", "#16a34a"
    );
    private static final Map<String, String> ACTION_COLORS = Map.of(
            "DECLINE", "#dc2626", "REVIEW", "#d97706", "ALERT", "#2563eb", "HIGH_RISK", "#9333ea"
    );
    private static final Map<String, String> STATUS_COLORS = Map.of(
            "ACTIVE", "#16a34a", "PENDING_APPROVAL", "#d97706", "DISABLED", "#6b7280"
    );

    private static final String STYLE = """
            <style>
              body { font-family: -apple-system, Segoe UI, Roboto, sans-serif; margin: 2rem;
                     background: #0b0f19; color: #e5e7eb; }
              h1 { font-size: 1.4rem; margin-bottom: 0.25rem; }
              p.sub { color: #9ca3af; margin-top: 0; margin-bottom: 1.5rem; }
              h2 { font-size: 1.05rem; margin-top: 2rem; border-bottom: 1px solid #374151; padding-bottom: 0.4rem; }
              table { border-collapse: collapse; width: 100%; margin-top: 0.75rem; font-size: 0.85rem; }
              th, td { text-align: left; padding: 0.4rem 0.6rem; border-bottom: 1px solid #1f2937; vertical-align: top; }
              th { color: #9ca3af; font-weight: 600; text-transform: uppercase; font-size: 0.72rem; letter-spacing: 0.03em; }
              code { background: #1f2937; padding: 0.1rem 0.35rem; border-radius: 4px; font-size: 0.8rem; }
              .badge { display: inline-block; padding: 0.1rem 0.5rem; border-radius: 999px; font-size: 0.72rem;
                       font-weight: 600; color: white; }
              .empty { color: #6b7280; font-style: italic; padding: 0.75rem 0; }
              a { color: #60a5fa; }
              @media (prefers-color-scheme: light) {
                body { background: #f9fafb; color: #111827; }
                th { color: #6b7280; }
                th, td { border-bottom: 1px solid #e5e7eb; }
                code { background: #f3f4f6; }
              }
            </style>
            """;

    private DashboardHtml() {
    }

    private static String badge(String value, Map<String, String> colors) {
        String color = colors.getOrDefault(value, "#6b7280");
        return "<span class=\"badge\" style=\"background:" + color + "\">" + HtmlUtils.htmlEscape(value) + "</span>";
    }

    private static String rulesTable(List<Rule> rules) {
        if (rules.isEmpty()) {
            return "<p class=\"empty\">No rules yet.</p>";
        }
        StringBuilder rows = new StringBuilder();
        rules.stream().sorted((a, b) -> Long.compare(a.getId(), b.getId())).forEach(r -> rows
                .append("<tr><td>").append(r.getId()).append("</td><td>")
                .append(HtmlUtils.htmlEscape(r.getName())).append("</td><td><code>")
                .append(HtmlUtils.htmlEscape(r.getConditionDsl() != null ? r.getConditionDsl() : "(LLM/migrated -- see conditionJson)"))
                .append("</code></td><td>").append(badge(r.getAction(), ACTION_COLORS))
                .append("</td><td>").append(r.getSeverityWeight())
                .append("</td><td>").append(r.getPriority())
                .append("</td><td>").append(badge(r.getStatus(), STATUS_COLORS))
                .append("</td><td>").append(HtmlUtils.htmlEscape(r.getSource())).append("</td></tr>"));

        return "<table><thead><tr><th>ID</th><th>Name</th><th>Condition</th><th>Action</th>"
                + "<th>Weight</th><th>Priority</th><th>Status</th><th>Source</th></tr></thead>"
                + "<tbody>" + rows + "</tbody></table>";
    }

    private static String evaluationsTable(List<EvaluationEntity> evaluations) {
        if (evaluations.isEmpty()) {
            return "<p class=\"empty\">No transactions evaluated yet -- POST to /evaluate.</p>";
        }
        StringBuilder rows = new StringBuilder();
        for (EvaluationEntity e : evaluations) {
            String matched = e.getMatchedRules().isEmpty() ? "&mdash;" : String.join(", ", e.getMatchedRules());
            rows.append("<tr><td><code>").append(HtmlUtils.htmlEscape(e.getTransactionId())).append("</code></td><td>")
                    .append(badge(e.getDecision(), DECISION_COLORS)).append("</td><td>")
                    .append(e.getRiskScore()).append("</td><td>").append(matched).append("</td><td>")
                    .append(HtmlUtils.htmlEscape(e.getReason())).append("</td><td>")
                    .append(e.getEvaluatedAt()).append("</td></tr>");
        }
        return "<table><thead><tr><th>Transaction</th><th>Decision</th><th>Risk</th>"
                + "<th>Matched Rules</th><th>Reason</th><th>Evaluated At</th></tr></thead>"
                + "<tbody>" + rows + "</tbody></table>";
    }

    static String render(List<Rule> rules, List<EvaluationEntity> evaluations) {
        return "<!doctype html><html><head><meta charset=\"utf-8\"><title>Fraud Rules MCP Server (Java)</title>"
                + STYLE + "</head><body>"
                + "<h1>Fraud Rules MCP Server (Java / Spring Boot)</h1>"
                + "<p class=\"sub\">Live view of stored rules and recent transaction evaluations</p>"
                + "<h2>Rules (" + rules.size() + ")</h2>" + rulesTable(rules)
                + "<h2>Recent Evaluations (" + evaluations.size() + ")</h2>" + evaluationsTable(evaluations)
                + "</body></html>";
    }
}
