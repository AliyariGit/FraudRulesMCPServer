package com.frauddemo.fraudmcp.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the REST facade end-to-end against the live Postgres instance
 * (docker-compose.yml, port 5433) via MockMvc -- the same round trip
 * client_smoke_test.py verifies for the Python implementation, just over the
 * servlet stack instead of MCP stdio.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FraudRuleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createEvaluateExplainRoundTrip() throws Exception {
        mockMvc.perform(post("/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"IT Test Rule","condition":"deviceRisk == HIGH AND merchant == CryptoExchange","action":"DECLINE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(post("/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"transactionId":"TX-IT-0001","amount":8500,"country":"CA","merchant":"CryptoExchange",
                                 "customerAge":22,"deviceRisk":"HIGH","customer":{"country":"US"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("DECLINE"))
                .andExpect(jsonPath("$.riskScore").value(greaterThan(0)));

        mockMvc.perform(get("/explain/TX-IT-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("DECLINE"))
                .andExpect(jsonPath("$.explanation").value(containsString("declined")));
    }

    @Test
    void simulateRuleReportsImpact() throws Exception {
        mockMvc.perform(post("/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"condition":"amount > 5000","action":"REVIEW","name":"IT Sim"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionsTested").value(300))
                .andExpect(jsonPath("$.ruleName").value("IT Sim"));
    }

    @Test
    void migrateLegacyRulePersistsPending() throws Exception {
        mockMvc.perform(post("/migrate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"legacyText":"IF TX_AMT > 9000 SET FRAUD_FLAG=Y"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.source").value("MIGRATED"));
    }

    @Test
    void explainMissingTransactionReturns404() throws Exception {
        mockMvc.perform(get("/explain/TX-DOES-NOT-EXIST"))
                .andExpect(status().isNotFound());
    }
}
