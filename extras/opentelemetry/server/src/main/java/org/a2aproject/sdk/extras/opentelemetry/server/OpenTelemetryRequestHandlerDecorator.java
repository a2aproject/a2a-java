package org.a2aproject.sdk.extras.opentelemetry.server;

import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.ERROR_TYPE;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.EXTRACT_REQUEST_SYS_PROPERTY;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.EXTRACT_RESPONSE_SYS_PROPERTY;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_CONFIG_ID;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_CONTEXT_ID;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_EXTENSIONS;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_MESSAGE_ID;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_OPERATION_NAME;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_PARTS_NUMBER;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_PROTOCOL_VERSION;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_REQUEST;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_RESPONSE;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_ROLE;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_STREAMING_DURATION;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_STREAMING_EVENT;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_TASK_ID;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_TRANSPORT;
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
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import org.a2aproject.sdk.extras.opentelemetry.StreamingEventAttributes;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.TaskOperation;
import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.A2AMethods;
import org.a2aproject.sdk.spec.CancelTaskParams;
import org.a2aproject.sdk.spec.DeleteTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.EventKind;
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
import org.a2aproject.sdk.spec.TransportProtocol;
import org.jspecify.annotations.Nullable;

/**
 * OpenTelemetry CDI Decorator for {@link RequestHandler}.
 * <p>
 * This decorator adds distributed tracing to A2A server request handlers.
 * It creates spans for each request handler method invocation, capturing:
 * <ul>
 * <li>Request parameters as span attributes</li>
 * <li>Response data as span attributes</li>
 * <li>Errors and exceptions with proper status codes</li>
 * <li>Timing information for performance monitoring</li>
 * <li>Streaming operation duration via {@code gen_ai.agent.a2a.streaming.duration} histogram</li>
 * </ul>
 * <p>
 * To enable this decorator, add it to your beans.xml:
 * <pre>{@code
 * <decorators>
 *     <class>org.a2aproject.sdk.extras.opentelemetry.server.OpenTelemetryRequestHandlerDecorator</class>
 * </decorators>
 * }</pre>
 */
@Decorator
@Priority(100)
public abstract class OpenTelemetryRequestHandlerDecorator implements RequestHandler {

    @Inject
    @Delegate
    @Any
    private RequestHandler delegate;

    @Inject
    private Tracer tracer;

    @Inject
    private Instance<Meter> meterInstance;

    @Nullable
    private volatile DoubleHistogram streamingDurationHistogram;

    /**
     * Default constructor for CDI.
     */
    public OpenTelemetryRequestHandlerDecorator() {
    }

    /**
     * Constructor for testing without metrics.
     *
     * @param delegate the delegate request handler
     * @param tracer the tracer to use
     */
    public OpenTelemetryRequestHandlerDecorator(RequestHandler delegate, Tracer tracer) {
        this(delegate, tracer, null);
    }

    /**
     * Constructor for testing with metrics.
     *
     * @param delegate the delegate request handler
     * @param tracer the tracer to use
     * @param meter optional meter for streaming duration histogram; null disables metrics
     */
    public OpenTelemetryRequestHandlerDecorator(RequestHandler delegate, Tracer tracer, @Nullable Meter meter) {
        this.delegate = delegate;
        this.tracer = tracer;
        this.streamingDurationHistogram = buildStreamingDurationHistogram(meter);
    }

    @Nullable
    private DoubleHistogram streamingDurationHistogram() {
        if (streamingDurationHistogram == null && meterInstance != null && meterInstance.isResolvable()) {
            synchronized (this) {
                if (streamingDurationHistogram == null) {
                    streamingDurationHistogram = buildStreamingDurationHistogram(meterInstance.get());
                }
            }
        }
        return streamingDurationHistogram;
    }

