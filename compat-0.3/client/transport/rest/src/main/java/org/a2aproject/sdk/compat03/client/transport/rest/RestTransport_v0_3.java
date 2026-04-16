package org.a2aproject.sdk.compat03.client.transport.rest;

import static org.a2aproject.sdk.util.Assert.checkNotNullParam;

import org.a2aproject.sdk.compat03.json.JsonProcessingException_v0_3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import org.a2aproject.sdk.compat03.client.http.A2ACardResolver_v0_3;
import org.a2aproject.sdk.compat03.client.http.A2AHttpClient_v0_3;
import org.a2aproject.sdk.compat03.client.http.A2AHttpResponse_v0_3;
import org.a2aproject.sdk.compat03.client.http.JdkA2AHttpClient_v0_3;
import org.a2aproject.sdk.compat03.client.transport.rest.sse.RestSSEEventListener_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.ClientTransport_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallContext_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallInterceptor_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.PayloadAndHeaders_v0_3;
import org.a2aproject.sdk.compat03.grpc.CancelTaskRequest;
import org.a2aproject.sdk.compat03.grpc.CreateTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.compat03.grpc.GetTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.compat03.grpc.GetTaskRequest;
import org.a2aproject.sdk.compat03.grpc.ListTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.compat03.spec.CancelTaskRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.ListTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.SendMessageRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskPushNotificationConfig_v0_3;
import org.a2aproject.sdk.compat03.spec.A2AClientException_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.EventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.GetAuthenticatedExtendedCardRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.ListTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.MessageSendParams_v0_3;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskResubscriptionRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskIdParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskQueryParams_v0_3;
import org.a2aproject.sdk.compat03.grpc.utils.ProtoUtils_v0_3;
import org.a2aproject.sdk.compat03.spec.A2AClientError_v0_3;
import org.a2aproject.sdk.compat03.spec.SendStreamingMessageRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.SetTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.json.JsonUtil_v0_3;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

public class RestTransport_v0_3 implements ClientTransport_v0_3 {

    private static final Logger log = Logger.getLogger(RestTransport_v0_3.class.getName());
    private final A2AHttpClient_v0_3 httpClient;
    private final String agentUrl;
    private @Nullable final List<ClientCallInterceptor_v0_3> interceptors;
    private AgentCard_v0_3 agentCard;
    private boolean needsExtendedCard = false;

    public RestTransport_v0_3(AgentCard_v0_3 agentCard) {
        this(null, agentCard, agentCard.url(), null);
    }

    public RestTransport_v0_3(@Nullable A2AHttpClient_v0_3 httpClient, AgentCard_v0_3 agentCard,
                              String agentUrl, @Nullable List<ClientCallInterceptor_v0_3> interceptors) {
        this.httpClient = httpClient == null ? new JdkA2AHttpClient_v0_3() : httpClient;
        this.agentCard = agentCard;
        this.agentUrl = agentUrl.endsWith("/") ? agentUrl.substring(0, agentUrl.length() - 1) : agentUrl;
        this.interceptors = interceptors;
    }

    @Override
    public EventKind_v0_3 sendMessage(MessageSendParams_v0_3 messageSendParams, @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
        checkNotNullParam("messageSendParams", messageSendParams);
        org.a2aproject.sdk.compat03.grpc.SendMessageRequest.Builder builder = org.a2aproject.sdk.compat03.grpc.SendMessageRequest.newBuilder(ProtoUtils_v0_3.ToProto.sendMessageRequest(messageSendParams));
        PayloadAndHeaders_v0_3 payloadAndHeaders = applyInterceptors(SendMessageRequest_v0_3.METHOD, builder, agentCard, context);
        try {
            String httpResponseBody = sendPostRequest(agentUrl + "/v1/message:send", payloadAndHeaders);
            org.a2aproject.sdk.compat03.grpc.SendMessageResponse.Builder responseBuilder = org.a2aproject.sdk.compat03.grpc.SendMessageResponse.newBuilder();
            JsonFormat.parser().merge(httpResponseBody, responseBuilder);
            if (responseBuilder.hasMsg()) {
                return ProtoUtils_v0_3.FromProto.message(responseBuilder.getMsg());
            }
            if (responseBuilder.hasTask()) {
                return ProtoUtils_v0_3.FromProto.task(responseBuilder.getTask());
            }
            throw new A2AClientException_v0_3("Failed to send message, wrong response:" + httpResponseBody);
        } catch (A2AClientException_v0_3 e) {
            throw e;
        } catch (IOException | InterruptedException | JsonProcessingException_v0_3 e) {
            throw new A2AClientException_v0_3("Failed to send message: " + e, e);
        }
    }

