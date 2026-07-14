package com.frauddemo.fraudmcp.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddemo.fraudmcp.model.SimulationReport;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Dry-runs a candidate rule (never stored) against the bundled synthetic
 * historical dataset -- a Java port of tools/simulate.py. Shares the exact
 * same fixture file as the Python implementation for comparable results.
 */
@Service
public class SimulationService {

    private final ObjectMapper objectMapper;
    private List<Map<String, Object>> transactions;

    public SimulationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private synchronized List<Map<String, Object>> loadTransactions() {
        if (transactions == null) {
            try (InputStream is = getClass().getResourceAsStream("/data/synthetic_transactions.json")) {
                transactions = objectMapper.readValue(is, new TypeReference<>() {
                });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return transactions;
    }

    public SimulationReport simulate(String condition, String action, String name) {
        ConditionNode conditionJson = ConditionDslParser.parse(condition);
        List<Map<String, Object>> txns = loadTransactions();

        int blocked = 0;
        int falsePositives = 0;
        double fraudPrevented = 0.0;

        for (Map<String, Object> txn : txns) {
            boolean matched = ConditionTreeEvaluator.evaluate(conditionJson, txn).matched();
            if (!matched) {
                continue;
            }
            blocked++;
            boolean isFraud = Boolean.TRUE.equals(txn.get("is_fraud"));
            if (isFraud) {
                fraudPrevented += ((Number) txn.get("amount")).doubleValue();
            } else {
                falsePositives++;
            }
        }

        return new SimulationReport(
                txns.size(), blocked, falsePositives, Math.round(fraudPrevented * 100.0) / 100.0,
                name, condition, action
        );
    }
}
