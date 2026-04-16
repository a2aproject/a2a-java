package org.a2aproject.sdk.compat03.client.transport.jsonrpc;

import static org.a2aproject.sdk.util.Assert.checkNotNullParam;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.a2aproject.sdk.compat03.json.JsonProcessingException_v0_3;
import org.a2aproject.sdk.compat03.json.JsonUtil_v0_3;

import org.a2aproject.sdk.compat03.client.http.A2ACardResolver_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallContext_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallInterceptor_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.PayloadAndHeaders_v0_3;
import org.a2aproject.sdk.compat03.client.http.A2AHttpClient_v0_3;
import org.a2aproject.sdk.compat03.client.http.A2AHttpResponse_v0_3;
import org.a2aproject.sdk.compat03.client.http.JdkA2AHttpClient_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.ClientTransport_v0_3;
import org.a2aproject.sdk.compat03.spec.A2AClientError_v0_3;
import org.a2aproject.sdk.compat03.spec.A2AClientException_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.CancelTaskRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.CancelTaskResponse_v0_3;

import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.EventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.GetAuthenticatedExtendedCardRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.GetAuthenticatedExtendedCardResponse_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigResponse_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskResponse_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONRPCError_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONRPCMessage_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONRPCResponse_v0_3;

import org.a2aproject.sdk.compat03.spec.ListTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.ListTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.ListTaskPushNotificationConfigResponse_v0_3;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigResponse_v0_3;
import org.a2aproject.sdk.compat03.spec.MessageSendParams_v0_3;
import org.a2aproject.sdk.compat03.spec.SendMessageRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.SendMessageResponse_v0_3;
import org.a2aproject.sdk.compat03.spec.SendStreamingMessageRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.SetTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.SetTaskPushNotificationConfigResponse_v0_3;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskIdParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskPushNotificationConfig_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskQueryParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskResubscriptionRequest_v0_3;
import org.a2aproject.sdk.compat03.client.transport.jsonrpc.sse.SSEEventListener_v0_3;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class JSONRPCTransport_v0_3 implements ClientTransport_v0_3 {

    private static final Class<SendMessageResponse_v0_3> SEND_MESSAGE_RESPONSE_REFERENCE = SendMessageResponse_v0_3.class;
    private static final Class<GetTaskResponse_v0_3> GET_TASK_RESPONSE_REFERENCE = GetTaskResponse_v0_3.class;
    private static final Class<CancelTaskResponse_v0_3> CANCEL_TASK_RESPONSE_REFERENCE = CancelTaskResponse_v0_3.class;
    private static final Class<GetTaskPushNotificationConfigResponse_v0_3> GET_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE = GetTaskPushNotificationConfigResponse_v0_3.class;
    private static final Class<SetTaskPushNotificationConfigResponse_v0_3> SET_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE = SetTaskPushNotificationConfigResponse_v0_3.class;
    private static final Class<ListTaskPushNotificationConfigResponse_v0_3> LIST_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE = ListTaskPushNotificationConfigResponse_v0_3.class;
    private static final Class<DeleteTaskPushNotificationConfigResponse_v0_3> DELETE_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE = DeleteTaskPushNotificationConfigResponse_v0_3.class;
    private static final Class<GetAuthenticatedExtendedCardResponse_v0_3> GET_AUTHENTICATED_EXTENDED_CARD_RESPONSE_REFERENCE = GetAuthenticatedExtendedCardResponse_v0_3.class;

    private final A2AHttpClient_v0_3 httpClient;
    private final String agentUrl;
    private final List<ClientCallInterceptor_v0_3> interceptors;
    private AgentCard_v0_3 agentCard;
    private boolean needsExtendedCard = false;

    public JSONRPCTransport_v0_3(String agentUrl) {
        this(null, null, agentUrl, null);
    }

    public JSONRPCTransport_v0_3(AgentCard_v0_3 agentCard) {
        this(null, agentCard, agentCard.url(), null);
    }

    public JSONRPCTransport_v0_3(A2AHttpClient_v0_3 httpClient, AgentCard_v0_3 agentCard,
                                 String agentUrl, List<ClientCallInterceptor_v0_3> interceptors) {
        this.httpClient = httpClient == null ? new JdkA2AHttpClient_v0_3() : httpClient;
        this.agentCard = agentCard;
        this.agentUrl = agentUrl;
        this.interceptors = interceptors;
        this.needsExtendedCard = agentCard == null || agentCard.supportsAuthenticatedExtendedCard();
    }

    @Override
    public EventKind_v0_3 sendMessage(MessageSendParams_v0_3 request, ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
        checkNotNullParam("request", request);
        SendMessageRequest_v0_3 sendMessageRequest = new SendMessageRequest_v0_3.Builder()
                .jsonrpc(JSONRPCMessage_v0_3.JSONRPC_VERSION)
                .method(SendMessageRequest_v0_3.METHOD)
                .params(request)
                .build(); // id will be randomly generated

        PayloadAndHeaders_v0_3 payloadAndHeaders = applyInterceptors(SendMessageRequest_v0_3.METHOD, sendMessageRequest,
                agentCard, context);

        try {
            String httpResponseBody = sendPostRequest(payloadAndHeaders);
            SendMessageResponse_v0_3 response = unmarshalResponse(httpResponseBody, SEND_MESSAGE_RESPONSE_REFERENCE);
            return response.getResult();
        } catch (A2AClientException_v0_3 e) {
            throw e;
        } catch (IOException | InterruptedException | JsonProcessingException_v0_3 e) {
            throw new A2AClientException_v0_3("Failed to send message: " + e, e);
        }
    }

    @Override
    public void sendMessageStreaming(MessageSendParams_v0_3 request, Consumer<StreamingEventKind_v0_3> eventConsumer,
                                     Consumer<Throwable> errorConsumer, ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
        checkNotNullParam("request", request);
        checkNotNullParam("eventConsumer", eventConsumer);
        SendStreamingMessageRequest_v0_3 sendStreamingMessageRequest = new SendStreamingMessageRequest_v0_3.Builder()
                .jsonrpc(JSONRPCMessage_v0_3.JSONRPC_VERSION)
                .method(SendStreamingMessageRequest_v0_3.METHOD)
                .params(request)
                .build(); // id will be randomly generated

        PayloadAndHeaders_v0_3 payloadAndHeaders = applyInterceptors(SendStreamingMessageRequest_v0_3.METHOD,
                sendStreamingMessageRequest, agentCard, context);

        AtomicReference<CompletableFuture<Void>> ref = new AtomicReference<>();
        SSEEventListener_v0_3 sseEventListener = new SSEEventListener_v0_3(eventConsumer, errorConsumer);

        try {
            A2AHttpClient_v0_3.PostBuilder builder = createPostBuilder(payloadAndHeaders);
            ref.set(builder.postAsyncSSE(
                    msg -> sseEventListener.onMessage(msg, ref.get()),
                    throwable -> sseEventListener.onError(throwable, ref.get()),
                    () -> {
                        // Signal normal stream completion to error handler (null error means success)
                        sseEventListener.onComplete();
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
    public Task_v0_3 getTask(TaskQueryParams_v0_3 request, ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
        checkNotNullParam("request", request);
        GetTaskRequest_v0_3 getTaskRequest = new GetTaskRequest_v0_3.Builder()
                .jsonrpc(JSONRPCMessage_v0_3.JSONRPC_VERSION)
                .method(GetTaskRequest_v0_3.METHOD)
                .params(request)
                .build(); // id will be randomly generated

        PayloadAndHeaders_v0_3 payloadAndHeaders = applyInterceptors(GetTaskRequest_v0_3.METHOD, getTaskRequest,
                agentCard, context);

        try {
            String httpResponseBody = sendPostRequest(payloadAndHeaders);
            GetTaskResponse_v0_3 response = unmarshalResponse(httpResponseBody, GET_TASK_RESPONSE_REFERENCE);
            return response.getResult();
        } catch (A2AClientException_v0_3 e) {
            throw e;
        } catch (IOException | InterruptedException | JsonProcessingException_v0_3 e) {
            throw new A2AClientException_v0_3("Failed to get task: " + e, e);
        }
    }

    @Override
    public Task_v0_3 cancelTask(TaskIdParams_v0_3 request, ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
        checkNotNullParam("request", request);
        CancelTaskRequest_v0_3 cancelTaskRequest = new CancelTaskRequest_v0_3.Builder()
                .jsonrpc(JSONRPCMessage_v0_3.JSONRPC_VERSION)
                .method(CancelTaskRequest_v0_3.METHOD)
                .params(request)
                .build(); // id will be randomly generated

        PayloadAndHeaders_v0_3 payloadAndHeaders = applyInterceptors(CancelTaskRequest_v0_3.METHOD, cancelTaskRequest,
                agentCard, context);

        try {
            String httpResponseBody = sendPostRequest(payloadAndHeaders);
            CancelTaskResponse_v0_3 response = unmarshalResponse(httpResponseBody, CANCEL_TASK_RESPONSE_REFERENCE);
            return response.getResult();
        } catch (A2AClientException_v0_3 e) {
            throw e;
        } catch (IOException | InterruptedException | JsonProcessingException_v0_3 e) {
            throw new A2AClientException_v0_3("Failed to cancel task: " + e, e);
        }
    }

    @Override
    public TaskPushNotificationConfig_v0_3 setTaskPushNotificationConfiguration(TaskPushNotificationConfig_v0_3 request,
                                                                                ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
        checkNotNullParam("request", request);
        SetTaskPushNotificationConfigRequest_v0_3 setTaskPushNotificationRequest = new SetTaskPushNotificationConfigRequest_v0_3.Builder()
                .jsonrpc(JSONRPCMessage_v0_3.JSONRPC_VERSION)
                .method(SetTaskPushNotificationConfigRequest_v0_3.METHOD)
                .params(request)
                .build(); // id will be randomly generated

        PayloadAndHeaders_v0_3 payloadAndHeaders = applyInterceptors(SetTaskPushNotificationConfigRequest_v0_3.METHOD,
                setTaskPushNotificationRequest, agentCard, context);

        try {
            String httpResponseBody = sendPostRequest(payloadAndHeaders);
            SetTaskPushNotificationConfigResponse_v0_3 response = unmarshalResponse(httpResponseBody,
                    SET_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE);
            return response.getResult();
        } catch (A2AClientException_v0_3 e) {
            throw e;
        } catch (IOException | InterruptedException | JsonProcessingException_v0_3 e) {
            throw new A2AClientException_v0_3("Failed to set task push notification config: " + e, e);
        }
    }

    @Override
    public TaskPushNotificationConfig_v0_3 getTaskPushNotificationConfiguration(GetTaskPushNotificationConfigParams_v0_3 request,
                                                                                ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
        checkNotNullParam("request", request);
        GetTaskPushNotificationConfigRequest_v0_3 getTaskPushNotificationRequest = new GetTaskPushNotificationConfigRequest_v0_3.Builder()
                .jsonrpc(JSONRPCMessage_v0_3.JSONRPC_VERSION)
                .method(GetTaskPushNotificationConfigRequest_v0_3.METHOD)
                .params(request)
                .build(); // id will be randomly generated

        PayloadAndHeaders_v0_3 payloadAndHeaders = applyInterceptors(GetTaskPushNotificationConfigRequest_v0_3.METHOD,
                getTaskPushNotificationRequest, agentCard, context);

        try {
            String httpResponseBody = sendPostRequest(payloadAndHeaders);
            GetTaskPushNotificationConfigResponse_v0_3 response = unmarshalResponse(httpResponseBody,
                    GET_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE);
            return response.getResult();
        } catch (A2AClientException_v0_3 e) {
            throw e;
        } catch (IOException | InterruptedException | JsonProcessingException_v0_3 e) {
            throw new A2AClientException_v0_3("Failed to get task push notification config: " + e, e);
        }
    }

    @Override
    public List<TaskPushNotificationConfig_v0_3> listTaskPushNotificationConfigurations(
            ListTaskPushNotificationConfigParams_v0_3 request,
            ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
        checkNotNullParam("request", request);
        ListTaskPushNotificationConfigRequest_v0_3 listTaskPushNotificationRequest = new ListTaskPushNotificationConfigRequest_v0_3.Builder()
                .jsonrpc(JSONRPCMessage_v0_3.JSONRPC_VERSION)
                .method(ListTaskPushNotificationConfigRequest_v0_3.METHOD)
                .params(request)
                .build(); // id will be randomly generated

        PayloadAndHeaders_v0_3 payloadAndHeaders = applyInterceptors(ListTaskPushNotificationConfigRequest_v0_3.METHOD,
                listTaskPushNotificationRequest, agentCard, context);

        try {
            String httpResponseBody = sendPostRequest(payloadAndHeaders);
            ListTaskPushNotificationConfigResponse_v0_3 response = unmarshalResponse(httpResponseBody,
                    LIST_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE);
            return response.getResult();
        } catch (A2AClientException_v0_3 e) {
            throw e;
        } catch (IOException | InterruptedException | JsonProcessingException_v0_3 e) {
            throw new A2AClientException_v0_3("Failed to list task push notification configs: " + e, e);
        }
    }

    @Override
    public void deleteTaskPushNotificationConfigurations(DeleteTaskPushNotificationConfigParams_v0_3 request,
                                                         ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
        checkNotNullParam("request", request);
        DeleteTaskPushNotificationConfigRequest_v0_3 deleteTaskPushNotificationRequest = new DeleteTaskPushNotificationConfigRequest_v0_3.Builder()
                .jsonrpc(JSONRPCMessage_v0_3.JSONRPC_VERSION)
                .method(DeleteTaskPushNotificationConfigRequest_v0_3.METHOD)
                .params(request)
                .build(); // id will be randomly generated

        PayloadAndHeaders_v0_3 payloadAndHeaders = applyInterceptors(DeleteTaskPushNotificationConfigRequest_v0_3.METHOD,
                deleteTaskPushNotificationRequest, agentCard, context);

        try {
            String httpResponseBody = sendPostRequest(payloadAndHeaders);
            unmarshalResponse(httpResponseBody, DELETE_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE);
        } catch (A2AClientException_v0_3 e) {
            throw e;
        } catch (IOException | InterruptedException | JsonProcessingException_v0_3 e) {
            throw new A2AClientException_v0_3("Failed to delete task push notification configs: " + e, e);
        }
    }

    @Override
    public void resubscribe(TaskIdParams_v0_3 request, Consumer<StreamingEventKind_v0_3> eventConsumer,
                            Consumer<Throwable> errorConsumer, ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
        checkNotNullParam("request", request);
        checkNotNullParam("eventConsumer", eventConsumer);
        checkNotNullParam("errorConsumer", errorConsumer);
        TaskResubscriptionRequest_v0_3 taskResubscriptionRequest = new TaskResubscriptionRequest_v0_3.Builder()
                .jsonrpc(JSONRPCMessage_v0_3.JSONRPC_VERSION)
                .method(TaskResubscriptionRequest_v0_3.METHOD)
                .params(request)
                .build(); // id will be randomly generated

        PayloadAndHeaders_v0_3 payloadAndHeaders = applyInterceptors(TaskResubscriptionRequest_v0_3.METHOD,
                taskResubscriptionRequest, agentCard, context);

        AtomicReference<CompletableFuture<Void>> ref = new AtomicReference<>();
        SSEEventListener_v0_3 sseEventListener = new SSEEventListener_v0_3(eventConsumer, errorConsumer);

        try {
            A2AHttpClient_v0_3.PostBuilder builder = createPostBuilder(payloadAndHeaders);
            ref.set(builder.postAsyncSSE(
                    msg -> sseEventListener.onMessage(msg, ref.get()),
                    throwable -> sseEventListener.onError(throwable, ref.get()),
                    () -> {
                        // Signal normal stream completion to error handler (null error means success)
                        sseEventListener.onComplete();
                    }));
        } catch (IOException e) {
            throw new A2AClientException_v0_3("Failed to send task resubscription request: " + e, e);
        } catch (InterruptedException e) {
            throw new A2AClientException_v0_3("Task resubscription request timed out: " + e, e);
        } catch (JsonProcessingException_v0_3 e) {
            throw new A2AClientException_v0_3("Failed to process JSON for task resubscription request: " + e, e);
        }
    }

    @Override
    public AgentCard_v0_3 getAgentCard(ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
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

            GetAuthenticatedExtendedCardRequest_v0_3 getExtendedAgentCardRequest = new GetAuthenticatedExtendedCardRequest_v0_3.Builder()
                    .jsonrpc(JSONRPCMessage_v0_3.JSONRPC_VERSION)
                    .method(GetAuthenticatedExtendedCardRequest_v0_3.METHOD)
                    .build(); // id will be randomly generated

            PayloadAndHeaders_v0_3 payloadAndHeaders = applyInterceptors(GetAuthenticatedExtendedCardRequest_v0_3.METHOD,
                    getExtendedAgentCardRequest, agentCard, context);

            try {
                String httpResponseBody = sendPostRequest(payloadAndHeaders);
                GetAuthenticatedExtendedCardResponse_v0_3 response = unmarshalResponse(httpResponseBody,
                        GET_AUTHENTICATED_EXTENDED_CARD_RESPONSE_REFERENCE);
                agentCard = response.getResult();
                needsExtendedCard = false;
                return agentCard;
            } catch (IOException | InterruptedException | JsonProcessingException_v0_3 e) {
                throw new A2AClientException_v0_3("Failed to get authenticated extended agent card: " + e, e);
            }
        } catch(A2AClientError_v0_3 e){
            throw new A2AClientException_v0_3("Failed to get agent card: " + e, e);
        }
    }

    @Override
    public void close() {
        // no-op
    }

    private PayloadAndHeaders_v0_3 applyInterceptors(String methodName, Object payload,
                                                     AgentCard_v0_3 agentCard, ClientCallContext_v0_3 clientCallContext) {
        PayloadAndHeaders_v0_3 payloadAndHeaders = new PayloadAndHeaders_v0_3(payload, getHttpHeaders(clientCallContext));
        if (interceptors != null && ! interceptors.isEmpty()) {
            for (ClientCallInterceptor_v0_3 interceptor : interceptors) {
                payloadAndHeaders = interceptor.intercept(methodName, payloadAndHeaders.getPayload(),
                        payloadAndHeaders.getHeaders(), agentCard, clientCallContext);
            }
        }
        return payloadAndHeaders;
    }

    private String sendPostRequest(PayloadAndHeaders_v0_3 payloadAndHeaders) throws IOException, InterruptedException, JsonProcessingException_v0_3 {
        A2AHttpClient_v0_3.PostBuilder builder = createPostBuilder(payloadAndHeaders);
        A2AHttpResponse_v0_3 response = builder.post();
        if (!response.success()) {
            throw new IOException("Request failed " + response.status());
        }
        return response.body();
    }

    private A2AHttpClient_v0_3.PostBuilder createPostBuilder(PayloadAndHeaders_v0_3 payloadAndHeaders) throws JsonProcessingException_v0_3 {
        A2AHttpClient_v0_3.PostBuilder postBuilder = httpClient.createPost()
                .url(agentUrl)
                .addHeader("Content-Type", "application/json")
                .body(JsonUtil_v0_3.toJson(payloadAndHeaders.getPayload()));

        if (payloadAndHeaders.getHeaders() != null) {
            for (Map.Entry<String, String> entry : payloadAndHeaders.getHeaders().entrySet()) {
                postBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        return postBuilder;
    }

    private <T extends JSONRPCResponse_v0_3<?>> T unmarshalResponse(String response, Class<T> responseClass)
            throws A2AClientException_v0_3, JsonProcessingException_v0_3 {
        T value = JsonUtil_v0_3.fromJson(response, responseClass);
        JSONRPCError_v0_3 error = value.getError();
        if (error != null) {
            throw new A2AClientException_v0_3(error.getMessage() + (error.getData() != null ? ": " + error.getData() : ""), error);
        }
        return value;
    }

    private Map<String, String> getHttpHeaders(ClientCallContext_v0_3 context) {
        return context != null ? context.getHeaders() : null;
    }
}