package org.a2aproject.sdk.server.multitenancy;

import org.a2aproject.sdk.spec.AgentCard;
import org.jspecify.annotations.Nullable;

/**
 * Resolves tenant-specific {@link AgentCard} instances.
 * <p>
 * Implementations should return the default (unqualified) card when the tenant
 * is {@code null}, blank, or does not match any registered tenant.
 */
public interface AgentCardRouter {

    /**
     * Resolves the extended {@link AgentCard} for the given tenant.
     *
     * @param tenant the tenant identifier, may be {@code null}
     * @return the resolved extended agent card, or {@code null} if none is configured
     */
    @Nullable AgentCard resolveExtendedCard(@Nullable String tenant);

    /**
     * Resolves the public {@link AgentCard} for the given tenant.
     * <p>
     * Returns {@code null} by default, signaling the handler to fall back to the
     * default (non-tenant-specific) public agent card injected via {@code @PublicAgentCard}.
     * Implementations that manage tenant-specific public cards should return
     * a non-{@code null} card for known tenants.
     *
     * @param tenant the tenant identifier, may be {@code null}
     * @return the tenant-specific public agent card, or {@code null} to fall back to the default public card
     */
    default @Nullable AgentCard resolvePublicCard(@Nullable String tenant) {
        return null;
    }
}
