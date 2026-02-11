package io.a2a.extras.opentelemetry.client;

import static io.a2a.extras.opentelemetry.A2AObservabilityNames.EXTRACT_REQUEST_SYS_PROPERTY;
import static io.a2a.extras.opentelemetry.A2AObservabilityNames.EXTRACT_RESPONSE_SYS_PROPERTY;
import static io.a2a.extras.opentelemetry.A2AObservabilityNames.GENAI_CONFIG_ID;
import static io.a2a.extras.opentelemetry.A2AObservabilityNames.GENAI_CONTEXT_ID;
import static io.a2a.extras.opentelemetry.A2AObservabilityNames.GENAI_EXTENSIONS;
import static io.a2a.extras.opentelemetry.A2AObservabilityNames.GENAI_MESSAGE_ID;
import static io.a2a.extras.opentelemetry.A2AObservabilityNames.GENAI_OPERATION_NAME;
import static io.a2a.extras.opentelemetry.A2AObservabilityNames.GENAI_PARTS_NUMBER;
import static io.a2a.extras.opentelemetry.A2AObservabilityNames.GENAI_REQUEST;
import static io.a2a.extras.opentelemetry.A2AObservabilityNames.GENAI_RESPONSE;
import static io.a2a.extras.opentelemetry.A2AObservabilityNames.GENAI_ROLE;
import static io.a2a.extras.opentelemetry.A2AObservabilityNames.GENAI_TASK_ID;

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
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

public class OpenTelemetryClientTransport implements ClientTransport {

    private final Tracer tracer;
    private final ClientTransport delegate;

