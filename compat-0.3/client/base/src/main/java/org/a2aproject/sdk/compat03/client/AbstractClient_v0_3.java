package org.a2aproject.sdk.compat03.client;

import static org.a2aproject.sdk.util.Assert.checkNotNullParam;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallContext_v0_3;
import org.a2aproject.sdk.compat03.spec.A2AClientException_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.ListTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.compat03.spec.PushNotificationConfig_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskIdParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskPushNotificationConfig_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskQueryParams_v0_3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Abstract class representing an A2A client. Provides a standard set
 * of methods for interacting with an A2A agent, regardless of the underlying
 * transport protocol. It supports sending messages, managing tasks, and
 * handling event streams.
 */
public abstract class AbstractClient_v0_3 {

    private final List<BiConsumer<ClientEvent_v0_3, AgentCard_v0_3>> consumers;
    private final @Nullable Consumer<Throwable> streamingErrorHandler;

    public AbstractClient_v0_3(List<BiConsumer<ClientEvent_v0_3, AgentCard_v0_3>> consumers) {
        this(consumers, null);
    }

    public AbstractClient_v0_3(@NonNull List<BiConsumer<ClientEvent_v0_3, AgentCard_v0_3>> consumers, @Nullable Consumer<Throwable> streamingErrorHandler) {
        checkNotNullParam("consumers", consumers);
        this.consumers = consumers;
        this.streamingErrorHandler = streamingErrorHandler;
    }

    /**
     * Send a message to the remote agent. This method will automatically use
     * the streaming or non-streaming approach as determined by the server's
     * agent card and the client configuration. The configured client consumers
     * will be used to handle messages, tasks, and update events received
     * from the remote agent. The configured streaming error handler will be used
     * if an error occurs during streaming. The configured client push notification
     * configuration will get used for streaming.
     *
     * @param request the message
     * @throws A2AClientException_v0_3 if sending the message fails for any reason
     */
    public void sendMessage(Message_v0_3 request) throws A2AClientException_v0_3 {
        sendMessage(request, null);
    }

