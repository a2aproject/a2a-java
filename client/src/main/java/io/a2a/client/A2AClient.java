package io.a2a.client;

import static io.a2a.util.Assert.checkNotNullParam;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import io.a2a.client.sse.SSEEventListener;
import io.a2a.http.A2AHttpClient;
import io.a2a.http.A2AHttpResponse;
import io.a2a.http.JdkA2AHttpClient;
import io.a2a.A2A;
import io.a2a.spec.A2AClientError;
import io.a2a.spec.A2AClientJSONError;
import io.a2a.spec.A2AServerException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.CancelTaskRequest;
import io.a2a.spec.CancelTaskResponse;
import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.DeleteTaskPushNotificationConfigRequest;
import io.a2a.spec.DeleteTaskPushNotificationConfigResponse;
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
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendMessageResponse;
import io.a2a.spec.SendStreamingMessageRequest;
import io.a2a.spec.SetTaskPushNotificationConfigRequest;
import io.a2a.spec.SetTaskPushNotificationConfigResponse;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import io.a2a.spec.TaskResubscriptionRequest;
import io.a2a.util.Utils;

/**
 * An A2A client.
 */
public class A2AClient {

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
    private AgentCard agentCard;


    /**
     * Create a new A2AClient.
     *
     * @param agentCard the agent card for the A2A server this client will be communicating with
     */
    public A2AClient(AgentCard agentCard) {
        checkNotNullParam("agentCard", agentCard);
        this.agentCard = agentCard;
        this.agentUrl = agentCard.url();
        this.httpClient = new JdkA2AHttpClient();
    }

    /**
     * Create a new A2AClient.
     *
     * @param agentUrl the URL for the A2A server this client will be communicating with
     */
    public A2AClient(String agentUrl) {
        checkNotNullParam("agentUrl", agentUrl);
        this.agentUrl = agentUrl;
        this.httpClient = new JdkA2AHttpClient();
    }

    /**
     * Fetches the agent card and initialises an A2A client.
     *
     * @param httpClient the {@link  A2AHttpClient} to use
     * @param baseUrl the base URL of the agent's host
     * @param agentCardPath the path to the agent card endpoint, relative to the {@code baseUrl}. If {@code null},  the
     *                      value {@link A2ACardResolver#DEFAULT_AGENT_CARD_PATH} will be used
     * @return an initialised {@code A2AClient} instance
     * @throws A2AClientError If an HTTP error occurs fetching the card
     * @throws A2AClientJSONError if the agent card response is invalid
     */
    public static A2AClient getClientFromAgentCardUrl(A2AHttpClient httpClient, String baseUrl,
                                                      String agentCardPath) throws A2AClientError, A2AClientJSONError {
        A2ACardResolver resolver = new A2ACardResolver(httpClient, baseUrl, agentCardPath);
        AgentCard card = resolver.getAgentCard();
        return new A2AClient(card);
    }

    /**
     * Get the agent card for the A2A server this client will be communicating with from
     * the default public agent card endpoint.
     *
     * @return the agent card for the A2A server
     * @throws A2AClientError If an HTTP error occurs fetching the card
     * @throws A2AClientJSONError f the response body cannot be decoded as JSON or validated against the AgentCard schema
     */
    public AgentCard getAgentCard() throws A2AClientError, A2AClientJSONError {
        if (this.agentCard == null) {
            this.agentCard = A2A.getAgentCard(this.httpClient, this.agentUrl);
        }
        return this.agentCard;
    }

    /**
     * Get the agent card for the A2A server this client will be communicating with.
     *
     * @param relativeCardPath the path to the agent card endpoint relative to the base URL of the A2A server
     * @param authHeaders the HTTP authentication headers to use
     * @return the agent card for the A2A server
     * @throws A2AClientError If an HTTP error occurs fetching the card
     * @throws A2AClientJSONError f the response body cannot be decoded as JSON or validated against the AgentCard schema
     */
    public AgentCard getAgentCard(String relativeCardPath, Map<String, String> authHeaders) throws A2AClientError, A2AClientJSONError {
        if (this.agentCard == null) {
            this.agentCard = A2A.getAgentCard(this.httpClient, this.agentUrl, relativeCardPath, authHeaders);
        }
        return this.agentCard;
    }

