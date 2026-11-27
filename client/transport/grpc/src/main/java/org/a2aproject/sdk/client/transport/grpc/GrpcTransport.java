package org.a2aproject.sdk.client.transport.grpc;

import static org.a2aproject.sdk.spec.A2AMethods.CANCEL_TASK_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.GET_EXTENDED_AGENT_CARD_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.GET_TASK_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.LIST_TASK_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.SEND_MESSAGE_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.SEND_STREAMING_MESSAGE_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.SUBSCRIBE_TO_TASK_METHOD;
import static org.a2aproject.sdk.util.Assert.checkNotNullParam;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.grpc.Channel;
import io.grpc.Metadata;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import org.a2aproject.sdk.client.transport.spi.ClientTransport;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallContext;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallInterceptor;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientRoutingInterceptor;
import org.a2aproject.sdk.client.transport.spi.interceptors.PayloadAndHeaders;
import org.a2aproject.sdk.client.transport.spi.interceptors.auth.AuthInterceptor;
import org.a2aproject.sdk.common.A2AHeaders;
import org.a2aproject.sdk.grpc.A2AServiceGrpc;
import org.a2aproject.sdk.grpc.A2AServiceGrpc.A2AServiceBlockingV2Stub;
import org.a2aproject.sdk.grpc.A2AServiceGrpc.A2AServiceStub;
import org.a2aproject.sdk.grpc.GetExtendedAgentCardRequest;
import org.a2aproject.sdk.grpc.utils.ProtoUtils.FromProto;
import org.a2aproject.sdk.grpc.utils.ProtoUtils.ToProto;
import org.a2aproject.sdk.grpc.utils.RequestTenant;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
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
import org.a2aproject.sdk.spec.util.Utils;
import org.a2aproject.sdk.util.InterceptorPayloads;
import org.jspecify.annotations.Nullable;

public class GrpcTransport implements ClientTransport {

