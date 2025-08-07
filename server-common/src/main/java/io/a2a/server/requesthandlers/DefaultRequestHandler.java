package io.a2a.server.requesthandlers;

import static io.a2a.server.util.async.AsyncUtils.convertingProcessor;
import static io.a2a.server.util.async.AsyncUtils.createTubeConfig;
import static io.a2a.server.util.async.AsyncUtils.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

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
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import io.a2a.spec.UnsupportedOperationError;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
        TaskManager taskManager = new TaskManager(
                task.getId(),
                task.getContextId(),
                taskStore,
                null);

        ResultAggregator resultAggregator = new ResultAggregator(taskManager, null);

        EventQueue queue = queueManager.tap(task.getId());
        if (queue == null) {
            queue = EventQueue.create();
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
        if (type instanceof Task tempTask) {
            return tempTask;
        }

        throw new InternalError("Agent did not return a valid response");
    }

    @Override
    public EventKind onMessageSend(MessageSendParams params, ServerCallContext context) throws JSONRPCError {
        LOGGER.debug("onMessageSend - task: {}; context {}", params.message().getTaskId(), params.message().getContextId());
        MessageSendSetup mss = initMessageSend(params, context);

        String taskId = mss.requestContext.getTaskId();
        LOGGER.debug("Request context taskId: {}", taskId);

        EventQueue queue = queueManager.createOrTap(taskId);
        ResultAggregator resultAggregator = new ResultAggregator(mss.taskManager, null);

        boolean interrupted = false;

        EnhancedRunnable producerRunnable = registerAndExecuteAgentAsync(taskId, mss.requestContext, queue);
        ResultAggregator.EventTypeAndInterrupt etai = null;
        try {
            EventConsumer consumer = new EventConsumer(queue);

            // This callback must be added before we start consuming. Otherwise,
            // any errors thrown by the producerRunnable are not picked up by the consumer
            producerRunnable.addDoneCallback(consumer.createAgentRunnableDoneCallback());
            etai = resultAggregator.consumeAndBreakOnInterrupt(consumer);
            
            if (etai == null) {
                LOGGER.debug("No result, throwing InternalError");
                throw new InternalError("No result");
            }
            interrupted = etai.interrupted();
            LOGGER.debug("Was interrupted: {}", interrupted);

            EventKind kind = etai.eventType();
            if (kind instanceof Task taskResult && !taskId.equals(taskResult.getId())) {
                throw new InternalError("Task ID mismatch in agent response");
            }

        } finally {
            if (interrupted) {
                // TODO Make this async
                cleanupProducer(taskId);
            } else {
                cleanupProducer(taskId);
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

        try {
            EventConsumer consumer = new EventConsumer(queue);

            // This callback must be added before we start consuming. Otherwise,
            // any errors thrown by the producerRunnable are not picked up by the consumer
            producerRunnable.addDoneCallback(consumer.createAgentRunnableDoneCallback());
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
                            params.configuration().pushNotification() != null) {

                        pushConfigStore.setInfo(
                                createdTask.getId(),
                                params.configuration().pushNotification());
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

            return convertingProcessor(eventPublisher, event -> (StreamingEventKind) event);
        } finally {
            cleanupProducer(taskId.get());
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

        pushConfigStore.setInfo(params.taskId(), params.pushNotificationConfig());

        return params;
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
        return pushConfigStore != null && params.configuration() != null && params.configuration().pushNotification() != null;
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
                    }
                    queue.close();
                    runnable.invokeDoneCallbacks();
                });
        runningAgents.put(taskId, cf);
        return runnable;
    }

    private void cleanupProducer(String taskId) {
        // TODO the Python implementation waits for the producerRunnable
        runningAgents.get(taskId)
                .whenComplete((v, t) -> {
                    queueManager.close(taskId);
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
                pushConfigStore.setInfo(task.getId(), params.configuration().pushNotification());
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

    private record MessageSendSetup(TaskManager taskManager, Task task, RequestContext requestContext) {}
}