    /**
     * Send a message to the remote agent.
     *
     * @param messageSendParams the parameters for the message to be sent
     * @return the response, may contain a message or a task
     * @throws A2AServerException if sending the message fails for any reason
     */
    public SendMessageResponse sendMessage(MessageSendParams messageSendParams) throws A2AServerException {
        return sendMessage(null, messageSendParams);
    }

    /**
     * Send a message to the remote agent.
     *
     * @param requestId the request ID to use
     * @param messageSendParams the parameters for the message to be sent
     * @return the response, may contain a message or a task
     * @throws A2AServerException if sending the message fails for any reason
     */
    public SendMessageResponse sendMessage(String requestId, MessageSendParams messageSendParams) throws A2AServerException {
        SendMessageRequest.Builder sendMessageRequestBuilder = new SendMessageRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(SendMessageRequest.METHOD)
                .params(messageSendParams);

        if (requestId != null) {
            sendMessageRequestBuilder.id(requestId);
        }

        SendMessageRequest sendMessageRequest = sendMessageRequestBuilder.build();

        try {
            String httpResponseBody = sendPostRequest(sendMessageRequest);
            return unmarshalResponse(httpResponseBody, SEND_MESSAGE_RESPONSE_REFERENCE);
        } catch (IOException | InterruptedException e) {
            throw new A2AServerException("Failed to send message: " + e, e.getCause());
        }
    }

    /**
     * Retrieve a task from the A2A server. This method can be used to retrieve the generated
     * artifacts for a task.
     *
     * @param id the task ID
     * @return the response containing the task
     * @throws A2AServerException if retrieving the task fails for any reason
     */
    public GetTaskResponse getTask(String id) throws A2AServerException {
        return getTask(null, new TaskQueryParams(id));
    }

    /**
     * Retrieve a task from the A2A server. This method can be used to retrieve the generated
     * artifacts for a task.
     *
     * @param taskQueryParams the params for the task to be queried
     * @return the response containing the task
     * @throws A2AServerException if retrieving the task fails for any reason
     */
    public GetTaskResponse getTask(TaskQueryParams taskQueryParams) throws A2AServerException {
        return getTask(null, taskQueryParams);
    }

    /**
     * Retrieve the generated artifacts for a task.
     *
     * @param requestId the request ID to use
     * @param taskQueryParams the params for the task to be queried
     * @return the response containing the task
     * @throws A2AServerException if retrieving the task fails for any reason
     */
    public GetTaskResponse getTask(String requestId, TaskQueryParams taskQueryParams) throws A2AServerException {
        GetTaskRequest.Builder getTaskRequestBuilder = new GetTaskRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(GetTaskRequest.METHOD)
                .params(taskQueryParams);

        if (requestId != null) {
            getTaskRequestBuilder.id(requestId);
        }

        GetTaskRequest getTaskRequest = getTaskRequestBuilder.build();

        try {
            String httpResponseBody = sendPostRequest(getTaskRequest);
            return unmarshalResponse(httpResponseBody, GET_TASK_RESPONSE_REFERENCE);
        } catch (IOException | InterruptedException e) {
            throw new A2AServerException("Failed to get task: " + e, e.getCause());
        }
    }

    /**
     * Cancel a task that was previously submitted to the A2A server.
     *
     * @param id the task ID
     * @return the response indicating if the task was cancelled
     * @throws A2AServerException if cancelling the task fails for any reason
     */
    public CancelTaskResponse cancelTask(String id) throws A2AServerException {
        return cancelTask(null, new TaskIdParams(id));
    }

    /**
     * Cancel a task that was previously submitted to the A2A server.
     *
     * @param taskIdParams the params for the task to be cancelled
     * @return the response indicating if the task was cancelled
     * @throws A2AServerException if cancelling the task fails for any reason
     */
    public CancelTaskResponse cancelTask(TaskIdParams taskIdParams) throws A2AServerException {
        return cancelTask(null, taskIdParams);
    }

