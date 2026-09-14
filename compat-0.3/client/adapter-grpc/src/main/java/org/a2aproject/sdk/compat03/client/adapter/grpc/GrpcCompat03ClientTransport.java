package org.a2aproject.sdk.compat03.client.adapter.grpc;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallContext;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallInterceptor;
import org.a2aproject.sdk.client.transport.spi.interceptors.PayloadAndHeaders;
import org.a2aproject.sdk.compat03.client.adapter.Compat03ClientCallContextMapper;
import org.a2aproject.sdk.compat03.client.adapter.Compat03ClientTransportBase;
import org.a2aproject.sdk.compat03.client.adapter.Compat03ClientTransportSupport;
import org.a2aproject.sdk.compat03.client.adapter.Compat03InterceptorSupport;
import org.a2aproject.sdk.compat03.client.transport.grpc.GrpcTransport_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.domain.TaskMapper_v0_3;
import org.a2aproject.sdk.grpc.utils.ProtoUtils;
import org.a2aproject.sdk.spec.A2AMethods;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.CancelTaskParams;
import org.a2aproject.sdk.spec.DeleteTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.EventKind;
import org.a2aproject.sdk.spec.GetTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsParams;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsResult;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.StreamingEventKind;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskIdParams;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.jspecify.annotations.Nullable;

/** 1.0 client facade backed by the legacy gRPC transport. */
public final class GrpcCompat03ClientTransport extends Compat03ClientTransportBase {
    public GrpcCompat03ClientTransport(GrpcTransport_v0_3 delegate, AgentCard card,
            List<ClientCallInterceptor> interceptors) {
        super(delegate, card, interceptors);
    }

    @Override
    public EventKind sendMessage(MessageSendParams request, @Nullable ClientCallContext context) {
        Compat03ClientTransportSupport.validateMessageSend(request);
        PayloadAndHeaders payload = apply(A2AMethods.SEND_MESSAGE_METHOD,
                ProtoUtils.ToProto.sendMessageRequest(request), org.a2aproject.sdk.grpc.SendMessageRequest.class, context);
        return Compat03ClientTransportSupport.call(() -> Compat03ClientTransportSupport.toV10(delegate.sendMessage(
                Compat03ClientTransportSupport.toV03(ProtoUtils.FromProto.messageSendParams(payloadPayload(payload))),
                legacyContext(context, payload))));
    }

    @Override
    public void sendMessageStreaming(MessageSendParams request, Consumer<StreamingEventKind> events,
            Consumer<Throwable> errors, @Nullable ClientCallContext context) {
        Compat03ClientTransportSupport.validateMessageSend(request);
        PayloadAndHeaders payload = apply(A2AMethods.SEND_STREAMING_MESSAGE_METHOD,
                ProtoUtils.ToProto.sendMessageRequest(request), org.a2aproject.sdk.grpc.SendMessageRequest.class, context);
        Compat03ClientTransportSupport.run(() -> delegate.sendMessageStreaming(
                Compat03ClientTransportSupport.toV03(ProtoUtils.FromProto.messageSendParams(payloadPayload(payload))),
                event -> events.accept(Compat03ClientTransportSupport.toV10(event)), errors, legacyContext(context, payload)));
    }

    @Override
    public Task getTask(TaskQueryParams request, @Nullable ClientCallContext context) {
        Compat03ClientTransportSupport.validateTaskQuery(request);
        PayloadAndHeaders payload = apply(A2AMethods.GET_TASK_METHOD, ProtoUtils.ToProto.getTaskRequest(request),
                org.a2aproject.sdk.grpc.GetTaskRequest.class, context);
        return Compat03ClientTransportSupport.call(() -> TaskMapper_v0_3.INSTANCE.toV10(delegate.getTask(
                Compat03ClientTransportSupport.toV03(ProtoUtils.FromProto.taskQueryParams(
                        taskQueryPayload(payload))), legacyContext(context, payload))));
    }

    @Override
    public Task cancelTask(CancelTaskParams request, @Nullable ClientCallContext context) {
        Compat03ClientTransportSupport.validateCancel(request);
        PayloadAndHeaders payload = apply(A2AMethods.CANCEL_TASK_METHOD, ProtoUtils.ToProto.cancelTaskRequest(request),
                org.a2aproject.sdk.grpc.CancelTaskRequest.class, context);
        return Compat03ClientTransportSupport.call(() -> TaskMapper_v0_3.INSTANCE.toV10(delegate.cancelTask(
                Compat03ClientTransportSupport.toV03(ProtoUtils.FromProto.cancelTaskParams(cancelPayload(payload))),
                legacyContext(context, payload))));
    }

    @Override
    public TaskPushNotificationConfig createTaskPushNotificationConfiguration(TaskPushNotificationConfig request,
            @Nullable ClientCallContext context) {
        Compat03ClientTransportSupport.validatePushConfig(request);
        PayloadAndHeaders payload = apply(A2AMethods.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD,
                ProtoUtils.ToProto.taskPushNotificationConfig(request), org.a2aproject.sdk.grpc.TaskPushNotificationConfig.class, context);
        return Compat03ClientTransportSupport.call(() -> Compat03ClientTransportSupport.toV10(
                delegate.setTaskPushNotificationConfiguration(Compat03ClientTransportSupport.toV03(
                        ProtoUtils.FromProto.taskPushNotificationConfig(pushConfigPayload(payload))),
                        legacyContext(context, payload))));
    }

