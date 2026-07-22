package org.a2aproject.sdk.spec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class AgentSkillTest {

    @Test
    void testConstruction_defensivelyCopiesCollections() {
        List<String> tags = new ArrayList<>();
        tags.add("weather");
        List<String> examples = new ArrayList<>();
        examples.add("What's the weather in Tokyo?");
        List<String> inputModes = new ArrayList<>();
        inputModes.add("text");
        List<String> outputModes = new ArrayList<>();
        outputModes.add("text");

        AgentSkill skill = new AgentSkill(
                "weather_query",
                "Weather Queries",
                "Get weather information",
                tags,
                examples,
                inputModes,
                outputModes,
                null);

        tags.add("mutated");
        examples.add("mutated");
        inputModes.add("audio");
        outputModes.add("audio");

        assertEquals(List.of("weather"), skill.tags());
        assertEquals(List.of("What's the weather in Tokyo?"), skill.examples());
        assertEquals(List.of("text"), skill.inputModes());
        assertEquals(List.of("text"), skill.outputModes());
        assertThrows(UnsupportedOperationException.class, () -> skill.tags().add("x"));
    }
}
