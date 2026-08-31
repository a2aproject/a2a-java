package org.a2aproject.sdk.extras.multitenancy;

import java.lang.annotation.Annotation;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.a2aproject.sdk.server.ExtendedAgentCard;
import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.server.multitenancy.AgentCardRouter;
import org.a2aproject.sdk.server.util.CdiUtils;
import org.a2aproject.sdk.spec.AgentCard;
import org.jspecify.annotations.Nullable;

/**
 * CDI-based {@link AgentCardRouter} that resolves tenant-specific {@link AgentCard} beans.
 * <p>
 * Extended cards are resolved via {@code @Tenant("x") @ExtendedAgentCard}-qualified beans.
 * Tenant-specific public cards are resolved via {@code @Tenant("x")}-qualified beans that
 * carry neither {@code @ExtendedAgentCard} nor {@code @PublicAgentCard}. The {@code @PublicAgentCard}
 * qualifier must NOT be used on tenant-specific public cards because it would cause CDI
 * ambiguity on injection points requesting the default public card.
 * <p>
 * Returns {@code null} from {@link #resolvePublicCard} when the tenant is {@code null},
 * blank, or does not match any registered tenant — the caller decides whether to fall
 * back to the default card or return 404 based on the original input.
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
        return defaultExtendedCard;
    }

    @Override
    public @Nullable AgentCard resolvePublicCard(@Nullable String tenant) {
        if (tenant == null || tenant.isBlank()) {
            return null;
        }
        for (Instance.Handle<AgentCard> handle : allCards.handles()) {
            Set<Annotation> qualifiers = handle.getBean().getQualifiers();
            boolean matchesTenant = qualifiers.stream()
                    .anyMatch(a -> a instanceof Tenant t && tenant.equals(t.value()));
            if (matchesTenant
                    && qualifiers.stream().noneMatch(ExtendedAgentCard.class::isInstance)
                    && qualifiers.stream().noneMatch(PublicAgentCard.class::isInstance)) {
                return handle.get();
            }
        }
        return null;
    }
}
