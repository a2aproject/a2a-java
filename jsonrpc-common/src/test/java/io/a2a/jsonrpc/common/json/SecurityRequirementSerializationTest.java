package io.a2a.jsonrpc.common.json;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.a2a.spec.SecurityRequirement;

/**
 * Tests for SecurityRequirement serialization and deserialization with JSON.
 */
class SecurityRequirementSerializationTest {

    @Test
    void testSecurityRequirementSerializationWithSingleScheme() throws JsonProcessingException {
        SecurityRequirement requirement = SecurityRequirement.builder()
                .scheme("oauth2", List.of("read", "write"))
                .build();

        String json = JsonUtil.toJson(requirement);
        assertNotNull(json);

        String expected = """
            {"schemes":{"oauth2":{"list":["read","write"]}}}""";
        assertEquals(expected, json);

        SecurityRequirement deserialized = JsonUtil.fromJson(json, SecurityRequirement.class);
        assertEquals(requirement, deserialized);
    }

    @Test
    void testSecurityRequirementSerializationWithMultipleSchemes() throws JsonProcessingException {
        SecurityRequirement requirement = SecurityRequirement.builder()
                .scheme("oauth2", List.of("profile"))
                .scheme("apiKey", List.of())
                .build();

        String json = JsonUtil.toJson(requirement);
        assertNotNull(json);

        String expected = """
            {"schemes":{"oauth2":{"list":["profile"]},"apiKey":{"list":[]}}}""";
        assertEquals(expected, json);

        SecurityRequirement deserialized = JsonUtil.fromJson(json, SecurityRequirement.class);
        assertEquals(requirement, deserialized);
    }

    @Test
    void testSecurityRequirementSerializationWithEmptyScopes() throws JsonProcessingException {
        SecurityRequirement requirement = SecurityRequirement.builder()
                .scheme("apiKey", List.of())
                .build();

        String json = JsonUtil.toJson(requirement);
        assertNotNull(json);

        String expected = """
                {"schemes":{"apiKey":{"list":[]}}}""";
        assertEquals(expected, json);

        SecurityRequirement deserialized = JsonUtil.fromJson(json, SecurityRequirement.class);
        assertEquals(requirement, deserialized);
    }

    @Test
    void testSecurityRequirementSerializationWithNullSchemes() throws JsonProcessingException {
        SecurityRequirement requirement = new SecurityRequirement(emptyMap());

        String json = JsonUtil.toJson(requirement);

        assertNotNull(json);
        String expected = """
              {"schemes":{}}""";
        assertEquals(expected, json);

        SecurityRequirement deserialized = JsonUtil.fromJson(json, SecurityRequirement.class);
        assertNotNull(deserialized);
        assertEquals(requirement, deserialized);
    }
}
