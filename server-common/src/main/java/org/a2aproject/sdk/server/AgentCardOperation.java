package org.a2aproject.sdk.server;

import java.util.function.Predicate;
import java.util.function.Supplier;

import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.InternalError;
import org.a2aproject.sdk.spec.PushNotificationNotSupportedError;
import org.a2aproject.sdk.spec.UnsupportedOperationError;
import org.a2aproject.sdk.util.Assert;

/** The capability and transport-specific failure for an operation. */
public record AgentCardOperation(Predicate<AgentCard> capability,
        Supplier<? extends A2AError> capabilityFailure) {
    public static final AgentCardOperation STREAMING = new AgentCardOperation(
            card -> card.capabilities().streaming(),
            () -> new UnsupportedOperationError(null, "Streaming is not supported by the agent", null));
    public static final AgentCardOperation PUSH_NOTIFICATIONS = new AgentCardOperation(
            card -> card.capabilities().pushNotifications(), PushNotificationNotSupportedError::new);
    public static final AgentCardOperation EXTENDED_CARD = new AgentCardOperation(
            card -> card.capabilities().extendedAgentCard(), UnsupportedOperationError::new);

    public AgentCardOperation {
        Assert.checkNotNullParam("capability", capability);
        Assert.checkNotNullParam("capabilityFailure", capabilityFailure);
    }

    public static AgentCardOperation none() {
        return new AgentCardOperation(card -> true,
                () -> new InternalError("No capability failure was configured"));
    }
}
