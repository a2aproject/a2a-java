package org.a2aproject.sdk.compat03.client.adapter.jsonrpc;

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
import org.a2aproject.sdk.compat03.client.transport.spi.ClientTransport_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.domain.TaskMapper_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.params.MessageSendParamsMapper_v0_3;
import org.a2aproject.sdk.compat03.spec.MessageSendParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskIdParams_v0_3;
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

/** 1.0 client facade backed by the legacy JSON-RPC transport. */
public class JSONRPCCompat03ClientTransport extends Compat03ClientTransportBase {
    public JSONRPCCompat03ClientTransport(ClientTransport_v0_3 delegate, AgentCard card,
            List<ClientCallInterceptor> interceptors) {
        super(delegate, card, interceptors);
    }

    @Override
    public EventKind sendMessage(MessageSendParams request, @Nullable ClientCallContext context) {
        MessageSendParams_v0_3 legacyRequest = Compat03ClientTransportSupport.toV03(request);
        org.a2aproject.sdk.grpc.SendMessageRequest original = ProtoUtils.ToProto.sendMessageRequest(request);
        PayloadAndHeaders payload = apply(A2AMethods.SEND_MESSAGE_METHOD,
                original, org.a2aproject.sdk.grpc.SendMessageRequest.class, context);
        return Compat03ClientTransportSupport.call(() -> Compat03ClientTransportSupport.toV10(delegate.sendMessage(
                adaptMessage(legacyRequest, original,
                        (org.a2aproject.sdk.grpc.SendMessageRequest) payload.getPayload()),
                Compat03ClientCallContextMapper.toV03(contextWithHeaders(context, payload)))));
    }

    @Override
    public void sendMessageStreaming(MessageSendParams request, Consumer<StreamingEventKind> events,
            Consumer<Throwable> errors, @Nullable ClientCallContext context) {
        MessageSendParams_v0_3 legacyRequest = Compat03ClientTransportSupport.toV03(request);
        org.a2aproject.sdk.grpc.SendMessageRequest original = ProtoUtils.ToProto.sendMessageRequest(request);
        PayloadAndHeaders payload = apply(A2AMethods.SEND_STREAMING_MESSAGE_METHOD,
                original, org.a2aproject.sdk.grpc.SendMessageRequest.class, context);
        Compat03ClientTransportSupport.run(() -> delegate.sendMessageStreaming(
                adaptMessage(legacyRequest, original,
                        (org.a2aproject.sdk.grpc.SendMessageRequest) payload.getPayload()),
                event -> events.accept(Compat03ClientTransportSupport.toV10(event)),
                Compat03ClientTransportSupport.mapAsyncError(errors),
                Compat03ClientCallContextMapper.toV03(contextWithHeaders(context, payload))));
    }

    @Override
    public Task getTask(TaskQueryParams request, @Nullable ClientCallContext context) {
        Compat03ClientTransportSupport.validateTaskQuery(request);
        PayloadAndHeaders payload = apply(A2AMethods.GET_TASK_METHOD, ProtoUtils.ToProto.getTaskRequest(request),
                org.a2aproject.sdk.grpc.GetTaskRequest.class, context);
        return Compat03ClientTransportSupport.call(() -> TaskMapper_v0_3.INSTANCE.toV10(delegate.getTask(
                Compat03ClientTransportSupport.toV03(ProtoUtils.FromProto.taskQueryParams(
                        (org.a2aproject.sdk.grpc.GetTaskRequest) payload.getPayload())),
                Compat03ClientCallContextMapper.toV03(contextWithHeaders(context, payload)))));
    }

    @Override
    public Task cancelTask(CancelTaskParams request, @Nullable ClientCallContext context) {
        Compat03ClientTransportSupport.validateCancel(request);
        org.a2aproject.sdk.grpc.CancelTaskRequest original = ProtoUtils.ToProto.cancelTaskRequest(request);
        PayloadAndHeaders payload = apply(A2AMethods.CANCEL_TASK_METHOD, original,
                org.a2aproject.sdk.grpc.CancelTaskRequest.class, context);
        return Compat03ClientTransportSupport.call(() -> TaskMapper_v0_3.INSTANCE.toV10(delegate.cancelTask(
                adaptCancel(request, original, (org.a2aproject.sdk.grpc.CancelTaskRequest) payload.getPayload()),
                Compat03ClientCallContextMapper.toV03(contextWithHeaders(context, payload)))));
    }

    @Override
    public TaskPushNotificationConfig createTaskPushNotificationConfiguration(TaskPushNotificationConfig request,
            @Nullable ClientCallContext context) {
        Compat03ClientTransportSupport.validatePushConfig(request);
        PayloadAndHeaders payload = apply(A2AMethods.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD,
                ProtoUtils.ToProto.taskPushNotificationConfig(request), org.a2aproject.sdk.grpc.TaskPushNotificationConfig.class, context);
        return Compat03ClientTransportSupport.call(() -> Compat03ClientTransportSupport.toV10(
                delegate.setTaskPushNotificationConfiguration(Compat03ClientTransportSupport.toV03(
                        ProtoUtils.FromProto.taskPushNotificationConfig((org.a2aproject.sdk.grpc.TaskPushNotificationConfig) payload.getPayload())),
                        Compat03ClientCallContextMapper.toV03(contextWithHeaders(context, payload)))));
    }

