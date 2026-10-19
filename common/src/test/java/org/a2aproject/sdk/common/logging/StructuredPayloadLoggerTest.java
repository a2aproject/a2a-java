package org.a2aproject.sdk.common.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

public class StructuredPayloadLoggerTest {

    private ListAppender<ILoggingEvent> appender;
    private Logger payloadLogger;
    private A2APayloadLoggingConfig originalConfig;

    @BeforeEach
    void setUp() {
        originalConfig = StructuredPayloadLogger.getConfig();
        payloadLogger = (Logger) LoggerFactory.getLogger("org.a2aproject.sdk.payload");
        payloadLogger.setLevel(Level.DEBUG);
        appender = new ListAppender<>();
        appender.start();
        payloadLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        payloadLogger.detachAppender(appender);
        StructuredPayloadLogger.setConfig(originalConfig);
        MDC.clear();
        TraceCorrelation.clearMdc();
        System.clearProperty(A2APayloadLoggingConfig.ENABLED_PROPERTY);
        System.clearProperty(A2APayloadLoggingConfig.MAX_SIZE_PROPERTY);
        System.clearProperty(A2APayloadLoggingConfig.MASKING_ENABLED_PROPERTY);
    }

    @Test
    void testLogWithTruncation() {
        A2APayloadLoggingConfig config = A2APayloadLoggingConfig.builder()
                .enabled(true)
                .maxSize(10)
                .maskingEnabled(false)
                .build();
        StructuredPayloadLogger.setConfig(config);

        String largePayload = "a".repeat(20);
        StructuredPayloadLogger.log(A2AEventType.TASK_REQUEST, largePayload);

        assertEquals(1, appender.list.size());
        ILoggingEvent event = appender.list.get(0);
        // Check message contains truncated marker
        assertTrue(event.getFormattedMessage().contains(PayloadTruncator.TRUNCATED_MARKER));
        // Check structured keyValues
        boolean hasTruncatedFlag = event.getKeyValuePairs().stream()
                .anyMatch(kvp -> "a2a.payload.truncated".equals(kvp.key) && "true".equals(kvp.value));
        assertTrue(hasTruncatedFlag);
    }

    @Test
    void testLogWithMasking() {
        A2APayloadLoggingConfig config = A2APayloadLoggingConfig.builder()
                .enabled(true)
                .maxSize(4096)
                .maskingEnabled(true)
                .build();
        StructuredPayloadLogger.setConfig(config);

        String payload = "{\"token\":\"secret\",\"data\":\"value\"}";
        StructuredPayloadLogger.log(A2AEventType.TASK_REQUEST, payload);

        assertEquals(1, appender.list.size());
        ILoggingEvent event = appender.list.get(0);
        // Masked payload should not contain secret
        assertFalse(event.getFormattedMessage().contains("secret"));
        assertTrue(event.getFormattedMessage().contains("\"token\":\"***\""));
    }

    @Test
    void testLogWithoutMasking() {
        A2APayloadLoggingConfig config = A2APayloadLoggingConfig.builder()
                .enabled(true)
                .maxSize(4096)
                .maskingEnabled(false)
                .build();
        StructuredPayloadLogger.setConfig(config);

        String payload = "{\"token\":\"secret\",\"data\":\"value\"}";
        StructuredPayloadLogger.log(A2AEventType.TASK_REQUEST, payload);

        assertEquals(1, appender.list.size());
        ILoggingEvent event = appender.list.get(0);
        assertTrue(event.getFormattedMessage().contains("secret"));
    }

    @Test
    void testLogDisabledWhenConfigDisabled() {
        A2APayloadLoggingConfig config = A2APayloadLoggingConfig.builder()
                .enabled(false)
                .build();
        StructuredPayloadLogger.setConfig(config);
        StructuredPayloadLogger.log(A2AEventType.TASK_REQUEST, "{\"data\":\"value\"}");
        assertEquals(0, appender.list.size());
    }

