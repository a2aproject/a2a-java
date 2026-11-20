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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.A2AHttpClientConfiguration;
import org.a2aproject.sdk.client.http.A2AHttpClientFactory;
import org.a2aproject.sdk.client.http.A2AHttpResponse;
import org.a2aproject.sdk.common.A2AHeaders;
import org.a2aproject.sdk.client.transport.rest.sse.SSEEventListener;
import org.a2aproject.sdk.client.transport.spi.ClientTransport;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallContext;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallInterceptor;
import org.a2aproject.sdk.client.transport.spi.interceptors.PayloadAndHeaders;
import org.a2aproject.sdk.grpc.utils.ProtoJsonUtils;
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
import org.a2aproject.sdk.spec.util.Utils;
import org.jspecify.annotations.Nullable;

public class RestTransport implements ClientTransport {

    private static final Logger LOGGER = Logger.getLogger(RestTransport.class.getName());
    private final A2AHttpClient httpClient;
    private final AgentInterface agentInterface;
    private @Nullable final List<ClientCallInterceptor> interceptors;
    private final AgentCard agentCard;
    private final boolean followRedirects;

    public RestTransport(AgentCard agentCard) {
        this(null, agentCard, Utils.getFavoriteInterface(agentCard), null);
    }

    public RestTransport(@Nullable A2AHttpClient httpClient, AgentCard agentCard,
            AgentInterface agentInterface, @Nullable List<ClientCallInterceptor> interceptors) {
        this(httpClient, agentCard, agentInterface, interceptors, false);
    }

    public RestTransport(@Nullable A2AHttpClient httpClient, AgentCard agentCard,
            AgentInterface agentInterface, @Nullable List<ClientCallInterceptor> interceptors,
            boolean followRedirects) {
        this.httpClient = httpClient == null ? A2AHttpClientFactory.create(A2AHttpClientConfiguration.DEFAULT) : httpClient;
        this.agentCard = agentCard;
        this.agentInterface = agentInterface;
        this.interceptors = interceptors;
        this.followRedirects = followRedirects;
    }

