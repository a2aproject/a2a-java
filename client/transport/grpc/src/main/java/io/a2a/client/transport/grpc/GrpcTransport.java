package io.a2a.client.transport.grpc;

import static io.a2a.spec.A2AMethods.CANCEL_TASK_METHOD;
import static io.a2a.spec.A2AMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static io.a2a.spec.A2AMethods.GET_EXTENDED_AGENT_CARD_METHOD;
import static io.a2a.spec.A2AMethods.GET_TASK_METHOD;
import static io.a2a.spec.A2AMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static io.a2a.spec.A2AMethods.LIST_TASK_METHOD;
import static io.a2a.spec.A2AMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static io.a2a.spec.A2AMethods.SEND_MESSAGE_METHOD;
import static io.a2a.spec.A2AMethods.SEND_STREAMING_MESSAGE_METHOD;
import static io.a2a.spec.A2AMethods.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static io.a2a.spec.A2AMethods.SUBSCRIBE_TO_TASK_METHOD;
import static io.a2a.util.Assert.checkNotNullParam;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.a2a.client.transport.spi.ClientTransport;
import io.a2a.client.transport.spi.interceptors.ClientCallContext;
import io.a2a.client.transport.spi.interceptors.ClientCallInterceptor;
import io.a2a.client.transport.spi.interceptors.PayloadAndHeaders;
import io.a2a.client.transport.spi.interceptors.auth.AuthInterceptor;
import io.a2a.common.A2AHeaders;
import io.a2a.grpc.A2AServiceGrpc;
import io.a2a.grpc.A2AServiceGrpc.A2AServiceBlockingV2Stub;
import io.a2a.grpc.A2AServiceGrpc.A2AServiceStub;
import io.a2a.grpc.GetExtendedAgentCardRequest;
import io.a2a.grpc.utils.ProtoUtils.FromProto;
import io.a2a.grpc.utils.ProtoUtils.ToProto;
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
import io.grpc.Channel;
import io.grpc.Metadata;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import org.jspecify.annotations.Nullable;

public class GrpcTransport implements ClientTransport {

