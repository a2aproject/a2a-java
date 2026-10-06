package org.a2aproject.sdk.compat03.conversion.mappers.domain;

import java.util.List;
import java.util.Map;

import org.a2aproject.sdk.compat03.spec.APIKeySecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCapabilities_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCardSignature_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentInterface_v0_3;
import org.a2aproject.sdk.compat03.spec.AuthorizationCodeOAuthFlow_v0_3;
import org.a2aproject.sdk.compat03.spec.ClientCredentialsOAuthFlow_v0_3;
import org.a2aproject.sdk.compat03.spec.HTTPAuthSecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentSkill_v0_3;
import org.a2aproject.sdk.compat03.spec.MutualTLSSecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.OAuth2SecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.OAuthFlows_v0_3;
import org.a2aproject.sdk.compat03.spec.OpenIdConnectSecurityScheme_v0_3;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.APIKeySecurityScheme;
import org.a2aproject.sdk.spec.HTTPAuthSecurityScheme;
import org.a2aproject.sdk.spec.MutualTLSSecurityScheme;
import org.a2aproject.sdk.spec.OAuth2SecurityScheme;
import org.a2aproject.sdk.spec.OpenIdConnectSecurityScheme;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AgentCardMapper_v0_3_Test {

    @Test
    void projectsCompleteLegacyCardAndConvertsItBack() {
        AgentCard_v0_3 legacy = new AgentCard_v0_3(
            "agent", "description", "https://agent.example", null, "1", "https://docs.example",
            new AgentCapabilities_v0_3(true, true, false, null), List.of("text"), List.of("text"),
            List.of(new AgentSkill_v0_3("skill", "Skill", "Does things", List.of("tag"),
                List.of("example"), List.of("text"), List.of("text"), List.of(Map.of("auth", List.of("read"))))),
            true, Map.of("auth", new APIKeySecurityScheme_v0_3("header", "Authorization", "token")),
            List.of(Map.of("auth", List.of("read"))), "https://icon.example",
            List.of(new AgentInterface_v0_3("JSONRPC", "https://agent.example/rpc")), "JSONRPC", "0.3.0",
            List.of(new AgentCardSignature_v0_3(Map.of("alg", "none"), "protected", "signature")));

        AgentCard current = AgentCardMapper_v0_3.INSTANCE.toV10(legacy);

        assertEquals("https://agent.example", current.url());
        assertEquals("JSONRPC", current.preferredTransport());
        assertEquals("0.3", current.supportedInterfaces().get(0).protocolVersion());
        assertEquals(true, current.capabilities().extendedAgentCard());
        assertEquals("Authorization", ((APIKeySecurityScheme) current.securitySchemes().get("auth")).name());
        assertNotNull(current.signatures());

        AgentCard_v0_3 roundTrip = AgentCardMapper_v0_3.INSTANCE.fromV10(current);
        assertEquals("https://agent.example", roundTrip.url());
        assertEquals("0.3", roundTrip.protocolVersion());
        assertEquals(true, roundTrip.supportsAuthenticatedExtendedCard());
        assertEquals(false, roundTrip.capabilities().stateTransitionHistory());
        assertEquals("Authorization", ((APIKeySecurityScheme_v0_3) roundTrip.securitySchemes().get("auth")).name());
        assertEquals("signature", roundTrip.signatures().get(0).signature());
    }

    @ParameterizedTest
    @CsvSource({
        "jsonrpc,JSONRPC",
        "http,HTTP+JSON",
        "rest,HTTP+JSON",
        "grpc,GRPC",
        "HTTP+JSON,HTTP+JSON"
    })
    void primaryEndpointBecomesSupportedInterface(String preferred, String expectedBinding) {
        AgentCard_v0_3 card = primaryOnlyCard(preferred);

        AgentCard projected = AgentCardMapper_v0_3.INSTANCE.toV10(card);

        assertEquals(1, projected.supportedInterfaces().size());
        assertEquals(expectedBinding, projected.supportedInterfaces().get(0).protocolBinding());
        assertEquals("0.3", projected.supportedInterfaces().get(0).protocolVersion());
        assertEquals(expectedBinding, projected.preferredTransport());
    }

    @Test
    void projectsPrimaryInterfaceBeforeAdditionalInterfaces() {
        AgentCard_v0_3 card = new AgentCard_v0_3(
            "legacy", "legacy", "https://agent.example/jsonrpc", null, "1", null,
            new AgentCapabilities_v0_3.Builder().build(), List.of("text"), List.of("text"), List.of(),
            false, null, null, null,
            List.of(new AgentInterface_v0_3("grpc", "https://agent.example/grpc")), "jsonrpc", "0.3", null);

        AgentCard projected = AgentCardMapper_v0_3.INSTANCE.toV10(card);

        assertEquals(List.of("JSONRPC", "GRPC"), projected.supportedInterfaces().stream()
            .map(AgentInterface::protocolBinding).toList());
        assertEquals("https://agent.example/jsonrpc", projected.supportedInterfaces().get(0).url());
    }

    @Test
    void projectsCardWhenAdditionalInterfacesAreOmitted() {
        AgentCard_v0_3 card = new AgentCard_v0_3(
            "legacy", "legacy", "https://agent.example/jsonrpc", null, "1", null,
            new AgentCapabilities_v0_3.Builder().build(), List.of("text"), List.of("text"), List.of(),
            false, null, null, null, null, "jsonrpc", "0.3", null);

        AgentCard projected = AgentCardMapper_v0_3.INSTANCE.toV10(card);

        assertEquals(1, projected.supportedInterfaces().size());
        assertEquals("JSONRPC", projected.supportedInterfaces().get(0).protocolBinding());
    }

    @Test
    void doesNotCopyPrimaryInterfaceIntoAdditionalInterfacesOnRoundTrip() {
        AgentCard_v0_3 card = primaryOnlyCard("jsonrpc");

        AgentCard_v0_3 roundTrip = AgentCardMapper_v0_3.INSTANCE.fromV10(
            AgentCardMapper_v0_3.INSTANCE.toV10(card));

        assertEquals(List.of(), roundTrip.additionalInterfaces());
    }

    @Test
    void convertsPatchVersionLegacyInterfaceWhenV1CardHasNoLegacyUrl() {
        AgentCard card = AgentCard.builder()
                .name("agent").description("description").version("1")
                .capabilities(new org.a2aproject.sdk.spec.AgentCapabilities(false, false, false, null))
                .defaultInputModes(List.of("text")).defaultOutputModes(List.of("text"))
                .skills(List.of())
                .supportedInterfaces(List.of(new AgentInterface("JSONRPC", "https://agent.example/rpc", null, "0.3.0")))
                .build();

        AgentCard_v0_3 legacy = AgentCardMapper_v0_3.INSTANCE.fromV10(card);

        assertEquals("https://agent.example/rpc", legacy.url());
        assertEquals("JSONRPC", legacy.preferredTransport());
        assertEquals(List.of(), legacy.additionalInterfaces());
    }

    @Test
    void convertsHttpAuthSecuritySchemeBothWays() {
        HTTPAuthSecurityScheme_v0_3 legacyScheme = new HTTPAuthSecurityScheme_v0_3.Builder()
            .scheme("basic").bearerFormat("none").description("HTTP Basic authentication").build();
        AgentCard_v0_3 card = primaryOnlyCard("http", Map.of("basicAuth", legacyScheme));

        AgentCard projected = AgentCardMapper_v0_3.INSTANCE.toV10(card);
        HTTPAuthSecurityScheme projectedScheme =
            (HTTPAuthSecurityScheme) projected.securitySchemes().get("basicAuth");
        assertEquals("basic", projectedScheme.scheme());
        assertEquals("none", projectedScheme.bearerFormat());
        assertEquals("HTTP Basic authentication", projectedScheme.description());

        HTTPAuthSecurityScheme_v0_3 roundTripScheme = (HTTPAuthSecurityScheme_v0_3)
            AgentCardMapper_v0_3.INSTANCE.fromV10(projected).securitySchemes().get("basicAuth");
        assertEquals("basic", roundTripScheme.scheme());
        assertEquals("none", roundTripScheme.bearerFormat());
        assertEquals("HTTP Basic authentication", roundTripScheme.description());
    }

    @Test
    void convertsOpenIdConnectMutualTlsAndCompatibleOAuthSecuritySchemes() {
        OAuthFlows_v0_3 flows = new OAuthFlows_v0_3(
            new AuthorizationCodeOAuthFlow_v0_3("https://auth.example", "https://refresh.example",
                Map.of("read", "Read"), "https://token.example"),
            new ClientCredentialsOAuthFlow_v0_3("https://refresh.example", Map.of("write", "Write"),
                "https://token.example"), null, null);
        AgentCard_v0_3 card = primaryOnlyCard("jsonrpc", Map.of(
            "oidc", new OpenIdConnectSecurityScheme_v0_3("https://oidc.example", "OIDC"),
            "mtls", new MutualTLSSecurityScheme_v0_3("mTLS"),
            "oauth", new OAuth2SecurityScheme_v0_3(flows, "OAuth", "https://metadata.example")));

        AgentCard projected = AgentCardMapper_v0_3.INSTANCE.toV10(card);

        assertEquals("https://oidc.example", ((OpenIdConnectSecurityScheme) projected.securitySchemes().get("oidc"))
            .openIdConnectUrl());
        assertEquals("mTLS", ((MutualTLSSecurityScheme) projected.securitySchemes().get("mtls")).description());
        OAuth2SecurityScheme oauth = (OAuth2SecurityScheme) projected.securitySchemes().get("oauth");
        assertEquals("https://auth.example", oauth.flows().authorizationCode().authorizationUrl());
        assertEquals("https://token.example", oauth.flows().clientCredentials().tokenUrl());
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
