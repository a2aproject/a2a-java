package io.a2a.client.transport.jsonrpc;

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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.google.protobuf.MessageOrBuilder;

import io.a2a.client.http.A2AHttpClient;
import io.a2a.client.http.A2AHttpClientFactory;
import io.a2a.client.http.A2AHttpResponse;
import io.a2a.client.transport.jsonrpc.sse.SSEEventListener;
import io.a2a.client.transport.spi.ClientTransport;
import io.a2a.client.transport.spi.interceptors.ClientCallContext;
import io.a2a.client.transport.spi.interceptors.ClientCallInterceptor;
import io.a2a.client.transport.spi.interceptors.PayloadAndHeaders;
import io.a2a.grpc.utils.JSONRPCUtils;
import io.a2a.grpc.utils.ProtoUtils;
import io.a2a.jsonrpc.common.json.JsonProcessingException;
import io.a2a.jsonrpc.common.wrappers.A2AResponse;
import io.a2a.jsonrpc.common.wrappers.CancelTaskResponse;
import io.a2a.jsonrpc.common.wrappers.DeleteTaskPushNotificationConfigResponse;
import io.a2a.jsonrpc.common.wrappers.GetExtendedAgentCardResponse;
import io.a2a.jsonrpc.common.wrappers.GetTaskPushNotificationConfigResponse;
import io.a2a.jsonrpc.common.wrappers.GetTaskResponse;
import io.a2a.jsonrpc.common.wrappers.ListTaskPushNotificationConfigResponse;
import io.a2a.jsonrpc.common.wrappers.ListTasksResponse;
import io.a2a.jsonrpc.common.wrappers.ListTasksResult;
import io.a2a.jsonrpc.common.wrappers.SendMessageResponse;
import io.a2a.jsonrpc.common.wrappers.CreateTaskPushNotificationConfigResponse;
import io.a2a.spec.A2AClientError;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.A2AError;
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

public class JSONRPCTransport implements ClientTransport {

    private final A2AHttpClient httpClient;
    private final AgentInterface agentInterface;
    private final @Nullable List<ClientCallInterceptor> interceptors;
    private final @Nullable AgentCard agentCard;

    public JSONRPCTransport(String agentUrl) {
        this(null, null, new AgentInterface("JSONRPC", agentUrl),  null);
    }

    public JSONRPCTransport(AgentCard agentCard) {
        this(null, agentCard, Utils.getFavoriteInterface(agentCard), null);
    }

    public JSONRPCTransport(@Nullable A2AHttpClient httpClient, @Nullable AgentCard agentCard,
                            AgentInterface agentInterface, @Nullable List<ClientCallInterceptor> interceptors) {
        this.httpClient = httpClient == null ? A2AHttpClientFactory.create() : httpClient;
        this.agentCard = agentCard;
        this.agentInterface = agentInterface;
        this.interceptors = interceptors;
    }

    @Override
    public EventKind sendMessage(MessageSendParams request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(SEND_MESSAGE_METHOD, ProtoUtils.ToProto.sendMessageRequest(request),
                agentCard, context);

        try {
            String httpResponseBody = sendPostRequest(Utils.buildBaseUrl(agentInterface, request.tenant()), payloadAndHeaders, SEND_MESSAGE_METHOD);
            SendMessageResponse response = unmarshalResponse(httpResponseBody, SEND_MESSAGE_METHOD);
            return response.getResult();
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException | JsonProcessingException e) {
            throw new A2AClientException("Failed to send message: " + e, e);
        }
    }

