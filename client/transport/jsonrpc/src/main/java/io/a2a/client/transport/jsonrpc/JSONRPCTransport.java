package io.a2a.client.transport.jsonrpc;

import static io.a2a.util.Assert.checkNotNullParam;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import io.a2a.client.http.A2ACardResolver;
import io.a2a.client.transport.spi.AbstractClientTransport;
import io.a2a.client.transport.spi.interceptors.ClientCallContext;
import io.a2a.client.transport.spi.interceptors.ClientCallInterceptor;
import io.a2a.client.transport.spi.interceptors.PayloadAndHeaders;
import io.a2a.client.http.HttpClient;
import io.a2a.client.http.HttpResponse;
import io.a2a.common.A2AErrorMessages;
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
import java.util.function.Function;
import java.util.logging.Logger;

import io.a2a.util.Utils;
import org.jspecify.annotations.Nullable;

public class JSONRPCTransport extends AbstractClientTransport {

    private static final Logger log = Logger.getLogger(JSONRPCTransport.class.getName());

    private static final TypeReference<SendMessageResponse> SEND_MESSAGE_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<GetTaskResponse> GET_TASK_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<CancelTaskResponse> CANCEL_TASK_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<GetTaskPushNotificationConfigResponse> GET_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<SetTaskPushNotificationConfigResponse> SET_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<ListTaskPushNotificationConfigResponse> LIST_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<DeleteTaskPushNotificationConfigResponse> DELETE_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<GetAuthenticatedExtendedCardResponse> GET_AUTHENTICATED_EXTENDED_CARD_RESPONSE_REFERENCE = new TypeReference<>() {};

    private final HttpClient httpClient;
    private final String agentPath;
    private AgentCard agentCard;
    private boolean needsExtendedCard = false;

    public JSONRPCTransport(String agentUrl) {
        this(null, null, agentUrl, null);
    }

    public JSONRPCTransport(@Nullable HttpClient httpClient, @Nullable AgentCard agentCard,
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

        this.needsExtendedCard = agentCard == null || agentCard.supportsAuthenticatedExtendedCard();
    }

    @Override
    public CompletableFuture<EventKind> sendMessage(MessageSendParams request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        SendMessageRequest sendMessageRequest = new SendMessageRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(SendMessageRequest.METHOD)
                .params(request)
                .build(); // id will be randomly generated

        PayloadAndHeaders payloadAndHeaders = applyInterceptors(SendMessageRequest.METHOD, sendMessageRequest, agentCard, context);

        try {
            return sendPostRequest(payloadAndHeaders)
                    .thenCompose(new Function<String, CompletionStage<EventKind>>() {
                        @Override
                        public CompletionStage<EventKind> apply(String httpResponseBody) {
                            try {
                                return CompletableFuture.completedFuture(unmarshalResponse(httpResponseBody, SEND_MESSAGE_RESPONSE_REFERENCE).getResult());
                            } catch (A2AClientException e) {
                                return CompletableFuture.failedFuture(e);
                            } catch (IOException e) {
                                return CompletableFuture.failedFuture(new A2AClientException("Failed to send message: " + e, e));
                            }
                        }
                    });
        } catch (IOException e) {
            return CompletableFuture.failedFuture(new A2AClientException("Failed to prepare request: " + e, e));
        }
    }