    @Override
    public void sendMessageStreaming(MessageSendParams_v0_3 messageSendParams, Consumer<StreamingEventKind_v0_3> eventConsumer, Consumer<Throwable> errorConsumer, @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
        checkNotNullParam("request", messageSendParams);
        checkNotNullParam("eventConsumer", eventConsumer);
        checkNotNullParam("messageSendParams", messageSendParams);
        org.a2aproject.sdk.compat03.grpc.SendMessageRequest.Builder builder = org.a2aproject.sdk.compat03.grpc.SendMessageRequest.newBuilder(ProtoUtils_v0_3.ToProto.sendMessageRequest(messageSendParams));
        PayloadAndHeaders_v0_3 payloadAndHeaders = applyInterceptors(SendStreamingMessageRequest_v0_3.METHOD,
                builder, agentCard, context);
        AtomicReference<CompletableFuture<Void>> ref = new AtomicReference<>();
        RestSSEEventListener_v0_3 sseEventListener = new RestSSEEventListener_v0_3(eventConsumer, errorConsumer);
        try {
            A2AHttpClient_v0_3.PostBuilder postBuilder = createPostBuilder(agentUrl + "/v1/message:stream", payloadAndHeaders);
            ref.set(postBuilder.postAsyncSSE(
                    msg -> sseEventListener.onMessage(msg, ref.get()),
                    throwable -> sseEventListener.onError(throwable, ref.get()),
                    () -> {
                        // We don't need to do anything special on completion
                    }));
        } catch (IOException e) {
            throw new A2AClientException_v0_3("Failed to send streaming message request: " + e, e);
        } catch (InterruptedException e) {
            throw new A2AClientException_v0_3("Send streaming message request timed out: " + e, e);
        } catch (JsonProcessingException_v0_3 e) {
            throw new A2AClientException_v0_3("Failed to process JSON for streaming message request: " + e, e);
        }
    }