    /**
     * Cancel a task that was previously submitted to the A2A server.
     *
     * @param requestId the request ID to use
     * @param taskIdParams the params for the task to be cancelled
     * @return the response indicating if the task was cancelled
     * @throws A2AServerException if retrieving the task fails for any reason
     */
    public CancelTaskResponse cancelTask(String requestId, TaskIdParams taskIdParams) throws A2AServerException {
        CancelTaskRequest.Builder cancelTaskRequestBuilder = new CancelTaskRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(CancelTaskRequest.METHOD)
                .params(taskIdParams);

        if (requestId != null) {
            cancelTaskRequestBuilder.id(requestId);
        }

        CancelTaskRequest cancelTaskRequest = cancelTaskRequestBuilder.build();

        try {
            String httpResponseBody = sendPostRequest(cancelTaskRequest);
            return unmarshalResponse(httpResponseBody, CANCEL_TASK_RESPONSE_REFERENCE);
        } catch (IOException | InterruptedException e) {
            throw new A2AServerException("Failed to cancel task: " + e, e.getCause());
        }
    }

    /**
     * Get the push notification configuration for a task.
     *
     * @param taskId the task ID
     * @return the response containing the push notification configuration
     * @throws A2AServerException if getting the push notification configuration fails for any reason
     */
    public GetTaskPushNotificationConfigResponse getTaskPushNotificationConfig(String taskId) throws A2AServerException {
        return getTaskPushNotificationConfig(null, new GetTaskPushNotificationConfigParams(taskId));
    }

    /**
     * Get the push notification configuration for a task.
     *
     * @param taskId the task ID
     * @param pushNotificationConfigId the push notification configuration ID
     * @return the response containing the push notification configuration
     * @throws A2AServerException if getting the push notification configuration fails for any reason
     */
    public GetTaskPushNotificationConfigResponse getTaskPushNotificationConfig(String taskId, String pushNotificationConfigId) throws A2AServerException {
        return getTaskPushNotificationConfig(null, new GetTaskPushNotificationConfigParams(taskId, pushNotificationConfigId));
    }

    /**
     * Get the push notification configuration for a task.
     *
     * @param getTaskPushNotificationConfigParams the params for the task
     * @return the response containing the push notification configuration
     * @throws A2AServerException if getting the push notification configuration fails for any reason
     */
    public GetTaskPushNotificationConfigResponse getTaskPushNotificationConfig(GetTaskPushNotificationConfigParams getTaskPushNotificationConfigParams) throws A2AServerException {
        return getTaskPushNotificationConfig(null, getTaskPushNotificationConfigParams);
    }

    /**
     * Get the push notification configuration for a task.
     *
     * @param requestId the request ID to use
     * @param getTaskPushNotificationConfigParams the params for the task
     * @return the response containing the push notification configuration
     * @throws A2AServerException if getting the push notification configuration fails for any reason
     */
    public GetTaskPushNotificationConfigResponse getTaskPushNotificationConfig(String requestId, GetTaskPushNotificationConfigParams getTaskPushNotificationConfigParams) throws A2AServerException {
        GetTaskPushNotificationConfigRequest.Builder getTaskPushNotificationRequestBuilder = new GetTaskPushNotificationConfigRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(GetTaskPushNotificationConfigRequest.METHOD)
                .params(getTaskPushNotificationConfigParams);

        if (requestId != null) {
            getTaskPushNotificationRequestBuilder.id(requestId);
        }

        GetTaskPushNotificationConfigRequest getTaskPushNotificationRequest = getTaskPushNotificationRequestBuilder.build();

        try {
            String httpResponseBody = sendPostRequest(getTaskPushNotificationRequest);
            return unmarshalResponse(httpResponseBody, GET_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE);
        } catch (IOException | InterruptedException e) {
            throw new A2AServerException("Failed to get task push notification config: " + e, e.getCause());
        }
    }

    /**
     * Set push notification configuration for a task.
     *
     * @param taskId the task ID
     * @param pushNotificationConfig the push notification configuration
     * @return the response indicating whether setting the task push notification configuration succeeded
     * @throws A2AServerException if setting the push notification configuration fails for any reason
     */
    public SetTaskPushNotificationConfigResponse setTaskPushNotificationConfig(String taskId,
                                                                               PushNotificationConfig pushNotificationConfig) throws A2AServerException {
        return setTaskPushNotificationConfig(null, taskId, pushNotificationConfig);
    }

