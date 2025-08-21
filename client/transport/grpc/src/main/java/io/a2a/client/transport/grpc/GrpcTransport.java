package io.a2a.client.transport.grpc;

import static io.a2a.grpc.A2AServiceGrpc.A2AServiceBlockingV2Stub;
import static io.a2a.grpc.A2AServiceGrpc.A2AServiceStub;
import static io.a2a.grpc.utils.ProtoUtils.FromProto;
import static io.a2a.grpc.utils.ProtoUtils.ToProto;
import static io.a2a.util.Assert.checkNotNullParam;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.a2a.client.transport.spi.interceptors.ClientCallContext;
import io.a2a.client.transport.spi.ClientTransport;
import io.a2a.grpc.A2AServiceGrpc;
import io.a2a.grpc.CancelTaskRequest;
import io.a2a.grpc.CreateTaskPushNotificationConfigRequest;
import io.a2a.grpc.DeleteTaskPushNotificationConfigRequest;
import io.a2a.grpc.GetTaskPushNotificationConfigRequest;
import io.a2a.grpc.GetTaskRequest;
import io.a2a.grpc.ListTaskPushNotificationConfigRequest;
import io.a2a.grpc.SendMessageRequest;
import io.a2a.grpc.SendMessageResponse;
import io.a2a.grpc.StreamResponse;
import io.a2a.grpc.TaskSubscriptionRequest;

import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.EventKind;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import io.grpc.Channel;

import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

public class GrpcTransport implements ClientTransport {

    private final A2AServiceBlockingV2Stub blockingStub;
    private final A2AServiceStub asyncStub;
    private AgentCard agentCard;

    public GrpcTransport(Channel channel, AgentCard agentCard) {
        checkNotNullParam("channel", channel);
        this.asyncStub = A2AServiceGrpc.newStub(channel);
        this.blockingStub = A2AServiceGrpc.newBlockingV2Stub(channel);
        this.agentCard = agentCard;
    }

    @Override
    public EventKind sendMessage(MessageSendParams request, ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);

        SendMessageRequest sendMessageRequest = createGrpcSendMessageRequest(request, context);

