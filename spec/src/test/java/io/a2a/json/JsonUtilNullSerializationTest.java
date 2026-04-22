package io.a2a.json;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentProvider;

/**
 * Test to verify that JsonUtil.OBJECT_MAPPER does not serialize null fields.
 * This is critical for A2A spec compliance, particularly for AgentCard where
 * optional fields must be omitted (not present as "field": null) when they are null.
 */
public class JsonUtilNullSerializationTest {

    @Test
    public void testAgentCardOptionalFieldsOmittedInSerialization() throws Exception {
        // Create an AgentCard with REQUIRED fields only, leaving OPTIONAL fields as null
        // Per A2A spec 5.5, REQUIRED fields are:
        // - name, description, url, version, capabilities, defaultInputModes, defaultOutputModes, skills
        // - preferredTransport (has default), protocolVersion (has default), supportsAuthenticatedExtendedCard (has default)
        //
        // OPTIONAL fields that should be omitted when null:
        // - provider, documentationUrl, securitySchemes, security, iconUrl, signatures, additionalInterfaces
        AgentCard card = new AgentCard.Builder()
                .name("Test Agent")
                .description("Test Description")
                .url("https://example.com")
                .version("1.0.0")
                .capabilities(new AgentCapabilities(true, false, false, null))
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                // OPTIONAL fields intentionally not set (will be null):
                // - provider
                // - documentationUrl
                // - securitySchemes
                // - security
                // - iconUrl
                // - signatures
                // Note: additionalInterfaces gets a default value in the builder, so we don't test it here
                .build();

        // Serialize to JSON
        String json = JsonUtil.toJson(card);

        assertNotNull(json);

        // Verify that OPTIONAL fields with null values are NOT present in the JSON
        // (not even as "field": null)
        assertFalse(json.contains("\"provider\""),
            "OPTIONAL 'provider' field should be omitted when null, not serialized");
        assertFalse(json.contains("\"documentationUrl\""),
            "OPTIONAL 'documentationUrl' field should be omitted when null, not serialized");
        assertFalse(json.contains("\"securitySchemes\""),
            "OPTIONAL 'securitySchemes' field should be omitted when null, not serialized");
        assertFalse(json.contains("\"security\""),
            "OPTIONAL 'security' field should be omitted when null, not serialized");
        assertFalse(json.contains("\"iconUrl\""),
            "OPTIONAL 'iconUrl' field should be omitted when null, not serialized");
        assertFalse(json.contains("\"signatures\""),
            "OPTIONAL 'signatures' field should be omitted when null, not serialized");

        // Verify that NO fields are serialized with explicit null values
        assertFalse(json.contains(": null"),
            "No fields should be serialized with explicit null values. Found in JSON: " + json);
    }

    @Test
    public void testAgentCardWithProviderIsSerializedCorrectly() throws Exception {
        // Create an AgentCard with provider set (non-null)
        AgentProvider provider = new AgentProvider("Test Org", "https://testorg.com");

        AgentCard card = new AgentCard.Builder()
                .name("Test Agent")
                .description("Test Description")
                .url("https://example.com")
                .version("1.0.0")
                .capabilities(new AgentCapabilities(true, false, false, null))
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .provider(provider)  // Set provider explicitly
                .build();

        // Serialize to JSON
        String json = JsonUtil.toJson(card);

        assertNotNull(json);

        // Verify that provider IS present in the JSON when set
        assertFalse(json.contains("\"provider\":null"),
            "Provider should not be serialized as null when it has a value");
        assertFalse(json.contains("\"provider\": null"),
            "Provider should not be serialized as null when it has a value");
        assertFalse(json.contains(": null"),
            "No fields should be serialized with explicit null values. Found in JSON: " + json);
    }

    @Test
    public void testAgentCapabilitiesOptionalFieldsOmitted() throws Exception {
        // AgentCapabilities has all OPTIONAL fields per spec 5.5.2:
        // - streaming, pushNotifications, stateTransitionHistory (booleans - always serialized)
        // - extensions (List - should be omitted when null)
        AgentCapabilities caps = new AgentCapabilities(false, false, false, null);

        String json = JsonUtil.toJson(caps);

        assertNotNull(json);

        // Note: streaming, pushNotifications, stateTransitionHistory use primitive booleans,
        // so they will always be serialized (even with default false values)
        // But extensions should be omitted when null
        assertFalse(json.contains("\"extensions\""),
            "OPTIONAL 'extensions' field should be omitted when null");
        assertFalse(json.contains(": null"),
            "No fields should be serialized with explicit null values. Found in JSON: " + json);
    }
}
