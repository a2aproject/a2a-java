package io.a2a.client.transport.jsonrpc;

import static io.a2a.util.Assert.checkNotNullParam;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import io.a2a.client.A2ACardResolver;
import io.a2a.client.config.ClientCallContext;
import io.a2a.client.config.ClientCallInterceptor;
import io.a2a.client.config.PayloadAndHeaders;
import io.a2a.client.http.A2AHttpClient;
import io.a2a.client.http.A2AHttpResponse;
import io.a2a.client.http.JdkA2AHttpClient;
import io.a2a.client.transport.spi.ClientTransport;
import io.a2a.spec.A2AClientError;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.CancelTaskRequest;
import io.a2a.spec.CancelTaskResponse;

import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.EventKind;
import io.a2a.spec.GetAuthenticatedExtendedCardRequest;
import io.a2a.spec.GetAuthenticatedExtendedCardResponse;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.GetTaskPushNotificationConfigRequest;
import io.a2a.spec.GetTaskPushNotificationConfigResponse;
import io.a2a.spec.GetTaskRequest;
import io.a2a.spec.GetTaskResponse;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.JSONRPCMessage;
import io.a2a.spec.JSONRPCResponse;

import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigRequest;
import io.a2a.spec.ListTaskPushNotificationConfigResponse;
import io.a2a.spec.DeleteTaskPushNotificationConfigRequest;
import io.a2a.spec.DeleteTaskPushNotificationConfigResponse;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendMessageResponse;
import io.a2a.spec.SendStreamingMessageRequest;
import io.a2a.spec.SetTaskPushNotificationConfigRequest;
import io.a2a.spec.SetTaskPushNotificationConfigResponse;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import io.a2a.spec.TaskResubscriptionRequest;
import io.a2a.client.transport.jsonrpc.sse.SSEEventListener;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import io.a2a.util.Utils;

public class JSONRPCTransport implements ClientTransport {

    private static final TypeReference<SendMessageResponse> SEND_MESSAGE_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<GetTaskResponse> GET_TASK_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<CancelTaskResponse> CANCEL_TASK_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<GetTaskPushNotificationConfigResponse> GET_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<SetTaskPushNotificationConfigResponse> SET_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<ListTaskPushNotificationConfigResponse> LIST_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<DeleteTaskPushNotificationConfigResponse> DELETE_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<GetAuthenticatedExtendedCardResponse> GET_AUTHENTICATED_EXTENDED_CARD_RESPONSE_REFERENCE = new TypeReference<>() {};

    private final A2AHttpClient httpClient;
    private final String agentUrl;
    private final List<ClientCallInterceptor> interceptors;
    private AgentCard agentCard;
    private boolean needsExtendedCard = false;

    public JSONRPCTransport(String agentUrl) {
        this(null, null, agentUrl, null);
    }

    public JSONRPCTransport(AgentCard agentCard) {
        this(null, agentCard, agentCard.url(), null);
    }

    public JSONRPCTransport(A2AHttpClient httpClient, AgentCard agentCard,
                            String agentUrl, List<ClientCallInterceptor> interceptors) {
        this.httpClient = httpClient == null ? new JdkA2AHttpClient() : httpClient;
        this.agentCard = agentCard;
        this.agentUrl = agentUrl;
        this.interceptors = interceptors;
        this.needsExtendedCard = agentCard == null || agentCard.supportsAuthenticatedExtendedCard();
    }

    @Override
    public EventKind sendMessage(MessageSendParams request, ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        SendMessageRequest sendMessageRequest = new SendMessageRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(SendMessageRequest.METHOD)
                .params(request)
                .build(); // id will be randomly generated

        PayloadAndHeaders payloadAndHeaders = applyInterceptors(SendMessageRequest.METHOD, sendMessageRequest,
                agentCard, context);

        try {
            String httpResponseBody = sendPostRequest(payloadAndHeaders);
            SendMessageResponse response = unmarshalResponse(httpResponseBody, SEND_MESSAGE_RESPONSE_REFERENCE);
            return response.getResult();
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            throw new A2AClientException("Failed to send message: " + e, e);
        }
    }

