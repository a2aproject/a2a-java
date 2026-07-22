package org.a2aproject.sdk.spec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class AgentCardTest {

    @Test
    void testConstruction_defensivelyCopiesCollections() {
        List<String> defaultInputModes = new ArrayList<>();
        defaultInputModes.add("text");
        List<String> defaultOutputModes = new ArrayList<>();
        defaultOutputModes.add("text");
        List<AgentSkill> skills = new ArrayList<>();
        skills.add(AgentSkill.builder()
                .id("weather_query")
                .name("Weather Queries")
                .description("Get weather information")
                .tags(List.of("weather"))
                .build());
        List<AgentInterface> supportedInterfaces = new ArrayList<>();
        supportedInterfaces.add(new AgentInterface("JSONRPC", "http://localhost:9999", null, "1.0"));

        AgentCard card = new AgentCard(
                "Weather Agent",
                "Provides weather information",
                null,
                "1.0.0",
                null,
                AgentCapabilities.builder().streaming(true).build(),
                defaultInputModes,
                defaultOutputModes,
                skills,
                null,
                null,
                null,
                supportedInterfaces,
                null,
                null,
                null,
                null);

        defaultInputModes.add("audio");
        defaultOutputModes.add("audio");
        skills.add(AgentSkill.builder()
                .id("translate")
                .name("Translation")
                .description("Translate text")
                .tags(List.of("translate"))
                .build());
        supportedInterfaces.add(new AgentInterface("GRPC", "grpc://localhost:9090", null, "1.0"));

        assertEquals(List.of("text"), card.defaultInputModes());
        assertEquals(List.of("text"), card.defaultOutputModes());
        assertEquals(1, card.skills().size());
        assertEquals(1, card.supportedInterfaces().size());
        assertThrows(UnsupportedOperationException.class, () -> card.defaultInputModes().add("video"));
    }
}