    @Override
    public void sendMessageStreaming(MessageSendParams request, Consumer<StreamingEventKind> eventConsumer,
                                     @Nullable Consumer<Throwable> errorConsumer, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        checkNotNullParam("eventConsumer", eventConsumer);
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(SEND_STREAMING_MESSAGE_METHOD,
                ProtoUtils.ToProto.sendMessageRequest(request), agentCard, context);

        final AtomicReference<CompletableFuture<Void>> ref = new AtomicReference<>();
        SSEEventListener sseEventListener = new SSEEventListener(eventConsumer, errorConsumer);

        try {
            A2AHttpClient.PostBuilder builder = createPostBuilder(Utils.buildBaseUrl(agentInterface, request.tenant()), payloadAndHeaders, SEND_STREAMING_MESSAGE_METHOD);
            ref.set(builder.postAsyncSSE(
                    msg -> sseEventListener.onMessage(msg, ref.get()),
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
    public Task getTask(TaskQueryParams request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(GET_TASK_METHOD, ProtoUtils.ToProto.getTaskRequest(request),
                agentCard, context);

        try {
            String httpResponseBody = sendPostRequest(Utils.buildBaseUrl(agentInterface, request.tenant()), payloadAndHeaders, GET_TASK_METHOD);
            GetTaskResponse response = unmarshalResponse(httpResponseBody, GET_TASK_METHOD);
            return response.getResult();
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException | JsonProcessingException e) {
            throw new A2AClientException("Failed to get task: " + e, e);
        }
    }

    @Override
    public Task cancelTask(TaskIdParams request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(CANCEL_TASK_METHOD, ProtoUtils.ToProto.cancelTaskRequest(request),
                agentCard, context);

        try {
            String httpResponseBody = sendPostRequest(Utils.buildBaseUrl(agentInterface, request.tenant()), payloadAndHeaders, CANCEL_TASK_METHOD);
            CancelTaskResponse response = unmarshalResponse(httpResponseBody, CANCEL_TASK_METHOD);
            return response.getResult();
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException | JsonProcessingException e) {
            throw new A2AClientException("Failed to cancel task: " + e, e);
        }
    }

    @Override
    public ListTasksResult listTasks(ListTasksParams request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(LIST_TASK_METHOD, ProtoUtils.ToProto.listTasksParams(request),
                agentCard, context);
        try {
            String httpResponseBody = sendPostRequest(Utils.buildBaseUrl(agentInterface, request.tenant()), payloadAndHeaders, LIST_TASK_METHOD);
            ListTasksResponse response = unmarshalResponse(httpResponseBody, LIST_TASK_METHOD);
            return response.getResult();
        } catch (IOException | InterruptedException | JsonProcessingException e) {
            throw new A2AClientException("Failed to list tasks: " + e, e);
        }
    }

    @Override
    public TaskPushNotificationConfig createTaskPushNotificationConfiguration(TaskPushNotificationConfig request,
                                                                              @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD,
                ProtoUtils.ToProto.createTaskPushNotificationConfigRequest(request), agentCard, context);

        try {
            String httpResponseBody = sendPostRequest(Utils.buildBaseUrl(agentInterface, request.tenant()), payloadAndHeaders, SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
            CreateTaskPushNotificationConfigResponse response = unmarshalResponse(httpResponseBody, SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
            return response.getResult();
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException | JsonProcessingException e) {
            throw new A2AClientException("Failed to set task push notification config: " + e, e);
        }
    }

    @Override
    public TaskPushNotificationConfig getTaskPushNotificationConfiguration(GetTaskPushNotificationConfigParams request,
                                                                           @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD,
                ProtoUtils.ToProto.getTaskPushNotificationConfigRequest(request), agentCard, context);

        try {
            String httpResponseBody = sendPostRequest(Utils.buildBaseUrl(agentInterface, request.tenant()), payloadAndHeaders, GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
            GetTaskPushNotificationConfigResponse response = unmarshalResponse(httpResponseBody, GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
            return response.getResult();
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException | JsonProcessingException e) {
            throw new A2AClientException("Failed to get task push notification config: " + e, e);
        }
    }

    @Override
    public ListTaskPushNotificationConfigResult listTaskPushNotificationConfigurations(
            ListTaskPushNotificationConfigParams request,
            @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD,
                ProtoUtils.ToProto.listTaskPushNotificationConfigRequest(request), agentCard, context);

        try {
            String httpResponseBody = sendPostRequest(Utils.buildBaseUrl(agentInterface, request.tenant()), payloadAndHeaders, LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
            ListTaskPushNotificationConfigResponse response = unmarshalResponse(httpResponseBody, LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
            return response.getResult();
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException | JsonProcessingException e) {
            throw new A2AClientException("Failed to list task push notification configs: " + e, e);
        }
    }

    @Override
    public void deleteTaskPushNotificationConfigurations(DeleteTaskPushNotificationConfigParams request,
                                                         @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD,
                ProtoUtils.ToProto.deleteTaskPushNotificationConfigRequest(request), agentCard, context);

        try {
            String httpResponseBody = sendPostRequest(Utils.buildBaseUrl(agentInterface, request.tenant()), payloadAndHeaders, DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
            DeleteTaskPushNotificationConfigResponse response = unmarshalResponse(httpResponseBody, DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
            // Response validated (no error), but no result to return
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException | JsonProcessingException e) {
            throw new A2AClientException("Failed to delete task push notification configs: " + e, e);
        }
    }

    @Override
    public void subscribeToTask(TaskIdParams request, Consumer<StreamingEventKind> eventConsumer,
                            Consumer<Throwable> errorConsumer, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        checkNotNullParam("eventConsumer", eventConsumer);
        checkNotNullParam("errorConsumer", errorConsumer);
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(SUBSCRIBE_TO_TASK_METHOD, ProtoUtils.ToProto.subscribeToTaskRequest(request), agentCard, context);

        AtomicReference<CompletableFuture<Void>> ref = new AtomicReference<>();
        SSEEventListener sseEventListener = new SSEEventListener(eventConsumer, errorConsumer);

        try {
            A2AHttpClient.PostBuilder builder = createPostBuilder(Utils.buildBaseUrl(agentInterface, request.tenant()), payloadAndHeaders, SUBSCRIBE_TO_TASK_METHOD);
            ref.set(builder.postAsyncSSE(
                    msg -> sseEventListener.onMessage(msg, ref.get()),
                    throwable -> sseEventListener.onError(throwable, ref.get()),
                    () -> {
                        // Signal normal stream completion to error handler (null error means success)
                        sseEventListener.onComplete();
                    }));
        } catch (IOException e) {
            throw new A2AClientException("Failed to send task resubscription request: " + e, e);
        } catch (InterruptedException e) {
            throw new A2AClientException("Task resubscription request timed out: " + e, e);
        } catch (JsonProcessingException e) {
            throw new A2AClientException("Failed to process JSON for task resubscription request: " + e, e);
        }
    }

    @Override
    public AgentCard getExtendedAgentCard(@Nullable ClientCallContext context) throws A2AClientException {
        try {
            PayloadAndHeaders payloadAndHeaders = applyInterceptors(GET_EXTENDED_AGENT_CARD_METHOD,
                    ProtoUtils.ToProto.extendedAgentCard(), agentCard, context);

            try {
                String httpResponseBody = sendPostRequest(Utils.buildBaseUrl(agentInterface, ""), payloadAndHeaders, GET_EXTENDED_AGENT_CARD_METHOD);
                GetExtendedAgentCardResponse response = unmarshalResponse(httpResponseBody, GET_EXTENDED_AGENT_CARD_METHOD);
                return response.getResult();
            } catch (IOException | InterruptedException | JsonProcessingException e) {
                throw new A2AClientException("Failed to get authenticated extended agent card: " + e, e);
            }
        } catch(A2AClientError e){
            throw new A2AClientException("Failed to get agent card: " + e, e);
        }
    }

    @Override
    public void close() {
        // no-op
    }

    private PayloadAndHeaders applyInterceptors(String methodName, @Nullable Object payload,
                                                @Nullable AgentCard agentCard, @Nullable ClientCallContext clientCallContext) {
        PayloadAndHeaders payloadAndHeaders = new PayloadAndHeaders(payload, getHttpHeaders(clientCallContext));
        if (interceptors != null && ! interceptors.isEmpty()) {
            for (ClientCallInterceptor interceptor : interceptors) {
                payloadAndHeaders = interceptor.intercept(methodName, payloadAndHeaders.getPayload(),
                        payloadAndHeaders.getHeaders(), agentCard, clientCallContext);
            }
        }
        return payloadAndHeaders;
    }

    private String sendPostRequest(String url, PayloadAndHeaders payloadAndHeaders, String method) throws IOException, InterruptedException, JsonProcessingException {
        A2AHttpClient.PostBuilder builder = createPostBuilder(url, payloadAndHeaders,method);
        A2AHttpResponse response = builder.post();
        if (!response.success()) {
            throw new IOException("Request failed " + response.status());
        }
        return response.body();
    }

    private A2AHttpClient.PostBuilder createPostBuilder(String url, PayloadAndHeaders payloadAndHeaders, String method) throws JsonProcessingException {
        A2AHttpClient.PostBuilder postBuilder = httpClient.createPost()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .body(JSONRPCUtils.toJsonRPCRequest(null, method, (MessageOrBuilder) payloadAndHeaders.getPayload()));

        if (payloadAndHeaders.getHeaders() != null) {
            for (Map.Entry<String, String> entry : payloadAndHeaders.getHeaders().entrySet()) {
                postBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        return postBuilder;
    }

    /**
     * Unmarshals a JSON-RPC response string into a type-safe response object.
     * <p>
     * This method parses the JSON-RPC response body and returns the appropriate
     * response type based on the method parameter. If the response contains an error,
     * an A2AClientException is thrown.
     *
     * @param <T> the expected response type, must extend JSONRPCResponse
     * @param response the JSON-RPC response body as a string
     * @param method the method name used to determine the response type
     * @return the parsed response object of type T
     * @throws A2AClientException if the response contains an error or parsing fails
     * @throws JsonProcessingException if the JSON cannot be processed
     */
    @SuppressWarnings("unchecked")
    private <T extends A2AResponse<?>> T unmarshalResponse(String response, String method)
            throws A2AClientException, JsonProcessingException {
        A2AResponse<?> value = JSONRPCUtils.parseResponseBody(response, method);
        A2AError error = value.getError();
        if (error != null) {
            throw new A2AClientException(error.getMessage() + (error.getData() != null ? ": " + error.getData() : ""), error);
        }
        // Safe cast: JSONRPCUtils.parseResponseBody returns the correct concrete type based on method
        return (T) value;
    }

    private @Nullable Map<String, String> getHttpHeaders(@Nullable ClientCallContext context) {
        return context != null ? context.getHeaders() : null;
    }
}