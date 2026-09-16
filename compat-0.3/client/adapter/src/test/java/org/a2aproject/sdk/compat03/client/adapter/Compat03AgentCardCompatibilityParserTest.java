package org.a2aproject.sdk.compat03.client.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.a2aproject.sdk.compat03.json.JsonUtil_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCapabilities_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentSkill_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentInterface_v0_3;
import org.a2aproject.sdk.compat03.spec.HTTPAuthSecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.ImplicitOAuthFlow_v0_3;
import org.a2aproject.sdk.compat03.spec.OAuth2SecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.OAuthFlows_v0_3;
import org.a2aproject.sdk.spec.A2AClientJSONError;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.HTTPAuthSecurityScheme;
import org.junit.jupiter.api.Test;

class Compat03AgentCardCompatibilityParserTest {
    @Test
    void parsesDeclaredPatchVersionAndProjectsInterface() throws Exception {
        AgentCard_v0_3 card = new AgentCard_v0_3.Builder()
                .name("legacy")
                .description("legacy")
                .url("https://example.test/a2a")
                .version("1")
                .capabilities(new AgentCapabilities_v0_3.Builder().build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of(new AgentSkill_v0_3.Builder().id("skill").name("skill")
                        .description("skill").tags(List.of("tag")).build()))
                .additionalInterfaces(List.of(new AgentInterface_v0_3("JSONRPC", "https://example.test/a2a")))
                .protocolVersion("0.3.0")
                .build();

        var result = new Compat03AgentCardCompatibilityParser().parse(
                JsonUtil_v0_3.toJson(card), null, Set.of("1.0", "0.3"));

        assertTrue(result.isPresent());
        assertEquals("0.3", result.orElseThrow().supportedInterfaces().get(0).protocolVersion());
    }

    @Test
    void parsesPrimaryOnlyCardAndProjectsCanonicalInterface() throws Exception {
        AgentCard_v0_3 card = primaryOnlyCard("rest");

        var result = new Compat03AgentCardCompatibilityParser().parse(
                JsonUtil_v0_3.toJson(card), null, Set.of("1.0", "0.3"));

        assertTrue(result.isPresent());
        AgentCard projected = result.orElseThrow();
        assertEquals(1, projected.supportedInterfaces().size());
        assertEquals("HTTP+JSON", projected.supportedInterfaces().get(0).protocolBinding());
        assertEquals("0.3", projected.supportedInterfaces().get(0).protocolVersion());
    }

    @Test
    void parsesPrimaryOnlyCardWithHttpAuthScheme() throws Exception {
        AgentCard_v0_3 card = primaryOnlyCard("http", Map.of("basicAuth", new HTTPAuthSecurityScheme_v0_3.Builder()
            .scheme("basic").bearerFormat("none").description("HTTP Basic authentication").build()));

        var result = new Compat03AgentCardCompatibilityParser().parse(
                JsonUtil_v0_3.toJson(card), null, Set.of("1.0", "0.3"));

        assertTrue(result.isPresent());
        HTTPAuthSecurityScheme projectedScheme = (HTTPAuthSecurityScheme)
            result.orElseThrow().securitySchemes().get("basicAuth");
        assertEquals("basic", projectedScheme.scheme());
        assertEquals("none", projectedScheme.bearerFormat());
        assertEquals("HTTP Basic authentication", projectedScheme.description());
    }

    @Test
    void reportsUnsupportedSecuritySchemeConversion() throws Exception {
        AgentCard_v0_3 card = primaryOnlyCard("jsonrpc", Map.of("oauth", new OAuth2SecurityScheme_v0_3(
                new OAuthFlows_v0_3(null, null,
                        new ImplicitOAuthFlow_v0_3("https://example.test/authorize", null, Map.of()), null),
                "OAuth", null)));

        A2AClientJSONError exception = assertThrows(A2AClientJSONError.class,
                () -> new Compat03AgentCardCompatibilityParser().parse(
                        JsonUtil_v0_3.toJson(card), null, Set.of("1.0", "0.3")));

        assertTrue(exception.getMessage().contains("Could not convert A2A 0.3 agent card"));
        assertTrue(exception.getCause().getMessage().contains("implicit"));
    }

    private static AgentCard_v0_3 primaryOnlyCard(String preferredTransport) {
        return primaryOnlyCard(preferredTransport, null);
    }

    private static AgentCard_v0_3 primaryOnlyCard(String preferredTransport,
            Map<String, org.a2aproject.sdk.compat03.spec.SecurityScheme_v0_3> securitySchemes) {
        return new AgentCard_v0_3.Builder()
            .name("legacy").description("legacy").url("http://localhost:8081")
            .version("1.0.0").preferredTransport(preferredTransport).protocolVersion("0.3.0")
            .capabilities(new AgentCapabilities_v0_3.Builder().build())
            .defaultInputModes(List.of("text")).defaultOutputModes(List.of("text"))
            .skills(List.of()).securitySchemes(securitySchemes).additionalInterfaces(List.of()).build();
    }
}
