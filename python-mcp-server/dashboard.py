"""Minimal server-rendered HTML dashboard for the FastAPI wrapper's "/" route --
lets a human glance at current rules and recent evaluations in a browser
instead of raw JSON.
"""
from __future__ import annotations

import html

from models.schemas import EvaluationResult, Rule

_DECISION_COLORS = {"DECLINE": "#dc2626", "REVIEW": "#d97706", "APPROVE": "#16a34a"}
_ACTION_COLORS = {
    "DECLINE": "#dc2626", "REVIEW": "#d97706", "ALERT": "#2563eb", "HIGH_RISK": "#9333ea"
}
_STATUS_COLORS = {"ACTIVE": "#16a34a", "PENDING_APPROVAL": "#d97706", "DISABLED": "#6b7280"}

_STYLE = """
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
"""


def _badge(value: str, colors: dict[str, str]) -> str:
    color = colors.get(value, "#6b7280")
    return f'<span class="badge" style="background:{color}">{html.escape(value)}</span>'


def _rules_table(rules: list[Rule]) -> str:
    if not rules:
        return '<p class="empty">No rules yet.</p>'
    rows = []
    for r in sorted(rules, key=lambda r: r.id or 0):
        rows.append(
            f"<tr><td>{r.id}</td><td>{html.escape(r.name)}</td>"
            f"<td><code>{html.escape(r.condition_dsl or '(LLM/migrated -- see condition_json)')}</code></td>"
            f"<td>{_badge(r.action, _ACTION_COLORS)}</td>"
            f"<td>{r.severity_weight}</td><td>{r.priority}</td>"
            f"<td>{_badge(r.status, _STATUS_COLORS)}</td><td>{html.escape(r.source)}</td></tr>"
        )
    return (
        "<table><thead><tr><th>ID</th><th>Name</th><th>Condition</th><th>Action</th>"
        "<th>Weight</th><th>Priority</th><th>Status</th><th>Source</th></tr></thead>"
        f"<tbody>{''.join(rows)}</tbody></table>"
    )


def _evaluations_table(evaluations: list[dict]) -> str:
    if not evaluations:
        return '<p class="empty">No transactions evaluated yet -- POST to /evaluate.</p>'
    rows = []
    for e in evaluations:
        matched = ", ".join(e["matched_rules"]) or "&mdash;"
        rows.append(
            f"<tr><td><code>{html.escape(e['transaction_id'])}</code></td>"
            f"<td>{_badge(e['decision'], _DECISION_COLORS)}</td>"
            f"<td>{e['risk_score']}</td><td>{matched}</td>"
            f"<td>{html.escape(e['reason'])}</td>"
            f"<td>{e['evaluated_at']}</td></tr>"
        )
    return (
        "<table><thead><tr><th>Transaction</th><th>Decision</th><th>Risk</th>"
        "<th>Matched Rules</th><th>Reason</th><th>Evaluated At</th></tr></thead>"
        f"<tbody>{''.join(rows)}</tbody></table>"
    )


def render_dashboard(rules: list[Rule], evaluations: list[dict]) -> str:
    return f"""<!doctype html>
<html><head><meta charset="utf-8"><title>Fraud Rules MCP Server</title>{_STYLE}</head>
<body>
<h1>Fraud Rules MCP Server</h1>
<p class="sub">Live view of stored rules and recent transaction evaluations &middot;
<a href="/docs">API docs (Swagger)</a></p>

<h2>Rules ({len(rules)})</h2>
{_rules_table(rules)}

<h2>Recent Evaluations ({len(evaluations)})</h2>
{_evaluations_table(evaluations)}
</body></html>"""
