package com.frauddemo.fraudmcp.migration;

import com.frauddemo.fraudmcp.engine.ConditionDslParser;
import com.frauddemo.fraudmcp.engine.ConditionNode;
import com.frauddemo.fraudmcp.model.Rule;
import com.frauddemo.fraudmcp.model.RuleActions;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Deterministic migration of legacy COBOL-style fraud rule text into the
 * modern structured rule format. Regex + lookup tables only (no LLM call):
 * migration correctness for a fraud/compliance engine must be auditable and
 * reproducible, not probabilistic. A direct port of rules/legacy_migration.py.
 *
 * Example legacy input:
 *   IF TX_AMT > 5000 AND CNTRY <> HOME_CNTRY SET FRAUD_FLAG=Y
 * becomes the modern DSL:
 *   amount > 5000 AND country <> customer.country
 * with action HIGH_RISK.
 */
public final class LegacyRuleMigrator {

    private static final Map<String, String> FIELD_ALIASES = Map.of(
            "TX_AMT", "amount",
            "CNTRY", "country",
            "HOME_CNTRY", "customer.country",
            "DEVICE_RISK", "deviceRisk",
            "MERCH", "merchant",
            "CUST_AGE", "customerAge",
            "TXN_ID", "transactionId"
    );

    private static final Map<String, String> FLAG_ACTION_MAP = Map.of(
            "FRAUD_FLAG", RuleActions.HIGH_RISK,
            "DECLINE_FLAG", RuleActions.DECLINE,
            "REVIEW_FLAG", RuleActions.REVIEW,
            "ALERT_FLAG", RuleActions.ALERT
    );

    private static final Pattern LEGACY_PATTERN = Pattern.compile(
            "^\\s*IF\\s+(.+?)\\s+SET\\s+(\\w+)\\s*=\\s*(\\w+)\\s*$", Pattern.CASE_INSENSITIVE
    );

    private static final Pattern FIELD_TOKEN_PATTERN = Pattern.compile(
            "\\b(" + FIELD_ALIASES.keySet().stream().map(Pattern::quote).collect(Collectors.joining("|")) + ")\\b"
    );

    private LegacyRuleMigrator() {
    }

    public static class LegacyMigrationException extends RuntimeException {
        public LegacyMigrationException(String message) {
            super(message);
        }
    }

    private static String inferAction(String flagName) {
        String upper = flagName.toUpperCase();
        if (FLAG_ACTION_MAP.containsKey(upper)) {
            return FLAG_ACTION_MAP.get(upper);
        }
        if (upper.contains("DECLINE")) {
            return RuleActions.DECLINE;
        }
        if (upper.contains("REVIEW")) {
            return RuleActions.REVIEW;
        }
        if (upper.contains("ALERT")) {
            return RuleActions.ALERT;
        }
        return RuleActions.HIGH_RISK;
    }

    /** Replaces legacy field tokens with their modern equivalents; legacy
     * operators ({@code <>}, {@code =}) pass through unchanged since
     * ConditionDslParser already accepts them as aliases. */
    public static String translateLegacyCondition(String legacyCondition) {
        Matcher matcher = FIELD_TOKEN_PATTERN.matcher(legacyCondition);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(result, FIELD_ALIASES.get(matcher.group(1).toUpperCase()));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public static Rule migrate(String legacyText, String name) {
        Matcher matcher = LEGACY_PATTERN.matcher(legacyText.trim());
        if (!matcher.matches()) {
            throw new LegacyMigrationException(
                    "Legacy rule text did not match the expected "
                            + "'IF <conditions> SET <FLAG>=<value>' shape: " + legacyText
            );
        }

        String modernDsl = translateLegacyCondition(matcher.group(1));
        ConditionNode conditionJson;
        try {
            conditionJson = ConditionDslParser.parse(modernDsl);
        } catch (ConditionDslParser.ConditionParseException ex) {
            throw new LegacyMigrationException("Could not migrate condition: " + ex.getMessage());
        }

        String action = inferAction(matcher.group(2));
        String ruleName = (name != null && !name.isBlank()) ? name : "Migrated Legacy Rule (" + matcher.group(2) + ")";

        Rule rule = new Rule();
        rule.setName(ruleName);
        rule.setConditionDsl(modernDsl);
        rule.setConditionJson(conditionJson);
        rule.setAction(action);
        rule.setSeverityWeight(RuleActions.defaultSeverityWeight(action));
        rule.setStatus("PENDING_APPROVAL");
        rule.setSource("MIGRATED");
        return rule;
    }
}
