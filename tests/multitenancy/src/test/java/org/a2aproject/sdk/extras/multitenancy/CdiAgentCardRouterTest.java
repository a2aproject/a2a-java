package org.a2aproject.sdk.extras.multitenancy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.List;

import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;

import org.a2aproject.sdk.server.ExtendedAgentCard;
import org.a2aproject.sdk.server.PublicAgentCard;
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
    void publicCardNullTenantReturnsNull() {
        startContainer(FullCardProducer.class);
        CdiAgentCardRouter router = container.select(CdiAgentCardRouter.class).get();
        assertNull(router.resolvePublicCard(null));
    }

    @Test
    void publicCardNoDefaultReturnsNull() {
        startContainer(TenantOnlyCardProducer.class);
        CdiAgentCardRouter router = container.select(CdiAgentCardRouter.class).get();
        assertNull(router.resolvePublicCard(null));
        assertNull(router.resolvePublicCard("unknown"));
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
}
