package io.a2a.client.transport.rest;

import static io.a2a.util.Assert.checkNotNullParam;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.a2a.client.http.A2ACardResolver;
import io.a2a.client.http.HttpClient;
import io.a2a.client.http.HttpResponse;
import io.a2a.client.transport.rest.sse.RestSSEEventListener;
import io.a2a.client.transport.spi.AbstractClientTransport;
import io.a2a.client.transport.spi.interceptors.ClientCallContext;
import io.a2a.client.transport.spi.interceptors.ClientCallInterceptor;
import io.a2a.client.transport.spi.interceptors.PayloadAndHeaders;
import io.a2a.grpc.CancelTaskRequest;
import io.a2a.grpc.CreateTaskPushNotificationConfigRequest;
import io.a2a.grpc.GetTaskPushNotificationConfigRequest;
import io.a2a.grpc.GetTaskRequest;
import io.a2a.grpc.ListTaskPushNotificationConfigRequest;
import io.a2a.spec.*;
import io.a2a.grpc.utils.ProtoUtils;
import io.a2a.util.Utils;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

public class RestTransport extends AbstractClientTransport {

    private static final Logger log = Logger.getLogger(RestTransport.class.getName());
    private final HttpClient httpClient;
    private final String agentPath;
    private @Nullable AgentCard agentCard;
    private boolean needsExtendedCard = false;

    public RestTransport(String agentUrl) {
        this(null, null, agentUrl, null);
    }

    public RestTransport(@Nullable HttpClient httpClient, @Nullable AgentCard agentCard,
            String agentUrl, @Nullable List<ClientCallInterceptor> interceptors) {
        super(interceptors);
        this.httpClient = httpClient == null ? HttpClient.createHttpClient(agentUrl) : httpClient;
        this.agentCard = agentCard;
        String sAgentPath = URI.create(agentUrl).getPath();

        // Strip the last slash if one is provided
        if (sAgentPath.endsWith("/")) {
            this.agentPath = sAgentPath.substring(0, sAgentPath.length() - 1);
        } else {
            this.agentPath = sAgentPath;
        }
    }

    @Override
    public CompletableFuture<EventKind> sendMessage(MessageSendParams messageSendParams, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("messageSendParams", messageSendParams);
        io.a2a.grpc.SendMessageRequest.Builder builder = io.a2a.grpc.SendMessageRequest.newBuilder(ProtoUtils.ToProto.sendMessageRequest(messageSendParams));
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(io.a2a.spec.SendMessageRequest.METHOD, builder, agentCard, context);
        return sendPostRequest("/v1/message:send", payloadAndHeaders)
                    .thenCompose(new Function<String, CompletionStage<EventKind>>() {
                        @Override
                        public CompletionStage<EventKind> apply(String httpResponseBody) {
                            io.a2a.grpc.SendMessageResponse.Builder responseBuilder = io.a2a.grpc.SendMessageResponse.newBuilder();
                            try {
                                JsonFormat.parser().merge(httpResponseBody, responseBuilder);
                            } catch (InvalidProtocolBufferException e) {
                                return CompletableFuture.failedFuture(new A2AClientException("Failed to send message: " + e, e));
                            }

                            if (responseBuilder.hasMsg()) {
                                return CompletableFuture.completedFuture(ProtoUtils.FromProto.message(responseBuilder.getMsg()));
                            }
                            if (responseBuilder.hasTask()) {
                                return CompletableFuture.completedFuture(ProtoUtils.FromProto.task(responseBuilder.getTask()));
                            }

                            return CompletableFuture.failedFuture(new A2AClientException("Failed to send message, wrong response:" + httpResponseBody));
                        }
                    });
    }