    @Override
    public void sendMessageStreaming(MessageSendParams request, Consumer<StreamingEventKind> eventConsumer,
                                     Consumer<Throwable> errorConsumer, ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        checkNotNullParam("eventConsumer", eventConsumer);
        SendStreamingMessageRequest sendStreamingMessageRequest = new SendStreamingMessageRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(SendStreamingMessageRequest.METHOD)
                .params(request)
                .build(); // id will be randomly generated

        PayloadAndHeaders payloadAndHeaders = applyInterceptors(SendStreamingMessageRequest.METHOD,
                sendStreamingMessageRequest, agentCard, context);

        AtomicReference<CompletableFuture<Void>> ref = new AtomicReference<>();
        SSEEventListener sseEventListener = new SSEEventListener(eventConsumer, errorConsumer);

        try {
            A2AHttpClient.PostBuilder builder = createPostBuilder(payloadAndHeaders);
            ref.set(builder.postAsyncSSE(
                    msg -> sseEventListener.onMessage(msg, ref.get()),
                    throwable -> sseEventListener.onError(throwable, ref.get()),
                    () -> {
                        // We don't need to do anything special on completion
                    }));
        } catch (IOException e) {
            throw new A2AClientException("Failed to send streaming message request: " + e, e);
        } catch (InterruptedException e) {
            throw new A2AClientException("Send streaming message request timed out: " + e, e);
        }
    }

    @Override
    public Task getTask(TaskQueryParams request, ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        GetTaskRequest getTaskRequest = new GetTaskRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(GetTaskRequest.METHOD)
                .params(request)
                .build(); // id will be randomly generated

        PayloadAndHeaders payloadAndHeaders = applyInterceptors(GetTaskRequest.METHOD, getTaskRequest,
                agentCard, context);

        try {
            String httpResponseBody = sendPostRequest(payloadAndHeaders);
            GetTaskResponse response = unmarshalResponse(httpResponseBody, GET_TASK_RESPONSE_REFERENCE);
            return response.getResult();
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            throw new A2AClientException("Failed to get task: " + e, e);
        }
    }

    @Override
    public Task cancelTask(TaskIdParams request, ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        CancelTaskRequest cancelTaskRequest = new CancelTaskRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(CancelTaskRequest.METHOD)
                .params(request)
                .build(); // id will be randomly generated

        PayloadAndHeaders payloadAndHeaders = applyInterceptors(CancelTaskRequest.METHOD, cancelTaskRequest,
                agentCard, context);

        try {
            String httpResponseBody = sendPostRequest(payloadAndHeaders);
            CancelTaskResponse response = unmarshalResponse(httpResponseBody, CANCEL_TASK_RESPONSE_REFERENCE);
            return response.getResult();
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            throw new A2AClientException("Failed to cancel task: " + e, e);
        }
    }

    @Override
    public TaskPushNotificationConfig setTaskPushNotificationConfiguration(TaskPushNotificationConfig request,
                                                                           ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        SetTaskPushNotificationConfigRequest setTaskPushNotificationRequest = new SetTaskPushNotificationConfigRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(SetTaskPushNotificationConfigRequest.METHOD)
                .params(request)
                .build(); // id will be randomly generated

        PayloadAndHeaders payloadAndHeaders = applyInterceptors(SetTaskPushNotificationConfigRequest.METHOD,
                setTaskPushNotificationRequest, agentCard, context);

        try {
            String httpResponseBody = sendPostRequest(payloadAndHeaders);
            SetTaskPushNotificationConfigResponse response = unmarshalResponse(httpResponseBody,
                    SET_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE);
            return response.getResult();
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            throw new A2AClientException("Failed to set task push notification config: " + e, e);
        }
    }

