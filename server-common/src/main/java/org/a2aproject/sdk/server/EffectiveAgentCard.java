package org.a2aproject.sdk.server;

import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.util.Assert;
import org.jspecify.annotations.Nullable;

/** The canonical tenant and Agent Card selected for a request. */
public record EffectiveAgentCard(@Nullable String tenant, AgentCard card) {
    public EffectiveAgentCard {
        Assert.checkNotNullParam("card", card);
    }
}
