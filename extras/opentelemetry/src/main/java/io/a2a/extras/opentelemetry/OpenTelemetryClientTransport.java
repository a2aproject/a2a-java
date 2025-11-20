package io.a2a.extras.opentelemetry;

import io.a2a.client.transport.spi.ClientTransport;
import io.a2a.client.transport.spi.interceptors.ClientCallContext;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.EventKind;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.ListTasksParams;
import io.a2a.spec.ListTasksResult;
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
import java.util.List;
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

    @Override
    public EventKind sendMessage(MessageSendParams request, @Nullable ClientCallContext context) throws A2AClientException {
        SpanBuilder spanBuilder = tracer.spanBuilder("sendMessage")
                .setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute("request", request.toString());
        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            EventKind ret = delegate.sendMessage(request, context);
            if (ret != null) {
                span.setAttribute("response", ret.toString());
                span.setStatus(StatusCode.OK);
            }
            return ret;
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
        }
    }

    @Override
    public void sendMessageStreaming(MessageSendParams request, Consumer<StreamingEventKind> eventConsumer, Consumer<Throwable> errorConsumer, @Nullable ClientCallContext context) throws A2AClientException {
        SpanBuilder spanBuilder = tracer.spanBuilder("sendMessageStreaming")
                .setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute("request", request.toString());
        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            delegate.sendMessageStreaming(request, new OpenTelemetryEventConsumer("sendMessageStreaming-event", eventConsumer, tracer, span.getSpanContext()),
                    new OpenTelemetryErrorConsumer("sendMessageStreaming-error", errorConsumer, tracer, span.getSpanContext()), context);
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
        SpanBuilder spanBuilder = tracer.spanBuilder("getTask")
                .setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute("request", request.toString());
        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            Task ret = delegate.getTask(request, context);
            if (ret != null) {
                span.setAttribute("response", ret.toString());
                span.setStatus(StatusCode.OK);
            }
            return ret;
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
        }
    }

    @Override
    public Task cancelTask(TaskIdParams request, @Nullable ClientCallContext context) throws A2AClientException {
        SpanBuilder spanBuilder = tracer.spanBuilder("cancelTask")
                .setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute("request", request.toString());
        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            Task ret = delegate.cancelTask(request, context);
            if (ret != null) {
                span.setAttribute("response", ret.toString());
                span.setStatus(StatusCode.OK);
            }
            return ret;
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
        }
    }

    @Override
    public ListTasksResult listTasks(ListTasksParams request, @Nullable ClientCallContext context) throws A2AClientException {
        SpanBuilder spanBuilder = tracer.spanBuilder("listTasks")
                .setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute("request", request.toString());
        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            ListTasksResult ret = delegate.listTasks(request, context);
            if (ret != null) {
                span.setAttribute("response", ret.toString());
                span.setStatus(StatusCode.OK);
            }
            return ret;
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
        }
    }

    @Override
    public TaskPushNotificationConfig setTaskPushNotificationConfiguration(TaskPushNotificationConfig request, @Nullable ClientCallContext context) throws A2AClientException {
        SpanBuilder spanBuilder = tracer.spanBuilder("setTaskPushNotificationConfiguration")
                .setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute("request", request.toString());
        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            TaskPushNotificationConfig ret = delegate.setTaskPushNotificationConfiguration(request, context);
            if (ret != null) {
                span.setAttribute("response", ret.toString());
                span.setStatus(StatusCode.OK);
            }
            return ret;
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
        }
    }

    @Override
    public TaskPushNotificationConfig getTaskPushNotificationConfiguration(GetTaskPushNotificationConfigParams request, @Nullable ClientCallContext context) throws A2AClientException {
        SpanBuilder spanBuilder = tracer.spanBuilder("getTaskPushNotificationConfiguration")
                .setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute("request", request.toString());
        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            TaskPushNotificationConfig ret = delegate.getTaskPushNotificationConfiguration(request, context);
            if (ret != null) {
                span.setAttribute("response", ret.toString());
                span.setStatus(StatusCode.OK);
            }
            return ret;
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
        }
    }

    @Override
    public List<TaskPushNotificationConfig> listTaskPushNotificationConfigurations(ListTaskPushNotificationConfigParams request, @Nullable ClientCallContext context) throws A2AClientException {
        SpanBuilder spanBuilder = tracer.spanBuilder("listTaskPushNotificationConfigurations")
                .setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute("request", request.toString());
        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            List<TaskPushNotificationConfig> ret = delegate.listTaskPushNotificationConfigurations(request, context);
            if (ret != null) {
                span.setAttribute("response", ret.stream().map(TaskPushNotificationConfig::toString).collect(Collectors.joining(",")));
                span.setStatus(StatusCode.OK);
            }
            return ret;
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
        }
    }

    @Override
    public void deleteTaskPushNotificationConfigurations(DeleteTaskPushNotificationConfigParams request, @Nullable ClientCallContext context) throws A2AClientException {
        SpanBuilder spanBuilder = tracer.spanBuilder("deleteTaskPushNotificationConfigurations")
                .setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute("request", request.toString());
        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            delegate.deleteTaskPushNotificationConfigurations(request, context);
            span.setStatus(StatusCode.OK);
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
        }
    }

    @Override
    public void resubscribe(TaskIdParams request, Consumer<StreamingEventKind> eventConsumer, Consumer<Throwable> errorConsumer, @Nullable ClientCallContext context) throws A2AClientException {
        SpanBuilder spanBuilder = tracer.spanBuilder("resubscribe")
                .setSpanKind(SpanKind.CLIENT);
        spanBuilder.setAttribute("request", request.toString());
        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            delegate.resubscribe(request, new OpenTelemetryEventConsumer("resubscribe-event", eventConsumer, tracer, span.getSpanContext()),
                    new OpenTelemetryErrorConsumer("resubscribe-error", errorConsumer, tracer, span.getSpanContext()), context);
            span.setStatus(StatusCode.OK);
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
        }
    }

    @Override
    public AgentCard getAgentCard(@Nullable ClientCallContext context) throws A2AClientException {
        SpanBuilder spanBuilder = tracer.spanBuilder("getAgentCard")
                .setSpanKind(SpanKind.CLIENT);
        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            AgentCard ret = delegate.getAgentCard(context);
            if (ret != null) {
                span.setAttribute("response", ret.toString());
                span.setStatus(StatusCode.OK);
            }
            return ret;
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
        }
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
            spanBuilder.setAttribute("StreamingEventKind", t.toString());
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
