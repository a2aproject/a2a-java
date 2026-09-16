package org.a2aproject.sdk.client.http;

import java.util.Optional;
import java.util.Set;

import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.AgentSkill;

public final class TestAgentCardCompatibilityParser implements AgentCardCompatibilityParser {
    @Override
    public String supportedProtocolVersion() {
        return "0.3";
    }

    @Override
    public Optional<AgentCard> parse(String rawCardJson, AgentCard parsedV10Card,
            Set<String> requestedProtocolVersions) {
        return Optional.of(AgentCard.builder()
                .name("legacy")
                .description("legacy")
                .version("1")
                .url("http://example.com")
                .capabilities(new AgentCapabilities(false, false, false, null))
                .defaultInputModes(java.util.List.of("text"))
                .defaultOutputModes(java.util.List.of("text"))
                .skills(java.util.List.of(AgentSkill.builder().id("legacy").name("legacy").description("legacy")
                        .tags(java.util.List.of("legacy")).build()))
                .supportedInterfaces(java.util.List.of(new AgentInterface("JSONRPC", "http://example.com", null, "0.3")))
                .build());
    }
}
