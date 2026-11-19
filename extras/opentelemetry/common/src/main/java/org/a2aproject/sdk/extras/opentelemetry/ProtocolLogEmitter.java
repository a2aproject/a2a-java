package org.a2aproject.sdk.extras.opentelemetry;

import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.A2A_EVENT_AUTHORIZATION;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.A2A_EVENT_NOTIFICATION_DELIVERY_ATTEMPT;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.A2A_EVENT_TASK_ARTIFACT;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.A2A_EVENT_TASK_EVENT;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.A2A_EVENT_TASK_STATE_CHANGE;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.A2A_KIND_ALLOW;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.A2A_KIND_DENY;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.A2A_KIND_FAILURE;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.A2A_KIND_SUCCESS;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.A2A_LOG_DIRECTION_INBOUND;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.A2A_LOG_DIRECTION_OUTBOUND;

import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.context.Context;
import org.a2aproject.sdk.spec.DeleteTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.EventKind;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.StreamingEventKind;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskArtifactUpdateEvent;
import org.a2aproject.sdk.spec.TaskIdParams;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;
import org.a2aproject.sdk.util.Assert;
import org.jspecify.annotations.Nullable;

/** Emits protocol events with optional, sanitized payloads. */
public final class ProtocolLogEmitter {
    private final Logger logger;
    private final @Nullable ProtocolPayloadFormatter payloadFormatter;
    private final Severity severity;

    /** Creates a metadata-only emitter at DEBUG severity. */
    public ProtocolLogEmitter(Logger logger) {
        this(logger, null);
    }

    /** Pass a formatter to opt in to payload capture. */
    public ProtocolLogEmitter(Logger logger, @Nullable ProtocolPayloadFormatter payloadFormatter) {
        this(logger, payloadFormatter, Severity.DEBUG);
    }

    /**
     * Creates an emitter at the requested severity.
     *
     * @param logger OpenTelemetry logger used to emit records
     * @param payloadFormatter optional formatter that enables sanitized payload capture
     * @param severity emitted record severity
     */
    public ProtocolLogEmitter(
            Logger logger, @Nullable ProtocolPayloadFormatter payloadFormatter, Severity severity) {
        this.logger = Assert.checkNotNullParam("logger", logger);
        this.payloadFormatter = payloadFormatter;
        this.severity = Assert.checkNotNullParam("severity", severity);
    }

    /** Returns the protocol log event name for a streaming event. */
    public static String streamingEventName(StreamingEventKind event) {
        if (event instanceof TaskStatusUpdateEvent) {
            return A2A_EVENT_TASK_STATE_CHANGE;
        }
        if (event instanceof TaskArtifactUpdateEvent) {
            return A2A_EVENT_TASK_ARTIFACT;
        }
        return A2A_EVENT_TASK_EVENT;
    }

    /** Emits a metadata-only protocol event. Observability never affects the protocol operation. */
    public void emit(String event, String operation, String direction, @Nullable String kind) {
        emit(event, operation, direction, kind, null);
    }

    /** Emits a protocol event and captures its payload when a formatter is configured. */
    public void emit(
            String event,
            String operation,
            String direction,
            @Nullable String kind,
            @Nullable Object payload) {
        emit(event, operation, direction, kind, payload, payload, ExtraAttributes.NONE);
    }

    /** Emits metadata derived from {@code attributesSource} without capturing it as the log body. */
    public void emitMetadata(
            String event,
            String operation,
            String direction,
            @Nullable String kind,
            @Nullable Object attributesSource) {
        emit(event, operation, direction, kind, attributesSource, null, ExtraAttributes.NONE);
    }

    /** Emits a response while containing failures from custom {@link EventKind#kind()} implementations. */
    public void emitResponse(String event, String operation, String direction, EventKind response) {
        try {
            emit(event, operation, direction, response.kind(), response);
        } catch (RuntimeException ignored) {
            // Logging is best effort and must not interfere with protocol execution.
        }
    }

    /** Emits an individual notification outcome without exposing its destination or credentials. */
    public void emitNotificationDelivery(
            @Nullable String taskId, @Nullable String configId, boolean success) {
        emit(
                A2A_EVENT_NOTIFICATION_DELIVERY_ATTEMPT,
                "SendPushNotification",
                A2A_LOG_DIRECTION_OUTBOUND,
                success ? A2A_KIND_SUCCESS : A2A_KIND_FAILURE,
                null,
                null,
                new ExtraAttributes(taskId, configId, null));
    }

    /**
     * Emits an authorization decision as a server-side audit record.
     *
     * <p>The A2A protocol deliberately reports a missing task and an inaccessible task the same way
     * so that callers cannot probe for task existence. {@code errorType} keeps that distinction on
     * the server, where it is needed to investigate denials.
     *
     * @param operation operation whose access was checked
     * @param taskId task the check applied to, or null when the operation is not task-scoped
     * @param errorType simple class name of the rejection, or null when access was granted
     */
    public void emitAuthorization(
            String operation, @Nullable String taskId, @Nullable String errorType) {
        emit(
                A2A_EVENT_AUTHORIZATION,
                operation,
                A2A_LOG_DIRECTION_INBOUND,
                errorType == null ? A2A_KIND_ALLOW : A2A_KIND_DENY,
                null,
                null,
                new ExtraAttributes(taskId, null, errorType));
    }

