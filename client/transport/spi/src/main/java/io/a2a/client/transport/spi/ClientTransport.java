package io.a2a.client.transport.spi;

import java.util.List;
import java.util.function.Consumer;

import io.a2a.client.transport.spi.interceptors.ClientCallContext;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.EventKind;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;

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
    EventKind sendMessage(MessageSendParams request, ClientCallContext context)
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
                              Consumer<Throwable> errorConsumer, ClientCallContext context) throws A2AClientException;

    /**
     * Retrieve the current state and history of a specific task.
     *
     * @param request the task query parameters specifying which task to retrieve
     * @param context optional client call context for the request (may be {@code null})
     * @return the task
     * @throws A2AClientException if retrieving the task fails for any reason
     */
    Task getTask(TaskQueryParams request, ClientCallContext context) throws A2AClientException;

    /**
     * Request the agent to cancel a specific task.
     *
     * @param request the task ID parameters specifying which task to cancel
     * @param context optional client call context for the request (may be {@code null})
     * @return the cancelled task
     * @throws A2AClientException if cancelling the task fails for any reason
     */
    Task cancelTask(TaskIdParams request, ClientCallContext context) throws A2AClientException;

    /**
     * Set or update the push notification configuration for a specific task.
     *
     * @param request the push notification configuration to set for the task
     * @param context optional client call context for the request (may be {@code null})
     * @return the configured TaskPushNotificationConfig
     * @throws A2AClientException if setting the task push notification configuration fails for any reason
     */
    TaskPushNotificationConfig setTaskPushNotificationConfiguration(TaskPushNotificationConfig request,
                                                                    ClientCallContext context) throws A2AClientException;

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
            ClientCallContext context) throws A2AClientException;

    /**
     * Retrieve the list of push notification configurations for a specific task.
     *
     * @param request the parameters specifying which task's notification configs to retrieve
     * @param context optional client call context for the request (may be {@code null})
     * @return the list of task push notification configs
     * @throws A2AClientException if getting the task push notification configs fails for any reason
     */
    List<TaskPushNotificationConfig> listTaskPushNotificationConfigurations(
            ListTaskPushNotificationConfigParams request,
            ClientCallContext context) throws A2AClientException;

    /**
     * Delete the list of push notification configurations for a specific task.
     *
     * @param request the parameters specifying which task's notification configs to delete
     * @param context optional client call context for the request (may be {@code null})
     * @throws A2AClientException if deleting the task push notification configs fails for any reason
     */
    void deleteTaskPushNotificationConfigurations(
            DeleteTaskPushNotificationConfigParams request,
            ClientCallContext context) throws A2AClientException;

    /**
     * Reconnect to get task updates for an existing task.
     *
     * @param request       the task ID parameters specifying which task to resubscribe to
     * @param eventConsumer consumer that will receive streaming events as they arrive
     * @param errorConsumer consumer that will be called if an error occurs during streaming
     * @param context       optional client call context for the request (may be {@code null})
     * @throws A2AClientException if resubscribing to the task fails for any reason
     */
    void resubscribe(TaskIdParams request, Consumer<StreamingEventKind> eventConsumer,
                     Consumer<Throwable> errorConsumer, ClientCallContext context) throws A2AClientException;

    /**
     * Retrieve the AgentCard.
     *
     * @param context optional client call context for the request (may be {@code null})
     * @return the AgentCard
     * @throws A2AClientException if retrieving the agent card fails for any reason
     */
    AgentCard getAgentCard(ClientCallContext context) throws A2AClientException;

    /**
     * Close the transport and release any associated resources.
     */
    void close();

}
