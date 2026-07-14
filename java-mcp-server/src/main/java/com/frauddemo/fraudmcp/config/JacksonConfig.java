package com.frauddemo.fraudmcp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 4 auto-configures a Jackson 3 ({@code tools.jackson.*}) ObjectMapper
 * for its own HTTP message conversion, but the MCP SDK and Anthropic SDK still
 * use classic Jackson 2 ({@code com.fasterxml.jackson.*}). This bean is the
 * classic-Jackson2 mapper our own code (Transaction.toMap, SimulationService,
 * MCP tool serialization) needs -- the two Jackson majors coexist on the
 * classpath without conflict since their packages don't overlap.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
