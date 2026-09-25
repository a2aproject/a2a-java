package org.a2aproject.sdk.extras.multitenancy;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.a2aproject.sdk.server.ExtendedAgentCard;
import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.server.multitenancy.AgentCardRouter;
import org.a2aproject.sdk.server.multitenancy.Tenant;
import org.a2aproject.sdk.server.util.CdiUtils;
import org.a2aproject.sdk.spec.AgentCard;
import org.jspecify.annotations.Nullable;

/**
 * CDI-based {@link AgentCardRouter} that resolves tenant-specific {@link AgentCard} beans.
 * <p>
 * Extended cards are resolved via {@code @Tenant("x") @ExtendedAgentCard}-qualified beans.
 * Tenant-specific public cards are resolved by matching beans that carry the
 * {@code @Tenant("x")} qualifier and either:
 * <ul>
 *   <li>carry {@code @PublicAgentCard} (preferred — these cards are resolved before bare
 *       tenant cards, even if they also carry {@code @ExtendedAgentCard}), or</li>
 *   <li>carry neither {@code @ExtendedAgentCard} nor {@code @PublicAgentCard} (bare
 *       tenant card).</li>
 * </ul>
 * Using {@code @Tenant("x") @PublicAgentCard} on a tenant-specific bean is safe because
 * both the router's default-card lookup and the transport handler injection points use
 * {@link org.a2aproject.sdk.server.util.CdiUtils#resolveDefault} to exclude
 * {@code @Tenant}-qualified beans when resolving the default card.
 * <p>
 * Returns the default public card from {@link #resolvePublicCard} when the tenant is
 * {@code null} or blank. Returns {@code null} when a non-blank tenant does not match
 * any registered tenant — the caller treats that as a 404.
 */
@ApplicationScoped
public class CdiAgentCardRouter implements AgentCardRouter {

    @Inject
    @Any
    Instance<AgentCard> allCards;

    private @Nullable AgentCard defaultExtendedCard;
    private @Nullable AgentCard defaultPublicCard;

    @PostConstruct
    void init() {
        defaultExtendedCard = CdiUtils.resolveDefaultBean(
                allCards, Tenant.class, ExtendedAgentCard.class, "@ExtendedAgentCard");
        defaultPublicCard = CdiUtils.resolveDefaultBean(
                allCards, Tenant.class, PublicAgentCard.class, "@PublicAgentCard");
    }

    @Override
    public @Nullable AgentCard resolveExtendedCard(@Nullable String tenant) {
        if (tenant == null || tenant.isBlank()) {
            return defaultExtendedCard;
        }
        Instance<AgentCard> selected = allCards.select(
                new Tenant.Literal(tenant), ExtendedAgentCard.Literal.INSTANCE);
        if (selected.isResolvable()) {
            return selected.get();
        }
        if (selected.isAmbiguous()) {
            throw duplicateTenantBean("@ExtendedAgentCard", tenant);
        }
        return defaultExtendedCard;
    }

    @Override
    public @Nullable AgentCard resolvePublicCard(@Nullable String tenant) {
        if (tenant == null || tenant.isBlank()) {
            return defaultPublicCard;
        }
        Instance.Handle<AgentCard> publicHandle = null;
        Instance.Handle<AgentCard> bareHandle = null;
        List<Instance.Handle<AgentCard>> allHandles = new ArrayList<>();
        try {
            for (Instance.Handle<AgentCard> handle : allCards.handles()) {
                allHandles.add(handle);
                boolean matchesTenant = false;
                boolean hasPublic = false;
                boolean hasExtended = false;
                for (Annotation q : handle.getBean().getQualifiers()) {
                    if (q instanceof Tenant t && tenant.equals(t.value())) {
                        matchesTenant = true;
                    } else if (q instanceof PublicAgentCard) {
                        hasPublic = true;
                    } else if (q instanceof ExtendedAgentCard) {
                        hasExtended = true;
                    }
                }
                if (!matchesTenant) {
                    continue;
                }
                if (hasPublic) {
                    if (publicHandle != null) {
                        throw duplicateTenantBean("@PublicAgentCard", tenant);
                    }
                    publicHandle = handle;
                } else if (!hasExtended) {
                    if (bareHandle != null) {
                        throw duplicateTenantBean("bare public", tenant);
                    }
                    bareHandle = handle;
                }
            }
            Instance.Handle<AgentCard> selected = publicHandle != null ? publicHandle : bareHandle;
            return selected != null ? selected.get() : null;
        } finally {
            Instance.Handle<AgentCard> kept = publicHandle != null ? publicHandle : bareHandle;
            for (Instance.Handle<AgentCard> h : allHandles) {
                if (h != kept) {
                    h.close();
                }
            }
        }
    }

    private static IllegalStateException duplicateTenantBean(String cardType, String tenant) {
        return new IllegalStateException(
                String.format("Multiple tenant %s cards detected for tenant '%s'", cardType, tenant));
    }
}
