package org.a2aproject.sdk.extras.opentelemetry;

import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.ERROR_TYPE;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.ERROR_TYPE_VALUE;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_OPERATION_NAME;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_SYSTEM;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_SYSTEM_VALUE;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_TASK_ID;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_TASK_STATUS_STATE;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.trace.SpanBuilder;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.StreamingEventKind;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskArtifactUpdateEvent;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;
import org.jspecify.annotations.Nullable;

public final class StreamingEventAttributes {

    private StreamingEventAttributes() {
    }

    /**
     * Applies task-specific attributes derived from the streaming event to the span builder.
     * Populates task ID, task status state, and other event-kind-specific fields.
     *
     * @param builder the span builder to enrich with event attributes
     * @param item the streaming event to extract attributes from
     */
    public static void apply(SpanBuilder builder, StreamingEventKind item) {
        switch (item.kind()) {
            case TaskStatusUpdateEvent.STREAMING_EVENT_ID -> {
                TaskStatusUpdateEvent event = (TaskStatusUpdateEvent) item;
                builder.setAttribute(GENAI_TASK_STATUS_STATE, event.status().state().name());
                builder.setAttribute(GENAI_TASK_ID, event.taskId());
            }
            case Task.STREAMING_EVENT_ID -> {
                Task task = (Task) item;
                builder.setAttribute(GENAI_TASK_STATUS_STATE, task.status().state().name());
                builder.setAttribute(GENAI_TASK_ID, task.id());
            }
            case TaskArtifactUpdateEvent.STREAMING_EVENT_ID -> {
                TaskArtifactUpdateEvent event = (TaskArtifactUpdateEvent) item;
                builder.setAttribute(GENAI_TASK_ID, event.taskId());
            }
            case Message.STREAMING_EVENT_ID -> {
                Message message = (Message) item;
                if (message.taskId() != null) {
                    builder.setAttribute(GENAI_TASK_ID, message.taskId());
                }
            }
            default -> {
            }
        }
    }

    /**
     * Records elapsed time to a histogram using the standard A2A operation attributes.
     *
     * <p>The {@code error.type} attribute is set to the fixed string {@value A2AObservabilityNames#ERROR_TYPE_VALUE}
     * on failures rather than the actual exception class name. This is intentional: metrics use a low-cardinality
     * error marker to avoid unbounded label sets, while spans carry the full class name for debugging.
     *
     * @param histogram  the histogram to record to; no-op if {@code null}
     * @param startNanos the start time from {@link System#nanoTime()}
     * @param operationName the A2A operation name recorded as {@code gen_ai.agent.a2a.operation.name}
     * @param success    {@code false} adds the {@code error.type} attribute
     */
    public static void recordDuration(@Nullable DoubleHistogram histogram, long startNanos,
            String operationName, boolean success) {
        if (histogram == null) {
            return;
        }
        double seconds = (System.nanoTime() - startNanos) / 1_000_000_000.0;
        AttributesBuilder builder = Attributes.builder()
                .put(GENAI_OPERATION_NAME, operationName)
                .put(GENAI_SYSTEM, GENAI_SYSTEM_VALUE);
        if (!success) {
            builder.put(ERROR_TYPE, ERROR_TYPE_VALUE);
        }
        histogram.record(seconds, builder.build());
    }
}
