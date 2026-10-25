package org.a2aproject.sdk.server;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
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

    @Test
    void cachingValidatorWaitsForSuccessfulValidationBeforeReturning() throws Exception {
        AgentCard card = createTestAgentCardBuilder().build();
        CountDownLatch validating = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        AtomicInteger invocations = new AtomicInteger();
        CachingAgentCardTransportValidator validator = new CachingAgentCardTransportValidator(selected -> {
            invocations.incrementAndGet();
            validating.countDown();
            try {
                assertTrue(release.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        });
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> validator.validate(card));
            assertTrue(validating.await(5, TimeUnit.SECONDS));
            Future<?> second = executor.submit(() -> {
                secondStarted.countDown();
                validator.validate(card);
            });
            assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> second.get(100, TimeUnit.MILLISECONDS));
            release.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
            assertEquals(1, invocations.get());
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void failedValidationLeavesNoCacheEntryAndRetriesSuccessfully() {
        AgentCard card = createTestAgentCardBuilder().build();
        ConcurrentMap<AgentCard, Boolean> cache = AgentCardValidator.newValidatedCardsMap();
        AtomicInteger invocations = new AtomicInteger();
        Consumer<AgentCard> validator = selected -> {
            if (invocations.incrementAndGet() == 1) {
                throw new IllegalStateException("retry validation");
            }
        };
        assertThrows(IllegalStateException.class, () -> AgentCardValidator.validateOnce(card, cache, validator));
        assertFalse(cache.containsKey(card));
        AgentCardValidator.validateOnce(card, cache, validator);
        AgentCardValidator.validateOnce(card, cache, validator);
        assertEquals(Boolean.TRUE, cache.get(card));
        assertEquals(2, invocations.get());
    }

    @Test
    void cachingValidatorsOwnIndependentCachesAndRetryFailures() {
        AgentCard card = createTestAgentCardBuilder().build();
        AtomicInteger firstCalls = new AtomicInteger();
        AtomicInteger secondCalls = new AtomicInteger();
        CachingAgentCardTransportValidator first = new CachingAgentCardTransportValidator(selected -> {
            if (firstCalls.incrementAndGet() == 1) {
                throw new IllegalStateException("retry");
            }
        });
        CachingAgentCardTransportValidator second = new CachingAgentCardTransportValidator(
                selected -> secondCalls.incrementAndGet());
        assertThrows(IllegalStateException.class, () -> first.validate(card));
        first.validate(card);
        first.validate(card);
        second.validate(card);
        second.validate(card);
        first.validate(createTestAgentCardBuilder().name("distinct").build());
        assertEquals(3, firstCalls.get());
        assertEquals(1, secondCalls.get());
    }

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
