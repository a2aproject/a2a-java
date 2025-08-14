package io.a2a.client;

import io.a2a.A2A;
import io.a2a.spec.*;
import io.a2a.transport.jsonrpc.client.A2AHttpClient;
import io.a2a.transport.jsonrpc.client.JSONRPCTransport;
import io.a2a.transport.jsonrpc.client.JdkA2AHttpClient;
import io.a2a.transport.spi.client.Transport;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static io.a2a.util.Assert.checkNotNullParam;

/**
 * An A2A client.
 */
public class A2AClient {

    private Transport transport;
    private AgentCard agentCard;


    /**
     * Create a new A2AClient.
     *
     * @param agentCard the agent card for the A2A server this client will be communicating with
     */
    public A2AClient(AgentCard agentCard) {
        checkNotNullParam("agentCard", agentCard);
        this.agentCard = agentCard;
        this.transport = new JSONRPCTransport(agentCard.url(), new JdkA2AHttpClient());
    }

    /**
     * Create a new A2AClient.
     *
     * @param agentUrl the URL for the A2A server this client will be communicating with
     */
    public A2AClient(String agentUrl) {
        checkNotNullParam("agentUrl", agentUrl);
        this.transport = new JSONRPCTransport(agentUrl, new JdkA2AHttpClient());
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
    public EventKind sendMessage(MessageSendParams messageSendParams) throws A2AServerException {
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
    public EventKind sendMessage(String requestId, MessageSendParams messageSendParams) throws A2AServerException {
        return transport.sendMessage(requestId, messageSendParams);
    }

    /**
     * Retrieve a task from the A2A server. This method can be used to retrieve the generated
     * artifacts for a task.
     *
     * @param id the task ID
     * @return the response containing the task
     * @throws A2AServerException if retrieving the task fails for any reason
     */
    public Task getTask(String id) throws A2AServerException {
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
    public Task getTask(TaskQueryParams taskQueryParams) throws A2AServerException {
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
    public Task getTask(String requestId, TaskQueryParams taskQueryParams) throws A2AServerException {
        return transport.getTask(requestId, taskQueryParams);
    }

    /**
     * Cancel a task that was previously submitted to the A2A server.
     *
     * @param id the task ID
     * @return the response indicating if the task was cancelled
     * @throws A2AServerException if cancelling the task fails for any reason
     */
    public Task cancelTask(String id) throws A2AServerException {
        return cancelTask(null, new TaskIdParams(id));
    }

    /**
     * Cancel a task that was previously submitted to the A2A server.
     *
     * @param taskIdParams the params for the task to be cancelled
     * @return the response indicating if the task was cancelled
     * @throws A2AServerException if cancelling the task fails for any reason
     */
    public Task cancelTask(TaskIdParams taskIdParams) throws A2AServerException {
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
    public Task cancelTask(String requestId, TaskIdParams taskIdParams) throws A2AServerException {
        return transport.cancelTask(requestId, taskIdParams);
    }

    /**
     * Get the push notification configuration for a task.
     *
     * @param taskId the task ID
     * @return the response containing the push notification configuration
     * @throws A2AServerException if getting the push notification configuration fails for any reason
     */
    public TaskPushNotificationConfig getTaskPushNotificationConfig(String taskId) throws A2AServerException {
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
    public TaskPushNotificationConfig getTaskPushNotificationConfig(String taskId, String pushNotificationConfigId) throws A2AServerException {
        return getTaskPushNotificationConfig(null, new GetTaskPushNotificationConfigParams(taskId, pushNotificationConfigId));
    }

    /**
     * Get the push notification configuration for a task.
     *
     * @param getTaskPushNotificationConfigParams the params for the task
     * @return the response containing the push notification configuration
     * @throws A2AServerException if getting the push notification configuration fails for any reason
     */
    public TaskPushNotificationConfig getTaskPushNotificationConfig(GetTaskPushNotificationConfigParams getTaskPushNotificationConfigParams) throws A2AServerException {
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
    public TaskPushNotificationConfig getTaskPushNotificationConfig(String requestId, GetTaskPushNotificationConfigParams getTaskPushNotificationConfigParams) throws A2AServerException {
        return transport.getTaskPushNotificationConfig(requestId, getTaskPushNotificationConfigParams);
    }

    /**
     * Set push notification configuration for a task.
     *
     * @param taskId the task ID
     * @param pushNotificationConfig the push notification configuration
     * @return the response indicating whether setting the task push notification configuration succeeded
     * @throws A2AServerException if setting the push notification configuration fails for any reason
     */
    public TaskPushNotificationConfig setTaskPushNotificationConfig(String taskId,
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
    public TaskPushNotificationConfig setTaskPushNotificationConfig(String requestId, String taskId,
                                                                               PushNotificationConfig pushNotificationConfig) throws A2AServerException {
        return transport.setTaskPushNotificationConfig(requestId, taskId, pushNotificationConfig);
    }

    /**
     * Retrieves the push notification configurations for a specified task.
     *
     * @param requestId the request ID to use
     * @param taskId the task ID to use
     * @return the response containing the push notification configuration
     * @throws A2AServerException if getting the push notification configuration fails for any reason
     */
    public List<TaskPushNotificationConfig> listTaskPushNotificationConfig(String requestId, String taskId) throws A2AServerException {
        return listTaskPushNotificationConfig(requestId, new ListTaskPushNotificationConfigParams(taskId));
    }

    /**
     * Retrieves the push notification configurations for a specified task.
     *
     * @param taskId the task ID to use
     * @return the response containing the push notification configuration
     * @throws A2AServerException if getting the push notification configuration fails for any reason
     */
    public List<TaskPushNotificationConfig> listTaskPushNotificationConfig(String taskId) throws A2AServerException {
        return listTaskPushNotificationConfig(null, new ListTaskPushNotificationConfigParams(taskId));
    }

    /**
     * Retrieves the push notification configurations for a specified task.
     *
     * @param listTaskPushNotificationConfigParams the params for retrieving the push notification configuration
     * @return the response containing the push notification configuration
     * @throws A2AServerException if getting the push notification configuration fails for any reason
     */
    public List<TaskPushNotificationConfig> listTaskPushNotificationConfig(ListTaskPushNotificationConfigParams listTaskPushNotificationConfigParams) throws A2AServerException {
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
    public List<TaskPushNotificationConfig> listTaskPushNotificationConfig(String requestId,
                                                                                 ListTaskPushNotificationConfigParams listTaskPushNotificationConfigParams) throws A2AServerException {
        return transport.listTaskPushNotificationConfig(requestId, listTaskPushNotificationConfigParams);
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
    public void deleteTaskPushNotificationConfig(String requestId, String taskId,
                                                                                     String pushNotificationConfigId) throws A2AServerException {
        deleteTaskPushNotificationConfig(requestId, new DeleteTaskPushNotificationConfigParams(taskId, pushNotificationConfigId));
    }

    /**
     * Delete the push notification configuration for a specified task.
     *
     * @param taskId the task ID
     * @param pushNotificationConfigId the push notification config ID
     * @return the response
     * @throws A2AServerException if deleting the push notification configuration fails for any reason
     */
    public void deleteTaskPushNotificationConfig(String taskId,
                                                                                     String pushNotificationConfigId) throws A2AServerException {
        deleteTaskPushNotificationConfig(null, new DeleteTaskPushNotificationConfigParams(taskId, pushNotificationConfigId));
    }

    /**
     * Delete the push notification configuration for a specified task.
     *
     * @param deleteTaskPushNotificationConfigParams the params for deleting the push notification configuration
     * @return the response
     * @throws A2AServerException if deleting the push notification configuration fails for any reason
     */
    public void deleteTaskPushNotificationConfig(DeleteTaskPushNotificationConfigParams deleteTaskPushNotificationConfigParams) throws A2AServerException {
        deleteTaskPushNotificationConfig(null, deleteTaskPushNotificationConfigParams);
    }

    /**
     * Delete the push notification configuration for a specified task.
     *
     * @param requestId the request ID to use
     * @param deleteTaskPushNotificationConfigParams the params for deleting the push notification configuration
     * @return the response
     * @throws A2AServerException if deleting the push notification configuration fails for any reason
     */
    public void deleteTaskPushNotificationConfig(String requestId,
                                                 DeleteTaskPushNotificationConfigParams deleteTaskPushNotificationConfigParams) throws A2AServerException {
        transport.deleteTaskPushNotificationConfig(requestId, deleteTaskPushNotificationConfigParams);
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

        transport.sendStreamingMessage(requestId, messageSendParams, eventHandler, errorHandler, failureHandler);
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

        transport.resubscribeToTask(requestId, taskIdParams, eventHandler, errorHandler, failureHandler);
    }
}
