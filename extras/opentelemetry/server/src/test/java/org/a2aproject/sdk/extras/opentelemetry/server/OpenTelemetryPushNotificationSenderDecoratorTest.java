package org.a2aproject.sdk.extras.opentelemetry.server;

import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.ERROR_TYPE;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.EXTRACT_RESPONSE_SYS_PROPERTY;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_CONTEXT_ID;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_OPERATION_NAME;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_RESPONSE;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_TASK_ID;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.PUSH_NOTIFICATION_EVENT_KIND;
import static org.a2aproject.sdk.extras.opentelemetry.server.OpenTelemetryPushNotificationSenderDecorator.SEND_PUSH_NOTIFICATION;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.util.Collections;
import java.util.List;
import org.a2aproject.sdk.server.tasks.PushNotificationSender;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskArtifactUpdateEvent;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpenTelemetryPushNotificationSenderDecoratorTest {

    @Mock
    private PushNotificationSender delegate;

    @Mock
    private Tracer tracer;

    @Mock
    private SpanBuilder spanBuilder;

    @Mock
    private Span span;

    @Mock
    private Scope scope;

    private OpenTelemetryPushNotificationSenderDecorator decorator;

    @BeforeEach
    void setUp() {
        when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.setSpanKind(any(SpanKind.class))).thenReturn(spanBuilder);
        when(spanBuilder.setAttribute(anyString(), anyString())).thenReturn(spanBuilder);
        when(spanBuilder.startSpan()).thenReturn(span);
        when(span.makeCurrent()).thenReturn(scope);

        decorator = new TestablePushNotificationSenderDecorator(delegate, tracer);
    }

    @Test
    void sendNotification_createsSpanWithTaskIdFromSnapshot() {
        when(delegate.sendNotification(any(), any())).thenReturn(true);
        Task taskSnapshot = Task.builder()
                .id("task-1")
                .contextId("ctx-1")
                .status(new TaskStatus(TaskState.TASK_STATE_WORKING))
                .history(Collections.emptyList())
                .artifacts(Collections.emptyList())
                .build();
        TaskStatusUpdateEvent event = TaskStatusUpdateEvent.builder()
                .taskId("task-1")
                .contextId("ctx-1")
                .status(new TaskStatus(TaskState.TASK_STATE_WORKING))
                .build();

        decorator.sendNotification(event, taskSnapshot);

        verify(tracer).spanBuilder(SEND_PUSH_NOTIFICATION);
        verify(spanBuilder).setSpanKind(SpanKind.CLIENT);
        verify(spanBuilder).setAttribute(GENAI_OPERATION_NAME, SEND_PUSH_NOTIFICATION);
        verify(spanBuilder).setAttribute(GENAI_TASK_ID, "task-1");
        verify(spanBuilder).setAttribute(GENAI_CONTEXT_ID, "ctx-1");
        verify(span).setStatus(StatusCode.OK);
        verify(span).end();
        verify(delegate).sendNotification(event, taskSnapshot);
    }

    @Test
    void sendNotification_createsSpanWithTaskIdFromEvent() {
        when(delegate.sendNotification(any(), any())).thenReturn(true);
        org.a2aproject.sdk.spec.Artifact artifact = org.a2aproject.sdk.spec.Artifact.builder()
                .artifactId("artifact-id")
                .parts(List.of(new org.a2aproject.sdk.spec.TextPart("result")))
                .build();
        TaskArtifactUpdateEvent event = new TaskArtifactUpdateEvent(
                "task-event-1", artifact, "ctx-event-1", null, null, null);

        decorator.sendNotification(event, null);

        verify(spanBuilder).setAttribute(GENAI_TASK_ID, "task-event-1");
        verify(spanBuilder).setAttribute(GENAI_CONTEXT_ID, "ctx-event-1");
        verify(spanBuilder, never()).setAttribute(eq(GENAI_TASK_ID), eq((String) null));
        verify(span).setStatus(StatusCode.OK);
        verify(span).end();
    }

    @Test
    void sendNotification_setsErrorStatusOnDeliveryFailure() {
        TaskStatusUpdateEvent event = TaskStatusUpdateEvent.builder()
                .taskId("task-fail")
                .contextId("ctx-fail")
                .status(new TaskStatus(TaskState.TASK_STATE_WORKING))
                .build();
        when(delegate.sendNotification(any(), any())).thenReturn(false);

        boolean result = decorator.sendNotification(event, null);

        assertFalse(result);
        verify(span).setAttribute(ERROR_TYPE, "delivery_failed");
        verify(span).setStatus(StatusCode.ERROR, "One or more push notification deliveries failed");
        verify(span).end();
    }

    @Test
    void sendNotification_setsErrorStatusOnException() {
        TaskStatusUpdateEvent event = TaskStatusUpdateEvent.builder()
                .taskId("task-err")
                .contextId("ctx-err")
                .status(new TaskStatus(TaskState.TASK_STATE_FAILED))
                .build();
        RuntimeException ex = new RuntimeException("webhook unreachable");
        doThrow(ex).when(delegate).sendNotification(any(), any());

        assertThrows(RuntimeException.class, () -> decorator.sendNotification(event, null));

        verify(span).setStatus(StatusCode.ERROR, "webhook unreachable");
        verify(span).setAttribute(ERROR_TYPE, "webhook unreachable");
        verify(span).end();
    }

    @Test
    void sendNotification_setsEventKindAttribute() {
        when(delegate.sendNotification(any(), any())).thenReturn(true);
        TaskStatusUpdateEvent event = TaskStatusUpdateEvent.builder()
                .taskId("task-kind")
                .contextId("ctx-kind")
                .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED))
                .build();

        decorator.sendNotification(event, null);

        verify(spanBuilder).setAttribute(PUSH_NOTIFICATION_EVENT_KIND, "TaskStatusUpdateEvent");
    }

    @Test
    void sendNotification_extractsResponseWhenEnabled() {
        when(delegate.sendNotification(any(), any())).thenReturn(true);
        System.setProperty(EXTRACT_RESPONSE_SYS_PROPERTY, "true");
        try {
            TaskStatusUpdateEvent event = TaskStatusUpdateEvent.builder()
                    .taskId("task-resp")
                    .contextId("ctx-resp")
                    .status(new TaskStatus(TaskState.TASK_STATE_WORKING))
                    .build();

            decorator.sendNotification(event, null);

            verify(spanBuilder).setAttribute(eq(GENAI_RESPONSE), anyString());
        } finally {
            System.clearProperty(EXTRACT_RESPONSE_SYS_PROPERTY);
        }
    }

    @Test
    void sendNotification_noResponseAttributeWhenDisabled() {
        when(delegate.sendNotification(any(), any())).thenReturn(true);
        System.clearProperty(EXTRACT_RESPONSE_SYS_PROPERTY);
        TaskStatusUpdateEvent event = TaskStatusUpdateEvent.builder()
                .taskId("task-noresp")
                .contextId("ctx-noresp")
                .status(new TaskStatus(TaskState.TASK_STATE_WORKING))
                .build();

        decorator.sendNotification(event, null);

        verify(spanBuilder, never()).setAttribute(eq(GENAI_RESPONSE), anyString());
    }

    static class TestablePushNotificationSenderDecorator extends OpenTelemetryPushNotificationSenderDecorator {
        TestablePushNotificationSenderDecorator(PushNotificationSender delegate, Tracer tracer) {
            super(delegate, tracer);
        }
    }
}
