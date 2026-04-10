package org.a2aproject.sdk.compat03.client.transport.grpc;

import static org.a2aproject.sdk.util.Assert.checkNotNullParam;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallContext;
import org.a2aproject.sdk.compat03.client.transport.spi.ClientTransport;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallInterceptor;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.PayloadAndHeaders;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.auth.AuthInterceptor;
import org.a2aproject.sdk.common.A2AHeaders;
import org.a2aproject.sdk.compat03.grpc.A2AServiceGrpc;
import org.a2aproject.sdk.compat03.grpc.A2AServiceGrpc.A2AServiceBlockingV2Stub;
import org.a2aproject.sdk.compat03.grpc.A2AServiceGrpc.A2AServiceStub;
import org.a2aproject.sdk.compat03.grpc.CancelTaskRequest;
import org.a2aproject.sdk.compat03.grpc.CreateTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.compat03.grpc.DeleteTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.compat03.grpc.GetTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.compat03.grpc.GetTaskRequest;
import org.a2aproject.sdk.compat03.grpc.ListTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.compat03.grpc.SendMessageRequest;
import org.a2aproject.sdk.compat03.grpc.SendMessageResponse;
import org.a2aproject.sdk.compat03.grpc.StreamResponse;
import org.a2aproject.sdk.compat03.grpc.TaskSubscriptionRequest;
import org.a2aproject.sdk.compat03.grpc.utils.ProtoUtils.FromProto;
import org.a2aproject.sdk.compat03.grpc.utils.ProtoUtils.ToProto;
import io.grpc.StatusException;
import org.a2aproject.sdk.compat03.spec.A2AClientException;
import org.a2aproject.sdk.compat03.spec.AgentCard;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigParams;
import org.a2aproject.sdk.compat03.spec.EventKind;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigParams;
import org.a2aproject.sdk.compat03.spec.ListTaskPushNotificationConfigParams;
import org.a2aproject.sdk.compat03.spec.MessageSendParams;
import org.a2aproject.sdk.compat03.spec.SendStreamingMessageRequest;
import org.a2aproject.sdk.compat03.spec.SetTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind;
import org.a2aproject.sdk.compat03.spec.Task;
import org.a2aproject.sdk.compat03.spec.TaskIdParams;
import org.a2aproject.sdk.compat03.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.compat03.spec.TaskQueryParams;
import org.a2aproject.sdk.compat03.spec.TaskResubscriptionRequest;
import io.grpc.Channel;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import org.jspecify.annotations.Nullable;

public class GrpcTransport implements ClientTransport {

    private static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY = Metadata.Key.of(
            AuthInterceptor.AUTHORIZATION,
            Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> EXTENSIONS_KEY = Metadata.Key.of(
            "X-A2A-Extensions",
            Metadata.ASCII_STRING_MARSHALLER);
    private final A2AServiceBlockingV2Stub blockingStub;
    private final A2AServiceStub asyncStub;
    private final @Nullable List<ClientCallInterceptor> interceptors;
    private AgentCard agentCard;

    public GrpcTransport(Channel channel, AgentCard agentCard) {
        this(channel, agentCard, null);
    }

    public GrpcTransport(Channel channel, AgentCard agentCard, @Nullable List<ClientCallInterceptor> interceptors) {
        checkNotNullParam("channel", channel);
        checkNotNullParam("agentCard", agentCard);
        this.asyncStub = A2AServiceGrpc.newStub(channel);
        this.blockingStub = A2AServiceGrpc.newBlockingV2Stub(channel);
        this.agentCard = agentCard;
        this.interceptors = interceptors;
    }

    @Override
    public EventKind sendMessage(MessageSendParams request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);

        SendMessageRequest sendMessageRequest = createGrpcSendMessageRequest(request, context);
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(org.a2aproject.sdk.compat03.spec.SendMessageRequest.METHOD, sendMessageRequest,
                agentCard, context);

        try {
            A2AServiceBlockingV2Stub stubWithMetadata = createBlockingStubWithMetadata(context, payloadAndHeaders);
            SendMessageResponse response = stubWithMetadata.sendMessage(sendMessageRequest);
            if (response.hasMsg()) {
                return FromProto.message(response.getMsg());
            } else if (response.hasTask()) {
                return FromProto.task(response.getTask());
            } else {
                throw new A2AClientException("Server response did not contain a message or task");
            }
        } catch (StatusRuntimeException | StatusException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to send message: ");
        }
    }

    @Override
    public void sendMessageStreaming(MessageSendParams request, Consumer<StreamingEventKind> eventConsumer,
                                     Consumer<Throwable> errorConsumer, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        checkNotNullParam("eventConsumer", eventConsumer);
        SendMessageRequest grpcRequest = createGrpcSendMessageRequest(request, context);
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(SendStreamingMessageRequest.METHOD,
                grpcRequest, agentCard, context);
        StreamObserver<StreamResponse> streamObserver = new EventStreamObserver(eventConsumer, errorConsumer);

        try {
            A2AServiceStub stubWithMetadata = createAsyncStubWithMetadata(context, payloadAndHeaders);
            stubWithMetadata.sendStreamingMessage(grpcRequest, streamObserver);
        } catch (StatusRuntimeException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to send streaming message request: ");
        }
    }

