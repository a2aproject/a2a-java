package org.a2aproject.sdk.extras.multitenancy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;

import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;

import org.a2aproject.sdk.server.ExtendedAgentCard;
import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.server.multitenancy.Tenant;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CdiAgentCardRouterTest {

    private SeContainer container;

    private void startContainer(Class<?>... beanClasses) {
        SeContainerInitializer initializer = SeContainerInitializer.newInstance()
                .disableDiscovery()
                .addBeanClasses(CdiAgentCardRouter.class);
        for (Class<?> beanClass : beanClasses) {
            initializer.addBeanClasses(beanClass);
        }
        container = initializer.initialize();
    }

    @AfterEach
    void closeContainer() {
        if (container != null) {
            container.close();
        }
    }

    @Test
    void knownTenantResolvesToTenantSpecificCard() {
        startContainer(DefaultAndAcmeCardProducer.class);
        CdiAgentCardRouter router = container.select(CdiAgentCardRouter.class).get();
        assertEquals("acme-extended", router.resolveExtendedCard("acme").name());
    }

    @Test
    void unknownTenantFallsBackToDefaultCard() {
        startContainer(DefaultAndAcmeCardProducer.class);
        CdiAgentCardRouter router = container.select(CdiAgentCardRouter.class).get();
        assertEquals("default-extended", router.resolveExtendedCard("unknown").name());
    }

    @Test
    void nullTenantReturnsDefaultCard() {
        startContainer(DefaultAndAcmeCardProducer.class);
        CdiAgentCardRouter router = container.select(CdiAgentCardRouter.class).get();
        assertEquals("default-extended", router.resolveExtendedCard(null).name());
    }

    @Test
    void blankTenantReturnsDefaultCard() {
        startContainer(DefaultAndAcmeCardProducer.class);
        CdiAgentCardRouter router = container.select(CdiAgentCardRouter.class).get();
        assertEquals("default-extended", router.resolveExtendedCard("").name());
        assertEquals("default-extended", router.resolveExtendedCard("   ").name());
    }

    @Test
    void noDefaultCardReturnsNull() {
        startContainer(TenantOnlyCardProducer.class);
        CdiAgentCardRouter router = container.select(CdiAgentCardRouter.class).get();
        assertNull(router.resolveExtendedCard(null));
        assertNull(router.resolveExtendedCard("unknown"));
    }

    @Test
    void publicCardKnownTenantResolvesToTenantSpecific() {
        startContainer(FullCardProducer.class);
        CdiAgentCardRouter router = container.select(CdiAgentCardRouter.class).get();
        assertEquals("acme-public", router.resolvePublicCard("acme").name());
    }

    @Test
    void publicCardUnknownTenantReturnsNull() {
        startContainer(FullCardProducer.class);
        CdiAgentCardRouter router = container.select(CdiAgentCardRouter.class).get();
        assertNull(router.resolvePublicCard("unknown"));
    }

    @Test
    void publicCardNullTenantReturnsDefaultCard() {
        startContainer(FullCardProducer.class);
        CdiAgentCardRouter router = container.select(CdiAgentCardRouter.class).get();
        assertEquals("default-public", router.resolvePublicCard(null).name());
    }

    @Test
    void publicCardBlankTenantReturnsDefaultCard() {
        startContainer(FullCardProducer.class);
        CdiAgentCardRouter router = container.select(CdiAgentCardRouter.class).get();
        assertEquals("default-public", router.resolvePublicCard("").name());
        assertEquals("default-public", router.resolvePublicCard("   ").name());
    }

    @Test
    void publicCardNoDefaultReturnsNull() {
        startContainer(TenantOnlyCardProducer.class);
        CdiAgentCardRouter router = container.select(CdiAgentCardRouter.class).get();
        assertNull(router.resolvePublicCard(null));
        assertNull(router.resolvePublicCard("unknown"));
    }

    @Test
    void publicCardWithPublicQualifierResolvesToTenantSpecific() {
        startContainer(DefaultPublicAndExtendedCardProducer.class, PublicQualifiedTenantCardProducer.class);
        CdiAgentCardRouter router = container.select(CdiAgentCardRouter.class).get();
        assertEquals("acme-public-qualified", router.resolvePublicCard("acme").name());
    }

    @Test
    void publicCardWithBothPublicAndExtendedQualifierResolvesToTenantSpecific() {
        startContainer(DefaultPublicAndExtendedCardProducer.class, PublicAndExtendedTenantCardProducer.class);
        CdiAgentCardRouter router = container.select(CdiAgentCardRouter.class).get();
        assertEquals("acme-both", router.resolvePublicCard("acme").name());
    }

    @Test
    void explicitPublicCardTakesPrecedenceOverBareTenantCard() {
        startContainer(DefaultPublicAndExtendedCardProducer.class, BareAndPublicQualifiedTenantCardProducer.class);
        CdiAgentCardRouter router = container.select(CdiAgentCardRouter.class).get();
        assertEquals("acme-public-qualified", router.resolvePublicCard("acme").name());
    }

    @Test
    void duplicatePublicQualifiedCardsThrows() {
        startContainer(DefaultPublicAndExtendedCardProducer.class, DuplicatePublicQualifiedTenantCardProducer.class);
        CdiAgentCardRouter router = container.select(CdiAgentCardRouter.class).get();
        assertThrows(IllegalStateException.class, () -> router.resolvePublicCard("acme"));
    }

    @Test
    void duplicateBareTenantCardsThrows() {
        startContainer(DefaultPublicAndExtendedCardProducer.class, DuplicateBareTenantCardProducer.class);
        CdiAgentCardRouter router = container.select(CdiAgentCardRouter.class).get();
        assertThrows(IllegalStateException.class, () -> router.resolvePublicCard("acme"));
    }

    @Test
    void duplicateExtendedTenantCardsThrows() {
        startContainer(DefaultPublicAndExtendedCardProducer.class, DuplicateExtendedTenantCardProducer.class);
        CdiAgentCardRouter router = container.select(CdiAgentCardRouter.class).get();
        assertThrows(IllegalStateException.class, () -> router.resolveExtendedCard("acme"));
    }

    @Test
    void dualQualifiedCardResolvedByBothPublicAndExtended() {
        startContainer(DefaultPublicAndExtendedCardProducer.class, PublicAndExtendedTenantCardProducer.class);
        CdiAgentCardRouter router = container.select(CdiAgentCardRouter.class).get();
        AgentCard publicCard = router.resolvePublicCard("acme");
        AgentCard extendedCard = router.resolveExtendedCard("acme");
        assertEquals("acme-both", publicCard.name());
        assertEquals("acme-both", extendedCard.name());
    }

    private static AgentCard buildCard(String name) {
        return AgentCard.builder()
                .name(name)
                .description(name)
                .version("1.0.0")
                .supportedInterfaces(Collections.singletonList(new AgentInterface("jsonrpc", "http://localhost:8080")))
                .capabilities(AgentCapabilities.builder().build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .build();
    }

    static class DefaultAndAcmeCardProducer {

        @Produces
        @ExtendedAgentCard
        AgentCard defaultExtendedCard() {
            return buildCard("default-extended");
        }

        @Produces
        @Tenant("acme")
        @ExtendedAgentCard
        AgentCard acmeExtendedCard() {
            return buildCard("acme-extended");
        }
    }

    static class TenantOnlyCardProducer {

        @Produces
        @Tenant("acme")
        @ExtendedAgentCard
        AgentCard acmeExtendedCard() {
            return buildCard("acme-extended");
        }
    }

    static class FullCardProducer {

        @Produces
        @PublicAgentCard
        AgentCard defaultPublicCard() {
            return buildCard("default-public");
        }

        @Produces
        @Tenant("acme")
        AgentCard acmePublicCard() {
            return buildCard("acme-public");
        }

        @Produces
        @ExtendedAgentCard
        AgentCard defaultExtendedCard() {
            return buildCard("default-extended");
        }

        @Produces
        @Tenant("acme")
        @ExtendedAgentCard
        AgentCard acmeExtendedCard() {
            return buildCard("acme-extended");
        }
    }

    static class DefaultPublicAndExtendedCardProducer {

        @Produces
        @PublicAgentCard
        AgentCard defaultPublicCard() {
            return buildCard("default-public");
        }

        @Produces
        @ExtendedAgentCard
        AgentCard defaultExtendedCard() {
            return buildCard("default-extended");
        }
    }

    static class PublicQualifiedTenantCardProducer {

        @Produces
        @Tenant("acme")
        @PublicAgentCard
        AgentCard acmePublicCard() {
            return buildCard("acme-public-qualified");
        }
    }

    static class PublicAndExtendedTenantCardProducer {

        @Produces
        @Tenant("acme")
        @PublicAgentCard
        @ExtendedAgentCard
        AgentCard acmeBothCard() {
            return buildCard("acme-both");
        }
    }

    static class BareAndPublicQualifiedTenantCardProducer {

        @Produces
        @Tenant("acme")
        AgentCard acmeBareCard() {
            return buildCard("acme-bare");
        }

        @Produces
        @Tenant("acme")
        @PublicAgentCard
        AgentCard acmePublicCard() {
            return buildCard("acme-public-qualified");
        }
    }

    static class DuplicatePublicQualifiedTenantCardProducer {

        @Produces
        @Tenant("acme")
        @PublicAgentCard
        AgentCard acmePublicCard1() {
            return buildCard("acme-public-1");
        }

        @Produces
        @Tenant("acme")
        @PublicAgentCard
        AgentCard acmePublicCard2() {
            return buildCard("acme-public-2");
        }
    }

    static class DuplicateBareTenantCardProducer {

        @Produces
        @Tenant("acme")
        AgentCard acmeBareCard1() {
            return buildCard("acme-bare-1");
        }

        @Produces
        @Tenant("acme")
        AgentCard acmeBareCard2() {
            return buildCard("acme-bare-2");
        }
    }

    static class DuplicateExtendedTenantCardProducer {

        @Produces
        @Tenant("acme")
        @ExtendedAgentCard
        AgentCard acmeExtendedCard1() {
            return buildCard("acme-extended-1");
        }

        @Produces
        @Tenant("acme")
        @ExtendedAgentCard
        AgentCard acmeExtendedCard2() {
            return buildCard("acme-extended-2");
        }
    }
}
