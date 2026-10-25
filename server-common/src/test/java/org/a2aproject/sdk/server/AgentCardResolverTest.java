package org.a2aproject.sdk.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.a2aproject.sdk.server.multitenancy.AgentCardRouter;
import org.a2aproject.sdk.server.multitenancy.TenantNotFoundException;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class AgentCardResolverTest {

    @Test
    void effectiveDefaultFallbackKeepsExactSupplierAndRouterOrder() {
        AgentCard[] cards = {card("default-public"), card("router-public"),
                card("default-extended"), card("router-extended")};
        List<String> order = List.of("default-public", "router-public:null", "default-extended", "router-extended:null");
        for (int selected = 0; selected < cards.length; selected++) {
            int index = selected;
            List<String> calls = new ArrayList<>();
            AgentCardResolver resolver = AgentCardResolver.of(() -> {
                calls.add("default-public");
                return index == 0 ? cards[0] : null;
            }, () -> {
                calls.add("default-extended");
                return index == 2 ? cards[2] : null;
            }, new AgentCardRouter() {
                @Override
                public @Nullable AgentCard resolvePublicCard(@Nullable String tenant) {
                    calls.add("router-public:" + tenant);
                    return index == 1 ? cards[1] : null;
                }

                @Override
                public @Nullable AgentCard resolveExtendedCard(@Nullable String tenant) {
                    calls.add("router-extended:" + tenant);
                    return index == 3 ? cards[3] : null;
                }
            });

            assertSame(cards[index], resolver.resolveEffectiveCard(" "));
            assertEquals(order.subList(0, index + 1), calls);
        }
    }

    @Test
    void routedTenantUsesOnlyItsPublicThenExtendedCards() {
        AgentCard defaultPublic = card("default-public");
        AgentCard defaultExtended = card("default-extended");
        AgentCard routedPublic = card("routed-public");
        AgentCard routedExtended = card("routed-extended");
        AgentCardResolver resolver = AgentCardResolver.of(() -> defaultPublic, () -> defaultExtended,
                router(routedPublic, routedExtended));
        assertSame(routedPublic, resolver.resolveEffectiveCard("/acme/"));
        assertSame(routedPublic, resolver.resolvePublicDiscoveryCard("/acme/"));
        assertSame(routedExtended, resolver.resolveExtendedDiscoveryCard("/acme/"));

        AgentCardResolver extendedOnly = AgentCardResolver.of(() -> defaultPublic, () -> defaultExtended,
                router(null, routedExtended));
        assertSame(routedExtended, extendedOnly.resolveEffectiveCard("acme"));
        assertThrows(TenantNotFoundException.class, () -> extendedOnly.resolvePublicDiscoveryCard("acme"));
    }

    @Test
    void unknownRoutedTenantDoesNotFallBackToDefaultCards() {
        AgentCardResolver resolver = AgentCardResolver.of(() -> card("default-public"),
                () -> card("default-extended"), router(null, null));
        TenantNotFoundException failure = assertThrows(TenantNotFoundException.class,
                () -> resolver.resolveEffectiveCard("unknown"));
        assertEquals("unknown", failure.getTenant());
        assertThrows(TenantNotFoundException.class, () -> resolver.resolvePublicDiscoveryCard("unknown"));
        assertNull(resolver.resolveExtendedDiscoveryCard("unknown"));
    }

    @Test
    void noRouterAllowsNonblankTenantToUseDefaultCards() {
        AgentCard publicCard = card("public");
        AgentCard extendedCard = card("extended");
        AgentCardResolver resolver = AgentCardResolver.of(() -> publicCard, () -> extendedCard, null);
        assertSame(publicCard, resolver.resolveEffectiveCard("acme"));
        assertSame(publicCard, resolver.resolvePublicDiscoveryCard("acme"));
        assertSame(extendedCard, resolver.resolveExtendedDiscoveryCard("acme"));

        AgentCardResolver extendedOnly = AgentCardResolver.of(() -> null, () -> extendedCard, null);
        assertSame(extendedCard, extendedOnly.resolveEffectiveCard("acme"));
        assertNull(extendedOnly.resolvePublicDiscoveryCard("acme"));
    }

    @Test
    void defaultDiscoveryUsesOnlyTheCorrectCardKind() {
        AgentCard publicCard = card("public");
        AgentCard extendedCard = card("extended");
        AgentCard routedPublic = card("routed-public");
        AgentCard routedExtended = card("routed-extended");
        AgentCardResolver resolver = AgentCardResolver.of(() -> publicCard, () -> extendedCard,
                router(routedPublic, routedExtended));
        assertSame(publicCard, resolver.resolvePublicDiscoveryCard(null));
        assertSame(extendedCard, resolver.resolveExtendedDiscoveryCard(null));

        AgentCardResolver routedOnly = AgentCardResolver.of(() -> null, () -> null,
                router(routedPublic, routedExtended));
        assertSame(routedPublic, routedOnly.resolvePublicDiscoveryCard("/"));
        assertNull(routedOnly.resolveExtendedDiscoveryCard(null));

        AgentCardResolver extendedOnly = AgentCardResolver.of(() -> null, () -> extendedCard,
                router(null, routedExtended));
        assertNull(extendedOnly.resolvePublicDiscoveryCard(null));
    }

    @Test
    void absentCardsFailEffectiveResolutionButNotDiscovery() {
        for (AgentCardRouter router : new AgentCardRouter[] {null, router(null, null)}) {
            AgentCardResolver resolver = AgentCardResolver.of(() -> null, () -> null, router);
            IllegalStateException failure = assertThrows(IllegalStateException.class,
                    () -> resolver.resolveEffectiveCard(null));
            assertEquals(AgentCardValidator.NO_AGENT_CARD_MESSAGE, failure.getMessage());
            assertNull(resolver.resolvePublicDiscoveryCard(null));
            assertNull(resolver.resolveExtendedDiscoveryCard(null));
        }
    }

    @Test
    void canonicalTenantNormalizesDefaultSpellingsAndPermittedSlashes() {
        for (String tenant : new String[] {null, "", " ", "/", "//"}) {
            assertNull(AgentCardResolver.normalizeTenantForResolution(tenant));
        }
        for (String tenant : List.of("acme", "/acme", "acme/", "/acme/")) {
            assertEquals("acme", AgentCardResolver.normalizeTenantForResolution(tenant));
        }
        assertThrows(IllegalArgumentException.class,
                () -> AgentCardResolver.normalizeTenantForResolution("acme/other"));
        assertThrows(IllegalArgumentException.class,
                () -> AgentCardResolver.normalizeTenantForResolution(" acme "));
        assertThrows(IllegalArgumentException.class,
                () -> AgentCardResolver.normalizeTenantForResolution("a".repeat(257)));
    }

    static AgentCard card(String name) {
        return card(name, AgentCapabilities.builder().build());
    }

    static AgentCard card(String name, AgentCapabilities capabilities) {
        return AgentCard.builder().name(name).description("Test Agent")
                .supportedInterfaces(List.of(new AgentInterface(TransportProtocol.JSONRPC.asString(),
                        "http://localhost:9999", null, "1.0")))
                .version("1.0.0").capabilities(capabilities)
                .defaultInputModes(List.of("text")).defaultOutputModes(List.of("text"))
                .skills(List.of()).build();
    }

    private static AgentCardRouter router(@Nullable AgentCard publicCard, @Nullable AgentCard extendedCard) {
        return new AgentCardRouter() {
            @Override
            public @Nullable AgentCard resolvePublicCard(@Nullable String tenant) {
                return publicCard;
            }

            @Override
            public @Nullable AgentCard resolveExtendedCard(@Nullable String tenant) {
                return extendedCard;
            }
        };
    }
}