    /**
     * Send a message to the remote agent. This method will automatically use
     * the streaming or non-streaming approach as determined by the server's
     * agent card and the client configuration. The configured client consumers
     * will be used to handle messages, tasks, and update events received
     * from the remote agent. The configured streaming error handler will be used
     * if an error occurs during streaming. The configured client push notification
     * configuration will get used for streaming.
     *
     * @param request the message
     * @param context optional client call context for the request (may be {@code null})
     * @throws A2AClientException_v0_3 if sending the message fails for any reason
     */
    public abstract void sendMessage(Message_v0_3 request, @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3;

    /**
     * Send a message to the remote agent. This method will automatically use
     * the streaming or non-streaming approach as determined by the server's
     * agent card and the client configuration. The specified client consumers
     * will be used to handle messages, tasks, and update events received
     * from the remote agent. The specified streaming error handler will be used
     * if an error occurs during streaming. The configured client push notification
     * configuration will get used for streaming.
     *
     * @param request the message
     * @param consumers a list of consumers to pass responses from the remote agent to
     * @param streamingErrorHandler an error handler that should be used for the streaming case if an error occurs
     * @throws A2AClientException_v0_3 if sending the message fails for any reason
     */
    public void sendMessage(Message_v0_3 request,
                            List<BiConsumer<ClientEvent_v0_3, AgentCard_v0_3>> consumers,
                            Consumer<Throwable> streamingErrorHandler) throws A2AClientException_v0_3 {
        sendMessage(request, consumers, streamingErrorHandler, null);
    }

    /**
     * Send a message to the remote agent. This method will automatically use
     * the streaming or non-streaming approach as determined by the server's
     * agent card and the client configuration. The specified client consumers
     * will be used to handle messages, tasks, and update events received
     * from the remote agent. The specified streaming error handler will be used
     * if an error occurs during streaming. The configured client push notification
     * configuration will get used for streaming.
     *
     * @param request the message
     * @param consumers a list of consumers to pass responses from the remote agent to
     * @param streamingErrorHandler an error handler that should be used for the streaming case if an error occurs
     * @param context optional client call context for the request (may be {@code null})
     * @throws A2AClientException_v0_3 if sending the message fails for any reason
     */
    public abstract void sendMessage(Message_v0_3 request,
                                     List<BiConsumer<ClientEvent_v0_3, AgentCard_v0_3>> consumers,
                                     Consumer<Throwable> streamingErrorHandler,
                                     @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3;

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
     * @throws A2AClientException_v0_3 if sending the message fails for any reason
     */
    public void sendMessage(Message_v0_3 request, PushNotificationConfig_v0_3 pushNotificationConfiguration,
                            Map<String, Object> metadata) throws A2AClientException_v0_3 {
        sendMessage(request, pushNotificationConfiguration, metadata, null);
    }

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
     * @param context optional client call context for the request (may be {@code null})
     * @throws A2AClientException_v0_3 if sending the message fails for any reason
     */
    public abstract void sendMessage(Message_v0_3 request, PushNotificationConfig_v0_3 pushNotificationConfiguration,
                                     Map<String, Object> metadata, @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3;

    /**
     * Retrieve the current state and history of a specific task.
     *
     * @param request the task query parameters specifying which task to retrieve
     * @return the task
     * @throws A2AClientException_v0_3 if retrieving the task fails for any reason
     */
    public Task_v0_3 getTask(TaskQueryParams_v0_3 request) throws A2AClientException_v0_3 {
        return getTask(request, null);
    }

    /**
     * Retrieve the current state and history of a specific task.
     *
     * @param request the task query parameters specifying which task to retrieve
     * @param context optional client call context for the request (may be {@code null})
     * @return the task
     * @throws A2AClientException_v0_3 if retrieving the task fails for any reason
     */
    public abstract Task_v0_3 getTask(TaskQueryParams_v0_3 request, @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3;

    /**
     * Request the agent to cancel a specific task.
     *
     * @param request the task ID parameters specifying which task to cancel
     * @return the cancelled task
     * @throws A2AClientException_v0_3 if cancelling the task fails for any reason
     */
    public Task_v0_3 cancelTask(TaskIdParams_v0_3 request) throws A2AClientException_v0_3 {
        return cancelTask(request, null);
    }

    /**
     * Request the agent to cancel a specific task.
     *
     * @param request the task ID parameters specifying which task to cancel
     * @param context optional client call context for the request (may be {@code null})
     * @return the cancelled task
     * @throws A2AClientException_v0_3 if cancelling the task fails for any reason
     */
    public abstract Task_v0_3 cancelTask(TaskIdParams_v0_3 request, @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3;

    /**
     * Set or update the push notification configuration for a specific task.
     *
     * @param request the push notification configuration to set for the task
     * @return the configured TaskPushNotificationConfig
     * @throws A2AClientException_v0_3 if setting the task push notification configuration fails for any reason
     */
    public TaskPushNotificationConfig_v0_3 setTaskPushNotificationConfiguration(
            TaskPushNotificationConfig_v0_3 request) throws A2AClientException_v0_3 {
        return setTaskPushNotificationConfiguration(request, null);
    }

    /**
     * Set or update the push notification configuration for a specific task.
     *
     * @param request the push notification configuration to set for the task
     * @param context optional client call context for the request (may be {@code null})
     * @return the configured TaskPushNotificationConfig
     * @throws A2AClientException_v0_3 if setting the task push notification configuration fails for any reason
     */
    public abstract TaskPushNotificationConfig_v0_3 setTaskPushNotificationConfiguration(
            TaskPushNotificationConfig_v0_3 request,
            @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3;

    /**
     * Retrieve the push notification configuration for a specific task.
     *
     * @param request the parameters specifying which task's notification config to retrieve
     * @return the task push notification config
     * @throws A2AClientException_v0_3 if getting the task push notification config fails for any reason
     */
    public TaskPushNotificationConfig_v0_3 getTaskPushNotificationConfiguration(
            GetTaskPushNotificationConfigParams_v0_3 request) throws A2AClientException_v0_3 {
        return getTaskPushNotificationConfiguration(request, null);
    }

    /**
     * Retrieve the push notification configuration for a specific task.
     *
     * @param request the parameters specifying which task's notification config to retrieve
     * @param context optional client call context for the request (may be {@code null})
     * @return the task push notification config
     * @throws A2AClientException_v0_3 if getting the task push notification config fails for any reason
     */
    public abstract TaskPushNotificationConfig_v0_3 getTaskPushNotificationConfiguration(
            GetTaskPushNotificationConfigParams_v0_3 request,
            @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3;

    /**
     * Retrieve the list of push notification configurations for a specific task.
     *
     * @param request the parameters specifying which task's notification configs to retrieve
     * @return the list of task push notification configs
     * @throws A2AClientException_v0_3 if getting the task push notification configs fails for any reason
     */
    public List<TaskPushNotificationConfig_v0_3> listTaskPushNotificationConfigurations(
            ListTaskPushNotificationConfigParams_v0_3 request) throws A2AClientException_v0_3 {
        return listTaskPushNotificationConfigurations(request, null);
    }

    /**
     * Retrieve the list of push notification configurations for a specific task.
     *
     * @param request the parameters specifying which task's notification configs to retrieve
     * @param context optional client call context for the request (may be {@code null})
     * @return the list of task push notification configs
     * @throws A2AClientException_v0_3 if getting the task push notification configs fails for any reason
     */
    public abstract List<TaskPushNotificationConfig_v0_3> listTaskPushNotificationConfigurations(
            ListTaskPushNotificationConfigParams_v0_3 request,
            @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3;

    /**
     * Delete the list of push notification configurations for a specific task.
     *
     * @param request the parameters specifying which task's notification configs to delete
     * @throws A2AClientException_v0_3 if deleting the task push notification configs fails for any reason
     */
    public void deleteTaskPushNotificationConfigurations(
            DeleteTaskPushNotificationConfigParams_v0_3 request) throws A2AClientException_v0_3 {
        deleteTaskPushNotificationConfigurations(request, null);
    }

    /**
     * Delete the list of push notification configurations for a specific task.
     *
     * @param request the parameters specifying which task's notification configs to delete
     * @param context optional client call context for the request (may be {@code null})
     * @throws A2AClientException_v0_3 if deleting the task push notification configs fails for any reason
     */
    public abstract void deleteTaskPushNotificationConfigurations(
            DeleteTaskPushNotificationConfigParams_v0_3 request,
            @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3;

    /**
     * Resubscribe to a task's event stream.
     * This is only available if both the client and server support streaming.
     * The configured client consumers will be used to handle messages, tasks,
     * and update events received from the remote agent. The configured streaming
     * error handler will be used if an error occurs during streaming.
     *
     * @param request the parameters specifying which task's notification configs to delete
     * @throws A2AClientException_v0_3 if resubscribing fails for any reason
     */
    public void resubscribe(TaskIdParams_v0_3 request) throws A2AClientException_v0_3 {
        resubscribe(request, null);
    }

    /**
     * Resubscribe to a task's event stream.
     * This is only available if both the client and server support streaming.
     * The configured client consumers will be used to handle messages, tasks,
     * and update events received from the remote agent. The configured streaming
     * error handler will be used if an error occurs during streaming.
     *
     * @param request the parameters specifying which task's notification configs to delete
     * @param context optional client call context for the request (may be {@code null})
     * @throws A2AClientException_v0_3 if resubscribing fails for any reason
     */
    public abstract void resubscribe(TaskIdParams_v0_3 request, @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3;

    /**
     * Resubscribe to a task's event stream.
     * This is only available if both the client and server support streaming.
     * The specified client consumers will be used to handle messages, tasks, and
     * update events received from the remote agent. The specified streaming error
     * handler will be used if an error occurs during streaming.
     *
     * @param request the parameters specifying which task's notification configs to delete
     * @param consumers a list of consumers to pass responses from the remote agent to
     * @param streamingErrorHandler an error handler that should be used for the streaming case if an error occurs
     * @throws A2AClientException_v0_3 if resubscribing fails for any reason
     */
    public void resubscribe(TaskIdParams_v0_3 request, List<BiConsumer<ClientEvent_v0_3, AgentCard_v0_3>> consumers,
                            Consumer<Throwable> streamingErrorHandler) throws A2AClientException_v0_3 {
        resubscribe(request, consumers, streamingErrorHandler, null);
    }

    /**
     * Resubscribe to a task's event stream.
     * This is only available if both the client and server support streaming.
     * The specified client consumers will be used to handle messages, tasks, and
     * update events received from the remote agent. The specified streaming error
     * handler will be used if an error occurs during streaming.
     *
     * @param request the parameters specifying which task's notification configs to delete
     * @param consumers a list of consumers to pass responses from the remote agent to
     * @param streamingErrorHandler an error handler that should be used for the streaming case if an error occurs
     * @param context optional client call context for the request (may be {@code null})
     * @throws A2AClientException_v0_3 if resubscribing fails for any reason
     */
    public abstract void resubscribe(TaskIdParams_v0_3 request, List<BiConsumer<ClientEvent_v0_3, AgentCard_v0_3>> consumers,
                                     Consumer<Throwable> streamingErrorHandler, @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3;

    /**
     * Retrieve the AgentCard.
     *
     * @return the AgentCard
     * @throws A2AClientException_v0_3 if retrieving the agent card fails for any reason
     */
    public AgentCard_v0_3 getAgentCard() throws A2AClientException_v0_3 {
        return getAgentCard(null);
    }

    /**
     * Retrieve the AgentCard.
     *
     * @param context optional client call context for the request (may be {@code null})
     * @return the AgentCard
     * @throws A2AClientException_v0_3 if retrieving the agent card fails for any reason
     */
    public abstract AgentCard_v0_3 getAgentCard(@Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3;

    /**
     * Close the transport and release any associated resources.
     */
    public abstract void close();

    /**
     * Process the event using all configured consumers.
     */
    void consume(ClientEvent_v0_3 clientEventOrMessage, AgentCard_v0_3 agentCard) {
        for (BiConsumer<ClientEvent_v0_3, AgentCard_v0_3> consumer : consumers) {
            consumer.accept(clientEventOrMessage, agentCard);
        }
    }

    /**
     * Get the error handler that should be used during streaming.
     *
     * @return the streaming error handler
     */
    public @Nullable Consumer<Throwable> getStreamingErrorHandler() {
        return streamingErrorHandler;
    }

}