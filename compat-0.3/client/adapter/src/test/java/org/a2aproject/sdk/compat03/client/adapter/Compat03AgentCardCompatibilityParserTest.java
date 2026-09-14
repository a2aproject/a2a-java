package org.a2aproject.sdk.compat03.client.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.a2aproject.sdk.compat03.json.JsonUtil_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCapabilities_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentSkill_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentInterface_v0_3;
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
}
