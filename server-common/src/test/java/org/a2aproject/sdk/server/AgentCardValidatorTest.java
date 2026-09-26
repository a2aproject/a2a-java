package org.a2aproject.sdk.server;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.a2aproject.sdk.server.multitenancy.AgentCardRouter;
import org.a2aproject.sdk.server.multitenancy.TenantNotFoundException;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

public class AgentCardValidatorTest {

    private AgentCard.Builder createTestAgentCardBuilder() {
        return AgentCard.builder()
                .name("Test Agent")
                .description("Test Description")
                .supportedInterfaces(Collections.singletonList(
                        new AgentInterface(TransportProtocol.JSONRPC.asString(), "http://localhost:9999")))
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder().build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(Collections.emptyList());
    }

    @Test
    void testValidationWithSimpleAgentCard() {
        // Create a simple AgentCard (uses default JSONRPC transport)
        AgentCard agentCard = createTestAgentCardBuilder()
                .build();

        // Define available transports
        Set<String> availableTransports = Set.of(TransportProtocol.JSONRPC.asString());

        // Validation should now pass
        assertDoesNotThrow(() -> AgentCardValidator.validateTransportConfiguration(agentCard, availableTransports));
    }

    @Test
    void testValidationWithMultipleTransports() {
        // Create AgentCard that specifies multiple transports
        AgentCard agentCard = createTestAgentCardBuilder()
                .supportedInterfaces(List.of(
                        new AgentInterface(TransportProtocol.JSONRPC.asString(), "http://localhost:9999"),
                        new AgentInterface(TransportProtocol.GRPC.asString(), "http://localhost:9000")
                ))
                .build();

        // Define available transports
        Set<String> availableTransports =
                Set.of(TransportProtocol.JSONRPC.asString(), TransportProtocol.GRPC.asString());

        // Validation should now pass
        assertDoesNotThrow(() -> AgentCardValidator.validateTransportConfiguration(agentCard, availableTransports));
    }

    @Test
    void testLogWarningWhenExtraTransportsFound() {
        // Create an AgentCard with only JSONRPC
        AgentCard agentCard = createTestAgentCardBuilder()
                .supportedInterfaces(Collections.singletonList(new AgentInterface(TransportProtocol.JSONRPC.asString(), "http://localhost:9999")))
                .build();

        // Define available transports (more than in AgentCard)
        Set<String> availableTransports =
                Set.of(TransportProtocol.JSONRPC.asString(), TransportProtocol.GRPC.asString());

        // Capture logs
        Logger logger = Logger.getLogger(AgentCardValidator.class.getName());
        TestLogHandler testLogHandler = new TestLogHandler();
        logger.addHandler(testLogHandler);

        try {
            AgentCardValidator.validateTransportConfiguration(agentCard, availableTransports);
        } finally {
            logger.removeHandler(testLogHandler);
        }

        // Assert that a warning was logged
        assertTrue(testLogHandler.getLogMessages().stream()
                .anyMatch(msg -> msg.contains("AgentCard does not include all available transports. Missing: [GRPC]")));
    }

