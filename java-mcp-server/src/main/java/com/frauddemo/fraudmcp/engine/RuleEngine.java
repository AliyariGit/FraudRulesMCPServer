package com.frauddemo.fraudmcp.engine;

import com.frauddemo.fraudmcp.model.EvaluationResult;
import com.frauddemo.fraudmcp.model.Rule;
import com.frauddemo.fraudmcp.model.RuleMatch;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Deterministic rule scoring/decision logic -- a direct port of rules/engine.py. */
public final class RuleEngine {

    private RuleEngine() {
    }

    private static final Map<String, Function<ConditionTrace, String>> FIELD_DESCRIPTIONS = Map.of(
            "amount", t -> "Amount (" + t.getActual() + ") " + t.getOperator() + " threshold (" + t.getExpected() + ")",
            "deviceRisk", t -> "Device risk was " + t.getActual(),
            "merchant", t -> "Merchant category was " + t.getActual(),
            "country", t -> "!=".equals(t.getOperator())
                    ? "Transaction country (" + t.getActual() + ") differs from customer's home country (" + t.getExpected() + ")"
                    : "Transaction country (" + t.getActual() + ") " + t.getOperator() + " " + t.getExpected(),
            "customerAge", t -> "Customer age (" + t.getActual() + ") " + t.getOperator() + " " + t.getExpected(),
            "transactionId", t -> "Transaction ID " + t.getOperator() + " " + t.getExpected(),
            "customer.country", t -> "Customer home country (" + t.getActual() + ") " + t.getOperator() + " " + t.getExpected()
    );

    public static List<String> describeMatchedLeaves(ConditionTrace trace) {
        List<String> descriptions = new ArrayList<>();
        if (trace.getCombinator() != null) {
            for (ConditionTrace child : trace.getChildren()) {
                descriptions.addAll(describeMatchedLeaves(child));
            }
            return descriptions;
        }
        if (!trace.isMatched()) {
            return descriptions;
        }
        Function<ConditionTrace, String> describe = FIELD_DESCRIPTIONS.getOrDefault(
                trace.getField(), t -> t.getField() + " " + t.getOperator() + " " + t.getExpected()
        );
        descriptions.add(describe.apply(trace));
        return descriptions;
    }

    public static EvaluationResult evaluateRules(Map<String, Object> txn, List<Rule> activeRules) {
        List<RuleMatch> matches = new ArrayList<>();
        List<String> reasons = new ArrayList<>();

        List<Rule> sorted = activeRules.stream()
                .sorted(Comparator.comparingInt(Rule::getPriority))
                .toList();

        for (Rule rule : sorted) {
            ConditionTreeEvaluator.Outcome outcome = ConditionTreeEvaluator.evaluate(rule.getConditionJson(), txn);
            if (!outcome.matched()) {
                continue;
            }
            matches.add(new RuleMatch(rule.getId(), rule.getName(), rule.getAction(), rule.getSeverityWeight(), outcome.trace()));
            reasons.addAll(describeMatchedLeaves(outcome.trace()));
        }

        int riskScore = Math.min(100, matches.stream().mapToInt(RuleMatch::getSeverityWeight).sum());

        Set<String> actions = new LinkedHashSet<>();
        matches.forEach(m -> actions.add(m.getAction()));

        String decision;
        if (actions.contains("DECLINE")) {
            decision = "DECLINE";
        } else if (actions.contains("REVIEW") || actions.contains("ALERT")) {
            decision = "REVIEW";
        } else {
            decision = "APPROVE";
        }

        String reason = reasons.isEmpty()
                ? "No fraud rules matched."
                : String.join("; ", new LinkedHashSet<>(reasons));

        List<String> matchedRuleLabels = matches.stream()
                .map(m -> m.getRuleId() != null ? String.format("RULE-%03d", m.getRuleId()) : m.getRuleName())
                .toList();

        return new EvaluationResult(
                (String) txn.get("transactionId"), decision, riskScore, matchedRuleLabels, reason, matches
        );
    }
}