    @Override
    public Task_v0_3 getTask(TaskQueryParams_v0_3 taskQueryParams, @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
        checkNotNullParam("taskQueryParams", taskQueryParams);
        GetTaskRequest.Builder builder = GetTaskRequest.newBuilder();
        builder.setName("tasks/" + taskQueryParams.id());
        PayloadAndHeaders_v0_3 payloadAndHeaders = applyInterceptors(GetTaskRequest_v0_3.METHOD, builder,
                agentCard, context);
        try {
            String url;
            if (taskQueryParams.historyLength() > 0) {
                url = agentUrl + String.format("/v1/tasks/%1s?historyLength=%2d", taskQueryParams.id(), taskQueryParams.historyLength());
            } else {
                url = agentUrl + String.format("/v1/tasks/%1s", taskQueryParams.id());
            }
            A2AHttpClient_v0_3.GetBuilder getBuilder = httpClient.createGet().url(url);
            if (payloadAndHeaders.getHeaders() != null) {
                for (Map.Entry<String, String> entry : payloadAndHeaders.getHeaders().entrySet()) {
                    getBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }
            A2AHttpResponse_v0_3 response = getBuilder.get();
            if (!response.success()) {
                throw RestErrorMapper_v0_3.mapRestError(response);
            }
            String httpResponseBody = response.body();
            org.a2aproject.sdk.compat03.grpc.Task.Builder responseBuilder = org.a2aproject.sdk.compat03.grpc.Task.newBuilder();
            JsonFormat.parser().merge(httpResponseBody, responseBuilder);
            return ProtoUtils_v0_3.FromProto.task(responseBuilder);
        } catch (A2AClientException_v0_3 e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            throw new A2AClientException_v0_3("Failed to get task: " + e, e);
        }
    }

    @Override
    public Task_v0_3 cancelTask(TaskIdParams_v0_3 taskIdParams, @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
        checkNotNullParam("taskIdParams", taskIdParams);
        CancelTaskRequest.Builder builder = CancelTaskRequest.newBuilder();
        builder.setName("tasks/" + taskIdParams.id());
        PayloadAndHeaders_v0_3 payloadAndHeaders = applyInterceptors(CancelTaskRequest_v0_3.METHOD, builder,
                agentCard, context);
        try {
            String httpResponseBody = sendPostRequest(agentUrl + String.format("/v1/tasks/%1s:cancel", taskIdParams.id()), payloadAndHeaders);
            org.a2aproject.sdk.compat03.grpc.Task.Builder responseBuilder = org.a2aproject.sdk.compat03.grpc.Task.newBuilder();
            JsonFormat.parser().merge(httpResponseBody, responseBuilder);
            return ProtoUtils_v0_3.FromProto.task(responseBuilder);
        } catch (A2AClientException_v0_3 e) {
            throw e;
        } catch (IOException | InterruptedException | JsonProcessingException_v0_3 e) {
            throw new A2AClientException_v0_3("Failed to cancel task: " + e, e);
        }
    }

    @Override
    public TaskPushNotificationConfig_v0_3 setTaskPushNotificationConfiguration(TaskPushNotificationConfig_v0_3 request, @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
        checkNotNullParam("request", request);
        CreateTaskPushNotificationConfigRequest.Builder builder = CreateTaskPushNotificationConfigRequest.newBuilder();
        builder.setConfig(ProtoUtils_v0_3.ToProto.taskPushNotificationConfig(request))
                .setParent("tasks/" + request.taskId());
        if (request.pushNotificationConfig().id() != null) {
            builder.setConfigId(request.pushNotificationConfig().id());
        }
        PayloadAndHeaders_v0_3 payloadAndHeaders = applyInterceptors(SetTaskPushNotificationConfigRequest_v0_3.METHOD, builder, agentCard, context);
        try {
            String httpResponseBody = sendPostRequest(agentUrl + String.format("/v1/tasks/%1s/pushNotificationConfigs", request.taskId()), payloadAndHeaders);
            org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig.Builder responseBuilder = org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig.newBuilder();
            JsonFormat.parser().merge(httpResponseBody, responseBuilder);
            return ProtoUtils_v0_3.FromProto.taskPushNotificationConfig(responseBuilder);
        } catch (A2AClientException_v0_3 e) {
            throw e;
        } catch (IOException | InterruptedException | JsonProcessingException_v0_3 e) {
            throw new A2AClientException_v0_3("Failed to set task push notification config: " + e, e);
        }
    }

    @Override
    public TaskPushNotificationConfig_v0_3 getTaskPushNotificationConfiguration(GetTaskPushNotificationConfigParams_v0_3 request, @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
        checkNotNullParam("request", request);
        // When configId is not specified, use taskId as the default configId
        String configId = request.pushNotificationConfigId() != null ? request.pushNotificationConfigId() : request.id();
        GetTaskPushNotificationConfigRequest.Builder builder = GetTaskPushNotificationConfigRequest.newBuilder();
        builder.setName(String.format("/tasks/%1s/pushNotificationConfigs/%2s", request.id(), configId));
        PayloadAndHeaders_v0_3 payloadAndHeaders = applyInterceptors(GetTaskPushNotificationConfigRequest_v0_3.METHOD, builder,
                agentCard, context);
        try {
            String url = agentUrl + String.format("/v1/tasks/%1s/pushNotificationConfigs/%2s", request.id(), configId);
            A2AHttpClient_v0_3.GetBuilder getBuilder = httpClient.createGet().url(url);
            if (payloadAndHeaders.getHeaders() != null) {
                for (Map.Entry<String, String> entry : payloadAndHeaders.getHeaders().entrySet()) {
                    getBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }
            A2AHttpResponse_v0_3 response = getBuilder.get();
            if (!response.success()) {
                throw RestErrorMapper_v0_3.mapRestError(response);
            }
            String httpResponseBody = response.body();
            org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig.Builder responseBuilder = org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig.newBuilder();
            JsonFormat.parser().merge(httpResponseBody, responseBuilder);
            return ProtoUtils_v0_3.FromProto.taskPushNotificationConfig(responseBuilder);
        } catch (A2AClientException_v0_3 e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            throw new A2AClientException_v0_3("Failed to get push notifications: " + e, e);
        }
    }

    @Override
    public List<TaskPushNotificationConfig_v0_3> listTaskPushNotificationConfigurations(ListTaskPushNotificationConfigParams_v0_3 request, @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
        checkNotNullParam("request", request);
        ListTaskPushNotificationConfigRequest.Builder builder = ListTaskPushNotificationConfigRequest.newBuilder();
        builder.setParent(String.format("/tasks/%1s/pushNotificationConfigs", request.id()));
        PayloadAndHeaders_v0_3 payloadAndHeaders = applyInterceptors(ListTaskPushNotificationConfigRequest_v0_3.METHOD, builder,
                agentCard, context);
        try {
            String url = agentUrl + String.format("/v1/tasks/%1s/pushNotificationConfigs", request.id());
            A2AHttpClient_v0_3.GetBuilder getBuilder = httpClient.createGet().url(url);
            if (payloadAndHeaders.getHeaders() != null) {
                for (Map.Entry<String, String> entry : payloadAndHeaders.getHeaders().entrySet()) {
                    getBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }
            A2AHttpResponse_v0_3 response = getBuilder.get();
            if (!response.success()) {
                throw RestErrorMapper_v0_3.mapRestError(response);
            }
            String httpResponseBody = response.body();
            org.a2aproject.sdk.compat03.grpc.ListTaskPushNotificationConfigResponse.Builder responseBuilder = org.a2aproject.sdk.compat03.grpc.ListTaskPushNotificationConfigResponse.newBuilder();
            JsonFormat.parser().merge(httpResponseBody, responseBuilder);
            return ProtoUtils_v0_3.FromProto.listTaskPushNotificationConfigParams(responseBuilder);
        } catch (A2AClientException_v0_3 e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            throw new A2AClientException_v0_3("Failed to list push notifications: " + e, e);
        }
    }

    @Override
    public void deleteTaskPushNotificationConfigurations(DeleteTaskPushNotificationConfigParams_v0_3 request, @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
        checkNotNullParam("request", request);
        org.a2aproject.sdk.compat03.grpc.DeleteTaskPushNotificationConfigRequestOrBuilder builder = org.a2aproject.sdk.compat03.grpc.DeleteTaskPushNotificationConfigRequest.newBuilder();
        PayloadAndHeaders_v0_3 payloadAndHeaders = applyInterceptors(DeleteTaskPushNotificationConfigRequest_v0_3.METHOD, builder,
                agentCard, context);
        try {
            String url = agentUrl + String.format("/v1/tasks/%1s/pushNotificationConfigs/%2s", request.id(), request.pushNotificationConfigId());
            A2AHttpClient_v0_3.DeleteBuilder deleteBuilder = httpClient.createDelete().url(url);
            if (payloadAndHeaders.getHeaders() != null) {
                for (Map.Entry<String, String> entry : payloadAndHeaders.getHeaders().entrySet()) {
                    deleteBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }
            A2AHttpResponse_v0_3 response = deleteBuilder.delete();
            if (!response.success()) {
                throw RestErrorMapper_v0_3.mapRestError(response);
            }
        } catch (A2AClientException_v0_3 e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            throw new A2AClientException_v0_3("Failed to delete push notification config: " + e, e);
        }
    }

    @Override
    public void resubscribe(TaskIdParams_v0_3 request, Consumer<StreamingEventKind_v0_3> eventConsumer,
                            Consumer<Throwable> errorConsumer, @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
        checkNotNullParam("request", request);
        org.a2aproject.sdk.compat03.grpc.TaskSubscriptionRequest.Builder builder = org.a2aproject.sdk.compat03.grpc.TaskSubscriptionRequest.newBuilder();
        builder.setName("tasks/" + request.id());
        PayloadAndHeaders_v0_3 payloadAndHeaders = applyInterceptors(TaskResubscriptionRequest_v0_3.METHOD, builder,
                agentCard, context);
        AtomicReference<CompletableFuture<Void>> ref = new AtomicReference<>();
        RestSSEEventListener_v0_3 sseEventListener = new RestSSEEventListener_v0_3(eventConsumer, errorConsumer);
        try {
            String url = agentUrl + String.format("/v1/tasks/%1s:subscribe", request.id());
            A2AHttpClient_v0_3.PostBuilder postBuilder = createPostBuilder(url, payloadAndHeaders);
            ref.set(postBuilder.postAsyncSSE(
                    msg -> sseEventListener.onMessage(msg, ref.get()),
                    throwable -> sseEventListener.onError(throwable, ref.get()),
                    () -> {
                        // We don't need to do anything special on completion
                    }));
        } catch (IOException e) {
            throw new A2AClientException_v0_3("Failed to send streaming message request: " + e, e);
        } catch (InterruptedException e) {
            throw new A2AClientException_v0_3("Send streaming message request timed out: " + e, e);
        } catch (JsonProcessingException_v0_3 e) {
            throw new A2AClientException_v0_3("Failed to process JSON for streaming message request: " + e, e);
        }
    }

    @Override
    public AgentCard_v0_3 getAgentCard(@Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
        A2ACardResolver_v0_3 resolver;
        try {
            if (agentCard == null) {
                resolver = new A2ACardResolver_v0_3(httpClient, agentUrl, null, getHttpHeaders(context));
                agentCard = resolver.getAgentCard();
                needsExtendedCard = agentCard.supportsAuthenticatedExtendedCard();
            }
            if (!needsExtendedCard) {
                return agentCard;
            }
            PayloadAndHeaders_v0_3 payloadAndHeaders = applyInterceptors(GetAuthenticatedExtendedCardRequest_v0_3.METHOD, null,
                    agentCard, context);
            String url = agentUrl + String.format("/v1/card");
            A2AHttpClient_v0_3.GetBuilder getBuilder = httpClient.createGet().url(url);
            if (payloadAndHeaders.getHeaders() != null) {
                for (Map.Entry<String, String> entry : payloadAndHeaders.getHeaders().entrySet()) {
                    getBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }
            A2AHttpResponse_v0_3 response = getBuilder.get();
            if (!response.success()) {
                throw RestErrorMapper_v0_3.mapRestError(response);
            }
            String httpResponseBody = response.body();
            agentCard = JsonUtil_v0_3.fromJson(httpResponseBody, AgentCard_v0_3.class);
            needsExtendedCard = false;
            return agentCard;
        } catch (IOException | InterruptedException | JsonProcessingException_v0_3 e) {
            throw new A2AClientException_v0_3("Failed to get authenticated extended agent card: " + e, e);
        } catch (A2AClientError_v0_3 e) {
            throw new A2AClientException_v0_3("Failed to get agent card: " + e, e);
        }
    }

    @Override
    public void close() {
        // no-op
    }

    private PayloadAndHeaders_v0_3 applyInterceptors(String methodName, @Nullable MessageOrBuilder payload,
                                                     AgentCard_v0_3 agentCard, @Nullable ClientCallContext_v0_3 clientCallContext) {
        PayloadAndHeaders_v0_3 payloadAndHeaders = new PayloadAndHeaders_v0_3(payload, getHttpHeaders(clientCallContext));
        if (interceptors != null && !interceptors.isEmpty()) {
            for (ClientCallInterceptor_v0_3 interceptor : interceptors) {
                payloadAndHeaders = interceptor.intercept(methodName, payloadAndHeaders.getPayload(),
                        payloadAndHeaders.getHeaders(), agentCard, clientCallContext);
            }
        }
        return payloadAndHeaders;
    }

    private String sendPostRequest(String url, PayloadAndHeaders_v0_3 payloadAndHeaders) throws IOException, InterruptedException, JsonProcessingException_v0_3 {
        A2AHttpClient_v0_3.PostBuilder builder = createPostBuilder(url, payloadAndHeaders);
        A2AHttpResponse_v0_3 response = builder.post();
        if (!response.success()) {
            log.fine("Error on POST processing " + JsonFormat.printer().print((MessageOrBuilder) payloadAndHeaders.getPayload()));
            throw RestErrorMapper_v0_3.mapRestError(response);
        }
        return response.body();
    }

    private A2AHttpClient_v0_3.PostBuilder createPostBuilder(String url, PayloadAndHeaders_v0_3 payloadAndHeaders) throws JsonProcessingException_v0_3, InvalidProtocolBufferException {
        log.fine(JsonFormat.printer().print((MessageOrBuilder) payloadAndHeaders.getPayload()));
        A2AHttpClient_v0_3.PostBuilder postBuilder = httpClient.createPost()
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

    private Map<String, String> getHttpHeaders(@Nullable ClientCallContext_v0_3 context) {
        return context != null ? context.getHeaders() : Collections.emptyMap();
    }
}