    @Override
    public TaskPushNotificationConfig getTaskPushNotificationConfiguration(GetTaskPushNotificationConfigParams request,
            @Nullable ClientCallContext context) {
        Compat03ClientTransportSupport.validateGetPush(request);
        PayloadAndHeaders payload = apply(A2AMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD,
                ProtoUtils.ToProto.getTaskPushNotificationConfigRequest(request),
                org.a2aproject.sdk.grpc.GetTaskPushNotificationConfigRequest.class, context);
        return Compat03ClientTransportSupport.call(() -> Compat03ClientTransportSupport.toV10(
                delegate.getTaskPushNotificationConfiguration(Compat03ClientTransportSupport.toV03(
                        ProtoUtils.FromProto.getTaskPushNotificationConfigParams(getPushPayload(payload))),
                        legacyContext(context, payload))));
    }

    @Override
    public ListTaskPushNotificationConfigsResult listTaskPushNotificationConfigurations(
            ListTaskPushNotificationConfigsParams request, @Nullable ClientCallContext context) {
        Compat03ClientTransportSupport.validatePushList(request);
        PayloadAndHeaders payload = apply(A2AMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD,
                ProtoUtils.ToProto.listTaskPushNotificationConfigsRequest(request),
                org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsRequest.class, context);
        return Compat03ClientTransportSupport.call(() -> Compat03ClientTransportSupport.toV10PushList(
                delegate.listTaskPushNotificationConfigurations(Compat03ClientTransportSupport.toV03(
                        ProtoUtils.FromProto.listTaskPushNotificationConfigsParams(listPayload(payload))),
                        legacyContext(context, payload))));
    }

    @Override
    public void deleteTaskPushNotificationConfigurations(DeleteTaskPushNotificationConfigParams request,
            @Nullable ClientCallContext context) {
        Compat03ClientTransportSupport.validateDeletePush(request);
        PayloadAndHeaders payload = apply(A2AMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD,
                ProtoUtils.ToProto.deleteTaskPushNotificationConfigRequest(request),
                org.a2aproject.sdk.grpc.DeleteTaskPushNotificationConfigRequest.class, context);
        Compat03ClientTransportSupport.run(() -> delegate.deleteTaskPushNotificationConfigurations(
                Compat03ClientTransportSupport.toV03(ProtoUtils.FromProto.deleteTaskPushNotificationConfigParams(
                        deletePayload(payload))), legacyContext(context, payload)));
    }

    @Override
    public void subscribeToTask(TaskIdParams request, Consumer<StreamingEventKind> events,
            Consumer<Throwable> errors, @Nullable ClientCallContext context) {
        Compat03ClientTransportSupport.validateTenant("subscribeToTask", request.tenant());
        PayloadAndHeaders payload = apply(A2AMethods.SUBSCRIBE_TO_TASK_METHOD,
                ProtoUtils.ToProto.subscribeToTaskRequest(request), org.a2aproject.sdk.grpc.SubscribeToTaskRequest.class, context);
        Compat03ClientTransportSupport.run(() -> delegate.resubscribe(
                Compat03ClientTransportSupport.toV03(ProtoUtils.FromProto.taskIdParams(subscribePayload(payload))),
                event -> events.accept(Compat03ClientTransportSupport.toV10(event)), errors, legacyContext(context, payload)));
    }

    private PayloadAndHeaders apply(String method, Object payload, Class<?> expected,
            @Nullable ClientCallContext context) {
        return Compat03InterceptorSupport.apply(interceptors, method, payload, agentCard, context, expected);
    }

    private static org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallContext_v0_3 legacyContext(
            @Nullable ClientCallContext original, PayloadAndHeaders payload) {
        return Compat03ClientCallContextMapper.toV03(
                new ClientCallContext(original == null ? Map.of() : original.getState(), payload.getHeaders()));
    }

    private static org.a2aproject.sdk.grpc.SendMessageRequest payloadPayload(PayloadAndHeaders payload) {
        return (org.a2aproject.sdk.grpc.SendMessageRequest) payload.getPayload();
    }

    private static org.a2aproject.sdk.grpc.GetTaskRequest taskQueryPayload(PayloadAndHeaders payload) {
        return (org.a2aproject.sdk.grpc.GetTaskRequest) payload.getPayload();
    }

    private static org.a2aproject.sdk.grpc.CancelTaskRequest cancelPayload(PayloadAndHeaders payload) {
        return (org.a2aproject.sdk.grpc.CancelTaskRequest) payload.getPayload();
    }

    private static org.a2aproject.sdk.grpc.TaskPushNotificationConfig pushConfigPayload(PayloadAndHeaders payload) {
        return (org.a2aproject.sdk.grpc.TaskPushNotificationConfig) payload.getPayload();
    }

    private static org.a2aproject.sdk.grpc.GetTaskPushNotificationConfigRequest getPushPayload(PayloadAndHeaders payload) {
        return (org.a2aproject.sdk.grpc.GetTaskPushNotificationConfigRequest) payload.getPayload();
    }

    private static org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsRequest listPayload(PayloadAndHeaders payload) {
        return (org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsRequest) payload.getPayload();
    }

    private static org.a2aproject.sdk.grpc.DeleteTaskPushNotificationConfigRequest deletePayload(PayloadAndHeaders payload) {
        return (org.a2aproject.sdk.grpc.DeleteTaskPushNotificationConfigRequest) payload.getPayload();
    }

    private static org.a2aproject.sdk.grpc.SubscribeToTaskRequest subscribePayload(PayloadAndHeaders payload) {
        return (org.a2aproject.sdk.grpc.SubscribeToTaskRequest) payload.getPayload();
    }
}