    @Test
    void testValidationWithUnavailableTransport() {
        // Create a simple AgentCard (uses default JSONRPC transport)
        AgentCard agentCard = createTestAgentCardBuilder()
                .build();

        // Define available transports (empty)
        Set<String> availableTransports = Collections.emptySet();

        // Should throw exception because no transports are available
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> AgentCardValidator.validateTransportConfiguration(agentCard, availableTransports));
        assertTrue(exception.getMessage().contains("unavailable transports: [JSONRPC]"));
    }

    @Test
    void testGlobalSkipProperty() {
        System.setProperty(AgentCardValidator.SKIP_PROPERTY, "true");
        try {
            AgentCard agentCard = createTestAgentCardBuilder()
                    .build();

            Set<String> availableTransports = Collections.emptySet();

            assertDoesNotThrow(() -> AgentCardValidator.validateTransportConfiguration(agentCard, availableTransports));
        } finally {
            System.clearProperty(AgentCardValidator.SKIP_PROPERTY);
        }
    }

    @Test
    void testSkipJsonrpcProperty() {
        System.setProperty(AgentCardValidator.SKIP_JSONRPC_PROPERTY, "true");
        try {
            AgentCard agentCard = createTestAgentCardBuilder()
                    .supportedInterfaces(Collections.singletonList(new AgentInterface(TransportProtocol.JSONRPC.asString(), "http://localhost:9999")))
                    .build();

            Set<String> availableTransports = Set.of(TransportProtocol.GRPC.asString());

            assertDoesNotThrow(() -> AgentCardValidator.validateTransportConfiguration(agentCard, availableTransports));
        } finally {
            System.clearProperty(AgentCardValidator.SKIP_JSONRPC_PROPERTY);
        }
    }

    @Test
    void testSkipGrpcProperty() {
        System.setProperty(AgentCardValidator.SKIP_GRPC_PROPERTY, "true");
        try {
            AgentCard agentCard = createTestAgentCardBuilder()
                    .supportedInterfaces(Collections.singletonList(new AgentInterface(TransportProtocol.GRPC.asString(), "http://localhost:9000")))
                    .build();

            Set<String> availableTransports = Set.of(TransportProtocol.JSONRPC.asString());

            assertDoesNotThrow(() -> AgentCardValidator.validateTransportConfiguration(agentCard, availableTransports));
        } finally {
            System.clearProperty(AgentCardValidator.SKIP_GRPC_PROPERTY);
        }
    }

    @Test
    void testSkipRestProperty() {
        System.setProperty(AgentCardValidator.SKIP_REST_PROPERTY, "true");
        try {
            AgentCard agentCard = createTestAgentCardBuilder()
                    .supportedInterfaces(List.of(
                            new AgentInterface(TransportProtocol.HTTP_JSON.asString(), "http://localhost:8080")
                    ))
                    .build();

            Set<String> availableTransports = Set.of(TransportProtocol.JSONRPC.asString());

            assertDoesNotThrow(() -> AgentCardValidator.validateTransportConfiguration(agentCard, availableTransports));
        } finally {
            System.clearProperty(AgentCardValidator.SKIP_REST_PROPERTY);
        }
    }

    @Test
    void testMultipleTransportsWithMixedSkipProperties() {
        System.setProperty(AgentCardValidator.SKIP_GRPC_PROPERTY, "true");
        try {
            AgentCard agentCard = createTestAgentCardBuilder()
                    .supportedInterfaces(List.of(
                            new AgentInterface(TransportProtocol.JSONRPC.asString(), "http://localhost:9999"),
                            new AgentInterface(TransportProtocol.GRPC.asString(), "http://localhost:9000"),
                            new AgentInterface(TransportProtocol.HTTP_JSON.asString(), "http://localhost:8080")
                    ))
                    .build();

            Set<String> availableTransports = Set.of(TransportProtocol.JSONRPC.asString());

            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> AgentCardValidator.validateTransportConfiguration(agentCard, availableTransports));
            assertTrue(exception.getMessage().contains("unavailable transports: [HTTP+JSON]"));
        } finally {
            System.clearProperty(AgentCardValidator.SKIP_GRPC_PROPERTY);
        }
    }

    @Test
    void testSkipPropertiesFilterWarnings() {
        System.setProperty(AgentCardValidator.SKIP_GRPC_PROPERTY, "true");
        try {
            AgentCard agentCard = createTestAgentCardBuilder()
                    .supportedInterfaces(Collections.singletonList(new AgentInterface(TransportProtocol.JSONRPC.asString(), "http://localhost:9999")))
                    .build();

            Set<String> availableTransports = Set.of(
                    TransportProtocol.JSONRPC.asString(),
                    TransportProtocol.GRPC.asString(),
                    TransportProtocol.HTTP_JSON.asString()
            );

            Logger logger = Logger.getLogger(AgentCardValidator.class.getName());
            TestLogHandler testLogHandler = new TestLogHandler();
            logger.addHandler(testLogHandler);

            try {
                AgentCardValidator.validateTransportConfiguration(agentCard, availableTransports);
            } finally {
                logger.removeHandler(testLogHandler);
            }

            boolean foundWarning = testLogHandler.getLogMessages().stream()
                    .anyMatch(msg -> msg.contains("Missing: [HTTP+JSON]"));
            assertTrue(foundWarning);

            boolean grpcMentioned = testLogHandler.getLogMessages().stream()
                    .anyMatch(msg -> msg.contains("GRPC"));
            assertFalse(grpcMentioned);
        } finally {
            System.clearProperty(AgentCardValidator.SKIP_GRPC_PROPERTY);
        }
    }

    @Test
    void resolveAndValidateOnceRetriesAfterFailure() {
        AgentCard card = createTestAgentCardBuilder().build();
        Set<AgentCard> validatedCards = AgentCardValidator.newValidatedCardsSet();
        AtomicInteger validationCount = new AtomicInteger(0);

        assertThrows(IllegalStateException.class, () ->
                AgentCardValidator.resolveAndValidateOnce(
                        () -> card, validatedCards, c -> {
                            validationCount.incrementAndGet();
                            throw new IllegalStateException("transient failure");
                        }));

        assertFalse(validatedCards.contains(card), "card should be removed after failure");
        assertEquals(1, validationCount.get());

        AgentCard result = AgentCardValidator.resolveAndValidateOnce(
                () -> card, validatedCards, c -> validationCount.incrementAndGet());

        assertTrue(validatedCards.contains(card), "card should be in set after success");
        assertEquals(2, validationCount.get());
        assertEquals(card, result);
    }

    @Test
    void resolveAndValidateOnceSkipsValidationOnSubsequentCalls() {
        AgentCard card = createTestAgentCardBuilder().build();
        Set<AgentCard> validatedCards = AgentCardValidator.newValidatedCardsSet();
        AtomicInteger validationCount = new AtomicInteger(0);

        AgentCardValidator.resolveAndValidateOnce(
                () -> card, validatedCards, c -> validationCount.incrementAndGet());
        AgentCardValidator.resolveAndValidateOnce(
                () -> card, validatedCards, c -> validationCount.incrementAndGet());

        assertEquals(1, validationCount.get(), "validation should run only once for the same card");
    }

    @Test
    void resolveAndValidateOnceValidatesEachDistinctCard() {
        AgentCard card1 = createTestAgentCardBuilder().name("card-1").build();
        AgentCard card2 = createTestAgentCardBuilder().name("card-2").build();
        Set<AgentCard> validatedCards = AgentCardValidator.newValidatedCardsSet();
        AtomicInteger validationCount = new AtomicInteger(0);

        AgentCardValidator.resolveAndValidateOnce(
                () -> card1, validatedCards, c -> validationCount.incrementAndGet());
        AgentCardValidator.resolveAndValidateOnce(
                () -> card2, validatedCards, c -> validationCount.incrementAndGet());

        assertEquals(2, validationCount.get(), "validation should run once per distinct card");
        assertTrue(validatedCards.contains(card1));
        assertTrue(validatedCards.contains(card2));
    }

    @Test
    void resolveWithFallbackUsesPublicCardWhenPresent() {
        System.setProperty(AgentCardValidator.SKIP_PROPERTY, "true");
        try {
            AgentCard publicCard = createTestAgentCardBuilder().name("public").build();
            AgentCard extendedCard = createTestAgentCardBuilder().name("extended").build();
            Set<AgentCard> validatedCards = AgentCardValidator.newValidatedCardsSet();

            AgentCard result = AgentCardValidator.resolveWithFallback(
                    new FixedInstance<>(publicCard), new FixedInstance<>(extendedCard), validatedCards);

            assertEquals("public", result.name());
        } finally {
            System.clearProperty(AgentCardValidator.SKIP_PROPERTY);
        }
    }

    @Test
    void resolveWithFallbackFallsBackToExtendedCard() {
        System.setProperty(AgentCardValidator.SKIP_PROPERTY, "true");
        try {
            AgentCard extendedCard = createTestAgentCardBuilder().name("extended").build();
            Set<AgentCard> validatedCards = AgentCardValidator.newValidatedCardsSet();

            AgentCard result = AgentCardValidator.resolveWithFallback(
                    FixedInstance.empty(), new FixedInstance<>(extendedCard), validatedCards);

            assertEquals("extended", result.name());
        } finally {
            System.clearProperty(AgentCardValidator.SKIP_PROPERTY);
        }
    }

    @Test
    void resolveWithFallbackThrowsWhenBothAbsent() {
        Set<AgentCard> validatedCards = AgentCardValidator.newValidatedCardsSet();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                AgentCardValidator.resolveWithFallback(
                        FixedInstance.empty(), FixedInstance.empty(), validatedCards));

        assertEquals(AgentCardValidator.NO_AGENT_CARD_MESSAGE, ex.getMessage());
    }

    @Test
    void resolveWithFallbackThrowsWhenExtendedIsNull() {
        Set<AgentCard> validatedCards = AgentCardValidator.newValidatedCardsSet();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                AgentCardValidator.resolveWithFallback(
                        FixedInstance.empty(), null, validatedCards));

        assertEquals(AgentCardValidator.NO_AGENT_CARD_MESSAGE, ex.getMessage());
    }

    @Test
    void requireFirstReturnsPublicCard() {
        AgentCard publicCard = createTestAgentCardBuilder().name("public").build();
        AgentCard extendedCard = createTestAgentCardBuilder().name("extended").build();

        assertEquals("public", AgentCardValidator.requireFirst(publicCard, extendedCard).name());
    }

    @Test
    void requireFirstReturnsExtendedWhenPublicIsNull() {
        AgentCard extendedCard = createTestAgentCardBuilder().name("extended").build();

        assertEquals("extended", AgentCardValidator.requireFirst(null, extendedCard).name());
    }

    @Test
    void requireFirstThrowsWhenBothNull() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                AgentCardValidator.requireFirst(null, null));

        assertEquals(AgentCardValidator.NO_AGENT_CARD_MESSAGE, ex.getMessage());
    }

    @Test
    void resolveWithFallbackUsesRouterWhenNoDefaultBean() {
        System.setProperty(AgentCardValidator.SKIP_PROPERTY, "true");
        try {
            AgentCard routerCard = createTestAgentCardBuilder().name("router-card").build();
            AgentCardRouter router = new AgentCardRouter() {
                @Override
                public @Nullable AgentCard resolveExtendedCard(@Nullable String tenant) {
                    return null;
                }

                @Override
                public @Nullable AgentCard resolvePublicCard(@Nullable String tenant) {
                    return routerCard;
                }
            };
            Set<AgentCard> validatedCards = AgentCardValidator.newValidatedCardsSet();

            AgentCard result = AgentCardValidator.resolveWithFallback(
                    FixedInstance.empty(), FixedInstance.empty(), router, "tenant-1", validatedCards);

            assertEquals("router-card", result.name());
        } finally {
            System.clearProperty(AgentCardValidator.SKIP_PROPERTY);
        }
    }

    @Test
    void resolveWithFallbackPrefersTenantCardOverDefaultBean() {
        System.setProperty(AgentCardValidator.SKIP_PROPERTY, "true");
        try {
            AgentCard publicCard = createTestAgentCardBuilder().name("default").build();
            AgentCard routerCard = createTestAgentCardBuilder().name("tenant-card").build();
            AgentCardRouter router = new AgentCardRouter() {
                @Override
                public @Nullable AgentCard resolveExtendedCard(@Nullable String tenant) {
                    return null;
                }

                @Override
                public @Nullable AgentCard resolvePublicCard(@Nullable String tenant) {
                    return "tenant-1".equals(tenant) ? routerCard : null;
                }
            };
            Set<AgentCard> validatedCards = AgentCardValidator.newValidatedCardsSet();

            AgentCard result = AgentCardValidator.resolveWithFallback(
                    new FixedInstance<>(publicCard), FixedInstance.empty(), router, "tenant-1", validatedCards);

            assertEquals("tenant-card", result.name());
        } finally {
            System.clearProperty(AgentCardValidator.SKIP_PROPERTY);
        }
    }

    @Test
    void resolveWithFallbackPrefersDefaultBeanOverRouterWithoutTenant() {
        System.setProperty(AgentCardValidator.SKIP_PROPERTY, "true");
        try {
            AgentCard publicCard = createTestAgentCardBuilder().name("default").build();
            AgentCard routerCard = createTestAgentCardBuilder().name("router-default").build();
            AgentCardRouter router = new AgentCardRouter() {
                @Override
                public @Nullable AgentCard resolveExtendedCard(@Nullable String tenant) {
                    return null;
                }

                @Override
                public @Nullable AgentCard resolvePublicCard(@Nullable String tenant) {
                    return routerCard;
                }
            };
            Set<AgentCard> validatedCards = AgentCardValidator.newValidatedCardsSet();

            AgentCard result = AgentCardValidator.resolveWithFallback(
                    new FixedInstance<>(publicCard), FixedInstance.empty(), router, null, validatedCards);

            assertEquals("default", result.name());
        } finally {
            System.clearProperty(AgentCardValidator.SKIP_PROPERTY);
        }
    }

    @Test
    void resolveWithFallbackPrefersTenantPublicCardOverDefaultExtendedCard() {
        System.setProperty(AgentCardValidator.SKIP_PROPERTY, "true");
        try {
            AgentCard defaultExtended = createTestAgentCardBuilder().name("default-extended").build();
            AgentCard tenantPublic = createTestAgentCardBuilder().name("tenant-public").build();
            AgentCardRouter router = new AgentCardRouter() {
                @Override
                public @Nullable AgentCard resolveExtendedCard(@Nullable String tenant) {
                    return null;
                }

                @Override
                public @Nullable AgentCard resolvePublicCard(@Nullable String tenant) {
                    return "tenant-1".equals(tenant) ? tenantPublic : null;
                }
            };
            Set<AgentCard> validatedCards = AgentCardValidator.newValidatedCardsSet();

            AgentCard result = AgentCardValidator.resolveWithFallback(
                    FixedInstance.empty(), new FixedInstance<>(defaultExtended), router, "tenant-1", validatedCards);

            assertEquals("tenant-public", result.name());
        } finally {
            System.clearProperty(AgentCardValidator.SKIP_PROPERTY);
        }
    }

    @Test
    void resolveWithFallbackThrowsForUnknownTenantEvenWhenDefaultExtendedExists() {
        AgentCardRouter router = new AgentCardRouter() {
            @Override
            public @Nullable AgentCard resolveExtendedCard(@Nullable String tenant) {
                return null;
            }

            @Override
            public @Nullable AgentCard resolvePublicCard(@Nullable String tenant) {
                return null;
            }
        };
        Set<AgentCard> validatedCards = AgentCardValidator.newValidatedCardsSet();
        AgentCard defaultExtended = createTestAgentCardBuilder().name("default-extended").build();

        TenantNotFoundException ex = assertThrows(TenantNotFoundException.class, () ->
                AgentCardValidator.resolveWithFallback(
                        FixedInstance.empty(), new FixedInstance<>(defaultExtended), router, "unknown-tenant",
                        validatedCards));

        assertEquals("unknown-tenant", ex.getTenant());
    }

    @Test
    void resolveWithFallbackThrowsForUnknownTenantEvenWhenDefaultPublicExists() {
        AgentCardRouter router = new AgentCardRouter() {
            @Override
            public @Nullable AgentCard resolveExtendedCard(@Nullable String tenant) {
                return null;
            }

            @Override
            public @Nullable AgentCard resolvePublicCard(@Nullable String tenant) {
                return null;
            }
        };
        Set<AgentCard> validatedCards = AgentCardValidator.newValidatedCardsSet();
        AgentCard publicCard = createTestAgentCardBuilder().name("default").build();

        TenantNotFoundException ex = assertThrows(TenantNotFoundException.class, () ->
                AgentCardValidator.resolveWithFallback(
                        new FixedInstance<>(publicCard), FixedInstance.empty(), router, "unknown-tenant",
                        validatedCards));

        assertEquals("unknown-tenant", ex.getTenant());
    }

    @Test
    void resolveWithFallbackUsesRouterExtendedCardWhenPublicCardIsNull() {
        System.setProperty(AgentCardValidator.SKIP_PROPERTY, "true");
        try {
            AgentCard extCard = createTestAgentCardBuilder().name("router-extended").build();
            AgentCardRouter router = new AgentCardRouter() {
                @Override
                public @Nullable AgentCard resolveExtendedCard(@Nullable String tenant) {
                    return extCard;
                }

                @Override
                public @Nullable AgentCard resolvePublicCard(@Nullable String tenant) {
                    return null;
                }
            };
            Set<AgentCard> validatedCards = AgentCardValidator.newValidatedCardsSet();

            AgentCard result = AgentCardValidator.resolveWithFallback(
                    FixedInstance.empty(), FixedInstance.empty(), router, "tenant-1", validatedCards);

            assertEquals("router-extended", result.name());
        } finally {
            System.clearProperty(AgentCardValidator.SKIP_PROPERTY);
        }
    }

    @Test
    void resolveWithFallbackThrowsWhenRouterReturnsNull() {
        AgentCardRouter router = new AgentCardRouter() {
            @Override
            public @Nullable AgentCard resolveExtendedCard(@Nullable String tenant) {
                return null;
            }

            @Override
            public @Nullable AgentCard resolvePublicCard(@Nullable String tenant) {
                return null;
            }
        };
        Set<AgentCard> validatedCards = AgentCardValidator.newValidatedCardsSet();

        TenantNotFoundException ex = assertThrows(TenantNotFoundException.class, () ->
                AgentCardValidator.resolveWithFallback(
                        FixedInstance.empty(), FixedInstance.empty(), router, "unknown", validatedCards));

        assertEquals("unknown", ex.getTenant());
    }

    // A simple log handler for testing
    private static class TestLogHandler extends Handler {
        private final List<String> logMessages = new java.util.ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            logMessages.add(record.getMessage());
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }

        public List<String> getLogMessages() {
            return logMessages;
        }
    }
}
