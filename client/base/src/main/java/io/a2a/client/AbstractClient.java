package io.a2a.client;

import static io.a2a.util.Assert.checkNotNullParam;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.a2a.client.transport.spi.interceptors.ClientCallContext;
import io.a2a.jsonrpc.common.wrappers.ListTasksResult;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigResult;
import io.a2a.spec.ListTasksParams;
import io.a2a.spec.Message;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Abstract class representing an A2A client. Provides a standard set
 * of methods for interacting with an A2A agent, regardless of the underlying
 * transport protocol. It supports sending messages, managing tasks, and
 * handling event streams.
 */
public abstract class AbstractClient implements AutoCloseable {

    protected final @NonNull List<BiConsumer<ClientEvent, AgentCard>> consumers;
    protected final @Nullable Consumer<Throwable> streamingErrorHandler;

    public AbstractClient(@NonNull List<BiConsumer<ClientEvent, AgentCard>> consumers) {
        this(consumers, null);
    }

    public AbstractClient(@NonNull List<BiConsumer<ClientEvent, AgentCard>> consumers, @Nullable Consumer<Throwable> streamingErrorHandler) {
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
     * @throws A2AClientException if sending the message fails for any reason
     */
    public void sendMessage(@NonNull Message request) throws A2AClientException {
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
     * @param context optional client call context for the request
     * @throws A2AClientException if sending the message fails for any reason
     */
    public void sendMessage(@NonNull Message request,
                            @Nullable ClientCallContext context) throws A2AClientException {
        sendMessage(request, consumers, streamingErrorHandler, context);
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
     * @throws A2AClientException if sending the message fails for any reason
     */
    public void sendMessage(@NonNull Message request,
                            @NonNull List<BiConsumer<ClientEvent, AgentCard>> consumers,
                            @Nullable Consumer<Throwable> streamingErrorHandler) throws A2AClientException {
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
     * @param context optional client call context for the request
     * @throws A2AClientException if sending the message fails for any reason
     */
    public abstract void sendMessage(@NonNull Message request,
                                     @NonNull List<BiConsumer<ClientEvent, AgentCard>> consumers,
                                     @Nullable Consumer<Throwable> streamingErrorHandler,
                                     @Nullable ClientCallContext context) throws A2AClientException;

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
    public void sendMessage(@NonNull Message request,
                            @Nullable PushNotificationConfig pushNotificationConfiguration,
                            @Nullable Map<String, Object> metadata) throws A2AClientException {
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
     * @param context optional client call context for the request
     * @throws A2AClientException if sending the message fails for any reason
     */
    public abstract void sendMessage(@NonNull Message request,
                                     @Nullable PushNotificationConfig pushNotificationConfiguration,
                                     @Nullable Map<String, Object> metadata,
                                     @Nullable ClientCallContext context) throws A2AClientException;

    /**
     * Retrieve the current state and history of a specific task.
     *
     * @param request the task query parameters specifying which task to retrieve
     * @return the task
     * @throws A2AClientException if retrieving the task fails for any reason
     */
    public Task getTask(TaskQueryParams request) throws A2AClientException {
        return getTask(request, null);
    }

    /**
     * Retrieve the current state and history of a specific task.
     *
     * @param request the task query parameters specifying which task to retrieve
     * @param context optional client call context for the request (may be {@code null})
     * @return the task
     * @throws A2AClientException if retrieving the task fails for any reason
     */
    public abstract Task getTask(TaskQueryParams request, @Nullable ClientCallContext context) throws A2AClientException;

    /**
     * List tasks with optional filtering and pagination.
     *
     * @param request the list tasks parameters including filters and pagination
     * @return the list tasks result containing tasks and pagination information
     * @throws A2AClientException if listing tasks fails for any reason
     */
    public ListTasksResult listTasks(ListTasksParams request) throws A2AClientException {
        return listTasks(request, null);
    }

    /**
     * List tasks with optional filtering and pagination.
     *
     * @param request the list tasks parameters including filters and pagination
     * @param context optional client call context for the request (may be {@code null})
     * @return the list tasks result containing tasks and pagination information
     * @throws A2AClientException if listing tasks fails for any reason
     */
    public abstract ListTasksResult listTasks(ListTasksParams request, @Nullable ClientCallContext context) throws A2AClientException;

    /**
     * Request the agent to cancel a specific task.
     *
     * @param request the task ID parameters specifying which task to cancel
     * @return the cancelled task
     * @throws A2AClientException if cancelling the task fails for any reason
     */
    public Task cancelTask(TaskIdParams request) throws A2AClientException {
        return cancelTask(request, null);
    }

    /**
     * Request the agent to cancel a specific task.
     *
     * @param request the task ID parameters specifying which task to cancel
     * @param context optional client call context for the request (may be {@code null})
     * @return the cancelled task
     * @throws A2AClientException if cancelling the task fails for any reason
     */
    public abstract Task cancelTask(TaskIdParams request, @Nullable ClientCallContext context) throws A2AClientException;

    /**
     * Create or update the push notification configuration for a specific task.
     *
     * @param request the push notification configuration to set for the task
     * @return the configured TaskPushNotificationConfig
     * @throws A2AClientException if setting the task push notification configuration fails for any reason
     */
    public TaskPushNotificationConfig createTaskPushNotificationConfiguration(
            TaskPushNotificationConfig request) throws A2AClientException {
        return createTaskPushNotificationConfiguration(request, null);
    }

    /**
     * Create or update the push notification configuration for a specific task.
     *
     * @param request the push notification configuration to set for the task
     * @param context optional client call context for the request (may be {@code null})
     * @return the configured TaskPushNotificationConfig
     * @throws A2AClientException if setting the task push notification configuration fails for any reason
     */
    public abstract TaskPushNotificationConfig createTaskPushNotificationConfiguration(
            TaskPushNotificationConfig request,
            @Nullable ClientCallContext context) throws A2AClientException;

    /**
     * Retrieve the push notification configuration for a specific task.
     *
     * @param request the parameters specifying which task's notification config to retrieve
     * @return the task push notification config
     * @throws A2AClientException if getting the task push notification config fails for any reason
     */
    public TaskPushNotificationConfig getTaskPushNotificationConfiguration(
            GetTaskPushNotificationConfigParams request) throws A2AClientException {
        return getTaskPushNotificationConfiguration(request, null);
    }

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
            @Nullable ClientCallContext context) throws A2AClientException;

    /**
     * Retrieve the list of push notification configurations for a specific task with pagination support.
     *
     * @param request the parameters specifying which task's notification configs to retrieve
     * @return the result containing the list of task push notification configs and pagination information
     * @throws A2AClientException if getting the task push notification configs fails for any reason
     */
    public ListTaskPushNotificationConfigResult listTaskPushNotificationConfigurations(
            ListTaskPushNotificationConfigParams request) throws A2AClientException {
        return listTaskPushNotificationConfigurations(request, null);
    }

    /**
     * Retrieve the list of push notification configurations for a specific task with pagination support.
     *
     * @param request the parameters specifying which task's notification configs to retrieve
     * @param context optional client call context for the request (may be {@code null})
     * @return the result containing the list of task push notification configs and pagination information
     * @throws A2AClientException if getting the task push notification configs fails for any reason
     */
    public abstract ListTaskPushNotificationConfigResult listTaskPushNotificationConfigurations(
            ListTaskPushNotificationConfigParams request,
            @Nullable ClientCallContext context) throws A2AClientException;

    /**
     * Delete the list of push notification configurations for a specific task.
     *
     * @param request the parameters specifying which task's notification configs to delete
     * @throws A2AClientException if deleting the task push notification configs fails for any reason
     */
    public void deleteTaskPushNotificationConfigurations(
            DeleteTaskPushNotificationConfigParams request) throws A2AClientException {
        deleteTaskPushNotificationConfigurations(request, null);
    }

    /**
     * Delete the list of push notification configurations for a specific task.
     *
     * @param request the parameters specifying which task's notification configs to delete
     * @param context optional client call context for the request (may be {@code null})
     * @throws A2AClientException if deleting the task push notification configs fails for any reason
     */
    public abstract void deleteTaskPushNotificationConfigurations(
            DeleteTaskPushNotificationConfigParams request,
            @Nullable ClientCallContext context) throws A2AClientException;

    /**
     * Subscribe to a task's event stream.
     * This is only available if both the client and server support streaming.
     * The configured client consumers will be used to handle messages, tasks,
     * and update events received from the remote agent. The configured streaming
     * error handler will be used if an error occurs during streaming.
     *
     * @param request the parameters specifying which task's notification configs to delete
     * @throws A2AClientException if resubscribing fails for any reason
     */
    public void subscribeToTask(@NonNull TaskIdParams request) throws A2AClientException {
        subscribeToTask(request, consumers, streamingErrorHandler, null);
    }

    /**
     * Subscribe to a task's event stream.
     * This is only available if both the client and server support streaming.
     * The configured client consumers will be used to handle messages, tasks,
     * and update events received from the remote agent. The configured streaming
     * error handler will be used if an error occurs during streaming.
     *
     * @param request the parameters specifying which task's notification configs to delete
     * @param context optional client call context for the request
     * @throws A2AClientException if resubscribing fails for any reason
     */
    public void subscribeToTask(@NonNull TaskIdParams request,
                            @Nullable ClientCallContext context) throws A2AClientException {
        subscribeToTask(request, consumers, streamingErrorHandler, context);
    }

    /**
     * Subscribe to a task's event stream.
     * This is only available if both the client and server support streaming.
     * The specified client consumers will be used to handle messages, tasks, and
     * update events received from the remote agent. The specified streaming error
     * handler will be used if an error occurs during streaming.
     *
     * @param request the parameters specifying which task's notification configs to delete
     * @param consumers a list of consumers to pass responses from the remote agent to
     * @param streamingErrorHandler an error handler that should be used for the streaming case if an error occurs
     * @throws A2AClientException if resubscribing fails for any reason
     */
    public void subscribeToTask(@NonNull TaskIdParams request,
                            @NonNull List<BiConsumer<ClientEvent, AgentCard>> consumers,
                            @Nullable Consumer<Throwable> streamingErrorHandler) throws A2AClientException {
        subscribeToTask(request, consumers, streamingErrorHandler, null);
    }

    /**
     * Subscribe to a task's event stream.
     * This is only available if both the client and server support streaming.
     * The specified client consumers will be used to handle messages, tasks, and
     * update events received from the remote agent. The specified streaming error
     * handler will be used if an error occurs during streaming.
     *
     * @param request the parameters specifying which task's notification configs to delete
     * @param consumers a list of consumers to pass responses from the remote agent to
     * @param streamingErrorHandler an error handler that should be used for the streaming case if an error occurs
     * @param context optional client call context for the request
     * @throws A2AClientException if resubscribing fails for any reason
     */
    public abstract void subscribeToTask(@NonNull TaskIdParams request,
                                     @NonNull List<BiConsumer<ClientEvent, AgentCard>> consumers,
                                     @Nullable Consumer<Throwable> streamingErrorHandler,
                                     @Nullable ClientCallContext context) throws A2AClientException;

    /**
     * Retrieve the extended AgentCard.
     *
     * @return the extended AgentCard
     * @throws A2AClientException if retrieving the extended agent card fails for any reason
     */
    public AgentCard getExtendedAgentCard() throws A2AClientException {
        return getExtendedAgentCard(null);
    }

    /**
     * Retrieve the extended AgentCard.
     *
     * @param context optional client call context for the request (may be {@code null})
     * @return the extended AgentCard
     * @throws A2AClientException if retrieving the extended agent card fails for any reason
     */
    public abstract AgentCard getExtendedAgentCard(@Nullable ClientCallContext context) throws A2AClientException;

    /**
     * Close the transport and release any associated resources.
     */
    public abstract void close();

    /**
     * Get the error handler that should be used during streaming.
     *
     * @return the streaming error handler
     */
    public @Nullable Consumer<Throwable> getStreamingErrorHandler() {
        return streamingErrorHandler;
    }

}