    /**
     * Set push notification configuration for a task.
     *
     * @param requestId the request ID to use
     * @param taskId the task ID
     * @param pushNotificationConfig the push notification configuration
     * @return the response indicating whether setting the task push notification configuration succeeded
     * @throws A2AServerException if setting the push notification configuration fails for any reason
     */
    public SetTaskPushNotificationConfigResponse setTaskPushNotificationConfig(String requestId, String taskId,
                                                                               PushNotificationConfig pushNotificationConfig) throws A2AServerException {
        SetTaskPushNotificationConfigRequest.Builder setTaskPushNotificationRequestBuilder = new SetTaskPushNotificationConfigRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(SetTaskPushNotificationConfigRequest.METHOD)
                .params(new TaskPushNotificationConfig(taskId, pushNotificationConfig));

        if (requestId != null) {
            setTaskPushNotificationRequestBuilder.id(requestId);
        }

        SetTaskPushNotificationConfigRequest setTaskPushNotificationRequest = setTaskPushNotificationRequestBuilder.build();

        try {
            String httpResponseBody = sendPostRequest(setTaskPushNotificationRequest);
            return unmarshalResponse(httpResponseBody, SET_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE);
        } catch (IOException | InterruptedException e) {
            throw new A2AServerException("Failed to set task push notification config: " + e, e.getCause());
        }
    }

    /**
     * Retrieves the push notification configurations for a specified task.
     *
     * @param requestId the request ID to use
     * @param taskId the task ID to use
     * @return the response containing the push notification configuration
     * @throws A2AServerException if getting the push notification configuration fails for any reason
     */
    public ListTaskPushNotificationConfigResponse listTaskPushNotificationConfig(String requestId, String taskId) throws A2AServerException {
        return listTaskPushNotificationConfig(requestId, new ListTaskPushNotificationConfigParams(taskId));
    }

    /**
     * Retrieves the push notification configurations for a specified task.
     *
     * @param taskId the task ID to use
     * @return the response containing the push notification configuration
     * @throws A2AServerException if getting the push notification configuration fails for any reason
     */
    public ListTaskPushNotificationConfigResponse listTaskPushNotificationConfig(String taskId) throws A2AServerException {
        return listTaskPushNotificationConfig(null, new ListTaskPushNotificationConfigParams(taskId));
    }

    /**
     * Retrieves the push notification configurations for a specified task.
     *
     * @param listTaskPushNotificationConfigParams the params for retrieving the push notification configuration
     * @return the response containing the push notification configuration
     * @throws A2AServerException if getting the push notification configuration fails for any reason
     */
    public ListTaskPushNotificationConfigResponse listTaskPushNotificationConfig(ListTaskPushNotificationConfigParams listTaskPushNotificationConfigParams) throws A2AServerException {
        return listTaskPushNotificationConfig(null, listTaskPushNotificationConfigParams);
    }

    /**
     * Retrieves the push notification configurations for a specified task.
     *
     * @param requestId the request ID to use
     * @param listTaskPushNotificationConfigParams the params for retrieving the push notification configuration
     * @return the response containing the push notification configuration
     * @throws A2AServerException if getting the push notification configuration fails for any reason
     */
    public ListTaskPushNotificationConfigResponse listTaskPushNotificationConfig(String requestId,
                                                                                 ListTaskPushNotificationConfigParams listTaskPushNotificationConfigParams) throws A2AServerException {
        ListTaskPushNotificationConfigRequest.Builder listTaskPushNotificationRequestBuilder = new ListTaskPushNotificationConfigRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(ListTaskPushNotificationConfigRequest.METHOD)
                .params(listTaskPushNotificationConfigParams);

        if (requestId != null) {
            listTaskPushNotificationRequestBuilder.id(requestId);
        }

        ListTaskPushNotificationConfigRequest listTaskPushNotificationRequest = listTaskPushNotificationRequestBuilder.build();

        try {
            String httpResponseBody = sendPostRequest(listTaskPushNotificationRequest);
            return unmarshalResponse(httpResponseBody, LIST_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE);
        } catch (IOException | InterruptedException e) {
            throw new A2AServerException("Failed to list task push notification config: " + e, e.getCause());
        }
    }

