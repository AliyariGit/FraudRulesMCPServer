package com.frauddemo.fraudmcp.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.frauddemo.fraudmcp.model.EvaluationResult;
import com.frauddemo.fraudmcp.model.Rule;
import com.frauddemo.fraudmcp.model.RuleActions;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RuleEngineTest {

    private static Rule makeRule(long id, String name, String dsl, String action, int priority) {
        Rule rule = new Rule();
        rule.setId(id);
        rule.setName(name);
        rule.setConditionDsl(dsl);
        rule.setConditionJson(ConditionDslParser.parse(dsl));
        rule.setAction(action);
        rule.setSeverityWeight(RuleActions.defaultSeverityWeight(action));
        rule.setPriority(priority);
        rule.setStatus("ACTIVE");
        return rule;
    }

    private static final List<Rule> RULES = List.of(
            makeRule(1, "High International Transfer", "amount > 5000 AND country != customer.country", RuleActions.HIGH_RISK, 100),
            makeRule(2, "Crypto High Device Risk", "deviceRisk == HIGH AND merchant == CryptoExchange", RuleActions.DECLINE, 100),
            makeRule(3, "Young Customer Large Amount", "customerAge < 25 AND amount > 10000", RuleActions.REVIEW, 100)
    );

    private static Map<String, Object> txn(String id, double amount, String country, String merchant,
                                            int age, String deviceRisk, String homeCountry) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("transactionId", id);
        map.put("amount", amount);
        map.put("country", country);
        map.put("merchant", merchant);
        map.put("customerAge", age);
        map.put("deviceRisk", deviceRisk);
        map.put("customer", Map.of("country", homeCountry));
        return map;
    }

    @Test
    void declineTransactionMatchesMultipleRules() {
        Map<String, Object> txn = txn("TX12345", 8500, "CA", "CryptoExchange", 22, "HIGH", "US");
        EvaluationResult result = RuleEngine.evaluateRules(txn, RULES);

        assertThat(result.getDecision()).isEqualTo("DECLINE");
        assertThat(result.getRiskScore()).isEqualTo(80); // 20 (HIGH_RISK) + 60 (DECLINE)
        assertThat(result.getMatchedRules()).contains("RULE-001", "RULE-002").doesNotContain("RULE-003");
        assertThat(result.getReason()).contains("Device risk was HIGH");
    }

    @Test
    void cleanTransactionIsApproved() {
        Map<String, Object> txn = txn("TX99999", 50, "CA", "GroceryStore", 40, "LOW", "CA");
        EvaluationResult result = RuleEngine.evaluateRules(txn, RULES);

        assertThat(result.getDecision()).isEqualTo("APPROVE");
        assertThat(result.getRiskScore()).isEqualTo(0);
        assertThat(result.getMatchedRules()).isEmpty();
    }

    @Test
    void reviewOnlyTransaction() {
        Map<String, Object> txn = txn("TX55555", 15000, "CA", "Electronics", 20, "LOW", "CA");
        EvaluationResult result = RuleEngine.evaluateRules(txn, RULES);

        assertThat(result.getDecision()).isEqualTo("REVIEW");
        assertThat(result.getRiskScore()).isEqualTo(30);
        assertThat(result.getMatchedRules()).containsExactly("RULE-003");
    }

    @Test
    void riskScoreCappedAt100() {
        List<Rule> rules = new java.util.ArrayList<>(RULES);
        rules.add(makeRule(4, "Extra Decline A", "amount > 1", RuleActions.DECLINE, 100));
        rules.add(makeRule(5, "Extra Decline B", "amount > 1", RuleActions.DECLINE, 100));

        Map<String, Object> txn = txn("TX1", 8500, "CA", "CryptoExchange", 22, "HIGH", "US");
        EvaluationResult result = RuleEngine.evaluateRules(txn, rules);

        assertThat(result.getRiskScore()).isEqualTo(100);
    }
}