    @Override
    public void sendMessageStreaming(MessageSendParams request, Consumer<StreamingEventKind> eventConsumer,
                                     Consumer<Throwable> errorConsumer, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        checkNotNullParam("eventConsumer", eventConsumer);
        SendStreamingMessageRequest sendStreamingMessageRequest = new SendStreamingMessageRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(SendStreamingMessageRequest.METHOD)
                .params(request)
                .build(); // id will be randomly generated

        PayloadAndHeaders payloadAndHeaders = applyInterceptors(SendStreamingMessageRequest.METHOD, sendStreamingMessageRequest, agentCard, context);

        AtomicReference<CompletableFuture<HttpResponse>> ref = new AtomicReference<>();
        SSEEventListener sseEventListener = new SSEEventListener(eventConsumer, errorConsumer);

        try {
            HttpClient.PostRequestBuilder builder = createPostBuilder(payloadAndHeaders).asSSE();
            ref.set(builder.send()
                    .whenComplete(new BiConsumer<HttpResponse, Throwable>() {
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
    public CompletableFuture<Task> getTask(TaskQueryParams request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        GetTaskRequest getTaskRequest = new GetTaskRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(GetTaskRequest.METHOD)
                .params(request)
                .build(); // id will be randomly generated

        PayloadAndHeaders payloadAndHeaders = applyInterceptors(GetTaskRequest.METHOD, getTaskRequest, agentCard, context);

        try {
            return sendPostRequest(payloadAndHeaders)
                    .thenCompose(new Function<String, CompletionStage<Task>>() {
                        @Override
                        public CompletionStage<Task> apply(String httpResponseBody) {
                            try {
                                return CompletableFuture.completedFuture(unmarshalResponse(httpResponseBody, GET_TASK_RESPONSE_REFERENCE).getResult());
                            } catch (A2AClientException e) {
                                return CompletableFuture.failedFuture(e);
                            } catch (IOException e) {
                                return CompletableFuture.failedFuture(new A2AClientException("Failed to get task: " + e, e));
                            }
                        }
                    });
        } catch (IOException e) {
            return CompletableFuture.failedFuture(new A2AClientException("Failed to prepare request: " + e, e));
        }
    }

    @Override
    public CompletableFuture<Task> cancelTask(TaskIdParams request, @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        CancelTaskRequest cancelTaskRequest = new CancelTaskRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(CancelTaskRequest.METHOD)
                .params(request)
                .build(); // id will be randomly generated

        PayloadAndHeaders payloadAndHeaders = applyInterceptors(CancelTaskRequest.METHOD, cancelTaskRequest, agentCard, context);

        try {
            return sendPostRequest(payloadAndHeaders)
                    .thenCompose(new Function<String, CompletionStage<Task>>() {
                        @Override
                        public CompletionStage<Task> apply(String httpResponseBody) {
                            try {
                                return CompletableFuture.completedFuture(unmarshalResponse(httpResponseBody, CANCEL_TASK_RESPONSE_REFERENCE).getResult());
                            } catch (A2AClientException e) {
                                return CompletableFuture.failedFuture(e);
                            } catch (IOException e) {
                                return CompletableFuture.failedFuture(new A2AClientException("Failed to cancel task: " + e, e));
                            }
                        }
                    });
        } catch (IOException e) {
            return CompletableFuture.failedFuture(new A2AClientException("Failed to prepare request: " + e, e));
        }
    }

    @Override
    public CompletableFuture<TaskPushNotificationConfig> setTaskPushNotificationConfiguration(TaskPushNotificationConfig request,
                                                                           @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        SetTaskPushNotificationConfigRequest setTaskPushNotificationRequest = new SetTaskPushNotificationConfigRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(SetTaskPushNotificationConfigRequest.METHOD)
                .params(request)
                .build(); // id will be randomly generated

        PayloadAndHeaders payloadAndHeaders = applyInterceptors(SetTaskPushNotificationConfigRequest.METHOD,
                setTaskPushNotificationRequest, agentCard, context);

        try {
            return sendPostRequest(payloadAndHeaders)
                    .thenCompose(new Function<String, CompletionStage<TaskPushNotificationConfig>>() {
                        @Override
                        public CompletionStage<TaskPushNotificationConfig> apply(String httpResponseBody) {
                            try {
                                return CompletableFuture.completedFuture(unmarshalResponse(httpResponseBody, SET_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE).getResult());
                            } catch (A2AClientException e) {
                                return CompletableFuture.failedFuture(e);
                            } catch (IOException e) {
                                return CompletableFuture.failedFuture(new A2AClientException("Failed to set task push notification config: " + e, e));
                            }
                        }
                    });
        } catch (IOException e) {
            return CompletableFuture.failedFuture(new A2AClientException("Failed to prepare request: " + e, e));
        }
    }

    @Override
    public CompletableFuture<TaskPushNotificationConfig> getTaskPushNotificationConfiguration(GetTaskPushNotificationConfigParams request,
                                                                           @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        GetTaskPushNotificationConfigRequest getTaskPushNotificationRequest = new GetTaskPushNotificationConfigRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(GetTaskPushNotificationConfigRequest.METHOD)
                .params(request)
                .build(); // id will be randomly generated

        PayloadAndHeaders payloadAndHeaders = applyInterceptors(GetTaskPushNotificationConfigRequest.METHOD,
                getTaskPushNotificationRequest, agentCard, context);

        try {
            return sendPostRequest(payloadAndHeaders)
                    .thenCompose(new Function<String, CompletionStage<TaskPushNotificationConfig>>() {
                        @Override
                        public CompletionStage<TaskPushNotificationConfig> apply(String httpResponseBody) {
                            try {
                                return CompletableFuture.completedFuture(unmarshalResponse(httpResponseBody, GET_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE).getResult());
                            } catch (A2AClientException e) {
                                return CompletableFuture.failedFuture(e);
                            } catch (IOException e) {
                                return CompletableFuture.failedFuture(new A2AClientException("Failed to get task push notification config: " + e, e));
                            }
                        }
                    });
        } catch (IOException e) {
            return CompletableFuture.failedFuture(new A2AClientException("Failed to prepare request: " + e, e));
        }
    }

    @Override
    public CompletableFuture<List<TaskPushNotificationConfig>> listTaskPushNotificationConfigurations(
            ListTaskPushNotificationConfigParams request,
            @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        ListTaskPushNotificationConfigRequest listTaskPushNotificationRequest = new ListTaskPushNotificationConfigRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(ListTaskPushNotificationConfigRequest.METHOD)
                .params(request)
                .build(); // id will be randomly generated

        PayloadAndHeaders payloadAndHeaders = applyInterceptors(ListTaskPushNotificationConfigRequest.METHOD,
                listTaskPushNotificationRequest, agentCard, context);

        try {
            return sendPostRequest(payloadAndHeaders)
                    .thenCompose(new Function<String, CompletionStage<List<TaskPushNotificationConfig>>>() {
                        @Override
                        public CompletionStage<List<TaskPushNotificationConfig>> apply(String httpResponseBody) {
                            try {
                                return CompletableFuture.completedFuture(unmarshalResponse(httpResponseBody, LIST_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE).getResult());
                            } catch (A2AClientException e) {
                                return CompletableFuture.failedFuture(e);
                            } catch (IOException e) {
                                return CompletableFuture.failedFuture(new A2AClientException("Failed to list task push notification configs: " + e, e));
                            }
                        }
                    });
        } catch (IOException e) {
            return CompletableFuture.failedFuture(new A2AClientException("Failed to prepare request: " + e, e));
        }
    }

    @Override
    public CompletableFuture<Void> deleteTaskPushNotificationConfigurations(DeleteTaskPushNotificationConfigParams request,
                                                         @Nullable ClientCallContext context) throws A2AClientException {
        checkNotNullParam("request", request);
        DeleteTaskPushNotificationConfigRequest deleteTaskPushNotificationRequest = new DeleteTaskPushNotificationConfigRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(DeleteTaskPushNotificationConfigRequest.METHOD)
                .params(request)
                .build(); // id will be randomly generated

        PayloadAndHeaders payloadAndHeaders = applyInterceptors(DeleteTaskPushNotificationConfigRequest.METHOD,
                deleteTaskPushNotificationRequest, agentCard, context);

        try {
            return sendPostRequest(payloadAndHeaders)
                    .thenCompose(new Function<String, CompletionStage<Void>>() {
                        @Override
                        public CompletionStage<Void> apply(String httpResponseBody) {
                            try {
                                unmarshalResponse(httpResponseBody, DELETE_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE);
                                return CompletableFuture.completedFuture(null);
                            } catch (A2AClientException e) {
                                return CompletableFuture.failedFuture(e);
                            } catch (IOException e) {
                                return CompletableFuture.failedFuture(new A2AClientException("Failed to delete task push notification configs: " + e, e));
                            }
                        }
                    });
        } catch (IOException e) {
            return CompletableFuture.failedFuture(new A2AClientException("Failed to prepare request: " + e, e));
        }
    }

    @Override
    public void resubscribe(TaskIdParams request, Consumer<StreamingEventKind> eventConsumer,
                            Consumer<Throwable> errorConsumer, @Nullable ClientCallContext context) throws A2AClientException {
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

        AtomicReference<CompletableFuture<HttpResponse>> ref = new AtomicReference<>();
        SSEEventListener sseEventListener = new SSEEventListener(eventConsumer, errorConsumer);

        try {
            HttpClient.PostRequestBuilder builder = createPostBuilder(payloadAndHeaders).asSSE();
            ref.set(builder.send().whenComplete(new BiConsumer<HttpResponse, Throwable>() {
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
            throw new A2AClientException("Failed to send task resubscription request: " + e, e);
        }
    }

    @Override
    public CompletableFuture<AgentCard> getAgentCard(@Nullable ClientCallContext context) throws A2AClientException {
            if (agentCard == null) {
                try {
                    A2ACardResolver resolver = new A2ACardResolver(httpClient, agentPath, getHttpHeaders(context));
                    agentCard = resolver.getAgentCard();
                    needsExtendedCard = agentCard.supportsAuthenticatedExtendedCard();
                } catch (A2AClientError e) {
                    return CompletableFuture.failedFuture(new A2AClientException("Failed to get agent card: " + e, e));
                }
            }
            if (!needsExtendedCard) {
                return CompletableFuture.completedFuture(agentCard);
            }

            GetAuthenticatedExtendedCardRequest getExtendedAgentCardRequest = new GetAuthenticatedExtendedCardRequest.Builder()
                    .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                    .method(GetAuthenticatedExtendedCardRequest.METHOD)
                    .build(); // id will be randomly generated

            PayloadAndHeaders payloadAndHeaders = applyInterceptors(GetAuthenticatedExtendedCardRequest.METHOD,
                    getExtendedAgentCardRequest, agentCard, context);

            try {
                return sendPostRequest(payloadAndHeaders)
                        .thenCompose(new Function<String, CompletionStage<AgentCard>>() {
                            @Override
                            public CompletionStage<AgentCard> apply(String httpResponseBody) {
                                try {
                                    return CompletableFuture.completedFuture(unmarshalResponse(httpResponseBody, GET_AUTHENTICATED_EXTENDED_CARD_RESPONSE_REFERENCE).getResult());
                                } catch (A2AClientException e) {
                                    return CompletableFuture.failedFuture(e);
                                } catch (IOException e) {
                                    return CompletableFuture.failedFuture(new A2AClientException("Failed to get authenticated extended agent card: " + e, e));
                                }
                            }
                        }).whenComplete(new BiConsumer<AgentCard, Throwable>() {
                            @Override
                            public void accept(AgentCard agentCard, Throwable throwable) {
                                JSONRPCTransport.this.agentCard = agentCard;
                                needsExtendedCard = false;
                            }
                        });
            } catch (IOException e) {
                return CompletableFuture.failedFuture(new A2AClientException("Failed to prepare request: " + e, e));
            }
    }

    @Override
    public void close() {
        // no-op
    }

    private CompletableFuture<String> sendPostRequest(PayloadAndHeaders payloadAndHeaders) throws JsonProcessingException {
        return createPostBuilder(payloadAndHeaders)
                .send()
                .thenCompose(new Function<HttpResponse, CompletionStage<String>>() {
                    @Override
                    public CompletionStage<String> apply(HttpResponse response) {
                        if (!response.success()) {
                            log.fine("Error on POST processing " + payloadAndHeaders.getPayload());
                            if (response.statusCode() == HTTP_UNAUTHORIZED) {
                                return CompletableFuture.failedStage(new A2AClientException(A2AErrorMessages.AUTHENTICATION_FAILED));
                            } else if (response.statusCode() == HTTP_FORBIDDEN) {
                                return CompletableFuture.failedStage(new A2AClientException(A2AErrorMessages.AUTHORIZATION_FAILED));
                            }

                            return CompletableFuture.failedFuture(new A2AClientException("Request failed " + response.statusCode()));
                        }

                        return response.body();
                    }
                });
        /*
        try {
            HttpResponse response = builder.send().get();
            if (!response.success()) {
                throw ;
            }
            return response.body();

        } catch (ExecutionException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw new IOException("Failed to send request", e.getCause());
        }
         */
    }

    private HttpClient.PostRequestBuilder createPostBuilder(PayloadAndHeaders payloadAndHeaders) throws JsonProcessingException {
        HttpClient.PostRequestBuilder postBuilder = httpClient.post(agentPath)
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

    private Map<String, String> getHttpHeaders(@Nullable ClientCallContext context) {
        return context != null ? context.getHeaders() : null;
    }
}