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
import jakarta.inject.Inject;

import io.a2a.server.ServerCallContext;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.agentexecution.SimpleRequestContextBuilder;
import io.a2a.server.events.EnhancedRunnable;
import io.a2a.server.events.EventConsumer;
import io.a2a.server.events.EventQueue;
import io.a2a.server.events.QueueManager;
import io.a2a.server.events.TaskQueueExistsException;
import io.a2a.server.tasks.PushNotificationConfigStore;
import io.a2a.server.tasks.PushNotificationSender;
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

        ResultAggregator resultAggregator = new ResultAggregator(taskManager, null);

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
        ResultAggregator resultAggregator = new ResultAggregator(mss.taskManager, null);

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
            if (interruptedOrNonBlocking) {
                CompletableFuture<Void> cleanupTask = CompletableFuture.runAsync(() -> cleanupProducer(taskId, false), executor);
                trackBackgroundTask(cleanupTask);
            } else {
                cleanupProducer(taskId, false);
            }
        }

        LOGGER.debug("Returning: {}", etai.eventType());
        return etai.eventType();
    }

    @Override
    public Flow.Publisher<StreamingEventKind> onMessageSendStream(
            MessageSendParams params, ServerCallContext context) throws JSONRPCError {
        LOGGER.debug("onMessageSendStream - task: {}; context {}", params.message().getTaskId(), params.message().getContextId());
        MessageSendSetup mss = initMessageSend(params, context);

        AtomicReference<String> taskId = new AtomicReference<>(mss.requestContext.getTaskId());
        EventQueue queue = queueManager.createOrTap(taskId.get());
        ResultAggregator resultAggregator = new ResultAggregator(mss.taskManager, null);

        EnhancedRunnable producerRunnable = registerAndExecuteAgentAsync(taskId.get(), mss.requestContext, queue);

        // Move consumer creation and callback registration outside try block
        // so consumer is available for background consumption on client disconnect
        EventConsumer consumer = new EventConsumer(queue);
        producerRunnable.addDoneCallback(consumer.createAgentRunnableDoneCallback());

        AtomicBoolean backgroundConsumeStarted = new AtomicBoolean(false);

        try {
            Flow.Publisher<Event> results = resultAggregator.consumeAndEmit(consumer);

            Flow.Publisher<Event> eventPublisher =
                    processor(createTubeConfig(), results, ((errorConsumer, event) -> {
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

            Flow.Publisher<StreamingEventKind> finalPublisher = convertingProcessor(eventPublisher, event -> (StreamingEventKind) event);

            // Wrap publisher to detect client disconnect and continue background consumption
            return subscriber -> finalPublisher.subscribe(new Flow.Subscriber<StreamingEventKind>() {
                private Flow.Subscription subscription;

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    this.subscription = subscription;
                    // Wrap subscription to detect cancellation
                    subscriber.onSubscribe(new Flow.Subscription() {
                        @Override
                        public void request(long n) {
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
                    subscriber.onNext(item);
                }

                @Override
                public void onError(Throwable throwable) {
                    subscriber.onError(throwable);
                }

                @Override
                public void onComplete() {
                    subscriber.onComplete();
                }

                private void startBackgroundConsumption() {
                    if (backgroundConsumeStarted.compareAndSet(false, true)) {
                        // Client disconnected: continue consuming and persisting events in background
                        CompletableFuture<Void> bgTask = CompletableFuture.runAsync(() -> {
                            try {
                                resultAggregator.consumeAll(consumer);
                                LOGGER.debug("Background consumption completed for task {}", taskId.get());
                            } catch (Exception e) {
                                LOGGER.error("Error during background consumption for task {}", taskId.get(), e);
                            }
                        }, executor);
                        trackBackgroundTask(bgTask);
                    }
                }
            });
        } finally {
            CompletableFuture<Void> cleanupTask = CompletableFuture.runAsync(() -> cleanupProducer(taskId.get(), true), executor);
            trackBackgroundTask(cleanupTask);
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
        Task task = taskStore.get(params.id());
        if (task == null) {
            throw new TaskNotFoundError();
        }

        TaskManager taskManager = new TaskManager(task.getId(), task.getContextId(), taskStore, null);
        ResultAggregator resultAggregator = new ResultAggregator(taskManager, null);
        EventQueue queue = queueManager.tap(task.getId());

        if (queue == null) {
            throw new TaskNotFoundError();
        }

        EventConsumer consumer = new EventConsumer(queue);
        Flow.Publisher<Event> results = resultAggregator.consumeAndEmit(consumer);
        return convertingProcessor(results, e -> (StreamingEventKind) e);
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

    private EnhancedRunnable registerAndExecuteAgentAsync(String taskId, RequestContext requestContext, EventQueue queue) {
        EnhancedRunnable runnable = new EnhancedRunnable() {
            @Override
            public void run() {
                agentExecutor.execute(requestContext, queue);
                try {
                    queueManager.awaitQueuePollerStart(queue);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };

        CompletableFuture<Void> cf = CompletableFuture.runAsync(runnable, executor)
                .whenComplete((v, err) -> {
                    if (err != null) {
                        runnable.setError(err);
                        // Close queue on error
                        queue.close();
                    } else {
                        // Only close queue if task is in a final state
                        Task task = taskStore.get(taskId);
                        if (task != null && task.getStatus() != null && task.getStatus().state().isFinal()) {
                            queue.close();
                        } else {
                            LOGGER.debug("Task {} not in final state or not yet created, keeping queue open", taskId);
                        }
                    }
                    runnable.invokeDoneCallbacks();
                });
        runningAgents.put(taskId, cf);
        return runnable;
    }

    private void trackBackgroundTask(CompletableFuture<Void> task) {
        backgroundTasks.add(task);

        task.whenComplete((result, throwable) -> {
            try {
                if (throwable != null) {
                    if (throwable instanceof java.util.concurrent.CancellationException) {
                        LOGGER.debug("Background task cancelled: {}", task);
                    } else {
                        LOGGER.error("Background task failed", throwable);
                    }
                }
            } finally {
                backgroundTasks.remove(task);
            }
        });
    }

    private void cleanupProducer(String taskId, boolean isStreaming) {
        LOGGER.debug("Starting cleanup for task {} (streaming={})", taskId, isStreaming);
        // TODO the Python implementation waits for the producerRunnable
        CompletableFuture<Void> agentFuture = runningAgents.get(taskId);
        if (agentFuture == null) {
            LOGGER.debug("No running agent found for task {}", taskId);
            return;
        }
        agentFuture.whenComplete((v, t) -> {
                    LOGGER.debug("Agent completed for task {}", taskId);

                    boolean closeQueue = false;
                    if (isStreaming) {
                        // For streaming calls, always close queue when agent completes
                        // Each streaming call is independent and doesn't support multi-message reuse
                        LOGGER.debug("Streaming call, closing queue for task {}", taskId);
                        closeQueue = true;
                    } else {
                        // For non-streaming calls, only close queue if task is in final state
                        // For non-final states, queue must stay open for potential future messages to same taskId
                        // so we can handle the "fire and forget' case used e.g. in the TCK
                        Task task = taskStore.get(taskId);
                        if (task != null && task.getStatus() != null && task.getStatus().state().isFinal()) {
                            LOGGER.debug("Task in final state, closing queue for task {}", taskId);
                            closeQueue = true;
                        } else {
                            LOGGER.debug("Task not in final state, keeping queue open for task {}", taskId);
                        }
                    }

                    if (closeQueue) {
                        try {
                            queueManager.close(taskId);
                        } catch (Exception e) {
                            LOGGER.debug("Error closing queue for task {}: {}", taskId, e.getMessage());
                        }
                    }

                    // Always remove from running agents
                    runningAgents.remove(taskId);
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

    private record MessageSendSetup(TaskManager taskManager, Task task, RequestContext requestContext) {}
}
