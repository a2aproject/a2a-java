package io.a2a.server.requesthandlers;

import static io.a2a.server.util.async.AsyncUtils.convertingProcessor;
import static io.a2a.server.util.async.AsyncUtils.createTubeConfig;
import static io.a2a.server.util.async.AsyncUtils.processor;

import java.util.ArrayList;
import java.util.List;
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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.a2a.server.ServerCallContext;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.agentexecution.SimpleRequestContextBuilder;
import io.a2a.server.events.EnhancedRunnable;
import io.a2a.server.events.EventConsumer;
import io.a2a.server.events.EventQueue;
import io.a2a.server.events.EventQueueItem;
import io.a2a.server.events.QueueManager;
import io.a2a.server.events.TaskQueueExistsException;
import io.a2a.server.tasks.PushNotificationConfigStore;
import io.a2a.server.tasks.PushNotificationSender;
import io.a2a.server.tasks.TaskStateProvider;
import io.a2a.server.tasks.ResultAggregator;
import io.a2a.server.tasks.TaskManager;
import io.a2a.server.tasks.TaskStore;
import io.a2a.server.util.async.Internal;
import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.Event;
import io.a2a.spec.EventKind;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.InternalError;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskNotCancelableError;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import io.a2a.spec.UnsupportedOperationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DefaultRequestHandler implements RequestHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRequestHandler.class);

    private final AgentExecutor agentExecutor;
    private final TaskStore taskStore;
    private final QueueManager queueManager;
    private final PushNotificationConfigStore pushConfigStore;
    private final PushNotificationSender pushSender;
    private final Supplier<RequestContext.Builder> requestContextBuilder;

    private final ConcurrentMap<String, CompletableFuture<Void>> runningAgents = new ConcurrentHashMap<>();
    private final Set<CompletableFuture<Void>> backgroundTasks = ConcurrentHashMap.newKeySet();

    private final Executor executor;

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
        this.requestContextBuilder = () -> new SimpleRequestContextBuilder(taskStore, false);
    }

    @Override
    public Task onGetTask(TaskQueryParams params, ServerCallContext context) throws JSONRPCError {
        LOGGER.debug("onGetTask {}", params.id());
        Task task = taskStore.get(params.id());
        if (task == null) {
            LOGGER.debug("No task found for {}. Throwing TaskNotFoundError", params.id());
            throw new TaskNotFoundError();
        }
        if (params.historyLength() != null && task.getHistory() != null && params.historyLength() < task.getHistory().size()) {
            List<Message> history;
            if (params.historyLength() <= 0) {
                history = new ArrayList<>();
            } else {
                history = task.getHistory().subList(
                        task.getHistory().size() - params.historyLength(),
                        task.getHistory().size() - 1);
            }

            task = new Task.Builder(task)
                    .history(history)
                    .build();
        }

        LOGGER.debug("Task found {}", task);
        return task;
    }

    @Override
    public Task onCancelTask(TaskIdParams params, ServerCallContext context) throws JSONRPCError {
        Task task = taskStore.get(params.id());
        if (task == null) {
            throw new TaskNotFoundError();
        }

        // Check if task is in a non-cancelable state (completed, canceled, failed, rejected)
        if (task.getStatus().state().isFinal()) {
            throw new TaskNotCancelableError(
                    "Task cannot be canceled - current state: " + task.getStatus().state().asString());
        }

        TaskManager taskManager = new TaskManager(
                task.getId(),
                task.getContextId(),
                taskStore,
                null);

        ResultAggregator resultAggregator = new ResultAggregator(taskManager, null, executor);

        EventQueue queue = queueManager.tap(task.getId());
        if (queue == null) {
            queue = queueManager.getEventQueueBuilder(task.getId()).build();
        }
        agentExecutor.cancel(
                requestContextBuilder.get()
                        .setTaskId(task.getId())
                        .setContextId(task.getContextId())
                        .setTask(task)
                        .setServerCallContext(context)
                        .build(),
                queue);

        Optional.ofNullable(runningAgents.get(task.getId()))
                .ifPresent(cf -> cf.cancel(true));

        EventConsumer consumer = new EventConsumer(queue);
        EventKind type = resultAggregator.consumeAll(consumer);
        if (!(type instanceof Task tempTask)) {
            throw new InternalError("Agent did not return valid response for cancel");
        }

        // Verify task was actually canceled (not completed concurrently)
        if (tempTask.getStatus().state() != TaskState.CANCELED) {
            throw new TaskNotCancelableError(
                    "Task cannot be canceled - current state: " + tempTask.getStatus().state().asString());
        }

        return tempTask;
    }

    @Override
    public EventKind onMessageSend(MessageSendParams params, ServerCallContext context) throws JSONRPCError {
        LOGGER.debug("onMessageSend - task: {}; context {}", params.message().getTaskId(), params.message().getContextId());
        MessageSendSetup mss = initMessageSend(params, context);

        String taskId = mss.requestContext.getTaskId();
        LOGGER.debug("Request context taskId: {}", taskId);

        EventQueue queue = queueManager.createOrTap(taskId);
        ResultAggregator resultAggregator = new ResultAggregator(mss.taskManager, null, executor);

        boolean blocking = true; // Default to blocking behavior
        if (params.configuration() != null && Boolean.FALSE.equals(params.configuration().blocking())) {
            blocking = false;
        }

        boolean interruptedOrNonBlocking = false;

        EnhancedRunnable producerRunnable = registerAndExecuteAgentAsync(taskId, mss.requestContext, queue);
        ResultAggregator.EventTypeAndInterrupt etai = null;
        try {
            // Create callback for push notifications during background event processing
            Runnable pushNotificationCallback = () -> sendPushNotification(taskId, resultAggregator);

            EventConsumer consumer = new EventConsumer(queue);

            // This callback must be added before we start consuming. Otherwise,
            // any errors thrown by the producerRunnable are not picked up by the consumer
            producerRunnable.addDoneCallback(consumer.createAgentRunnableDoneCallback());
            etai = resultAggregator.consumeAndBreakOnInterrupt(consumer, blocking, pushNotificationCallback);

            if (etai == null) {
                LOGGER.debug("No result, throwing InternalError");
                throw new InternalError("No result");
            }
            interruptedOrNonBlocking = etai.interrupted();
            LOGGER.debug("Was interrupted or non-blocking: {}", interruptedOrNonBlocking);

            EventKind kind = etai.eventType();
            if (kind instanceof Task taskResult && !taskId.equals(taskResult.getId())) {
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

        LOGGER.debug("Returning: {}", etai.eventType());
        return etai.eventType();
    }

    @Override
    public Flow.Publisher<StreamingEventKind> onMessageSendStream(
            MessageSendParams params, ServerCallContext context) throws JSONRPCError {
        LOGGER.debug("onMessageSendStream START - task: {}; context: {}; runningAgents: {}; backgroundTasks: {}",
                params.message().getTaskId(), params.message().getContextId(), runningAgents.size(), backgroundTasks.size());
        MessageSendSetup mss = initMessageSend(params, context);

        AtomicReference<String> taskId = new AtomicReference<>(mss.requestContext.getTaskId());
        EventQueue queue = queueManager.createOrTap(taskId.get());
        LOGGER.debug("Created/tapped queue for task {}: {}", taskId.get(), queue);
        ResultAggregator resultAggregator = new ResultAggregator(mss.taskManager, null, executor);

        EnhancedRunnable producerRunnable = registerAndExecuteAgentAsync(taskId.get(), mss.requestContext, queue);

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
                    if (!Objects.equals(taskId.get(), createdTask.getId())) {
                        errorConsumer.accept(new InternalError("Task ID mismatch in agent response"));
                    }

                    // TODO the Python implementation no longer has the following block but removing it causes
                    //  failures here
                    try {
                        queueManager.add(createdTask.getId(), queue);
                        taskId.set(createdTask.getId());
                    } catch (TaskQueueExistsException e) {
                        // TODO Log
                    }
                    if (pushConfigStore != null &&
                            params.configuration() != null &&
                            params.configuration().pushNotificationConfig() != null) {

                        pushConfigStore.setInfo(
                                createdTask.getId(),
                                params.configuration().pushNotificationConfig());
                    }

                }
                if (pushSender != null && taskId.get() != null) {
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
                LOGGER.debug("Creating subscription wrapper for task {}", taskId.get());
                finalPublisher.subscribe(new Flow.Subscriber<StreamingEventKind>() {
                    private Flow.Subscription subscription;

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

            trackBackgroundTask(cleanupProducer(agentFuture, null, taskId.get(), queue, true));
        }
    }

    @Override
    public TaskPushNotificationConfig onSetTaskPushNotificationConfig(
            TaskPushNotificationConfig params, ServerCallContext context) throws JSONRPCError {
        if (pushConfigStore == null) {
            throw new UnsupportedOperationError();
        }
        Task task = taskStore.get(params.taskId());
        if (task == null) {
            throw new TaskNotFoundError();
        }

        PushNotificationConfig pushNotificationConfig = pushConfigStore.setInfo(params.taskId(), params.pushNotificationConfig());
        return new TaskPushNotificationConfig(params.taskId(), pushNotificationConfig);
    }

    @Override
    public TaskPushNotificationConfig onGetTaskPushNotificationConfig(
            GetTaskPushNotificationConfigParams params, ServerCallContext context) throws JSONRPCError {
        if (pushConfigStore == null) {
            throw new UnsupportedOperationError();
        }
        Task task = taskStore.get(params.id());
        if (task == null) {
            throw new TaskNotFoundError();
        }

        List<PushNotificationConfig> pushNotificationConfigList = pushConfigStore.getInfo(params.id());
        if (pushNotificationConfigList == null || pushNotificationConfigList.isEmpty()) {
            throw new InternalError("No push notification config found");
        }

        return new TaskPushNotificationConfig(params.id(), getPushNotificationConfig(pushNotificationConfigList, params.pushNotificationConfigId()));
    }

    private PushNotificationConfig getPushNotificationConfig(List<PushNotificationConfig> notificationConfigList,
                                                             String configId) {
        if (configId != null) {
            for (PushNotificationConfig notificationConfig : notificationConfigList) {
                if (configId.equals(notificationConfig.id())) {
                    return notificationConfig;
                }
            }
        }
        return notificationConfigList.get(0);
    }

    @Override
    public Flow.Publisher<StreamingEventKind> onResubscribeToTask(
            TaskIdParams params, ServerCallContext context) throws JSONRPCError {
        LOGGER.debug("onResubscribeToTask - taskId: {}", params.id());
        Task task = taskStore.get(params.id());
        if (task == null) {
            throw new TaskNotFoundError();
        }

        TaskManager taskManager = new TaskManager(task.getId(), task.getContextId(), taskStore, null);
        ResultAggregator resultAggregator = new ResultAggregator(taskManager, null, executor);
        EventQueue queue = queueManager.tap(task.getId());
        LOGGER.debug("onResubscribeToTask - tapped queue: {}", queue != null ? System.identityHashCode(queue) : "null");

        if (queue == null) {
            // If task is in final state, queue legitimately doesn't exist anymore
            if (task.getStatus().state().isFinal()) {
                throw new TaskNotFoundError();
            }
            // For non-final tasks, recreate the queue so client can receive future events
            // (Note: historical events from before queue closed are not available)
            LOGGER.debug("Queue not found for active task {}, creating new queue for future events", task.getId());
            queue = queueManager.createOrTap(task.getId());
        }

        EventConsumer consumer = new EventConsumer(queue);
        Flow.Publisher<EventQueueItem> results = resultAggregator.consumeAndEmit(consumer);
        LOGGER.debug("onResubscribeToTask - returning publisher for taskId: {}", params.id());
        return convertingProcessor(results, item -> (StreamingEventKind) item.getEvent());
    }

    @Override
    public List<TaskPushNotificationConfig> onListTaskPushNotificationConfig(
            ListTaskPushNotificationConfigParams params, ServerCallContext context) throws JSONRPCError {
        if (pushConfigStore == null) {
            throw new UnsupportedOperationError();
        }

        Task task = taskStore.get(params.id());
        if (task == null) {
            throw new TaskNotFoundError();
        }

        List<PushNotificationConfig> pushNotificationConfigList = pushConfigStore.getInfo(params.id());
        List<TaskPushNotificationConfig> taskPushNotificationConfigList = new ArrayList<>();
        if (pushNotificationConfigList != null) {
            for (PushNotificationConfig pushNotificationConfig : pushNotificationConfigList) {
                TaskPushNotificationConfig taskPushNotificationConfig = new TaskPushNotificationConfig(params.id(), pushNotificationConfig);
                taskPushNotificationConfigList.add(taskPushNotificationConfig);
            }
        }
        return taskPushNotificationConfigList;
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

    private CompletableFuture<Void> cleanupProducer(CompletableFuture<Void> agentFuture, CompletableFuture<Void> consumptionFuture, String taskId, EventQueue queue, boolean isStreaming) {
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
                params.message().getTaskId(),
                params.message().getContextId(),
                taskStore,
                params.message());

        Task task = taskManager.getTask();
        if (task != null) {
            LOGGER.debug("Found task updating with message {}", params.message());
            task = taskManager.updateWithMessage(params.message(), task);

            if (shouldAddPushInfo(params)) {
                LOGGER.debug("Adding push info");
                pushConfigStore.setInfo(task.getId(), params.configuration().pushNotificationConfig());
            }
        }

        RequestContext requestContext = requestContextBuilder.get()
                .setParams(params)
                .setTaskId(task == null ? null : task.getId())
                .setContextId(params.message().getContextId())
                .setTask(task)
                .setServerCallContext(context)
                .build();
        return new MessageSendSetup(taskManager, task, requestContext);
    }

    private void sendPushNotification(String taskId, ResultAggregator resultAggregator) {
        if (pushSender != null && taskId != null) {
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

    private record MessageSendSetup(TaskManager taskManager, Task task, RequestContext requestContext) {}
}
