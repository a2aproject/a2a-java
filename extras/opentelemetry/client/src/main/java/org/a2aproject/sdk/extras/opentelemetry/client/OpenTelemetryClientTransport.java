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
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_STREAMING_EVENT;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_TASK_ID;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.STREAMING_END_SUFFIX;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.STREAMING_EVENT_SUFFIX;

import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.a2aproject.sdk.client.transport.spi.ClientTransport;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallContext;
import org.a2aproject.sdk.extras.opentelemetry.StreamingEventAttributes;
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
        AtomicBoolean terminated = new AtomicBoolean(false);
        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            SpanContext spanContext = span.getSpanContext();
            delegate.sendMessageStreaming(
                    request,
                    new OpenTelemetryEventConsumer(A2AMethods.SEND_STREAMING_MESSAGE_METHOD + STREAMING_EVENT_SUFFIX,
                            eventConsumer, tracer, spanContext),
                    new OpenTelemetryTerminalConsumer(errorConsumer,
                            new TerminalConsumerConfig(A2AMethods.SEND_STREAMING_MESSAGE_METHOD + STREAMING_END_SUFFIX,
                                    tracer, spanContext, startNanos, A2AMethods.SEND_STREAMING_MESSAGE_METHOD,
                                    operationDurationHistogram, terminated)),
                    clientContext
            );
            span.setStatus(StatusCode.OK);
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            if (!terminated.get()) {
                recordOperationDuration(startNanos, A2AMethods.SEND_STREAMING_MESSAGE_METHOD, false);
            }
            throw ex;
        } finally {
            span.end();
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
        AtomicBoolean terminated = new AtomicBoolean(false);
        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            SpanContext spanContext = span.getSpanContext();
            delegate.subscribeToTask(
                    request,
                    new OpenTelemetryEventConsumer(A2AMethods.SUBSCRIBE_TO_TASK_METHOD + STREAMING_EVENT_SUFFIX,
                            eventConsumer, tracer, spanContext),
                    new OpenTelemetryTerminalConsumer(errorConsumer,
                            new TerminalConsumerConfig(A2AMethods.SUBSCRIBE_TO_TASK_METHOD + STREAMING_END_SUFFIX,
                                    tracer, spanContext, startNanos, A2AMethods.SUBSCRIBE_TO_TASK_METHOD,
                                    operationDurationHistogram, terminated)),
                    clientContext
            );
            span.setStatus(StatusCode.OK);
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            if (!terminated.get()) {
                recordOperationDuration(startNanos, A2AMethods.SUBSCRIBE_TO_TASK_METHOD, false);
            }
            throw ex;
        } finally {
            span.end();
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
        StreamingEventAttributes.recordDuration(operationDurationHistogram, startNanos, operationName, success);
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
        private final Tracer tracer;
        private final SpanContext parentSpanContext;
        private final String name;

        OpenTelemetryEventConsumer(String name, Consumer<StreamingEventKind> delegate,
                Tracer tracer, SpanContext parentSpanContext) {
            this.delegate = delegate;
            this.tracer = tracer;
            this.parentSpanContext = parentSpanContext;
            this.name = name;
        }

        @Override
        public void accept(StreamingEventKind t) {
            SpanBuilder builder = tracer.spanBuilder(name)
                    .setSpanKind(SpanKind.CLIENT)
                    .addLink(parentSpanContext)
                    .setAttribute(GENAI_STREAMING_EVENT, t.kind());
            StreamingEventAttributes.apply(builder, t);
            Span eventSpan = builder.startSpan();
            try (Scope ignored = eventSpan.makeCurrent()) {
                delegate.accept(t);
                eventSpan.setStatus(StatusCode.OK);
            } catch (RuntimeException | Error e) {
                eventSpan.recordException(e);
                eventSpan.setStatus(StatusCode.ERROR);
                throw e;
            } finally {
                eventSpan.end();
            }
        }
    }

    private record TerminalConsumerConfig(String name, Tracer tracer, SpanContext parentSpanContext,
            long startNanos, String operationName, @Nullable DoubleHistogram operationDurationHistogram,
            AtomicBoolean terminated) {}

    private static class OpenTelemetryTerminalConsumer implements Consumer<Throwable> {

        private final Consumer<Throwable> delegate;
        private final TerminalConsumerConfig config;

        OpenTelemetryTerminalConsumer(Consumer<Throwable> delegate, TerminalConsumerConfig config) {
            this.delegate = delegate;
            this.config = config;
        }

        @Override
        public void accept(@Nullable Throwable t) {
            if (!config.terminated().compareAndSet(false, true)) {
                return;
            }
            Span closingSpan = config.tracer().spanBuilder(config.name())
                    .setSpanKind(SpanKind.CLIENT)
                    .addLink(config.parentSpanContext())
                    .startSpan();
            try (Scope ignored = closingSpan.makeCurrent()) {
                if (t == null) {
                    closingSpan.setStatus(StatusCode.OK);
                    delegate.accept(null);
                } else {
                    closingSpan.setAttribute(ERROR_TYPE, t.getClass().getName());
                    closingSpan.setStatus(StatusCode.ERROR, t.getMessage());
                    delegate.accept(t);
                }
            } finally {
                closingSpan.end();
                StreamingEventAttributes.recordDuration(config.operationDurationHistogram(),
                        config.startNanos(), config.operationName(), t == null);
            }
        }
    }
}
