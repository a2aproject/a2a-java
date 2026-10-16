package org.a2aproject.sdk.extras.opentelemetry.server;

import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.ERROR_TYPE;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.EXTRACT_RESPONSE_SYS_PROPERTY;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_CONTEXT_ID;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_OPERATION_NAME;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_RESPONSE;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_TASK_ID;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.PUSH_NOTIFICATION_EVENT_KIND;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.a2aproject.sdk.server.tasks.PushNotificationSender;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.StreamingEventKind;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskArtifactUpdateEvent;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;
import org.jspecify.annotations.Nullable;

/**
 * OpenTelemetry CDI Decorator for {@link PushNotificationSender}.
 * <p>
 * Creates a {@code CLIENT} span named {@code SendPushNotification} for every push
 * notification delivery attempt, capturing the task ID, context ID, and event kind
 * as span attributes. The span status reflects the actual delivery outcome:
 * {@code OK} when all notifications succeed, {@code ERROR} when one or more
 * deliveries fail or an exception is thrown.
 * <p>
 * To enable this decorator, add it to your beans.xml:
 * <pre>{@code
 * <decorators>
 *     <class>org.a2aproject.sdk.extras.opentelemetry.server.OpenTelemetryPushNotificationSenderDecorator</class>
 * </decorators>
 * }</pre>
 */
@Decorator
@Priority(100)
public abstract class OpenTelemetryPushNotificationSenderDecorator implements PushNotificationSender {

    static final String SEND_PUSH_NOTIFICATION = "SendPushNotification";

    @Inject
    @Delegate
    @Any
    private PushNotificationSender delegate;

    @Inject
    private Tracer tracer;

    /**
     * Default constructor for CDI.
     */
    public OpenTelemetryPushNotificationSenderDecorator() {
    }

    /**
     * Constructor for unit testing.
     *
     * @param delegate the delegate push notification sender
     * @param tracer the tracer to use
     */
    OpenTelemetryPushNotificationSenderDecorator(PushNotificationSender delegate, Tracer tracer) {
        this.delegate = delegate;
        this.tracer = tracer;
    }

    @Override
    public boolean sendNotification(StreamingEventKind event, @Nullable Task taskSnapshot) {
        var spanBuilder = tracer.spanBuilder(SEND_PUSH_NOTIFICATION)
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute(GENAI_OPERATION_NAME, SEND_PUSH_NOTIFICATION)
                .setAttribute(PUSH_NOTIFICATION_EVENT_KIND, event.getClass().getSimpleName());

        EventIds ids = extractIds(event, taskSnapshot);
        if (ids.taskId != null) {
            spanBuilder.setAttribute(GENAI_TASK_ID, ids.taskId);
        }
        if (ids.contextId != null) {
            spanBuilder.setAttribute(GENAI_CONTEXT_ID, ids.contextId);
        }
        if (extractResponse()) {
            spanBuilder.setAttribute(GENAI_RESPONSE, event.toString());
        }

        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            boolean result = delegate.sendNotification(event, taskSnapshot);
            if (result) {
                span.setStatus(StatusCode.OK);
            } else {
                span.setAttribute(ERROR_TYPE, "delivery_failed");
                span.setStatus(StatusCode.ERROR, "One or more push notification deliveries failed");
            }
            return result;
        } catch (Exception ex) {
            String errorDescription = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getName();
            span.setAttribute(ERROR_TYPE, errorDescription);
            span.setStatus(StatusCode.ERROR, errorDescription);
            throw ex;
        } finally {
            span.end();
        }
    }

    private record EventIds(@Nullable String taskId, @Nullable String contextId) {
    }

    private static EventIds extractIds(StreamingEventKind event, @Nullable Task taskSnapshot) {
        if (taskSnapshot != null) {
            return new EventIds(taskSnapshot.id(), taskSnapshot.contextId());
        }
        if (event instanceof Task task) {
            return new EventIds(task.id(), task.contextId());
        }
        if (event instanceof Message message) {
            return new EventIds(message.taskId(), message.contextId());
        }
        if (event instanceof TaskStatusUpdateEvent statusUpdate) {
            return new EventIds(statusUpdate.taskId(), statusUpdate.contextId());
        }
        if (event instanceof TaskArtifactUpdateEvent artifactUpdate) {
            return new EventIds(artifactUpdate.taskId(), artifactUpdate.contextId());
        }
        return new EventIds(null, null);
    }

    private boolean extractResponse() {
        return Boolean.getBoolean(EXTRACT_RESPONSE_SYS_PROPERTY);
    }
}
