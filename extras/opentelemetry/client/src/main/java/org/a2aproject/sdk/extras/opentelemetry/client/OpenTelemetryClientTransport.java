package org.a2aproject.sdk.extras.opentelemetry.client;

import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.ERROR_TYPE;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.EXTRACT_REQUEST_SYS_PROPERTY;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.EXTRACT_RESPONSE_SYS_PROPERTY;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_CLIENT_OPERATION_DURATION;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_CONFIG_ID;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_CONTEXT_ID;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_EXTENSIONS;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_MESSAGE_ID;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_OPERATION_NAME;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_PARTS_NUMBER;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_REQUEST;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_RESPONSE;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_ROLE;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_SYSTEM;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_SYSTEM_VALUE;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_TASK_ID;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.a2aproject.sdk.client.transport.spi.ClientTransport;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallContext;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.A2AMethods;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.CancelTaskParams;
import org.a2aproject.sdk.spec.DeleteTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.EventKind;
import org.a2aproject.sdk.spec.GetExtendedAgentCardParams;
import org.a2aproject.sdk.spec.GetTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsParams;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsResult;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.StreamingEventKind;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskIdParams;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.jspecify.annotations.Nullable;

public class OpenTelemetryClientTransport implements ClientTransport {

    private final Tracer tracer;
    private final ClientTransport delegate;
    @Nullable private final DoubleHistogram operationDurationHistogram;

    public OpenTelemetryClientTransport(ClientTransport delegate, Tracer tracer) {
        this(delegate, tracer, null);
    }

    /**
     * @param meter optional meter for recording {@code gen_ai.client.operation.duration}; null disables metrics
     */
    public OpenTelemetryClientTransport(ClientTransport delegate, Tracer tracer, @Nullable Meter meter) {
        this.delegate = delegate;
        this.tracer = tracer;
        this.operationDurationHistogram = meter == null ? null
                : meter.histogramBuilder(GENAI_CLIENT_OPERATION_DURATION)
                        .setUnit("s")
                        .setDescription("Duration of A2A client operations")
                        .build();
    }

    private boolean extractRequest() {
        return Boolean.getBoolean(EXTRACT_REQUEST_SYS_PROPERTY);
    }

    private boolean extractResponse() {
        return Boolean.getBoolean(EXTRACT_RESPONSE_SYS_PROPERTY);
    }