    private static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY = Metadata.Key.of(
            AuthInterceptor.AUTHORIZATION,
            Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> EXTENSIONS_KEY = Metadata.Key.of(
            A2AHeaders.X_A2A_EXTENSIONS,
            Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> VERSION_KEY = Metadata.Key.of(
            A2AHeaders.X_A2A_VERSION,
            Metadata.ASCII_STRING_MARSHALLER);
    private final A2AServiceBlockingV2Stub blockingStub;
    private final A2AServiceStub asyncStub;
    private final @Nullable List<ClientCallInterceptor> interceptors;
    private final AgentCard agentCard;
    private final String agentTenant;

    public GrpcTransport(Channel channel, AgentCard agentCard) {
        this(channel, agentCard, "", null);
    }

    public GrpcTransport(Channel channel, AgentCard agentCard, @Nullable String agentTenant, @Nullable List<ClientCallInterceptor> interceptors) {
        checkNotNullParam("channel", channel);
        checkNotNullParam("agentCard", agentCard);
        this.asyncStub = A2AServiceGrpc.newStub(channel);
        this.blockingStub = A2AServiceGrpc.newBlockingV2Stub(channel);
        this.agentCard = agentCard;
        this.interceptors = interceptors;
        this.agentTenant = agentTenant == null || agentTenant.isBlank() ? "" : agentTenant;
    }

    /**
     * Resolves the tenant to use, preferring the request tenant over the agent default.
     *
     * @param requestTenant the tenant from the request, may be null or blank
     * @return the tenant to use (request tenant if provided, otherwise agent default)
     */
    private String resolveTenant(@Nullable String requestTenant) {
        return (requestTenant == null || requestTenant.isBlank()) ? agentTenant : requestTenant;
    }

    @Override
    public EventKind sendMessage(MessageSendParams request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        MessageSendParams tenantRequest = createRequestWithTenant(request);

        io.a2a.grpc.SendMessageRequest sendMessageRequest = createGrpcSendMessageRequest(tenantRequest, context);
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(SEND_MESSAGE_METHOD, sendMessageRequest,
                agentCard, context);

        try {
            A2AServiceBlockingV2Stub stubWithMetadata = createBlockingStubWithMetadata(context, payloadAndHeaders);
            io.a2a.grpc.SendMessageResponse response = stubWithMetadata.sendMessage(sendMessageRequest);
            if (response.hasMessage()) {
                return FromProto.message(response.getMessage());
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
        MessageSendParams tenantRequest = createRequestWithTenant(request);

        io.a2a.grpc.SendMessageRequest grpcRequest = createGrpcSendMessageRequest(tenantRequest, context);
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(SEND_STREAMING_MESSAGE_METHOD,
                grpcRequest, agentCard, context);
        StreamObserver<io.a2a.grpc.StreamResponse> streamObserver = new EventStreamObserver(eventConsumer, errorConsumer);

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
        io.a2a.grpc.GetTaskRequest.Builder requestBuilder = io.a2a.grpc.GetTaskRequest.newBuilder();
        requestBuilder.setId(request.id());
        if (request.historyLength() != null) {
            requestBuilder.setHistoryLength(request.historyLength());
        }
        requestBuilder.setTenant(resolveTenant(request.tenant()));
        io.a2a.grpc.GetTaskRequest getTaskRequest = requestBuilder.build();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(GET_TASK_METHOD, getTaskRequest,
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

        io.a2a.grpc.CancelTaskRequest cancelTaskRequest = io.a2a.grpc.CancelTaskRequest.newBuilder()
                .setId(request.id())
                .setTenant(resolveTenant(request.tenant()))
                .build();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(CANCEL_TASK_METHOD, cancelTaskRequest, agentCard, context);

        try {
            A2AServiceBlockingV2Stub stubWithMetadata = createBlockingStubWithMetadata(context, payloadAndHeaders);
            return FromProto.task(stubWithMetadata.cancelTask(cancelTaskRequest));
        } catch (StatusRuntimeException | StatusException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to cancel task: ");
        }
    }

    @Override
    public ListTasksResult listTasks(ListTasksParams request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);

        io.a2a.grpc.ListTasksRequest.Builder requestBuilder = io.a2a.grpc.ListTasksRequest.newBuilder();
        if (request.contextId() != null) {
            requestBuilder.setContextId(request.contextId());
        }
        if (request.status() != null) {
            requestBuilder.setStatus(ToProto.taskState(request.status()));
        }
        if (request.pageSize() != null) {
            requestBuilder.setPageSize(request.pageSize());
        }
        if (request.pageToken() != null) {
            requestBuilder.setPageToken(request.pageToken());
        }
        if (request.historyLength() != null) {
            requestBuilder.setHistoryLength(request.historyLength());
        }
        if (request.statusTimestampAfter() != null) {
            requestBuilder.setStatusTimestampAfter(
                    com.google.protobuf.Timestamp.newBuilder()
                            .setSeconds(request.statusTimestampAfter().getEpochSecond())
                            .setNanos(request.statusTimestampAfter().getNano())
                            .build());
        }
        if (request.includeArtifacts() != null) {
            requestBuilder.setIncludeArtifacts(request.includeArtifacts());
        }
        requestBuilder.setTenant(resolveTenant(request.tenant()));
        io.a2a.grpc.ListTasksRequest listTasksRequest = requestBuilder.build();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(LIST_TASK_METHOD, listTasksRequest, agentCard, context);

        try {
            A2AServiceBlockingV2Stub stubWithMetadata = createBlockingStubWithMetadata(context, payloadAndHeaders);
            io.a2a.grpc.ListTasksResponse grpcResponse = stubWithMetadata.listTasks(listTasksRequest);

            return new ListTasksResult(
                    grpcResponse.getTasksList().stream()
                            .map(FromProto::task)
                            .collect(Collectors.toList()),
                    grpcResponse.getTotalSize(),
                    grpcResponse.getTasksCount(),
                    grpcResponse.getNextPageToken().isEmpty() ? null : grpcResponse.getNextPageToken()
            );
        } catch (StatusRuntimeException | StatusException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to list tasks: ");
        }
    }

    @Override
    public TaskPushNotificationConfig createTaskPushNotificationConfiguration(TaskPushNotificationConfig request,
            @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);

        String configId = request.pushNotificationConfig().id();
        io.a2a.grpc.CreateTaskPushNotificationConfigRequest grpcRequest = io.a2a.grpc.CreateTaskPushNotificationConfigRequest.newBuilder()
                .setTaskId(request.taskId())
                .setConfig(ToProto.taskPushNotificationConfig(request).getPushNotificationConfig())
                .setConfigId(configId != null ? configId : request.taskId())
                .setTenant(resolveTenant(request.tenant()))
                .build();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, grpcRequest, agentCard, context);

        try {
            A2AServiceBlockingV2Stub stubWithMetadata = createBlockingStubWithMetadata(context, payloadAndHeaders);
            return FromProto.taskPushNotificationConfig(stubWithMetadata.createTaskPushNotificationConfig(grpcRequest));
        } catch (StatusRuntimeException | StatusException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to create task push notification config: ");
        }
    }

    @Override
    public TaskPushNotificationConfig getTaskPushNotificationConfiguration(GetTaskPushNotificationConfigParams request,
            @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        checkNotNullParam("taskId", request.id());
        if(request.pushNotificationConfigId() == null) {
             throw new IllegalArgumentException("Id must not be null");
        }

        io.a2a.grpc.GetTaskPushNotificationConfigRequest grpcRequest = io.a2a.grpc.GetTaskPushNotificationConfigRequest.newBuilder()
                .setTaskId(request.id())
                .setTenant(resolveTenant(request.tenant()))
                .setId(request.pushNotificationConfigId())
                .build();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, grpcRequest, agentCard, context);

        try {
            A2AServiceBlockingV2Stub stubWithMetadata = createBlockingStubWithMetadata(context, payloadAndHeaders);
            return FromProto.taskPushNotificationConfig(stubWithMetadata.getTaskPushNotificationConfig(grpcRequest));
        } catch (StatusRuntimeException | StatusException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to get task push notification config: ");
        }
    }

    @Override
    public ListTaskPushNotificationConfigResult listTaskPushNotificationConfigurations(
            ListTaskPushNotificationConfigParams request,
            @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);

        io.a2a.grpc.ListTaskPushNotificationConfigRequest grpcRequest = io.a2a.grpc.ListTaskPushNotificationConfigRequest.newBuilder()
                .setTaskId(request.id())
                .setTenant(resolveTenant(request.tenant()))
                .setPageSize(request.pageSize())
                .setPageToken(request.pageToken())
                .build();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD,
                grpcRequest, agentCard, context);

