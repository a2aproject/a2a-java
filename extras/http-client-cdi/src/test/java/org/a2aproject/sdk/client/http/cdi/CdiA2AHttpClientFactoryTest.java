package org.a2aproject.sdk.client.http.cdi;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.A2AHttpClientFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class CdiA2AHttpClientFactoryTest {

    private static void assertCdiProviderSkippedLogged(List<LogRecord> records, String context) {
        assertTrue(records.stream()
                .anyMatch(r -> r.getLevel() == Level.WARNING
                        && r.getMessage().equals("Provider cdi skipped")),
                "Expected WARNING 'Provider cdi skipped' " + context);
    }

    private static List<LogRecord> captureFactoryWarnings(Runnable action) {
        Logger logger = Logger.getLogger(A2AHttpClientFactory.class.getName());
        List<LogRecord> records = new ArrayList<>();
        Handler captureHandler = new Handler() {
            @Override public void publish(LogRecord r) { records.add(r); }
            @Override public void flush() {}
            @Override public void close() {}
        };
        Level saved = logger.getLevel();
        logger.setLevel(Level.WARNING);
        logger.addHandler(captureHandler);
        try {
            action.run();
        } finally {
            logger.removeHandler(captureHandler);
            logger.setLevel(saved);
        }
        return records;
    }

    @Nested
    class WithCdiContainer {

        private SeContainer container;

        @BeforeEach
        void startContainer() {
            container = SeContainerInitializer.newInstance()
                    .disableDiscovery()
                    .addBeanClasses(DummyA2AHttpClient.class)
                    .initialize();
        }

        @AfterEach
        void closeContainer() {
            container.close();
        }

        @Test
        public void testCreateUsesCdiBeanAtPriority200() {
            A2AHttpClient result = A2AHttpClientFactory.create();
            assertNotNull(result);
            assertInstanceOf(DummyA2AHttpClient.class, result,
                    "Factory should return CDI-provided client (priority 200) over JDK/Vert.x");
        }

        @Test
        public void testCreateDoesNotLogWarningWhenCdiSucceeds() {
            List<LogRecord> records = captureFactoryWarnings(A2AHttpClientFactory::create);
            assertFalse(records.stream().anyMatch(r -> r.getLevel() == Level.WARNING),
                    "No warning should be logged when CDI provider succeeds");
        }
    }

    @Nested
    class WithCdiContainerButNoBeanRegistered {

        private SeContainer container;

        @BeforeEach
        void startContainer() {
            container = SeContainerInitializer.newInstance()
                    .disableDiscovery()
                    .addBeanClasses(FillerBean.class)
                    .initialize();
        }

        @AfterEach
        void closeContainer() {
            container.close();
        }

        @Test
        public void testCreateFallsBackWhenNoBeanRegistered() {
            A2AHttpClient client = A2AHttpClientFactory.create();
            assertNotNull(client, "Factory should fall back when CDI has no A2AHttpClient bean");
            assertFalse(client instanceof DummyA2AHttpClient,
                    "Fallback client must not be the CDI bean");
        }

        @Test
        public void testCreateLogsWarningWhenNoBeanRegistered() {
            List<LogRecord> records = captureFactoryWarnings(A2AHttpClientFactory::create);
            assertCdiProviderSkippedLogged(records, "when CDI has no A2AHttpClient bean");
        }
    }

    @Nested
    class WithAmbiguousBeansInContainer {

        private SeContainer container;

        @BeforeEach
        void startContainer() {
            container = SeContainerInitializer.newInstance()
                    .disableDiscovery()
                    .addBeanClasses(DummyA2AHttpClient.class, AnotherDummyA2AHttpClient.class)
                    .initialize();
        }

        @AfterEach
        void closeContainer() {
            container.close();
        }

        @Test
        public void testCreateFallsBackWhenBeansAreAmbiguous() {
            A2AHttpClient client = A2AHttpClientFactory.create();
            assertNotNull(client, "Factory should fall back when CDI resolution is ambiguous");
            assertFalse(client instanceof DummyA2AHttpClient || client instanceof AnotherDummyA2AHttpClient,
                    "Fallback client must not be any of the ambiguous CDI beans");
        }

        @Test
        public void testCreateLogsWarningWhenBeansAreAmbiguous() {
            List<LogRecord> records = captureFactoryWarnings(A2AHttpClientFactory::create);
            assertCdiProviderSkippedLogged(records, "when CDI resolution is ambiguous");
        }
    }

    @Nested
    class WithoutCdiContainer {

        @Test
        public void testCreateFallsBackWhenNoCdiContainer() {
            A2AHttpClient client = A2AHttpClientFactory.create();
            assertNotNull(client, "Factory should fall back to next provider when CDI is unavailable");
        }

        @Test
        public void testCreateLogsWarningWhenNoCdiContainer() {
            List<LogRecord> records = captureFactoryWarnings(A2AHttpClientFactory::create);
            assertCdiProviderSkippedLogged(records, "when no CDI container is active");
        }
    }
}
