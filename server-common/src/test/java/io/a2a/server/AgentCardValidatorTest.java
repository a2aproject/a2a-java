package io.a2a.server;

import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.TransportProtocol;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class AgentCardValidatorTest {

    private AgentCard.Builder createTestAgentCardBuilder() {
        return new AgentCard.Builder()
                .name("Test Agent")
                .description("Test Description")
                .supportedInterfaces(Collections.singletonList(
                        new AgentInterface(TransportProtocol.JSONRPC.asString(), "http://localhost:9999")))
                .version("1.0.0")
                .capabilities(new AgentCapabilities.Builder().build())
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