    @Override
    public TaskPushNotificationConfig getTaskPushNotificationConfiguration(GetTaskPushNotificationConfigParams request,
                                                                           ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        GetTaskPushNotificationConfigRequest getTaskPushNotificationRequest = new GetTaskPushNotificationConfigRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(GetTaskPushNotificationConfigRequest.METHOD)
                .params(request)
                .build(); // id will be randomly generated

        PayloadAndHeaders payloadAndHeaders = applyInterceptors(GetTaskPushNotificationConfigRequest.METHOD,
                getTaskPushNotificationRequest, agentCard, context);

        try {
            String httpResponseBody = sendPostRequest(payloadAndHeaders);
            GetTaskPushNotificationConfigResponse response = unmarshalResponse(httpResponseBody,
                    GET_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE);
            return response.getResult();
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            throw new A2AClientException("Failed to get task push notification config: " + e, e);
        }
    }

    @Override
    public List<TaskPushNotificationConfig> listTaskPushNotificationConfigurations(
            ListTaskPushNotificationConfigParams request,
            ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        ListTaskPushNotificationConfigRequest listTaskPushNotificationRequest = new ListTaskPushNotificationConfigRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(ListTaskPushNotificationConfigRequest.METHOD)
                .params(request)
                .build(); // id will be randomly generated

        PayloadAndHeaders payloadAndHeaders = applyInterceptors(ListTaskPushNotificationConfigRequest.METHOD,
                listTaskPushNotificationRequest, agentCard, context);

        try {
            String httpResponseBody = sendPostRequest(payloadAndHeaders);
            ListTaskPushNotificationConfigResponse response = unmarshalResponse(httpResponseBody,
                    LIST_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE);
            return response.getResult();
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            throw new A2AClientException("Failed to list task push notification configs: " + e, e);
        }
    }

    @Override
    public void deleteTaskPushNotificationConfigurations(DeleteTaskPushNotificationConfigParams request,
                                                         ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        DeleteTaskPushNotificationConfigRequest deleteTaskPushNotificationRequest = new DeleteTaskPushNotificationConfigRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(DeleteTaskPushNotificationConfigRequest.METHOD)
                .params(request)
                .build(); // id will be randomly generated

        PayloadAndHeaders payloadAndHeaders = applyInterceptors(DeleteTaskPushNotificationConfigRequest.METHOD,
                deleteTaskPushNotificationRequest, agentCard, context);

        try {
            String httpResponseBody = sendPostRequest(payloadAndHeaders);
            unmarshalResponse(httpResponseBody, DELETE_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE);
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            throw new A2AClientException("Failed to delete task push notification configs: " + e, e);
        }
    }

    @Override
    public void resubscribe(TaskIdParams request, Consumer<StreamingEventKind> eventConsumer,
                            Consumer<Throwable> errorConsumer, ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        checkNotNullParam("eventConsumer", eventConsumer);
        checkNotNullParam("errorConsumer", errorConsumer);
        TaskResubscriptionRequest taskResubscriptionRequest = new TaskResubscriptionRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(TaskResubscriptionRequest.METHOD)
                .params(request)
                .build(); // id will be randomly generated

        PayloadAndHeaders payloadAndHeaders = applyInterceptors(TaskResubscriptionRequest.METHOD,
                taskResubscriptionRequest, agentCard, context);

        AtomicReference<CompletableFuture<Void>> ref = new AtomicReference<>();
        SSEEventListener sseEventListener = new SSEEventListener(eventConsumer, errorConsumer);

        try {
            A2AHttpClient.PostBuilder builder = createPostBuilder(payloadAndHeaders);
            ref.set(builder.postAsyncSSE(
                    msg -> sseEventListener.onMessage(msg, ref.get()),
                    throwable -> sseEventListener.onError(throwable, ref.get()),
                    () -> {
                        // We don't need to do anything special on completion
                    }));
        } catch (IOException e) {
            throw new A2AClientException("Failed to send task resubscription request: " + e, e);
        } catch (InterruptedException e) {
            throw new A2AClientException("Task resubscription request timed out: " + e, e);
        }
    }

