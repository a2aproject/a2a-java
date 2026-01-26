package io.a2a.server.tasks;

import static io.a2a.server.util.async.AsyncUtils.consumer;
import static io.a2a.server.util.async.AsyncUtils.createTubeConfig;
import static io.a2a.server.util.async.AsyncUtils.processor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.a2a.server.events.EventConsumer;
import io.a2a.server.events.EventQueueItem;
import io.a2a.spec.A2AError;
import io.a2a.spec.Event;
import io.a2a.spec.EventKind;
import io.a2a.spec.InternalError;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.util.Utils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultAggregator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResultAggregator.class);

    private final TaskManager taskManager;
    private final Executor executor;
    private final Executor eventConsumerExecutor;
    private volatile @Nullable Message message;

    public ResultAggregator(TaskManager taskManager, @Nullable Message message, Executor executor, Executor eventConsumerExecutor) {
        this.taskManager = taskManager;
        this.message = message;
        this.executor = executor;
        this.eventConsumerExecutor = eventConsumerExecutor;
    }

    public @Nullable EventKind getCurrentResult() {
        if (message != null) {
            return message;
        }
        return taskManager.getTask();
    }

    public Flow.Publisher<EventQueueItem> consumeAndEmit(EventConsumer consumer) {
        Flow.Publisher<EventQueueItem> allItems = consumer.consumeAll();

        // Just stream events - no persistence needed
        // TaskStore update moved to MainEventBusProcessor
        Flow.Publisher<EventQueueItem> processed = processor(createTubeConfig(), allItems, (errorConsumer, item) -> {
            // Continue processing and emit all events
            return true;
        });

        // Wrap the publisher to ensure subscription happens on eventConsumerExecutor
        // This prevents EventConsumer polling loop from running on AgentExecutor threads
        // which caused thread accumulation when those threads didn't timeout
        return new Flow.Publisher<EventQueueItem>() {
            @Override
            public void subscribe(Flow.Subscriber<? super EventQueueItem> subscriber) {
                // Submit subscription to eventConsumerExecutor to isolate polling work
                eventConsumerExecutor.execute(() -> processed.subscribe(subscriber));
            }
        };
    }

    public EventKind consumeAll(EventConsumer consumer) throws A2AError {
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
                    // TaskStore update moved to MainEventBusProcessor
                    return true;
                },
                error::set);

        Throwable err = error.get();
        if (err != null) {
            Utils.rethrow(err);
        }

        EventKind result = returnedEvent.get();
        if (result != null) {
            return result;
        }
        Task task = taskManager.getTask();
        if (task == null) {
            throw new io.a2a.spec.InternalError("No task or message available after consuming all events");
        }
        return task;
    }

    public EventTypeAndInterrupt consumeAndBreakOnInterrupt(EventConsumer consumer, boolean blocking) throws A2AError {
        Flow.Publisher<EventQueueItem> allItems = consumer.consumeAll();
        AtomicReference<Message> message = new AtomicReference<>();
        AtomicReference<Task> capturedTask = new AtomicReference<>();  // Capture Task events
        AtomicBoolean interrupted = new AtomicBoolean(false);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CompletableFuture<Void> completionFuture = new CompletableFuture<>();
        // Separate future for tracking background consumption completion
        CompletableFuture<Void> consumptionCompletionFuture = new CompletableFuture<>();
        // Latch to ensure EventConsumer starts polling before we wait on completionFuture
        java.util.concurrent.CountDownLatch pollingStarted = new java.util.concurrent.CountDownLatch(1);

        // CRITICAL: The subscription itself must run on a background thread to avoid blocking
        // the Vert.x worker thread. EventConsumer.consumeAll() starts a polling loop that
        // blocks in dequeueEventItem(), so we must subscribe from a background thread.
        // Use the dedicated @EventConsumerExecutor (cached thread pool) which creates threads
        // on demand for I/O-bound polling. Using the @Internal executor caused deadlock when
        // pool exhausted (100+ concurrent queues but maxPoolSize=50).
        CompletableFuture.runAsync(() -> {
            // Signal that polling is about to start
            pollingStarted.countDown();
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

                    // Capture Task events (especially for new tasks where taskManager.getTask() would return null)
                    // We capture the LATEST task to ensure we get the most up-to-date state
                    if (event instanceof Task t) {
                        Task previousTask = capturedTask.get();
                        capturedTask.set(t);
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Captured Task event: id={}, state={} (previous: {})",
                                    t.id(), t.status().state(),
                                    previousTask != null ? previousTask.id() + "/" + previousTask.status().state() : "none");
                        }
                    }

                    // TaskStore update moved to MainEventBusProcessor

                    // Determine interrupt behavior
                    boolean shouldInterrupt = false;
                    boolean isFinalEvent = (event instanceof Task task && task.status().state().isFinal())
                            || (event instanceof TaskStatusUpdateEvent tsue && tsue.isFinal());
                    boolean isAuthRequired = (event instanceof Task task && task.status().state() == TaskState.AUTH_REQUIRED)
                            || (event instanceof TaskStatusUpdateEvent tsue && tsue.status().state() == TaskState.AUTH_REQUIRED);

                    LOGGER.debug("ResultAggregator: Evaluating interrupt (blocking={}, isFinal={}, isAuth={}, eventType={})",
                        blocking, isFinalEvent, isAuthRequired, event.getClass().getSimpleName());

                    // Always interrupt on auth_required, as it needs external action.
                    if (isAuthRequired) {
                        // auth-required is a special state: the message should be
                        // escalated back to the caller, but the agent is expected to
                        // continue producing events once the authorization is received
                        // out-of-band. This is in contrast to input-required, where a
                        // new request is expected in order for the agent to make progress,
                        // so the agent should exit.
                        shouldInterrupt = true;
                        LOGGER.debug("ResultAggregator: Setting shouldInterrupt=true (AUTH_REQUIRED)");
                    }
                    else if (!blocking) {
                        // For non-blocking calls, interrupt as soon as a task is available.
                        shouldInterrupt = true;
                        LOGGER.debug("ResultAggregator: Setting shouldInterrupt=true (non-blocking)");
                    }
                    else if (blocking) {
                        // For blocking calls: Interrupt to free Vert.x thread, but continue in background
                        // Python's async consumption doesn't block threads, but Java's does
                        // So we interrupt to return quickly, then rely on background consumption
                        shouldInterrupt = true;
                        LOGGER.debug("ResultAggregator: Setting shouldInterrupt=true (blocking, isFinal={})", isFinalEvent);
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Blocking call for task {}: {} event, returning with background consumption",
                                taskIdForLogging(), isFinalEvent ? "final" : "non-final");
                        }
                    }

                    if (shouldInterrupt) {
                        LOGGER.debug("ResultAggregator: Interrupting consumption (setting interrupted=true)");
                        // Complete the future to unblock the main thread
                        interrupted.set(true);
                        completionFuture.complete(null);

                        // For blocking calls, DON'T complete consumptionCompletionFuture here.
                        // Let it complete naturally when subscription finishes (onComplete callback below).
                        // This ensures all events are fully processed before cleanup.
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
                    }

                    // Continue processing
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
                        completionFuture.complete(null);
                        consumptionCompletionFuture.complete(null);
                    }
                }
            );
        }, eventConsumerExecutor);

        // Wait for EventConsumer to start polling before we wait for events
        // This prevents race where agent enqueues events before EventConsumer starts
        try {
            pollingStarted.await(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new io.a2a.spec.InternalError("Interrupted waiting for EventConsumer to start");
        }

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

        // Return Message if captured, otherwise Task if captured, otherwise fetch from TaskStore
        EventKind eventKind = message.get();
        if (eventKind == null) {
            eventKind = capturedTask.get();
            if (LOGGER.isDebugEnabled() && eventKind instanceof Task t) {
                LOGGER.debug("Returning capturedTask: id={}, state={}", t.id(), t.status().state());
            }
        }
        if (eventKind == null) {
            eventKind = taskManager.getTask();
            if (LOGGER.isDebugEnabled() && eventKind instanceof Task t) {
                LOGGER.debug("Returning task from TaskStore: id={}, state={}", t.id(), t.status().state());
            }
        }
        if (eventKind == null) {
            throw new InternalError("Could not find a Task/Message for " + taskManager.getTaskId());
        }

        return new EventTypeAndInterrupt(
                eventKind,
                interrupted.get(),
                consumptionCompletionFuture);
    }

    private String taskIdForLogging() {
        Task task = taskManager.getTask();
        return task != null ? task.id() : "unknown";
    }

    public record EventTypeAndInterrupt(EventKind eventType, boolean interrupted, CompletableFuture<Void> consumptionFuture) {

    }
}
