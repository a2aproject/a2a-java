package io.a2a.server.tasks;

import static io.a2a.server.util.async.AsyncUtils.consumer;
import static io.a2a.server.util.async.AsyncUtils.createTubeConfig;
import static io.a2a.server.util.async.AsyncUtils.processor;
import static io.a2a.util.Assert.checkNotNullParam;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.a2a.server.events.EventConsumer;
import io.a2a.server.events.EventQueueItem;
import io.a2a.spec.A2AServerException;
import io.a2a.spec.Event;
import io.a2a.spec.EventKind;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultAggregator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResultAggregator.class);

    private final TaskManager taskManager;
    private final Executor executor;
    private final TaskStateProcessor stateProcessor;
    private volatile Message message;

    public ResultAggregator(TaskManager taskManager, Message message, Executor executor, TaskStateProcessor stateProcessor) {
        checkNotNullParam("taskManager", taskManager);
        checkNotNullParam("executor", executor);
        checkNotNullParam("stateProcessor", stateProcessor);
        this.taskManager = taskManager;
        this.message = message;
        this.executor = executor;
        this.stateProcessor = stateProcessor;
    }

    public EventKind getCurrentResult() {
        if (message != null) {
            return message;
        }
        return taskManager.getTask();
    }

    public Flow.Publisher<EventQueueItem> consumeAndEmit(EventConsumer consumer) {
        Flow.Publisher<EventQueueItem> allItems = consumer.consumeAll();

        // Process items to build state without persisting
        return processor(createTubeConfig(), allItems, (errorConsumer, item) -> {
            // Build state for non-replicated events (don't persist yet)
            if (!item.isReplicated()) {
                try {
                    taskManager.processEvent(item.getEvent());
                } catch (A2AServerException e) {
                    errorConsumer.accept(e);
                    return false;
                }
            }
            // Continue processing and emit (both replicated and non-replicated)
            return true;
        });
    }

    public EventKind consumeAll(EventConsumer consumer) throws JSONRPCError {
        AtomicReference<EventKind> returnedEvent = new AtomicReference<>();
        Flow.Publisher<EventQueueItem> allItems = consumer.consumeAll();
        AtomicReference<Throwable> error = new AtomicReference<>();
        consumer(
                createTubeConfig(),
                allItems,
                (item) -> {
                    Event event = item.getEvent();
                    if (event instanceof Message msg) {
                        message = msg;
                        if (returnedEvent.get() == null) {
                            returnedEvent.set(msg);
                            return false;
                        }
                    }
                    // Build state for non-replicated events (don't persist yet)
                    if (!item.isReplicated()) {
                        try {
                            taskManager.processEvent(event);
                        } catch (A2AServerException e) {
                            error.set(e);
                            return false;
                        }
                    }
                    return true;
                },
                error::set);

        Throwable err = error.get();
        if (err != null) {
            Utils.rethrow(err);
        }

        if (returnedEvent.get() != null) {
            return returnedEvent.get();
        }
        return taskManager.getTask();
    }

    public EventTypeAndInterrupt consumeAndBreakOnInterrupt(EventConsumer consumer, boolean blocking) throws JSONRPCError {
        Flow.Publisher<EventQueueItem> allItems = consumer.consumeAll();
        AtomicReference<Message> message = new AtomicReference<>();
        AtomicBoolean interrupted = new AtomicBoolean(false);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CompletableFuture<Void> completionFuture = new CompletableFuture<>();
        // Separate future for tracking background consumption completion
        CompletableFuture<Void> consumptionCompletionFuture = new CompletableFuture<>();

        // CRITICAL: The subscription itself must run on a background thread to avoid blocking
        // the Vert.x worker thread. EventConsumer.consumeAll() starts a polling loop that
        // blocks in dequeueEventItem(), so we must subscribe from a background thread.
        // Use the @Internal executor (not ForkJoinPool.commonPool) to avoid saturation
        // during concurrent request bursts.
        CompletableFuture.runAsync(() -> {
            consumer(
                createTubeConfig(),
                allItems,
                (item) -> {
                    Event event = item.getEvent();

                    // Handle Throwable events
                    if (event instanceof Throwable t) {
                        errorRef.set(t);
                        completionFuture.completeExceptionally(t);
                        return false;
                    }

                    // Handle Message events
                    if (event instanceof Message msg) {
                        ResultAggregator.this.message = msg;
                        message.set(msg);
                        completionFuture.complete(null);
                        return false;
                    }

                    // Build state for non-replicated events (don't persist yet)
                    if (!item.isReplicated()) {
                        try {
                            taskManager.processEvent(event);
                        } catch (A2AServerException e) {
                            errorRef.set(e);
                            completionFuture.completeExceptionally(e);
                            return false;
                        }
                    }

                    // Determine interrupt behavior
                    boolean isFinalEvent = (event instanceof Task task && task.getStatus().state().isFinal())
                            || (event instanceof TaskStatusUpdateEvent tsue && tsue.isFinal());
                    boolean isAuthRequired = (event instanceof Task task && task.getStatus().state() == TaskState.AUTH_REQUIRED)
                            || (event instanceof TaskStatusUpdateEvent tsue && tsue.getStatus().state() == TaskState.AUTH_REQUIRED);

                        // auth-required is a special state: the message should be
                        // escalated back to the caller, but the agent is expected to
                        // continue producing events once the authorization is received
                        // out-of-band. This is in contrast to input-required, where a
                        // new request is expected in order for the agent to make progress,
                        // so the agent should exit.
                    if (!blocking) {
                        // For blocking calls: Interrupt to free Vert.x thread, but continue in background
                        // Python's async consumption doesn't block threads, but Java's does
                        // So we interrupt to return quickly, then rely on background consumption
                        // DefaultRequestHandler will fetch the final state from TaskStore
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Blocking call for task {}: {} event, returning with background consumption",
                                taskIdForLogging(), isFinalEvent ? "final" : "non-final");
                        }
                    }

                    // Complete the future to unblock the main thread
                    interrupted.set(true);
                    completionFuture.complete(null);

                    // For blocking calls, DON'T complete consumptionCompletionFuture here.
                    // Let it complete naturally when subscription finishes (onComplete callback below).
                    // This ensures all events are processed and persisted to TaskStore before
                    // DefaultRequestHandler.cleanupProducer() proceeds with cleanup.
                    //
                    // For non-blocking and auth-required calls, complete immediately to allow
                    // cleanup to proceed while consumption continues in background.
                    if (!blocking) {
                        consumptionCompletionFuture.complete(null);
                    }
                    // else: blocking calls wait for actual consumption completion in onComplete

                    // Continue consuming in background - keep requesting events
                    // Note: continueInBackground is always true when shouldInterrupt is true
                    // (auth-required, non-blocking, or blocking all set it to true)
                    if (LOGGER.isDebugEnabled()) {
                        String reason = isAuthRequired ? "auth-required" : (blocking ? "blocking" : "non-blocking");
                        LOGGER.debug("Task {}: Continuing background consumption (reason: {})", taskIdForLogging(), reason);
                    }
                    return true;
                },
                throwable -> {
                    // Handle onError and onComplete
                    if (throwable != null) {
                        errorRef.set(throwable);
                        completionFuture.completeExceptionally(throwable);
                        consumptionCompletionFuture.completeExceptionally(throwable);
                    } else {
                        // onComplete - subscription finished normally
                        // For non-blocking calls, persist the final task state after consumption completes
                        if (!blocking) {
                            Task finalTask = taskManager.getTask();
                            if (finalTask != null) {
                                taskManager.saveTask(finalTask);
                                if (LOGGER.isDebugEnabled()) {
                                    LOGGER.debug("Persisted final task state after background consumption: {}", taskIdForLogging());
                                }
                                // Remove task from state processor after final persistence
                                stateProcessor.removeTask(finalTask.getId());
                                LOGGER.debug("Removed task {} from state processor after background consumption", finalTask.getId());
                            }
                        }
                        completionFuture.complete(null);
                        consumptionCompletionFuture.complete(null);
                    }
                }
            );
        }, executor);

        // Wait for completion or interruption
        try {
            completionFuture.join();
        } catch (CompletionException e) {
            // CompletionException wraps the actual exception
            Throwable cause = e.getCause();
            if (cause != null) {
                Utils.rethrow(cause);
            } else {
                throw e;
            }
        }

        // Note: For blocking calls that were interrupted, the wait logic has been moved
        // to DefaultRequestHandler.onMessageSend() to avoid blocking Vert.x worker threads.
        // Queue lifecycle is managed by DefaultRequestHandler.cleanupProducer()

        Throwable error = errorRef.get();
        if (error != null) {
            Utils.rethrow(error);
        }

        return new EventTypeAndInterrupt(
                message.get() != null ? message.get() : taskManager.getTask(),
                interrupted.get(),
                consumptionCompletionFuture);
    }

    private String taskIdForLogging() {
        Task task = taskManager.getTask();
        return task != null ? task.getId() : "unknown";
    }

    public record EventTypeAndInterrupt(EventKind eventType, boolean interrupted, CompletableFuture<Void> consumptionFuture) {

    }
}
