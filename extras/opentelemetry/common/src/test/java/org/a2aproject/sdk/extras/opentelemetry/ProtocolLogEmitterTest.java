package org.a2aproject.sdk.extras.opentelemetry;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.context.Context;
import java.util.List;
import org.a2aproject.sdk.spec.EventKind;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.TaskArtifactUpdateEvent;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;
import org.a2aproject.sdk.spec.TextPart;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;

class ProtocolLogEmitterTest {
    @Test
    void emitsStructuredMetadataWithoutPayload() {
        Logger logger = mock(Logger.class);
        LogRecordBuilder record = mock(LogRecordBuilder.class, Answers.RETURNS_SELF);
        when(logger.isEnabled(any(Severity.class), any(Context.class))).thenReturn(true);
        when(logger.logRecordBuilder()).thenReturn(record);

        new ProtocolLogEmitter(logger).emit("a2a.task.request", "SendMessage", "outbound", null);

        verify(record).setSeverity(Severity.DEBUG);
        verify(record).setEventName("a2a.task.request");
        verify(record).setAttribute("a2a.event.name", "a2a.task.request");
        verify(record).setAttribute("a2a.operation.name", "SendMessage");
        verify(record).setAttribute("a2a.log.direction", "outbound");
        verify(record).emit();
    }

    @Test
    void disabledLoggerDoesNotCreateRecord() {
        Logger logger = mock(Logger.class);
        new ProtocolLogEmitter(logger).emit("event", "operation", "outbound", null);
        verify(logger, never()).logRecordBuilder();
    }

    @Test
    void recordsMessageIdsWithoutEnablingPayloads() {
        Logger logger = mock(Logger.class);
        LogRecordBuilder record = mock(LogRecordBuilder.class, Answers.RETURNS_SELF);
        when(logger.isEnabled(any(Severity.class), any(Context.class))).thenReturn(true);
        when(logger.logRecordBuilder()).thenReturn(record);
        Message message =
                Message.builder()
                        .role(Message.Role.ROLE_USER)
                        .messageId("message-id")
                        .taskId("task-id")
                        .contextId("context-id")
                        .parts(List.of(new TextPart("private")))
                        .build();

        new ProtocolLogEmitter(logger)
                .emit("event", "operation", "outbound", message.kind(), message);

        verify(record).setAttribute("a2a.message.id", "message-id");
        verify(record).setAttribute("a2a.task.id", "task-id");
        verify(record).setAttribute("a2a.context.id", "context-id");
        ArgumentCaptor<String> bodies = ArgumentCaptor.forClass(String.class);
        verify(record, atLeastOnce()).setBody(bodies.capture());
        assertFalse(bodies.getAllValues().stream().anyMatch(body -> body.contains("private")));
    }

    @Test
    void loggerFailureDoesNotEscape() {
        Logger logger = mock(Logger.class);
        when(logger.isEnabled(any(Severity.class), any(Context.class)))
                .thenThrow(new IllegalStateException());
        assertDoesNotThrow(
                () -> new ProtocolLogEmitter(logger).emit("event", "operation", "outbound", null));
    }

    @Test
    void responseKindFailureDoesNotEscape() {
        Logger logger = mock(Logger.class);
        EventKind response = mock(EventKind.class);
        when(response.kind()).thenThrow(new IllegalStateException("invalid kind"));

        assertDoesNotThrow(() -> new ProtocolLogEmitter(logger).emitResponse(
                "event", "operation", "inbound", response));
    }

    @Test
    void sanitizerFailureOmitsPayload() {
        Logger logger = mock(Logger.class);
        LogRecordBuilder record = mock(LogRecordBuilder.class, Answers.RETURNS_SELF);
        when(logger.isEnabled(any(Severity.class), any(Context.class))).thenReturn(true);
        when(logger.logRecordBuilder()).thenReturn(record);
        ProtocolPayloadFormatter formatter =
                new ProtocolPayloadFormatter(
                        4096,
                        value -> {
                            throw new IllegalArgumentException();
                        });

        new ProtocolLogEmitter(logger, formatter)
                .emit("event", "operation", "outbound", null, "sensitive");

        verify(record).setAttribute("a2a.payload.omitted", true);
        ArgumentCaptor<String> bodies = ArgumentCaptor.forClass(String.class);
        verify(record, atLeastOnce()).setBody(bodies.capture());
        assertFalse(bodies.getAllValues().stream().anyMatch(body -> body.contains("sensitive")));
        verify(record).emit();
    }