    /**
     * Delete the push notification configuration for a specified task.
     *
     * @param requestId the request ID to use
     * @param taskId the task ID
     * @param pushNotificationConfigId the push notification config ID
     * @return the response
     * @throws A2AServerException if deleting the push notification configuration fails for any reason
     */
    public DeleteTaskPushNotificationConfigResponse deleteTaskPushNotificationConfig(String requestId, String taskId,
                                                                                     String pushNotificationConfigId) throws A2AServerException {
        return deleteTaskPushNotificationConfig(requestId, new DeleteTaskPushNotificationConfigParams(taskId, pushNotificationConfigId));
    }

    /**
     * Delete the push notification configuration for a specified task.
     *
     * @param taskId the task ID
     * @param pushNotificationConfigId the push notification config ID
     * @return the response
     * @throws A2AServerException if deleting the push notification configuration fails for any reason
     */
    public DeleteTaskPushNotificationConfigResponse deleteTaskPushNotificationConfig(String taskId,
                                                                                     String pushNotificationConfigId) throws A2AServerException {
        return deleteTaskPushNotificationConfig(null, new DeleteTaskPushNotificationConfigParams(taskId, pushNotificationConfigId));
    }

    /**
     * Delete the push notification configuration for a specified task.
     *
     * @param deleteTaskPushNotificationConfigParams the params for deleting the push notification configuration
     * @return the response
     * @throws A2AServerException if deleting the push notification configuration fails for any reason
     */
    public DeleteTaskPushNotificationConfigResponse deleteTaskPushNotificationConfig(DeleteTaskPushNotificationConfigParams deleteTaskPushNotificationConfigParams) throws A2AServerException {
        return deleteTaskPushNotificationConfig(null, deleteTaskPushNotificationConfigParams);
    }

    /**
     * Delete the push notification configuration for a specified task.
     *
     * @param requestId the request ID to use
     * @param deleteTaskPushNotificationConfigParams the params for deleting the push notification configuration
     * @return the response
     * @throws A2AServerException if deleting the push notification configuration fails for any reason
     */
    public DeleteTaskPushNotificationConfigResponse deleteTaskPushNotificationConfig(String requestId,
                                                 DeleteTaskPushNotificationConfigParams deleteTaskPushNotificationConfigParams) throws A2AServerException {
        DeleteTaskPushNotificationConfigRequest.Builder deleteTaskPushNotificationRequestBuilder = new DeleteTaskPushNotificationConfigRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(DeleteTaskPushNotificationConfigRequest.METHOD)
                .params(deleteTaskPushNotificationConfigParams);

        if (requestId != null) {
            deleteTaskPushNotificationRequestBuilder.id(requestId);
        }

        DeleteTaskPushNotificationConfigRequest deleteTaskPushNotificationRequest = deleteTaskPushNotificationRequestBuilder.build();

        try {
            String httpResponseBody = sendPostRequest(deleteTaskPushNotificationRequest);
            return unmarshalResponse(httpResponseBody, DELETE_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE);
        } catch (IOException | InterruptedException e) {
            throw new A2AServerException("Failed to delete task push notification config: " + e, e.getCause());
        }
    }

    /**
     * Send a streaming message to the remote agent.
     *
     * @param messageSendParams the parameters for the message to be sent
     * @param eventHandler a consumer that will be invoked for each event received from the remote agent
     * @param errorHandler a consumer that will be invoked if the remote agent returns an error
     * @param failureHandler a consumer that will be invoked if a failure occurs when processing events
     * @throws A2AServerException if sending the streaming message fails for any reason
     */
    public void sendStreamingMessage(MessageSendParams messageSendParams, Consumer<StreamingEventKind> eventHandler,
                                     Consumer<JSONRPCError> errorHandler, Runnable failureHandler) throws A2AServerException {
        sendStreamingMessage(null, messageSendParams, eventHandler, errorHandler, failureHandler);
    }

