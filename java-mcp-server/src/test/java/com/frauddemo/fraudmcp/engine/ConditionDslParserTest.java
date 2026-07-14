package com.frauddemo.fraudmcp.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ConditionDslParserTest {

    @Test
    void singleLeaf() {
        ConditionNode node = ConditionDslParser.parse("amount > 10000");
        assertThat(node.getField()).isEqualTo("amount");
        assertThat(node.getOperator()).isEqualTo(">");
        assertThat(node.getValue()).isEqualTo(10000);
    }

    @Test
    void andChain() {
        ConditionNode node = ConditionDslParser.parse("amount > 10000 AND country != CA");
        assertThat(node.getAllOf()).hasSize(2);
        assertThat(node.getAllOf().get(0).getField()).isEqualTo("amount");
        assertThat(node.getAllOf().get(1).getField()).isEqualTo("country");
        assertThat(node.getAllOf().get(1).getValue()).isEqualTo("CA");
    }

    @Test
    void orChain() {
        ConditionNode node = ConditionDslParser.parse("deviceRisk == HIGH OR customerAge < 18");
        assertThat(node.getAnyOf()).hasSize(2);
    }

    @Test
    void andOrPrecedence() {
        ConditionNode node = ConditionDslParser.parse("amount > 5000 AND country != CA OR deviceRisk == HIGH");
        assertThat(node.getAnyOf()).hasSize(2);
        assertThat(node.getAnyOf().get(0).getAllOf()).hasSize(2);
    }

    @Test
    void fieldReferenceValue() {
        ConditionNode node = ConditionDslParser.parse("country != customer.country");
        assertThat(node.getValue()).isEqualTo(ConditionNode.fieldRef("customer.country"));
    }

    @Test
    void unknownFieldRejected() {
        assertThrows(ConditionDslParser.ConditionParseException.class,
                () -> ConditionDslParser.parse("totallyUnknownField > 5"));
    }

    @Test
    void quotedStringValue() {
        ConditionNode node = ConditionDslParser.parse("merchant == \"CryptoExchange\"");
        assertThat(node.getValue()).isEqualTo("CryptoExchange");
    }

    @Test
    void aliasOperators() {
        ConditionNode node = ConditionDslParser.parse("country <> CA");
        assertThat(node.getOperator()).isEqualTo("!=");

        ConditionNode node2 = ConditionDslParser.parse("amount = 100");
        assertThat(node2.getOperator()).isEqualTo("==");
    }
}
