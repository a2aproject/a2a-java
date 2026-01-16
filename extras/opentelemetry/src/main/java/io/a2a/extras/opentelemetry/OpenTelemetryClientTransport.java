package io.a2a.extras.opentelemetry;


import io.a2a.client.transport.spi.ClientTransport;
import io.a2a.client.transport.spi.interceptors.ClientCallContext;
import io.a2a.jsonrpc.common.wrappers.ListTasksResult;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.A2AMethods;
import io.a2a.spec.AgentCard;
import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.EventKind;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigResult;
import io.a2a.spec.ListTasksParams;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

public class OpenTelemetryClientTransport implements ClientTransport {

    private final Tracer tracer;
    private final OpenTelemetry openTelemetry;
    private final ClientTransport delegate;
    private static final String REQUEST_TRACE_ATTRIBUTE = "request";
    private static final String RESPONSE_TRACE_ATTRIBUTE = "response";

    private static final TextMapSetter<Map<String, String>> MAP_SETTER = new TextMapSetter<Map<String, String>>() {
        @Override
        public void set(@Nullable Map<String, String> carrier, String key, String value) {
            if (carrier != null) {
                carrier.put(key, value);
            }
        }
    };

    public OpenTelemetryClientTransport(ClientTransport delegate, OpenTelemetry openTelemetry, Tracer tracer) {
        this.delegate = delegate;
        this.openTelemetry = openTelemetry;
        this.tracer = tracer;
    }

