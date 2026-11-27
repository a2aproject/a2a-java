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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.A2AHttpClientConfiguration;
import org.a2aproject.sdk.client.http.A2AHttpClientFactory;
import org.a2aproject.sdk.client.http.A2AHttpClientFactory.ClientSelection;
import org.a2aproject.sdk.client.http.A2AHttpResponse;
import org.a2aproject.sdk.client.http.HttpEndpoint;
import org.a2aproject.sdk.client.http.HttpStreamLifecycle;
import org.a2aproject.sdk.client.transport.rest.sse.SSEEventListener;
import org.a2aproject.sdk.client.transport.spi.ClientTransport;
import org.a2aproject.sdk.client.transport.spi.HttpTransportSecurity.PostRedirectPolicy;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallContext;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallInterceptor;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientRoutingInterceptor;
import org.a2aproject.sdk.client.transport.spi.interceptors.PayloadAndHeaders;
import org.a2aproject.sdk.common.A2AHeaders;
import org.a2aproject.sdk.grpc.CancelTaskRequest;
import org.a2aproject.sdk.grpc.CancelTaskRequestOrBuilder;
import org.a2aproject.sdk.grpc.DeleteTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.grpc.DeleteTaskPushNotificationConfigRequestOrBuilder;
import org.a2aproject.sdk.grpc.GetExtendedAgentCardRequest;
import org.a2aproject.sdk.grpc.GetExtendedAgentCardRequestOrBuilder;
import org.a2aproject.sdk.grpc.GetTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.grpc.GetTaskPushNotificationConfigRequestOrBuilder;
import org.a2aproject.sdk.grpc.GetTaskRequest;
import org.a2aproject.sdk.grpc.GetTaskRequestOrBuilder;
import org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsRequest;
import org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsRequestOrBuilder;
import org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsResponse;
import org.a2aproject.sdk.grpc.ListTasksRequest;
import org.a2aproject.sdk.grpc.ListTasksRequestOrBuilder;
import org.a2aproject.sdk.grpc.ListTasksResponse;
import org.a2aproject.sdk.grpc.SendMessageRequest;
import org.a2aproject.sdk.grpc.SendMessageRequestOrBuilder;
import org.a2aproject.sdk.grpc.SendMessageResponse;
import org.a2aproject.sdk.grpc.SubscribeToTaskRequest;
import org.a2aproject.sdk.grpc.SubscribeToTaskRequestOrBuilder;
import org.a2aproject.sdk.grpc.TaskPushNotificationConfigOrBuilder;
import org.a2aproject.sdk.grpc.TaskState;
import org.a2aproject.sdk.grpc.utils.ProtoJsonUtils;
import org.a2aproject.sdk.grpc.utils.ProtoUtils;
import org.a2aproject.sdk.grpc.utils.RequestTenant;
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
import org.a2aproject.sdk.util.InterceptorPayloads;
import org.jspecify.annotations.Nullable;

