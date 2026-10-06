package org.a2aproject.sdk.client.http;

import java.util.Optional;
import java.util.Set;

import org.a2aproject.sdk.spec.AgentCard;
import org.jspecify.annotations.Nullable;

/** Optional parser for agent-card formats supported by compatibility artifacts. */
public interface AgentCardCompatibilityParser {
    String supportedProtocolVersion();

    Optional<AgentCard> parse(String rawCardJson, @Nullable AgentCard parsedV10Card,
            Set<String> requestedProtocolVersions);
}
