package io.a2a.transport.grpc.client;

import io.a2a.grpc.*;
import io.a2a.grpc.SendMessageRequest;
import io.a2a.grpc.SendMessageResponse;
import io.a2a.grpc.utils.ProtoUtils;
import io.a2a.spec.*;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Task;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.transport.spi.client.Transport;
import io.grpc.Channel;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.function.Consumer;

import static io.a2a.util.Assert.checkNotNullParam;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GrpcTransport implements Transport {

    private A2AServiceGrpc.A2AServiceBlockingV2Stub blockingStub;
    private A2AServiceGrpc.A2AServiceStub asyncStub;
    private AgentCard agentCard;

    /**
     * Create an A2A client for interacting with an A2A agent via gRPC.
     *
     * @param channel the gRPC channel
     * @param agentCard the agent card for the A2A server this client will be communicating with
     */
    public GrpcTransport(Channel channel, AgentCard agentCard) {
        checkNotNullParam("channel", channel);
        checkNotNullParam("agentCard", agentCard);
        this.asyncStub = A2AServiceGrpc.newStub(channel);
        this.blockingStub = A2AServiceGrpc.newBlockingV2Stub(channel);
        this.agentCard = agentCard;
    }

    @Override
    public EventKind sendMessage(String requestId, MessageSendParams messageSendParams) throws A2AServerException {
        SendMessageRequest request = createGrpcSendMessageRequestFromMessageSendParams(messageSendParams);
        try {
            SendMessageResponse response = blockingStub.sendMessage(request);
            if (response.hasMsg()) {
                return ProtoUtils.FromProto.message(response.getMsg());
            } else if (response.hasTask()) {
                return ProtoUtils.FromProto.task(response.getTask());
            } else {
                throw new A2AServerException("Server response did not contain a message or task");
            }
        } catch (StatusRuntimeException e) {
            throw new A2AServerException("Failed to send message: " + e, e);
        }
    }

    @Override
    public Task getTask(String requestId, TaskQueryParams taskQueryParams) throws A2AServerException {
        io.a2a.grpc.GetTaskRequest.Builder requestBuilder = io.a2a.grpc.GetTaskRequest.newBuilder();
        requestBuilder.setName("tasks/" + taskQueryParams.id());
        if (taskQueryParams.historyLength() != null) {
            requestBuilder.setHistoryLength(taskQueryParams.historyLength());
        }
        io.a2a.grpc.GetTaskRequest getTaskRequest = requestBuilder.build();
        try {
            return ProtoUtils.FromProto.task(blockingStub.getTask(getTaskRequest));
        } catch (StatusRuntimeException e) {
            throw new A2AServerException("Failed to get task: " + e, e);
        }
    }

    @Override
    public Task cancelTask(String requestId, TaskIdParams taskIdParams) throws A2AServerException {
        io.a2a.grpc.CancelTaskRequest cancelTaskRequest = io.a2a.grpc.CancelTaskRequest.newBuilder()
                .setName("tasks/" + taskIdParams.id())
                .build();
        try {
            return ProtoUtils.FromProto.task(blockingStub.cancelTask(cancelTaskRequest));
        } catch (StatusRuntimeException e) {
            throw new A2AServerException("Failed to cancel task: " + e, e);
        }
    }

    @Override
    public TaskPushNotificationConfig getTaskPushNotificationConfig(String requestId, GetTaskPushNotificationConfigParams getTaskPushNotificationConfigParams) throws A2AServerException {
        io.a2a.grpc.GetTaskPushNotificationConfigRequest getTaskPushNotificationConfigRequest = io.a2a.grpc.GetTaskPushNotificationConfigRequest.newBuilder()
                .setName(getTaskPushNotificationConfigName(getTaskPushNotificationConfigParams))
                .build();
        try {
            return ProtoUtils.FromProto.taskPushNotificationConfig(blockingStub.getTaskPushNotificationConfig(getTaskPushNotificationConfigRequest));
        } catch (StatusRuntimeException e) {
            throw new A2AServerException("Failed to get the task push notification config: " + e, e);
        }
    }

    @Override
    public TaskPushNotificationConfig setTaskPushNotificationConfig(String requestId, String taskId, TaskPushNotificationConfig taskPushNotificationConfig) throws A2AServerException {
        String configId = taskPushNotificationConfig.pushNotificationConfig().id();
        CreateTaskPushNotificationConfigRequest request = CreateTaskPushNotificationConfigRequest.newBuilder()
                .setParent("tasks/" + taskPushNotificationConfig.taskId())
                .setConfig(ProtoUtils.ToProto.taskPushNotificationConfig(taskPushNotificationConfig))
                .setConfigId(configId == null ? "" : configId)
                .build();
        try {
            return ProtoUtils.FromProto.taskPushNotificationConfig(blockingStub.createTaskPushNotificationConfig(request));
        } catch (StatusRuntimeException e) {
            throw new A2AServerException("Failed to set the task push notification config: " + e, e);
        }
    }

    @Override
    public List<TaskPushNotificationConfig> listTaskPushNotificationConfig(String requestId, ListTaskPushNotificationConfigParams listTaskPushNotificationConfigParams) throws A2AServerException {
        return List.of();
    }

    @Override
    public void deleteTaskPushNotificationConfig(String requestId, DeleteTaskPushNotificationConfigParams deleteTaskPushNotificationConfigParams) throws A2AServerException {

    }

    @Override
    public void sendStreamingMessage(String requestId, MessageSendParams messageSendParams, Consumer<StreamingEventKind> eventHandler, Consumer<JSONRPCError> errorHandler, Runnable failureHandler) throws A2AServerException {
        SendMessageRequest request = createGrpcSendMessageRequestFromMessageSendParams(messageSendParams);
        StreamObserver<StreamResponse> streamObserver = new EventStreamObserver(eventHandler, errorHandler);
        try {
            asyncStub.sendStreamingMessage(request, streamObserver);
        } catch (StatusRuntimeException e) {
            throw new A2AServerException("Failed to send streaming message: " + e, e);
        }
    }

    @Override
    public void resubscribeToTask(String requestId, TaskIdParams taskIdParams, Consumer<StreamingEventKind> eventHandler, Consumer<JSONRPCError> errorHandler, Runnable failureHandler) throws A2AServerException {

    }

    private SendMessageRequest createGrpcSendMessageRequestFromMessageSendParams(MessageSendParams messageSendParams) {
        SendMessageRequest.Builder builder = SendMessageRequest.newBuilder();
        builder.setRequest(ProtoUtils.ToProto.message(messageSendParams.message()));
        if (messageSendParams.configuration() != null) {
            builder.setConfiguration(ProtoUtils.ToProto.messageSendConfiguration(messageSendParams.configuration()));
        }
        if (messageSendParams.metadata() != null) {
            builder.setMetadata(ProtoUtils.ToProto.struct(messageSendParams.metadata()));
        }
        return builder.build();
    }

    private String getTaskPushNotificationConfigName(GetTaskPushNotificationConfigParams getTaskPushNotificationConfigParams) {
        StringBuilder name = new StringBuilder();
        name.append("tasks/");
        name.append(getTaskPushNotificationConfigParams.id());
        if (getTaskPushNotificationConfigParams.pushNotificationConfigId() != null) {
            name.append("/pushNotificationConfigs/");
            name.append(getTaskPushNotificationConfigParams.pushNotificationConfigId());
        }
        return name.toString();
    }
}