    private static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY = Metadata.Key.of(
            AuthInterceptor.AUTHORIZATION,
            Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> EXTENSIONS_KEY = Metadata.Key.of(
            A2AHeaders.A2A_EXTENSIONS.toLowerCase(),
            Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> VERSION_KEY = Metadata.Key.of(
            A2AHeaders.A2A_VERSION.toLowerCase(),
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
        this.agentTenant = Utils.defaultIfNull(Utils.canonicalizeTenant(agentTenant), "");
    }

    @Override
    public EventKind sendMessage(MessageSendParams request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        org.a2aproject.sdk.grpc.SendMessageRequest sendMessageRequest = ToProto.sendMessageRequest(request);
        RoutedRequest call = applyInterceptors(SEND_MESSAGE_METHOD, sendMessageRequest,
                request.tenant(), context);

        try {
            A2AServiceBlockingV2Stub stubWithMetadata = createBlockingStubWithMetadata(context, call);
            org.a2aproject.sdk.grpc.SendMessageResponse response = stubWithMetadata.sendMessage(
                    interceptedRequest(call, org.a2aproject.sdk.grpc.SendMessageRequest.class));
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
        org.a2aproject.sdk.grpc.SendMessageRequest grpcRequest = ToProto.sendMessageRequest(request);
        RoutedRequest call = applyInterceptors(SEND_STREAMING_MESSAGE_METHOD,
                grpcRequest, request.tenant(), context);
        StreamObserver<org.a2aproject.sdk.grpc.StreamResponse> streamObserver = new EventStreamObserver(eventConsumer, errorConsumer);

        try {
            A2AServiceStub stubWithMetadata = createAsyncStubWithMetadata(context, call);
            stubWithMetadata.sendStreamingMessage(
                    interceptedRequest(call, org.a2aproject.sdk.grpc.SendMessageRequest.class), streamObserver);
        } catch (StatusRuntimeException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to send streaming message request: ");
        }
    }

    @Override
    public Task getTask(TaskQueryParams request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        org.a2aproject.sdk.grpc.GetTaskRequest.Builder requestBuilder = org.a2aproject.sdk.grpc.GetTaskRequest.newBuilder();
        requestBuilder.setId(request.id());
        if (request.historyLength() != null) {
            requestBuilder.setHistoryLength(request.historyLength());
        }
        org.a2aproject.sdk.grpc.GetTaskRequest getTaskRequest = requestBuilder.build();
        RoutedRequest call = applyInterceptors(GET_TASK_METHOD, getTaskRequest,
                request.tenant(), context);

        try {
            A2AServiceBlockingV2Stub stubWithMetadata = createBlockingStubWithMetadata(context, call);
            return FromProto.task(stubWithMetadata.getTask(
                    interceptedRequest(call, org.a2aproject.sdk.grpc.GetTaskRequest.class)));
        } catch (StatusRuntimeException | StatusException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to get task: ");
        }
    }

    @Override
    public Task cancelTask(CancelTaskParams request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);

        org.a2aproject.sdk.grpc.CancelTaskRequest cancelTaskRequest = org.a2aproject.sdk.grpc.CancelTaskRequest.newBuilder()
                .setId(request.id())
                .build();
        RoutedRequest call = applyInterceptors(CANCEL_TASK_METHOD, cancelTaskRequest, request.tenant(), context);

        try {
            A2AServiceBlockingV2Stub stubWithMetadata = createBlockingStubWithMetadata(context, call);
            return FromProto.task(stubWithMetadata.cancelTask(
                    interceptedRequest(call, org.a2aproject.sdk.grpc.CancelTaskRequest.class)));
        } catch (StatusRuntimeException | StatusException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to cancel task: ");
        }
    }

    @Override
    public ListTasksResult listTasks(ListTasksParams request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);

        org.a2aproject.sdk.grpc.ListTasksRequest.Builder requestBuilder = org.a2aproject.sdk.grpc.ListTasksRequest.newBuilder();
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
                    Timestamp.newBuilder()
                            .setSeconds(request.statusTimestampAfter().getEpochSecond())
                            .setNanos(request.statusTimestampAfter().getNano())
                            .build());
        }
        if (request.includeArtifacts() != null) {
            requestBuilder.setIncludeArtifacts(request.includeArtifacts());
        }
        org.a2aproject.sdk.grpc.ListTasksRequest listTasksRequest = requestBuilder.build();
        RoutedRequest call = applyInterceptors(LIST_TASK_METHOD, listTasksRequest, request.tenant(), context);

        try {
            A2AServiceBlockingV2Stub stubWithMetadata = createBlockingStubWithMetadata(context, call);
            org.a2aproject.sdk.grpc.ListTasksResponse grpcResponse = stubWithMetadata.listTasks(
                    interceptedRequest(call, org.a2aproject.sdk.grpc.ListTasksRequest.class));

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

        org.a2aproject.sdk.grpc.TaskPushNotificationConfig grpcRequest = ToProto.taskPushNotificationConfig(request);
        RoutedRequest call = applyInterceptors(SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, grpcRequest, request.tenant(), context);

        try {
            A2AServiceBlockingV2Stub stubWithMetadata = createBlockingStubWithMetadata(context, call);
            return FromProto.taskPushNotificationConfig(stubWithMetadata.createTaskPushNotificationConfig(
                    interceptedRequest(call, org.a2aproject.sdk.grpc.TaskPushNotificationConfig.class)));
        } catch (StatusRuntimeException | StatusException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to create task push notification config: ");
        }
    }

    @Override
    public TaskPushNotificationConfig getTaskPushNotificationConfiguration(GetTaskPushNotificationConfigParams request,
            @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        checkNotNullParam("taskId", request.taskId());
        String configId = request.id();

        org.a2aproject.sdk.grpc.GetTaskPushNotificationConfigRequest grpcRequest = org.a2aproject.sdk.grpc.GetTaskPushNotificationConfigRequest.newBuilder()
                .setTaskId(request.taskId())
                .setId(configId == null ? "" : configId)
                .build();
        RoutedRequest call = applyInterceptors(GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, grpcRequest, request.tenant(), context);

        try {
            A2AServiceBlockingV2Stub stubWithMetadata = createBlockingStubWithMetadata(context, call);
            return FromProto.taskPushNotificationConfig(stubWithMetadata.getTaskPushNotificationConfig(
                    interceptedRequest(call, org.a2aproject.sdk.grpc.GetTaskPushNotificationConfigRequest.class)));
        } catch (StatusRuntimeException | StatusException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to get task push notification config: ");
        }
    }

