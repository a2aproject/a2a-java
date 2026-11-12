package org.a2aproject.sdk.extras.opentelemetry.server;

import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.A2A_EVENT_NOTIFICATION_DELIVERY;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.A2A_KIND_ERROR;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.A2A_KIND_FAILURE;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.A2A_KIND_SUCCESS;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.A2A_LOG_DIRECTION_OUTBOUND;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.ERROR_TYPE;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.EXTRACT_RESPONSE_SYS_PROPERTY;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_CONTEXT_ID;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_OPERATION_NAME;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_RESPONSE;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_TASK_ID;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.PUSH_NOTIFICATION_DELIVERY_FAILED;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.PUSH_NOTIFICATION_EVENT_KIND;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.function.Supplier;
import org.a2aproject.sdk.extras.opentelemetry.ProtocolLogEmitter;
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
 * This decorator is registered in the module's {@code META-INF/beans.xml} and is
 * therefore active automatically when this module is on the classpath — no additional
 * configuration is needed.
 */
@Decorator
@Priority(100)
public abstract class OpenTelemetryPushNotificationSenderDecorator
        implements PushNotificationSender {

    static final String SEND_PUSH_NOTIFICATION = "SendPushNotification";

    @Inject @Delegate @Any private PushNotificationSender delegate;

    @Inject private Tracer tracer;

    @Inject private Instance<ProtocolLogEmitter> emitterInstance;

    /**
     * Memoized supplier for the optional protocol log emitter.
     * {@code null} until first call to {@link #logDelivery} (CDI path); set directly by test constructors.
     */
    @Nullable
    private volatile Supplier<@Nullable ProtocolLogEmitter> emitterSupplier;

    private final boolean extractResponse;

    /**
     * Default constructor for CDI. Reads {@code EXTRACT_RESPONSE_SYS_PROPERTY} once
     * at bean initialisation so the value is not re-evaluated on every notification.
     */
    public OpenTelemetryPushNotificationSenderDecorator() {
        this.extractResponse = Boolean.getBoolean(EXTRACT_RESPONSE_SYS_PROPERTY);
    }

    /**
     * Constructor for unit testing.
     *
     * @param delegate the delegate push notification sender
     * @param tracer the tracer to use
     * @param extractResponse whether to capture the full event payload as a span attribute
     */
    OpenTelemetryPushNotificationSenderDecorator(
            PushNotificationSender delegate, Tracer tracer, boolean extractResponse) {
        this(delegate, tracer, extractResponse, null);
    }

    OpenTelemetryPushNotificationSenderDecorator(
            PushNotificationSender delegate,
            Tracer tracer,
            boolean extractResponse,
            @Nullable ProtocolLogEmitter emitter) {
        this.delegate = delegate;
        this.tracer = tracer;
        this.extractResponse = extractResponse;
        this.emitterSupplier = () -> emitter;
    }

    @Override
    public boolean sendNotification(StreamingEventKind event, @Nullable Task taskSnapshot) {
        SpanBuilder spanBuilder =
                tracer.spanBuilder(SEND_PUSH_NOTIFICATION)
                        .setSpanKind(SpanKind.CLIENT)
                        .setAttribute(GENAI_OPERATION_NAME, SEND_PUSH_NOTIFICATION)
                        .setAttribute(
                                PUSH_NOTIFICATION_EVENT_KIND, event.getClass().getSimpleName());

        EventIds ids = extractIds(event, taskSnapshot);
        if (ids.taskId != null) {
            spanBuilder.setAttribute(GENAI_TASK_ID, ids.taskId);
        }
        if (ids.contextId != null) {
            spanBuilder.setAttribute(GENAI_CONTEXT_ID, ids.contextId);
        }
        if (extractResponse) {
            spanBuilder.setAttribute(GENAI_RESPONSE, event.toString());
        }

        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            boolean result = delegate.sendNotification(event, taskSnapshot);
            logDelivery(result ? A2A_KIND_SUCCESS : A2A_KIND_FAILURE, event);
            if (result) {
                span.setStatus(StatusCode.OK);
            } else {
                span.setAttribute(ERROR_TYPE, PUSH_NOTIFICATION_DELIVERY_FAILED);
                span.setStatus(StatusCode.ERROR, "One or more push notification deliveries failed");
            }
            return result;
        } catch (Error err) {
            recordFailure(span, err, event);
            throw err;
        } catch (Exception ex) {
            recordFailure(span, ex, event);
            throw ex;
        } finally {
            span.end();
        }
    }

    private void logDelivery(String outcome, StreamingEventKind event) {
        Supplier<@Nullable ProtocolLogEmitter> s = emitterSupplier;
        if (s == null) {
            synchronized (this) {
                if ((s = emitterSupplier) == null) {
                    emitterSupplier = s = ProtocolLogEmitters.lazy(emitterInstance);
                }
            }
        }
        ProtocolLogEmitter emitter = s.get();
        if (emitter != null) {
            emitter.emitMetadata(
                    A2A_EVENT_NOTIFICATION_DELIVERY,
                    SEND_PUSH_NOTIFICATION,
                    A2A_LOG_DIRECTION_OUTBOUND,
                    outcome,
                    event);
        }
    }

    /**
     * Records a delivery failure on the span and returns a description suitable for {@code error.type}.
     * The description is the exception message when present, otherwise the class name.
     */
    private void recordFailure(Span span, Throwable t, StreamingEventKind event) {
        try (Scope ignored = span.makeCurrent()) {
            logDelivery(A2A_KIND_ERROR, event);
        }
        String desc = t.getMessage() != null ? t.getMessage() : t.getClass().getName();
        span.setAttribute(ERROR_TYPE, desc);
        span.setStatus(StatusCode.ERROR, desc);
    }

    private record EventIds(@Nullable String taskId, @Nullable String contextId) {}

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
}