    /**
     * Send a streaming message to the remote agent.
     *
     * @param requestId the request ID to use
     * @param messageSendParams the parameters for the message to be sent
     * @param eventHandler a consumer that will be invoked for each event received from the remote agent
     * @param errorHandler a consumer that will be invoked if the remote agent returns an error
     * @param failureHandler a consumer that will be invoked if a failure occurs when processing events
     * @throws A2AServerException if sending the streaming message fails for any reason
     */
    public void sendStreamingMessage(String requestId, MessageSendParams messageSendParams, Consumer<StreamingEventKind> eventHandler,
                                       Consumer<JSONRPCError> errorHandler, Runnable failureHandler) throws A2AServerException {
        checkNotNullParam("messageSendParams", messageSendParams);
        checkNotNullParam("eventHandler", eventHandler);
        checkNotNullParam("errorHandler", errorHandler);
        checkNotNullParam("failureHandler", failureHandler);

        SendStreamingMessageRequest.Builder sendStreamingMessageRequestBuilder = new SendStreamingMessageRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(SendStreamingMessageRequest.METHOD)
                .params(messageSendParams);

        if (requestId != null) {
            sendStreamingMessageRequestBuilder.id(requestId);
        }

        AtomicReference<CompletableFuture<Void>> ref = new AtomicReference<>();
        SSEEventListener sseEventListener = new SSEEventListener(eventHandler, errorHandler, failureHandler);
        SendStreamingMessageRequest sendStreamingMessageRequest = sendStreamingMessageRequestBuilder.build();
        try {
            A2AHttpClient.PostBuilder builder = createPostBuilder(sendStreamingMessageRequest);
            ref.set(builder.postAsyncSSE(
                    msg -> sseEventListener.onMessage(msg, ref.get()),
                    throwable -> sseEventListener.onError(throwable, ref.get()),
                    () -> {
                        // We don't need to do anything special on completion
                    }));

        } catch (IOException e) {
            throw new A2AServerException("Failed to send streaming message request: " + e, e.getCause());
        } catch (InterruptedException e) {
            throw new A2AServerException("Send streaming message request timed out: " + e, e.getCause());
        }
    }

    /**
     * Resubscribe to an ongoing task.
     *
     * @param taskIdParams the params for the task to resubscribe to
     * @param eventHandler a consumer that will be invoked for each event received from the remote agent
     * @param errorHandler a consumer that will be invoked if the remote agent returns an error
     * @param failureHandler a consumer that will be invoked if a failure occurs when processing events
     * @throws A2AServerException if resubscribing to the task fails for any reason
     */
    public void resubscribeToTask(TaskIdParams taskIdParams, Consumer<StreamingEventKind> eventHandler,
                                  Consumer<JSONRPCError> errorHandler, Runnable failureHandler) throws A2AServerException {
        resubscribeToTask(null, taskIdParams, eventHandler, errorHandler, failureHandler);
    }

    /**
     * Resubscribe to an ongoing task.
     *
     * @param requestId the request ID to use
     * @param taskIdParams the params for the task to resubscribe to
     * @param eventHandler a consumer that will be invoked for each event received from the remote agent
     * @param errorHandler a consumer that will be invoked if the remote agent returns an error
     * @param failureHandler a consumer that will be invoked if a failure occurs when processing events
     * @throws A2AServerException if resubscribing to the task fails for any reason
     */
    public void resubscribeToTask(String requestId, TaskIdParams taskIdParams, Consumer<StreamingEventKind> eventHandler,
                                  Consumer<JSONRPCError> errorHandler, Runnable failureHandler) throws A2AServerException {
        checkNotNullParam("taskIdParams", taskIdParams);
        checkNotNullParam("eventHandler", eventHandler);
        checkNotNullParam("errorHandler", errorHandler);
        checkNotNullParam("failureHandler", failureHandler);

        TaskResubscriptionRequest.Builder taskResubscriptionRequestBuilder = new TaskResubscriptionRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(TaskResubscriptionRequest.METHOD)
                .params(taskIdParams);

        if (requestId != null) {
            taskResubscriptionRequestBuilder.id(requestId);
        }

        AtomicReference<CompletableFuture<Void>> ref = new AtomicReference<>();
        SSEEventListener sseEventListener = new SSEEventListener(eventHandler, errorHandler, failureHandler);
        TaskResubscriptionRequest taskResubscriptionRequest = taskResubscriptionRequestBuilder.build();
        try {
            A2AHttpClient.PostBuilder builder = createPostBuilder(taskResubscriptionRequest);
            ref.set(builder.postAsyncSSE(
                    msg -> sseEventListener.onMessage(msg, ref.get()),
                    throwable -> sseEventListener.onError(throwable, ref.get()),
                    () -> {
                        // We don't need to do anything special on completion
                    }));

        } catch (IOException e) {
            throw new A2AServerException("Failed to send task resubscription request: " + e, e.getCause());
        } catch (InterruptedException e) {
            throw new A2AServerException("Task resubscription request timed out: " + e, e.getCause());
        }
    }

