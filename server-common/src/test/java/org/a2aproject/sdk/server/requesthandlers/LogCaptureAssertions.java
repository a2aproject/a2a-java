package org.a2aproject.sdk.server.requesthandlers;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;

public class LogCaptureAssertions {

    public static class TestLogHandler extends java.util.logging.Handler {
        private final List<String> logMessages = new ArrayList<>();

        @Override
        public void publish(java.util.logging.LogRecord record) {
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

    public static void assertSensitiveDataNotLogged(
            java.util.logging.Logger logger, Runnable operation, String... sensitiveValues) {
        TestLogHandler testLogHandler = new TestLogHandler();
        logger.addHandler(testLogHandler);
        try {
            operation.run();
            List<String> logMessages = testLogHandler.getLogMessages();
            for (String sensitive : sensitiveValues) {
                Assertions.assertTrue(logMessages.stream().noneMatch(msg -> msg.contains(sensitive)),
                        "Log should not contain sensitive value: " + sensitive);
            }
            Assertions.assertTrue(
                    logMessages.stream().anyMatch(msg -> msg.contains("Error parsing JSON request body (length=")),
                    "Log should contain body length message");
        } finally {
            logger.removeHandler(testLogHandler);
        }
    }
}