    @Override
    public void sendMessageStreaming(MessageSendParams messageSendParams, Consumer<StreamingEventKind> eventConsumer, Consumer<Throwable> errorConsumer, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", messageSendParams);
        checkNotNullParam("eventConsumer", eventConsumer);
        checkNotNullParam("messageSendParams", messageSendParams);
        io.a2a.grpc.SendMessageRequest.Builder builder = io.a2a.grpc.SendMessageRequest.newBuilder(ProtoUtils.ToProto.sendMessageRequest(messageSendParams));
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(SendStreamingMessageRequest.METHOD,
                builder, agentCard, context);
        AtomicReference<CompletableFuture<HttpResponse>> ref = new AtomicReference<>();
        RestSSEEventListener sseEventListener = new RestSSEEventListener(eventConsumer, errorConsumer);
    //    try {
            HttpClient.PostRequestBuilder postBuilder = createPostBuilder("/v1/message:stream", payloadAndHeaders).asSSE();
            ref.set(postBuilder.send().whenComplete(new BiConsumer<HttpResponse, Throwable>() {
                @Override
                public void accept(HttpResponse httpResponse, Throwable throwable) {
                    if (httpResponse != null) {
                        httpResponse.bodyAsSse(
                                msg -> sseEventListener.onMessage(msg, ref.get()),
                                cause -> sseEventListener.onError(cause, ref.get()));
                    } else {
                        errorConsumer.accept(throwable);
                    }
                }
            }));
            /*
        } catch (IOException e) {
            throw new A2AClientException("Failed to send streaming message request: " + e, e);
        }

             */
    }