    /**
     * Retrieve the authenticated extended agent card.
     *
     * @param authHeaders the HTTP authentication headers to use
     * @return the response
     * @throws A2AServerException if retrieving the authenticated extended agent card fails for any reason
     */
    public GetAuthenticatedExtendedCardResponse getAuthenticatedExtendedCard(Map<String, String> authHeaders) throws A2AServerException {
        return getAuthenticatedExtendedCard(null, authHeaders);
    }

    /**
     * Retrieve the authenticated extended agent card.
     *
     * @param requestId the request ID to use
     * @param authHeaders the HTTP authentication headers to use
     * @return the response
     * @throws A2AServerException if retrieving the authenticated extended agent card fails for any reason
     */
    public GetAuthenticatedExtendedCardResponse getAuthenticatedExtendedCard(String requestId,
                                                                             Map<String, String> authHeaders) throws A2AServerException {
        GetAuthenticatedExtendedCardRequest.Builder requestBuilder = new GetAuthenticatedExtendedCardRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(GetAuthenticatedExtendedCardRequest.METHOD);

        if (requestId != null) {
            requestBuilder.id(requestId);
        }

        GetAuthenticatedExtendedCardRequest request = requestBuilder.build();

        try {
            String httpResponseBody = sendPostRequest(request, authHeaders);
            return unmarshalResponse(httpResponseBody, GET_AUTHENTICATED_EXTENDED_CARD_RESPONSE_REFERENCE);
        } catch (IOException | InterruptedException e) {
            throw new A2AServerException("Failed to get authenticated extended agent card: " + e, e);
        }
    }

    private String sendPostRequest(Object value) throws IOException, InterruptedException {
        return sendPostRequest(value, null);
    }

    private String sendPostRequest(Object value, Map<String, String> authHeaders) throws IOException, InterruptedException {
        A2AHttpClient.PostBuilder builder = createPostBuilder(value, authHeaders);
        A2AHttpResponse response = builder.post();
        if (!response.success()) {
            throw new IOException("Request failed " + response.status());
        }
        return response.body();
    }

    private A2AHttpClient.PostBuilder createPostBuilder(Object value) throws JsonProcessingException {
        return createPostBuilder(value, null);
    }

    private A2AHttpClient.PostBuilder createPostBuilder(Object value, Map<String, String> authHeaders) throws JsonProcessingException {
        A2AHttpClient.PostBuilder builder =  httpClient.createPost()
                .url(agentUrl)
                .addHeader("Content-Type", "application/json")
                .body(Utils.OBJECT_MAPPER.writeValueAsString(value));
        if (authHeaders != null) {
            for (Map.Entry<String, String> entry : authHeaders.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        return builder;
    }

    private <T extends JSONRPCResponse> T unmarshalResponse(String response, TypeReference<T> typeReference)
            throws A2AServerException, JsonProcessingException {
        T value = Utils.unmarshalFrom(response, typeReference);
        JSONRPCError error = value.getError();
        if (error != null) {
            throw new A2AServerException(error.getMessage() + (error.getData() != null ? ": " + error.getData() : ""), error);
        }
        return value;
    }
}
