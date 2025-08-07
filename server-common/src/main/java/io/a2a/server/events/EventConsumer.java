package io.a2a.server.events;

import java.util.concurrent.Flow;

import io.a2a.spec.A2AServerException;
import io.a2a.spec.Event;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskStatusUpdateEvent;
import mutiny.zero.BackpressureStrategy;
import mutiny.zero.TubeConfiguration;
import mutiny.zero.ZeroPublisher;

public class EventConsumer {
    private final EventQueue queue;
    private Throwable error;

    private static final String ERROR_MSG = "Agent did not return any response";
    private static final int NO_WAIT = -1;
    private static final int QUEUE_WAIT_MILLISECONDS = 500;

    public EventConsumer(EventQueue queue) {
        this.queue = queue;
    }

    public Event consumeOne() throws A2AServerException, EventQueueClosedException {
        Event event = queue.dequeueEvent(NO_WAIT);
        if (event == null) {
            throw new A2AServerException(ERROR_MSG, new InternalError(ERROR_MSG));
        }
        return event;
    }

    public Flow.Publisher<Event> consumeAll() {
        TubeConfiguration conf = new TubeConfiguration()
                .withBackpressureStrategy(BackpressureStrategy.BUFFER)
                .withBufferSize(256);
        return ZeroPublisher.create(conf, tube -> {
            boolean completed = false;
            try {
                while (true) {
                    if (error != null) {
                        completed = true;
                        tube.fail(error);
                        return;
                    }
                    // We use a timeout when waiting for an event from the queue.
                    // This is required because it allows the loop to check if
                    // `self._exception` has been set by the `agent_task_callback`.
                    // Without the timeout, loop might hang indefinitely if no events are
                    // enqueued by the agent and the agent simply threw an exception

                    // TODO the callback mentioned above seems unused in the Python 0.2.1 tag
                    Event event;
                    try {
                        event = queue.dequeueEvent(QUEUE_WAIT_MILLISECONDS);
                        if (event == null) {
                            continue;
                        }
                        if (event instanceof Throwable thr) {
                            tube.fail(thr);
                            return;
                        }
                        tube.send(event);
                    } catch (EventQueueClosedException e) {
                        completed = true;
                        tube.complete();
                        return;
                    } catch (Throwable t) {
                        tube.fail(t);
                        return;
                    }

                    boolean isFinalEvent = false;
                    if (event instanceof TaskStatusUpdateEvent tue && tue.isFinal()) {
                        isFinalEvent = true;
                    } else if (event instanceof Message) {
                        isFinalEvent = true;
                    } else if (event instanceof Task task) {
                        switch (task.getStatus().state()) {
                            case COMPLETED:
                            case CANCELED:
                            case FAILED:
                            case REJECTED:
                            case UNKNOWN:
                                isFinalEvent = true;
                        }
                    }

                    if (isFinalEvent) {
                        queue.close();
                        break;
                    }
                }
            } finally {
                if (!completed) {
                    tube.complete();
                }
            }
        });
    }

    public EnhancedRunnable.DoneCallback createAgentRunnableDoneCallback() {
        return agentRunnable -> {
            if (agentRunnable.getError() != null) {
                error = agentRunnable.getError();
            }
        };
    }
}