    @Test
    void nullSanitizerResultOmitsPayload() {
        Logger logger = mock(Logger.class);
        LogRecordBuilder record = mock(LogRecordBuilder.class, Answers.RETURNS_SELF);
        when(logger.isEnabled(any(Severity.class), any(Context.class))).thenReturn(true);
        when(logger.logRecordBuilder()).thenReturn(record);
        ProtocolPayloadFormatter formatter = new ProtocolPayloadFormatter(4096, value -> null);

        new ProtocolLogEmitter(logger, formatter)
                .emit("event", "operation", "outbound", null, "sensitive");

        verify(record).setAttribute("a2a.payload.omitted", true);
        ArgumentCaptor<String> bodies = ArgumentCaptor.forClass(String.class);
        verify(record, atLeastOnce()).setBody(bodies.capture());
        assertFalse(bodies.getAllValues().stream().anyMatch(body -> body.contains("sensitive")));
    }

    @Test
    void emitsNotificationDeliveryAttributes() {
        Logger logger = mock(Logger.class);
        LogRecordBuilder record = mock(LogRecordBuilder.class, Answers.RETURNS_SELF);
        when(logger.isEnabled(any(Severity.class), any(Context.class))).thenReturn(true);
        when(logger.logRecordBuilder()).thenReturn(record);

        new ProtocolLogEmitter(logger).emitNotificationDelivery("task-id", "config-id", false);

        verify(record).setAttribute("a2a.event.kind", "failure");
        verify(record).setAttribute("a2a.task.id", "task-id");
        verify(record).setAttribute("a2a.notification.id", "config-id");
    }

    @Test
    void emitsAllowedAuthorizationDecision() {
        Logger logger = mock(Logger.class);
        LogRecordBuilder record = mock(LogRecordBuilder.class, Answers.RETURNS_SELF);
        when(logger.isEnabled(any(Severity.class), any(Context.class))).thenReturn(true);
        when(logger.logRecordBuilder()).thenReturn(record);

        new ProtocolLogEmitter(logger).emitAuthorization("GET_TASK", "task-id", null);

        verify(record).setEventName("a2a.authorization");
        verify(record).setAttribute("a2a.operation.name", "GET_TASK");
        verify(record).setAttribute("a2a.log.direction", "inbound");
        verify(record).setAttribute("a2a.event.kind", "allow");
        verify(record).setAttribute("a2a.task.id", "task-id");
        verify(record, never()).setAttribute(eq("error.type"), anyString());
    }

    @Test
    void emitsDeniedAuthorizationDecisionWithErrorType() {
        Logger logger = mock(Logger.class);
        LogRecordBuilder record = mock(LogRecordBuilder.class, Answers.RETURNS_SELF);
        when(logger.isEnabled(any(Severity.class), any(Context.class))).thenReturn(true);
        when(logger.logRecordBuilder()).thenReturn(record);

        new ProtocolLogEmitter(logger)
                .emitAuthorization("CANCEL_TASK", "task-id", "TaskNotFoundError");

        verify(record).setAttribute("a2a.event.kind", "deny");
        verify(record).setAttribute("error.type", "TaskNotFoundError");
        verify(record).emit();
    }

    @Test
    void omitsTaskIdForOperationsWithoutATask() {
        Logger logger = mock(Logger.class);
        LogRecordBuilder record = mock(LogRecordBuilder.class, Answers.RETURNS_SELF);
        when(logger.isEnabled(any(Severity.class), any(Context.class))).thenReturn(true);
        when(logger.logRecordBuilder()).thenReturn(record);

        new ProtocolLogEmitter(logger).emitAuthorization("LIST_TASKS", null, null);

        verify(record).setAttribute("a2a.event.kind", "allow");
        verify(record, never()).setAttribute(eq("a2a.task.id"), anyString());
    }

    @Test
    void mapsStreamingEventNames() {
        assertEquals(
                "a2a.task.state_change",
                ProtocolLogEmitter.streamingEventName(mock(TaskStatusUpdateEvent.class)));
        assertEquals(
                "a2a.task.artifact",
                ProtocolLogEmitter.streamingEventName(mock(TaskArtifactUpdateEvent.class)));
    }
}