    @Override
    public Task getTask(TaskQueryParams request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);

        GetTaskRequest.Builder requestBuilder = GetTaskRequest.newBuilder();
        requestBuilder.setName("tasks/" + request.id());
        requestBuilder.setHistoryLength(request.historyLength());
        GetTaskRequest getTaskRequest = requestBuilder.build();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(org.a2aproject.sdk.compat03.spec.GetTaskRequest.METHOD, getTaskRequest,
                agentCard, context);

        try {
            A2AServiceBlockingV2Stub stubWithMetadata = createBlockingStubWithMetadata(context, payloadAndHeaders);
            return FromProto.task(stubWithMetadata.getTask(getTaskRequest));
        } catch (StatusRuntimeException | StatusException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to get task: ");
        }
    }

    @Override
    public Task cancelTask(TaskIdParams request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);

        CancelTaskRequest cancelTaskRequest = CancelTaskRequest.newBuilder()
                .setName("tasks/" + request.id())
                .build();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(org.a2aproject.sdk.compat03.spec.CancelTaskRequest.METHOD, cancelTaskRequest,
                agentCard, context);

        try {
            A2AServiceBlockingV2Stub stubWithMetadata = createBlockingStubWithMetadata(context, payloadAndHeaders);
            return FromProto.task(stubWithMetadata.cancelTask(cancelTaskRequest));
        } catch (StatusRuntimeException | StatusException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to cancel task: ");
        }
    }

    @Override
    public TaskPushNotificationConfig setTaskPushNotificationConfiguration(TaskPushNotificationConfig request,
                                                                           @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);

        String configId = request.pushNotificationConfig().id();
        CreateTaskPushNotificationConfigRequest grpcRequest = CreateTaskPushNotificationConfigRequest.newBuilder()
                .setParent("tasks/" + request.taskId())
                .setConfig(ToProto.taskPushNotificationConfig(request))
                .setConfigId(configId != null ? configId : request.taskId())
                .build();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(SetTaskPushNotificationConfigRequest.METHOD,
                grpcRequest, agentCard, context);

        try {
            A2AServiceBlockingV2Stub stubWithMetadata = createBlockingStubWithMetadata(context, payloadAndHeaders);
            return FromProto.taskPushNotificationConfig(stubWithMetadata.createTaskPushNotificationConfig(grpcRequest));
        } catch (StatusRuntimeException | StatusException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to create task push notification config: ");
        }
    }

    @Override
    public TaskPushNotificationConfig getTaskPushNotificationConfiguration(
            GetTaskPushNotificationConfigParams request,
            @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);

        GetTaskPushNotificationConfigRequest grpcRequest = GetTaskPushNotificationConfigRequest.newBuilder()
                .setName(getTaskPushNotificationConfigName(request))
                .build();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigRequest.METHOD,
                grpcRequest, agentCard, context);

        try {
            A2AServiceBlockingV2Stub stubWithMetadata = createBlockingStubWithMetadata(context, payloadAndHeaders);
            return FromProto.taskPushNotificationConfig(stubWithMetadata.getTaskPushNotificationConfig(grpcRequest));
        } catch (StatusRuntimeException | StatusException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to get task push notification config: ");
        }
    }

    @Override
    public List<TaskPushNotificationConfig> listTaskPushNotificationConfigurations(
            ListTaskPushNotificationConfigParams request,
            @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);

        ListTaskPushNotificationConfigRequest grpcRequest = ListTaskPushNotificationConfigRequest.newBuilder()
                .setParent("tasks/" + request.id())
                .build();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(org.a2aproject.sdk.compat03.spec.ListTaskPushNotificationConfigRequest.METHOD,
                grpcRequest, agentCard, context);

        try {
            A2AServiceBlockingV2Stub stubWithMetadata = createBlockingStubWithMetadata(context, payloadAndHeaders);
            return stubWithMetadata.listTaskPushNotificationConfig(grpcRequest).getConfigsList().stream()
                    .map(FromProto::taskPushNotificationConfig)
                    .collect(Collectors.toList());
        } catch (StatusRuntimeException | StatusException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to list task push notification config: ");
        }
    }

    @Override
    public void deleteTaskPushNotificationConfigurations(DeleteTaskPushNotificationConfigParams request,
                                                         @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);

        DeleteTaskPushNotificationConfigRequest grpcRequest = DeleteTaskPushNotificationConfigRequest.newBuilder()
                .setName(getTaskPushNotificationConfigName(request.id(), request.pushNotificationConfigId()))
                .build();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigRequest.METHOD,
                grpcRequest, agentCard, context);

        try {
            A2AServiceBlockingV2Stub stubWithMetadata = createBlockingStubWithMetadata(context, payloadAndHeaders);
            stubWithMetadata.deleteTaskPushNotificationConfig(grpcRequest);
        } catch (StatusRuntimeException | StatusException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to delete task push notification config: ");
        }
    }

    @Override
    public void resubscribe(TaskIdParams request, Consumer<StreamingEventKind> eventConsumer,
                            Consumer<Throwable> errorConsumer, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        checkNotNullParam("eventConsumer", eventConsumer);

        TaskSubscriptionRequest grpcRequest = TaskSubscriptionRequest.newBuilder()
                .setName("tasks/" + request.id())
                .build();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(TaskResubscriptionRequest.METHOD,
                grpcRequest, agentCard, context);

        StreamObserver<StreamResponse> streamObserver = new EventStreamObserver(eventConsumer, errorConsumer);

        try {
            A2AServiceStub stubWithMetadata = createAsyncStubWithMetadata(context, payloadAndHeaders);
            stubWithMetadata.taskSubscription(grpcRequest, streamObserver);
        } catch (StatusRuntimeException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to resubscribe task push notification config: ");
        }
    }

    @Override
    public AgentCard getAgentCard(@Nullable ClientCallContext context) throws A2AClientException {
        // TODO: Determine how to handle retrieving the authenticated extended agent card
        return agentCard;
    }

    @Override
    public void close() {
    }

    private SendMessageRequest createGrpcSendMessageRequest(MessageSendParams messageSendParams, @Nullable ClientCallContext context) {
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

    /**
     * Creates gRPC metadata from ClientCallContext headers.
     * Extracts headers like X-A2A-Extensions and sets them as gRPC metadata.
     * @param context the client call context containing headers, may be null
     * @param payloadAndHeaders the payload and headers wrapper, may be null
     * @return the gRPC metadata
     */
    private Metadata createGrpcMetadata(@Nullable ClientCallContext context, @Nullable PayloadAndHeaders payloadAndHeaders) {
        Metadata metadata = new Metadata();
        
        if (context != null && context.getHeaders() != null) {
            // Set X-A2A-Extensions header if present
            String extensionsHeader = context.getHeaders().get("X-A2A-Extensions");
            if (extensionsHeader != null) {
                metadata.put(EXTENSIONS_KEY, extensionsHeader);
            }
            
            // Add other headers as needed in the future
            // For now, we only handle X-A2A-Extensions
        }
        if (payloadAndHeaders != null && payloadAndHeaders.getHeaders() != null) {
            // Handle all headers from interceptors (including auth headers)
            for (Map.Entry<String, String> headerEntry : payloadAndHeaders.getHeaders().entrySet()) {
                String headerName = headerEntry.getKey();
                String headerValue = headerEntry.getValue();
                
                if (headerValue != null) {
                    // Use static key for common Authorization header, create dynamic keys for others
                    if (AuthInterceptor.AUTHORIZATION.equals(headerName)) {
                        metadata.put(AUTHORIZATION_METADATA_KEY, headerValue);
                    } else {
                        // Create a metadata key dynamically for API keys and other custom headers
                        Metadata.Key<String> metadataKey = Metadata.Key.of(headerName, Metadata.ASCII_STRING_MARSHALLER);
                        metadata.put(metadataKey, headerValue);
                    }
                }
            }
        }
        
        return metadata;
    }

    /**
     * Creates a blocking stub with metadata attached from the ClientCallContext.
     *
     * @param context           the client call context
     * @param payloadAndHeaders the payloadAndHeaders after applying any interceptors
     * @return blocking stub with metadata interceptor
     */
    private A2AServiceBlockingV2Stub createBlockingStubWithMetadata(@Nullable ClientCallContext context,
                                                                    PayloadAndHeaders payloadAndHeaders) {
        Metadata metadata = createGrpcMetadata(context, payloadAndHeaders);
        return blockingStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));
    }

    /**
     * Creates an async stub with metadata attached from the ClientCallContext.
     *
     * @param context           the client call context
     * @param payloadAndHeaders the payloadAndHeaders after applying any interceptors
     * @return async stub with metadata interceptor
     */
    private A2AServiceStub createAsyncStubWithMetadata(@Nullable ClientCallContext context,
                                                       PayloadAndHeaders payloadAndHeaders) {
        Metadata metadata = createGrpcMetadata(context, payloadAndHeaders);
        return asyncStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));
    }

    private String getTaskPushNotificationConfigName(GetTaskPushNotificationConfigParams params) {
        return getTaskPushNotificationConfigName(params.id(), params.pushNotificationConfigId());
    }

    private String getTaskPushNotificationConfigName(String taskId, @Nullable String pushNotificationConfigId) {
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

    private PayloadAndHeaders applyInterceptors(String methodName, Object payload,
                                                AgentCard agentCard, @Nullable ClientCallContext clientCallContext) {
        PayloadAndHeaders payloadAndHeaders = new PayloadAndHeaders(payload,
                clientCallContext != null ? clientCallContext.getHeaders() : null);
        if (interceptors != null && ! interceptors.isEmpty()) {
            for (ClientCallInterceptor interceptor : interceptors) {
                payloadAndHeaders = interceptor.intercept(methodName, payloadAndHeaders.getPayload(),
                        payloadAndHeaders.getHeaders(), agentCard, clientCallContext);
            }
        }
        return payloadAndHeaders;
    }

}