    @Override
    public ListTaskPushNotificationConfigsResult listTaskPushNotificationConfigurations(
            ListTaskPushNotificationConfigsParams request,
            @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);

        org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsRequest grpcRequest = org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsRequest.newBuilder()
                .setTaskId(request.id())
                .setPageSize(request.pageSize())
                .setPageToken(request.pageToken())
                .build();
        RoutedRequest call = applyInterceptors(LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD,
                grpcRequest, request.tenant(), context);

        try {
            A2AServiceBlockingV2Stub stubWithMetadata = createBlockingStubWithMetadata(context, call);
            org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsResponse grpcResponse = stubWithMetadata.listTaskPushNotificationConfigs(
                    interceptedRequest(call, org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsRequest.class));
            return FromProto.listTaskPushNotificationConfigsResult(grpcResponse);
        } catch (StatusRuntimeException | StatusException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to list task push notification configs: ");
        }
    }

    @Override
    public void deleteTaskPushNotificationConfigurations(DeleteTaskPushNotificationConfigParams request,
            @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);

        org.a2aproject.sdk.grpc.DeleteTaskPushNotificationConfigRequest grpcRequest = org.a2aproject.sdk.grpc.DeleteTaskPushNotificationConfigRequest.newBuilder()
                .setTaskId(request.taskId())
                .setId(request.id())
                .build();
        RoutedRequest call = applyInterceptors(DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, grpcRequest, request.tenant(), context);

        try {
            A2AServiceBlockingV2Stub stubWithMetadata = createBlockingStubWithMetadata(context, call);
            stubWithMetadata.deleteTaskPushNotificationConfig(
                    interceptedRequest(call, org.a2aproject.sdk.grpc.DeleteTaskPushNotificationConfigRequest.class));
        } catch (StatusRuntimeException | StatusException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to delete task push notification config: ");
        }
    }

    @Override
    public void subscribeToTask(TaskIdParams request, Consumer<StreamingEventKind> eventConsumer,
            Consumer<Throwable> errorConsumer, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        checkNotNullParam("eventConsumer", eventConsumer);

        org.a2aproject.sdk.grpc.SubscribeToTaskRequest grpcRequest = org.a2aproject.sdk.grpc.SubscribeToTaskRequest.newBuilder()
                .setId(request.id())
                .build();
        RoutedRequest call = applyInterceptors(SUBSCRIBE_TO_TASK_METHOD, grpcRequest, request.tenant(), context);

        StreamObserver<org.a2aproject.sdk.grpc.StreamResponse> streamObserver = new EventStreamObserver(eventConsumer, errorConsumer);

        try {
            A2AServiceStub stubWithMetadata = createAsyncStubWithMetadata(context, call);
            stubWithMetadata.subscribeToTask(
                    interceptedRequest(call, org.a2aproject.sdk.grpc.SubscribeToTaskRequest.class), streamObserver);
        } catch (StatusRuntimeException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to subscribe to task: ");
        }
    }

    @Override
    public AgentCard getExtendedAgentCard(GetExtendedAgentCardParams params, @Nullable ClientCallContext context) throws A2AClientException {
        GetExtendedAgentCardRequest request = GetExtendedAgentCardRequest.newBuilder()
                .build();
        RoutedRequest call = applyInterceptors(GET_EXTENDED_AGENT_CARD_METHOD, request, params.tenant(), context);

        try {
            A2AServiceBlockingV2Stub stubWithMetadata = createBlockingStubWithMetadata(context, call);
            org.a2aproject.sdk.grpc.AgentCard response = stubWithMetadata.getExtendedAgentCard(
                    interceptedRequest(call, GetExtendedAgentCardRequest.class));

            return FromProto.agentCard(response);
        } catch (StatusRuntimeException | StatusException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to get extended agent card: ");
        }
    }

    @Override
    public void close() {
    }

    /**
     * Creates gRPC metadata from ClientCallContext headers.
     * Extracts headers like a2a-extensions and sets them as gRPC metadata.
     * The headers are lower-cased (compared to the HTTP headers).
     * @param context the client call context containing headers, may be null
     * @param payloadAndHeaders the payload and headers wrapper, may be null
     * @return the gRPC metadata
     */
    private Metadata createGrpcMetadata(@Nullable ClientCallContext context, @Nullable PayloadAndHeaders payloadAndHeaders) {
        Metadata metadata = new Metadata();
        metadata.put(VERSION_KEY, AgentInterface.CURRENT_PROTOCOL_VERSION);

        if (context != null && context.getHeaders() != null) {
            // Set a2a-version and a2a-extensions headers if present, ignoring case
            for (Map.Entry<String, String> header : context.getHeaders().entrySet()) {
                if (A2AHeaders.A2A_VERSION.equalsIgnoreCase(header.getKey())) {
                    if (header.getValue() != null) {
                        metadata.put(VERSION_KEY, header.getValue());
                    }
                } else if (A2AHeaders.A2A_EXTENSIONS.equalsIgnoreCase(header.getKey())) {
                    if (header.getValue() != null) {
                        metadata.put(EXTENSIONS_KEY, header.getValue());
                    }
                }
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
     * @param call routed request and interceptor headers
     * @return blocking stub with metadata interceptor
     */
    private A2AServiceBlockingV2Stub createBlockingStubWithMetadata(@Nullable ClientCallContext context,
            RoutedRequest call) {
        Metadata metadata = createGrpcMetadata(context, call.payloadAndHeaders());
        return blockingStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));
    }

    /**
     * Creates an async stub with metadata attached from the ClientCallContext.
     *
     * @param context the client call context
     * @param call routed request and interceptor headers
     * @return async stub with metadata interceptor
     */
    private A2AServiceStub createAsyncStubWithMetadata(@Nullable ClientCallContext context,
            RoutedRequest call) {
        Metadata metadata = createGrpcMetadata(context, call.payloadAndHeaders());
        return asyncStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));
    }

    private record RoutedRequest(PayloadAndHeaders payloadAndHeaders, @Nullable String tenant) {
    }

    private RoutedRequest applyInterceptors(String methodName, Message payload,
            @Nullable String requestTenant, @Nullable ClientCallContext context) {
        String tenant = ClientRoutingInterceptor.selectTenant(interceptors, null, methodName,
                requestTenant == null || requestTenant.isBlank() ? agentTenant : requestTenant, agentCard, context);
        Message.Builder builder = payload.toBuilder();
        RequestTenant.set(builder, tenant);
        Message routed = builder.build();
        var call = new PayloadAndHeaders(routed, context == null ? null : context.getHeaders());
        if (interceptors != null) {
            for (var interceptor : interceptors) {
                if (!(interceptor instanceof ClientRoutingInterceptor)) {
                    call = interceptor.intercept(methodName, call.getPayload(), call.getHeaders(), agentCard, context);
                    RequestTenant.requireUnchanged(call.getPayload(), tenant);
                }
            }
        }
        if (call.getPayload() instanceof Message.Builder replacement) {
            Message request = replacement.build();
            RequestTenant.requireUnchanged(request, tenant);
            call = new PayloadAndHeaders(request, call.getHeaders());
        }
        return new RoutedRequest(call, tenant);
    }

    private static <T extends Message> T interceptedRequest(RoutedRequest call, Class<T> type) {
        return InterceptorPayloads.cast(call.payloadAndHeaders().getPayload(), type);
    }

}
