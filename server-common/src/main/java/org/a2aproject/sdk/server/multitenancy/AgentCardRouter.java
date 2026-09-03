package org.a2aproject.sdk.server.multitenancy;

import org.a2aproject.sdk.spec.AgentCard;
import org.jspecify.annotations.Nullable;

/**
 * Resolves tenant-specific {@link AgentCard} instances.
 * <p>
 * Implementations should return the default public card when the tenant is {@code null}
 * or blank. When a non-blank tenant is provided and no matching card exists,
 * implementations should return {@code null}; the caller will treat that as a 404.
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
     * Implementations should return the default public card when {@code tenant} is
     * {@code null} or blank, the tenant-specific card when a match is found, and
     * {@code null} when a non-blank tenant has no matching card — the caller treats
     * that as HTTP 404.
     * <p>
     * The default implementation returns {@code null} (no public-card routing configured).
     *
     * @param tenant the tenant identifier, may be {@code null}
     * @return the public agent card, or {@code null} if a non-blank tenant is unknown
     */
    default @Nullable AgentCard resolvePublicCard(@Nullable String tenant) {
        return null;
    }
}
