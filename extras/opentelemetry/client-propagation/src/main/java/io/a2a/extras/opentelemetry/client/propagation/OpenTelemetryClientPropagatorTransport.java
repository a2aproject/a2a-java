package io.a2a.extras.opentelemetry.client.propagation;

import io.a2a.client.transport.spi.ClientTransport;
import io.a2a.client.transport.spi.interceptors.ClientCallContext;
import io.a2a.jsonrpc.common.wrappers.ListTasksResult;
import io.a2a.spec.A2AClientException;
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
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

public class OpenTelemetryClientPropagatorTransport implements ClientTransport {

    private final OpenTelemetry openTelemetry;
    private final ClientTransport delegate;

    private static final TextMapSetter<Map<String, String>> MAP_SETTER = new TextMapSetter<Map<String, String>>() {
        @Override
        public void set(@Nullable Map<String, String> carrier, String key, String value) {
            if (carrier != null) {
                carrier.put(key, value);
            }
        }
    };

    public OpenTelemetryClientPropagatorTransport(ClientTransport delegate, OpenTelemetry openTelemetry) {
        this.delegate = delegate;
        this.openTelemetry = openTelemetry;
    }

    private ClientCallContext propagateContext(@Nullable ClientCallContext context) {
        ClientCallContext clientContext;
        if (context == null) {
            clientContext = new ClientCallContext(Map.of(), new HashMap<>());
        } else {
            clientContext =  new ClientCallContext(context.getState(), new HashMap<>(context.getHeaders()));
        }
        openTelemetry.getPropagators().getTextMapPropagator().inject(Context.current(), clientContext.getHeaders(), MAP_SETTER);
        return clientContext;
    }

    @Override
    public EventKind sendMessage(MessageSendParams request, @Nullable ClientCallContext context) throws A2AClientException {
        return delegate.sendMessage(request, propagateContext(context));
    }

    @Override
    public void sendMessageStreaming(MessageSendParams request, Consumer<StreamingEventKind> eventConsumer,
            Consumer<Throwable> errorConsumer, @Nullable ClientCallContext context) throws A2AClientException {
        delegate.sendMessageStreaming(request, eventConsumer, errorConsumer, propagateContext(context));
    }

    @Override
    public Task getTask(TaskQueryParams request, @Nullable ClientCallContext context) throws A2AClientException {
        return delegate.getTask(request, propagateContext(context));
    }

    @Override
    public Task cancelTask(TaskIdParams request, @Nullable ClientCallContext context) throws A2AClientException {
        return delegate.cancelTask(request, propagateContext(context));
    }

    @Override
    public ListTasksResult listTasks(ListTasksParams request, @Nullable ClientCallContext context) throws A2AClientException {
        return delegate.listTasks(request, propagateContext(context));
    }

    @Override
    public TaskPushNotificationConfig createTaskPushNotificationConfiguration(TaskPushNotificationConfig request,
            @Nullable ClientCallContext context) throws A2AClientException {
        return delegate.createTaskPushNotificationConfiguration(request, propagateContext(context));
    }

    @Override
    public TaskPushNotificationConfig getTaskPushNotificationConfiguration(GetTaskPushNotificationConfigParams request,
            @Nullable ClientCallContext context) throws A2AClientException {
        return delegate.getTaskPushNotificationConfiguration(request, propagateContext(context));
    }

    @Override
    public ListTaskPushNotificationConfigResult listTaskPushNotificationConfigurations(ListTaskPushNotificationConfigParams request,
            @Nullable ClientCallContext context) throws A2AClientException {
        return delegate.listTaskPushNotificationConfigurations(request, propagateContext(context));
    }

    @Override
    public void deleteTaskPushNotificationConfigurations(DeleteTaskPushNotificationConfigParams request,
            @Nullable ClientCallContext context) throws A2AClientException {
        delegate.deleteTaskPushNotificationConfigurations(request, propagateContext(context));
    }

    @Override
    public void subscribeToTask(TaskIdParams request, Consumer<StreamingEventKind> eventConsumer,
            Consumer<Throwable> errorConsumer, @Nullable ClientCallContext context) throws A2AClientException {
        delegate.subscribeToTask(request, eventConsumer, errorConsumer, propagateContext(context));
    }

    @Override
    public AgentCard getExtendedAgentCard(@Nullable ClientCallContext context) throws A2AClientException {
        return delegate.getExtendedAgentCard(propagateContext(context));
    }

    @Override
    public void close() {
        delegate.close();
    }
}
