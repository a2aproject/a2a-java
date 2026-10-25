package org.a2aproject.sdk.server;

import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.util.Assert;

/** Caches successful validation within one transport configuration. */
public final class CachingAgentCardTransportValidator implements AgentCardTransportValidator {
    private final ConcurrentMap<AgentCard, Boolean> validatedCards = AgentCardValidator.newValidatedCardsMap();
    private final Consumer<AgentCard> validator;

    public CachingAgentCardTransportValidator(Consumer<AgentCard> validator) {
        this.validator = Assert.checkNotNullParam("validator", validator);
    }

    @Override
    public void validate(AgentCard card) {
        AgentCardValidator.validateOnce(card, validatedCards, validator);
    }
}