    /**
     * Traces an operation that returns a value with a request parameter.
     *
     * @param operationName the name of the operation for the span
     * @param request the request object
     * @param operation the operation to execute
     * @param responseFormatter optional function to format the response for span attribute (defaults to toString)
     * @param <R> the request type
     * @param <T> the return type
     * @return the result of the operation
     * @throws A2AClientException if the operation fails
     */
    private <R, T> T traceOperation(String operationName, R request, ClientCallContext context, ThrowingSupplier<T> operation,
                                     @Nullable Function<T, String> responseFormatter) throws A2AClientException {
        SpanBuilder spanBuilder = tracer.spanBuilder(operationName).setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute(REQUEST_TRACE_ATTRIBUTE, request.toString());
        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            openTelemetry.getPropagators().getTextMapPropagator().inject(Context.current(), context.getHeaders(), MAP_SETTER);
            T result = operation.get();
            if (result != null) {
                String responseValue = responseFormatter != null ? responseFormatter.apply(result) : result.toString();
                span.setAttribute(RESPONSE_TRACE_ATTRIBUTE, responseValue);
                span.setStatus(StatusCode.OK);
            }
            return result;
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
        }
    }

    /**
     * Traces an operation that returns a value without a request parameter.
     *
     * @param operationName the name of the operation for the span
     * @param operation the operation to execute
     * @param <T> the return type
     * @return the result of the operation
     * @throws A2AClientException if the operation fails
     */
    private <T> T traceOperationNoRequest(String operationName,
                                            ClientCallContext context,
                                            ThrowingSupplier<T> operation) throws A2AClientException {
        SpanBuilder spanBuilder = tracer.spanBuilder(operationName).setSpanKind(SpanKind.CLIENT);
        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            openTelemetry.getPropagators().getTextMapPropagator().inject(Context.current(), context.getHeaders(), MAP_SETTER);
            T result = operation.get();
            if (result != null) {
                span.setAttribute(RESPONSE_TRACE_ATTRIBUTE, result.toString());
                span.setStatus(StatusCode.OK);
            }
            return result;
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
        }
    }

    /**
     * Traces a void operation with a request parameter.
     *
     * @param operationName the name of the operation for the span
     * @param request the request object
     * @param operation the operation to execute
     * @param <R> the request type
     * @throws A2AClientException if the operation fails
     */
    private <R> void traceVoidOperation(String operationName,
                                          R request,
                                          ClientCallContext context,
                                          ThrowingRunnable operation) throws A2AClientException {
        SpanBuilder spanBuilder = tracer.spanBuilder(operationName).setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute(REQUEST_TRACE_ATTRIBUTE, request.toString());
        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            openTelemetry.getPropagators().getTextMapPropagator().inject(Context.current(), context.getHeaders(), MAP_SETTER);
            operation.run();
            span.setStatus(StatusCode.OK);
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
        }
    }

    /**
     * Traces a streaming operation with wrapped consumers.
     *
     * @param operationName the name of the operation for the span
     * @param request the request object
     * @param eventConsumer the event consumer
     * @param errorConsumer the error consumer
     * @param operation the operation to execute with wrapped consumers
     * @param <R> the request type
     * @throws A2AClientException if the operation fails
     */
    private <R> void traceStreamingOperation(String operationName, R request,
                                              ClientCallContext context,
                                              Consumer<StreamingEventKind> eventConsumer,
                                              Consumer<Throwable> errorConsumer,
                                              StreamingOperation operation) throws A2AClientException {
        SpanBuilder spanBuilder = tracer.spanBuilder(operationName).setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute(REQUEST_TRACE_ATTRIBUTE, request.toString());
        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            openTelemetry.getPropagators().getTextMapPropagator().inject(Context.current(), context.getHeaders(), MAP_SETTER);
            operation.execute(
                new OpenTelemetryEventConsumer(operationName + "-event", eventConsumer, tracer, span.getSpanContext()),
                new OpenTelemetryErrorConsumer(operationName + "-error", errorConsumer, tracer, span.getSpanContext())
            );
            span.setStatus(StatusCode.OK);
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
        }
    }

    @Override
    public EventKind sendMessage(MessageSendParams request, @Nullable ClientCallContext context) throws A2AClientException {
        ClientCallContext clientContext = createContext(context);
        return traceOperation(A2AMethods.SEND_MESSAGE_METHOD, request, clientContext, () -> delegate.sendMessage(request, clientContext), null);
    }

    @Override
    public void sendMessageStreaming(MessageSendParams request, Consumer<StreamingEventKind> eventConsumer,
                                      Consumer<Throwable> errorConsumer, @Nullable ClientCallContext context) throws A2AClientException {
        ClientCallContext clientContext = createContext(context);
        traceStreamingOperation(A2AMethods.SEND_STREAMING_MESSAGE_METHOD, request, clientContext, eventConsumer, errorConsumer,
                (wrappedEvent, wrappedError) -> delegate.sendMessageStreaming(request, wrappedEvent, wrappedError, clientContext));
    }

    @Override
    public Task getTask(TaskQueryParams request, @Nullable ClientCallContext context) throws A2AClientException {
        ClientCallContext clientContext = createContext(context);
        return traceOperation(A2AMethods.GET_TASK_METHOD, request, clientContext, () -> delegate.getTask(request, clientContext), null);
    }

    @Override
    public Task cancelTask(TaskIdParams request, @Nullable ClientCallContext context) throws A2AClientException {
        ClientCallContext clientContext = createContext(context);
        return traceOperation(A2AMethods.CANCEL_TASK_METHOD, request, clientContext, () -> delegate.cancelTask(request, clientContext), null);
    }

    @Override
    public ListTasksResult listTasks(ListTasksParams request, @Nullable ClientCallContext context) throws A2AClientException {
        ClientCallContext clientContext = createContext(context);
        return traceOperation(A2AMethods.LIST_TASK_METHOD, request, clientContext, () -> delegate.listTasks(request, clientContext), null);
    }

    @Override
    public TaskPushNotificationConfig setTaskPushNotificationConfiguration(TaskPushNotificationConfig request,
                                                                            @Nullable ClientCallContext context) throws A2AClientException {
        ClientCallContext clientContext = createContext(context);
        return traceOperation(A2AMethods.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, request, clientContext,
                () -> delegate.setTaskPushNotificationConfiguration(request, clientContext), null);
    }

    @Override
    public TaskPushNotificationConfig getTaskPushNotificationConfiguration(GetTaskPushNotificationConfigParams request,
                                                                            @Nullable ClientCallContext context) throws A2AClientException {
        ClientCallContext clientContext = createContext(context);
        return traceOperation(A2AMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, request, clientContext,
                () -> delegate.getTaskPushNotificationConfiguration(request, clientContext), null);
    }

    @Override
    public ListTaskPushNotificationConfigResult listTaskPushNotificationConfigurations(ListTaskPushNotificationConfigParams request,
                                                                                        @Nullable ClientCallContext context) throws A2AClientException {
        ClientCallContext clientContext = createContext(context);
        return traceOperation(A2AMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, request, clientContext,
                () -> delegate.listTaskPushNotificationConfigurations(request, clientContext),
                result -> result.configs().stream().map(TaskPushNotificationConfig::toString).collect(Collectors.joining(",")));
    }

    @Override
    public void deleteTaskPushNotificationConfigurations(DeleteTaskPushNotificationConfigParams request,
                                                          @Nullable ClientCallContext context) throws A2AClientException {
        ClientCallContext clientContext = createContext(context);
        traceVoidOperation(A2AMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, request, clientContext,
                () -> delegate.deleteTaskPushNotificationConfigurations(request, clientContext));
    }

    @Override
    public void resubscribe(TaskIdParams request, Consumer<StreamingEventKind> eventConsumer,
                            Consumer<Throwable> errorConsumer, @Nullable ClientCallContext context) throws A2AClientException {
        ClientCallContext clientContext = createContext(context);
        traceStreamingOperation(A2AMethods.SUBSCRIBE_TO_TASK_METHOD, request, clientContext, eventConsumer, errorConsumer,
                (wrappedEvent, wrappedError) -> delegate.resubscribe(request, wrappedEvent, wrappedError, clientContext));
    }

    @Override
    public AgentCard getAgentCard(@Nullable ClientCallContext context) throws A2AClientException {
        ClientCallContext clientContext = createContext(context);
        return traceOperationNoRequest(A2AMethods.GET_EXTENDED_AGENT_CARD_METHOD, clientContext, () -> delegate.getAgentCard(clientContext));
    }

    private ClientCallContext createContext(@Nullable ClientCallContext context) {
        if(context == null) {
            return new ClientCallContext(Map.of(), new HashMap<>());
        }
        return new ClientCallContext(context.getState(), new HashMap<>(context.getHeaders()));
    }

    @Override
    public void close() {
        delegate.close();
    }

    /**
     * Functional interface for operations that may throw A2AClientException.
     *
     * @param <T> the return type
     */
    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws A2AClientException;
    }

    /**
     * Functional interface for void operations that may throw A2AClientException.
     */
    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws A2AClientException;
    }

    /**
     * Functional interface for streaming operations with wrapped consumers.
     */
    @FunctionalInterface
    private interface StreamingOperation {
        void execute(Consumer<StreamingEventKind> eventConsumer, Consumer<Throwable> errorConsumer) throws A2AClientException;
    }

    private static class OpenTelemetryEventConsumer implements Consumer<StreamingEventKind> {

        private final Consumer<StreamingEventKind> delegate;
        private final Tracer tracer;
        private final SpanContext context;
        private final String name;

        public OpenTelemetryEventConsumer(String name, Consumer<StreamingEventKind> delegate, Tracer tracer, SpanContext context) {
            this.delegate = delegate;
            this.tracer = tracer;
            this.context = context;
            this.name = name;
        }

        @Override
        public void accept(StreamingEventKind t) {
            SpanBuilder spanBuilder = tracer.spanBuilder(name)
                    .setSpanKind(SpanKind.CLIENT);
            spanBuilder.setAttribute("gen_ai.agent.a2a.streaming-event", t.toString());
            spanBuilder.addLink(context);
            Span span = spanBuilder.startSpan();
            try {
                delegate.accept(t);
                span.setStatus(StatusCode.OK);
            } finally {
                span.end();
            }
        }
    }

    private static class OpenTelemetryErrorConsumer implements Consumer<Throwable> {

        private final Consumer<Throwable> delegate;
        private final Tracer tracer;
        private final SpanContext context;
        private final String name;

        public OpenTelemetryErrorConsumer(String name, Consumer<java.lang.Throwable> delegate, Tracer tracer, SpanContext context) {
            this.delegate = delegate;
            this.tracer = tracer;
            this.context = context;
            this.name = name;
        }

        @Override
        public void accept(Throwable t) {
            if (t == null) {
                return;
            }
            SpanBuilder spanBuilder = tracer.spanBuilder(name)
                    .setSpanKind(SpanKind.CLIENT);
            spanBuilder.addLink(context);
            Span span = spanBuilder.startSpan();
            try {
                span.setStatus(StatusCode.ERROR, t.getMessage());
                delegate.accept(t);
            } finally {
                span.end();
            }
        }
    }
}
