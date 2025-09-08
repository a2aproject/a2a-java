package io.a2a.client.transport.rest;

import static io.a2a.util.Assert.checkNotNullParam;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
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
import io.a2a.spec.TaskPushNotificationConfig;
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
import io.a2a.spec.TaskQueryParams;
import io.a2a.grpc.utils.ProtoUtils;
import io.a2a.spec.A2AClientError;
import io.a2a.spec.SendStreamingMessageRequest;
import io.a2a.spec.SetTaskPushNotificationConfigRequest;
import io.a2a.util.Utils;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
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
    public EventKind sendMessage(MessageSendParams messageSendParams, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("messageSendParams", messageSendParams);
        io.a2a.grpc.SendMessageRequest.Builder builder = io.a2a.grpc.SendMessageRequest.newBuilder(ProtoUtils.ToProto.sendMessageRequest(messageSendParams));
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(io.a2a.spec.SendMessageRequest.METHOD, builder, agentCard, context);
        try {
            String httpResponseBody = sendPostRequest("/v1/message:send", payloadAndHeaders);
            io.a2a.grpc.SendMessageResponse.Builder responseBuilder = io.a2a.grpc.SendMessageResponse.newBuilder();
            JsonFormat.parser().merge(httpResponseBody, responseBuilder);
            if (responseBuilder.hasMsg()) {
                return ProtoUtils.FromProto.message(responseBuilder.getMsg());
            }
            if (responseBuilder.hasTask()) {
                return ProtoUtils.FromProto.task(responseBuilder.getTask());
            }
            throw new A2AClientException("Failed to send message, wrong response:" + httpResponseBody);
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new A2AClientException("Failed to send message: " + e, e);
        }
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
        try {
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
        } catch (IOException e) {
            throw new A2AClientException("Failed to send streaming message request: " + e, e);
        }
    }

    @Override
    public Task getTask(TaskQueryParams taskQueryParams, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("taskQueryParams", taskQueryParams);
        GetTaskRequest.Builder builder = GetTaskRequest.newBuilder();
        builder.setName("tasks/" + taskQueryParams.id());
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(io.a2a.spec.GetTaskRequest.METHOD, builder,
                agentCard, context);
        try {
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
            CompletableFuture<HttpResponse> responseFut = getBuilder.send();
            HttpResponse response = responseFut.get();
            if (!response.success()) {
                throw RestErrorMapper.mapRestError(response);
            }
            String httpResponseBody = response.body();
            io.a2a.grpc.Task.Builder responseBuilder = io.a2a.grpc.Task.newBuilder();
            JsonFormat.parser().merge(httpResponseBody, responseBuilder);
            return ProtoUtils.FromProto.task(responseBuilder);
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new A2AClientException("Failed to get task: " + e, e);
        }
    }

    @Override
    public Task cancelTask(TaskIdParams taskIdParams, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("taskIdParams", taskIdParams);
        CancelTaskRequest.Builder builder = CancelTaskRequest.newBuilder();
        builder.setName("tasks/" + taskIdParams.id());
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(io.a2a.spec.CancelTaskRequest.METHOD, builder,
                agentCard, context);
        try {
            String httpResponseBody = sendPostRequest(String.format("/v1/tasks/%1s:cancel", taskIdParams.id()), payloadAndHeaders);
            io.a2a.grpc.Task.Builder responseBuilder = io.a2a.grpc.Task.newBuilder();
            JsonFormat.parser().merge(httpResponseBody, responseBuilder);
            return ProtoUtils.FromProto.task(responseBuilder);
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new A2AClientException("Failed to cancel task: " + e, e);
        }
    }

    @Override
    public TaskPushNotificationConfig setTaskPushNotificationConfiguration(TaskPushNotificationConfig request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        CreateTaskPushNotificationConfigRequest.Builder builder = CreateTaskPushNotificationConfigRequest.newBuilder();
        builder.setConfig(ProtoUtils.ToProto.taskPushNotificationConfig(request))
                .setParent("tasks/" + request.taskId());
        if (request.pushNotificationConfig().id() != null) {
            builder.setConfigId(request.pushNotificationConfig().id());
        }
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(SetTaskPushNotificationConfigRequest.METHOD, builder, agentCard, context);
        try {
            String httpResponseBody = sendPostRequest(String.format("/v1/tasks/%1s/pushNotificationConfigs", request.taskId()), payloadAndHeaders);
            io.a2a.grpc.TaskPushNotificationConfig.Builder responseBuilder = io.a2a.grpc.TaskPushNotificationConfig.newBuilder();
            JsonFormat.parser().merge(httpResponseBody, responseBuilder);
            return ProtoUtils.FromProto.taskPushNotificationConfig(responseBuilder);
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new A2AClientException("Failed to set task push notification config: " + e, e);
        }
    }

    @Override
    public TaskPushNotificationConfig getTaskPushNotificationConfiguration(GetTaskPushNotificationConfigParams request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        GetTaskPushNotificationConfigRequest.Builder builder = GetTaskPushNotificationConfigRequest.newBuilder();
        builder.setName(String.format("/tasks/%1s/pushNotificationConfigs/%2s", request.id(), request.pushNotificationConfigId()));
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(io.a2a.spec.GetTaskPushNotificationConfigRequest.METHOD, builder,
                agentCard, context);
        try {
            String path = String.format("/v1/tasks/%1s/pushNotificationConfigs/%2s", request.id(), request.pushNotificationConfigId());
            HttpClient.GetRequestBuilder getBuilder = httpClient.get(agentPath + path);
            if (payloadAndHeaders.getHeaders() != null) {
                for (Map.Entry<String, String> entry : payloadAndHeaders.getHeaders().entrySet()) {
                    getBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }

            CompletableFuture<HttpResponse> responseFut = getBuilder.send();
            HttpResponse response = responseFut.get();

            if (!response.success()) {
                throw RestErrorMapper.mapRestError(response);
            }
            String httpResponseBody = response.body();
            io.a2a.grpc.TaskPushNotificationConfig.Builder responseBuilder = io.a2a.grpc.TaskPushNotificationConfig.newBuilder();
            JsonFormat.parser().merge(httpResponseBody, responseBuilder);
            return ProtoUtils.FromProto.taskPushNotificationConfig(responseBuilder);
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new A2AClientException("Failed to get push notifications: " + e, e);
        }
    }

    @Override
    public List<TaskPushNotificationConfig> listTaskPushNotificationConfigurations(ListTaskPushNotificationConfigParams request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        ListTaskPushNotificationConfigRequest.Builder builder = ListTaskPushNotificationConfigRequest.newBuilder();
        builder.setParent(String.format("/tasks/%1s/pushNotificationConfigs", request.id()));
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(io.a2a.spec.ListTaskPushNotificationConfigRequest.METHOD, builder,
                agentCard, context);
        try {
            String path = String.format("/v1/tasks/%1s/pushNotificationConfigs", request.id());
            HttpClient.GetRequestBuilder getBuilder = httpClient.get(agentPath + path);
            if (payloadAndHeaders.getHeaders() != null) {
                for (Map.Entry<String, String> entry : payloadAndHeaders.getHeaders().entrySet()) {
                    getBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }
            CompletableFuture<HttpResponse> responseFut = getBuilder.send();
            HttpResponse response = responseFut.get();

            if (!response.success()) {
                throw RestErrorMapper.mapRestError(response);
            }
            String httpResponseBody = response.body();
            io.a2a.grpc.ListTaskPushNotificationConfigResponse.Builder responseBuilder = io.a2a.grpc.ListTaskPushNotificationConfigResponse.newBuilder();
            JsonFormat.parser().merge(httpResponseBody, responseBuilder);
            return ProtoUtils.FromProto.listTaskPushNotificationConfigParams(responseBuilder);
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new A2AClientException("Failed to list push notifications: " + e, e);
        }
    }

    @Override
    public void deleteTaskPushNotificationConfigurations(DeleteTaskPushNotificationConfigParams request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        io.a2a.grpc.DeleteTaskPushNotificationConfigRequestOrBuilder builder = io.a2a.grpc.DeleteTaskPushNotificationConfigRequest.newBuilder();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(io.a2a.spec.DeleteTaskPushNotificationConfigRequest.METHOD, builder,
                agentCard, context);
        try {
            String path = String.format("/v1/tasks/%1s/pushNotificationConfigs/%2s", request.id(), request.pushNotificationConfigId());
            HttpClient.DeleteRequestBuilder deleteBuilder = httpClient.delete(agentPath + path);
            if (payloadAndHeaders.getHeaders() != null) {
                for (Map.Entry<String, String> entry : payloadAndHeaders.getHeaders().entrySet()) {
                    deleteBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }
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
        try {
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
        } catch (IOException e) {
            throw new A2AClientException("Failed to send streaming message request: " + e, e);
        }
    }

    @Override
    public AgentCard getAgentCard(@Nullable ClientCallContext context) throws A2AClientException {
        A2ACardResolver resolver;
        try {
            if (agentCard == null) {
                resolver = new A2ACardResolver(httpClient, agentPath, getHttpHeaders(context));
                agentCard = resolver.getAgentCard();
                needsExtendedCard = agentCard.supportsAuthenticatedExtendedCard();
            }
            if (!needsExtendedCard) {
                return agentCard;
            }
            PayloadAndHeaders payloadAndHeaders = applyInterceptors(io.a2a.spec.GetTaskRequest.METHOD, null,
                    agentCard, context);

            HttpClient.GetRequestBuilder getBuilder = httpClient.get(agentPath + "/v1/card");
            if (payloadAndHeaders.getHeaders() != null) {
                for (Map.Entry<String, String> entry : payloadAndHeaders.getHeaders().entrySet()) {
                    getBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }
            CompletableFuture<HttpResponse> responseFut = getBuilder.send();
            HttpResponse response = responseFut.get();

            if (!response.success()) {
                throw RestErrorMapper.mapRestError(response);
            }
            String httpResponseBody = response.body();
            agentCard = Utils.OBJECT_MAPPER.readValue(httpResponseBody, AgentCard.class);
            needsExtendedCard = false;
            return agentCard;
        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new A2AClientException("Failed to get authenticated extended agent card: " + e, e);
        } catch (A2AClientError e) {
            throw new A2AClientException("Failed to get agent card: " + e, e);
        }
    }

    @Override
    public void close() {
        // no-op
    }

    private String sendPostRequest(String path, PayloadAndHeaders payloadAndHeaders) throws IOException, InterruptedException, ExecutionException {
        HttpClient.PostRequestBuilder builder = createPostBuilder(path, payloadAndHeaders);
        CompletableFuture<HttpResponse> responseFut = builder.send();

        HttpResponse response = responseFut.get();
        if (!response.success()) {
            log.fine("Error on POST processing " + JsonFormat.printer().print((MessageOrBuilder) payloadAndHeaders.getPayload()));
            throw RestErrorMapper.mapRestError(response);
        }
        return response.body();
    }

    private HttpClient.PostRequestBuilder createPostBuilder(String path, PayloadAndHeaders payloadAndHeaders) throws JsonProcessingException, InvalidProtocolBufferException {
        log.fine(JsonFormat.printer().print((MessageOrBuilder) payloadAndHeaders.getPayload()));
        HttpClient.PostRequestBuilder postBuilder = httpClient.post(agentPath + path)
                .addHeader("Content-Type", "application/json")
                .body(JsonFormat.printer().print((MessageOrBuilder) payloadAndHeaders.getPayload()));

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
}
