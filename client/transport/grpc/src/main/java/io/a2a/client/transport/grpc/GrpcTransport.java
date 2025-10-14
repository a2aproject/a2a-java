package io.a2a.client.transport.grpc;

import static io.a2a.grpc.A2AServiceGrpc.A2AServiceBlockingV2Stub;
import static io.a2a.grpc.A2AServiceGrpc.A2AServiceStub;
import static io.a2a.grpc.utils.ProtoUtils.FromProto;
import static io.a2a.grpc.utils.ProtoUtils.ToProto;
import static io.a2a.util.Assert.checkNotNullParam;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.protobuf.Empty;
import io.a2a.client.transport.spi.AbstractClientTransport;
import io.a2a.client.transport.spi.interceptors.ClientCallContext;
import io.a2a.client.transport.spi.interceptors.ClientCallInterceptor;
import io.a2a.client.transport.spi.interceptors.PayloadAndHeaders;
import io.a2a.client.transport.spi.interceptors.auth.AuthInterceptor;
import io.a2a.common.A2AHeaders;
import io.a2a.grpc.A2AServiceGrpc;
import io.a2a.grpc.CancelTaskRequest;
import io.a2a.grpc.CreateTaskPushNotificationConfigRequest;
import io.a2a.grpc.DeleteTaskPushNotificationConfigRequest;
import io.a2a.grpc.GetTaskPushNotificationConfigRequest;
import io.a2a.grpc.GetTaskRequest;
import io.a2a.grpc.ListTaskPushNotificationConfigRequest;
import io.a2a.grpc.SendMessageRequest;
import io.a2a.grpc.StreamResponse;
import io.a2a.grpc.TaskSubscriptionRequest;

import io.a2a.spec.*;
import io.grpc.Channel;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;

public class GrpcTransport extends AbstractClientTransport {

    private static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY = Metadata.Key.of(
            AuthInterceptor.AUTHORIZATION,
            Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> EXTENSIONS_KEY = Metadata.Key.of(
            A2AHeaders.X_A2A_EXTENSIONS,
            Metadata.ASCII_STRING_MARSHALLER);
    private final A2AServiceBlockingV2Stub blockingStub;
    private final A2AServiceStub asyncStub;
    private AgentCard agentCard;

    public GrpcTransport(Channel channel, AgentCard agentCard) {
        this(channel, agentCard, null);
    }

    public GrpcTransport(Channel channel, AgentCard agentCard, List<ClientCallInterceptor> interceptors) {
        super(interceptors);
        checkNotNullParam("channel", channel);
        this.asyncStub = A2AServiceGrpc.newStub(channel);
        this.blockingStub = A2AServiceGrpc.newBlockingV2Stub(channel);
        this.agentCard = agentCard;
    }

    @Override
    public CompletableFuture<EventKind> sendMessage(MessageSendParams request, ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);

        SendMessageRequest sendMessageRequest = createGrpcSendMessageRequest(request, context);
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(io.a2a.spec.SendMessageRequest.METHOD, sendMessageRequest,
                agentCard, context);

            A2AServiceStub stubWithMetadata = createAsyncStubWithMetadata(context, payloadAndHeaders);
            SingleValueStreamObserver<io.a2a.grpc.SendMessageResponse> observer = new SingleValueStreamObserver<>();
            stubWithMetadata.sendMessage(sendMessageRequest, observer);

            return observer.completionStage()
                    .thenCompose(new Function<io.a2a.grpc.SendMessageResponse, CompletionStage<EventKind>>() {
                @Override
                public CompletionStage<EventKind> apply(io.a2a.grpc.SendMessageResponse response) {
                    if (response.hasMsg()) {
                        return CompletableFuture.completedFuture(FromProto.message(response.getMsg()));
                    } else if (response.hasTask()) {
                        return CompletableFuture.completedFuture(FromProto.task(response.getTask()));
                    } else {
                        return CompletableFuture.failedFuture(new A2AClientException("Server response did not contain a message or task"));
                    }
                }
            }).toCompletableFuture();
    }