    @Override
    public EventKind sendMessage(MessageSendParams messageSendParams, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("messageSendParams", messageSendParams);
        org.a2aproject.sdk.grpc.SendMessageRequest.Builder builder = org.a2aproject.sdk.grpc.SendMessageRequest.newBuilder(ProtoUtils.ToProto.sendMessageRequest(messageSendParams));
        var originalPayload = builder.build();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(SEND_MESSAGE_METHOD, builder, agentCard, context);
        try {
            String httpResponseBody = sendPostRequest(Utils.buildBaseUrl(agentInterface, ((org.a2aproject.sdk.grpc.SendMessageRequestOrBuilder) Objects.requireNonNullElse(payloadAndHeaders.getPayload(), originalPayload)).getTenant()) + "/message:send", payloadAndHeaders);
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
        var originalPayload = builder.build();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(SEND_STREAMING_MESSAGE_METHOD, builder, agentCard, context);
        AtomicReference<CompletableFuture<Void>> ref = new AtomicReference<>();
        SSEEventListener sseEventListener = new SSEEventListener(eventConsumer, errorConsumer);
        try {
            A2AHttpClient.PostBuilder postBuilder = createPostBuilder(
                    Utils.buildBaseUrl(agentInterface, ((org.a2aproject.sdk.grpc.SendMessageRequestOrBuilder) Objects.requireNonNullElse(payloadAndHeaders.getPayload(), originalPayload)).getTenant()) + "/message:stream",
                    payloadAndHeaders,
                    false);
            ref.set(postBuilder.postAsyncSSE(
                    event -> sseEventListener.onMessage(event, ref.get()),
                    throwable -> sseEventListener.onError(throwable, ref.get()),
                    () -> {
                        // Signal normal stream completion to error handler (null error means success)
                        sseEventListener.onComplete();
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
        if (taskQueryParams.historyLength() != null) {
            builder.setHistoryLength(taskQueryParams.historyLength());
        }
        if (taskQueryParams.tenant() != null) {
            builder.setTenant(taskQueryParams.tenant());
        }
        var originalPayload = builder.build();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(GET_TASK_METHOD, builder, agentCard, context);
        try {
            org.a2aproject.sdk.grpc.GetTaskRequestOrBuilder effective = (org.a2aproject.sdk.grpc.GetTaskRequestOrBuilder) Objects.requireNonNullElse(payloadAndHeaders.getPayload(), originalPayload);
            StringBuilder url = new StringBuilder(Utils.buildBaseUrl(agentInterface, effective.getTenant()));
            if (effective.hasHistoryLength()) {
                url.append(String.format("/tasks/%s?historyLength=%d", effective.getId(), effective.getHistoryLength()));
            } else {
                url.append(String.format("/tasks/%1s", effective.getId()));
            }
            A2AHttpClient.GetBuilder getBuilder = httpClient.createGet().url(url.toString());
            addStandardHeaders(getBuilder, payloadAndHeaders);
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
        var originalPayload = builder.build();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(CANCEL_TASK_METHOD, builder, agentCard, context);
        try {
            org.a2aproject.sdk.grpc.CancelTaskRequestOrBuilder effective = (org.a2aproject.sdk.grpc.CancelTaskRequestOrBuilder) Objects.requireNonNullElse(payloadAndHeaders.getPayload(), originalPayload);
            String httpResponseBody = sendPostRequest(Utils.buildBaseUrl(agentInterface, effective.getTenant()) + String.format("/tasks/%1s:cancel", effective.getId()), payloadAndHeaders);
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
        if (request.includeArtifacts() != null) {
            builder.setIncludeArtifacts(request.includeArtifacts());
        }
        if (request.statusTimestampAfter() != null) {
            builder.setStatusTimestampAfter(com.google.protobuf.Timestamp.newBuilder()
                    .setSeconds(request.statusTimestampAfter().getEpochSecond())
                    .setNanos(request.statusTimestampAfter().getNano())
                    .build());
        }

        var originalPayload = builder.build();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(LIST_TASK_METHOD, builder, agentCard, context);

        try {
            // Build query string
            org.a2aproject.sdk.grpc.ListTasksRequestOrBuilder effective = (org.a2aproject.sdk.grpc.ListTasksRequestOrBuilder) Objects.requireNonNullElse(payloadAndHeaders.getPayload(), originalPayload);
            StringBuilder urlBuilder = new StringBuilder(Utils.buildBaseUrl(agentInterface, effective.getTenant()));
            urlBuilder.append("/tasks");
            String queryParams = buildListTasksQueryString(effective, request);
            if (!queryParams.isEmpty()) {
                urlBuilder.append("?").append(queryParams);
            }

            A2AHttpClient.GetBuilder getBuilder = httpClient.createGet().url(urlBuilder.toString());
            addStandardHeaders(getBuilder, payloadAndHeaders);
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

    
    private String buildListTasksQueryString(org.a2aproject.sdk.grpc.ListTasksRequestOrBuilder request, ListTasksParams original) {
        java.util.List<String> queryParts = new java.util.ArrayList<>();
        if (!request.getContextId().isEmpty() || "".equals(original.contextId())) {
            queryParts.add("contextId=" + URLEncoder.encode(request.getContextId(), StandardCharsets.UTF_8));
        }
        if (request.getStatus() != org.a2aproject.sdk.grpc.TaskState.TASK_STATE_UNSPECIFIED
                || original.status() == org.a2aproject.sdk.spec.TaskState.TASK_STATE_UNSPECIFIED) {
            queryParts.add("status=" + URLEncoder.encode(request.getStatus().toString(), StandardCharsets.UTF_8));
        }
        if (request.hasPageSize()) {
            queryParts.add("pageSize=" + request.getPageSize());
        }
        if (!request.getPageToken().isEmpty() || "".equals(original.pageToken())) {
            queryParts.add("pageToken=" + URLEncoder.encode(request.getPageToken(), StandardCharsets.UTF_8));
        }
        if (request.hasHistoryLength()) {
            queryParts.add("historyLength=" + request.getHistoryLength());
        }
        if (request.hasStatusTimestampAfter()) {
            var timestamp = request.getStatusTimestampAfter();
            var instant = java.time.Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
            queryParts.add("statusTimestampAfter=" + URLEncoder.encode(instant.toString(), StandardCharsets.UTF_8));
        }
        if (request.hasIncludeArtifacts()) {
            queryParts.add("includeArtifacts=" + request.getIncludeArtifacts());
        }
        return String.join("&", queryParts);
    }

    private String buildListPushNotificationConfigsQueryString(ListTaskPushNotificationConfigsParams request) {
        java.util.List<String> queryParts = new java.util.ArrayList<>();
        if (request.pageSize() > 0) {
            queryParts.add("pageSize=" + request.pageSize());
        }
        if (request.pageToken() != null && !request.pageToken().isEmpty()) {
            queryParts.add("pageToken=" + URLEncoder.encode(request.pageToken(), StandardCharsets.UTF_8));
        }
        return String.join("&", queryParts);
    }

    @Override
    public TaskPushNotificationConfig createTaskPushNotificationConfiguration(TaskPushNotificationConfig request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        org.a2aproject.sdk.grpc.TaskPushNotificationConfig.Builder builder
                = ProtoUtils.ToProto.taskPushNotificationConfig(request).toBuilder();
        var originalPayload = builder.build();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, builder, agentCard, context);
        try {
            org.a2aproject.sdk.grpc.TaskPushNotificationConfigOrBuilder effective = (org.a2aproject.sdk.grpc.TaskPushNotificationConfigOrBuilder) Objects.requireNonNullElse(payloadAndHeaders.getPayload(), originalPayload);
            String httpResponseBody = sendPostRequest(Utils.buildBaseUrl(agentInterface, effective.getTenant()) + String.format("/tasks/%1s/pushNotificationConfigs", effective.getTaskId()), payloadAndHeaders);
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
        builder.setTaskId(request.taskId());
        if (request.id() != null) {
            builder.setId(request.id());
        }
        if (request.tenant() != null) {
            builder.setTenant(request.tenant());
        }
        var originalPayload = builder.build();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, builder,
                agentCard, context);
        try {
            org.a2aproject.sdk.grpc.GetTaskPushNotificationConfigRequestOrBuilder effective = (org.a2aproject.sdk.grpc.GetTaskPushNotificationConfigRequestOrBuilder) Objects.requireNonNullElse(payloadAndHeaders.getPayload(), originalPayload);
            StringBuilder url = new StringBuilder(Utils.buildBaseUrl(agentInterface, effective.getTenant()));
            String configId = effective.getId();
            if (!configId.isEmpty()) {
                url.append(String.format("/tasks/%1s/pushNotificationConfigs/%2s", effective.getTaskId(), configId));
            } else {
                url.append(String.format("/tasks/%1s/pushNotificationConfigs/", effective.getTaskId()));
            }
            A2AHttpClient.GetBuilder getBuilder = httpClient.createGet().url(url.toString());
            addStandardHeaders(getBuilder, payloadAndHeaders);
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
        if (request.tenant() != null) {
            builder.setTenant(request.tenant());
        }
        if (request.pageSize() > 0) {
            builder.setPageSize(request.pageSize());
        }
        if (request.pageToken() != null && !request.pageToken().isEmpty()) {
            builder.setPageToken(request.pageToken());
        }
        var originalPayload = builder.build();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, builder,
                agentCard, context);
        try {
            org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsRequestOrBuilder effective = (org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsRequestOrBuilder) Objects.requireNonNullElse(payloadAndHeaders.getPayload(), originalPayload);
            StringBuilder urlBuilder = new StringBuilder(Utils.buildBaseUrl(agentInterface, effective.getTenant()));
            urlBuilder.append(String.format("/tasks/%1s/pushNotificationConfigs", effective.getTaskId()));
            String queryString = buildListPushNotificationConfigsQueryString(ProtoUtils.FromProto.listTaskPushNotificationConfigsParams(effective));
            if (!queryString.isEmpty()) {
                urlBuilder.append("?").append(queryString);
            }
            A2AHttpClient.GetBuilder getBuilder = httpClient.createGet().url(urlBuilder.toString());
            addStandardHeaders(getBuilder, payloadAndHeaders);
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
        org.a2aproject.sdk.grpc.DeleteTaskPushNotificationConfigRequest.Builder builder = org.a2aproject.sdk.grpc.DeleteTaskPushNotificationConfigRequest.newBuilder().setTaskId(request.taskId()).setId(request.id());
        if (request.tenant() != null) {
            builder.setTenant(request.tenant());
        }
        var originalPayload = builder.build();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, builder,
                agentCard, context);
        try {
            org.a2aproject.sdk.grpc.DeleteTaskPushNotificationConfigRequestOrBuilder effective = (org.a2aproject.sdk.grpc.DeleteTaskPushNotificationConfigRequestOrBuilder) Objects.requireNonNullElse(payloadAndHeaders.getPayload(), originalPayload);
            String url = Utils.buildBaseUrl(agentInterface, effective.getTenant()) + String.format("/tasks/%1s/pushNotificationConfigs/%2s", effective.getTaskId(), effective.getId());
            A2AHttpClient.DeleteBuilder deleteBuilder = httpClient.createDelete().url(url);
            addStandardHeaders(deleteBuilder, payloadAndHeaders);
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
        if (request.tenant() != null) {
            builder.setTenant(request.tenant());
        }
        var originalPayload = builder.build();
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(SUBSCRIBE_TO_TASK_METHOD, builder,
                agentCard, context);
        AtomicReference<CompletableFuture<Void>> ref = new AtomicReference<>();
        SSEEventListener sseEventListener = new SSEEventListener(eventConsumer, errorConsumer);
        try {
            org.a2aproject.sdk.grpc.SubscribeToTaskRequestOrBuilder effective = (org.a2aproject.sdk.grpc.SubscribeToTaskRequestOrBuilder) Objects.requireNonNullElse(payloadAndHeaders.getPayload(), originalPayload);
            String url = Utils.buildBaseUrl(agentInterface, effective.getTenant()) + String.format("/tasks/%1s:subscribe", effective.getId());
            A2AHttpClient.PostBuilder postBuilder = createPostBuilder(url, payloadAndHeaders, false);
            ref.set(postBuilder.postAsyncSSE(
                    event -> sseEventListener.onMessage(event, ref.get()),
                    throwable -> sseEventListener.onError(throwable, ref.get()),
                    () -> {
                        // Signal normal stream completion to error handler (null error means success)
                        sseEventListener.onComplete();
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
            org.a2aproject.sdk.grpc.GetExtendedAgentCardRequest.Builder builder = org.a2aproject.sdk.grpc.GetExtendedAgentCardRequest.newBuilder();
            if (params.tenant() != null) {
                builder.setTenant(params.tenant());
            }
            var originalPayload = builder.build();
            PayloadAndHeaders payloadAndHeaders = applyInterceptors(GET_EXTENDED_AGENT_CARD_METHOD, builder, agentCard, context);
            org.a2aproject.sdk.grpc.GetExtendedAgentCardRequestOrBuilder effective = (org.a2aproject.sdk.grpc.GetExtendedAgentCardRequestOrBuilder) Objects.requireNonNullElse(payloadAndHeaders.getPayload(), originalPayload);
            String url = Utils.buildBaseUrl(agentInterface, effective.getTenant()) + "/extendedAgentCard";
            A2AHttpClient.GetBuilder getBuilder = httpClient.createGet().url(url);
            addStandardHeaders(getBuilder, payloadAndHeaders);
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
        MessageOrBuilder payload = (MessageOrBuilder) payloadAndHeaders.getPayload();
        String body = payload != null ? ProtoJsonUtils.toJson(JsonFormat.printer(), payload) : "";
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(body);
        }
        A2AHttpClient.PostBuilder builder = buildPost(url, payloadAndHeaders, body, followRedirects);
        A2AHttpResponse response = builder.post();
        if (!response.success()) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Error on POST processing " + body);
            }
            throw RestErrorMapper.mapRestError(response);
        }
        return response.body();
    }

    private A2AHttpClient.PostBuilder createPostBuilder(
            String url, PayloadAndHeaders payloadAndHeaders, boolean followRedirects)
            throws JsonProcessingException, InvalidProtocolBufferException {
        String body = ProtoJsonUtils.toJson(JsonFormat.printer(), (MessageOrBuilder) payloadAndHeaders.getPayload());
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(body);
        }
        return buildPost(url, payloadAndHeaders, body, followRedirects);
    }

    private A2AHttpClient.PostBuilder buildPost(
            String url, PayloadAndHeaders payloadAndHeaders, String body, boolean followRedirects)
            throws JsonProcessingException {
        A2AHttpClient.PostBuilder postBuilder = httpClient.createPost()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader(A2AHeaders.A2A_VERSION, AgentInterface.CURRENT_PROTOCOL_VERSION)
                .followRedirects(followRedirects)
                .body(body);

        if (payloadAndHeaders.getHeaders() != null) {
            for (Map.Entry<String, String> entry : payloadAndHeaders.getHeaders().entrySet()) {
                postBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        return postBuilder;
    }

    private static <T extends A2AHttpClient.Builder<T>> void addStandardHeaders(T builder, PayloadAndHeaders payloadAndHeaders) {
        builder.addHeader(A2AHeaders.A2A_VERSION, AgentInterface.CURRENT_PROTOCOL_VERSION);
        if (payloadAndHeaders.getHeaders() != null) {
            for (Map.Entry<String, String> entry : payloadAndHeaders.getHeaders().entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
    }

    private Map<String, String> getHttpHeaders(@Nullable ClientCallContext context) {
        return context != null ? context.getHeaders() : Collections.emptyMap();
    }
}
