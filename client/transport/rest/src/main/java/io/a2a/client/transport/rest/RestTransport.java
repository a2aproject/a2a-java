package io.a2a.client.transport.rest;

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

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import io.a2a.client.http.A2AHttpClient;
import io.a2a.client.http.A2AHttpClientFactory;
import io.a2a.client.http.A2AHttpResponse;
import io.a2a.client.transport.rest.sse.SSEEventListener;
import io.a2a.client.transport.spi.ClientTransport;
import io.a2a.client.transport.spi.interceptors.ClientCallContext;
import io.a2a.client.transport.spi.interceptors.ClientCallInterceptor;
import io.a2a.client.transport.spi.interceptors.PayloadAndHeaders;
import io.a2a.grpc.utils.ProtoUtils;
import io.a2a.jsonrpc.common.json.JsonProcessingException;
import io.a2a.jsonrpc.common.json.JsonUtil;
import io.a2a.jsonrpc.common.wrappers.ListTasksResult;
import io.a2a.spec.A2AClientError;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
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
import io.a2a.util.Utils;
import org.jspecify.annotations.Nullable;

public class RestTransport implements ClientTransport {

    private static final Logger log = Logger.getLogger(RestTransport.class.getName());
    private final A2AHttpClient httpClient;
    private final AgentInterface agentInterface;
    private @Nullable final List<ClientCallInterceptor> interceptors;
    private final AgentCard agentCard;

    public RestTransport(AgentCard agentCard) {
        this(null, agentCard, Utils.getFavoriteInterface(agentCard), null);
    }

    public RestTransport(@Nullable A2AHttpClient httpClient, AgentCard agentCard,
            AgentInterface agentInterface, @Nullable List<ClientCallInterceptor> interceptors) {
        this.httpClient = httpClient == null ? A2AHttpClientFactory.create() : httpClient;
        this.agentCard = agentCard;
        this.agentInterface = agentInterface;
        this.interceptors = interceptors;
    }

    @Override
    public EventKind sendMessage(MessageSendParams messageSendParams, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("messageSendParams", messageSendParams);
        io.a2a.grpc.SendMessageRequest.Builder builder = io.a2a.grpc.SendMessageRequest.newBuilder(ProtoUtils.ToProto.sendMessageRequest(messageSendParams));
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(SEND_MESSAGE_METHOD, builder, agentCard, context);
        try {
            String httpResponseBody = sendPostRequest(Utils.buildBaseUrl(agentInterface, messageSendParams.tenant()) + "/message:send", payloadAndHeaders);
            io.a2a.grpc.SendMessageResponse.Builder responseBuilder = io.a2a.grpc.SendMessageResponse.newBuilder();
            JsonFormat.parser().merge(httpResponseBody, responseBuilder);
            if (responseBuilder.hasMessage()) {
                return ProtoUtils.FromProto.message(responseBuilder.getMessage());
            }
            if (responseBuilder.hasTask()) {
                return ProtoUtils.FromProto.task(responseBuilder.getTask());
            }
            throw new A2AClientException("Failed to send message, wrong response:" + httpResponseBody);
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException | JsonProcessingException e) {
            throw new A2AClientException("Failed to send message: " + e, e);
        }
    }

    @Override
    public void sendMessageStreaming(MessageSendParams messageSendParams, Consumer<StreamingEventKind> eventConsumer, Consumer<Throwable> errorConsumer, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", messageSendParams);
        checkNotNullParam("eventConsumer", eventConsumer);
        checkNotNullParam("messageSendParams", messageSendParams);
        io.a2a.grpc.SendMessageRequest.Builder builder = io.a2a.grpc.SendMessageRequest.newBuilder(ProtoUtils.ToProto.sendMessageRequest(messageSendParams));
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(SEND_STREAMING_MESSAGE_METHOD, builder, agentCard, context);
        AtomicReference<CompletableFuture<Void>> ref = new AtomicReference<>();
        SSEEventListener sseEventListener = new SSEEventListener(eventConsumer, errorConsumer);
        try {
            A2AHttpClient.PostBuilder postBuilder = createPostBuilder(Utils.buildBaseUrl(agentInterface, messageSendParams.tenant()) + "/message:stream", payloadAndHeaders);
            ref.set(postBuilder.postAsyncSSE(
                    msg -> sseEventListener.onMessage(msg, ref.get()),
                    throwable -> sseEventListener.onError(throwable, ref.get()),
                    () -> {
                        // We don't need to do anything special on completion
                    }));
        } catch (IOException e) {
            throw new A2AClientException("Failed to send streaming message request: " + e, e);
        } catch (InterruptedException e) {
            throw new A2AClientException("Send streaming message request timed out: " + e, e);
        } catch (JsonProcessingException e) {
            throw new A2AClientException("Failed to process JSON for streaming message request: " + e, e);
        }
    }

