package org.a2aproject.sdk.client.transport.spi;

import java.util.function.Consumer;

import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallContext;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.AgentCard;
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
import org.jspecify.annotations.Nullable;

/**
 * Interface for a client transport.
 */
public interface ClientTransport {

    /**
     * Send a non-streaming message request to the agent.
     *
     * @param request the message send parameters
     * @param context optional client call context for the request (may be {@code null})
     * @return the response, either a Task or Message
     * @throws A2AClientException if sending the message fails for any reason
     */
    EventKind sendMessage(MessageSendParams request, @Nullable ClientCallContext context)
            throws A2AClientException;

    /**
     * Send a streaming message request to the agent and receive responses as they arrive.
     *
     * @param request       the message send parameters
     * @param eventConsumer consumer that will receive streaming events as they arrive
     * @param errorConsumer consumer that will be called if an error occurs during streaming
     * @param context       optional client call context for the request (may be {@code null})
     * @throws A2AClientException if setting up the streaming connection fails
     */
    void sendMessageStreaming(MessageSendParams request, Consumer<StreamingEventKind> eventConsumer,
                              Consumer<Throwable> errorConsumer, @Nullable ClientCallContext context) throws A2AClientException;

    /**
     * Retrieve the current state and history of a specific task.
     *
     * @param request the task query parameters specifying which task to retrieve
     * @param context optional client call context for the request (may be {@code null})
     * @return the task
     * @throws A2AClientException if retrieving the task fails for any reason
     */
    Task getTask(TaskQueryParams request, @Nullable ClientCallContext context) throws A2AClientException;

    /**
     * Request the agent to cancel a specific task.
     *
     * @param request the task ID parameters specifying which task to cancel
     * @param context optional client call context for the request (may be {@code null})
     * @return the cancelled task
     * @throws A2AClientException if cancelling the task fails for any reason
     */
    Task cancelTask(CancelTaskParams request, @Nullable ClientCallContext context) throws A2AClientException;

    /**
     * List tasks with optional filtering and pagination.
     *
     * @param request the list tasks parameters including filters and pagination
     * @param context optional client call context for the request (may be {@code null})
     * @return the list tasks result containing tasks and pagination information
     * @throws A2AClientException if listing tasks fails for any reason
     */
    ListTasksResult listTasks(ListTasksParams request, @Nullable ClientCallContext context) throws A2AClientException;

    /**
     * Create or update the push notification configuration for a specific task.
     *
     * @param request the push notification configuration to set for the task
     * @param context optional client call context for the request (may be {@code null})
     * @return the configured TaskPushNotificationConfig
     * @throws A2AClientException if creating or updating the task push notification configuration fails for any reason
     */
    TaskPushNotificationConfig createTaskPushNotificationConfiguration(TaskPushNotificationConfig request,
                                                                       @Nullable ClientCallContext context) throws A2AClientException;

    /**
     * Retrieve the push notification configuration for a specific task.
     *
     * @param request the parameters specifying which task's notification config to retrieve
     * @param context optional client call context for the request (may be {@code null})
     * @return the task push notification config
     * @throws A2AClientException if getting the task push notification config fails for any reason
     */
    TaskPushNotificationConfig getTaskPushNotificationConfiguration(
            GetTaskPushNotificationConfigParams request,
            @Nullable ClientCallContext context) throws A2AClientException;

    /**
     * Retrieve the list of push notification configurations for a specific task with pagination support.
     *
     * @param request the parameters specifying which task's notification configs to retrieve
     * @param context optional client call context for the request (may be {@code null})
     * @return the result containing the list of task push notification configs and pagination information
     * @throws A2AClientException if getting the task push notification configs fails for any reason
     */
    ListTaskPushNotificationConfigsResult listTaskPushNotificationConfigurations(
            ListTaskPushNotificationConfigsParams request,
            @Nullable ClientCallContext context) throws A2AClientException;

    /**
     * Delete the list of push notification configurations for a specific task.
     *
     * @param request the parameters specifying which task's notification configs to delete
     * @param context optional client call context for the request (may be {@code null})
     * @throws A2AClientException if deleting the task push notification configs fails for any reason
     */
    void deleteTaskPushNotificationConfigurations(
            DeleteTaskPushNotificationConfigParams request,
            @Nullable ClientCallContext context) throws A2AClientException;

    /**
     * Reconnect to get task updates for an existing task.
     *
     * @param request       the task ID parameters specifying which task to subscribe to
     * @param eventConsumer consumer that will receive streaming events as they arrive
     * @param errorConsumer consumer that will be called if an error occurs during streaming
     * @param context       optional client call context for the request (may be {@code null})
     * @throws A2AClientException if subscribing to the task fails for any reason
     */
    void subscribeToTask(TaskIdParams request, Consumer<StreamingEventKind> eventConsumer,
                     Consumer<Throwable> errorConsumer, @Nullable ClientCallContext context) throws A2AClientException;

    /**
     * Retrieve the extended AgentCard.
     *
     * @param params the parameters to get the extended agent card.
     * @param context optional client call context for the request (may be {@code null})
     * @return the extended agent card
     * @throws A2AClientException if retrieving the agent card fails for any reason
     */
    AgentCard getExtendedAgentCard(GetExtendedAgentCardParams params, @Nullable ClientCallContext context) throws A2AClientException;

    /**
     * Close the transport and release any associated resources.
     */
    void close();

}
