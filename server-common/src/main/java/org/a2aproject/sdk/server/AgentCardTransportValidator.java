package org.a2aproject.sdk.server;

import org.a2aproject.sdk.spec.AgentCard;

/** Validates a card using the current transport's configuration and class loader. */
@FunctionalInterface
public interface AgentCardTransportValidator {
    void validate(AgentCard card);
}
