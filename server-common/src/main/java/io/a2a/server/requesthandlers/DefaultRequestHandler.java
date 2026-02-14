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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeoutException;
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
import io.a2a.server.config.A2AConfigProvider;
import io.a2a.server.events.EnhancedRunnable;
import io.a2a.server.events.EventConsumer;
import io.a2a.server.events.EventQueue;
import io.a2a.server.events.EventQueueItem;
import io.a2a.server.events.MainEventBusProcessor;
import io.a2a.server.events.QueueManager;
import io.a2a.server.tasks.AgentEmitter;
import io.a2a.server.tasks.PushNotificationConfigStore;
import io.a2a.server.tasks.PushNotificationSender;
import io.a2a.server.tasks.ResultAggregator;
import io.a2a.server.tasks.TaskManager;
import io.a2a.server.tasks.TaskStore;
import io.a2a.server.util.async.EventConsumerExecutorProducer.EventConsumerExecutor;
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
import io.a2a.spec.TaskStatusUpdateEvent;
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
 *   <li>Execute {@link AgentExecutor#execute(RequestContext, AgentEmitter)} asynchronously in background thread pool</li>
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
 *   <li>Execute {@link AgentExecutor#execute(RequestContext, AgentEmitter)} asynchronously</li>
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
 * </ul>
 *
 * <h2>Threading Model</h2>
 * <ul>
 *   <li><b>Vert.x worker threads:</b> Execute request handler methods (onMessageSend, etc.)</li>
 *   <li><b>Agent-executor pool (@Internal):</b> Execute {@link AgentExecutor#execute(RequestContext, AgentEmitter)}</li>
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

    /**
     * Separate logger for thread statistics diagnostic logging.
     * This allows independent control of verbose thread pool monitoring without affecting
     * general request handler logging. Enable with: logging.level.io.a2a.server.diagnostics.ThreadStats=DEBUG
     */
    private static final Logger THREAD_STATS_LOGGER = LoggerFactory.getLogger("io.a2a.server.diagnostics.ThreadStats");

    private static final String A2A_BLOCKING_AGENT_TIMEOUT_SECONDS = "a2a.blocking.agent.timeout.seconds";
    private static final String A2A_BLOCKING_CONSUMPTION_TIMEOUT_SECONDS = "a2a.blocking.consumption.timeout.seconds";

    @Inject
    A2AConfigProvider configProvider;

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
    private MainEventBusProcessor mainEventBusProcessor;
    private Supplier<RequestContext.Builder> requestContextBuilder;

    private final ConcurrentMap<String, CompletableFuture<Void>> runningAgents = new ConcurrentHashMap<>();


    private Executor executor;
    private Executor eventConsumerExecutor;

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
        this.mainEventBusProcessor = null;
        this.requestContextBuilder = null;
        this.executor = null;
        this.eventConsumerExecutor = null;
    }

    @Inject
    public DefaultRequestHandler(AgentExecutor agentExecutor, TaskStore taskStore,
                                 QueueManager queueManager, PushNotificationConfigStore pushConfigStore,
                                 MainEventBusProcessor mainEventBusProcessor,
                                 @Internal Executor executor,
                                 @EventConsumerExecutor Executor eventConsumerExecutor) {
        this.agentExecutor = agentExecutor;
        this.taskStore = taskStore;
        this.queueManager = queueManager;
        this.pushConfigStore = pushConfigStore;
        this.mainEventBusProcessor = mainEventBusProcessor;
        this.executor = executor;
        this.eventConsumerExecutor = eventConsumerExecutor;
        // TODO In Python this is also a constructor parameter defaulting to this SimpleRequestContextBuilder
        //  implementation if the parameter is null. Skip that for now, since otherwise I get CDI errors, and
        //  I am unsure about the correct scope.
        //  Also reworked to make a Supplier since otherwise the builder gets polluted with wrong tasks
        this.requestContextBuilder = () -> new SimpleRequestContextBuilder(taskStore, false);
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
                         MainEventBusProcessor mainEventBusProcessor,
                         Executor executor, Executor eventConsumerExecutor) {
        DefaultRequestHandler handler =
                new DefaultRequestHandler(agentExecutor, taskStore, queueManager, pushConfigStore,
                        mainEventBusProcessor, executor, eventConsumerExecutor);
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

        ResultAggregator resultAggregator = new ResultAggregator(taskManager, null, executor, eventConsumerExecutor);

        EventQueue queue = queueManager.createOrTap(task.id());
        RequestContext cancelRequestContext = requestContextBuilder.get()
                .setTaskId(task.id())
                .setContextId(task.contextId())
                .setTask(task)
                .setServerCallContext(context)
                .build();
        AgentEmitter emitter = new AgentEmitter(cancelRequestContext, queue);
        try {
            agentExecutor.cancel(cancelRequestContext, emitter);
        } catch (TaskNotCancelableError e) {
            // Expected error - log at INFO level
            LOGGER.info("Task {} is not cancelable", task.id());
            throw e;
        } catch (A2AError e) {
            // Other A2A errors - log at WARN level with stack trace
            LOGGER.warn("Agent cancellation threw A2AError for task {}: {} - {}", 
                task.id(), e.getClass().getSimpleName(), e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            // Unexpected errors - log at ERROR level
            LOGGER.error("Agent cancellation threw unexpected exception for task {}", task.id(), e);
            throw new io.a2a.spec.InternalError("Agent cancellation failed: " + e.getMessage());
        }

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
    @SuppressWarnings("NullAway")
    public EventKind onMessageSend(MessageSendParams params, ServerCallContext context) throws A2AError {
        LOGGER.debug("onMessageSend - task: {}; context {}", params.message().taskId(), params.message().contextId());

        // Build MessageSendSetup which creates RequestContext with real taskId (auto-generated if needed)
        MessageSendSetup mss = initMessageSend(params, context);

        // Use the taskId from RequestContext for queue management (no temp ID needed!)
        // RequestContext.build() guarantees taskId is non-null via checkOrGenerateTaskId()
        String queueTaskId = java.util.Objects.requireNonNull(
                mss.requestContext.getTaskId(), "TaskId must be non-null after RequestContext.build()");
        LOGGER.debug("Queue taskId: {}", queueTaskId);

        // Create queue with real taskId (no tempId parameter needed)
        EventQueue queue = queueManager.createOrTap(queueTaskId);
        final java.util.concurrent.atomic.AtomicReference<@NonNull String> taskId = new java.util.concurrent.atomic.AtomicReference<>(queueTaskId);
        ResultAggregator resultAggregator = new ResultAggregator(mss.taskManager, null, executor, eventConsumerExecutor);

        // Default to blocking=false per A2A spec (return after task creation)
        boolean blocking = params.configuration() != null && Boolean.TRUE.equals(params.configuration().blocking());

        // Log blocking behavior from client request
        if (params.configuration() != null && params.configuration().blocking() != null) {
            LOGGER.debug("DefaultRequestHandler: Client requested blocking={} for task {}",
                params.configuration().blocking(), taskId.get());
        } else if (params.configuration() != null) {
            LOGGER.debug("DefaultRequestHandler: Client sent configuration but blocking=null, using default blocking={} for task {}", blocking, taskId.get());
        } else {
            LOGGER.debug("DefaultRequestHandler: Client sent no configuration, using default blocking={} for task {}", blocking, taskId.get());
        }
        LOGGER.debug("DefaultRequestHandler: Final blocking decision: {} for task {}", blocking, taskId.get());

        boolean interruptedOrNonBlocking = false;

        // Create consumer BEFORE starting agent - callback is registered inside registerAndExecuteAgentAsync
        EventConsumer consumer = new EventConsumer(queue);

        EnhancedRunnable producerRunnable = registerAndExecuteAgentAsync(queueTaskId, mss.requestContext, queue, consumer.createAgentRunnableDoneCallback());

        ResultAggregator.EventTypeAndInterrupt etai = null;
        EventKind kind = null;  // Declare outside try block so it's in scope for return
        try {

            // Get agent future before consuming (for blocking calls to wait for agent completion)
            CompletableFuture<Void> agentFuture = runningAgents.get(queueTaskId);
            etai = resultAggregator.consumeAndBreakOnInterrupt(consumer, blocking);

            if (etai == null) {
                LOGGER.debug("No result, throwing InternalError");
                throw new InternalError("No result");
            }
            interruptedOrNonBlocking = etai.interrupted();
            LOGGER.debug("DefaultRequestHandler: interruptedOrNonBlocking={} (blocking={}, eventType={})",
                interruptedOrNonBlocking, blocking, kind != null ? kind.getClass().getSimpleName() : null);

            // For blocking calls that were interrupted (returned on first event),
            // wait for agent execution and event processing BEFORE returning to client.
            // This ensures the returned Task has all artifacts and current state.
            // We do this HERE (not in ResultAggregator) to avoid blocking Vert.x worker threads
            // during the consumption loop itself.
            kind = etai.eventType();

            // No ID switching needed - agent uses context.getTaskId() which is the same as queue key

            // Store push notification config for newly created tasks (mirrors streaming logic)
            // Only for NEW tasks - existing tasks are handled by initMessageSend()
            if (mss.task() == null && kind instanceof Task createdTask && shouldAddPushInfo(params)) {
                LOGGER.debug("Storing push notification config for new task {} (original taskId from params: {})",
                        createdTask.id(), params.message().taskId());
                pushConfigStore.setInfo(createdTask.id(), params.configuration().pushNotificationConfig());
            }

            if (blocking && interruptedOrNonBlocking) {
                // For blocking calls: ensure all consumed events are persisted to TaskStore before returning
                // Order of operations is critical to avoid circular dependency and race conditions:
                // 1. Wait for agent to finish enqueueing events (or timeout)
                // 2. Close the queue to signal consumption can complete
                // 3. Wait for consumption to finish processing events
                // 4. (Implicit) MainEventBusProcessor persistence guarantee via consumption completion
                // 5. Fetch current task state from TaskStore (includes all consumed & persisted events)
                LOGGER.debug("DefaultRequestHandler: Entering blocking fire-and-forget handling for task {}", taskId.get());

                try {
                    // Step 1: Wait for agent to finish (with configurable timeout)
                    if (agentFuture != null) {
                        try {
                            agentFuture.get(agentCompletionTimeoutSeconds, SECONDS);
                            LOGGER.debug("DefaultRequestHandler: Step 1 - Agent completed for task {}", taskId.get());
                        } catch (java.util.concurrent.TimeoutException e) {
                            // Agent still running after timeout - that's fine, events already being processed
                            LOGGER.debug("DefaultRequestHandler: Step 1 - Agent still running for task {} after {}s timeout",
                                taskId.get(), agentCompletionTimeoutSeconds);
                        }
                    }

                    // Step 2: Close the queue to signal consumption can complete
                    // For fire-and-forget tasks, there's no final event, so we need to close the queue
                    // This allows EventConsumer.consumeAll() to exit
                    queue.close(false, false);  // graceful close, don't notify parent yet
                    LOGGER.debug("DefaultRequestHandler: Step 2 - Closed queue for task {} to allow consumption completion", taskId.get());

                    // Step 3: Wait for consumption to complete (now that queue is closed)
                    if (etai.consumptionFuture() != null) {
                        etai.consumptionFuture().get(consumptionCompletionTimeoutSeconds, SECONDS);
                        LOGGER.debug("DefaultRequestHandler: Step 3 - Consumption completed for task {}", taskId.get());
                    }

                    // Step 4: Implicit guarantee of persistence via consumption completion
                    // We do NOT add an explicit wait for MainEventBusProcessor here because:
                    // 1. MainEventBusProcessor persists BEFORE distributing to ChildQueues
                    // 2. Step 3 (consumption completion) already guarantees all consumed events are persisted
                    // 3. Adding another explicit synchronization point would require exposing
                    //    MainEventBusProcessor internals and blocking event loop threads
                    //
                    // Note: For fire-and-forget tasks, if the agent is still running after Step 1 timeout,
                    // it may enqueue additional events. These will be persisted asynchronously but won't
                    // be included in the task state returned to the client (already consumed in Step 3).

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    String msg = String.format("Error waiting for task %s completion", taskId.get());
                    LOGGER.warn(msg, e);
                    throw new InternalError(msg);
                } catch (java.util.concurrent.ExecutionException e) {
                    String msg = String.format("Error during task %s execution", taskId.get());
                    LOGGER.warn(msg, e.getCause());
                    throw new InternalError(msg);
                } catch (TimeoutException e) {
                    // Timeout from consumption future.get() - different from finalization timeout
                    String msg = String.format("Timeout waiting for task %s consumption", taskId.get());
                    LOGGER.warn(msg, e);
                    throw new InternalError(msg);
                }

                // Step 5: Fetch the current task state from TaskStore
                // All events consumed in Step 3 are guaranteed persisted (MainEventBusProcessor
                // ordering: persist → distribute → consume). This returns the persisted state
                // including all consumed events and artifacts.
                String nonNullTaskId = Objects.requireNonNull(taskId.get(), "taskId cannot be null");
                Task updatedTask = taskStore.get(nonNullTaskId);
                if (updatedTask != null) {
                    kind = updatedTask;
                    LOGGER.debug("DefaultRequestHandler: Step 5 - Fetched current task for {} with state {} and {} artifacts",
                        taskId.get(), updatedTask.status().state(),
                        updatedTask.artifacts().size());
                } else {
                    LOGGER.warn("DefaultRequestHandler: Step 5 - Task {} not found in TaskStore!", taskId.get());
                }
            }
            String finalTaskId = Objects.requireNonNull(taskId.get(), "taskId cannot be null");
            if (kind instanceof Task taskResult && !finalTaskId.equals(taskResult.id())) {
                throw new InternalError("Task ID mismatch in agent response");
            }
        } finally {
            // For non-blocking calls: close ChildQueue IMMEDIATELY to free EventConsumer thread
            // CRITICAL: Must use immediate=true to clear the local queue, otherwise EventConsumer
            // continues polling until queue drains naturally, holding executor thread.
            // Immediate close clears pending events and triggers EventQueueClosedException on next poll.
            // Events continue flowing through MainQueue → MainEventBus → TaskStore.
            if (!blocking && etai != null && etai.interrupted()) {
                LOGGER.debug("DefaultRequestHandler: Non-blocking call in finally - closing ChildQueue IMMEDIATELY for task {} to free EventConsumer", taskId.get());
                queue.close(true);  // immediate=true: clear queue and free EventConsumer
            }

            // Remove agent from map immediately to prevent accumulation
            CompletableFuture<Void> agentFuture = runningAgents.remove(queueTaskId);
            String cleanupTaskId = Objects.requireNonNull(taskId.get(), "taskId cannot be null");
            LOGGER.debug("Removed agent for task {} from runningAgents in finally block, size after: {}", cleanupTaskId, runningAgents.size());

            // Cleanup as background task to avoid blocking Vert.x threads
            // Pass the consumption future to ensure cleanup waits for background consumption to complete
            cleanupProducer(agentFuture, etai != null ? etai.consumptionFuture() : null, cleanupTaskId, queue, false)
                    .whenComplete((res, err) -> {
                        if (err != null) {
                            LOGGER.error("Error during async cleanup for task {}", taskId.get(), err);
                        }
                    });
        }

        LOGGER.debug("Returning: {}", kind);
        return kind;
    }

    @Override
    @SuppressWarnings("NullAway")
    public Flow.Publisher<StreamingEventKind> onMessageSendStream(
            MessageSendParams params, ServerCallContext context) throws A2AError {
        LOGGER.debug("onMessageSendStream START - task: {}; context: {}; runningAgents: {}",
                params.message().taskId(), params.message().contextId(), runningAgents.size());

        // Build MessageSendSetup which creates RequestContext with real taskId (auto-generated if needed)
        MessageSendSetup mss = initMessageSend(params, context);

        // Use the taskId from RequestContext for queue management (no temp ID needed!)
        // RequestContext.build() guarantees taskId is non-null via checkOrGenerateTaskId()
        String queueTaskId = java.util.Objects.requireNonNull(
                mss.requestContext.getTaskId(), "TaskId must be non-null after RequestContext.build()");
        final AtomicReference<@NonNull String> taskId = new AtomicReference<>(queueTaskId);

        // Create queue with real taskId (no tempId parameter needed)
        EventQueue queue = queueManager.createOrTap(queueTaskId);
        LOGGER.debug("Created/tapped queue for task {}: {}", taskId.get(), queue);

        // Store push notification config SYNCHRONOUSLY for new tasks before agent starts
        // This ensures config is available when MainEventBusProcessor sends push notifications
        // For existing tasks, config is stored in initMessageSend()
        if (mss.task() == null && shouldAddPushInfo(params)) {
            // Satisfy Nullaway
            Objects.requireNonNull(taskId.get(), "taskId was null");
            LOGGER.debug("Storing push notification config for new streaming task {} EARLY (original taskId from params: {})",
                    taskId.get(), params.message().taskId());
            pushConfigStore.setInfo(taskId.get(), params.configuration().pushNotificationConfig());
        }

        ResultAggregator resultAggregator = new ResultAggregator(mss.taskManager, null, executor, eventConsumerExecutor);

        // Create consumer BEFORE starting agent - callback is registered inside registerAndExecuteAgentAsync
        EventConsumer consumer = new EventConsumer(queue);

        EnhancedRunnable producerRunnable = registerAndExecuteAgentAsync(queueTaskId, mss.requestContext, queue, consumer.createAgentRunnableDoneCallback());

        // Store cancel callback in context for closeHandler to access
        // When client disconnects, closeHandler can call this to stop EventConsumer polling loop
        context.setEventConsumerCancelCallback(consumer::cancel);

        try {
            Flow.Publisher<EventQueueItem> results = resultAggregator.consumeAndEmit(consumer);

            // First process the items then convert to Event
            Flow.Publisher<EventQueueItem> processed =
                    processor(createTubeConfig(), results, ((errorConsumer, item) -> {
                Event event = item.getEvent();
                if (event instanceof Task createdTask) {
                    // Verify task ID matches (should always match now - agent uses context.getTaskId())
                    String currentId = Objects.requireNonNull(taskId.get(), "taskId cannot be null");
                    if (!currentId.equals(createdTask.id())) {
                        errorConsumer.accept(new InternalError("Task ID mismatch: expected " + currentId +
                                " but got " + createdTask.id()));
                    }
                }
                return true;
            }));

            // Then convert EventQueueItem -> Event
            Flow.Publisher<Event> eventPublisher = convertingProcessor(processed, EventQueueItem::getEvent);

            Flow.Publisher<StreamingEventKind> finalPublisher = convertingProcessor(eventPublisher, event -> (StreamingEventKind) event);

            // Wrap publisher to detect client disconnect and immediately close ChildQueue
            // This prevents ChildQueue backpressure from blocking MainEventBusProcessor
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
                                LOGGER.debug("Client cancelled subscription for task {}, closing ChildQueue immediately", taskId.get());
                                // Close ChildQueue immediately to prevent backpressure
                                // (clears queue and releases semaphore permits)
                                queue.close(true);  // immediate=true
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
                            // for streaming responses where client disconnect closes ChildQueue.
                            // Log and ignore.
                            if (e.getMessage() != null && e.getMessage().contains("Response has already been written")) {
                                LOGGER.debug("Client disconnected before onComplete, response already closed for task {}", taskId.get());
                            } else {
                                throw e;
                            }
                        }
                    }
                });
            };
        } finally {
            // Needed to satisfy Nullaway
            String idOfTask = taskId.get();
            if (idOfTask != null) {
                LOGGER.debug("onMessageSendStream FINALLY - task: {}; runningAgents: {}",
                        idOfTask, runningAgents.size());

                // Remove agent from map immediately to prevent accumulation
                CompletableFuture<Void> agentFuture = runningAgents.remove(idOfTask);
                LOGGER.debug("Removed agent for task {} from runningAgents in finally block, size after: {}", taskId.get(), runningAgents.size());

                cleanupProducer(agentFuture, null, idOfTask, queue, true)
                        .whenComplete((res, err) -> {
                            if (err != null) {
                                LOGGER.error("Error during async cleanup for streaming task {}", taskId.get(), err);
                            }
                        });
            }
        }
    }

    @Override
    public TaskPushNotificationConfig onCreateTaskPushNotificationConfig(
            TaskPushNotificationConfig params, ServerCallContext context) throws A2AError {
        if (pushConfigStore == null) {
            throw new UnsupportedOperationError();
        }
        Task task = taskStore.get(params.taskId());
        if (task == null) {
            throw new TaskNotFoundError();
        }

        PushNotificationConfig pushNotificationConfig = pushConfigStore.setInfo(params.taskId(), params.config());
        return new TaskPushNotificationConfig(params.taskId(), pushNotificationConfig, params.tenant());
    }

    @Override
    public TaskPushNotificationConfig onGetTaskPushNotificationConfig(
            GetTaskPushNotificationConfigParams params, ServerCallContext context) throws A2AError {
        if (pushConfigStore == null) {
            throw new UnsupportedOperationError();
        }
        Task task = taskStore.get(params.taskId());
        if (task == null) {
            throw new TaskNotFoundError();
        }

        ListTaskPushNotificationConfigResult listTaskPushNotificationConfigResult = pushConfigStore.getInfo(new ListTaskPushNotificationConfigParams(params.taskId()));
        if (listTaskPushNotificationConfigResult == null || listTaskPushNotificationConfigResult.isEmpty()) {
            throw new InternalError("No push notification config found");
        }

        String configId = params.id();
        return new TaskPushNotificationConfig(params.taskId(), getPushNotificationConfig(listTaskPushNotificationConfigResult, configId), params.tenant());
    }

    private PushNotificationConfig getPushNotificationConfig(ListTaskPushNotificationConfigResult notificationConfigList,
                                                             String configId) {
            for (TaskPushNotificationConfig notificationConfig : notificationConfigList.configs()) {
            if (configId.equals(notificationConfig.config().id())) {
                return notificationConfig.config();
            }
        }
        return notificationConfigList.configs().get(0).config();
    }

    @Override
    public Flow.Publisher<StreamingEventKind> onSubscribeToTask(
            TaskIdParams params, ServerCallContext context) throws A2AError {
        LOGGER.debug("onSubscribeToTask - taskId: {}", params.id());
        Task task = taskStore.get(params.id());
        if (task == null) {
            throw new TaskNotFoundError();
        }

        TaskManager taskManager = new TaskManager(task.id(), task.contextId(), taskStore, null);
        ResultAggregator resultAggregator = new ResultAggregator(taskManager, null, executor, eventConsumerExecutor);
        EventQueue queue = queueManager.tap(task.id());
        LOGGER.debug("onSubscribeToTask - tapped queue: {}", queue != null ? System.identityHashCode(queue) : "null");

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

        // Per A2A Protocol Spec 3.1.6 (Subscribe to Task):
        // "The operation MUST return a Task object as the first event in the stream,
        // representing the current state of the task at the time of subscription."
        // Enqueue the current task state directly to this ChildQueue only (already persisted, no need for MainEventBus)
        queue.enqueueEventLocalOnly(task);
        LOGGER.debug("onSubscribeToTask - enqueued current task state as first event for taskId: {}", params.id());

        EventConsumer consumer = new EventConsumer(queue);
        Flow.Publisher<EventQueueItem> results = resultAggregator.consumeAndEmit(consumer);
        LOGGER.debug("onSubscribeToTask - returning publisher for taskId: {}", params.id());
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

        Task task = taskStore.get(params.taskId());
        if (task == null) {
            throw new TaskNotFoundError();
        }

        pushConfigStore.deleteInfo(params.taskId(), params.id());
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
     *
     * @param doneCallback Callback to invoke when agent completes - MUST be added before starting CompletableFuture
     */
    private EnhancedRunnable registerAndExecuteAgentAsync(String taskId, RequestContext requestContext, EventQueue queue, EnhancedRunnable.DoneCallback doneCallback) {
        LOGGER.debug("Registering agent execution for task {}, runningAgents.size() before: {}", taskId, runningAgents.size());
        logThreadStats("AGENT START");
        EnhancedRunnable runnable = new EnhancedRunnable() {
            @Override
            public void run() {
                LOGGER.debug("Agent execution starting for task {}", taskId);
                AgentEmitter emitter = new AgentEmitter(requestContext, queue);
                try {
                    agentExecutor.execute(requestContext, emitter);
                } catch (A2AError e) {
                    // Log A2A errors at WARN level with full stack trace
                    // These are expected business errors but should be tracked
                    LOGGER.warn("Agent execution threw A2AError for task {}: {} - {}", 
                        taskId, e.getClass().getSimpleName(), e.getMessage(), e);
                    emitter.fail(e);
                } catch (RuntimeException e) {
                    // Log unexpected runtime exceptions at ERROR level
                    // These indicate bugs in agent implementation
                    LOGGER.error("Agent execution threw unexpected RuntimeException for task {}", taskId, e);
                    emitter.fail(new io.a2a.spec.InternalError("Agent execution failed: " + e.getMessage()));
                } catch (Exception e) {
                    // Log other exceptions at ERROR level
                    LOGGER.error("Agent execution threw unexpected Exception for task {}", taskId, e);
                    emitter.fail(new io.a2a.spec.InternalError("Agent execution failed: " + e.getMessage()));
                }
                LOGGER.debug("Agent execution completed for task {}", taskId);
                // The consumer (running on the Vert.x worker thread) handles queue lifecycle.
                // This avoids blocking agent-executor threads waiting for worker threads.
            }
        };

        // CRITICAL: Add callback BEFORE starting CompletableFuture to avoid race condition
        // If agent completes very fast, whenComplete can fire before caller adds callbacks
        runnable.addDoneCallback(doneCallback);
        
        // Mark as started to prevent further callback additions (enforced by runtime check)
        runnable.markStarted();

        CompletableFuture<Void> cf = CompletableFuture.runAsync(runnable, executor)
                .whenComplete((v, err) -> {
                    if (err != null) {
                        LOGGER.error("Agent execution failed for task {}", taskId, err);
                        runnable.setError(err);
                        // Don't close queue here - let the consumer handle it via error callback
                        // This ensures the consumer (which may not have started polling yet) gets the error
                    }
                    // Queue lifecycle is managed by EventConsumer.consumeAll()
                    // which closes the queue on final events.
                    logThreadStats("AGENT COMPLETE END");
                    runnable.invokeDoneCallbacks();
                });
        runningAgents.put(taskId, cf);
        LOGGER.debug("Registered agent for task {}, runningAgents.size() after: {}", taskId, runningAgents.size());
        return runnable;
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

            if (isStreaming) {
                // For streaming: EventConsumer handles queue closure via agentCompleted flag
                // When agent completes, EventConsumer.agentCompleted is set to true via agent done callback
                // EventConsumer drains remaining events from ChildQueue (waiting for MainEventBusProcessor)
                // After poll timeout with agentCompleted=true, EventConsumer closes queue and completes stream
                // This avoids race condition where cleanup closes queue before MainEventBusProcessor distributes events
                LOGGER.debug("Streaming call for task {}: queue lifecycle managed by EventConsumer (agentCompleted flag)", taskId);
            } else {
                // For non-streaming: close the ChildQueue directly
                // No EventConsumer polling, so we must close explicitly
                // This triggers MainQueue.childClosing() which handles cleanup and poison pill generation
                LOGGER.debug("Non-streaming call, closing ChildQueue for task {} (immediate=false, notifyParent=true)", taskId);
                queue.close(false, true);
            }

            // For replicated environments, the poison pill is now sent via CDI events
            // When JpaDatabaseTaskStore.save() persists a final task, it fires TaskFinalizedEvent
            // ReplicatedQueueManager.onTaskFinalized() observes AFTER_SUCCESS and sends poison pill
            // This guarantees the transaction is committed before the poison pill is sent
            LOGGER.debug("Queue cleanup completed for task {}", taskId);

            logThreadStats("CLEANUP END");
        });
    }

    private MessageSendSetup initMessageSend(MessageSendParams params, ServerCallContext context) {
        // Build RequestContext FIRST to get the real taskId (auto-generated if not provided)
        // This eliminates the need for temporary IDs - we use the same UUID throughout
        RequestContext requestContext = requestContextBuilder.get()
                .setParams(params)
                .setTaskId(params.message().taskId())  // Use client's ID or let RequestContext generate
                .setContextId(params.message().contextId())
                .setTask(null)  // Will be set below after TaskManager retrieves it
                .setServerCallContext(context)
                .build();

        // Get the actual taskId from RequestContext (either from client or auto-generated)
        // RequestContext.build() guarantees taskId is non-null via checkOrGenerateTaskId()
        String taskId = java.util.Objects.requireNonNull(
                requestContext.getTaskId(), "TaskId must be non-null after RequestContext.build()");

        // Create TaskManager with the real taskId
        TaskManager taskManager = new TaskManager(
                taskId,
                params.message().contextId(),
                taskStore,
                params.message());

        Task task = taskManager.getTask();
        if (task != null) {
            LOGGER.debug("Found task updating with message {}", params.message());
            task = taskManager.updateWithMessage(params.message(), task);

            if (pushConfigStore != null && params.configuration() != null && params.configuration().pushNotificationConfig() != null) {
                LOGGER.debug("Adding push info");
                pushConfigStore.setInfo(task.id(), params.configuration().pushNotificationConfig());
            }
        }

        // Only rebuild RequestContext if we have a task to add, or if taskId was auto-generated
        // and doesn't match the original message (to update params with generated taskId)
        boolean taskIdChanged = !taskId.equals(params.message().taskId());

        if (task != null || taskIdChanged) {
            MessageSendParams paramsToUse;

            if (taskIdChanged) {
                // Update message to include the taskId (handles auto-generated case)
                // This prevents "bad task id" validation errors
                Message updatedMessage = Message.builder(params.message())
                        .taskId(taskId)
                        .build();
                paramsToUse = new MessageSendParams(
                        updatedMessage,
                        params.configuration(),
                        params.metadata());
            } else {
                // TaskId matches, reuse original params to preserve Message object identity
                paramsToUse = params;
            }

            requestContext = requestContextBuilder.get()
                    .setParams(paramsToUse)
                    .setTask(task)
                    .setServerCallContext(context)
                    .build();
        }
        // else: task is null and taskId matches - use the original requestContext

        return new MessageSendSetup(taskManager, task, requestContext);
    }

    /**
     * Log current thread and resource statistics for debugging.
     * Uses dedicated {@link #THREAD_STATS_LOGGER} for independent logging control.
     * Only logs when DEBUG level is enabled. Call this from debugger or add strategic
     * calls during investigation. In production with INFO logging, this is a no-op.
     * <p>
     * Enable independently with: {@code logging.level.io.a2a.server.diagnostics.ThreadStats=DEBUG}
     * </p>
     */
    @SuppressWarnings("unused")  // Used for debugging
    private void logThreadStats(String label) {
        // Early return if debug logging is not enabled to avoid overhead
        if (!THREAD_STATS_LOGGER.isDebugEnabled()) {
            return;
        }

        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        while (rootGroup.getParent() != null) {
            rootGroup = rootGroup.getParent();
        }
        int activeThreads = rootGroup.activeCount();

        // Count specific thread types
        Thread[] threads = new Thread[activeThreads * 2];
        int count = rootGroup.enumerate(threads);
        int eventConsumerThreads = 0;
        int agentExecutorThreads = 0;
        for (int i = 0; i < count; i++) {
            if (threads[i] != null) {
                String name = threads[i].getName();
                if (name.startsWith("a2a-event-consumer-")) {
                    eventConsumerThreads++;
                } else if (name.startsWith("a2a-agent-executor-")) {
                    agentExecutorThreads++;
                }
            }
        }

        THREAD_STATS_LOGGER.debug("=== THREAD STATS: {} ===", label);
        THREAD_STATS_LOGGER.debug("Total active threads: {}", activeThreads);
        THREAD_STATS_LOGGER.debug("EventConsumer threads: {}", eventConsumerThreads);
        THREAD_STATS_LOGGER.debug("AgentExecutor threads: {}", agentExecutorThreads);
        THREAD_STATS_LOGGER.debug("Running agents: {}", runningAgents.size());
        THREAD_STATS_LOGGER.debug("Queue manager active queues: {}", queueManager.getClass().getSimpleName());

        // List running agents
        if (!runningAgents.isEmpty()) {
            THREAD_STATS_LOGGER.debug("Running agent tasks:");
            runningAgents.forEach((taskId, future) ->
                THREAD_STATS_LOGGER.debug("  - Task {}: {}", taskId, future.isDone() ? "DONE" : "RUNNING")
            );
        }

        THREAD_STATS_LOGGER.debug("=== END THREAD STATS ===");
    }

    /**
     * Check if an event represents a final task state.
     *
     * @param eventKind the event to check
     * @return true if the event represents a final state (COMPLETED, FAILED, CANCELED, REJECTED, UNKNOWN)
     */
    private boolean isFinalEvent(EventKind eventKind) {
        if (!(eventKind instanceof Event event)) {
            return false;
        }
        if (event instanceof Task task) {
            return task.status() != null && task.status().state() != null
                    && task.status().state().isFinal();
        } else if (event instanceof TaskStatusUpdateEvent statusUpdate) {
            return statusUpdate.isFinal();
        }
        return false;
    }

    private record MessageSendSetup(TaskManager taskManager, @Nullable Task task, RequestContext requestContext) {}
}