    @Override
    public void sendMessageStreaming(MessageSendParams request, Consumer<StreamingEventKind> eventConsumer,
                                     Consumer<Throwable> errorConsumer, ClientCallContext context) throws A2AClientException {
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
    public CompletableFuture<Task> getTask(TaskQueryParams request, ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);

        GetTaskRequest.Builder requestBuilder = GetTaskRequest.newBuilder();
        requestBuilder.setName("tasks/" + request.id());
        if (request.historyLength() != null) {
            requestBuilder.setHistoryLength(request.historyLength());
        }
        GetTaskRequest getTaskRequest = requestBuilder.build();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(io.a2a.spec.GetTaskRequest.METHOD, getTaskRequest,
                agentCard, context);

        A2AServiceStub stubWithMetadata = createAsyncStubWithMetadata(context, payloadAndHeaders);
        SingleValueStreamObserver<io.a2a.grpc.Task> observer = new SingleValueStreamObserver<>();
        stubWithMetadata.getTask(getTaskRequest, observer);

        return observer.completionStage()
                .thenCompose(task -> CompletableFuture.completedFuture(FromProto.task(task)))
                .toCompletableFuture();
    }

    @Override
    public CompletableFuture<Task> cancelTask(TaskIdParams request, ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);

        CancelTaskRequest cancelTaskRequest = CancelTaskRequest.newBuilder()
                .setName("tasks/" + request.id())
                .build();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(io.a2a.spec.CancelTaskRequest.METHOD, cancelTaskRequest,
                agentCard, context);

        A2AServiceStub stubWithMetadata = createAsyncStubWithMetadata(context, payloadAndHeaders);
        SingleValueStreamObserver<io.a2a.grpc.Task> observer = new SingleValueStreamObserver<>();
        stubWithMetadata.cancelTask(cancelTaskRequest, observer);

