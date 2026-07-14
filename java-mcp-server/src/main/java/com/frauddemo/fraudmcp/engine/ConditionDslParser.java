package com.frauddemo.fraudmcp.engine;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Compiles the human-friendly rule DSL (e.g. "amount > 10000 AND country != CA")
 * into a structured ConditionNode tree. No eval() involved: a fixed field
 * whitelist, a fixed operator set, and simple string splitting/regex are the
 * entire parser -- a direct port of the Python rules/condition_dsl.py.
 */
public final class ConditionDslParser {

    public static final Set<String> ALLOWED_FIELDS = Set.of(
            "amount", "country", "merchant", "customerAge", "deviceRisk",
            "transactionId", "customer.country"
    );

    private static final List<String> OPERATORS = List.of(">=", "<=", "!=", "==", "<>", ">", "<", "=");

    private static final Pattern LEAF_PATTERN = Pattern.compile(
            "^\\s*([\\w.]+)\\s*(" +
                    OPERATORS.stream().map(Pattern::quote).collect(Collectors.joining("|")) +
                    ")\\s*(.+?)\\s*$"
    );

    private ConditionDslParser() {
    }

    public static class ConditionParseException extends RuntimeException {
        public ConditionParseException(String message) {
            super(message);
        }
    }

    private static String normalizeOperator(String operator) {
        return switch (operator) {
            case "=" -> "==";
            case "<>" -> "!=";
            default -> operator;
        };
    }

    private static List<String> splitTopLevel(String text, String keyword) {
        return Arrays.asList(text.split("(?i)\\s+" + keyword + "\\s+"));
    }

    private static Object coerceValue(String raw) {
        raw = raw.trim();
        if (raw.length() >= 2
                && raw.charAt(0) == raw.charAt(raw.length() - 1)
                && (raw.charAt(0) == '\'' || raw.charAt(0) == '"')) {
            return raw.substring(1, raw.length() - 1);
        }
        if (raw.equalsIgnoreCase("true") || raw.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(raw);
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            // not an int
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ignored) {
            // not a number either -- treat as a bare string token
        }
        return raw;
    }

    private static ConditionNode parseLeaf(String text) {
        Matcher matcher = LEAF_PATTERN.matcher(text);
        if (!matcher.matches()) {
            throw new ConditionParseException("Could not parse condition clause: " + text);
        }
        String field = matcher.group(1);
        String operator = normalizeOperator(matcher.group(2));
        String rawValue = matcher.group(3).trim();

        if (!ALLOWED_FIELDS.contains(field)) {
            throw new ConditionParseException(
                    "Unknown field '" + field + "'; allowed fields: " + ALLOWED_FIELDS
            );
        }

        Object value = ALLOWED_FIELDS.contains(rawValue)
                ? ConditionNode.fieldRef(rawValue)
                : coerceValue(rawValue);

        return ConditionNode.leaf(field, operator, value);
    }

    /**
     * Parses "A > 1 AND B != 2 OR C == 3" as (A>1 AND B!=2) OR (C==3): OR has
     * lower precedence than AND, matching standard boolean logic. No parens
     * in v1.
     */
    public static ConditionNode parse(String dsl) {
        if (dsl == null || dsl.isBlank()) {
            throw new ConditionParseException("Condition string is empty");
        }

        List<String> orGroups = splitTopLevel(dsl, "OR");
        List<ConditionNode> parsedGroups = orGroups.stream().map(group -> {
            List<String> andClauses = splitTopLevel(group, "AND");
            List<ConditionNode> leaves = andClauses.stream()
                    .map(ConditionDslParser::parseLeaf)
                    .collect(Collectors.toList());
            return leaves.size() == 1 ? leaves.get(0) : ConditionNode.allOf(leaves);
        }).collect(Collectors.toList());

        return parsedGroups.size() == 1 ? parsedGroups.get(0) : ConditionNode.anyOf(parsedGroups);
    }
}