    @Test
    void testLogWithTraceCorrelationViaMDC() {
        A2APayloadLoggingConfig config = A2APayloadLoggingConfig.builder()
                .enabled(true)
                .build();
        StructuredPayloadLogger.setConfig(config);
        MDC.put("trace_id", "trace-123");
        MDC.put("span_id", "span-456");

        StructuredPayloadLogger.log(A2AEventType.TASK_REQUEST, "{\"data\":\"value\"}");

        assertEquals(1, appender.list.size());
        ILoggingEvent event = appender.list.get(0);
        String msg = event.getFormattedMessage();
        assertTrue(msg.contains("trace_id=trace-123"));
        assertTrue(msg.contains("span_id=span-456"));
        // Check keyValues
        boolean hasTrace = event.getKeyValuePairs().stream().anyMatch(kvp -> "trace_id".equals(kvp.key) && "trace-123".equals(kvp.value));
        assertTrue(hasTrace);
    }

    @Test
    void testCustomMaskingStrategy() {
        PayloadMaskingStrategy custom = payload -> payload.replace("secret", "***CUSTOM***");
        A2APayloadLoggingConfig config = A2APayloadLoggingConfig.builder()
                .enabled(true)
                .maskingEnabled(true)
                .maskingStrategy(custom)
                .build();
        StructuredPayloadLogger.setConfig(config);
        // Also set via setter to override default creation
        StructuredPayloadLogger.setMaskingStrategy(custom);

        StructuredPayloadLogger.log(A2AEventType.TASK_REQUEST, "{\"data\":\"secret\"}");

        assertEquals(1, appender.list.size());
        assertTrue(appender.list.get(0).getFormattedMessage().contains("***CUSTOM***"));
        assertFalse(appender.list.get(0).getFormattedMessage().contains("secret"));
    }

    @Test
    void testConfigFromSystemProperties() {
        System.setProperty(A2APayloadLoggingConfig.ENABLED_PROPERTY, "true");
        System.setProperty(A2APayloadLoggingConfig.MAX_SIZE_PROPERTY, "100");
        A2APayloadLoggingConfig cfg = A2APayloadLoggingConfig.fromSystemProperties();
        assertTrue(cfg.isEnabled());
        assertEquals(100, cfg.getMaxSize());
    }

    @Test
    void testDefaultMaskingKeysCoverPushNotification() {
        DefaultPayloadMaskingStrategy strategy = new DefaultPayloadMaskingStrategy();
        String payload = "{\"url\":\"https://hook.example.com\",\"token\":\"mytoken\",\"taskId\":\"123\"}";
        String masked = strategy.mask(payload);
        assertTrue(masked.contains("\"url\":\"***\""));
        assertTrue(masked.contains("\"token\":\"***\""));
        assertFalse(masked.contains("mytoken"));
        assertFalse(masked.contains("https://hook.example.com"));
        // taskId should not be masked
        assertTrue(masked.contains("123"));
    }

    @Test
    void testLogTaskRequestConvenience() {
        A2APayloadLoggingConfig config = A2APayloadLoggingConfig.builder().enabled(true).build();
        StructuredPayloadLogger.setConfig(config);

        StructuredPayloadLogger.logTaskRequest("message/send", "{\"msg\":\"hello\"}", "task-123", "ctx-456");
        assertEquals(1, appender.list.size());
        String msg = appender.list.get(0).getFormattedMessage();
        assertTrue(msg.contains("a2a.task_id=task-123"));
        assertTrue(msg.contains("a2a.context_id=ctx-456"));
        assertTrue(msg.contains("a2a.operation=message/send"));
    }

    @Test
    void testLogAuthorization() {
        A2APayloadLoggingConfig config = A2APayloadLoggingConfig.builder().enabled(true).build();
        StructuredPayloadLogger.setConfig(config);

        StructuredPayloadLogger.logAuthorization("GET_TASK", "task-123", true, "allowed by owner");
        assertEquals(1, appender.list.size());
        String msg = appender.list.get(0).getFormattedMessage();
        assertTrue(msg.contains("a2a.auth.allowed=true"));
        assertTrue(msg.contains("a2a.task_id=task-123"));
    }

    @Test
    void testLogPushDelivery() {
        A2APayloadLoggingConfig config = A2APayloadLoggingConfig.builder().enabled(true).build();
        StructuredPayloadLogger.setConfig(config);

        StructuredPayloadLogger.logPushDelivery("task-1", "https://example.com/hook", "{\"event\":\"data\"}", true);
        assertEquals(1, appender.list.size());
        String msg = appender.list.get(0).getFormattedMessage();
        assertTrue(msg.contains("a2a.task_id=task-1"));
        assertTrue(msg.contains("a2a.push.success=true"));
    }
}
