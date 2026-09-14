package org.a2aproject.sdk.compat03.client.adapter;

import java.util.Optional;
import java.util.Set;

import org.a2aproject.sdk.client.http.AgentCardCompatibilityParser;
import org.a2aproject.sdk.compat03.conversion.mappers.domain.AgentCardMapper_v0_3;
import org.a2aproject.sdk.compat03.json.JsonUtil_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
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
        try {
            AgentCard_v0_3 legacyCard = JsonUtil_v0_3.fromJson(rawCardJson, AgentCard_v0_3.class);
            if (!"0.3".equals(normalize(legacyCard.protocolVersion()))) {
                return Optional.empty();
            }
            return Optional.of(AgentCardMapper_v0_3.INSTANCE.toV10(legacyCard));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static String normalize(String version) {
        return switch (version) {
            case "0.3", "0.3.0" -> "0.3";
            default -> version;
        };
    }
}