        try {
            A2AServiceBlockingV2Stub stubWithMetadata = createBlockingStubWithMetadata(context, payloadAndHeaders);
            io.a2a.grpc.ListTaskPushNotificationConfigResponse grpcResponse = stubWithMetadata.listTaskPushNotificationConfig(grpcRequest);
            return FromProto.listTaskPushNotificationConfigResult(grpcResponse);
        } catch (StatusRuntimeException | StatusException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to list task push notification config: ");
        }
    }

    @Override
    public void deleteTaskPushNotificationConfigurations(DeleteTaskPushNotificationConfigParams request,
            @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);

        io.a2a.grpc.DeleteTaskPushNotificationConfigRequest grpcRequest = io.a2a.grpc.DeleteTaskPushNotificationConfigRequest.newBuilder()
                .setTaskId(request.id())
                .setId(request.pushNotificationConfigId())
                .setTenant(resolveTenant(request.tenant()))
                .build();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, grpcRequest, agentCard, context);

        try {
            A2AServiceBlockingV2Stub stubWithMetadata = createBlockingStubWithMetadata(context, payloadAndHeaders);
            stubWithMetadata.deleteTaskPushNotificationConfig(grpcRequest);
        } catch (StatusRuntimeException | StatusException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to delete task push notification config: ");
        }
    }

    @Override
    public void subscribeToTask(TaskIdParams request, Consumer<StreamingEventKind> eventConsumer,
            Consumer<Throwable> errorConsumer, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        checkNotNullParam("eventConsumer", eventConsumer);

        io.a2a.grpc.SubscribeToTaskRequest grpcRequest = io.a2a.grpc.SubscribeToTaskRequest.newBuilder()
                .setTenant(resolveTenant(request.tenant()))
                .setId(request.id())
                .build();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(SUBSCRIBE_TO_TASK_METHOD, grpcRequest, agentCard, context);

        StreamObserver<io.a2a.grpc.StreamResponse> streamObserver = new EventStreamObserver(eventConsumer, errorConsumer);

        try {
            A2AServiceStub stubWithMetadata = createAsyncStubWithMetadata(context, payloadAndHeaders);
            stubWithMetadata.subscribeToTask(grpcRequest, streamObserver);
        } catch (StatusRuntimeException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to subscribe task push notification config: ");
        }
    }

    /**
     * Ensure tenant is set, using agent default if not provided in request
     *
     * @param request the initial request.
     * @return the updated request with the tenant set.
     */
    private MessageSendParams createRequestWithTenant(MessageSendParams request) {
        return MessageSendParams.builder()
                .configuration(request.configuration())
                .message(request.message())
                .metadata(request.metadata())
                .tenant(resolveTenant(request.tenant()))
                .build();
    }

    @Override
    public AgentCard getExtendedAgentCard(@Nullable ClientCallContext context) throws A2AClientException {
        GetExtendedAgentCardRequest request = GetExtendedAgentCardRequest.newBuilder()
                .build();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(GET_EXTENDED_AGENT_CARD_METHOD, request, agentCard, context);

        try {
            A2AServiceBlockingV2Stub stubWithMetadata = createBlockingStubWithMetadata(context, payloadAndHeaders);
            io.a2a.grpc.AgentCard response = stubWithMetadata.getExtendedAgentCard(request);

            return FromProto.agentCard(response);
        } catch (StatusRuntimeException | StatusException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to get extended agent card: ");
        }
    }

    @Override
    public void close() {
    }

    private io.a2a.grpc.SendMessageRequest createGrpcSendMessageRequest(MessageSendParams messageSendParams, @Nullable ClientCallContext context) {
        return ToProto.sendMessageRequest(messageSendParams);
    }

    /**
     * Creates gRPC metadata from ClientCallContext headers.
     * Extracts headers like X-A2A-Extensions and sets them as gRPC metadata.
     *
     * @param context the client call context containing headers, may be null
     * @param payloadAndHeaders the payload and headers wrapper, may be null
     * @return the gRPC metadata
     */
    private Metadata createGrpcMetadata(@Nullable ClientCallContext context, @Nullable PayloadAndHeaders payloadAndHeaders) {
        Metadata metadata = new Metadata();

        if (context != null && context.getHeaders() != null) {
            // Set X-A2A-Version header if present
            String versionHeader = context.getHeaders().get(A2AHeaders.X_A2A_VERSION);
            if (versionHeader != null) {
                metadata.put(VERSION_KEY, versionHeader);
            }

            // Set X-A2A-Extensions header if present
            String extensionsHeader = context.getHeaders().get(A2AHeaders.X_A2A_EXTENSIONS);
            if (extensionsHeader != null) {
                metadata.put(EXTENSIONS_KEY, extensionsHeader);
            }

            // Add other headers as needed in the future
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
     * @param context the client call context
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
     * @param context the client call context
     * @param payloadAndHeaders the payloadAndHeaders after applying any interceptors
     * @return async stub with metadata interceptor
     */
    private A2AServiceStub createAsyncStubWithMetadata(@Nullable ClientCallContext context,
            PayloadAndHeaders payloadAndHeaders) {
        Metadata metadata = createGrpcMetadata(context, payloadAndHeaders);
        return asyncStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));
    }

    private PayloadAndHeaders applyInterceptors(String methodName, Object payload,
            AgentCard agentCard, @Nullable ClientCallContext clientCallContext) {
        PayloadAndHeaders payloadAndHeaders = new PayloadAndHeaders(payload,
                clientCallContext != null ? clientCallContext.getHeaders() : null);
        if (interceptors != null && !interceptors.isEmpty()) {
            for (ClientCallInterceptor interceptor : interceptors) {
                payloadAndHeaders = interceptor.intercept(methodName, payloadAndHeaders.getPayload(),
                        payloadAndHeaders.getHeaders(), agentCard, clientCallContext);
            }
        }
        return payloadAndHeaders;
    }

}