    @Nullable
    private static DoubleHistogram buildStreamingDurationHistogram(@Nullable Meter meter) {
        if (meter == null) {
            return null;
        }
        return meter.histogramBuilder(GENAI_STREAMING_DURATION)
                .setUnit("s")
                .setDescription("Duration of A2A streaming operations from initiation to last event")
                .build();
    }

    @Override
    public Task onGetTask(TaskQueryParams params, ServerCallContext context) throws A2AError {
        var spanBuilder = tracer.spanBuilder(A2AMethods.GET_TASK_METHOD)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute(GENAI_OPERATION_NAME, A2AMethods.GET_TASK_METHOD);

        if (params.id() != null) {
            spanBuilder.setAttribute(GENAI_TASK_ID, params.id());
        }
        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, params.toString());
        }

        applyTransportAttributes(spanBuilder, context);
        Span span = spanBuilder.startSpan();

        try (Scope scope = span.makeCurrent()) {
            Task result = delegate.onGetTask(params, context);

            if (result != null && extractResponse()) {
                span.setAttribute(GENAI_RESPONSE, result.toString());
            }

            span.setStatus(StatusCode.OK);
            return result;
        } catch (A2AError error) {
            span.setAttribute(ERROR_TYPE, error.getMessage());
            span.setStatus(StatusCode.ERROR, error.getMessage());
            throw error;
        } finally {
            span.end();
        }
    }

    @Override
    public ListTasksResult onListTasks(ListTasksParams params, ServerCallContext context) throws A2AError {
        var spanBuilder = tracer.spanBuilder(A2AMethods.LIST_TASK_METHOD)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute(GENAI_OPERATION_NAME, A2AMethods.LIST_TASK_METHOD);

        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, params.toString());
        }
        if (params.contextId() != null) {
            spanBuilder.setAttribute(GENAI_CONTEXT_ID, params.contextId());
        }
        applyTransportAttributes(spanBuilder, context);
        Span span = spanBuilder.startSpan();

        try (Scope scope = span.makeCurrent()) {
            ListTasksResult result = delegate.onListTasks(params, context);

            if (result != null && extractResponse()) {
                span.setAttribute(GENAI_RESPONSE, result.toString());
            }

            span.setStatus(StatusCode.OK);
            return result;
        } catch (A2AError error) {
            span.setAttribute(ERROR_TYPE, error.getMessage());
            span.setStatus(StatusCode.ERROR, error.getMessage());
            throw error;
        } finally {
            span.end();
        }
    }

    @Override
    public Task onCancelTask(CancelTaskParams params, ServerCallContext context) throws A2AError {
        var spanBuilder = tracer.spanBuilder(A2AMethods.CANCEL_TASK_METHOD)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute(GENAI_OPERATION_NAME, A2AMethods.CANCEL_TASK_METHOD);

        if (params.id() != null) {
            spanBuilder.setAttribute(GENAI_TASK_ID, params.id());
        }
        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, params.toString());
        }

        applyTransportAttributes(spanBuilder, context);
        Span span = spanBuilder.startSpan();

        try (Scope scope = span.makeCurrent()) {
            Task result = delegate.onCancelTask(params, context);

            if (result != null && extractResponse()) {
                span.setAttribute(GENAI_RESPONSE, result.toString());
            }

            span.setStatus(StatusCode.OK);
            return result;
        } catch (A2AError error) {
            span.setAttribute(ERROR_TYPE, error.getMessage());
            span.setStatus(StatusCode.ERROR, error.getMessage());
            throw error;
        } finally {
            span.end();
        }
    }

    @Override
    public EventKind onMessageSend(MessageSendParams params, ServerCallContext context) throws A2AError {
        var spanBuilder = tracer.spanBuilder(A2AMethods.SEND_MESSAGE_METHOD)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute(GENAI_OPERATION_NAME, A2AMethods.SEND_MESSAGE_METHOD);

        if (params.message() != null) {
            if (params.message().taskId() != null) {
                spanBuilder.setAttribute(GENAI_TASK_ID, params.message().taskId());
            }
            if (params.message().contextId() != null) {
                spanBuilder.setAttribute(GENAI_CONTEXT_ID, params.message().contextId());
            }
            if (params.message().messageId() != null) {
                spanBuilder.setAttribute(GENAI_MESSAGE_ID, params.message().messageId());
            }
            if (params.message().role() != null) {
                spanBuilder.setAttribute(GENAI_ROLE, params.message().role().name());
            }
            if (params.message().extensions() != null && !params.message().extensions().isEmpty()) {
                spanBuilder.setAttribute(GENAI_EXTENSIONS, String.join(",", params.message().extensions()));
            }
            spanBuilder.setAttribute(GENAI_PARTS_NUMBER, params.message().parts().size());
        }
        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, params.toString());
        }

        applyTransportAttributes(spanBuilder, context);
        Span span = spanBuilder.startSpan();

        try (Scope scope = span.makeCurrent()) {
            EventKind result = delegate.onMessageSend(params, context);

            if (result != null && extractResponse()) {
                span.setAttribute(GENAI_RESPONSE, result.toString());
            }

            span.setStatus(StatusCode.OK);
            return result;
        } catch (A2AError error) {
            span.setAttribute(ERROR_TYPE, error.getMessage());
            span.setStatus(StatusCode.ERROR, error.getMessage());
            throw error;
        } finally {
            span.end();
        }
    }

    @Override
    public Flow.Publisher<StreamingEventKind> onMessageSendStream(MessageSendParams params, ServerCallContext context) throws A2AError {
        var spanBuilder = tracer.spanBuilder(A2AMethods.SEND_STREAMING_MESSAGE_METHOD)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute(GENAI_OPERATION_NAME, A2AMethods.SEND_STREAMING_MESSAGE_METHOD);

        if (params.message() != null) {
            if (params.message().taskId() != null) {
                spanBuilder.setAttribute(GENAI_TASK_ID, params.message().taskId());
            }
            if (params.message().contextId() != null) {
                spanBuilder.setAttribute(GENAI_CONTEXT_ID, params.message().contextId());
            }
            if (params.message().messageId() != null) {
                spanBuilder.setAttribute(GENAI_MESSAGE_ID, params.message().messageId());
            }
            if (params.message().role() != null) {
                spanBuilder.setAttribute(GENAI_ROLE, params.message().role().name());
            }
            if (params.message().extensions() != null && !params.message().extensions().isEmpty()) {
                spanBuilder.setAttribute(GENAI_EXTENSIONS, String.join(",", params.message().extensions()));
            }
            spanBuilder.setAttribute(GENAI_PARTS_NUMBER, params.message().parts().size());
        }
        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, params.toString());
        }

        applyTransportAttributes(spanBuilder, context);
        Span span = spanBuilder.startSpan();
        long startNanos = System.nanoTime();

        try (Scope scope = span.makeCurrent()) {
            Flow.Publisher<StreamingEventKind> result = delegate.onMessageSendStream(params, context);

            if (extractResponse()) {
                span.setAttribute(GENAI_RESPONSE, "Stream publisher created");
            }

            span.setStatus(StatusCode.OK);
            SpanContext spanContext = span.getSpanContext();
            return new OpenTelemetryStreamPublisher(result,
                    new StreamingSpanConfig(tracer, A2AMethods.SEND_STREAMING_MESSAGE_METHOD,
                            spanContext, streamingDurationHistogram(), startNanos));
        } catch (A2AError error) {
            StreamingEventAttributes.recordDuration(streamingDurationHistogram(),
                    startNanos, A2AMethods.SEND_STREAMING_MESSAGE_METHOD, false);
            span.setAttribute(ERROR_TYPE, error.getMessage());
            span.setStatus(StatusCode.ERROR, error.getMessage());
            throw error;
        } finally {
            span.end();
        }
    }

    @Override
    public TaskPushNotificationConfig onCreateTaskPushNotificationConfig(TaskPushNotificationConfig params, ServerCallContext context) throws A2AError {
        var spanBuilder = tracer.spanBuilder(A2AMethods.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute(GENAI_OPERATION_NAME, A2AMethods.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);

        if (params.taskId() != null) {
            spanBuilder.setAttribute(GENAI_TASK_ID, params.taskId());
        }
        if (params.id() != null) {
            spanBuilder.setAttribute(GENAI_CONFIG_ID, params.id());
        }
        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, params.toString());
        }

        applyTransportAttributes(spanBuilder, context);
        Span span = spanBuilder.startSpan();

        try (Scope scope = span.makeCurrent()) {
            TaskPushNotificationConfig result = delegate.onCreateTaskPushNotificationConfig(params, context);

            if (result != null && extractResponse()) {
                span.setAttribute(GENAI_RESPONSE, result.toString());
            }

            span.setStatus(StatusCode.OK);
            return result;
        } catch (A2AError error) {
            span.setAttribute(ERROR_TYPE, error.getMessage());
            span.setStatus(StatusCode.ERROR, error.getMessage());
            throw error;
        } finally {
            span.end();
        }
    }

    @Override
    public TaskPushNotificationConfig onGetTaskPushNotificationConfig(GetTaskPushNotificationConfigParams params, ServerCallContext context) throws A2AError {
        var spanBuilder = tracer.spanBuilder(A2AMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute(GENAI_OPERATION_NAME, A2AMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);

        if (params.taskId() != null) {
            spanBuilder.setAttribute(GENAI_TASK_ID, params.taskId());
        }
        if (params.id() != null) {
            spanBuilder.setAttribute(GENAI_CONFIG_ID, params.id());
        }
        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, params.toString());
        }

        applyTransportAttributes(spanBuilder, context);
        Span span = spanBuilder.startSpan();

        try (Scope scope = span.makeCurrent()) {
            TaskPushNotificationConfig result = delegate.onGetTaskPushNotificationConfig(params, context);

            if (result != null && extractResponse()) {
                span.setAttribute(GENAI_RESPONSE, result.toString());
            }

            span.setStatus(StatusCode.OK);
            return result;
        } catch (A2AError error) {
            span.setAttribute(ERROR_TYPE, error.getMessage());
            span.setStatus(StatusCode.ERROR, error.getMessage());
            throw error;
        } finally {
            span.end();
        }
    }

    @Override
    public Flow.Publisher<StreamingEventKind> onSubscribeToTask(TaskIdParams params, ServerCallContext context) throws A2AError {
        var spanBuilder = tracer.spanBuilder(A2AMethods.SUBSCRIBE_TO_TASK_METHOD)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute(GENAI_OPERATION_NAME, A2AMethods.SUBSCRIBE_TO_TASK_METHOD);

        if (params.id() != null) {
            spanBuilder.setAttribute(GENAI_TASK_ID, params.id());
        }
        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, params.toString());
        }

        applyTransportAttributes(spanBuilder, context);
        Span span = spanBuilder.startSpan();
        long startNanos = System.nanoTime();

        try (Scope scope = span.makeCurrent()) {
            Flow.Publisher<StreamingEventKind> result = delegate.onSubscribeToTask(params, context);

            if (extractResponse()) {
                span.setAttribute(GENAI_RESPONSE, "Stream publisher created");
            }

            span.setStatus(StatusCode.OK);
            SpanContext spanContext = span.getSpanContext();
            return new OpenTelemetryStreamPublisher(result,
                    new StreamingSpanConfig(tracer, A2AMethods.SUBSCRIBE_TO_TASK_METHOD,
                            spanContext, streamingDurationHistogram(), startNanos));
        } catch (A2AError error) {
            StreamingEventAttributes.recordDuration(streamingDurationHistogram(),
                    startNanos, A2AMethods.SUBSCRIBE_TO_TASK_METHOD, false);
            span.setAttribute(ERROR_TYPE, error.getMessage());
            span.setStatus(StatusCode.ERROR, error.getMessage());
            throw error;
        } finally {
            span.end();
        }
    }

    @Override
    public ListTaskPushNotificationConfigsResult onListTaskPushNotificationConfigs(ListTaskPushNotificationConfigsParams params, ServerCallContext context) throws A2AError {
        var spanBuilder = tracer.spanBuilder(A2AMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute(GENAI_OPERATION_NAME, A2AMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);

        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, params.toString());
        }
        if (params.id() != null) {
            spanBuilder.setAttribute(GENAI_TASK_ID, params.id());
        }

        applyTransportAttributes(spanBuilder, context);
        Span span = spanBuilder.startSpan();

        try (Scope scope = span.makeCurrent()) {
            ListTaskPushNotificationConfigsResult result = delegate.onListTaskPushNotificationConfigs(params, context);

            if (result != null && extractResponse()) {
                span.setAttribute(GENAI_RESPONSE, result.toString());
            }

            span.setStatus(StatusCode.OK);
            return result;
        } catch (A2AError error) {
            span.setAttribute(ERROR_TYPE, error.getMessage());
            span.setStatus(StatusCode.ERROR, error.getMessage());
            throw error;
        } finally {
            span.end();
        }
    }

    @Override
    public void onDeleteTaskPushNotificationConfig(DeleteTaskPushNotificationConfigParams params, ServerCallContext context) throws A2AError {
        var spanBuilder = tracer.spanBuilder(A2AMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute(GENAI_OPERATION_NAME, A2AMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);

        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, params.toString());
        }
        if (params.taskId() != null) {
            spanBuilder.setAttribute(GENAI_TASK_ID, params.taskId());
        }
        applyTransportAttributes(spanBuilder, context);
        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            delegate.onDeleteTaskPushNotificationConfig(params, context);
            span.setStatus(StatusCode.OK);
        } catch (A2AError error) {
            span.setAttribute(ERROR_TYPE, error.getMessage());
            span.setStatus(StatusCode.ERROR, error.getMessage());
            throw error;
        } finally {
            span.end();
        }
    }

    @Override
    public void authorizeTaskAccess(@Nullable String requestedTaskId, ServerCallContext context,
            TaskOperation operation) throws A2AError {
        delegate.authorizeTaskAccess(requestedTaskId, context, operation);
    }

    private boolean extractRequest() {
        return Boolean.getBoolean(EXTRACT_REQUEST_SYS_PROPERTY);
    }

    private boolean extractResponse() {
        return Boolean.getBoolean(EXTRACT_RESPONSE_SYS_PROPERTY);
    }

    private static void applyTransportAttributes(SpanBuilder spanBuilder, ServerCallContext context) {
        Map<String, Object> state = context.getState();
        Object transport = state.get(ServerCallContext.TRANSPORT_KEY);
        if (transport instanceof TransportProtocol tp) {
            spanBuilder.setAttribute(GENAI_TRANSPORT, tp.asString());
        }
        String version = context.getRequestedProtocolVersion();
        if (version != null) {
            spanBuilder.setAttribute(GENAI_PROTOCOL_VERSION, version);
        }
    }

    private record StreamingSpanConfig(Tracer tracer, String spanName, SpanContext parentSpanContext,
            @Nullable DoubleHistogram streamingDurationHistogram, long startNanos) {}

    private static class OpenTelemetryStreamPublisher implements Flow.Publisher<StreamingEventKind> {

        private final Flow.Publisher<StreamingEventKind> delegate;
        private final StreamingSpanConfig config;

        OpenTelemetryStreamPublisher(Flow.Publisher<StreamingEventKind> delegate, StreamingSpanConfig config) {
            this.delegate = delegate;
            this.config = config;
        }

        @Override
        public void subscribe(Flow.Subscriber<? super StreamingEventKind> subscriber) {
            delegate.subscribe(new OpenTelemetryStreamSubscriber(subscriber, config));
        }
    }

    private static class OpenTelemetryStreamSubscriber implements Flow.Subscriber<StreamingEventKind> {

        private static final Logger LOGGER = Logger.getLogger(OpenTelemetryStreamSubscriber.class.getName());

        private final Flow.Subscriber<? super StreamingEventKind> delegate;
        private final StreamingSpanConfig config;
        private final AtomicBoolean terminated = new AtomicBoolean(false);

        OpenTelemetryStreamSubscriber(Flow.Subscriber<? super StreamingEventKind> delegate, StreamingSpanConfig config) {
            this.delegate = delegate;
            this.config = config;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            delegate.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {
                    subscription.request(n);
                }

                @Override
                public void cancel() {
                    if (terminated.compareAndSet(false, true)) {
                        recordStreamingDuration(true);
                        Span closingSpan = config.tracer().spanBuilder(config.spanName() + STREAMING_END_SUFFIX)
                                .setSpanKind(SpanKind.SERVER)
                                .addLink(config.parentSpanContext())
                                .startSpan();
                        try (Scope ignored = closingSpan.makeCurrent()) {
                            closingSpan.setStatus(StatusCode.OK);
                        } finally {
                            closingSpan.end();
                        }
                    }
                    subscription.cancel();
                }
            });
        }

        @Override
        public void onNext(StreamingEventKind item) {
            SpanBuilder builder = config.tracer().spanBuilder(config.spanName() + STREAMING_EVENT_SUFFIX)
                    .setSpanKind(SpanKind.SERVER)
                    .addLink(config.parentSpanContext())
                    .setAttribute(GENAI_STREAMING_EVENT, item.kind());
            StreamingEventAttributes.apply(builder, item);
            Span span = builder.startSpan();
            try (Scope ignored = span.makeCurrent()) {
                delegate.onNext(item);
                span.setStatus(StatusCode.OK);
            } catch (Throwable t) {
                span.recordException(t);
                span.setStatus(StatusCode.ERROR);
                throw t;
            } finally {
                span.end();
            }
        }

        @Override
        public void onError(Throwable throwable) {
            if (!terminated.compareAndSet(false, true)) {
                LOGGER.warning("onError() received after stream termination (publisher protocol violation); ignoring: " + throwable);
                return;
            }
            recordStreamingDuration(false);
            Span closingSpan = config.tracer().spanBuilder(config.spanName() + STREAMING_END_SUFFIX)
                    .setSpanKind(SpanKind.SERVER)
                    .addLink(config.parentSpanContext())
                    .startSpan();
            try (Scope ignored = closingSpan.makeCurrent()) {
                closingSpan.setAttribute(ERROR_TYPE, throwable.getClass().getName());
                closingSpan.setStatus(StatusCode.ERROR, throwable.getMessage());
                delegate.onError(throwable);
            } catch (Throwable t) {
                closingSpan.recordException(t);
                throw t;
            } finally {
                closingSpan.end();
            }
        }

        @Override
        public void onComplete() {
            if (!terminated.compareAndSet(false, true)) {
                return;
            }
            recordStreamingDuration(true);
            Span closingSpan = config.tracer().spanBuilder(config.spanName() + STREAMING_END_SUFFIX)
                    .setSpanKind(SpanKind.SERVER)
                    .addLink(config.parentSpanContext())
                    .startSpan();
            try (Scope ignored = closingSpan.makeCurrent()) {
                delegate.onComplete();
                closingSpan.setStatus(StatusCode.OK);
            } catch (Throwable t) {
                closingSpan.recordException(t);
                closingSpan.setStatus(StatusCode.ERROR);
                throw t;
            } finally {
                closingSpan.end();
            }
        }

        private void recordStreamingDuration(boolean success) {
            StreamingEventAttributes.recordDuration(config.streamingDurationHistogram(),
                    config.startNanos(), config.spanName(), success);
        }
    }
}