public class RestTransport implements ClientTransport, AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(RestTransport.class.getName());
    private final A2AHttpClient httpClient;
    private final AgentInterface agentInterface;
    private @Nullable final List<ClientCallInterceptor> interceptors;
    private final AgentCard agentCard;
    private final PostRedirectPolicy postRedirectPolicy;
    private final String credentialDestination;

    private final HttpStreamLifecycle streams;

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
        this(httpClient, agentCard, agentInterface, interceptors, followRedirects,
                () -> A2AHttpClientFactory.createWithOwnership(A2AHttpClientConfiguration.DEFAULT));
    }

    RestTransport(@Nullable A2AHttpClient httpClient, AgentCard agentCard, AgentInterface agentInterface,
            @Nullable List<ClientCallInterceptor> interceptors, boolean followRedirects,
            Supplier<ClientSelection> defaultClientFactory) {
        this.agentInterface = agentInterface;
        this.credentialDestination = HttpEndpoint.normalizeRest(agentInterface.url());
        var selected = httpClient == null ? defaultClientFactory.get() : new ClientSelection(httpClient, false);
        this.httpClient = selected.client();
        this.streams = new HttpStreamLifecycle(selected);
        this.agentCard = agentCard;
        this.interceptors = interceptors == null ? null : List.copyOf(interceptors);
        this.postRedirectPolicy = new PostRedirectPolicy(followRedirects);
    }

    @Override
    public EventKind sendMessage(MessageSendParams messageSendParams, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("messageSendParams", messageSendParams);
        SendMessageRequest.Builder builder = SendMessageRequest.newBuilder(ProtoUtils.ToProto.sendMessageRequest(messageSendParams));
        var call = prepareRequest(messageSendParams.tenant(), SEND_MESSAGE_METHOD, builder, context);
        String baseUrl = call.baseUrl();
        PayloadAndHeaders payloadAndHeaders = call.payloadAndHeaders();
        try {
            InterceptorPayloads.requireType(payloadAndHeaders.getPayload(), SendMessageRequestOrBuilder.class);
            String httpResponseBody = sendPostRequest(baseUrl + "/message:send", payloadAndHeaders);
            SendMessageResponse.Builder responseBuilder = SendMessageResponse.newBuilder();
            ProtoJsonUtils.mergeIgnoringUnknownFields(httpResponseBody, responseBuilder);
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
        SendMessageRequest.Builder builder = SendMessageRequest.newBuilder(ProtoUtils.ToProto.sendMessageRequest(messageSendParams));
        var call = prepareRequest(messageSendParams.tenant(), SEND_STREAMING_MESSAGE_METHOD, builder, context);
        String baseUrl = call.baseUrl();
        PayloadAndHeaders payloadAndHeaders = call.payloadAndHeaders();
        AtomicReference<CompletableFuture<Void>> ref = new AtomicReference<>();
        SSEEventListener sseEventListener = new SSEEventListener(eventConsumer, errorConsumer);
        try {
            InterceptorPayloads.requireType(payloadAndHeaders.getPayload(), SendMessageRequestOrBuilder.class);
            A2AHttpClient.PostBuilder postBuilder = createPostBuilder(
                    baseUrl + "/message:stream",
                    payloadAndHeaders,
                    false);
            var streamFuture = streams.track(postBuilder.postAsyncSSE(
                    event -> sseEventListener.onMessage(event, ref.get()),
                    throwable -> sseEventListener.onError(throwable, ref.get()),
                    () -> {
                        // Signal normal stream completion to error handler (null error means success)
                        sseEventListener.onComplete();
                    }), sseEventListener::onCancel);
            if (streamFuture != null) {
                ref.set(streamFuture);
            }
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
        HttpEndpoint.checkPathSegment(taskQueryParams.id());
        GetTaskRequest.Builder builder = GetTaskRequest.newBuilder();
        builder.setId(taskQueryParams.id());
        if (taskQueryParams.historyLength() != null) {
            builder.setHistoryLength(taskQueryParams.historyLength());
        }
        var call = prepareRequest(taskQueryParams.tenant(), GET_TASK_METHOD, builder, context);
        String baseUrl = call.baseUrl();
        PayloadAndHeaders payloadAndHeaders = call.payloadAndHeaders();
        try {
            var effective = InterceptorPayloads.cast(payloadAndHeaders.getPayload(),
                    GetTaskRequestOrBuilder.class);
            String url = baseUrl + "/tasks/" + HttpEndpoint.pathSegment(effective.getId());
            if (effective.hasHistoryLength()) {
                url += "?historyLength=" + effective.getHistoryLength();
            }
            A2AHttpClient.GetBuilder getBuilder = httpClient.createGet().url(url);
            addStandardHeaders(getBuilder, payloadAndHeaders);
            A2AHttpResponse response = getBuilder.get();
            if (!response.success()) {
                throw RestErrorMapper.mapRestError(response);
            }
            String httpResponseBody = response.body();
            org.a2aproject.sdk.grpc.Task.Builder responseBuilder = org.a2aproject.sdk.grpc.Task.newBuilder();
            ProtoJsonUtils.mergeIgnoringUnknownFields(httpResponseBody, responseBuilder);
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
        HttpEndpoint.checkPathSegment(taskIdParams.id());
        CancelTaskRequest.Builder builder = CancelTaskRequest.newBuilder(ProtoUtils.ToProto.cancelTaskRequest(taskIdParams));
        var call = prepareRequest(taskIdParams.tenant(), CANCEL_TASK_METHOD, builder, context);
        String baseUrl = call.baseUrl();
        PayloadAndHeaders payloadAndHeaders = call.payloadAndHeaders();
        try {
            var effective = InterceptorPayloads.cast(payloadAndHeaders.getPayload(),
                    CancelTaskRequestOrBuilder.class);
            String url = baseUrl + "/tasks/" + HttpEndpoint.pathSegment(effective.getId()) + ":cancel";
            String httpResponseBody = sendPostRequest(url, payloadAndHeaders);
            org.a2aproject.sdk.grpc.Task.Builder responseBuilder = org.a2aproject.sdk.grpc.Task.newBuilder();
            ProtoJsonUtils.mergeIgnoringUnknownFields(httpResponseBody, responseBuilder);
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
        ListTasksRequest.Builder builder = ListTasksRequest.newBuilder();
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
            builder.setStatusTimestampAfter(Timestamp.newBuilder()
                    .setSeconds(request.statusTimestampAfter().getEpochSecond())
                    .setNanos(request.statusTimestampAfter().getNano())
                    .build());
        }

        var call = prepareRequest(request.tenant(), LIST_TASK_METHOD, builder, context);
        String baseUrl = call.baseUrl();
        PayloadAndHeaders payloadAndHeaders = call.payloadAndHeaders();

        try {
            // Build query string
            ListTasksRequestOrBuilder effective = InterceptorPayloads.cast(payloadAndHeaders.getPayload(),
                    ListTasksRequestOrBuilder.class);
            StringBuilder urlBuilder = new StringBuilder(baseUrl);
            urlBuilder.append("/tasks");
            String queryParams = buildListTasksQueryString(effective);
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
            ListTasksResponse.Builder responseBuilder = ListTasksResponse.newBuilder();
            ProtoJsonUtils.mergeIgnoringUnknownFields(httpResponseBody, responseBuilder);

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


    private String buildListTasksQueryString(ListTasksRequestOrBuilder request) {
        List<String> queryParts = new ArrayList<>();
        if (!request.getContextId().isEmpty()) {
            queryParts.add("contextId=" + URLEncoder.encode(request.getContextId(), StandardCharsets.UTF_8));
        }
        if (request.getStatus() != TaskState.TASK_STATE_UNSPECIFIED) {
            queryParts.add("status=" + URLEncoder.encode(request.getStatus().toString(), StandardCharsets.UTF_8));
        }
        addPagination(queryParts, request.hasPageSize() ? request.getPageSize() : null, request.getPageToken());
        if (request.hasHistoryLength()) {
            queryParts.add("historyLength=" + request.getHistoryLength());
        }
        if (request.hasStatusTimestampAfter()) {
            var timestamp = request.getStatusTimestampAfter();
            var instant = Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
            queryParts.add("statusTimestampAfter=" + URLEncoder.encode(instant.toString(), StandardCharsets.UTF_8));
        }
        if (request.hasIncludeArtifacts()) {
            queryParts.add("includeArtifacts=" + request.getIncludeArtifacts());
        }
        return String.join("&", queryParts);
    }

    private String buildListPushNotificationConfigsQueryString(ListTaskPushNotificationConfigsRequestOrBuilder request) {
        List<String> queryParts = new ArrayList<>();
        addPagination(queryParts, request.getPageSize() > 0 ? request.getPageSize() : null, request.getPageToken());
        return String.join("&", queryParts);
    }

    private static void addPagination(List<String> queryParts, @Nullable Integer pageSize, String pageToken) {
        if (pageSize != null) {
            queryParts.add("pageSize=" + pageSize);
        }
        if (!pageToken.isEmpty()) {
            queryParts.add("pageToken=" + URLEncoder.encode(pageToken, StandardCharsets.UTF_8));
        }
    }

    @Override
    public TaskPushNotificationConfig createTaskPushNotificationConfiguration(TaskPushNotificationConfig request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        HttpEndpoint.checkPathSegment(request.taskId());
        org.a2aproject.sdk.grpc.TaskPushNotificationConfig.Builder builder
                = ProtoUtils.ToProto.taskPushNotificationConfig(request).toBuilder();
        var call = prepareRequest(request.tenant(), SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, builder, context);
        String baseUrl = call.baseUrl();
        PayloadAndHeaders payloadAndHeaders = call.payloadAndHeaders();
        try {
            var effective = InterceptorPayloads.cast(payloadAndHeaders.getPayload(),
                    TaskPushNotificationConfigOrBuilder.class);
            String url = baseUrl + "/tasks/" + HttpEndpoint.pathSegment(effective.getTaskId()) + "/pushNotificationConfigs";
            String httpResponseBody = sendPostRequest(url, payloadAndHeaders);
            org.a2aproject.sdk.grpc.TaskPushNotificationConfig.Builder responseBuilder = org.a2aproject.sdk.grpc.TaskPushNotificationConfig.newBuilder();
            ProtoJsonUtils.mergeIgnoringUnknownFields(httpResponseBody, responseBuilder);
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
        HttpEndpoint.checkPathSegment(request.taskId());
        GetTaskPushNotificationConfigRequest.Builder builder
                = GetTaskPushNotificationConfigRequest.newBuilder();
        builder.setTaskId(request.taskId());
        String configId = request.id();
        if (configId != null && !configId.isEmpty()) {
            HttpEndpoint.checkPathSegment(configId);
            builder.setId(configId);
        }
        var call = prepareRequest(request.tenant(), GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, builder, context);
        try {
            var effective = InterceptorPayloads.cast(call.payloadAndHeaders().getPayload(),
                    GetTaskPushNotificationConfigRequestOrBuilder.class);
            String url = call.baseUrl() + "/tasks/" + HttpEndpoint.pathSegment(effective.getTaskId())
                    + "/pushNotificationConfigs/";
            if (!effective.getId().isEmpty()) {
                url += HttpEndpoint.pathSegment(effective.getId());
            }
            A2AHttpClient.GetBuilder getBuilder = httpClient.createGet().url(url);
            addStandardHeaders(getBuilder, call.payloadAndHeaders());
            A2AHttpResponse response = getBuilder.get();
            if (!response.success()) {
                throw RestErrorMapper.mapRestError(response);
            }
            String httpResponseBody = response.body();
            org.a2aproject.sdk.grpc.TaskPushNotificationConfig.Builder responseBuilder = org.a2aproject.sdk.grpc.TaskPushNotificationConfig.newBuilder();
            ProtoJsonUtils.mergeIgnoringUnknownFields(httpResponseBody, responseBuilder);
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
        HttpEndpoint.checkPathSegment(request.id());
        ListTaskPushNotificationConfigsRequest.Builder builder
                = ListTaskPushNotificationConfigsRequest.newBuilder();
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
        var call = prepareRequest(request.tenant(), LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, builder, context);
        String baseUrl = call.baseUrl();
        PayloadAndHeaders payloadAndHeaders = call.payloadAndHeaders();
        try {
            var effective = InterceptorPayloads.cast(payloadAndHeaders.getPayload(),
                    ListTaskPushNotificationConfigsRequestOrBuilder.class);
            StringBuilder urlBuilder = new StringBuilder(baseUrl).append("/tasks/")
                    .append(HttpEndpoint.pathSegment(effective.getTaskId())).append("/pushNotificationConfigs");
            String queryString = buildListPushNotificationConfigsQueryString(effective);
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
            ListTaskPushNotificationConfigsResponse.Builder responseBuilder = ListTaskPushNotificationConfigsResponse.newBuilder();
            ProtoJsonUtils.mergeIgnoringUnknownFields(httpResponseBody, responseBuilder);
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
        HttpEndpoint.checkPathSegment(request.taskId());
        HttpEndpoint.checkPathSegment(request.id());
        var builder = DeleteTaskPushNotificationConfigRequest.newBuilder()
                .setTaskId(request.taskId()).setId(request.id());
        var call = prepareRequest(request.tenant(), DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, builder, context);
        String baseUrl = call.baseUrl();
        PayloadAndHeaders payloadAndHeaders = call.payloadAndHeaders();
        try {
            var effective = InterceptorPayloads.cast(payloadAndHeaders.getPayload(),
                    DeleteTaskPushNotificationConfigRequestOrBuilder.class);
            String url = baseUrl + "/tasks/" + HttpEndpoint.pathSegment(effective.getTaskId())
                    + "/pushNotificationConfigs/" + HttpEndpoint.pathSegment(effective.getId());
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
        HttpEndpoint.checkPathSegment(request.id());
        SubscribeToTaskRequest.Builder builder = SubscribeToTaskRequest.newBuilder();
        builder.setId(request.id());
        var call = prepareRequest(request.tenant(), SUBSCRIBE_TO_TASK_METHOD, builder, context);
        String baseUrl = call.baseUrl();
        PayloadAndHeaders payloadAndHeaders = call.payloadAndHeaders();
        AtomicReference<CompletableFuture<Void>> ref = new AtomicReference<>();
        SSEEventListener sseEventListener = new SSEEventListener(eventConsumer, errorConsumer);
        try {
            var effective = InterceptorPayloads.cast(payloadAndHeaders.getPayload(),
                    SubscribeToTaskRequestOrBuilder.class);
            String url = baseUrl + "/tasks/" + HttpEndpoint.pathSegment(effective.getId()) + ":subscribe";
            A2AHttpClient.PostBuilder postBuilder = createPostBuilder(url, payloadAndHeaders, false);
            var streamFuture = streams.track(postBuilder.postAsyncSSE(
                    event -> sseEventListener.onMessage(event, ref.get()),
                    throwable -> sseEventListener.onError(throwable, ref.get()),
                    () -> {
                        // Signal normal stream completion to error handler (null error means success)
                        sseEventListener.onComplete();
                    }), sseEventListener::onCancel);
            if (streamFuture != null) {
                ref.set(streamFuture);
            }
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
            GetExtendedAgentCardRequest.Builder builder = GetExtendedAgentCardRequest.newBuilder();
            if (params.tenant() != null) {
                builder.setTenant(params.tenant());
            }
            var call = prepareRequest(params.tenant(), GET_EXTENDED_AGENT_CARD_METHOD, builder, context);
            InterceptorPayloads.requireType(call.payloadAndHeaders().getPayload(),
                    GetExtendedAgentCardRequestOrBuilder.class);
            A2AHttpClient.GetBuilder getBuilder = httpClient.createGet().url(call.baseUrl() + "/extendedAgentCard");
            addStandardHeaders(getBuilder, call.payloadAndHeaders());
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

    /**
     * Cancels active streams and releases the HTTP client created by this transport, if closeable.
     * Each interrupted stream receives a cancellation error through its terminal callback.
     * Subsequent close calls have no effect.
     */
    @Override
    public void close() {
        streams.close();
    }

    private record RoutedRequest(String baseUrl, PayloadAndHeaders payloadAndHeaders) {
    }

    private RoutedRequest prepareRequest(@Nullable String requestTenant, String methodName, Message.Builder payload,
            @Nullable ClientCallContext context) {
        String initialTenant = requestTenant == null || requestTenant.isBlank() ? agentInterface.tenant() : requestTenant;
        String tenant = ClientRoutingInterceptor.selectTenant(interceptors, credentialDestination, methodName,
                initialTenant, agentCard, context);
        String tenantPath = tenant == null ? "" : "/" + tenant;
        String baseUrl = credentialDestination.endsWith(tenantPath) ? credentialDestination : credentialDestination + tenantPath;
        RequestTenant.set(payload, tenant);
        var call = new PayloadAndHeaders(payload, getHttpHeaders(context));
        if (interceptors != null) {
            for (var interceptor : interceptors) {
                if (interceptor instanceof ClientRoutingInterceptor) {
                    continue;
                }
                call = interceptor.intercept(methodName,
                        call.getPayload(), call.getHeaders(), agentCard, context);
                RequestTenant.requireUnchanged(call.getPayload(), tenant);
            }
            call = new PayloadAndHeaders(call.getPayload(), call.getHeaders());
        }
        return new RoutedRequest(baseUrl, call);
    }

    private String sendPostRequest(String url, PayloadAndHeaders payloadAndHeaders) throws IOException, InterruptedException, JsonProcessingException {
        MessageOrBuilder payload = InterceptorPayloads.cast(payloadAndHeaders.getPayload(), MessageOrBuilder.class);
        String body = ProtoJsonUtils.toJson(JsonFormat.printer(), payload);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(body);
        }
        boolean followRedirects = postRedirectPolicy.allows(payloadAndHeaders.getHeaders(), interceptors);
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
        String body = ProtoJsonUtils.toJson(JsonFormat.printer(), InterceptorPayloads.cast(payloadAndHeaders.getPayload(), MessageOrBuilder.class));
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

        for (Map.Entry<String, String> entry : payloadAndHeaders.getHeaders().entrySet()) {
            postBuilder.addHeader(entry.getKey(), entry.getValue());
        }
        return postBuilder;
    }

    private static <T extends A2AHttpClient.Builder<T>> void addStandardHeaders(T builder, PayloadAndHeaders payloadAndHeaders) {
        builder.addHeader(A2AHeaders.A2A_VERSION, AgentInterface.CURRENT_PROTOCOL_VERSION);
        for (Map.Entry<String, String> entry : payloadAndHeaders.getHeaders().entrySet()) {
            builder.addHeader(entry.getKey(), entry.getValue());
        }
    }

    private Map<String, String> getHttpHeaders(@Nullable ClientCallContext context) {
        return context != null ? context.getHeaders() : Collections.emptyMap();
    }
}
