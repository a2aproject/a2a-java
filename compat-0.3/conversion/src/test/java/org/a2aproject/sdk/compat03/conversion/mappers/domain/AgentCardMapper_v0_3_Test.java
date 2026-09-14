package org.a2aproject.sdk.compat03.conversion.mappers.domain;

import java.util.List;
import java.util.Map;

import org.a2aproject.sdk.compat03.spec.APIKeySecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCapabilities_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCardSignature_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentInterface_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentSkill_v0_3;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.APIKeySecurityScheme;
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
}
