package org.a2aproject.sdk.server;

import java.util.function.Supplier;

import org.a2aproject.sdk.server.multitenancy.AgentCardRouter;
import org.a2aproject.sdk.server.multitenancy.TenantNotFoundException;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.util.Utils;
import org.a2aproject.sdk.util.Assert;
import org.jspecify.annotations.Nullable;

/** Selects request and discovery cards independently of transport validation. */
public final class AgentCardResolver {
    private final Supplier<@Nullable AgentCard> publicCard;
    private final Supplier<@Nullable AgentCard> extendedCard;
    private final @Nullable AgentCardRouter router;

    private AgentCardResolver(Supplier<@Nullable AgentCard> publicCard,
            Supplier<@Nullable AgentCard> extendedCard, @Nullable AgentCardRouter router) {
        this.publicCard = Assert.checkNotNullParam("publicCard", publicCard);
        this.extendedCard = Assert.checkNotNullParam("extendedCard", extendedCard);
        this.router = router;
    }

    public static AgentCardResolver of(Supplier<@Nullable AgentCard> publicCard,
            Supplier<@Nullable AgentCard> extendedCard, @Nullable AgentCardRouter router) {
        return new AgentCardResolver(publicCard, extendedCard, router);
    }

    /** Selects public then extended cards, never using defaults for a routed nonblank tenant. */
    public AgentCard resolveEffectiveCard(@Nullable String tenant) {
        String normalizedTenant = normalizeTenantForResolution(tenant);
        return resolveEffectiveCardForNormalizedTenant(normalizedTenant);
    }

    /** Resolves a card for a tenant already normalized by the shared request policy. */
    AgentCard resolveEffectiveCardForNormalizedTenant(@Nullable String normalizedTenant) {
        if (router != null && normalizedTenant != null) {
            AgentCard card = router.resolvePublicCard(normalizedTenant);
            if (card == null) {
                card = router.resolveExtendedCard(normalizedTenant);
            }
            if (card == null) {
                throw new TenantNotFoundException(normalizedTenant);
            }
            return card;
        }
        AgentCard card = publicCard.get();
        if (card == null && router != null) {
            card = router.resolvePublicCard(null);
        }
        if (card == null) {
            card = extendedCard.get();
        }
        if (card == null && router != null) {
            card = router.resolveExtendedCard(null);
        }
        if (card == null) {
            throw new IllegalStateException(AgentCardValidator.NO_AGENT_CARD_MESSAGE);
        }
        return card;
    }

    /** Selects public cards only; extended cards are never exposed through public discovery. */
    public @Nullable AgentCard resolvePublicDiscoveryCard(@Nullable String tenant) {
        String normalizedTenant = normalizeTenantForResolution(tenant);
        if (router != null && normalizedTenant != null) {
            AgentCard card = router.resolvePublicCard(normalizedTenant);
            if (card == null) {
                throw new TenantNotFoundException(normalizedTenant);
            }
            return card;
        }
        AgentCard card = publicCard.get();
        if (card == null && router != null) {
            card = router.resolvePublicCard(null);
        }
        return card;
    }

    /** Selects a routed extended card for nonblank tenants and the default extended card otherwise. */
    public @Nullable AgentCard resolveExtendedDiscoveryCard(@Nullable String tenant) {
        String normalizedTenant = normalizeTenantForResolution(tenant);
        if (router != null && normalizedTenant != null) {
            return router.resolveExtendedCard(normalizedTenant);
        }
        return extendedCard.get();
    }

    /** Validates a tenant and removes its optional boundary slashes; blank denotes the default tenant. */
    public static @Nullable String normalizeTenantForResolution(@Nullable String tenant) {
        Utils.validateTenant(tenant);
        if (tenant == null || tenant.isBlank()) {
            return null;
        }
        String normalizedTenant = tenant;
        if (normalizedTenant.startsWith("/")) {
            normalizedTenant = normalizedTenant.substring(1);
        }
        if (normalizedTenant.endsWith("/")) {
            normalizedTenant = normalizedTenant.substring(0, normalizedTenant.length() - 1);
        }
        return normalizedTenant.isEmpty() ? null : normalizedTenant;
    }
}