        try {
            SendMessageResponse response = blockingStub.sendMessage(sendMessageRequest);
            if (response.hasMsg()) {
                return FromProto.message(response.getMsg());
            } else if (response.hasTask()) {
                return FromProto.task(response.getTask());
            } else {
                throw new A2AClientException("Server response did not contain a message or task");
            }
        } catch (StatusRuntimeException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to send message: ");
        }
    }

    @Override
    public void sendMessageStreaming(MessageSendParams request, Consumer<StreamingEventKind> eventConsumer,
                                     Consumer<Throwable> errorConsumer, ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        checkNotNullParam("eventConsumer", eventConsumer);
        SendMessageRequest grpcRequest = createGrpcSendMessageRequest(request, context);
        StreamObserver<StreamResponse> streamObserver = new EventStreamObserver(eventConsumer, errorConsumer);

        try {
            asyncStub.sendStreamingMessage(grpcRequest, streamObserver);
        } catch (StatusRuntimeException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to send streaming message request: ");
        }
    }

    @Override
    public Task getTask(TaskQueryParams request, ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);

        GetTaskRequest.Builder requestBuilder = GetTaskRequest.newBuilder();
        requestBuilder.setName("tasks/" + request.id());
        if (request.historyLength() != null) {
            requestBuilder.setHistoryLength(request.historyLength());
        }
        GetTaskRequest getTaskRequest = requestBuilder.build();

        try {
            return FromProto.task(blockingStub.getTask(getTaskRequest));
        } catch (StatusRuntimeException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to get task: ");
        }
    }

    @Override
    public Task cancelTask(TaskIdParams request, ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);

        CancelTaskRequest cancelTaskRequest = CancelTaskRequest.newBuilder()
                .setName("tasks/" + request.id())
                .build();

        try {
            return FromProto.task(blockingStub.cancelTask(cancelTaskRequest));
        } catch (StatusRuntimeException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to cancel task: ");
        }
    }

    @Override
    public TaskPushNotificationConfig setTaskPushNotificationConfiguration(TaskPushNotificationConfig request,
                                                                           ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);

        String configId = request.pushNotificationConfig().id();
        CreateTaskPushNotificationConfigRequest grpcRequest = CreateTaskPushNotificationConfigRequest.newBuilder()
                .setParent("tasks/" + request.taskId())
                .setConfig(ToProto.taskPushNotificationConfig(request))
                .setConfigId(configId != null ? configId : request.taskId())
                .build();

        try {
            return FromProto.taskPushNotificationConfig(blockingStub.createTaskPushNotificationConfig(grpcRequest));
        } catch (StatusRuntimeException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to create task push notification config: ");
        }
    }

    @Override
    public TaskPushNotificationConfig getTaskPushNotificationConfiguration(
            GetTaskPushNotificationConfigParams request,
            ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);

        GetTaskPushNotificationConfigRequest grpcRequest = GetTaskPushNotificationConfigRequest.newBuilder()
                .setName(getTaskPushNotificationConfigName(request))
                .build();

        try {
            return FromProto.taskPushNotificationConfig(blockingStub.getTaskPushNotificationConfig(grpcRequest));
        } catch (StatusRuntimeException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to get task push notification config: ");
        }
    }

    @Override
    public List<TaskPushNotificationConfig> listTaskPushNotificationConfigurations(
            ListTaskPushNotificationConfigParams request,
            ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);

        ListTaskPushNotificationConfigRequest grpcRequest = ListTaskPushNotificationConfigRequest.newBuilder()
                .setParent("tasks/" + request.id())
                .build();

        try {
            return blockingStub.listTaskPushNotificationConfig(grpcRequest).getConfigsList().stream()
                    .map(FromProto::taskPushNotificationConfig)
                    .collect(Collectors.toList());
        } catch (StatusRuntimeException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to list task push notification config: ");
        }
    }

    @Override
    public void deleteTaskPushNotificationConfigurations(DeleteTaskPushNotificationConfigParams request,
                                                         ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);

        DeleteTaskPushNotificationConfigRequest grpcRequest = DeleteTaskPushNotificationConfigRequest.newBuilder()
                .setName(getTaskPushNotificationConfigName(request.id(), request.pushNotificationConfigId()))
                .build();

        try {
            blockingStub.deleteTaskPushNotificationConfig(grpcRequest);
        } catch (StatusRuntimeException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to delete task push notification config: ");
        }
    }

    @Override
    public void resubscribe(TaskIdParams request, Consumer<StreamingEventKind> eventConsumer,
                            Consumer<Throwable> errorConsumer, ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        checkNotNullParam("eventConsumer", eventConsumer);

        TaskSubscriptionRequest grpcRequest = TaskSubscriptionRequest.newBuilder()
                .setName("tasks/" + request.id())
                .build();

        StreamObserver<StreamResponse> streamObserver = new EventStreamObserver(eventConsumer, errorConsumer);

        try {
            asyncStub.taskSubscription(grpcRequest, streamObserver);
        } catch (StatusRuntimeException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to resubscribe task push notification config: ");
        }
    }

    @Override
    public AgentCard getAgentCard(ClientCallContext context) throws A2AClientException {
        // TODO: Determine how to handle retrieving the authenticated extended agent card
        return agentCard;
    }

    @Override
    public void close() {
    }

    private SendMessageRequest createGrpcSendMessageRequest(MessageSendParams messageSendParams, ClientCallContext context) {
        SendMessageRequest.Builder builder = SendMessageRequest.newBuilder();
        builder.setRequest(ToProto.message(messageSendParams.message()));
        if (messageSendParams.configuration() != null) {
            builder.setConfiguration(ToProto.messageSendConfiguration(messageSendParams.configuration()));
        }
        if (messageSendParams.metadata() != null) {
            builder.setMetadata(ToProto.struct(messageSendParams.metadata()));
        }
        return builder.build();
    }

    private String getTaskPushNotificationConfigName(GetTaskPushNotificationConfigParams params) {
        return getTaskPushNotificationConfigName(params.id(), params.pushNotificationConfigId());
    }

    private String getTaskPushNotificationConfigName(String taskId, String pushNotificationConfigId) {
        StringBuilder name = new StringBuilder();
        name.append("tasks/");
        name.append(taskId);
        if (pushNotificationConfigId != null) {
            name.append("/pushNotificationConfigs/");
            name.append(pushNotificationConfigId);
        }
        //name.append("/pushNotificationConfigs/");
        // Use taskId as default config ID if none provided
        //name.append(pushNotificationConfigId != null ? pushNotificationConfigId : taskId);
        return name.toString();
    }

}