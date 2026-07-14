package com.frauddemo.fraudmcp.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.JsonOutputFormat;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StructuredContentBlock;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Calls the Anthropic API to draft a structured rule from a natural-language
 * instruction, using structured outputs so the response is schema-validated
 * against RuleDraft/ConditionNodeDraft before it ever reaches the rule
 * engine -- a Java port of tools/generate_rule.py.
 *
 * The JSON schema below is hand-written and passed via the raw
 * OutputConfig/JsonOutputFormat API rather than anthropic-java-core's
 * {@code outputConfig(Class<T>)} auto-derivation: that overload reflects into
 * a victools jsonschema-generator API that spring-ai-mcp-annotations pulls in
 * at an incompatible major version (5.x vs the 4.x anthropic-java-core
 * compiled against), and the two can't be reconciled with a single version
 * on the classpath. Building the schema by hand sidesteps the conflict
 * entirely; {@code RuleDraft.class} is still passed to
 * {@link StructuredMessageCreateParams}'s public constructor so the SDK
 * still parses the response into our POJO.
 */
@Component
public class AnthropicRuleGenerator {

    private static final ObjectMapper SCHEMA_MAPPER = new ObjectMapper();

    private static final String SCHEMA_JSON = """
            {
              "type": "object",
              "properties": {
                "name": {"type": "string"},
                "conditionJson": {"$ref": "#/$defs/ConditionNodeDraft"},
                "action": {"type": "string", "enum": ["DECLINE", "REVIEW", "ALERT", "HIGH_RISK"]},
                "reasoning": {"type": "string"}
              },
              "required": ["name", "conditionJson", "action", "reasoning"],
              "additionalProperties": false,
              "$defs": {
                "ConditionNodeDraft": {
                  "type": "object",
                  "properties": {
                    "allOf": {"type": ["array", "null"], "items": {"$ref": "#/$defs/ConditionNodeDraft"}},
                    "anyOf": {"type": ["array", "null"], "items": {"$ref": "#/$defs/ConditionNodeDraft"}},
                    "field": {"type": ["string", "null"]},
                    "operator": {"type": ["string", "null"], "enum": [">", "<", ">=", "<=", "==", "!=", null]},
                    "value": {"anyOf": [{"$ref": "#/$defs/ConditionValueDraft"}, {"type": "null"}]}
                  },
                  "required": ["allOf", "anyOf", "field", "operator", "value"],
                  "additionalProperties": false
                },
                "ConditionValueDraft": {
                  "type": "object",
                  "properties": {
                    "numberValue": {"type": ["number", "null"]},
                    "stringValue": {"type": ["string", "null"]},
                    "boolValue": {"type": ["boolean", "null"]},
                    "refValue": {"type": ["string", "null"]}
                  },
                  "required": ["numberValue", "stringValue", "boolValue", "refValue"],
                  "additionalProperties": false
                }
              }
            }
            """;

    private static final String SYSTEM_PROMPT = """
            You translate a natural-language fraud-prevention instruction into a \
            structured fraud rule condition tree.

            Allowed fields: amount (number), country (string, ISO-2), merchant (string), \
            customerAge (number), deviceRisk (string: LOW/MEDIUM/HIGH), transactionId (string), \
            customer.country (string, the customer's home country -- set refValue to \
            "customer.country" instead of stringValue when comparing a field against it).

            Allowed operators: > < >= <= == !=.

            Combine conditions with allOf (AND) or anyOf (OR); nest combinators if the \
            instruction requires it. A condition node is either a combinator (allOf/anyOf, \
            a list of child nodes) or a leaf (field, operator, value) -- never both. For a \
            leaf's value, set exactly one of numberValue, stringValue, boolValue, or refValue \
            and leave the others null.

            Choose action as one of DECLINE, REVIEW, ALERT, HIGH_RISK based on how severe the \
            instruction implies the rule should be: "block"/"reject" => DECLINE, "flag for \
            review" => REVIEW, "alert"/"notify" => ALERT, otherwise HIGH_RISK.

            Give a one-sentence reasoning explaining how the instruction maps to the \
            conditions and action you chose.
            """;

    private static OutputConfig buildOutputConfig() {
        JsonNode schemaTree;
        try {
            schemaTree = SCHEMA_MAPPER.readTree(SCHEMA_JSON);
        } catch (java.io.IOException e) {
            throw new UncheckedIOException(e);
        }
        Map<String, JsonValue> schemaProperties = new LinkedHashMap<>();
        schemaTree.fields().forEachRemaining(entry -> schemaProperties.put(entry.getKey(), JsonValue.fromJsonNode(entry.getValue())));

        JsonOutputFormat.Schema schema = JsonOutputFormat.Schema.builder()
                .putAllAdditionalProperties(schemaProperties)
                .build();
        JsonOutputFormat format = JsonOutputFormat.builder()
                .schema(schema)
                .type(JsonValue.from("json_schema"))
                .build();
        return OutputConfig.builder().format(format).build();
    }

    public RuleDraft generate(String instruction) {
        return generate(instruction, "claude-sonnet-5");
    }

    public RuleDraft generate(String instruction, String model) {
        AnthropicClient client = AnthropicOkHttpClient.fromEnv();

        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(1024L)
                .system(SYSTEM_PROMPT)
                .addUserMessage(instruction)
                .outputConfig(buildOutputConfig())
                .build();

        StructuredMessageCreateParams<RuleDraft> structuredParams =
                new StructuredMessageCreateParams<>(RuleDraft.class, params);

        StructuredMessage<RuleDraft> message = client.messages().create(structuredParams);

        return message.content().stream()
                .filter(StructuredContentBlock::isText)
                .findFirst()
                .map(block -> block.asText().text())
                .orElseThrow(() -> new IllegalStateException("The model did not return a parsable rule draft."));
    }
}
