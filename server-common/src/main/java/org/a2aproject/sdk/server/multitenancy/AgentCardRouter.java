package org.a2aproject.sdk.server.multitenancy;

import org.a2aproject.sdk.spec.AgentCard;
import org.jspecify.annotations.Nullable;

/**
 * Resolves tenant-specific {@link AgentCard} instances.
 * <p>
 * Implementations should return {@code null} when the tenant is {@code null} or blank
 * (no tenant specified — caller uses the default card). When a non-blank tenant is
 * provided and no matching card exists, implementations should also return {@code null};
 * the caller will treat that as a 404.
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
     * Returns {@code null} when no tenant-specific card is registered for the given tenant
     * (including when {@code tenant} is {@code null} or blank). The caller interprets
     * {@code null} as follows:
     * <ul>
     *   <li>If {@code tenant} was {@code null} or blank → fall back to the default public card.</li>
     *   <li>If {@code tenant} was non-blank → the tenant is unknown; respond with HTTP 404.</li>
     * </ul>
     *
     * @param tenant the tenant identifier, may be {@code null}
     * @return the tenant-specific public agent card, or {@code null} if not found
     */
    default @Nullable AgentCard resolvePublicCard(@Nullable String tenant) {
        return null;
    }
}