        return observer.completionStage()
                .thenCompose(task -> CompletableFuture.completedFuture(FromProto.task(task)))
                .toCompletableFuture();
    }

    @Override
    public CompletableFuture<TaskPushNotificationConfig> setTaskPushNotificationConfiguration(TaskPushNotificationConfig request,
                                                                           ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);

        String configId = request.pushNotificationConfig().id();
        CreateTaskPushNotificationConfigRequest grpcRequest = CreateTaskPushNotificationConfigRequest.newBuilder()
                .setParent("tasks/" + request.taskId())
                .setConfig(ToProto.taskPushNotificationConfig(request))
                .setConfigId(configId != null ? configId : request.taskId())
                .build();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(SetTaskPushNotificationConfigRequest.METHOD,
                grpcRequest, agentCard, context);

        A2AServiceStub stubWithMetadata = createAsyncStubWithMetadata(context, payloadAndHeaders);
        SingleValueStreamObserver<io.a2a.grpc.TaskPushNotificationConfig> observer = new SingleValueStreamObserver<>();
        stubWithMetadata.createTaskPushNotificationConfig(grpcRequest, observer);

        return observer.completionStage()
                .thenCompose(taskPushNotificationConfig -> CompletableFuture.completedFuture(FromProto.taskPushNotificationConfig(taskPushNotificationConfig)))
                .toCompletableFuture();
    }

    @Override
    public CompletableFuture<TaskPushNotificationConfig> getTaskPushNotificationConfiguration(
            GetTaskPushNotificationConfigParams request,
            ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);

        GetTaskPushNotificationConfigRequest grpcRequest = GetTaskPushNotificationConfigRequest.newBuilder()
                .setName(getTaskPushNotificationConfigName(request))
                .build();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(io.a2a.spec.GetTaskPushNotificationConfigRequest.METHOD,
                grpcRequest, agentCard, context);

        A2AServiceStub stubWithMetadata = createAsyncStubWithMetadata(context, payloadAndHeaders);
        SingleValueStreamObserver<io.a2a.grpc.TaskPushNotificationConfig> observer = new SingleValueStreamObserver<>();
        stubWithMetadata.getTaskPushNotificationConfig(grpcRequest, observer);

        return observer.completionStage()
                .thenCompose(taskPushNotificationConfig -> CompletableFuture.completedFuture(FromProto.taskPushNotificationConfig(taskPushNotificationConfig)))
                .toCompletableFuture();
    }

    @Override
    public CompletableFuture<List<TaskPushNotificationConfig>> listTaskPushNotificationConfigurations(
            ListTaskPushNotificationConfigParams request,
            ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);

        ListTaskPushNotificationConfigRequest grpcRequest = ListTaskPushNotificationConfigRequest.newBuilder()
                .setParent("tasks/" + request.id())
                .build();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(io.a2a.spec.ListTaskPushNotificationConfigRequest.METHOD,
                grpcRequest, agentCard, context);

        A2AServiceStub stubWithMetadata = createAsyncStubWithMetadata(context, payloadAndHeaders);
        SingleValueStreamObserver<io.a2a.grpc.ListTaskPushNotificationConfigResponse> observer = new SingleValueStreamObserver<>();
        stubWithMetadata.listTaskPushNotificationConfig(grpcRequest, observer);

        return observer.completionStage()
                .thenCompose(new Function<io.a2a.grpc.ListTaskPushNotificationConfigResponse, CompletionStage<List<TaskPushNotificationConfig>>>() {
                    @Override
                    public CompletionStage<List<TaskPushNotificationConfig>> apply(io.a2a.grpc.ListTaskPushNotificationConfigResponse listTaskPushNotificationConfigResponse) {
                        return CompletableFuture.completedFuture(
                                listTaskPushNotificationConfigResponse.getConfigsList().stream()
                                        .map(FromProto::taskPushNotificationConfig).collect(Collectors.toList()));
                    }
                })
                .toCompletableFuture();

        /*
        try {
            A2AServiceBlockingV2Stub stubWithMetadata = createBlockingStubWithMetadata(context, payloadAndHeaders);
            return stubWithMetadata.listTaskPushNotificationConfig(grpcRequest).getConfigsList().stream()
                    .map(FromProto::taskPushNotificationConfig)
                    ;
        } catch (StatusRuntimeException e) {
            throw GrpcErrorMapper.mapGrpcError(e, "Failed to list task push notification config: ");
        }
         */
    }

    @Override
    public CompletableFuture<Void> deleteTaskPushNotificationConfigurations(DeleteTaskPushNotificationConfigParams request,
                                                         ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);

        DeleteTaskPushNotificationConfigRequest grpcRequest = DeleteTaskPushNotificationConfigRequest.newBuilder()
                .setName(getTaskPushNotificationConfigName(request.id(), request.pushNotificationConfigId()))
                .build();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(io.a2a.spec.DeleteTaskPushNotificationConfigRequest.METHOD,
                grpcRequest, agentCard, context);

        A2AServiceStub stubWithMetadata = createAsyncStubWithMetadata(context, payloadAndHeaders);
        SingleValueStreamObserver<Empty> observer = new SingleValueStreamObserver<>();
        stubWithMetadata.deleteTaskPushNotificationConfig(grpcRequest, observer);

        return observer.completionStage()
                .thenApply((Function<Empty, Void>) empty -> null)
                .toCompletableFuture();
    }

    @Override
    public void resubscribe(TaskIdParams request, Consumer<StreamingEventKind> eventConsumer,
                            Consumer<Throwable> errorConsumer, ClientCallContext context) throws A2AClientException {
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
    public CompletableFuture<AgentCard> getAgentCard(ClientCallContext context) throws A2AClientException {
        // TODO: Determine how to handle retrieving the authenticated extended agent card
        return CompletableFuture.completedFuture(agentCard);
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

    /**
     * Creates gRPC metadata from ClientCallContext headers.
     * Extracts headers like X-A2A-Extensions and sets them as gRPC metadata.
     */
    private Metadata createGrpcMetadata(ClientCallContext context, PayloadAndHeaders payloadAndHeaders) {
        Metadata metadata = new Metadata();
        
        if (context != null && context.getHeaders() != null) {
            // Set X-A2A-Extensions header if present
            String extensionsHeader = context.getHeaders().get(A2AHeaders.X_A2A_EXTENSIONS);
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
    private A2AServiceBlockingV2Stub createBlockingStubWithMetadata(ClientCallContext context,
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
    private A2AServiceStub createAsyncStubWithMetadata(ClientCallContext context,
                                                       PayloadAndHeaders payloadAndHeaders) {
        Metadata metadata = createGrpcMetadata(context, payloadAndHeaders);
        return asyncStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));
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