-- Seed the 5 starter rules, matching python-mcp-server/data/seed_rules.json.
-- condition_json keys are camelCase (allOf/anyOf/field/operator/value) since
-- this implementation serializes ConditionNode via Jackson's default naming,
-- unlike the Python side's snake_case pydantic field names.

INSERT INTO rules (name, condition_dsl, condition_json, action, severity_weight, priority, status, source) VALUES
('High International Transfer',
 'amount > 5000 AND country != customer.country',
 '{"allOf":[{"field":"amount","operator":">","value":5000},{"field":"country","operator":"!=","value":{"ref":"customer.country"}}]}',
 'HIGH_RISK', 20, 10, 'ACTIVE', 'MANUAL'),

('Crypto High Device Risk',
 'deviceRisk == HIGH AND merchant == CryptoExchange',
 '{"allOf":[{"field":"deviceRisk","operator":"==","value":"HIGH"},{"field":"merchant","operator":"==","value":"CryptoExchange"}]}',
 'DECLINE', 60, 5, 'ACTIVE', 'MANUAL'),

('Young Customer Large Amount',
 'customerAge < 25 AND amount > 10000',
 '{"allOf":[{"field":"customerAge","operator":"<","value":25},{"field":"amount","operator":">","value":10000}]}',
 'REVIEW', 30, 20, 'ACTIVE', 'MANUAL'),

('Very High Amount',
 'amount > 20000',
 '{"field":"amount","operator":">","value":20000}',
 'DECLINE', 60, 1, 'ACTIVE', 'MANUAL'),

('High Risk Device Moderate Amount',
 'deviceRisk == HIGH AND amount > 3000',
 '{"allOf":[{"field":"deviceRisk","operator":"==","value":"HIGH"},{"field":"amount","operator":">","value":3000}]}',
 'ALERT', 30, 30, 'ACTIVE', 'MANUAL');