    public OpenTelemetryClientTransport(ClientTransport delegate, Tracer tracer) {
        this.delegate = delegate;
        this.tracer = tracer;
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
                spanBuilder.setAttribute(GENAI_ROLE, request.message().role().asString());
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
        try (Scope scope = span.makeCurrent()) {
            EventKind result = delegate.sendMessage(request, clientContext);
            if (result != null && extractResponse()) {
                span.setAttribute(GENAI_RESPONSE, result.toString());
            }
            if (result != null) {
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

    @Override
    public void sendMessageStreaming(MessageSendParams request, Consumer<StreamingEventKind> eventConsumer,
            Consumer<Throwable> errorConsumer, @Nullable ClientCallContext context) throws A2AClientException {
        ClientCallContext clientContext = createContext(context);
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
                spanBuilder.setAttribute(GENAI_ROLE, request.message().role().asString());
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
        try (Scope scope = span.makeCurrent()) {
            delegate.sendMessageStreaming(
                    request,
                    new OpenTelemetryEventConsumer(A2AMethods.SEND_STREAMING_MESSAGE_METHOD + "-event", eventConsumer, tracer, span.getSpanContext()),
                    new OpenTelemetryErrorConsumer(A2AMethods.SEND_STREAMING_MESSAGE_METHOD + "-error", errorConsumer, tracer, span.getSpanContext()),
                    clientContext
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
    public Task getTask(TaskQueryParams request, @Nullable ClientCallContext context) throws A2AClientException {
        ClientCallContext clientContext = createContext(context);
        SpanBuilder spanBuilder = tracer.spanBuilder(A2AMethods.GET_TASK_METHOD).setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute(GENAI_OPERATION_NAME, A2AMethods.GET_TASK_METHOD);
        if (request.id() != null) {
            spanBuilder.setAttribute(GENAI_TASK_ID, request.id());
        }
        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, request.toString());
        }
        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            Task result = delegate.getTask(request, clientContext);
            if (result != null && extractResponse()) {
                span.setAttribute(GENAI_RESPONSE, result.toString());
            }
            if (result != null) {
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

    @Override
    public Task cancelTask(TaskIdParams request, @Nullable ClientCallContext context) throws A2AClientException {
        ClientCallContext clientContext = createContext(context);
        SpanBuilder spanBuilder = tracer.spanBuilder(A2AMethods.CANCEL_TASK_METHOD).setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute(GENAI_OPERATION_NAME, A2AMethods.CANCEL_TASK_METHOD);
        if (request.id() != null) {
            spanBuilder.setAttribute(GENAI_TASK_ID, request.id());
        }
        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, request.toString());
        }
        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            Task result = delegate.cancelTask(request, clientContext);
            if (result != null && extractResponse()) {
                span.setAttribute(GENAI_RESPONSE, result.toString());
            }
            if (result != null) {
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

    @Override
    public ListTasksResult listTasks(ListTasksParams request, @Nullable ClientCallContext context) throws A2AClientException {
        ClientCallContext clientContext = createContext(context);
        SpanBuilder spanBuilder = tracer.spanBuilder(A2AMethods.LIST_TASK_METHOD).setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute(GENAI_OPERATION_NAME, A2AMethods.LIST_TASK_METHOD);
        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, request.toString());
        }
        if (request.contextId() != null) {
            spanBuilder.setAttribute(GENAI_CONTEXT_ID, request.contextId());
        }
        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            ListTasksResult result = delegate.listTasks(request, clientContext);
            if (result != null && extractResponse()) {
                span.setAttribute(GENAI_RESPONSE, result.toString());
            }
            if (result != null) {
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

    @Override
    public TaskPushNotificationConfig createTaskPushNotificationConfiguration(TaskPushNotificationConfig request,
            @Nullable ClientCallContext context) throws A2AClientException {
        ClientCallContext clientContext = createContext(context);
        SpanBuilder spanBuilder = tracer.spanBuilder(A2AMethods.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD).setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute(GENAI_OPERATION_NAME, A2AMethods.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
        if (request.taskId() != null) {
            spanBuilder.setAttribute(GENAI_TASK_ID, request.taskId());
        }
        if (request.pushNotificationConfig() != null && request.pushNotificationConfig().id() != null) {
            spanBuilder.setAttribute(GENAI_CONFIG_ID, request.pushNotificationConfig().id());
        }
        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, request.toString());
        }
        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            TaskPushNotificationConfig result = delegate.createTaskPushNotificationConfiguration(request, clientContext);
            if (result != null && extractResponse()) {
                span.setAttribute(GENAI_RESPONSE, result.toString());
            }
            if (result != null) {
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

    @Override
    public TaskPushNotificationConfig getTaskPushNotificationConfiguration(GetTaskPushNotificationConfigParams request,
            @Nullable ClientCallContext context) throws A2AClientException {
        ClientCallContext clientContext = createContext(context);
        SpanBuilder spanBuilder = tracer.spanBuilder(A2AMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD).setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute(GENAI_OPERATION_NAME, A2AMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
        if (request.id() != null) {
            spanBuilder.setAttribute(GENAI_TASK_ID, request.id());
        }
        if (request.pushNotificationConfigId() != null) {
            spanBuilder.setAttribute(GENAI_CONFIG_ID, request.pushNotificationConfigId());
        }
        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, request.toString());
        }
        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            TaskPushNotificationConfig result = delegate.getTaskPushNotificationConfiguration(request, clientContext);
            if (result != null && extractResponse()) {
                span.setAttribute(GENAI_RESPONSE, result.toString());
            }
            if (result != null) {
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

    @Override
    public ListTaskPushNotificationConfigResult listTaskPushNotificationConfigurations(ListTaskPushNotificationConfigParams request,
            @Nullable ClientCallContext context) throws A2AClientException {
        ClientCallContext clientContext = createContext(context);
        SpanBuilder spanBuilder = tracer.spanBuilder(A2AMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD).setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute(GENAI_OPERATION_NAME, A2AMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, request.toString());
        }
        if (request.id() != null) {
            spanBuilder.setAttribute(GENAI_TASK_ID, request.id());
        }
        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            ListTaskPushNotificationConfigResult result = delegate.listTaskPushNotificationConfigurations(request, clientContext);
            if (result != null && extractResponse()) {
                String responseValue = result.configs().stream()
                        .map(TaskPushNotificationConfig::toString)
                        .collect(Collectors.joining(","));
                span.setAttribute(GENAI_RESPONSE, responseValue);
            }
            if (result != null) {
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

    @Override
    public void deleteTaskPushNotificationConfigurations(DeleteTaskPushNotificationConfigParams request,
            @Nullable ClientCallContext context) throws A2AClientException {
        ClientCallContext clientContext = createContext(context);
        SpanBuilder spanBuilder = tracer.spanBuilder(A2AMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD).setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute(GENAI_OPERATION_NAME, A2AMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, request.toString());
        }
        if (request.id() != null) {
            spanBuilder.setAttribute(GENAI_TASK_ID, request.id());
        }
        if (request.pushNotificationConfigId() != null) {
            spanBuilder.setAttribute(GENAI_CONFIG_ID, request.pushNotificationConfigId());
        }
        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            delegate.deleteTaskPushNotificationConfigurations(request, clientContext);
            span.setStatus(StatusCode.OK);
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
        }
    }

    @Override
    public void subscribeToTask(TaskIdParams request, Consumer<StreamingEventKind> eventConsumer,
            Consumer<Throwable> errorConsumer, @Nullable ClientCallContext context) throws A2AClientException {
        ClientCallContext clientContext = createContext(context);
        SpanBuilder spanBuilder = tracer.spanBuilder(A2AMethods.SUBSCRIBE_TO_TASK_METHOD).setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute(GENAI_OPERATION_NAME, A2AMethods.SUBSCRIBE_TO_TASK_METHOD);
        if (request.id() != null) {
            spanBuilder.setAttribute(GENAI_TASK_ID, request.id());
        }
        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, request.toString());
        }
        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            delegate.subscribeToTask(
                    request,
                    new OpenTelemetryEventConsumer(A2AMethods.SUBSCRIBE_TO_TASK_METHOD + "-event", eventConsumer, tracer, span.getSpanContext()),
                    new OpenTelemetryErrorConsumer(A2AMethods.SUBSCRIBE_TO_TASK_METHOD + "-error", errorConsumer, tracer, span.getSpanContext()),
                    clientContext
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
    public AgentCard getExtendedAgentCard(@Nullable ClientCallContext context) throws A2AClientException {
        ClientCallContext clientContext = createContext(context);
        SpanBuilder spanBuilder = tracer.spanBuilder(A2AMethods.GET_EXTENDED_AGENT_CARD_METHOD).setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute(GENAI_OPERATION_NAME, A2AMethods.GET_EXTENDED_AGENT_CARD_METHOD);
        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            AgentCard result = delegate.getExtendedAgentCard(clientContext);
            if (result != null && extractResponse()) {
                span.setAttribute(GENAI_RESPONSE, result.toString());
            }
            if (result != null) {
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