    @Override
    public AgentCard getAgentCard(ClientCallContext context) throws A2AClientException {
        A2ACardResolver resolver;
        try {
            if (agentCard == null) {
                resolver = new A2ACardResolver(httpClient, agentUrl, null, getHttpHeaders(context));
                agentCard = resolver.getAgentCard();
                needsExtendedCard = agentCard.supportsAuthenticatedExtendedCard();
            }
            if (!needsExtendedCard) {
                return agentCard;
            }

            GetAuthenticatedExtendedCardRequest getExtendedAgentCardRequest = new GetAuthenticatedExtendedCardRequest.Builder()
                    .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                    .method(GetAuthenticatedExtendedCardRequest.METHOD)
                    .build(); // id will be randomly generated

            PayloadAndHeaders payloadAndHeaders = applyInterceptors(GetAuthenticatedExtendedCardRequest.METHOD,
                    getExtendedAgentCardRequest, agentCard, context);

            try {
                String httpResponseBody = sendPostRequest(payloadAndHeaders);
                GetAuthenticatedExtendedCardResponse response = unmarshalResponse(httpResponseBody,
                        GET_AUTHENTICATED_EXTENDED_CARD_RESPONSE_REFERENCE);
                agentCard = response.getResult();
                needsExtendedCard = false;
                return agentCard;
            } catch (IOException | InterruptedException e) {
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

    private PayloadAndHeaders applyInterceptors(String methodName, Object payload,
                                                AgentCard agentCard, ClientCallContext clientCallContext) {
        PayloadAndHeaders payloadAndHeaders = new PayloadAndHeaders(payload, getHttpHeaders(clientCallContext));
        if (interceptors != null && ! interceptors.isEmpty()) {
            for (ClientCallInterceptor interceptor : interceptors) {
                payloadAndHeaders = interceptor.intercept(methodName, payloadAndHeaders.getPayload(),
                        payloadAndHeaders.getHeaders(), agentCard, clientCallContext);
            }
        }
        return payloadAndHeaders;
    }

    private String sendPostRequest(PayloadAndHeaders payloadAndHeaders) throws IOException, InterruptedException {
        A2AHttpClient.PostBuilder builder = createPostBuilder(payloadAndHeaders);
        A2AHttpResponse response = builder.post();
        if (!response.success()) {
            throw new IOException("Request failed " + response.status());
        }
        return response.body();
    }

    private A2AHttpClient.PostBuilder createPostBuilder(PayloadAndHeaders payloadAndHeaders) throws JsonProcessingException {
        A2AHttpClient.PostBuilder postBuilder = httpClient.createPost()
                .url(agentUrl)
                .addHeader("Content-Type", "application/json")
                .body(Utils.OBJECT_MAPPER.writeValueAsString(payloadAndHeaders.getPayload()));

        if (payloadAndHeaders.getHeaders() != null) {
            for (Map.Entry<String, String> entry : payloadAndHeaders.getHeaders().entrySet()) {
                postBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        return postBuilder;
    }

    private <T extends JSONRPCResponse<?>> T unmarshalResponse(String response, TypeReference<T> typeReference)
            throws A2AClientException, JsonProcessingException {
        T value = Utils.unmarshalFrom(response, typeReference);
        JSONRPCError error = value.getError();
        if (error != null) {
            throw new A2AClientException(error.getMessage() + (error.getData() != null ? ": " + error.getData() : ""), error);
        }
        return value;
    }

    private Map<String, String> getHttpHeaders(ClientCallContext context) {
        return context != null ? context.getHeaders() : null;
    }
}