    @Override
    public TaskPushNotificationConfig getTaskPushNotificationConfiguration(GetTaskPushNotificationConfigParams request,
            @Nullable ClientCallContext context) {
        Compat03ClientTransportSupport.validateGetPush(request);
        PayloadAndHeaders payload = apply(A2AMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD,
                ProtoUtils.ToProto.getTaskPushNotificationConfigRequest(request), org.a2aproject.sdk.grpc.GetTaskPushNotificationConfigRequest.class, context);
        return Compat03ClientTransportSupport.call(() -> Compat03ClientTransportSupport.toV10(
                delegate.getTaskPushNotificationConfiguration(Compat03ClientTransportSupport.toV03(
                        ProtoUtils.FromProto.getTaskPushNotificationConfigParams((org.a2aproject.sdk.grpc.GetTaskPushNotificationConfigRequest) payload.getPayload())),
                        Compat03ClientCallContextMapper.toV03(contextWithHeaders(context, payload)))));
    }

    @Override
    public ListTaskPushNotificationConfigsResult listTaskPushNotificationConfigurations(
            ListTaskPushNotificationConfigsParams request, @Nullable ClientCallContext context) {
        Compat03ClientTransportSupport.validatePushList(request);
        PayloadAndHeaders payload = apply(A2AMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD,
                ProtoUtils.ToProto.listTaskPushNotificationConfigsRequest(request), org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsRequest.class, context);
        return Compat03ClientTransportSupport.call(() -> Compat03ClientTransportSupport.toV10PushList(
                delegate.listTaskPushNotificationConfigurations(Compat03ClientTransportSupport.toV03(
                        ProtoUtils.FromProto.listTaskPushNotificationConfigsParams((org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsRequest) payload.getPayload())),
                        Compat03ClientCallContextMapper.toV03(contextWithHeaders(context, payload)))));
    }

    @Override
    public void deleteTaskPushNotificationConfigurations(DeleteTaskPushNotificationConfigParams request,
            @Nullable ClientCallContext context) {
        Compat03ClientTransportSupport.validateDeletePush(request);
        PayloadAndHeaders payload = apply(A2AMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD,
                ProtoUtils.ToProto.deleteTaskPushNotificationConfigRequest(request), org.a2aproject.sdk.grpc.DeleteTaskPushNotificationConfigRequest.class, context);
        Compat03ClientTransportSupport.run(() -> delegate.deleteTaskPushNotificationConfigurations(
                Compat03ClientTransportSupport.toV03(ProtoUtils.FromProto.deleteTaskPushNotificationConfigParams(
                        (org.a2aproject.sdk.grpc.DeleteTaskPushNotificationConfigRequest) payload.getPayload())),
                Compat03ClientCallContextMapper.toV03(contextWithHeaders(context, payload))));
    }

    @Override
    public void subscribeToTask(TaskIdParams request, Consumer<StreamingEventKind> events,
            Consumer<Throwable> errors, @Nullable ClientCallContext context) {
        Compat03ClientTransportSupport.validateTenant("subscribeToTask", request.tenant());
        PayloadAndHeaders payload = apply(A2AMethods.SUBSCRIBE_TO_TASK_METHOD,
                ProtoUtils.ToProto.subscribeToTaskRequest(request), org.a2aproject.sdk.grpc.SubscribeToTaskRequest.class, context);
        Compat03ClientTransportSupport.run(() -> delegate.resubscribe(
                Compat03ClientTransportSupport.toV03(ProtoUtils.FromProto.taskIdParams(
                        (org.a2aproject.sdk.grpc.SubscribeToTaskRequest) payload.getPayload())),
                event -> events.accept(Compat03ClientTransportSupport.toV10(event)),
                Compat03ClientTransportSupport.mapAsyncError(errors),
                Compat03ClientCallContextMapper.toV03(contextWithHeaders(context, payload))));
    }

    private static MessageSendParams_v0_3 adaptMessage(MessageSendParams_v0_3 legacyRequest,
            org.a2aproject.sdk.grpc.SendMessageRequest original, org.a2aproject.sdk.grpc.SendMessageRequest modified) {
        if (original.equals(modified)) {
            return legacyRequest;
        }
        return JSONRPCCompat03PayloadSupport.preserveUnchangedValues(legacyRequest,
                MessageSendParamsMapper_v0_3.INSTANCE.fromV10(ProtoUtils.FromProto.messageSendParams(original)),
                Compat03ClientTransportSupport.toV03(ProtoUtils.FromProto.messageSendParams(modified)));
    }

    private static TaskIdParams_v0_3 adaptCancel(CancelTaskParams request,
            org.a2aproject.sdk.grpc.CancelTaskRequest original, org.a2aproject.sdk.grpc.CancelTaskRequest modified) {
        TaskIdParams_v0_3 legacy = Compat03ClientTransportSupport.toV03(request);
        if (original.equals(modified)) {
            return legacy;
        }
        return JSONRPCCompat03PayloadSupport.preserveUnchangedValues(legacy,
                Compat03ClientTransportSupport.toV03(ProtoUtils.FromProto.cancelTaskParams(original)),
                Compat03ClientTransportSupport.toV03(ProtoUtils.FromProto.cancelTaskParams(modified)));
    }

    private PayloadAndHeaders apply(String method, Object payload, Class<?> expected,
            @Nullable ClientCallContext context) {
        return Compat03InterceptorSupport.apply(interceptors, method, payload, agentCard, context, expected);
    }

    private static ClientCallContext contextWithHeaders(@Nullable ClientCallContext original, PayloadAndHeaders payload) {
        return new ClientCallContext(original == null ? Map.of() : original.getState(), payload.getHeaders());
    }
}
