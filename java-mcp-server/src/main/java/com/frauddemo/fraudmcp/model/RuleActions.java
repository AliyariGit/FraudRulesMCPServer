package com.frauddemo.fraudmcp.model;

import java.util.Map;
import java.util.Set;

/** Rule action vocabulary and default severity weights, mirroring
 * models/schemas.py's Action literal and DEFAULT_SEVERITY_WEIGHT. */
public final class RuleActions {

    public static final String DECLINE = "DECLINE";
    public static final String REVIEW = "REVIEW";
    public static final String ALERT = "ALERT";
    public static final String HIGH_RISK = "HIGH_RISK";

    public static final Set<String> ALL = Set.of(DECLINE, REVIEW, ALERT, HIGH_RISK);

    private static final Map<String, Integer> DEFAULT_SEVERITY_WEIGHT = Map.of(
            DECLINE, 60,
            REVIEW, 30,
            ALERT, 30,
            HIGH_RISK, 20
    );

    private RuleActions() {
    }

    public static int defaultSeverityWeight(String action) {
        Integer weight = DEFAULT_SEVERITY_WEIGHT.get(action);
        if (weight == null) {
            throw new IllegalArgumentException("Unknown action: " + action + "; expected one of " + ALL);
        }
        return weight;
    }
}
