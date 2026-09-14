package org.a2aproject.sdk.compat03.client.adapter;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.a2aproject.sdk.client.transport.spi.ClientTransport;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallContext;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallInterceptor;
import org.a2aproject.sdk.compat03.client.transport.spi.ClientTransport_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.EventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.A2AClientException;
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
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.jspecify.annotations.Nullable;

/** Common public 1.0 transport facade over a legacy 0.3 transport. */
public abstract class Compat03ClientTransportBase implements ClientTransport {
    protected final ClientTransport_v0_3 delegate;
    protected final AgentCard agentCard;
    protected final List<ClientCallInterceptor> interceptors;
    private final AtomicBoolean closed = new AtomicBoolean();

    protected Compat03ClientTransportBase(ClientTransport_v0_3 delegate, AgentCard agentCard,
            List<ClientCallInterceptor> interceptors) {
        this.delegate = delegate;
        this.agentCard = agentCard;
        this.interceptors = List.copyOf(interceptors);
    }

    @Override
    public EventKind sendMessage(MessageSendParams request, @Nullable ClientCallContext context) {
        Compat03ClientTransportSupport.validateMessageSend(request);
        return Compat03ClientTransportSupport.call(() ->
                Compat03ClientTransportSupport.toV10(delegate.sendMessage(
                        Compat03ClientTransportSupport.toV03(request), Compat03ClientTransportSupport.toV03Context(context))));
    }

    @Override
    public void sendMessageStreaming(MessageSendParams request, Consumer<StreamingEventKind> events,
            Consumer<Throwable> errors, @Nullable ClientCallContext context) {
        Compat03ClientTransportSupport.validateMessageSend(request);
        Compat03ClientTransportSupport.run(() -> delegate.sendMessageStreaming(
                Compat03ClientTransportSupport.toV03(request),
                event -> events.accept(Compat03ClientTransportSupport.toV10(event)), errors,
                Compat03ClientTransportSupport.toV03Context(context)));
    }

    @Override
    public Task getTask(TaskQueryParams request, @Nullable ClientCallContext context) {
        Compat03ClientTransportSupport.validateTaskQuery(request);
        return Compat03ClientTransportSupport.call(() ->
                org.a2aproject.sdk.compat03.conversion.mappers.domain.TaskMapper_v0_3.INSTANCE.toV10(delegate.getTask(
                        Compat03ClientTransportSupport.toV03(request), Compat03ClientTransportSupport.toV03Context(context))));
    }

    @Override
    public Task cancelTask(CancelTaskParams request, @Nullable ClientCallContext context) {
        Compat03ClientTransportSupport.validateCancel(request);
        return Compat03ClientTransportSupport.call(() ->
                org.a2aproject.sdk.compat03.conversion.mappers.domain.TaskMapper_v0_3.INSTANCE.toV10(delegate.cancelTask(
                        Compat03ClientTransportSupport.toV03(request), Compat03ClientTransportSupport.toV03Context(context))));
    }

    @Override
    public ListTasksResult listTasks(ListTasksParams request, @Nullable ClientCallContext context) {
        Compat03ClientTransportSupport.validateListTasks(request);
        throw new A2AClientException("listTasks is not supported by A2A protocol 0.3");
    }

    @Override
    public TaskPushNotificationConfig createTaskPushNotificationConfiguration(TaskPushNotificationConfig request,
            @Nullable ClientCallContext context) {
        Compat03ClientTransportSupport.validatePushConfig(request);
        return Compat03ClientTransportSupport.call(() -> Compat03ClientTransportSupport.toV10(
                delegate.setTaskPushNotificationConfiguration(Compat03ClientTransportSupport.toV03(request),
                        Compat03ClientTransportSupport.toV03Context(context))));
    }

    @Override
    public TaskPushNotificationConfig getTaskPushNotificationConfiguration(GetTaskPushNotificationConfigParams request,
            @Nullable ClientCallContext context) {
        Compat03ClientTransportSupport.validateGetPush(request);
        return Compat03ClientTransportSupport.call(() -> Compat03ClientTransportSupport.toV10(
                delegate.getTaskPushNotificationConfiguration(Compat03ClientTransportSupport.toV03(request),
                        Compat03ClientTransportSupport.toV03Context(context))));
    }

    @Override
    public org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsResult listTaskPushNotificationConfigurations(
            ListTaskPushNotificationConfigsParams request, @Nullable ClientCallContext context) {
        Compat03ClientTransportSupport.validatePushList(request);
        return Compat03ClientTransportSupport.call(() -> Compat03ClientTransportSupport.toV10PushList(
                delegate.listTaskPushNotificationConfigurations(Compat03ClientTransportSupport.toV03(request),
                        Compat03ClientTransportSupport.toV03Context(context))));
    }

    @Override
    public void deleteTaskPushNotificationConfigurations(DeleteTaskPushNotificationConfigParams request,
            @Nullable ClientCallContext context) {
        Compat03ClientTransportSupport.validateDeletePush(request);
        Compat03ClientTransportSupport.run(() -> delegate.deleteTaskPushNotificationConfigurations(
                Compat03ClientTransportSupport.toV03(request), Compat03ClientTransportSupport.toV03Context(context)));
    }

    @Override
    public void subscribeToTask(TaskIdParams request, Consumer<StreamingEventKind> events,
            Consumer<Throwable> errors, @Nullable ClientCallContext context) {
        Compat03ClientTransportSupport.validateTenant("subscribeToTask", request.tenant());
        Compat03ClientTransportSupport.run(() -> delegate.resubscribe(
                Compat03ClientTransportSupport.toV03(request),
                event -> events.accept(Compat03ClientTransportSupport.toV10(event)), errors,
                Compat03ClientTransportSupport.toV03Context(context)));
    }

    @Override
    public AgentCard getExtendedAgentCard(GetExtendedAgentCardParams params, @Nullable ClientCallContext context) {
        Compat03ClientTransportSupport.validateExtendedAgentCard(params);
        throw new A2AClientException("getExtendedAgentCard is not supported by A2A protocol 0.3");
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            delegate.close();
        }
    }

    public static AgentCard_v0_3 legacyCard(AgentCard card) {
        return org.a2aproject.sdk.compat03.conversion.mappers.domain.AgentCardMapper_v0_3.INSTANCE.fromV10(card);
    }
}
