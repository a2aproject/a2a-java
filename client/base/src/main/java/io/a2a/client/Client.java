package io.a2a.client;

import static io.a2a.util.Assert.checkNotNullParam;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.a2a.client.config.ClientConfig;
import io.a2a.client.transport.spi.ClientTransport;
import io.a2a.client.transport.spi.interceptors.ClientCallContext;
import io.a2a.jsonrpc.common.wrappers.ListTasksResult;
import io.a2a.spec.A2AClientError;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.A2AClientInvalidStateError;
import io.a2a.spec.AgentCard;
import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.EventKind;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigResult;
import io.a2a.spec.ListTasksParams;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendConfiguration;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import io.a2a.spec.TaskStatusUpdateEvent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A client for communicating with A2A agents using the Agent2Agent Protocol.
 * <p>
 * The Client class provides the primary API for sending messages to agents, managing tasks,
 * configuring push notifications, and subscribing to task updates. It abstracts the underlying
 * transport protocol (JSON-RPC, gRPC, REST) and provides a consistent interface for all
 * agent interactions.
 * <p>
 * <b>Key capabilities:</b>
 * <ul>
 *   <li><b>Message exchange:</b> Send messages to agents and receive responses via event consumers</li>
 *   <li><b>Task management:</b> Query, list, and cancel tasks</li>
 *   <li><b>Streaming support:</b> Real-time event streaming when both client and server support it</li>
 *   <li><b>Push notifications:</b> Configure webhooks for task state changes</li>
 *   <li><b>Resubscription:</b> Resume receiving events for ongoing tasks after disconnection</li>
 * </ul>
 * <p>
 * <b>Resource management:</b> Client implements {@link AutoCloseable} and should be used with
 * try-with-resources to ensure proper cleanup:
 * <pre>{@code
 * AgentCard card = A2A.getAgentCard("http://localhost:9999");
 *
 * try (Client client = Client.builder(card)
 *         .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder())
 *         .addConsumer((event, agentCard) -> {
 *             if (event instanceof MessageEvent me) {
 *                 System.out.println("Response: " + me.getMessage().parts());
 *             }
 *         })
 *         .build()) {
 *
 *     // Send messages - client automatically closed when done
 *     client.sendMessage(A2A.toUserMessage("Tell me a joke"));
 * }
 * }</pre>
 * <p>
 * <b>Manual resource management:</b> If not using try-with-resources, call {@link #close()}
 * explicitly when done:
 * <pre>{@code
 * Client client = Client.builder(card)
 *     .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder())
 *     .addConsumer((event, agentCard) -> {
 *         // Handle events
 *     })
 *     .build();
 *
 * try {
 *     client.sendMessage(A2A.toUserMessage("Tell me a joke"));
 * } finally {
 *     client.close();  // Always close to release resources
 * }
 * }</pre>
 * <p>
 * <b>Event consumption model:</b> Responses from the agent are delivered as {@link ClientEvent}
 * instances to the registered consumers:
 * <ul>
 *   <li>{@link MessageEvent} - contains agent response messages with content parts</li>
 *   <li>{@link TaskEvent} - contains complete task state (typically final state)</li>
 *   <li>{@link TaskUpdateEvent} - contains incremental task updates (status or artifact changes)</li>
 * </ul>
 * <p>
 * <b>Streaming vs blocking:</b> The client supports two communication modes:
 * <ul>
 *   <li><b>Blocking:</b> {@link #sendMessage} blocks until the agent completes the task</li>
 *   <li><b>Streaming:</b> {@link #sendMessage} returns immediately, events delivered asynchronously
 *       to consumers as the agent processes the request</li>
 * </ul>
 * The mode is determined by {@link ClientConfig#isStreaming()} AND {@link io.a2a.spec.AgentCapabilities#streaming()}.
 * Both must be {@code true} for streaming mode; otherwise blocking mode is used.
 * <p>
 * <b>Task lifecycle example:</b>
 * <pre>{@code
 * client.addConsumer((event, card) -> {
 *     if (event instanceof TaskUpdateEvent tue) {
 *         TaskState state = tue.getTask().status().state();
 *         switch (state) {
 *             case SUBMITTED -> System.out.println("Task created");
 *             case WORKING -> System.out.println("Agent is processing...");
 *             case COMPLETED -> System.out.println("Task finished");
 *             case FAILED -> System.err.println("Task failed: " +
 *                 tue.getTask().status().message());
 *         }
 *         
 *         // Check for new artifacts
 *         if (tue.getUpdateEvent() instanceof TaskArtifactUpdateEvent update) {
 *             Artifact artifact = update.artifact();
 *             System.out.println("New content: " + artifact.parts());
 *         }
 *     }
 * });
 * }</pre>
 * <p>
 * <b>Push notifications:</b> Configure webhooks to receive task updates:
 * <pre>{@code
 * // Configure push notifications for a task
 * PushNotificationConfig pushConfig = new PushNotificationConfig(
 *     "https://my-app.com/webhooks/task-updates",
 *     Map.of("Authorization", "Bearer my-token")
 * );
 *
 * // Send message with push notifications
 * client.sendMessage(
 *     A2A.toUserMessage("Process this data"),
 *     pushConfig,
 *     null,  // metadata
 *     null   // context
 * );
 * }</pre>
 * <p>
 * <b>Resubscription after disconnection:</b>
 * <pre>{@code
 * // Original request
 * client.sendMessage(A2A.toUserMessage("Long-running task"));
 * // ... client disconnects ...
 *
 * // Later, reconnect and resume receiving events
 * String taskId = "task-123";  // From original request
 * client.subscribeToTask(
 *     new TaskIdParams(taskId),
 *     List.of((event, card) -> {
 *         // Process events from where we left off
 *     }),
 *     null,  // error handler
 *     null   // context
 * );
 * }</pre>
 * <p>
 * <b>Thread safety:</b> Client instances are thread-safe and can be used concurrently from
 * multiple threads. Event consumers must also be thread-safe as they may be invoked concurrently
 * for different tasks.
 * <p>
 * <b>Resource management:</b> Clients hold resources (HTTP connections, gRPC channels, etc.)
 * and should be closed when no longer needed:
 * <pre>{@code
 * try (Client client = Client.builder(card)...build()) {
 *     client.sendMessage(...);
 * } // Automatically closed
 * }</pre>
 *
 * @see ClientBuilder
 * @see ClientEvent
 * @see MessageEvent
 * @see TaskEvent
 * @see TaskUpdateEvent
 * @see io.a2a.A2A
 */
public class Client extends AbstractClient {

    private final ClientConfig clientConfig;
    private final ClientTransport clientTransport;
    private AgentCard agentCard;

    /**
     * Package-private constructor used by {@link ClientBuilder#build()}.
     *
     * @param agentCard the agent card for the target agent
     * @param clientConfig the client configuration
     * @param clientTransport the transport protocol implementation
     * @param consumers the event consumers
     * @param streamingErrorHandler the error handler for streaming scenarios
     */
    Client(AgentCard agentCard, ClientConfig clientConfig, ClientTransport clientTransport,
                  List<BiConsumer<ClientEvent, AgentCard>> consumers, @Nullable Consumer<Throwable> streamingErrorHandler) {
        super(consumers, streamingErrorHandler);
        checkNotNullParam("agentCard", agentCard);

        this.agentCard = agentCard;
        this.clientConfig = clientConfig;
        this.clientTransport = clientTransport;
    }

    /**
     * Create a new builder for constructing a client instance.
     * <p>
     * This is the primary entry point for creating clients. The builder provides a fluent
     * API for configuring transports, event consumers, and client behavior.
     * <p>
     * Example:
     * <pre>{@code
     * AgentCard card = A2A.getAgentCard("http://localhost:9999");
     * Client client = Client.builder(card)
     *     .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder())
     *     .addConsumer((event, agentCard) -> processEvent(event))
     *     .build();
     * }</pre>
     *
     * @param agentCard the agent card describing the agent to communicate with
     * @return a new builder instance
     * @see ClientBuilder
     */
    public static ClientBuilder builder(AgentCard agentCard) {
        return new ClientBuilder(agentCard);
    }

    @Override
    public void sendMessage(@NonNull Message request,
                            @NonNull List<BiConsumer<ClientEvent, AgentCard>> consumers,
                            @Nullable Consumer<Throwable> streamingErrorHandler,
                            @Nullable ClientCallContext context) throws A2AClientException {
        MessageSendParams messageSendParams = getMessageSendParams(request, clientConfig);
        sendMessage(messageSendParams, consumers, streamingErrorHandler, context);
    }

    /**
     * Send a message to the agent.
     * <p>
     * This is the primary method for communicating with an agent. The behavior depends on
     * whether streaming is enabled:
     * <ul>
     *   <li><b>Streaming mode:</b> Returns immediately, events delivered asynchronously to consumers</li>
     *   <li><b>Blocking mode:</b> Blocks until the agent completes the task, then invokes consumers</li>
     * </ul>
     * Streaming mode is active when both {@link ClientConfig#isStreaming()} AND
     * {@link io.a2a.spec.AgentCapabilities#streaming()} are {@code true}.
     * <p>
     * <b>Simple example:</b>
     * <pre>{@code
     * Message userMessage = A2A.toUserMessage("What's the weather?");
     * client.sendMessage(userMessage, null, null, null);
     * // Events delivered to consumers registered during client construction
     * }</pre>
     * <p>
     * <b>With push notifications:</b>
     * <pre>{@code
     * PushNotificationConfig pushConfig = new PushNotificationConfig(
     *     "https://my-app.com/webhook",
     *     Map.of("Authorization", "Bearer token")
     * );
     * client.sendMessage(userMessage, pushConfig, null, null);
     * }</pre>
     * <p>
     * <b>With metadata:</b>
     * <pre>{@code
     * Map<String, Object> metadata = Map.of(
     *     "userId", "user-123",
     *     "sessionId", "session-456"
     * );
     * client.sendMessage(userMessage, null, metadata, null);
     * }</pre>
     *
     * @param request the message to send (required)
     * @param pushNotificationConfiguration webhook configuration for task updates (optional)
     * @param metadata custom metadata to attach to the request (optional)
     * @param context custom call context for request interceptors (optional)
     * @throws A2AClientException if the message cannot be sent or if the agent returns an error
     * @see #sendMessage(Message, List, Consumer, ClientCallContext)
     * @see PushNotificationConfig
     */
    @Override
    public void sendMessage(@NonNull Message request,
                            @Nullable PushNotificationConfig pushNotificationConfiguration,
                            @Nullable Map<String, Object> metadata,
                            @Nullable ClientCallContext context) throws A2AClientException {
        MessageSendConfiguration messageSendConfiguration = createMessageSendConfiguration(pushNotificationConfiguration);

        MessageSendParams messageSendParams = MessageSendParams.builder()
                .message(request)
                .configuration(messageSendConfiguration)
                .metadata(metadata)
                .build();

        sendMessage(messageSendParams, consumers, streamingErrorHandler, context);
    }

    /**
     * Retrieve a specific task by ID.
     * <p>
     * This method queries the agent for the current state of a task. It's useful for:
     * <ul>
     *   <li>Checking the status of a task after disconnection</li>
     *   <li>Retrieving task results without subscribing to events</li>
     *   <li>Polling for task completion (when streaming is not available)</li>
     * </ul>
     * <p>
     * Example:
     * <pre>{@code
     * Task task = client.getTask(new TaskQueryParams("task-123"));
     * if (task.status().state() == TaskState.COMPLETED) {
     *     Artifact result = task.artifact();
     *     System.out.println("Result: " + result.parts());
     * } else if (task.status().state() == TaskState.FAILED) {
     *     System.err.println("Task failed: " + task.status().message());
     * }
     * }</pre>
     *
     * @param request the task query parameters containing the task ID
     * @param context custom call context for request interceptors (optional)
     * @return the current task state
     * @throws A2AClientException if the task is not found or if a communication error occurs
     * @see TaskQueryParams
     * @see Task
     */
    @Override
    public Task getTask(TaskQueryParams request, @Nullable ClientCallContext context) throws A2AClientException {
        return clientTransport.getTask(request, context);
    }

    /**
     * List tasks for the current session or context.
     * <p>
     * This method retrieves multiple tasks based on filter criteria. Useful for:
     * <ul>
     *   <li>Viewing all tasks in a session/context</li>
     *   <li>Finding tasks by state (e.g., all failed tasks)</li>
     *   <li>Paginating through large task lists</li>
     * </ul>
     * <p>
     * Example:
     * <pre>{@code
     * // List all tasks for a context
     * ListTasksParams params = new ListTasksParams(
     *     "session-123",  // contextId
     *     null,           // state filter (null = all states)
     *     10,             // limit
     *     null            // offset
     * );
     * ListTasksResult result = client.listTasks(params);
     * for (Task task : result.tasks()) {
     *     System.out.println(task.id() + ": " + task.status().state());
     * }
     * }</pre>
     *
     * @param request the list parameters with optional filters
     * @param context custom call context for request interceptors (optional)
     * @return the list of tasks matching the criteria
     * @throws A2AClientException if a communication error occurs
     * @see ListTasksParams
     * @see ListTasksResult
     */
    @Override
    public ListTasksResult listTasks(ListTasksParams request, @Nullable ClientCallContext context) throws A2AClientException {
        return clientTransport.listTasks(request, context);
    }

    /**
     * Request cancellation of a task.
     * <p>
     * This method sends a cancellation request to the agent for the specified task. The agent
     * may or may not honor the request depending on its implementation and the task's current state.
     * <p>
     * <b>Important notes:</b>
     * <ul>
     *   <li>Cancellation is a request, not a guarantee - agents may decline or be unable to cancel</li>
     *   <li>Some agents don't support cancellation and will return {@link io.a2a.spec.UnsupportedOperationError}</li>
     *   <li>Tasks in final states (COMPLETED, FAILED, CANCELED) cannot be canceled</li>
     *   <li>The returned task will have state CANCELED if the cancellation succeeded</li>
     * </ul>
     * <p>
     * Example:
     * <pre>{@code
     * try {
     *     Task canceledTask = client.cancelTask(new TaskIdParams("task-123"));
     *     if (canceledTask.status().state() == TaskState.CANCELED) {
     *         System.out.println("Task successfully canceled");
     *     }
     * } catch (A2AClientException e) {
     *     if (e.getCause() instanceof UnsupportedOperationError) {
     *         System.err.println("Agent does not support cancellation");
     *     } else if (e.getCause() instanceof TaskNotFoundError) {
     *         System.err.println("Task not found");
     *     }
     * }
     * }</pre>
     *
     * @param request the task ID to cancel
     * @param context custom call context for request interceptors (optional)
     * @return the task with CANCELED status if successful
     * @throws A2AClientException if the task cannot be canceled or if a communication error occurs
     * @see TaskIdParams
     * @see io.a2a.spec.UnsupportedOperationError
     * @see io.a2a.spec.TaskNotFoundError
     */
    @Override
    public Task cancelTask(TaskIdParams request, @Nullable ClientCallContext context) throws A2AClientException {
        return clientTransport.cancelTask(request, context);
    }

    /**
     * Configure push notifications for a task.
     * <p>
     * Push notifications allow your application to receive task updates via webhook instead
     * of maintaining an active connection. When configured, the agent will POST events to
     * the specified URL as the task progresses.
     * <p>
     * Example:
     * <pre>{@code
     * TaskPushNotificationConfig config = new TaskPushNotificationConfig(
     *     "task-123",
     *     new PushNotificationConfig(
     *         "https://my-app.com/webhooks/task-updates",
     *         Map.of(
     *             "Authorization", "Bearer my-webhook-secret",
     *             "X-App-ID", "my-app"
     *         )
     *     )
     * );
     * client.createTaskPushNotificationConfiguration(config);
     * }</pre>
     *
     * @param request the push notification configuration for the task
     * @param context custom call context for request interceptors (optional)
     * @return the stored configuration (may include server-assigned IDs)
     * @throws A2AClientException if the configuration cannot be set
     * @see TaskPushNotificationConfig
     * @see PushNotificationConfig
     */
    @Override
    public TaskPushNotificationConfig createTaskPushNotificationConfiguration(
            TaskPushNotificationConfig request, @Nullable ClientCallContext context) throws A2AClientException {
        return clientTransport.createTaskPushNotificationConfiguration(request, context);
    }

    /**
     * Retrieve the push notification configuration for a task.
     * <p>
     * Example:
     * <pre>{@code
     * GetTaskPushNotificationConfigParams params =
     *     new GetTaskPushNotificationConfigParams("task-123");
     * TaskPushNotificationConfig config =
     *     client.getTaskPushNotificationConfiguration(params);
     * System.out.println("Webhook URL: " +
     *     config.pushNotificationConfig().url());
     * }</pre>
     *
     * @param request the parameters specifying which task's configuration to retrieve
     * @param context custom call context for request interceptors (optional)
     * @return the push notification configuration for the task
     * @throws A2AClientException if the configuration cannot be retrieved
     * @see GetTaskPushNotificationConfigParams
     */
    @Override
    public TaskPushNotificationConfig getTaskPushNotificationConfiguration(
            GetTaskPushNotificationConfigParams request, @Nullable ClientCallContext context) throws A2AClientException {
        return clientTransport.getTaskPushNotificationConfiguration(request, context);
    }

    /**
     * List all push notification configurations, optionally filtered by task or context.
     * <p>
     * Example:
     * <pre>{@code
     * // List all configurations for a context
     * ListTaskPushNotificationConfigParams params =
     *     new ListTaskPushNotificationConfigParams("session-123", null, 10, null);
     * ListTaskPushNotificationConfigResult result =
     *     client.listTaskPushNotificationConfigurations(params);
     * for (TaskPushNotificationConfig config : result.configurations()) {
     *     System.out.println("Task " + config.taskId() + " -> " +
     *         config.pushNotificationConfig().url());
     * }
     * }</pre>
     *
     * @param request the list parameters with optional filters
     * @param context custom call context for request interceptors (optional)
     * @return the list of push notification configurations
     * @throws A2AClientException if the configurations cannot be retrieved
     * @see ListTaskPushNotificationConfigParams
     */
    @Override
    public ListTaskPushNotificationConfigResult listTaskPushNotificationConfigurations(
            ListTaskPushNotificationConfigParams request, @Nullable ClientCallContext context) throws A2AClientException {
        return clientTransport.listTaskPushNotificationConfigurations(request, context);
    }

    /**
     * Delete push notification configurations.
     * <p>
     * This method removes push notification configurations for the specified tasks or context.
     * After deletion, the agent will stop sending webhook notifications for those tasks.
     * <p>
     * Example:
     * <pre>{@code
     * // Delete configuration for a specific task
     * DeleteTaskPushNotificationConfigParams params =
     *     new DeleteTaskPushNotificationConfigParams(
     *         null,           // contextId (null = not filtering by context)
     *         List.of("task-123", "task-456")  // specific task IDs
     *     );
     * client.deleteTaskPushNotificationConfigurations(params);
     * }</pre>
     *
     * @param request the delete parameters specifying which configurations to remove
     * @param context custom call context for request interceptors (optional)
     * @throws A2AClientException if the configurations cannot be deleted
     * @see DeleteTaskPushNotificationConfigParams
     */
    @Override
    public void deleteTaskPushNotificationConfigurations(
            DeleteTaskPushNotificationConfigParams request, @Nullable ClientCallContext context) throws A2AClientException {
        clientTransport.deleteTaskPushNotificationConfigurations(request, context);
    }

    /**
     * Subscribe to an existing task to receive remaining events.
     * <p>
     * This method is useful when a client disconnects during a long-running task and wants to
     * resume receiving events without starting a new task. The agent will deliver any events
     * that occurred since the original subscription.
     * <p>
     * <b>Requirements:</b>
     * <ul>
     *   <li>Both {@link ClientConfig#isStreaming()} and {@link io.a2a.spec.AgentCapabilities#streaming()}
     *       must be {@code true}</li>
     *   <li>The task must still exist and not be in a final state (or the agent must support
     *       historical event replay)</li>
     * </ul>
     * <p>
     * Example:
     * <pre>{@code
     * // Original request (client1)
     * client1.sendMessage(A2A.toUserMessage("Analyze this dataset"));
     * String taskId = ...; // Save task ID from TaskEvent
     * // ... client1 disconnects ...
     *
     * // Later, reconnect (client2)
     * client2.subscribeToTask(
     *     new TaskIdParams(taskId),
     *     List.of((event, card) -> {
     *         if (event instanceof TaskUpdateEvent tue) {
     *             System.out.println("Resumed - status: " +
     *                 tue.getTask().status().state());
     *         }
     *     }),
     *     throwable -> System.err.println("Subscribe error: " + throwable),
     *     null
     * );
     * }</pre>
     *
     * @param request the task ID to subscribe to
     * @param consumers the event consumers for processing events (required)
     * @param streamingErrorHandler error handler for streaming errors (optional)
     * @param context custom call context for request interceptors (optional)
     * @throws A2AClientException if subscription is not supported or if the task cannot be found
     */
    @Override
    public void subscribeToTask(@NonNull TaskIdParams request,
                            @NonNull List<BiConsumer<ClientEvent, AgentCard>> consumers,
                            @Nullable Consumer<Throwable> streamingErrorHandler,
                            @Nullable ClientCallContext context) throws A2AClientException {
        if (! clientConfig.isStreaming() || ! agentCard.capabilities().streaming()) {
            throw new A2AClientException("Client and/or server does not support resubscription");
        }
        ClientTaskManager tracker = new ClientTaskManager();
        Consumer<Throwable> overriddenErrorHandler = getOverriddenErrorHandler(streamingErrorHandler);
        Consumer<StreamingEventKind> eventHandler = event -> {
            try {
                ClientEvent clientEvent = getClientEvent(event, tracker);
                consume(clientEvent, agentCard, consumers);
            } catch (A2AClientError e) {
                overriddenErrorHandler.accept(e);
            }
        };
        clientTransport.subscribeToTask(request, eventHandler, overriddenErrorHandler, context);
    }

    /**
     * Retrieve the agent's extended agent card.
     * <p>
     * This method fetches the extended agent card from the agent (if the extendedAgentCard capability is supported).
     * The card may have changed since
     * client construction (e.g., new skills added, capabilities updated). The client's internal
     * reference is updated to the newly retrieved card.
     * <p>
     * Example:
     * <pre>{@code
     * AgentCard updatedCard = client.getExtendedAgentCard(null);
     * System.out.println("Agent version: " + updatedCard.version());
     * System.out.println("Skills: " + updatedCard.skills().size());
     * }</pre>
     *
     * @param context custom call context for request interceptors (optional)
     * @return the agent's extended agent card
     * @throws A2AClientException if the extended agent card cannot be retrieved
     * @see AgentCard
     */
    @Override
    public AgentCard getExtendedAgentCard(@Nullable ClientCallContext context) throws A2AClientException {
        agentCard = clientTransport.getExtendedAgentCard(context);
        return agentCard;
    }

    /**
     * Close this client and release all associated resources.
     * <p>
     * This method closes the underlying transport (HTTP connections, gRPC channels, etc.)
     * and releases any other resources held by the client. After calling this method, the
     * client instance should not be used further.
     * <p>
     * <b>Important:</b> Always close clients when done to avoid resource leaks:
     * <pre>{@code
     * Client client = Client.builder(card)...build();
     * try {
     *     client.sendMessage(...);
     * } finally {
     *     client.close();
     * }
     * // Or use try-with-resources if Client implements AutoCloseable
     * }</pre>
     */
    @Override
    public void close() {
        clientTransport.close();
    }

    private ClientEvent getClientEvent(StreamingEventKind event, ClientTaskManager taskManager) throws A2AClientError {
        if (event instanceof Message message) {
            return new MessageEvent(message);
        } else if (event instanceof Task task) {
            taskManager.saveTaskEvent(task);
            return new TaskEvent(taskManager.getCurrentTask());
        } else if (event instanceof TaskStatusUpdateEvent updateEvent) {
            taskManager.saveTaskEvent(updateEvent);
            return new TaskUpdateEvent(taskManager.getCurrentTask(), updateEvent);
        } else if (event instanceof TaskArtifactUpdateEvent updateEvent) {
            taskManager.saveTaskEvent(updateEvent);
            return new TaskUpdateEvent(taskManager.getCurrentTask(), updateEvent);
        } else {
            throw new A2AClientInvalidStateError("Invalid client event");
        }
    }

    private MessageSendConfiguration createMessageSendConfiguration(@Nullable PushNotificationConfig pushNotificationConfig) {
        return MessageSendConfiguration.builder()
                .acceptedOutputModes(clientConfig.getAcceptedOutputModes())
                .blocking(!clientConfig.isPolling())
                .historyLength(clientConfig.getHistoryLength())
                .pushNotificationConfig(pushNotificationConfig)
                .build();
    }

    private void sendMessage(@NonNull MessageSendParams messageSendParams, @NonNull List<BiConsumer<ClientEvent, AgentCard>> consumers,
                             @Nullable Consumer<Throwable> errorHandler, @Nullable ClientCallContext context) throws A2AClientException {
        if (! clientConfig.isStreaming() || ! agentCard.capabilities().streaming()) {
            EventKind eventKind = clientTransport.sendMessage(messageSendParams, context);
            ClientEvent clientEvent;
            if (eventKind instanceof Task task) {
                clientEvent = new TaskEvent(task);
            } else {
                // must be a message
                clientEvent = new MessageEvent((Message) eventKind);
            }
            consume(clientEvent, agentCard, consumers);
        } else {
            ClientTaskManager tracker = new ClientTaskManager();
            Consumer<Throwable> overriddenErrorHandler = getOverriddenErrorHandler(errorHandler);
            Consumer<StreamingEventKind> eventHandler = event -> {
                try {
                    ClientEvent clientEvent = getClientEvent(event, tracker);
                    consume(clientEvent, agentCard, consumers);
                } catch (A2AClientError e) {
                    overriddenErrorHandler.accept(e);
                }
            };
            clientTransport.sendMessageStreaming(messageSendParams, eventHandler, overriddenErrorHandler, context);
        }
    }

    private @NonNull Consumer<Throwable> getOverriddenErrorHandler(@Nullable Consumer<Throwable> errorHandler) {
        return e -> {
            if (errorHandler != null) {
                errorHandler.accept(e);
            } else {
                if (getStreamingErrorHandler() != null) {
                    getStreamingErrorHandler().accept(e);
                }
            }
        };
    }

    private void consume(ClientEvent clientEvent, AgentCard agentCard, @NonNull List<BiConsumer<ClientEvent, AgentCard>> consumers) {
        for (BiConsumer<ClientEvent, AgentCard> consumer : consumers) {
            consumer.accept(clientEvent, agentCard);
        }
    }

    private MessageSendParams getMessageSendParams(Message request, ClientConfig clientConfig) {
        MessageSendConfiguration messageSendConfiguration = createMessageSendConfiguration(clientConfig.getPushNotificationConfig());

        return MessageSendParams.builder()
                .message(request)
                .configuration(messageSendConfiguration)
                .metadata(clientConfig.getMetadata())
                .build();
    }
}
