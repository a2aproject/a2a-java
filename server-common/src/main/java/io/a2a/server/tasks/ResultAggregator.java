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
import io.a2a.spec.A2AServerException;
import io.a2a.spec.Event;
import io.a2a.spec.EventKind;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.util.Utils;

public class ResultAggregator {
    private final TaskManager taskManager;
    private final Executor executor;
    private volatile Message message;

    public ResultAggregator(TaskManager taskManager, Message message, Executor executor) {
        this.taskManager = taskManager;
        this.message = message;
        this.executor = executor;
    }

    public EventKind getCurrentResult() {
        if (message != null) {
            return message;
        }
        return taskManager.getTask();
    }

    public Flow.Publisher<Event> consumeAndEmit(EventConsumer consumer) {
        Flow.Publisher<Event> all = consumer.consumeAll();

        return processor(createTubeConfig(), all, ((errorConsumer, event) -> {
            try {
                callTaskManagerProcess(event);
            } catch (A2AServerException e) {
                errorConsumer.accept(e);
                return false;
            }
            return true;
        }));
    }

    public EventKind consumeAll(EventConsumer consumer) throws JSONRPCError {
        AtomicReference<EventKind> returnedEvent = new AtomicReference<>();
        Flow.Publisher<Event> all = consumer.consumeAll();
        AtomicReference<Throwable> error = new AtomicReference<>();
        consumer(
                createTubeConfig(),
                all,
                (event) -> {
                    if (event instanceof Message msg) {
                        message = msg;
                        if (returnedEvent.get() == null) {
                            returnedEvent.set(msg);
                            return false;
                        }
                    }
                    try {
                        callTaskManagerProcess(event);
                    } catch (A2AServerException e) {
                        error.set(e);
                        return false;
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
        return consumeAndBreakOnInterrupt(consumer, blocking, null);
    }

    public EventTypeAndInterrupt consumeAndBreakOnInterrupt(EventConsumer consumer, boolean blocking, Runnable eventCallback) throws JSONRPCError {
        Flow.Publisher<Event> all = consumer.consumeAll();
        AtomicReference<Message> message = new AtomicReference<>();
        AtomicBoolean interrupted = new AtomicBoolean(false);
        AtomicBoolean shouldCloseConsumer = new AtomicBoolean(false);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CompletableFuture<Void> completionFuture = new CompletableFuture<>();

        // CRITICAL: The subscription itself must run on a background thread to avoid blocking
        // the Vert.x worker thread. EventConsumer.consumeAll() starts a polling loop that
        // blocks in dequeueEvent(), so we must subscribe from a background thread.
        // Use the @Internal executor (not ForkJoinPool.commonPool) to avoid saturation
        // during concurrent request bursts.
        CompletableFuture.runAsync(() -> {
            consumer(
                createTubeConfig(),
                all,
                (event) -> {
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

                    // Process event through TaskManager
                    try {
                        callTaskManagerProcess(event);
                    } catch (A2AServerException e) {
                        errorRef.set(e);
                        completionFuture.completeExceptionally(e);
                        return false;
                    }

                    // Determine interrupt behavior
                    boolean shouldInterrupt = false;
                    boolean continueInBackground = false;
                    boolean isAuthRequired = (event instanceof Task task && task.getStatus().state() == TaskState.AUTH_REQUIRED)
                            || (event instanceof TaskStatusUpdateEvent tsue && tsue.getStatus().state() == TaskState.AUTH_REQUIRED);

                    // Always interrupt on auth_required, as it needs external action.
                    if (isAuthRequired) {
                        // auth-required is a special state: the message should be
                        // escalated back to the caller, but the agent is expected to
                        // continue producing events once the authorization is received
                        // out-of-band. This is in contrast to input-required, where a
                        // new request is expected in order for the agent to make progress,
                        // so the agent should exit.
                        shouldInterrupt = true;
                        continueInBackground = true;
                    }
                    // For non-blocking calls, interrupt as soon as a task is available.
                    else if (!blocking) {
                        shouldInterrupt = true;
                        continueInBackground = true;
                    }
                    // For blocking calls, interrupt when we get any task-related event (EventKind except Message)
                    // Cancel subscription to free resources (client can resubscribe if needed)
                    else {
                        // Note: The initial version of this change only triggered this block with
                        //  else if (blocking && (event instanceof EventKind && !(event instanceof Message)))
                        // However, a review comment indicated this might not be necessary, and everything passes
                        // with this simpler catch-all else block. Keeping this comment here for a while (added
                        // 9 Oct 2025) just in case.
                        shouldInterrupt = true;
                        continueInBackground = false;
                    }

                    if (shouldInterrupt) {
                        // Complete the future to unblock the main thread
                        interrupted.set(true);
                        completionFuture.complete(null);

                        if (continueInBackground) {
                            // Continue consuming in background - keep requesting events
                            return true;
                        } else {
                            // Blocking call - cancel subscription AND close consumer to stop polling loop
                            // We need to close the consumer after the consumer() call completes
                            shouldCloseConsumer.set(true);
                            return false;
                        }
                    }

                    // Continue processing
                    return true;
                },
                throwable -> {
                    // Handle onError and onComplete
                    if (throwable != null) {
                        errorRef.set(throwable);
                        completionFuture.completeExceptionally(throwable);
                    } else {
                        // onComplete
                        completionFuture.complete(null);
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

        // Close consumer if blocking interrupt occurred
        if (shouldCloseConsumer.get()) {
            // Close the EventConsumer's queue to stop the infinite polling loop
            // This will cause EventQueueClosedException and exit the while(true) loop
            consumer.close();
        }

        // Background consumption continues automatically via the subscription
        // No need to create a new publisher - returning true in nextFunction
        // keeps the subscription alive

        Throwable error = errorRef.get();
        if (error != null) {
            Utils.rethrow(error);
        }

        return new EventTypeAndInterrupt(
                message.get() != null ? message.get() : taskManager.getTask(), interrupted.get());
    }

    private void callTaskManagerProcess(Event event) throws A2AServerException {
        taskManager.process(event);
    }

    public record EventTypeAndInterrupt(EventKind eventType, boolean interrupted) {

    }
}
