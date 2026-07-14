package com.frauddemo.fraudmcp.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.frauddemo.fraudmcp.engine.ConditionNode;
import com.frauddemo.fraudmcp.model.Rule;
import org.junit.jupiter.api.Test;

class LegacyRuleMigratorTest {

    @Test
    void migrateFraudFlagRule() {
        Rule rule = LegacyRuleMigrator.migrate("IF TX_AMT > 5000 AND CNTRY <> HOME_CNTRY SET FRAUD_FLAG=Y", null);

        assertThat(rule.getConditionDsl()).isEqualTo("amount > 5000 AND country <> customer.country");
        assertThat(rule.getAction()).isEqualTo("HIGH_RISK");
        assertThat(rule.getStatus()).isEqualTo("PENDING_APPROVAL");
        assertThat(rule.getSource()).isEqualTo("MIGRATED");
        assertThat(rule.getConditionJson().getAllOf().get(1).getValue())
                .isEqualTo(ConditionNode.fieldRef("customer.country"));
        assertThat(rule.getConditionJson().getAllOf().get(1).getOperator()).isEqualTo("!=");
    }

    @Test
    void migrateDeclineFlagRule() {
        Rule rule = LegacyRuleMigrator.migrate("IF DEVICE_RISK = HIGH SET DECLINE_FLAG=Y", null);

        assertThat(rule.getAction()).isEqualTo("DECLINE");
        assertThat(rule.getConditionDsl()).isEqualTo("deviceRisk = HIGH");
    }

    @Test
    void migrateReviewFlagByNameHeuristic() {
        Rule rule = LegacyRuleMigrator.migrate("IF CUST_AGE < 25 SET REVIEW_ALERT=Y", null);

        assertThat(rule.getAction()).isEqualTo("REVIEW");
    }

    @Test
    void malformedLegacyTextRaises() {
        assertThrows(LegacyRuleMigrator.LegacyMigrationException.class,
                () -> LegacyRuleMigrator.migrate("this is not a legacy rule", null));
    }

    @Test
    void customNameUsedWhenProvided() {
        Rule rule = LegacyRuleMigrator.migrate("IF TX_AMT > 100 SET FRAUD_FLAG=Y", "Custom Migrated Name");

        assertThat(rule.getName()).isEqualTo("Custom Migrated Name");
    }
}
