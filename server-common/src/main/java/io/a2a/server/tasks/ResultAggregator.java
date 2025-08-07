package io.a2a.server.tasks;

import static io.a2a.server.util.async.AsyncUtils.consumer;
import static io.a2a.server.util.async.AsyncUtils.createTubeConfig;
import static io.a2a.server.util.async.AsyncUtils.processor;

import java.util.concurrent.CompletableFuture;
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
    private volatile Message message;

    public ResultAggregator(TaskManager taskManager, Message message) {
        this.taskManager = taskManager;
        this.message = message;
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
            callTaskManagerProcess(event);
            return true;
        }));
    }

    public EventKind consumeAll(EventConsumer consumer) {
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
                    callTaskManagerProcess(event);
                    return true;
                },
                error::set);

        if (returnedEvent.get() != null) {
            return returnedEvent.get();
        }
        return taskManager.getTask();
    }

    public EventTypeAndInterrupt consumeAndBreakOnInterrupt(EventConsumer consumer) throws JSONRPCError {
        Flow.Publisher<Event> all = consumer.consumeAll();
        AtomicReference<Message> message = new AtomicReference<>();
        AtomicBoolean interrupted = new AtomicBoolean(false);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        consumer(
                createTubeConfig(),
                all,
                (event -> {
                    if (event instanceof Throwable t) {
                        errorRef.set(t);
                        return false;
                    }
                    if (event instanceof Message msg) {
                        this.message = msg;
                        message.set(msg);
                        return false;
                    }

                    callTaskManagerProcess(event);

                    if ((event instanceof Task task && task.getStatus().state() == TaskState.AUTH_REQUIRED)
                            || (event instanceof TaskStatusUpdateEvent tsue && tsue.getStatus().state() == TaskState.AUTH_REQUIRED)) {
                        // auth-required is a special state: the message should be
                        // escalated back to the caller, but the agent is expected to
                        // continue producing events once the authorization is received
                        // out-of-band. This is in contrast to input-required, where a
                        // new request is expected in order for the agent to make progress,
                        // so the agent should exit.

                        CompletableFuture.runAsync(() -> continueConsuming(all));
                        interrupted.set(true);
                        return false;
                    }
                    return true;
                }),
                errorRef::set);

        Throwable error = errorRef.get();
        if (error != null) {
            Utils.rethrow(error);
        }

        return new EventTypeAndInterrupt(
                message.get() != null ? message.get() : taskManager.getTask(), interrupted.get());
    }

    private void continueConsuming(Flow.Publisher<Event> all) {
        consumer(createTubeConfig(),
                all,
                event -> {
                    callTaskManagerProcess(event);
                    return true;
                },
                t -> {});
    }

    private void callTaskManagerProcess(Event event) {
         try {
            taskManager.process(event);
        } catch (A2AServerException e) {
            // TODO Decide what to do in case of failure
            e.printStackTrace();
        }
    }

    public record EventTypeAndInterrupt(EventKind eventType, boolean interrupted) {

    }
}
