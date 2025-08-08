package io.a2a.client;

import static io.a2a.util.Assert.checkNotNullParam;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.a2a.client.config.ClientCallContext;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.Message;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;

/**
 * Abstract class representing an A2A client. Provides a standard set
 * of methods for interacting with an A2A agent, regardless of the underlying
 * transport protocol. It supports sending messages, managing tasks, and
 * handling event streams.
 */
public abstract class AbstractClient {

    private final List<BiConsumer<ClientEvent, AgentCard>> consumers;
    private final Consumer<Throwable> streamingErrorHandler;

    public AbstractClient(List<BiConsumer<ClientEvent, AgentCard>> consumers) {
        this(consumers, null);
    }

    public AbstractClient(List<BiConsumer<ClientEvent, AgentCard>> consumers, Consumer<Throwable> streamingErrorHandler) {
        checkNotNullParam("consumers", consumers);
        this.consumers = consumers;
        this.streamingErrorHandler = streamingErrorHandler;
    }

    /**
     * Send a message to the remote agent. This method will automatically use
     * the streaming or non-streaming approach as determined by the server's
     * agent card and the client configuration. The configured client consumers
     * and will be used to handle messages, tasks, and update events received
     * from the remote agent. The configured streaming error handler will be used
     * if an error occurs during streaming. The configured client push notification
     * configuration will get used for streaming.
     *
     * @param request the message
     * @param context optional client call context for the request (may be {@code null})
     * @throws A2AClientException if sending the message fails for any reason
     */
    public abstract void sendMessage(Message request, ClientCallContext context) throws A2AClientException;

    /**
     * Send a message to the remote agent. This method will automatically use
     * the streaming or non-streaming approach as determined by the server's
     * agent card and the client configuration. The configured client consumers
     * will be used to handle messages, tasks, and update events received from
     * the remote agent. The configured streaming error handler will be used
     * if an error occurs during streaming.
     *
     * @param request the message
     * @param pushNotificationConfiguration the push notification configuration that should be
     *                                      used if the streaming approach is used
     * @param metadata the optional metadata to include when sending the message
     * @throws A2AClientException if sending the message fails for any reason
     */
    public abstract void sendMessage(Message request, PushNotificationConfig pushNotificationConfiguration,
                                     Map<String, Object> metadata, ClientCallContext context) throws A2AClientException;

    /**
     * Retrieve the current state and history of a specific task.
     *
     * @param request the task query parameters specifying which task to retrieve
     * @param context optional client call context for the request (may be {@code null})
     * @return the task
     * @throws A2AClientException if retrieving the task fails for any reason
     */
    public abstract Task getTask(TaskQueryParams request, ClientCallContext context) throws A2AClientException;

    /**
     * Request the agent to cancel a specific task.
     *
     * @param request the task ID parameters specifying which task to cancel
     * @param context optional client call context for the request (may be {@code null})
     * @return the cancelled task
     * @throws A2AClientException if cancelling the task fails for any reason
     */
    public abstract Task cancelTask(TaskIdParams request, ClientCallContext context) throws A2AClientException;

    /**
     * Set or update the push notification configuration for a specific task.
     *
     * @param request the push notification configuration to set for the task
     * @param context optional client call context for the request (may be {@code null})
     * @return the configured TaskPushNotificationConfig
     * @throws A2AClientException if setting the task push notification configuration fails for any reason
     */
    public abstract TaskPushNotificationConfig setTaskPushNotificationConfiguration(
            TaskPushNotificationConfig request,
            ClientCallContext context) throws A2AClientException;

    /**
     * Retrieve the push notification configuration for a specific task.
     *
     * @param request the parameters specifying which task's notification config to retrieve
     * @param context optional client call context for the request (may be {@code null})
     * @return the task push notification config
     * @throws A2AClientException if getting the task push notification config fails for any reason
     */
    public abstract TaskPushNotificationConfig getTaskPushNotificationConfiguration(
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
    public abstract List<TaskPushNotificationConfig> listTaskPushNotificationConfigurations(
            ListTaskPushNotificationConfigParams request,
            ClientCallContext context) throws A2AClientException;

    /**
     * Delete the list of push notification configurations for a specific task.
     *
     * @param request the parameters specifying which task's notification configs to delete
     * @param context optional client call context for the request (may be {@code null})
     * @throws A2AClientException if deleting the task push notification configs fails for any reason
     */
    public abstract void deleteTaskPushNotificationConfigurations(
            DeleteTaskPushNotificationConfigParams request,
            ClientCallContext context) throws A2AClientException;

    /**
     * Resubscribe to a task's event stream.
     * This is only available if both the client and server support streaming.
     *
     * @param request the parameters specifying which task's notification configs to delete
     * @param context optional client call context for the request (may be {@code null})
     * @throws A2AClientException if resubscribing fails for any reason
     */
    public abstract void resubscribe(TaskIdParams request, ClientCallContext context) throws A2AClientException;

    /**
     * Retrieve the AgentCard.
     *
     * @param context optional client call context for the request (may be {@code null})
     * @return the AgentCard
     * @throws A2AClientException if retrieving the agent card fails for any reason
     */
    public abstract AgentCard getAgentCard(ClientCallContext context) throws A2AClientException;

    /**
     * Close the transport and release any associated resources.
     */
    public abstract void close();

    /**
     * Process the event using all configured consumers.
     */
    public void consume(ClientEvent clientEventOrMessage, AgentCard agentCard) {
        for (BiConsumer<ClientEvent, AgentCard> consumer : consumers) {
            consumer.accept(clientEventOrMessage, agentCard);
        }
    }

    /**
     * Get the error handler that should be used during streaming.
     *
     * @return the streaming error handler
     */
    public Consumer<Throwable> getStreamingErrorHandler() {
        return streamingErrorHandler;
    }

}