    @Override
    public CompletableFuture<Task> getTask(TaskQueryParams taskQueryParams, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("taskQueryParams", taskQueryParams);
        GetTaskRequest.Builder builder = GetTaskRequest.newBuilder();
        builder.setName("tasks/" + taskQueryParams.id());
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(io.a2a.spec.GetTaskRequest.METHOD, builder,
                agentCard, context);

        String path;
        if (taskQueryParams.historyLength() != null) {
            path = String.format("/v1/tasks/%1s?historyLength=%2d", taskQueryParams.id(), taskQueryParams.historyLength());
        } else {
            path = String.format("/v1/tasks/%1s", taskQueryParams.id());
        }
        HttpClient.GetRequestBuilder getBuilder = httpClient.get(agentPath + path);
        if (payloadAndHeaders.getHeaders() != null) {
            for (Map.Entry<String, String> entry : payloadAndHeaders.getHeaders().entrySet()) {
                getBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        return getBuilder.send()
                .thenCompose(new Function<HttpResponse, CompletionStage<String>>() {
                    @Override
                    public CompletionStage<String> apply(HttpResponse response) {
                        if (!response.success()) {
                            return RestErrorMapper.mapRestError(response);
                        }

                        return response.body();
                    }
                }).thenCompose(new Function<String, CompletionStage<Task>>() {
                    @Override
                    public CompletionStage<Task> apply(String httpResponseBody) {
                        io.a2a.grpc.Task.Builder responseBuilder = io.a2a.grpc.Task.newBuilder();
                        try {
                            JsonFormat.parser().merge(httpResponseBody, responseBuilder);
                            return CompletableFuture.completedFuture(ProtoUtils.FromProto.task(responseBuilder));
                        } catch (InvalidProtocolBufferException e) {
                            return CompletableFuture.failedFuture(new A2AClientException("Failed to get task: " + e, e));
                        }
                    }
                });
    }

    @Override
    public CompletableFuture<Task> cancelTask(TaskIdParams taskIdParams, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("taskIdParams", taskIdParams);
        CancelTaskRequest.Builder builder = CancelTaskRequest.newBuilder();
        builder.setName("tasks/" + taskIdParams.id());
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(io.a2a.spec.CancelTaskRequest.METHOD, builder,
                agentCard, context);

        return sendPostRequest(String.format("/v1/tasks/%1s:cancel", taskIdParams.id()), payloadAndHeaders)
                .thenCompose(new Function<String, CompletionStage<Task>>() {
                    @Override
                    public CompletionStage<Task> apply(String httpResponseBody) {
                        io.a2a.grpc.Task.Builder responseBuilder = io.a2a.grpc.Task.newBuilder();
                        try {
                            JsonFormat.parser().merge(httpResponseBody, responseBuilder);
                            return CompletableFuture.completedFuture(ProtoUtils.FromProto.task(responseBuilder));
                        } catch (InvalidProtocolBufferException e) {
                            return CompletableFuture.failedFuture(new A2AClientException("Failed to cancel task: " + e, e));
                        }
                    }
                });
    }

    @Override
    public CompletableFuture<TaskPushNotificationConfig> setTaskPushNotificationConfiguration(TaskPushNotificationConfig request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        CreateTaskPushNotificationConfigRequest.Builder builder = CreateTaskPushNotificationConfigRequest.newBuilder();
        builder.setConfig(ProtoUtils.ToProto.taskPushNotificationConfig(request))
                .setParent("tasks/" + request.taskId());
        if (request.pushNotificationConfig().id() != null) {
            builder.setConfigId(request.pushNotificationConfig().id());
        }
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(SetTaskPushNotificationConfigRequest.METHOD, builder, agentCard, context);

        return sendPostRequest(String.format("/v1/tasks/%1s/pushNotificationConfigs", request.taskId()), payloadAndHeaders)
                .thenCompose(new Function<String, CompletionStage<TaskPushNotificationConfig>>() {
                    @Override
                    public CompletionStage<TaskPushNotificationConfig> apply(String httpResponseBody) {
                        io.a2a.grpc.TaskPushNotificationConfig.Builder responseBuilder = io.a2a.grpc.TaskPushNotificationConfig.newBuilder();
                        try {
                            JsonFormat.parser().merge(httpResponseBody, responseBuilder);
                            return CompletableFuture.completedFuture(ProtoUtils.FromProto.taskPushNotificationConfig(responseBuilder));
                        } catch (InvalidProtocolBufferException e) {
                            return CompletableFuture.failedFuture(new A2AClientException("Failed to set task push notification config: " + e, e));
                        }
                    }
                });
    }

    @Override
    public CompletableFuture<TaskPushNotificationConfig> getTaskPushNotificationConfiguration(GetTaskPushNotificationConfigParams request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        GetTaskPushNotificationConfigRequest.Builder builder = GetTaskPushNotificationConfigRequest.newBuilder();
        builder.setName(String.format("/tasks/%1s/pushNotificationConfigs/%2s", request.id(), request.pushNotificationConfigId()));
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(io.a2a.spec.GetTaskPushNotificationConfigRequest.METHOD, builder,
                agentCard, context);

        String path = String.format("/v1/tasks/%1s/pushNotificationConfigs/%2s", request.id(), request.pushNotificationConfigId());
        HttpClient.GetRequestBuilder getBuilder = httpClient.get(agentPath + path);
        if (payloadAndHeaders.getHeaders() != null) {
            for (Map.Entry<String, String> entry : payloadAndHeaders.getHeaders().entrySet()) {
                getBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        return getBuilder.send()
                .thenCompose(new Function<HttpResponse, CompletionStage<String>>() {
                    @Override
                    public CompletionStage<String> apply(HttpResponse response) {
                        if (!response.success()) {
                            if (!response.success()) {
                                return RestErrorMapper.mapRestError(response);
                            }
                        }

                        return response.body();
                    }
                }).thenCompose(new Function<String, CompletionStage<TaskPushNotificationConfig>>() {
                    @Override
                    public CompletionStage<TaskPushNotificationConfig> apply(String httpResponseBody) {
                        io.a2a.grpc.TaskPushNotificationConfig.Builder responseBuilder = io.a2a.grpc.TaskPushNotificationConfig.newBuilder();
                        try {
                            JsonFormat.parser().merge(httpResponseBody, responseBuilder);
                            return CompletableFuture.completedFuture(ProtoUtils.FromProto.taskPushNotificationConfig(responseBuilder));
                        } catch (InvalidProtocolBufferException e) {
                            return CompletableFuture.failedFuture(new A2AClientException("Failed to get push notifications: " + e, e));
                        }
                    }
                });
    }

    @Override
    public CompletableFuture<List<TaskPushNotificationConfig>> listTaskPushNotificationConfigurations(ListTaskPushNotificationConfigParams request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        ListTaskPushNotificationConfigRequest.Builder builder = ListTaskPushNotificationConfigRequest.newBuilder();
        builder.setParent(String.format("/tasks/%1s/pushNotificationConfigs", request.id()));
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(io.a2a.spec.ListTaskPushNotificationConfigRequest.METHOD, builder,
                agentCard, context);

        String path = String.format("/v1/tasks/%1s/pushNotificationConfigs", request.id());
        HttpClient.GetRequestBuilder getBuilder = httpClient.get(agentPath + path);
        if (payloadAndHeaders.getHeaders() != null) {
            for (Map.Entry<String, String> entry : payloadAndHeaders.getHeaders().entrySet()) {
                getBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        return getBuilder.send()
                .thenCompose(new Function<HttpResponse, CompletionStage<String>>() {
                    @Override
                    public CompletionStage<String> apply(HttpResponse response) {
                        if (!response.success()) {
                            if (!response.success()) {
                                return RestErrorMapper.mapRestError(response);
                            }
                        }

                        return response.body();
                    }
                }).thenCompose(new Function<String, CompletionStage<List<TaskPushNotificationConfig>>>() {
                    @Override
                    public CompletionStage<List<TaskPushNotificationConfig>> apply(String httpResponseBody) {
                        io.a2a.grpc.ListTaskPushNotificationConfigResponse.Builder responseBuilder = io.a2a.grpc.ListTaskPushNotificationConfigResponse.newBuilder();
                        try {
                            JsonFormat.parser().merge(httpResponseBody, responseBuilder);
                            return CompletableFuture.completedFuture(ProtoUtils.FromProto.listTaskPushNotificationConfigParams(responseBuilder));
                        } catch (InvalidProtocolBufferException e) {
                            return CompletableFuture.failedFuture(new A2AClientException("Failed to list push notifications: " + e, e));
                        }
                    }
                });
    }

    @Override
    public CompletableFuture<Void> deleteTaskPushNotificationConfigurations(DeleteTaskPushNotificationConfigParams request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        io.a2a.grpc.DeleteTaskPushNotificationConfigRequestOrBuilder builder = io.a2a.grpc.DeleteTaskPushNotificationConfigRequest.newBuilder();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(io.a2a.spec.DeleteTaskPushNotificationConfigRequest.METHOD, builder,
                agentCard, context);

        String path = String.format("/v1/tasks/%1s/pushNotificationConfigs/%2s", request.id(), request.pushNotificationConfigId());
        HttpClient.DeleteRequestBuilder deleteBuilder = httpClient.delete(agentPath + path);
        if (payloadAndHeaders.getHeaders() != null) {
            for (Map.Entry<String, String> entry : payloadAndHeaders.getHeaders().entrySet()) {
                deleteBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        return deleteBuilder
                .send()
                .thenCompose(new Function<HttpResponse, CompletionStage<String>>() {
                    @Override
                    public CompletionStage<String> apply(HttpResponse response) {
                        if (!response.success()) {
                            if (!response.success()) {
                                return RestErrorMapper.mapRestError(response);
                            }
                        }

                        return response.body();
                    }
                })
                .thenApply(s -> null);
        /*
        try {

            CompletableFuture<HttpResponse> responseFut = deleteBuilder.send();
            HttpResponse response = responseFut.get();

            if (!response.success()) {
                throw RestErrorMapper.mapRestError(response);
            }
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new A2AClientException("Failed to delete push notification config: " + e, e);
        }
         */
    }

    @Override
    public void resubscribe(TaskIdParams request, Consumer<StreamingEventKind> eventConsumer,
            Consumer<Throwable> errorConsumer, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        io.a2a.grpc.TaskSubscriptionRequest.Builder builder = io.a2a.grpc.TaskSubscriptionRequest.newBuilder();
        builder.setName("tasks/" + request.id());
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(io.a2a.spec.TaskResubscriptionRequest.METHOD, builder,
                agentCard, context);
        AtomicReference<CompletableFuture<HttpResponse>> ref = new AtomicReference<>();
        RestSSEEventListener sseEventListener = new RestSSEEventListener(eventConsumer, errorConsumer);
        // try {
            String path = String.format("/v1/tasks/%1s:subscribe", request.id());
            HttpClient.PostRequestBuilder postBuilder = createPostBuilder(path, payloadAndHeaders).asSSE();
            ref.set(postBuilder.send().whenComplete(new BiConsumer<HttpResponse, Throwable>() {
                @Override
                public void accept(HttpResponse httpResponse, Throwable throwable) {
                    if (httpResponse != null) {
                        httpResponse.bodyAsSse(
                                msg -> sseEventListener.onMessage(msg, ref.get()),
                                cause -> sseEventListener.onError(cause, ref.get()));
                    } else {
                        errorConsumer.accept(throwable);
                    }
                }
            }));
            /*
        } catch (IOException e) {
            throw new A2AClientException("Failed to send streaming message request: " + e, e);
        }
             */
    }

    @Override
    public CompletableFuture<AgentCard> getAgentCard(@Nullable ClientCallContext context) throws A2AClientException {
        A2ACardResolver resolver;

            if (agentCard == null) {
                try {
                    resolver = new A2ACardResolver(httpClient, agentPath, getHttpHeaders(context));
                    agentCard = resolver.getAgentCard();
                    needsExtendedCard = agentCard.supportsAuthenticatedExtendedCard();
                } catch (A2AClientError e) {
                    return CompletableFuture.failedFuture(new A2AClientException("Failed to get agent card: " + e, e));
                }
            }
            if (!needsExtendedCard) {
                return CompletableFuture.completedFuture(agentCard);
            }
            PayloadAndHeaders payloadAndHeaders = applyInterceptors(io.a2a.spec.GetTaskRequest.METHOD, null,
                    agentCard, context);

            HttpClient.GetRequestBuilder getBuilder = httpClient.get(agentPath + "/v1/card");
            if (payloadAndHeaders.getHeaders() != null) {
                for (Map.Entry<String, String> entry : payloadAndHeaders.getHeaders().entrySet()) {
                    getBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }
            return getBuilder.send()
                    .thenCompose(new Function<HttpResponse, CompletionStage<String>>() {
                        @Override
                        public CompletionStage<String> apply(HttpResponse response) {
                            if (!response.success()) {
                                return RestErrorMapper.mapRestError(response);
                            }

                            return response.body();
                        }
                    }).thenCompose(new Function<String, CompletionStage<AgentCard>>() {
                        @Override
                        public CompletionStage<AgentCard> apply(String httpResponseBody) {
                            try {
                                return CompletableFuture.completedFuture(Utils.OBJECT_MAPPER.readValue(httpResponseBody, AgentCard.class));
                            } catch (JsonProcessingException e) {
                                return CompletableFuture.failedFuture(new A2AClientException("Failed to get authenticated extended agent card: " + e, e));
                            }
                        }
                    }).whenComplete(new BiConsumer<AgentCard, Throwable>() {
                        @Override
                        public void accept(AgentCard agentCard, Throwable throwable) {
                            RestTransport.this.agentCard = agentCard;
                            needsExtendedCard = false;
                        }
                    });
    }

    @Override
    public void close() {
        // no-op
    }

    private CompletableFuture<String> sendPostRequest(String path, PayloadAndHeaders payloadAndHeaders) {
        return createPostBuilder(path, payloadAndHeaders)
                .send()
                .thenCompose(new Function<HttpResponse, CompletionStage<String>>() {
                    @Override
                    public CompletionStage<String> apply(HttpResponse response) {
                        if (!response.success()) {
                            log.fine("Error on POST processing " + convertToJsonString(payloadAndHeaders.getPayload()));
                            return RestErrorMapper.mapRestError(response);
                        }

                        return response.body();
                    }
                });
    }

    private HttpClient.PostRequestBuilder createPostBuilder(String path, PayloadAndHeaders payloadAndHeaders) {
        log.fine(convertToJsonString(payloadAndHeaders.getPayload()));
        HttpClient.PostRequestBuilder postBuilder = httpClient.post(agentPath + path)
                .addHeader("Content-Type", "application/json")
                .body(convertToJsonString(payloadAndHeaders.getPayload()));

        if (payloadAndHeaders.getHeaders() != null) {
            for (Map.Entry<String, String> entry : payloadAndHeaders.getHeaders().entrySet()) {
                postBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        return postBuilder;
    }

    private Map<String, String> getHttpHeaders(@Nullable ClientCallContext context) {
        return context != null ? context.getHeaders() : Collections.emptyMap();
    }

    private @Nullable String convertToJsonString(@Nullable Object obj) {
        if (obj != null) {
            try {
                return JsonFormat.printer().print((com.google.protobuf.MessageOrBuilder) obj);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return null;
    }
}
