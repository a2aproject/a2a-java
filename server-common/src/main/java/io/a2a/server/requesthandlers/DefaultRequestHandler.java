package io.a2a.server.requesthandlers;

import static io.a2a.server.util.async.AsyncUtils.convertingProcessor;
import static io.a2a.server.util.async.AsyncUtils.createTubeConfig;
import static io.a2a.server.util.async.AsyncUtils.processor;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.a2a.jsonrpc.common.wrappers.ListTasksResult;
import io.a2a.server.ServerCallContext;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.agentexecution.SimpleRequestContextBuilder;
import io.a2a.server.util.IdGenerator;
import io.a2a.server.config.A2AConfigProvider;
import io.a2a.server.events.EnhancedRunnable;
import io.a2a.server.events.EventConsumer;
import io.a2a.server.events.EventQueue;
import io.a2a.server.events.EventQueueItem;
import io.a2a.server.events.QueueManager;
import io.a2a.server.events.TaskQueueExistsException;
import io.a2a.server.tasks.PushNotificationConfigStore;
import io.a2a.server.tasks.PushNotificationSender;
import io.a2a.server.tasks.ResultAggregator;
import io.a2a.server.tasks.TaskManager;
import io.a2a.server.tasks.TaskStore;
import io.a2a.server.util.async.Internal;
import io.a2a.spec.A2AError;
import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.Event;
import io.a2a.spec.EventKind;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.InternalError;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigResult;
import io.a2a.spec.ListTasksParams;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskNotCancelableError;
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import io.a2a.spec.TaskState;
import io.a2a.spec.UnsupportedOperationError;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central request orchestrator that coordinates transport requests with agent execution,
 * task persistence, event routing, and push notifications.
 * <p>
 * This class is the core of the A2A server runtime. It receives requests from transport
 * layers (JSON-RPC, gRPC, REST), executes user-provided {@link AgentExecutor} logic
 * asynchronously, manages event queues for response streaming, and ensures task state
 * is persisted through {@link TaskStore}.
 * </p>
 *
 * <h2>Architecture Overview</h2>
 * <pre>
 * Transport Layer (JSON-RPC/gRPC/REST)
 *     ↓ calls DefaultRequestHandler methods
 * DefaultRequestHandler (orchestrates)
 *     ↓
 * ┌─────────────┬──────────────┬─────────────────┬──────────────────┐
 * │ AgentExecutor│  TaskStore   │  QueueManager   │ PushNotification │
 * │ (user logic) │ (persistence)│ (event routing) │ (notifications)  │
 * └─────────────┴──────────────┴─────────────────┴──────────────────┘
 * </pre>
 *
 * <h2>Request Flow - Blocking Mode (onMessageSend)</h2>
 * <ol>
 *   <li>Transport calls {@link #onMessageSend(MessageSendParams, ServerCallContext)}</li>
 *   <li>Initialize {@link TaskManager} and {@link RequestContext}</li>
 *   <li>Create or tap {@link EventQueue} via {@link QueueManager}</li>
 *   <li>Execute {@link AgentExecutor#execute(RequestContext, EventQueue)} asynchronously in background thread pool</li>
 *   <li>Consume events from queue on Vert.x worker thread via {@link EventConsumer}</li>
 *   <li>For blocking=true: wait for agent completion and full event consumption</li>
 *   <li>Return {@link Task} or {@link Message} to transport</li>
 *   <li>Cleanup queue and agent future in background</li>
 * </ol>
 *
 * <h2>Request Flow - Streaming Mode (onMessageSendStream)</h2>
 * <ol>
 *   <li>Transport calls {@link #onMessageSendStream(MessageSendParams, ServerCallContext)}</li>
 *   <li>Initialize components (same as blocking)</li>
 *   <li>Execute {@link AgentExecutor#execute(RequestContext, EventQueue)} asynchronously</li>
 *   <li>Return {@link java.util.concurrent.Flow.Publisher Flow.Publisher}&lt;StreamingEventKind&gt; immediately</li>
 *   <li>Events stream to client as they arrive in the queue</li>
 *   <li>On client disconnect: continue consumption in background (fire-and-forget)</li>
 *   <li>Cleanup after streaming completes</li>
 * </ol>
 *
 * <h2>Queue Lifecycle Management</h2>
 * <ul>
 *   <li>{@link QueueManager#createOrTap(String)} creates a MainQueue (new task) or ChildQueue (resubscription)</li>
 *   <li>Agent enqueues events on background thread via {@link EventQueue#enqueueEvent(Event)}</li>
 *   <li>{@link EventConsumer} polls and processes events on Vert.x worker thread</li>
 *   <li>Queue closes automatically on final event (COMPLETED/FAILED/CANCELED)</li>
 *   <li>Cleanup waits for both agent execution AND event consumption to complete</li>
 *   <li>Background tasks tracked via {@link #trackBackgroundTask(CompletableFuture)}</li>
 * </ul>
 *
 * <h2>Threading Model</h2>
 * <ul>
 *   <li><b>Vert.x worker threads:</b> Execute request handler methods (onMessageSend, etc.)</li>
 *   <li><b>Agent-executor pool (@Internal):</b> Execute {@link AgentExecutor#execute(RequestContext, EventQueue)}</li>
 *   <li><b>Background cleanup:</b> {@link java.util.concurrent.CompletableFuture CompletableFuture} async tasks</li>
 * </ul>
 * <p>
 * <b>Important:</b> Avoid blocking operations on Vert.x worker threads - they are limited
 * and shared across all requests.
 * </p>
 *
 * <h2>Blocking vs Streaming</h2>
 * <ul>
 *   <li><b>Blocking (configuration.blocking=true):</b> Client waits for first event or final task state</li>
 *   <li><b>Streaming:</b> Client receives events as they arrive via reactive streams</li>
 *   <li>Both modes support fire-and-forget (agent continues after client disconnect)</li>
 *   <li>Configurable timeouts via {@code a2a.blocking.agent.timeout.seconds} and
 *       {@code a2a.blocking.consumption.timeout.seconds}</li>
 * </ul>
 *
 * <h2>CDI Dependencies</h2>
 * This class is {@code @ApplicationScoped} and automatically injects:
 * <ul>
 *   <li>{@link AgentExecutor} - User-provided agent business logic (required)</li>
 *   <li>{@link TaskStore} - Task persistence (default: {@link io.a2a.server.tasks.InMemoryTaskStore})</li>
 *   <li>{@link QueueManager} - Event queue management (default: {@link io.a2a.server.events.InMemoryQueueManager})</li>
 *   <li>{@link PushNotificationConfigStore} - Push config storage (default: {@link io.a2a.server.tasks.InMemoryPushNotificationConfigStore})</li>
 *   <li>{@link PushNotificationSender} - Push notification delivery (default: {@link io.a2a.server.tasks.BasePushNotificationSender})</li>
 *   <li>{@link io.a2a.server.config.A2AConfigProvider} - Configuration values</li>
 *   <li>{@link java.util.concurrent.Executor} (@Internal) - Background thread pool</li>
 * </ul>
 *
 * <h2>Extension Strategy</h2>
 * Users typically don't replace DefaultRequestHandler. Instead, provide custom implementations
 * of its dependencies via CDI:
 * <ul>
 *   <li>{@link AgentExecutor} (required) - Your agent business logic</li>
 *   <li>{@link TaskStore} (@Alternative @Priority) - Database persistence (see extras/task-store-database-jpa)</li>
 *   <li>{@link QueueManager} (@Alternative @Priority) - Replication support (see extras/queue-manager-replicated)</li>
 *   <li>{@link PushNotificationSender} (@Alternative @Priority) - Custom notification delivery</li>
 * </ul>
 *
 * @see RequestHandler
 * @see AgentExecutor
 * @see TaskStore
 * @see QueueManager
 * @see EventQueue
 * @see TaskManager
 */
@ApplicationScoped
public class DefaultRequestHandler implements RequestHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRequestHandler.class);

    private static final String A2A_BLOCKING_AGENT_TIMEOUT_SECONDS = "a2a.blocking.agent.timeout.seconds";
    private static final String A2A_BLOCKING_CONSUMPTION_TIMEOUT_SECONDS = "a2a.blocking.consumption.timeout.seconds";

    @Inject
    A2AConfigProvider configProvider;

    @Inject
    IdGenerator idGenerator;

    /**
     * Timeout in seconds to wait for agent execution to complete in blocking calls.
     * This allows slow agents (LLM-based, data processing, external APIs) sufficient time.
     * <p>
     * Property: {@code a2a.blocking.agent.timeout.seconds}<br>
     * Default: 30 seconds<br>
     * Note: Property override requires a configurable {@link A2AConfigProvider} on the classpath
     * (e.g., MicroProfileConfigProvider in reference implementations).
     */
    int agentCompletionTimeoutSeconds;

    /**
     * Timeout in seconds to wait for event consumption to complete in blocking calls.
     * This ensures all events are processed and persisted before returning to client.
     * <p>
     * Property: {@code a2a.blocking.consumption.timeout.seconds}<br>
     * Default: 5 seconds<br>
     * Note: Property override requires a configurable {@link A2AConfigProvider} on the classpath
     * (e.g., MicroProfileConfigProvider in reference implementations).
     */
    int consumptionCompletionTimeoutSeconds;

    // Fields set by constructor injection cannot be final. We need a noargs constructor for
    // Jakarta compatibility, and it seems that making fields set by constructor injection
    // final, is not proxyable in all runtimes
    private AgentExecutor agentExecutor;
    private TaskStore taskStore;
    private QueueManager queueManager;
    private PushNotificationConfigStore pushConfigStore;
    private PushNotificationSender pushSender;
    private Supplier<RequestContext.Builder> requestContextBuilder;

    private final ConcurrentMap<String, CompletableFuture<Void>> runningAgents = new ConcurrentHashMap<>();
    private final Set<CompletableFuture<Void>> backgroundTasks = ConcurrentHashMap.newKeySet();

    private Executor executor;

    /**
     * No-args constructor for CDI proxy creation.
     * CDI requires a non-private constructor to create proxies for @ApplicationScoped beans.
     * All fields are initialized by the @Inject constructor during actual bean creation.
     */
    @SuppressWarnings("NullAway")
    protected DefaultRequestHandler() {
        // For CDI proxy creation
        this.agentExecutor = null;
        this.taskStore = null;
        this.queueManager = null;
        this.pushConfigStore = null;
        this.pushSender = null;
        this.requestContextBuilder = null;
        this.executor = null;
    }

    @Inject
    public DefaultRequestHandler(AgentExecutor agentExecutor, TaskStore taskStore,
                                 QueueManager queueManager, PushNotificationConfigStore pushConfigStore,
                                 PushNotificationSender pushSender, @Internal Executor executor) {
        this.agentExecutor = agentExecutor;
        this.taskStore = taskStore;
        this.queueManager = queueManager;
        this.pushConfigStore = pushConfigStore;
        this.pushSender = pushSender;
        this.executor = executor;
        // TODO In Python this is also a constructor parameter defaulting to this SimpleRequestContextBuilder
        //  implementation if the parameter is null. Skip that for now, since otherwise I get CDI errors, and
        //  I am unsure about the correct scope.
        //  Also reworked to make a Supplier since otherwise the builder gets polluted with wrong tasks
        this.requestContextBuilder = () -> new SimpleRequestContextBuilder(taskStore, false, idGenerator);
    }

    @PostConstruct
    void initConfig() {
        agentCompletionTimeoutSeconds = Integer.parseInt(
                configProvider.getValue(A2A_BLOCKING_AGENT_TIMEOUT_SECONDS));
        consumptionCompletionTimeoutSeconds = Integer.parseInt(
                configProvider.getValue(A2A_BLOCKING_CONSUMPTION_TIMEOUT_SECONDS));
    }

    /**
     * For testing
     */
    public static DefaultRequestHandler create(AgentExecutor agentExecutor, TaskStore taskStore,
                         QueueManager queueManager, PushNotificationConfigStore pushConfigStore,
                         PushNotificationSender pushSender, Executor executor) {
        DefaultRequestHandler handler =
                new DefaultRequestHandler(agentExecutor, taskStore, queueManager, pushConfigStore, pushSender, executor);
        handler.agentCompletionTimeoutSeconds = 5;
        handler.consumptionCompletionTimeoutSeconds = 2;
        return handler;
    }

    @Override
    public Task onGetTask(TaskQueryParams params, ServerCallContext context) throws A2AError {
        LOGGER.debug("onGetTask {}", params.id());
        Task task = taskStore.get(params.id());
        if (task == null) {
            LOGGER.debug("No task found for {}. Throwing TaskNotFoundError", params.id());
            throw new TaskNotFoundError();
        }
        task = limitTaskHistory(task, params.historyLength());
        LOGGER.debug("Task found {}", task);
        return task;
    }

    /**
     * Limits the history of a task to the most recent N messages.
     *
     * @param task the task to limit
     * @param historyLength the maximum number of recent messages to keep (0 or negative = unlimited)
     * @return the task with limited history, or the original task if no limiting needed
     */
    private static Task limitTaskHistory(Task task, @Nullable Integer historyLength) {
        if (task.history() == null || historyLength == null || historyLength >= task.history().size()) {
            return task;
        }
        // Keep only the most recent historyLength messages
        List<Message> limitedHistory = task.history().subList(
                task.history().size() - historyLength,
                task.history().size());
        return Task.builder(task)
                .history(limitedHistory)
                .build();
    }

    @Override
    public ListTasksResult onListTasks(ListTasksParams params, ServerCallContext context) throws A2AError {
        LOGGER.debug("onListTasks with contextId={}, status={}, pageSize={}, pageToken={}, statusTimestampAfter={}",
                params.contextId(), params.status(), params.pageSize(), params.pageToken(), params.statusTimestampAfter());

        // Validate statusTimestampAfter timestamp if provided
        if (params.statusTimestampAfter() != null) {
            // Check if timestamp is in the future (optional validation per spec)
            Instant now = Instant.now();
            if (params.statusTimestampAfter().isAfter(now)) {
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("parameter", "lastUpdatedAfter");
                errorData.put("reason", "Timestamp cannot be in the future");
                throw new InvalidParamsError(null, "Invalid params", errorData);
            }
            // Check that timestamp is not negative
            long millis = params.statusTimestampAfter().toEpochMilli();
            if (millis < 0L) {
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("parameter", "statusTimestampAfter");
                errorData.put("reason", "Must be a non-negative timestamp value, got: " + millis);
                throw new InvalidParamsError(null, "Invalid params", errorData);
            }
        }

        ListTasksResult result = taskStore.list(params);
        LOGGER.debug("Found {} tasks (total: {})", result.pageSize(), result.totalSize());
        return result;
    }

    @Override
    public Task onCancelTask(TaskIdParams params, ServerCallContext context) throws A2AError {
        Task task = taskStore.get(params.id());
        if (task == null) {
            throw new TaskNotFoundError();
        }

        // Check if task is in a non-cancelable state (completed, canceled, failed, rejected)
        if (task.status().state().isFinal()) {
            throw new TaskNotCancelableError(
                    "Task cannot be canceled - current state: " + task.status().state().asString());
        }

        TaskManager taskManager = new TaskManager(
                task.id(),
                task.contextId(),
                taskStore,
                null);

        ResultAggregator resultAggregator = new ResultAggregator(taskManager, null, executor);

        EventQueue queue = queueManager.tap(task.id());
        if (queue == null) {
            queue = queueManager.getEventQueueBuilder(task.id()).build();
        }
        agentExecutor.cancel(
                requestContextBuilder.get()
                        .setTaskId(task.id())
                        .setContextId(task.contextId())
                        .setTask(task)
                        .setCallContext(context)
                        .build(),
                queue);

        Optional.ofNullable(runningAgents.get(task.id()))
                .ifPresent(cf -> cf.cancel(true));

        EventConsumer consumer = new EventConsumer(queue);
        EventKind type = resultAggregator.consumeAll(consumer);
        if (!(type instanceof Task tempTask)) {
            throw new InternalError("Agent did not return valid response for cancel");
        }

        // Verify task was actually canceled (not completed concurrently)
        if (tempTask.status().state() != TaskState.CANCELED) {
            throw new TaskNotCancelableError(
                    "Task cannot be canceled - current state: " + tempTask.status().state().asString());
        }

        return tempTask;
    }

    @Override
    public EventKind onMessageSend(MessageSendParams params, ServerCallContext context) throws A2AError {
        LOGGER.debug("onMessageSend - task: {}; context {}", params.message().taskId(), params.message().contextId());
        MessageSendSetup mss = initMessageSend(params, context);

        String taskId = mss.requestContext.getTaskId();
        LOGGER.debug("Request context taskId: {}", taskId);

        if (taskId == null) {
            throw new io.a2a.spec.InternalError("Task ID is null in onMessageSend");
        }
        EventQueue queue = queueManager.createOrTap(taskId);
        ResultAggregator resultAggregator = new ResultAggregator(mss.taskManager, null, executor);

        boolean blocking = params.configuration() != null && Boolean.TRUE.equals(params.configuration().blocking());

        boolean interruptedOrNonBlocking = false;

        EnhancedRunnable producerRunnable = registerAndExecuteAgentAsync(taskId, mss.requestContext, queue);
        ResultAggregator.EventTypeAndInterrupt etai = null;
        EventKind kind = null;  // Declare outside try block so it's in scope for return
        try {
            // Create callback for push notifications during background event processing
            Runnable pushNotificationCallback = () -> sendPushNotification(taskId, resultAggregator);

            EventConsumer consumer = new EventConsumer(queue);

            // This callback must be added before we start consuming. Otherwise,
            // any errors thrown by the producerRunnable are not picked up by the consumer
            producerRunnable.addDoneCallback(consumer.createAgentRunnableDoneCallback());

            // Get agent future before consuming (for blocking calls to wait for agent completion)
            CompletableFuture<Void> agentFuture = runningAgents.get(taskId);
            etai = resultAggregator.consumeAndBreakOnInterrupt(consumer, blocking);

            if (etai == null) {
                LOGGER.debug("No result, throwing InternalError");
                throw new InternalError("No result");
            }
            interruptedOrNonBlocking = etai.interrupted();
            LOGGER.debug("Was interrupted or non-blocking: {}", interruptedOrNonBlocking);

            // For blocking calls that were interrupted (returned on first event),
            // wait for agent execution and event processing BEFORE returning to client.
            // This ensures the returned Task has all artifacts and current state.
            // We do this HERE (not in ResultAggregator) to avoid blocking Vert.x worker threads
            // during the consumption loop itself.
            kind = etai.eventType();

            // Store push notification config for newly created tasks (mirrors streaming logic)
            // Only for NEW tasks - existing tasks are handled by initMessageSend()
            if (mss.task() == null && kind instanceof Task createdTask && shouldAddPushInfo(params)) {
                LOGGER.debug("Storing push notification config for new task {}", createdTask.id());
                pushConfigStore.setInfo(createdTask.id(), params.configuration().pushNotificationConfig());
            }

            if (blocking && interruptedOrNonBlocking) {
                // For blocking calls: ensure all events are processed before returning
                // Order of operations is critical to avoid circular dependency:
                // 1. Wait for agent to finish enqueueing events
                // 2. Close the queue to signal consumption can complete
                // 3. Wait for consumption to finish processing events
                // 4. Fetch final task state from TaskStore

                try {
                    // Step 1: Wait for agent to finish (with configurable timeout)
                    if (agentFuture != null) {
                        try {
                            agentFuture.get(agentCompletionTimeoutSeconds, SECONDS);
                            LOGGER.debug("Agent completed for task {}", taskId);
                        } catch (java.util.concurrent.TimeoutException e) {
                            // Agent still running after timeout - that's fine, events already being processed
                            LOGGER.debug("Agent still running for task {} after {}s", taskId, agentCompletionTimeoutSeconds);
                        }
                    }

                    // Step 2: Close the queue to signal consumption can complete
                    // For fire-and-forget tasks, there's no final event, so we need to close the queue
                    // This allows EventConsumer.consumeAll() to exit
                    queue.close(false, false);  // graceful close, don't notify parent yet
                    LOGGER.debug("Closed queue for task {} to allow consumption completion", taskId);

                    // Step 3: Wait for consumption to complete (now that queue is closed)
                    if (etai.consumptionFuture() != null) {
                        etai.consumptionFuture().get(consumptionCompletionTimeoutSeconds, SECONDS);
                        LOGGER.debug("Consumption completed for task {}", taskId);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    String msg = String.format("Error waiting for task %s completion", taskId);
                    LOGGER.warn(msg, e);
                    throw new InternalError(msg);
                } catch (java.util.concurrent.ExecutionException e) {
                    String msg = String.format("Error during task %s execution", taskId);
                    LOGGER.warn(msg, e.getCause());
                    throw new InternalError(msg);
                } catch (java.util.concurrent.TimeoutException e) {
                    String msg = String.format("Timeout waiting for consumption to complete for task %s", taskId);
                    LOGGER.warn(msg, taskId);
                    throw new InternalError(msg);
                }

                // Step 4: Fetch the final task state from TaskStore (all events have been processed)
                // taskId is guaranteed non-null here (checked earlier)
                String nonNullTaskId = taskId;
                Task updatedTask = taskStore.get(nonNullTaskId);
                if (updatedTask != null) {
                    kind = updatedTask;
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Fetched final task for {} with state {} and {} artifacts",
                            nonNullTaskId, updatedTask.status().state(),
                            updatedTask.artifacts().size());
                    }
                }
            }
            if (kind instanceof Task taskResult && !taskId.equals(taskResult.id())) {
                throw new InternalError("Task ID mismatch in agent response");
            }

            // Send push notification after initial return (for both blocking and non-blocking)
            pushNotificationCallback.run();
        } finally {
            // Remove agent from map immediately to prevent accumulation
            CompletableFuture<Void> agentFuture = runningAgents.remove(taskId);
            LOGGER.debug("Removed agent for task {} from runningAgents in finally block, size after: {}", taskId, runningAgents.size());

            // Track cleanup as background task to avoid blocking Vert.x threads
            // Pass the consumption future to ensure cleanup waits for background consumption to complete
            trackBackgroundTask(cleanupProducer(agentFuture, etai != null ? etai.consumptionFuture() : null, taskId, queue, false));
        }

        LOGGER.debug("Returning: {}", kind);
        return kind;
    }

    @Override
    public Flow.Publisher<StreamingEventKind> onMessageSendStream(
            MessageSendParams params, ServerCallContext context) throws A2AError {
        LOGGER.debug("onMessageSendStream START - task: {}; context: {}; runningAgents: {}; backgroundTasks: {}",
                params.message().taskId(), params.message().contextId(), runningAgents.size(), backgroundTasks.size());
        MessageSendSetup mss = initMessageSend(params, context);

        @Nullable String initialTaskId = mss.requestContext.getTaskId();
        // For streaming, taskId can be null initially (will be set when Task event arrives)
        // Use a temporary ID for queue creation if needed
        String queueTaskId = initialTaskId != null ? initialTaskId : "temp-" + java.util.UUID.randomUUID();

        AtomicReference<@NonNull String> taskId = new AtomicReference<>(queueTaskId);
        @SuppressWarnings("NullAway")
        EventQueue queue = queueManager.createOrTap(taskId.get());
        LOGGER.debug("Created/tapped queue for task {}: {}", taskId.get(), queue);
        ResultAggregator resultAggregator = new ResultAggregator(mss.taskManager, null, executor);

        EnhancedRunnable producerRunnable = registerAndExecuteAgentAsync(queueTaskId, mss.requestContext, queue);

        // Move consumer creation and callback registration outside try block
        // so consumer is available for background consumption on client disconnect
        EventConsumer consumer = new EventConsumer(queue);
        producerRunnable.addDoneCallback(consumer.createAgentRunnableDoneCallback());

        AtomicBoolean backgroundConsumeStarted = new AtomicBoolean(false);

        try {
            Flow.Publisher<EventQueueItem> results = resultAggregator.consumeAndEmit(consumer);

            // First process the items then convert to Event
            Flow.Publisher<EventQueueItem> processed =
                    processor(createTubeConfig(), results, ((errorConsumer, item) -> {
                Event event = item.getEvent();
                if (event instanceof Task createdTask) {
                    if (!Objects.equals(taskId.get(), createdTask.id())) {
                        errorConsumer.accept(new InternalError("Task ID mismatch in agent response"));
                    }

                    // TODO the Python implementation no longer has the following block but removing it causes
                    //  failures here
                    try {
                        queueManager.add(createdTask.id(), queue);
                        taskId.set(createdTask.id());
                    } catch (TaskQueueExistsException e) {
                        // TODO Log
                    }
                    if (pushConfigStore != null &&
                            params.configuration() != null &&
                            params.configuration().pushNotificationConfig() != null) {

                        pushConfigStore.setInfo(
                                createdTask.id(),
                                params.configuration().pushNotificationConfig());
                    }

                }
                String currentTaskId = taskId.get();
                if (pushSender != null && currentTaskId != null) {
                    EventKind latest = resultAggregator.getCurrentResult();
                    if (latest instanceof Task latestTask) {
                        pushSender.sendNotification(latestTask);
                    }
                }

                return true;
            }));

            // Then convert EventQueueItem -> Event
            Flow.Publisher<Event> eventPublisher = convertingProcessor(processed, EventQueueItem::getEvent);

            Flow.Publisher<StreamingEventKind> finalPublisher = convertingProcessor(eventPublisher, event -> (StreamingEventKind) event);

            // Wrap publisher to detect client disconnect and continue background consumption
            return subscriber -> {
                String currentTaskId = taskId.get();
                LOGGER.debug("Creating subscription wrapper for task {}", currentTaskId);
                finalPublisher.subscribe(new Flow.Subscriber<StreamingEventKind>() {
                    private Flow.@Nullable Subscription subscription;

                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        LOGGER.debug("onSubscribe called for task {}", taskId.get());
                        this.subscription = subscription;
                        // Wrap subscription to detect cancellation
                        subscriber.onSubscribe(new Flow.Subscription() {
                            @Override
                            public void request(long n) {
                                LOGGER.debug("Subscription.request({}) for task {}", n, taskId.get());
                                subscription.request(n);
                            }

                            @Override
                            public void cancel() {
                                LOGGER.debug("Client cancelled subscription for task {}, starting background consumption", taskId.get());
                                startBackgroundConsumption();
                                subscription.cancel();
                            }
                        });
                    }

                    @Override
                    public void onNext(StreamingEventKind item) {
                        LOGGER.debug("onNext: {} for task {}", item.getClass().getSimpleName(), taskId.get());
                        subscriber.onNext(item);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        LOGGER.error("onError for task {}", taskId.get(), throwable);
                        subscriber.onError(throwable);
                    }

                    @Override
                    public void onComplete() {
                        LOGGER.debug("onComplete for task {}", taskId.get());
                        try {
                            subscriber.onComplete();
                        } catch (IllegalStateException e) {
                            // Client already disconnected and response closed - this is expected
                            // for streaming responses where client disconnect triggers background
                            // consumption. Log and ignore.
                            if (e.getMessage() != null && e.getMessage().contains("Response has already been written")) {
                                LOGGER.debug("Client disconnected before onComplete, response already closed for task {}", taskId.get());
                            } else {
                                throw e;
                            }
                        }
                    }

                    private void startBackgroundConsumption() {
                        if (backgroundConsumeStarted.compareAndSet(false, true)) {
                            LOGGER.debug("Starting background consumption for task {}", taskId.get());
                            // Client disconnected: continue consuming and persisting events in background
                            CompletableFuture<Void> bgTask = CompletableFuture.runAsync(() -> {
                                try {
                                    LOGGER.debug("Background consumption thread started for task {}", taskId.get());
                                    resultAggregator.consumeAll(consumer);
                                    LOGGER.debug("Background consumption completed for task {}", taskId.get());
                                } catch (Exception e) {
                                    LOGGER.error("Error during background consumption for task {}", taskId.get(), e);
                                }
                            }, executor);
                            trackBackgroundTask(bgTask);
                        } else {
                            LOGGER.debug("Background consumption already started for task {}", taskId.get());
                        }
                    }
                });
            };
        } finally {
            LOGGER.debug("onMessageSendStream FINALLY - task: {}; runningAgents: {}; backgroundTasks: {}",
                    taskId.get(), runningAgents.size(), backgroundTasks.size());

            // Remove agent from map immediately to prevent accumulation
            CompletableFuture<Void> agentFuture = runningAgents.remove(taskId.get());
            LOGGER.debug("Removed agent for task {} from runningAgents in finally block, size after: {}", taskId.get(), runningAgents.size());

            trackBackgroundTask(cleanupProducer(agentFuture, null, Objects.requireNonNull(taskId.get()), queue, true));
        }
    }

    @Override
    public TaskPushNotificationConfig onSetTaskPushNotificationConfig(
            TaskPushNotificationConfig params, ServerCallContext context) throws A2AError {
        if (pushConfigStore == null) {
            throw new UnsupportedOperationError();
        }
        Task task = taskStore.get(params.taskId());
        if (task == null) {
            throw new TaskNotFoundError();
        }

        PushNotificationConfig pushNotificationConfig = pushConfigStore.setInfo(params.taskId(), params.pushNotificationConfig());
        return new TaskPushNotificationConfig(params.taskId(), pushNotificationConfig, params.tenant());
    }

    @Override
    public TaskPushNotificationConfig onGetTaskPushNotificationConfig(
            GetTaskPushNotificationConfigParams params, ServerCallContext context) throws A2AError {
        if (pushConfigStore == null) {
            throw new UnsupportedOperationError();
        }
        Task task = taskStore.get(params.id());
        if (task == null) {
            throw new TaskNotFoundError();
        }

        ListTaskPushNotificationConfigResult listTaskPushNotificationConfigResult = pushConfigStore.getInfo(new ListTaskPushNotificationConfigParams(params.id()));
        if (listTaskPushNotificationConfigResult == null || listTaskPushNotificationConfigResult.isEmpty()) {
            throw new InternalError("No push notification config found");
        }

        @Nullable String configId = params.pushNotificationConfigId();
        return new TaskPushNotificationConfig(params.id(), getPushNotificationConfig(listTaskPushNotificationConfigResult, configId), params.tenant());
    }

    private PushNotificationConfig getPushNotificationConfig(ListTaskPushNotificationConfigResult notificationConfigList,
                                                             @Nullable String configId) {
        if (configId != null) {
            for (TaskPushNotificationConfig notificationConfig : notificationConfigList.configs()) {
                if (configId.equals(notificationConfig.pushNotificationConfig().id())) {
                    return notificationConfig.pushNotificationConfig();
                }
            }
        }
        return notificationConfigList.configs().get(0).pushNotificationConfig();
    }

    @Override
    public Flow.Publisher<StreamingEventKind> onResubscribeToTask(
            TaskIdParams params, ServerCallContext context) throws A2AError {
        LOGGER.debug("onResubscribeToTask - taskId: {}", params.id());
        Task task = taskStore.get(params.id());
        if (task == null) {
            throw new TaskNotFoundError();
        }

        TaskManager taskManager = new TaskManager(task.id(), task.contextId(), taskStore, null);
        ResultAggregator resultAggregator = new ResultAggregator(taskManager, null, executor);
        EventQueue queue = queueManager.tap(task.id());
        LOGGER.debug("onResubscribeToTask - tapped queue: {}", queue != null ? System.identityHashCode(queue) : "null");

        if (queue == null) {
            // If task is in final state, queue legitimately doesn't exist anymore
            if (task.status().state().isFinal()) {
                throw new TaskNotFoundError();
            }
            // For non-final tasks, recreate the queue so client can receive future events
            // (Note: historical events from before queue closed are not available)
            LOGGER.debug("Queue not found for active task {}, creating new queue for future events", task.id());
            queue = queueManager.createOrTap(task.id());
        }

        EventConsumer consumer = new EventConsumer(queue);
        Flow.Publisher<EventQueueItem> results = resultAggregator.consumeAndEmit(consumer);
        LOGGER.debug("onResubscribeToTask - returning publisher for taskId: {}", params.id());
        return convertingProcessor(results, item -> (StreamingEventKind) item.getEvent());
    }

    @Override
    public ListTaskPushNotificationConfigResult onListTaskPushNotificationConfig(
            ListTaskPushNotificationConfigParams params, ServerCallContext context) throws A2AError {
        if (pushConfigStore == null) {
            throw new UnsupportedOperationError();
        }
        Task task = taskStore.get(params.id());
        if (task == null) {
            throw new TaskNotFoundError();
        }
        return pushConfigStore.getInfo(params);
    }

    @Override
    public void onDeleteTaskPushNotificationConfig(
            DeleteTaskPushNotificationConfigParams params, ServerCallContext context) {
        if (pushConfigStore == null) {
            throw new UnsupportedOperationError();
        }

        Task task = taskStore.get(params.id());
        if (task == null) {
            throw new TaskNotFoundError();
        }

        pushConfigStore.deleteInfo(params.id(), params.pushNotificationConfigId());
    }

    private boolean shouldAddPushInfo(MessageSendParams params) {
        return pushConfigStore != null && params.configuration() != null && params.configuration().pushNotificationConfig() != null;
    }

    /**
     * Register and execute the agent asynchronously in the agent-executor thread pool.
     *
     * Queue Lifecycle Architecture:
     * - Agent-executor thread: Executes agent and enqueues events, returns immediately
     * - Vert.x worker thread (consumer): Polls queue, processes events, closes queue on final event
     * - Background cleanup: Manages ChildQueue/MainQueue lifecycle after agent completes
     *
     * This design avoids blocking agent-executor threads waiting for consumer polling to start,
     * eliminating cascading delays when Vert.x worker threads are busy.
     */
    private EnhancedRunnable registerAndExecuteAgentAsync(String taskId, RequestContext requestContext, EventQueue queue) {
        LOGGER.debug("Registering agent execution for task {}, runningAgents.size() before: {}", taskId, runningAgents.size());
        logThreadStats("AGENT START");
        EnhancedRunnable runnable = new EnhancedRunnable() {
            @Override
            public void run() {
                LOGGER.debug("Agent execution starting for task {}", taskId);
                agentExecutor.execute(requestContext, queue);
                LOGGER.debug("Agent execution completed for task {}", taskId);
                // No longer wait for queue poller to start - the consumer (which is guaranteed
                // to be running on the Vert.x worker thread) will handle queue lifecycle.
                // This avoids blocking agent-executor threads waiting for worker threads.
            }
        };

        CompletableFuture<Void> cf = CompletableFuture.runAsync(runnable, executor)
                .whenComplete((v, err) -> {
                    if (err != null) {
                        LOGGER.error("Agent execution failed for task {}", taskId, err);
                        runnable.setError(err);
                        // Don't close queue here - let the consumer handle it via error callback
                        // This ensures the consumer (which may not have started polling yet) gets the error
                    }
                    // Queue lifecycle is now managed entirely by EventConsumer.consumeAll()
                    // which closes the queue on final events. No need to close here.
                    logThreadStats("AGENT COMPLETE END");
                    runnable.invokeDoneCallbacks();
                });
        runningAgents.put(taskId, cf);
        LOGGER.debug("Registered agent for task {}, runningAgents.size() after: {}", taskId, runningAgents.size());
        return runnable;
    }

    private void trackBackgroundTask(CompletableFuture<Void> task) {
        backgroundTasks.add(task);
        LOGGER.debug("Tracking background task (total: {}): {}", backgroundTasks.size(), task);

        task.whenComplete((result, throwable) -> {
            try {
                if (throwable != null) {
                    // Unwrap CompletionException to check for CancellationException
                    Throwable cause = throwable;
                    if (throwable instanceof java.util.concurrent.CompletionException && throwable.getCause() != null) {
                        cause = throwable.getCause();
                    }

                    if (cause instanceof java.util.concurrent.CancellationException) {
                        LOGGER.debug("Background task cancelled: {}", task);
                    } else {
                        LOGGER.error("Background task failed", throwable);
                    }
                }
            } finally {
                backgroundTasks.remove(task);
                LOGGER.debug("Removed background task (remaining: {}): {}", backgroundTasks.size(), task);
            }
        });
    }

    /**
     * Wait for all background tasks to complete.
     * Useful for testing to ensure cleanup completes before assertions.
     *
     * @return CompletableFuture that completes when all background tasks finish
     */
    public CompletableFuture<Void> waitForBackgroundTasks() {
        CompletableFuture<?>[] tasks = backgroundTasks.toArray(new CompletableFuture[0]);
        if (tasks.length == 0) {
            return CompletableFuture.completedFuture(null);
        }
        LOGGER.debug("Waiting for {} background tasks to complete", tasks.length);
        return CompletableFuture.allOf(tasks);
    }

    private CompletableFuture<Void> cleanupProducer(@Nullable CompletableFuture<Void> agentFuture, @Nullable CompletableFuture<Void> consumptionFuture, String taskId, EventQueue queue, boolean isStreaming) {
        LOGGER.debug("Starting cleanup for task {} (streaming={})", taskId, isStreaming);
        logThreadStats("CLEANUP START");

        if (agentFuture == null) {
            LOGGER.debug("No running agent found for task {}, cleanup complete", taskId);
            return CompletableFuture.completedFuture(null);
        }

        // Wait for BOTH agent AND consumption to complete before cleanup
        // This ensures TaskStore is fully updated before we check task finalization
        CompletableFuture<Void> bothComplete = agentFuture;
        if (consumptionFuture != null) {
            bothComplete = CompletableFuture.allOf(agentFuture, consumptionFuture);
            LOGGER.debug("Cleanup will wait for both agent and consumption to complete for task {}", taskId);
        }

        return bothComplete.whenComplete((v, t) -> {
            if (t != null) {
                LOGGER.debug("Agent/consumption completed with error for task {}", taskId, t);
            } else {
                LOGGER.debug("Agent and consumption both completed successfully for task {}", taskId);
            }

            // Always close the ChildQueue and notify the parent MainQueue
            // The parent will close itself when all children are closed (childClosing logic)
            // This ensures proper cleanup and removal from QueueManager map
            LOGGER.debug("{} call, closing ChildQueue for task {} (immediate=false, notifyParent=true)",
                    isStreaming ? "Streaming" : "Non-streaming", taskId);

            // Always notify parent so MainQueue can clean up when last child closes
            queue.close(false, true);

            // For replicated environments, the poison pill is now sent via CDI events
            // When JpaDatabaseTaskStore.save() persists a final task, it fires TaskFinalizedEvent
            // ReplicatedQueueManager.onTaskFinalized() observes AFTER_SUCCESS and sends poison pill
            // This guarantees the transaction is committed before the poison pill is sent
            LOGGER.debug("Queue cleanup completed for task {}", taskId);

            logThreadStats("CLEANUP END");
        });
    }

    private MessageSendSetup initMessageSend(MessageSendParams params, ServerCallContext context) {
        TaskManager taskManager = new TaskManager(
                params.message().taskId(),
                params.message().contextId(),
                taskStore,
                params.message());

        Task task = taskManager.getTask();
        if (task != null) {
            LOGGER.debug("Found task updating with message {}", params.message());
            task = taskManager.updateWithMessage(params.message(), task);

            if (shouldAddPushInfo(params)) {
                LOGGER.debug("Adding push info");
                pushConfigStore.setInfo(task.id(), params.configuration().pushNotificationConfig());
            }
        }

        RequestContext requestContext = requestContextBuilder.get()
                .setParams(params)
                .setTaskId(task == null ? null : task.id())
                .setContextId(params.message().contextId())
                .setTask(task)
                .setCallContext(context)
                .build();
        return new MessageSendSetup(taskManager, task, requestContext);
    }

    private void sendPushNotification(String taskId, ResultAggregator resultAggregator) {
        if (pushSender != null) {
            EventKind latest = resultAggregator.getCurrentResult();
            if (latest instanceof Task latestTask) {
                pushSender.sendNotification(latestTask);
            }
        }
    }

    /**
     * Log current thread and resource statistics for debugging.
     * Only logs when DEBUG level is enabled. Call this from debugger or add strategic
     * calls during investigation. In production with INFO logging, this is a no-op.
     */
    @SuppressWarnings("unused")  // Used for debugging
    private void logThreadStats(String label) {
        // Early return if debug logging is not enabled to avoid overhead
        if (!LOGGER.isDebugEnabled()) {
            return;
        }

        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        while (rootGroup.getParent() != null) {
            rootGroup = rootGroup.getParent();
        }
        int activeThreads = rootGroup.activeCount();

        LOGGER.debug("=== THREAD STATS: {} ===", label);
        LOGGER.debug("Active threads: {}", activeThreads);
        LOGGER.debug("Running agents: {}", runningAgents.size());
        LOGGER.debug("Background tasks: {}", backgroundTasks.size());
        LOGGER.debug("Queue manager active queues: {}", queueManager.getClass().getSimpleName());

        // List running agents
        if (!runningAgents.isEmpty()) {
            LOGGER.debug("Running agent tasks:");
            runningAgents.forEach((taskId, future) ->
                LOGGER.debug("  - Task {}: {}", taskId, future.isDone() ? "DONE" : "RUNNING")
            );
        }

        // List background tasks
        if (!backgroundTasks.isEmpty()) {
            LOGGER.debug("Background tasks:");
            backgroundTasks.forEach(task ->
                LOGGER.debug("  - {}: {}", task, task.isDone() ? "DONE" : "RUNNING")
            );
        }
        LOGGER.debug("=== END THREAD STATS ===");
    }

    private record MessageSendSetup(TaskManager taskManager, @Nullable Task task, RequestContext requestContext) {}
}
