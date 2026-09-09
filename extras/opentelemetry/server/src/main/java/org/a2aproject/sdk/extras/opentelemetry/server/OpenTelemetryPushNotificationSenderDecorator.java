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
 * as span attributes.
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
    public void sendNotification(StreamingEventKind event, @Nullable Task taskSnapshot) {
        var spanBuilder = tracer.spanBuilder(SEND_PUSH_NOTIFICATION)
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute(GENAI_OPERATION_NAME, SEND_PUSH_NOTIFICATION)
                .setAttribute(PUSH_NOTIFICATION_EVENT_KIND, event.getClass().getSimpleName());

        String taskId = extractTaskId(event, taskSnapshot);
        if (taskId != null) {
            spanBuilder.setAttribute(GENAI_TASK_ID, taskId);
        }
        String contextId = extractContextId(event, taskSnapshot);
        if (contextId != null) {
            spanBuilder.setAttribute(GENAI_CONTEXT_ID, contextId);
        }
        if (extractResponse()) {
            spanBuilder.setAttribute(GENAI_RESPONSE, event.toString());
        }

        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            delegate.sendNotification(event, taskSnapshot);
            span.setStatus(StatusCode.OK);
        } catch (Exception ex) {
            span.setAttribute(ERROR_TYPE, ex.getMessage() != null ? ex.getMessage() : ex.getClass().getName());
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
        }
    }

    @Nullable
    private static String extractTaskId(StreamingEventKind event, @Nullable Task taskSnapshot) {
        if (taskSnapshot != null) {
            return taskSnapshot.id();
        }
        if (event instanceof Task task) {
            return task.id();
        }
        if (event instanceof TaskStatusUpdateEvent statusUpdate) {
            return statusUpdate.taskId();
        }
        if (event instanceof TaskArtifactUpdateEvent artifactUpdate) {
            return artifactUpdate.taskId();
        }
        return null;
    }

    @Nullable
    private static String extractContextId(StreamingEventKind event, @Nullable Task taskSnapshot) {
        if (taskSnapshot != null) {
            return taskSnapshot.contextId();
        }
        if (event instanceof Task task) {
            return task.contextId();
        }
        if (event instanceof TaskStatusUpdateEvent statusUpdate) {
            return statusUpdate.contextId();
        }
        if (event instanceof TaskArtifactUpdateEvent artifactUpdate) {
            return artifactUpdate.contextId();
        }
        if (event instanceof Message message) {
            return message.contextId();
        }
        return null;
    }

    private boolean extractResponse() {
        return Boolean.getBoolean(EXTRACT_RESPONSE_SYS_PROPERTY);
    }
}