    @Override
    public EventKind sendMessage(MessageSendParams request, @Nullable ClientCallContext context) throws A2AClientException {
        ClientCallContext clientContext = createContext(context);
        long startNanos = System.nanoTime();
        SpanBuilder spanBuilder = tracer.spanBuilder(A2AMethods.SEND_MESSAGE_METHOD).setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute(GENAI_OPERATION_NAME, A2AMethods.SEND_MESSAGE_METHOD);
        if (request.message() != null) {
            if (request.message().taskId() != null) {
                spanBuilder.setAttribute(GENAI_TASK_ID, request.message().taskId());
            }
            if (request.message().contextId() != null) {
                spanBuilder.setAttribute(GENAI_CONTEXT_ID, request.message().contextId());
            }
            if (request.message().messageId() != null) {
                spanBuilder.setAttribute(GENAI_MESSAGE_ID, request.message().messageId());
            }
            if (request.message().role() != null) {
                spanBuilder.setAttribute(GENAI_ROLE, request.message().role().name());
            }
            if (request.message().extensions() != null && !request.message().extensions().isEmpty()) {
                spanBuilder.setAttribute(GENAI_EXTENSIONS, String.join(",", request.message().extensions()));
            }
            spanBuilder.setAttribute(GENAI_PARTS_NUMBER, request.message().parts().size());
        }
        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, request.toString());
        }
        Span span = spanBuilder.startSpan();
        boolean success = false;
        try (Scope scope = span.makeCurrent()) {
            EventKind result = delegate.sendMessage(request, clientContext);
            if (result != null && extractResponse()) {
                span.setAttribute(GENAI_RESPONSE, result.toString());
            }
            if (result != null) {
                span.setStatus(StatusCode.OK);
            }
            success = true;
            return result;
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
            recordOperationDuration(startNanos, A2AMethods.SEND_MESSAGE_METHOD, success);
        }
    }

    @Override
    public void sendMessageStreaming(MessageSendParams request, Consumer<StreamingEventKind> eventConsumer,
            Consumer<Throwable> errorConsumer, @Nullable ClientCallContext context) throws A2AClientException {
        ClientCallContext clientContext = createContext(context);
        long startNanos = System.nanoTime();
        SpanBuilder spanBuilder = tracer.spanBuilder(A2AMethods.SEND_STREAMING_MESSAGE_METHOD).setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute(GENAI_OPERATION_NAME, A2AMethods.SEND_STREAMING_MESSAGE_METHOD);
        if (request.message() != null) {
            if (request.message().taskId() != null) {
                spanBuilder.setAttribute(GENAI_TASK_ID, request.message().taskId());
            }
            if (request.message().contextId() != null) {
                spanBuilder.setAttribute(GENAI_CONTEXT_ID, request.message().contextId());
            }
            if (request.message().messageId() != null) {
                spanBuilder.setAttribute(GENAI_MESSAGE_ID, request.message().messageId());
            }
            if (request.message().role() != null) {
                spanBuilder.setAttribute(GENAI_ROLE, request.message().role().name());
            }
            if (request.message().extensions() != null && !request.message().extensions().isEmpty()) {
                spanBuilder.setAttribute(GENAI_EXTENSIONS, String.join(",", request.message().extensions()));
            }
            spanBuilder.setAttribute(GENAI_PARTS_NUMBER, request.message().parts().size());
        }
        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, request.toString());
        }
        Span span = spanBuilder.startSpan();
        boolean success = false;
        try (Scope scope = span.makeCurrent()) {
            delegate.sendMessageStreaming(
                    request,
                    new OpenTelemetryEventConsumer(A2AMethods.SEND_STREAMING_MESSAGE_METHOD + "-event", eventConsumer, span),
                    new OpenTelemetryErrorConsumer(A2AMethods.SEND_STREAMING_MESSAGE_METHOD + "-error", errorConsumer, span),
                    clientContext
            );
            span.setStatus(StatusCode.OK);
            success = true;
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
            recordOperationDuration(startNanos, A2AMethods.SEND_STREAMING_MESSAGE_METHOD, success);
        }
    }

    @Override
    public Task getTask(TaskQueryParams request, @Nullable ClientCallContext context) throws A2AClientException {
        ClientCallContext clientContext = createContext(context);
        long startNanos = System.nanoTime();
        SpanBuilder spanBuilder = tracer.spanBuilder(A2AMethods.GET_TASK_METHOD).setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute(GENAI_OPERATION_NAME, A2AMethods.GET_TASK_METHOD);
        if (request.id() != null) {
            spanBuilder.setAttribute(GENAI_TASK_ID, request.id());
        }
        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, request.toString());
        }
        Span span = spanBuilder.startSpan();
        boolean success = false;
        try (Scope scope = span.makeCurrent()) {
            Task result = delegate.getTask(request, clientContext);
            if (result != null && extractResponse()) {
                span.setAttribute(GENAI_RESPONSE, result.toString());
            }
            if (result != null) {
                span.setStatus(StatusCode.OK);
            }
            success = true;
            return result;
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
            recordOperationDuration(startNanos, A2AMethods.GET_TASK_METHOD, success);
        }
    }

    @Override
    public Task cancelTask(CancelTaskParams request, @Nullable ClientCallContext context) throws A2AClientException {
        ClientCallContext clientContext = createContext(context);
        long startNanos = System.nanoTime();
        SpanBuilder spanBuilder = tracer.spanBuilder(A2AMethods.CANCEL_TASK_METHOD).setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute(GENAI_OPERATION_NAME, A2AMethods.CANCEL_TASK_METHOD);
        if (request.id() != null) {
            spanBuilder.setAttribute(GENAI_TASK_ID, request.id());
        }
        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, request.toString());
        }
        Span span = spanBuilder.startSpan();
        boolean success = false;
        try (Scope scope = span.makeCurrent()) {
            Task result = delegate.cancelTask(request, clientContext);
            if (result != null && extractResponse()) {
                span.setAttribute(GENAI_RESPONSE, result.toString());
            }
            if (result != null) {
                span.setStatus(StatusCode.OK);
            }
            success = true;
            return result;
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
            recordOperationDuration(startNanos, A2AMethods.CANCEL_TASK_METHOD, success);
        }
    }

    @Override
    public ListTasksResult listTasks(ListTasksParams request, @Nullable ClientCallContext context) throws A2AClientException {
        ClientCallContext clientContext = createContext(context);
        long startNanos = System.nanoTime();
        SpanBuilder spanBuilder = tracer.spanBuilder(A2AMethods.LIST_TASK_METHOD).setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute(GENAI_OPERATION_NAME, A2AMethods.LIST_TASK_METHOD);
        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, request.toString());
        }
        if (request.contextId() != null) {
            spanBuilder.setAttribute(GENAI_CONTEXT_ID, request.contextId());
        }
        Span span = spanBuilder.startSpan();
        boolean success = false;
        try (Scope scope = span.makeCurrent()) {
            ListTasksResult result = delegate.listTasks(request, clientContext);
            if (result != null && extractResponse()) {
                span.setAttribute(GENAI_RESPONSE, result.toString());
            }
            if (result != null) {
                span.setStatus(StatusCode.OK);
            }
            success = true;
            return result;
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
            recordOperationDuration(startNanos, A2AMethods.LIST_TASK_METHOD, success);
        }
    }

    @Override
    public TaskPushNotificationConfig createTaskPushNotificationConfiguration(TaskPushNotificationConfig request,
            @Nullable ClientCallContext context) throws A2AClientException {
        ClientCallContext clientContext = createContext(context);
        long startNanos = System.nanoTime();
        SpanBuilder spanBuilder = tracer.spanBuilder(A2AMethods.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD).setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute(GENAI_OPERATION_NAME, A2AMethods.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
        if (request.taskId() != null) {
            spanBuilder.setAttribute(GENAI_TASK_ID, request.taskId());
        }
        if (request.id() != null) {
            spanBuilder.setAttribute(GENAI_CONFIG_ID, request.id());
        }
        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, request.toString());
        }
        Span span = spanBuilder.startSpan();
        boolean success = false;
        try (Scope scope = span.makeCurrent()) {
            TaskPushNotificationConfig result = delegate.createTaskPushNotificationConfiguration(request, clientContext);
            if (result != null && extractResponse()) {
                span.setAttribute(GENAI_RESPONSE, result.toString());
            }
            if (result != null) {
                span.setStatus(StatusCode.OK);
            }
            success = true;
            return result;
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
            recordOperationDuration(startNanos, A2AMethods.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, success);
        }
    }

    @Override
    public TaskPushNotificationConfig getTaskPushNotificationConfiguration(GetTaskPushNotificationConfigParams request,
            @Nullable ClientCallContext context) throws A2AClientException {
        ClientCallContext clientContext = createContext(context);
        long startNanos = System.nanoTime();
        SpanBuilder spanBuilder = tracer.spanBuilder(A2AMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD).setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute(GENAI_OPERATION_NAME, A2AMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
        if (request.taskId() != null) {
            spanBuilder.setAttribute(GENAI_TASK_ID, request.taskId());
        }
        if (request.id() != null) {
            spanBuilder.setAttribute(GENAI_CONFIG_ID, request.id());
        }
        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, request.toString());
        }
        Span span = spanBuilder.startSpan();
        boolean success = false;
        try (Scope scope = span.makeCurrent()) {
            TaskPushNotificationConfig result = delegate.getTaskPushNotificationConfiguration(request, clientContext);
            if (result != null && extractResponse()) {
                span.setAttribute(GENAI_RESPONSE, result.toString());
            }
            if (result != null) {
                span.setStatus(StatusCode.OK);
            }
            success = true;
            return result;
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
            recordOperationDuration(startNanos, A2AMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, success);
        }
    }

    @Override
    public ListTaskPushNotificationConfigsResult listTaskPushNotificationConfigurations(ListTaskPushNotificationConfigsParams request,
            @Nullable ClientCallContext context) throws A2AClientException {
        ClientCallContext clientContext = createContext(context);
        long startNanos = System.nanoTime();
        SpanBuilder spanBuilder = tracer.spanBuilder(A2AMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD).setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute(GENAI_OPERATION_NAME, A2AMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, request.toString());
        }
        if (request.id() != null) {
            spanBuilder.setAttribute(GENAI_TASK_ID, request.id());
        }
        Span span = spanBuilder.startSpan();
        boolean success = false;
        try (Scope scope = span.makeCurrent()) {
            ListTaskPushNotificationConfigsResult result = delegate.listTaskPushNotificationConfigurations(request, clientContext);
            if (result != null && extractResponse()) {
                String responseValue = result.configs().stream()
                        .map(TaskPushNotificationConfig::toString)
                        .collect(Collectors.joining(","));
                span.setAttribute(GENAI_RESPONSE, responseValue);
            }
            if (result != null) {
                span.setStatus(StatusCode.OK);
            }
            success = true;
            return result;
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
            recordOperationDuration(startNanos, A2AMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, success);
        }
    }

    @Override
    public void deleteTaskPushNotificationConfigurations(DeleteTaskPushNotificationConfigParams request,
            @Nullable ClientCallContext context) throws A2AClientException {
        ClientCallContext clientContext = createContext(context);
        long startNanos = System.nanoTime();
        SpanBuilder spanBuilder = tracer.spanBuilder(A2AMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD).setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute(GENAI_OPERATION_NAME, A2AMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, request.toString());
        }
        if (request.taskId() != null) {
            spanBuilder.setAttribute(GENAI_TASK_ID, request.taskId());
        }
        if (request.id() != null) {
            spanBuilder.setAttribute(GENAI_CONFIG_ID, request.id());
        }
        Span span = spanBuilder.startSpan();
        boolean success = false;
        try (Scope scope = span.makeCurrent()) {
            delegate.deleteTaskPushNotificationConfigurations(request, clientContext);
            span.setStatus(StatusCode.OK);
            success = true;
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
            recordOperationDuration(startNanos, A2AMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, success);
        }
    }

    @Override
    public void subscribeToTask(TaskIdParams request, Consumer<StreamingEventKind> eventConsumer,
            Consumer<Throwable> errorConsumer, @Nullable ClientCallContext context) throws A2AClientException {
        ClientCallContext clientContext = createContext(context);
        long startNanos = System.nanoTime();
        SpanBuilder spanBuilder = tracer.spanBuilder(A2AMethods.SUBSCRIBE_TO_TASK_METHOD).setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute(GENAI_OPERATION_NAME, A2AMethods.SUBSCRIBE_TO_TASK_METHOD);
        if (request.id() != null) {
            spanBuilder.setAttribute(GENAI_TASK_ID, request.id());
        }
        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, request.toString());
        }
        Span span = spanBuilder.startSpan();
        boolean success = false;
        try (Scope scope = span.makeCurrent()) {
            delegate.subscribeToTask(
                    request,
                    new OpenTelemetryEventConsumer(A2AMethods.SUBSCRIBE_TO_TASK_METHOD + "-event", eventConsumer, span),
                    new OpenTelemetryErrorConsumer(A2AMethods.SUBSCRIBE_TO_TASK_METHOD + "-error", errorConsumer, span),
                    clientContext
            );
            span.setStatus(StatusCode.OK);
            success = true;
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
            recordOperationDuration(startNanos, A2AMethods.SUBSCRIBE_TO_TASK_METHOD, success);
        }
    }

    @Override
    public AgentCard getExtendedAgentCard(GetExtendedAgentCardParams params, @Nullable ClientCallContext context) throws A2AClientException {
        ClientCallContext clientContext = createContext(context);
        long startNanos = System.nanoTime();
        SpanBuilder spanBuilder = tracer.spanBuilder(A2AMethods.GET_EXTENDED_AGENT_CARD_METHOD).setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute(GENAI_OPERATION_NAME, A2AMethods.GET_EXTENDED_AGENT_CARD_METHOD);
        Span span = spanBuilder.startSpan();
        boolean success = false;
        try (Scope scope = span.makeCurrent()) {
            AgentCard result = delegate.getExtendedAgentCard(params, clientContext);
            if (result != null && extractResponse()) {
                span.setAttribute(GENAI_RESPONSE, result.toString());
            }
            if (result != null) {
                span.setStatus(StatusCode.OK);
            }
            success = true;
            return result;
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
            recordOperationDuration(startNanos, A2AMethods.GET_EXTENDED_AGENT_CARD_METHOD, success);
        }
    }

    private void recordOperationDuration(long startNanos, String operationName, boolean success) {
        if (operationDurationHistogram == null) {
            return;
        }
        double seconds = (System.nanoTime() - startNanos) / 1_000_000_000.0;
        AttributesBuilder builder = Attributes.builder()
                .put(GENAI_OPERATION_NAME, operationName)
                .put(GENAI_SYSTEM, GENAI_SYSTEM_VALUE);
        if (!success) {
            builder.put(ERROR_TYPE, "error");
        }
        operationDurationHistogram.record(seconds, builder.build());
    }

    private ClientCallContext createContext(@Nullable ClientCallContext context) {
        if (context == null) {
            return new ClientCallContext(Map.of(), new HashMap<>());
        }
        return new ClientCallContext(context.getState(), new HashMap<>(context.getHeaders()));
    }

    @Override
    public void close() {
        delegate.close();
    }

    private static class OpenTelemetryEventConsumer implements Consumer<StreamingEventKind> {

        private final Consumer<StreamingEventKind> delegate;
        private final Span span;
        private final String name;

        public OpenTelemetryEventConsumer(String name, Consumer<StreamingEventKind> delegate, Span span) {
            this.delegate = delegate;
            this.span = span;
            this.name = name;
        }

        @Override
        public void accept(StreamingEventKind t) {
            AttributesBuilder builder = Attributes.builder();
            builder.put("gen_ai.agent.a2a.streaming-event", t.toString());
            try {
                delegate.accept(t);
                builder.put("gen_ai.agent.a2a.status.code", StatusCode.OK.name());
            } finally {
                span.addEvent(name, builder.build());
            }
        }
    }

    private static class OpenTelemetryErrorConsumer implements Consumer<Throwable> {

        private final Consumer<Throwable> delegate;
        private final Span span;
        private final String name;

        public OpenTelemetryErrorConsumer(String name, Consumer<Throwable> delegate, Span span) {
            this.delegate = delegate;
            this.span = span;
            this.name = name;
        }

        @Override
        public void accept(Throwable t) {
            if (t == null) {
                return;
            }
            AttributesBuilder builder = Attributes.builder();
            builder.put("gen_ai.agent.a2a.status.code", StatusCode.ERROR.name());
            builder.put("gen_ai.agent.a2a.status.description", t.getMessage());
            try {
                delegate.accept(t);
            } finally {
                span.addEvent(name, builder.build());
            }
        }
    }
}
