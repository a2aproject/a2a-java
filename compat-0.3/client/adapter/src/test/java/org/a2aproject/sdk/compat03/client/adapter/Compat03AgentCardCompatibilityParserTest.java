package org.a2aproject.sdk.compat03.client.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpServer;
import org.a2aproject.sdk.client.http.A2ACardResolver;
import org.a2aproject.sdk.compat03.json.JsonUtil_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCapabilities_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentInterface_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentSkill_v0_3;
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
    void resolverUsesProductionParserWithARealLegacyCardAndOneFetch() throws Exception {
        AtomicInteger fetches = new AtomicInteger();
        AgentCard card = resolveCard(JsonUtil_v0_3.toJson(primaryOnlyCard("rest")), fetches);
        assertEquals("legacy", card.name());
        assertEquals("HTTP+JSON", card.supportedInterfaces().get(0).protocolBinding());
        assertEquals("0.3", card.supportedInterfaces().get(0).protocolVersion());
        assertEquals(1, fetches.get());
    }

    @Test
    void resolverRejectsMalformedShapeAndUnrequestedLegacyVersion() throws Exception {
        AtomicInteger fetches = new AtomicInteger();
        assertThrows(A2AClientJSONError.class, () -> resolveCard("{\"legacy\":true}", fetches));
        assertEquals(1, fetches.get());
        fetches.set(0);
        String unsupported = JsonUtil_v0_3.toJson(primaryOnlyCard("rest"))
                .replace("\"0.3.0\"", "\"0.2.0\"");
        assertThrows(A2AClientJSONError.class, () -> resolveCard(unsupported, fetches));
        assertEquals(1, fetches.get());
    }

    private static AgentCard resolveCard(String rawCard, AtomicInteger fetches) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/.well-known/agent-card.json", exchange -> {
            fetches.incrementAndGet();
            byte[] body = rawCard.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            } finally {
                exchange.close();
            }
        });
        server.start();
        try {
            return A2ACardResolver.builder().baseUrl("http://localhost:" + server.getAddress().getPort())
                    .supportedProtocolVersions(Set.of("1.0", "0.3")).build().getAgentCard();
        } finally {
            server.stop(0);
        }
    }

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
