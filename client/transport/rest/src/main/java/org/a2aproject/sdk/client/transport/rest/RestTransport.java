package org.a2aproject.sdk.client.transport.rest;

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
import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.A2AHttpClientFactory;
import org.a2aproject.sdk.client.http.A2AHttpResponse;
import org.a2aproject.sdk.client.transport.rest.sse.SSEEventListener;
import org.a2aproject.sdk.client.transport.spi.ClientTransport;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallContext;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallInterceptor;
import org.a2aproject.sdk.client.transport.spi.interceptors.PayloadAndHeaders;
import org.a2aproject.sdk.grpc.utils.ProtoUtils;
import org.a2aproject.sdk.jsonrpc.common.json.JsonProcessingException;
import org.a2aproject.sdk.jsonrpc.common.json.JsonUtil;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.spec.A2AClientError;
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
import org.a2aproject.sdk.util.Utils;
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
        org.a2aproject.sdk.grpc.SendMessageRequest.Builder builder = org.a2aproject.sdk.grpc.SendMessageRequest.newBuilder(ProtoUtils.ToProto.sendMessageRequest(messageSendParams));
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(SEND_MESSAGE_METHOD, builder, agentCard, context);
        try {
            String httpResponseBody = sendPostRequest(Utils.buildBaseUrl(agentInterface, messageSendParams.tenant()) + "/message:send", payloadAndHeaders);
            org.a2aproject.sdk.grpc.SendMessageResponse.Builder responseBuilder = org.a2aproject.sdk.grpc.SendMessageResponse.newBuilder();
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
        org.a2aproject.sdk.grpc.SendMessageRequest.Builder builder = org.a2aproject.sdk.grpc.SendMessageRequest.newBuilder(ProtoUtils.ToProto.sendMessageRequest(messageSendParams));
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
        org.a2aproject.sdk.grpc.GetTaskRequest.Builder builder = org.a2aproject.sdk.grpc.GetTaskRequest.newBuilder();
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
            org.a2aproject.sdk.grpc.Task.Builder responseBuilder = org.a2aproject.sdk.grpc.Task.newBuilder();
            JsonFormat.parser().merge(httpResponseBody, responseBuilder);
            return ProtoUtils.FromProto.task(responseBuilder);
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            throw new A2AClientException("Failed to get task: " + e, e);
        }
    }

    @Override
    public Task cancelTask(CancelTaskParams taskIdParams, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("taskIdParams", taskIdParams);
        org.a2aproject.sdk.grpc.CancelTaskRequest.Builder builder = org.a2aproject.sdk.grpc.CancelTaskRequest.newBuilder(ProtoUtils.ToProto.cancelTaskRequest(taskIdParams));
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(CANCEL_TASK_METHOD, builder, agentCard, context);
        try {
            String httpResponseBody = sendPostRequest(Utils.buildBaseUrl(agentInterface, taskIdParams.tenant()) + String.format("/tasks/%1s:cancel", taskIdParams.id()), payloadAndHeaders);
            org.a2aproject.sdk.grpc.Task.Builder responseBuilder = org.a2aproject.sdk.grpc.Task.newBuilder();
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
        org.a2aproject.sdk.grpc.ListTasksRequest.Builder builder = org.a2aproject.sdk.grpc.ListTasksRequest.newBuilder();
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
            org.a2aproject.sdk.grpc.ListTasksResponse.Builder responseBuilder = org.a2aproject.sdk.grpc.ListTasksResponse.newBuilder();
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
            queryParts.add("status=" + request.status());
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
        org.a2aproject.sdk.grpc.TaskPushNotificationConfig.Builder builder
                = ProtoUtils.ToProto.taskPushNotificationConfig(request).toBuilder();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, builder, agentCard, context);
        try {
            String httpResponseBody = sendPostRequest(Utils.buildBaseUrl(agentInterface, request.tenant()) + String.format("/tasks/%1s/pushNotificationConfigs", request.taskId()), payloadAndHeaders);
            org.a2aproject.sdk.grpc.TaskPushNotificationConfig.Builder responseBuilder = org.a2aproject.sdk.grpc.TaskPushNotificationConfig.newBuilder();
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
        org.a2aproject.sdk.grpc.GetTaskPushNotificationConfigRequest.Builder builder
                = org.a2aproject.sdk.grpc.GetTaskPushNotificationConfigRequest.newBuilder();
        StringBuilder url = new StringBuilder(Utils.buildBaseUrl(agentInterface, request.tenant()));
        String configId = request.id();
        if (configId != null && !configId.isEmpty()) {
            builder.setId(configId).setTaskId(request.taskId());
            url.append(String.format("/tasks/%1s/pushNotificationConfigs/%2s", request.taskId(), configId));
        } else {
            // Use trailing slash to distinguish GET from LIST
            builder.setTaskId(request.taskId());
            url.append(String.format("/tasks/%1s/pushNotificationConfigs/", request.taskId()));
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
            org.a2aproject.sdk.grpc.TaskPushNotificationConfig.Builder responseBuilder = org.a2aproject.sdk.grpc.TaskPushNotificationConfig.newBuilder();
            JsonFormat.parser().merge(httpResponseBody, responseBuilder);
            return ProtoUtils.FromProto.taskPushNotificationConfig(responseBuilder);
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            throw new A2AClientException("Failed to get push notifications: " + e, e);
        }
    }

    @Override
    public ListTaskPushNotificationConfigsResult listTaskPushNotificationConfigurations(ListTaskPushNotificationConfigsParams request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsRequest.Builder builder
                = org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsRequest.newBuilder();
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
            org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsResponse.Builder responseBuilder = org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsResponse.newBuilder();
            JsonFormat.parser().merge(httpResponseBody, responseBuilder);
            return ProtoUtils.FromProto.listTaskPushNotificationConfigsResult(responseBuilder);
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            throw new A2AClientException("Failed to list push notifications: " + e, e);
        }
    }

    @Override
    public void deleteTaskPushNotificationConfigurations(DeleteTaskPushNotificationConfigParams request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        org.a2aproject.sdk.grpc.DeleteTaskPushNotificationConfigRequestOrBuilder builder = org.a2aproject.sdk.grpc.DeleteTaskPushNotificationConfigRequest.newBuilder();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, builder,
                agentCard, context);
        try {
            String url = Utils.buildBaseUrl(agentInterface, request.tenant()) + String.format("/tasks/%1s/pushNotificationConfigs/%2s", request.taskId(), request.id());
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
        org.a2aproject.sdk.grpc.SubscribeToTaskRequest.Builder builder = org.a2aproject.sdk.grpc.SubscribeToTaskRequest.newBuilder();
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
    public AgentCard getExtendedAgentCard(GetExtendedAgentCardParams params, @Nullable ClientCallContext context) throws A2AClientException {
        try {
            PayloadAndHeaders payloadAndHeaders = applyInterceptors(GET_EXTENDED_AGENT_CARD_METHOD, null, agentCard, context);
            String url = Utils.buildBaseUrl(agentInterface, params.tenant()) + "/extendedAgentCard";
            A2AHttpClient.GetBuilder getBuilder = httpClient.createGet().url(url);
            for (Map.Entry<String, String> entry : payloadAndHeaders.getHeaders().entrySet()) {
                    getBuilder.addHeader(entry.getKey(), entry.getValue());
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
