package org.a2aproject.sdk.extras.opentelemetry.client.propagation;

import org.a2aproject.sdk.client.transport.spi.ClientTransport;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallContext;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.spec.A2AClientException;
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
    public Task cancelTask(CancelTaskParams request, @Nullable ClientCallContext context) throws A2AClientException {
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
    public ListTaskPushNotificationConfigsResult listTaskPushNotificationConfigurations(ListTaskPushNotificationConfigsParams request,
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
    public AgentCard getExtendedAgentCard(GetExtendedAgentCardParams request, @Nullable ClientCallContext context) throws A2AClientException {
        return delegate.getExtendedAgentCard(request, propagateContext(context));
    }

    @Override
    public void close() {
        delegate.close();
    }
}
