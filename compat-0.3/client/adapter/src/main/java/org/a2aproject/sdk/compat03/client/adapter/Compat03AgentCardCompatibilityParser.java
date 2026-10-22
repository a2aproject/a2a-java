package org.a2aproject.sdk.compat03.client.adapter;

import java.util.Optional;
import java.util.Set;

import org.a2aproject.sdk.client.http.A2ACardResolver;
import org.a2aproject.sdk.client.http.AgentCardCompatibilityParser;
import org.a2aproject.sdk.compat03.conversion.mappers.domain.AgentCardMapper_v0_3;
import org.a2aproject.sdk.compat03.json.JsonProcessingException_v0_3;
import org.a2aproject.sdk.compat03.json.JsonUtil_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.spec.A2AClientJSONError;
import org.a2aproject.sdk.spec.AgentCard;
import org.jspecify.annotations.Nullable;

/** Parses a legacy 0.3 JSON card and projects it into the public 1.0 card model. */
public final class Compat03AgentCardCompatibilityParser implements AgentCardCompatibilityParser {
    @Override
    public String supportedProtocolVersion() {
        return "0.3";
    }

    @Override
    public Optional<AgentCard> parse(String rawCardJson, @Nullable AgentCard parsedV10Card,
            Set<String> requestedProtocolVersions) {
        final AgentCard_v0_3 legacyCard;
        try {
            legacyCard = JsonUtil_v0_3.fromJson(rawCardJson, AgentCard_v0_3.class);
        } catch (JsonProcessingException_v0_3 e) {
            throw new A2AClientJSONError("Could not convert A2A 0.3 agent card to the unified client model", e);
        }
        final String version;
        try {
            version = A2ACardResolver.normalizeSupportedProtocolVersion(legacyCard.protocolVersion());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        if (!"0.3".equals(version)) {
            return Optional.empty();
        }
        return Optional.of(AgentCardMapper_v0_3.INSTANCE.toV10(legacyCard));
    }
}
