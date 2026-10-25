package org.a2aproject.sdk.server;

import java.util.Objects;

import org.a2aproject.sdk.server.extensions.A2AExtensions;
import org.a2aproject.sdk.server.version.A2AVersionValidator;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.InvalidParamsError;
import org.a2aproject.sdk.util.Assert;
import org.jspecify.annotations.Nullable;

/** Shared v1 request preflight, with explicit stages for transports that check capability first. */
public final class AgentCardRequestPolicy {
    private final AgentCardResolver resolver;

    public AgentCardRequestPolicy(AgentCardResolver resolver) {
        this.resolver = Assert.checkNotNullParam("resolver", resolver);
    }

    public EffectiveAgentCard preflight(@Nullable String tenant, ServerCallContext context,
            AgentCardTransportValidator validator, AgentCardOperation operation) {
        String normalizedTenant = normalizeTenant(tenant);
        AgentCard card = resolver.resolveEffectiveCardForNormalizedTenant(normalizedTenant);
        validator.validate(card);
        validateContext(card, context);
        validateCapability(card, operation);
        return attach(card, normalizedTenant, context);
    }

    public AgentCard resolveAndValidateCapability(@Nullable String tenant,
            AgentCardTransportValidator validator, AgentCardOperation operation) {
        String normalizedTenant = normalizeTenant(tenant);
        AgentCard card = resolver.resolveEffectiveCardForNormalizedTenant(normalizedTenant);
        validator.validate(card);
        validateCapability(card, operation);
        return card;
    }

    public EffectiveAgentCard validateContextAndAttach(AgentCard card, @Nullable String tenant,
            ServerCallContext context) {
        String normalizedTenant = normalizeTenant(tenant);
        validateContext(card, context);
        return attach(card, normalizedTenant, context);
    }

    private static void validateContext(AgentCard card, ServerCallContext context) {
        A2AVersionValidator.validateProtocolVersion(card, context);
        A2AExtensions.validateRequiredExtensions(card, context);
    }

    private static void validateCapability(AgentCard card, AgentCardOperation operation) {
        if (!operation.capability().test(card)) {
            throw operation.capabilityFailure().get();
        }
    }

    private static EffectiveAgentCard attach(AgentCard card, @Nullable String tenant, ServerCallContext context) {
        context.attachEffectiveAgentCard(tenant, card);
        return Objects.requireNonNull(context.getEffectiveAgentCard());
    }

    private static @Nullable String normalizeTenant(@Nullable String tenant) {
        try {
            return AgentCardResolver.normalizeTenantForResolution(tenant);
        } catch (IllegalArgumentException e) {
            throw new InvalidParamsError(e.getMessage());
        }
    }
}