    @Override
    public Task getTask(TaskQueryParams taskQueryParams, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("taskQueryParams", taskQueryParams);
        io.a2a.grpc.GetTaskRequest.Builder builder = io.a2a.grpc.GetTaskRequest.newBuilder();
        builder.setId(taskQueryParams.id());
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(GET_TASK_METHOD, builder, agentCard, context);
        try {
            StringBuilder url = new StringBuilder(Utils.buildBaseUrl(agentInterface, taskQueryParams.tenant()));
            if (taskQueryParams.historyLength() != null && taskQueryParams.historyLength() > 0) {
                url.append(String.format("/tasks/%1s?historyLength=%2d", taskQueryParams.id(), taskQueryParams.historyLength()));
            } else {
                url.append(String.format("/tasks/%1s", taskQueryParams.id()));
            }
            A2AHttpClient.GetBuilder getBuilder = httpClient.createGet().url(url.toString());
            if (payloadAndHeaders.getHeaders() != null) {
                for (Map.Entry<String, String> entry : payloadAndHeaders.getHeaders().entrySet()) {
                    getBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }
            A2AHttpResponse response = getBuilder.get();
            if (!response.success()) {
                throw RestErrorMapper.mapRestError(response);
            }
            String httpResponseBody = response.body();
            io.a2a.grpc.Task.Builder responseBuilder = io.a2a.grpc.Task.newBuilder();
            JsonFormat.parser().merge(httpResponseBody, responseBuilder);
            return ProtoUtils.FromProto.task(responseBuilder);
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            throw new A2AClientException("Failed to get task: " + e, e);
        }
    }

    @Override
    public Task cancelTask(TaskIdParams taskIdParams, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("taskIdParams", taskIdParams);
        io.a2a.grpc.CancelTaskRequest.Builder builder = io.a2a.grpc.CancelTaskRequest.newBuilder();
        builder.setId(taskIdParams.id());
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(CANCEL_TASK_METHOD, builder, agentCard, context);
        try {
            String httpResponseBody = sendPostRequest(Utils.buildBaseUrl(agentInterface, taskIdParams.tenant()) + String.format("/tasks/%1s:cancel", taskIdParams.id()), payloadAndHeaders);
            io.a2a.grpc.Task.Builder responseBuilder = io.a2a.grpc.Task.newBuilder();
            JsonFormat.parser().merge(httpResponseBody, responseBuilder);
            return ProtoUtils.FromProto.task(responseBuilder);
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException | JsonProcessingException e) {
            throw new A2AClientException("Failed to cancel task: " + e, e);
        }
    }

    @Override
    public ListTasksResult listTasks(ListTasksParams request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        io.a2a.grpc.ListTasksRequest.Builder builder = io.a2a.grpc.ListTasksRequest.newBuilder();
        if (request.contextId() != null) {
            builder.setContextId(request.contextId());
        }
        if (request.status() != null) {
            builder.setStatus(ProtoUtils.ToProto.taskState(request.status()));
        }
        if (request.pageSize() != null) {
            builder.setPageSize(request.pageSize());
        }
        if (request.pageToken() != null) {
            builder.setPageToken(request.pageToken());
        }
        if (request.historyLength() != null) {
            builder.setHistoryLength(request.historyLength());
        }
        if (request.tenant() != null) {
            builder.setTenant(request.tenant());
        }
        if (request.includeArtifacts() != null && request.includeArtifacts()) {
            builder.setIncludeArtifacts(true);
        }

        PayloadAndHeaders payloadAndHeaders = applyInterceptors(LIST_TASK_METHOD, builder, agentCard, context);

        try {
            // Build query string
            StringBuilder urlBuilder = new StringBuilder(Utils.buildBaseUrl(agentInterface, request.tenant()));
            urlBuilder.append("/tasks");
            String queryParams = buildListTasksQueryString(request);
            if (!queryParams.isEmpty()) {
                urlBuilder.append("?").append(queryParams);
            }

            A2AHttpClient.GetBuilder getBuilder = httpClient.createGet().url(urlBuilder.toString());
            if (payloadAndHeaders.getHeaders() != null) {
                for (Map.Entry<String, String> entry : payloadAndHeaders.getHeaders().entrySet()) {
                    getBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }
            A2AHttpResponse response = getBuilder.get();
            if (!response.success()) {
                throw RestErrorMapper.mapRestError(response);
            }
            String httpResponseBody = response.body();
            io.a2a.grpc.ListTasksResponse.Builder responseBuilder = io.a2a.grpc.ListTasksResponse.newBuilder();
            JsonFormat.parser().merge(httpResponseBody, responseBuilder);

            return new ListTasksResult(
                    responseBuilder.getTasksList().stream()
                            .map(ProtoUtils.FromProto::task)
                            .toList(),
                    responseBuilder.getTotalSize(),
                    responseBuilder.getTasksCount(),
                    responseBuilder.getNextPageToken().isEmpty() ? null : responseBuilder.getNextPageToken()
            );
        } catch (IOException | InterruptedException e) {
            throw new A2AClientException("Failed to list tasks: " + e, e);
        }
    }

    
    private String buildListTasksQueryString(ListTasksParams request) {
        java.util.List<String> queryParts = new java.util.ArrayList<>();
        if (request.contextId() != null) {
            queryParts.add("contextId=" + URLEncoder.encode(request.contextId(), StandardCharsets.UTF_8));
        }
        if (request.status() != null) {
            queryParts.add("status=" + request.status().asString());
        }
        if (request.pageSize() != null) {
            queryParts.add("pageSize=" + request.pageSize());
        }
        if (request.pageToken() != null) {
            queryParts.add("pageToken=" + URLEncoder.encode(request.pageToken(), StandardCharsets.UTF_8));
        }
        if (request.historyLength() != null) {
            queryParts.add("historyLength=" + request.historyLength());
        }
        if (request.includeArtifacts() != null && request.includeArtifacts()) {
            queryParts.add("includeArtifacts=true");
        }
        return String.join("&", queryParts);
    }

    @Override
    public TaskPushNotificationConfig createTaskPushNotificationConfiguration(TaskPushNotificationConfig request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        io.a2a.grpc.CreateTaskPushNotificationConfigRequest.Builder builder
                = io.a2a.grpc.CreateTaskPushNotificationConfigRequest.newBuilder();
        builder.setConfig(ProtoUtils.ToProto.taskPushNotificationConfig(request).getPushNotificationConfig())
                .setTaskId(request.taskId());
        if (request.pushNotificationConfig().id() != null) {
            builder.setConfigId(request.pushNotificationConfig().id());
        }
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, builder, agentCard, context);
        try {
            String httpResponseBody = sendPostRequest(Utils.buildBaseUrl(agentInterface, request.tenant()) + String.format("/tasks/%1s/pushNotificationConfigs", request.taskId()), payloadAndHeaders);
            io.a2a.grpc.TaskPushNotificationConfig.Builder responseBuilder = io.a2a.grpc.TaskPushNotificationConfig.newBuilder();
            JsonFormat.parser().merge(httpResponseBody, responseBuilder);
            return ProtoUtils.FromProto.taskPushNotificationConfig(responseBuilder);
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException | JsonProcessingException e) {
            throw new A2AClientException("Failed to set task push notification config: " + e, e);
        }
    }

    @Override
    public TaskPushNotificationConfig getTaskPushNotificationConfiguration(GetTaskPushNotificationConfigParams request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        io.a2a.grpc.GetTaskPushNotificationConfigRequest.Builder builder
                = io.a2a.grpc.GetTaskPushNotificationConfigRequest.newBuilder();
        StringBuilder url = new StringBuilder(Utils.buildBaseUrl(agentInterface, request.tenant()));
        String configId = request.pushNotificationConfigId();
        if (configId != null && !configId.isEmpty()) {
            builder.setId(configId).setTaskId(request.id());
            url.append(String.format("/tasks/%1s/pushNotificationConfigs/%2s", request.id(), configId));
        } else {
            // Use trailing slash to distinguish GET from LIST
            builder.setTaskId(request.id());
            url.append(String.format("/tasks/%1s/pushNotificationConfigs/", request.id()));
        }
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, builder,
                agentCard, context);
        try {
            A2AHttpClient.GetBuilder getBuilder = httpClient.createGet().url(url.toString());
            if (payloadAndHeaders.getHeaders() != null) {
                for (Map.Entry<String, String> entry : payloadAndHeaders.getHeaders().entrySet()) {
                    getBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }
            A2AHttpResponse response = getBuilder.get();
            if (!response.success()) {
                throw RestErrorMapper.mapRestError(response);
            }
            String httpResponseBody = response.body();
            io.a2a.grpc.TaskPushNotificationConfig.Builder responseBuilder = io.a2a.grpc.TaskPushNotificationConfig.newBuilder();
            JsonFormat.parser().merge(httpResponseBody, responseBuilder);
            return ProtoUtils.FromProto.taskPushNotificationConfig(responseBuilder);
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            throw new A2AClientException("Failed to get push notifications: " + e, e);
        }
    }

    @Override
    public ListTaskPushNotificationConfigResult listTaskPushNotificationConfigurations(ListTaskPushNotificationConfigParams request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        io.a2a.grpc.ListTaskPushNotificationConfigRequest.Builder builder
                = io.a2a.grpc.ListTaskPushNotificationConfigRequest.newBuilder();
        builder.setTaskId(request.id());
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, builder,
                agentCard, context);
        try {
            String url = Utils.buildBaseUrl(agentInterface, request.tenant()) + String.format("/tasks/%1s/pushNotificationConfigs", request.id());
            A2AHttpClient.GetBuilder getBuilder = httpClient.createGet().url(url);
            if (payloadAndHeaders.getHeaders() != null) {
                for (Map.Entry<String, String> entry : payloadAndHeaders.getHeaders().entrySet()) {
                    getBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }
            A2AHttpResponse response = getBuilder.get();
            if (!response.success()) {
                throw RestErrorMapper.mapRestError(response);
            }
            String httpResponseBody = response.body();
            io.a2a.grpc.ListTaskPushNotificationConfigResponse.Builder responseBuilder = io.a2a.grpc.ListTaskPushNotificationConfigResponse.newBuilder();
            JsonFormat.parser().merge(httpResponseBody, responseBuilder);
            return ProtoUtils.FromProto.listTaskPushNotificationConfigResult(responseBuilder);
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            throw new A2AClientException("Failed to list push notifications: " + e, e);
        }
    }

    @Override
    public void deleteTaskPushNotificationConfigurations(DeleteTaskPushNotificationConfigParams request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        io.a2a.grpc.DeleteTaskPushNotificationConfigRequestOrBuilder builder = io.a2a.grpc.DeleteTaskPushNotificationConfigRequest.newBuilder();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, builder,
                agentCard, context);
        try {
            String url = Utils.buildBaseUrl(agentInterface, request.tenant()) + String.format("/tasks/%1s/pushNotificationConfigs/%2s", request.id(), request.pushNotificationConfigId());
            A2AHttpClient.DeleteBuilder deleteBuilder = httpClient.createDelete().url(url);
            if (payloadAndHeaders.getHeaders() != null) {
                for (Map.Entry<String, String> entry : payloadAndHeaders.getHeaders().entrySet()) {
                    deleteBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }
            A2AHttpResponse response = deleteBuilder.delete();
            if (!response.success()) {
                throw RestErrorMapper.mapRestError(response);
            }
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            throw new A2AClientException("Failed to delete push notification config: " + e, e);
        }
    }

    @Override
    public void subscribeToTask(TaskIdParams request, Consumer<StreamingEventKind> eventConsumer,
            Consumer<Throwable> errorConsumer, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        io.a2a.grpc.SubscribeToTaskRequest.Builder builder = io.a2a.grpc.SubscribeToTaskRequest.newBuilder();
        builder.setId(request.id());
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(SUBSCRIBE_TO_TASK_METHOD, builder,
                agentCard, context);
        AtomicReference<CompletableFuture<Void>> ref = new AtomicReference<>();
        SSEEventListener sseEventListener = new SSEEventListener(eventConsumer, errorConsumer);
        try {
            String url = Utils.buildBaseUrl(agentInterface, request.tenant()) + String.format("/tasks/%1s:subscribe", request.id());
            A2AHttpClient.PostBuilder postBuilder = createPostBuilder(url, payloadAndHeaders);
            ref.set(postBuilder.postAsyncSSE(
                    msg -> sseEventListener.onMessage(msg, ref.get()),
                    throwable -> sseEventListener.onError(throwable, ref.get()),
                    () -> {
                        // We don't need to do anything special on completion
                    }));
        } catch (IOException e) {
            throw new A2AClientException("Failed to send streaming message request: " + e, e);
        } catch (InterruptedException e) {
            throw new A2AClientException("Send streaming message request timed out: " + e, e);
        } catch (JsonProcessingException e) {
            throw new A2AClientException("Failed to process JSON for streaming message request: " + e, e);
        }
    }

    @Override
    public AgentCard getExtendedAgentCard(@Nullable ClientCallContext context) throws A2AClientException {
        try {
            PayloadAndHeaders payloadAndHeaders = applyInterceptors(GET_EXTENDED_AGENT_CARD_METHOD, null, agentCard, context);
            String url = Utils.buildBaseUrl(agentInterface, "") + "/extendedAgentCard";
            A2AHttpClient.GetBuilder getBuilder = httpClient.createGet().url(url);
            if (payloadAndHeaders.getHeaders() != null) {
                for (Map.Entry<String, String> entry : payloadAndHeaders.getHeaders().entrySet()) {
                    getBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }
            A2AHttpResponse response = getBuilder.get();
            if (!response.success()) {
                throw RestErrorMapper.mapRestError(response);
            }
            String httpResponseBody = response.body();
            return JsonUtil.fromJson(httpResponseBody, AgentCard.class);
        } catch (IOException | InterruptedException | JsonProcessingException e) {
            throw new A2AClientException("Failed to get authenticated extended agent card: " + e, e);
        } catch (A2AClientError e) {
            throw new A2AClientException("Failed to get agent card: " + e, e);
        }
    }

    @Override
    public void close() {
        // no-op
    }

    private PayloadAndHeaders applyInterceptors(String methodName, @Nullable MessageOrBuilder payload,
            AgentCard agentCard, @Nullable ClientCallContext clientCallContext) {
        PayloadAndHeaders payloadAndHeaders = new PayloadAndHeaders(payload, getHttpHeaders(clientCallContext));
        if (interceptors != null && !interceptors.isEmpty()) {
            for (ClientCallInterceptor interceptor : interceptors) {
                payloadAndHeaders = interceptor.intercept(methodName, payloadAndHeaders.getPayload(),
                        payloadAndHeaders.getHeaders(), agentCard, clientCallContext);
            }
        }
        return payloadAndHeaders;
    }

    private String sendPostRequest(String url, PayloadAndHeaders payloadAndHeaders) throws IOException, InterruptedException, JsonProcessingException {
        A2AHttpClient.PostBuilder builder = createPostBuilder(url, payloadAndHeaders);
        A2AHttpResponse response = builder.post();
        if (!response.success()) {
            log.fine("Error on POST processing " + JsonFormat.printer().print((MessageOrBuilder) payloadAndHeaders.getPayload()));
            throw RestErrorMapper.mapRestError(response);
        }
        return response.body();
    }

    private A2AHttpClient.PostBuilder createPostBuilder(String url, PayloadAndHeaders payloadAndHeaders) throws JsonProcessingException, InvalidProtocolBufferException {
        log.fine(JsonFormat.printer().print((MessageOrBuilder) payloadAndHeaders.getPayload()));
        A2AHttpClient.PostBuilder postBuilder = httpClient.createPost()
                .url(url)
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