    private void emit(
            String event,
            String operation,
            String direction,
            @Nullable String kind,
            @Nullable Object attributesSource,
            @Nullable Object payload,
            ExtraAttributes extra) {
        try {
            Context context = Context.current();
            if (!logger.isEnabled(severity, context)) {
                return;
            }
            LogRecordBuilder record =
                    logger.logRecordBuilder()
                            .setContext(context)
                            .setSeverity(severity)
                            .setSeverityText(severity.name())
                            .setEventName(event)
                            .setBody(event)
                            .setAttribute(A2AObservabilityNames.A2A_EVENT_NAME, event)
                            .setAttribute(A2AObservabilityNames.A2A_OPERATION_NAME, operation)
                            .setAttribute(A2AObservabilityNames.A2A_LOG_DIRECTION, direction);
            if (kind != null) {
                record.setAttribute(A2AObservabilityNames.A2A_EVENT_KIND, kind);
            }
            addProtocolAttributes(record, attributesSource);
            put(record, A2AObservabilityNames.A2A_TASK_ID, extra.taskId());
            put(record, A2AObservabilityNames.A2A_NOTIFICATION_ID, extra.configId());
            put(record, A2AObservabilityNames.ERROR_TYPE, extra.errorType());
            if (payloadFormatter != null && payload != null) {
                try {
                    ProtocolPayloadFormatter.Result formatted = payloadFormatter.format(payload);
                    record.setBody(formatted.text())
                            .setAttribute(
                                    A2AObservabilityNames.A2A_PAYLOAD_TRUNCATED,
                                    formatted.truncated());
                    if (formatted.projectedBytes() >= 0) {
                        record.setAttribute(
                                A2AObservabilityNames.A2A_PAYLOAD_PROJECTED_SIZE,
                                formatted.projectedBytes());
                    }
                } catch (RuntimeException ignored) {
                    record.setAttribute(A2AObservabilityNames.A2A_PAYLOAD_OMITTED, true);
                }
            }
            record.emit();
        } catch (RuntimeException ignored) {
            // Logging is best effort and must not interfere with protocol execution.
        }
    }

    private static void addProtocolAttributes(LogRecordBuilder record, @Nullable Object payload) {
        if (payload instanceof MessageSendParams params) {
            addProtocolAttributes(record, params.message());
        } else if (payload instanceof Message message) {
            put(record, A2AObservabilityNames.A2A_MESSAGE_ID, message.messageId());
            put(record, A2AObservabilityNames.A2A_TASK_ID, message.taskId());
            put(record, A2AObservabilityNames.A2A_CONTEXT_ID, message.contextId());
        } else if (payload instanceof Task task) {
            put(record, A2AObservabilityNames.A2A_TASK_ID, task.id());
            put(record, A2AObservabilityNames.A2A_CONTEXT_ID, task.contextId());
            put(record, A2AObservabilityNames.A2A_TASK_STATE, task.status().state().name());
        } else if (payload instanceof TaskStatusUpdateEvent status) {
            put(record, A2AObservabilityNames.A2A_TASK_ID, status.taskId());
            put(record, A2AObservabilityNames.A2A_CONTEXT_ID, status.contextId());
            put(record, A2AObservabilityNames.A2A_TASK_STATE, status.status().state().name());
        } else if (payload instanceof TaskArtifactUpdateEvent artifact) {
            put(record, A2AObservabilityNames.A2A_TASK_ID, artifact.taskId());
            put(record, A2AObservabilityNames.A2A_CONTEXT_ID, artifact.contextId());
            put(record, A2AObservabilityNames.A2A_ARTIFACT_ID, artifact.artifact().artifactId());
        } else if (payload instanceof TaskIdParams params) {
            put(record, A2AObservabilityNames.A2A_TASK_ID, params.id());
        } else if (payload instanceof TaskPushNotificationConfig config) {
            put(record, A2AObservabilityNames.A2A_TASK_ID, config.taskId());
            put(record, A2AObservabilityNames.A2A_NOTIFICATION_ID, config.id());
        } else if (payload instanceof DeleteTaskPushNotificationConfigParams params) {
            put(record, A2AObservabilityNames.A2A_TASK_ID, params.taskId());
            put(record, A2AObservabilityNames.A2A_NOTIFICATION_ID, params.id());
        }
    }

    private static void put(LogRecordBuilder record, String key, @Nullable String value) {
        if (value != null) {
            record.setAttribute(key, value);
        }
    }

    /** Attributes that cannot be derived from a payload. Each is omitted when null. */
    private record ExtraAttributes(
            @Nullable String taskId, @Nullable String configId, @Nullable String errorType) {
        private static final ExtraAttributes NONE = new ExtraAttributes(null, null, null);
    }
}
