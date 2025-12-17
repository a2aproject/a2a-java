package io.a2a.server.events;

import java.util.concurrent.Flow;

import org.jspecify.annotations.Nullable;

import io.a2a.spec.A2AServerException;
import io.a2a.spec.Event;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskStatusUpdateEvent;
import mutiny.zero.BackpressureStrategy;
import mutiny.zero.TubeConfiguration;
import mutiny.zero.ZeroPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventConsumer.class);
    private final EventQueue queue;
    private volatile @Nullable Throwable error;

    private static final String ERROR_MSG = "Agent did not return any response";
    private static final int NO_WAIT = -1;
    private static final int QUEUE_WAIT_MILLISECONDS = 500;

    public EventConsumer(EventQueue queue) {
        this.queue = queue;
        LOGGER.debug("EventConsumer created with queue {}", System.identityHashCode(queue));
    }

    public Event consumeOne() throws A2AServerException, EventQueueClosedException {
        EventQueueItem item = queue.dequeueEventItem(NO_WAIT);
        if (item == null) {
            throw new A2AServerException(ERROR_MSG, new InternalError(ERROR_MSG));
        }
        return item.getEvent();
    }

    public Flow.Publisher<EventQueueItem> consumeAll() {
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
                    EventQueueItem item;
                    Event event;
                    try {
                        item = queue.dequeueEventItem(QUEUE_WAIT_MILLISECONDS);
                        if (item == null) {
                            continue;
                        }
                        event = item.getEvent();

                        if (event instanceof Throwable thr) {
                            tube.fail(thr);
                            return;
                        }

                        // Check for QueueClosedEvent BEFORE sending to avoid delivering it to subscribers
                        boolean isFinalEvent = false;
                        if (event instanceof TaskStatusUpdateEvent tue && tue.isFinal()) {
                            isFinalEvent = true;
                        } else if (event instanceof Message) {
                            isFinalEvent = true;
                        } else if (event instanceof Task task) {
                            isFinalEvent = task.status().state().isFinal();
                        } else if (event instanceof QueueClosedEvent) {
                            // Poison pill event - signals queue closure from remote node
                            // Do NOT send to subscribers - just close the queue
                            LOGGER.debug("Received QueueClosedEvent for task {}, treating as final event",
                                ((QueueClosedEvent) event).getTaskId());
                            isFinalEvent = true;
                        }

                        // Only send event if it's not a QueueClosedEvent
                        // QueueClosedEvent is an internal coordination event used for replication
                        // and should not be exposed to API consumers
                        if (!(event instanceof QueueClosedEvent)) {
                            tube.send(item);
                        }

                        if (isFinalEvent) {
                            LOGGER.debug("Final event detected, closing queue and breaking loop for queue {}", System.identityHashCode(queue));
                            queue.close();
                            LOGGER.debug("Queue closed, breaking loop for queue {}", System.identityHashCode(queue));
                            break;
                        }
                    } catch (EventQueueClosedException e) {
                        completed = true;
                        tube.complete();
                        return;
                    } catch (Throwable t) {
                        tube.fail(t);
                        return;
                    }
                }
            } finally {
                if (!completed) {
                    LOGGER.debug("EventConsumer finally block: calling tube.complete() for queue {}", System.identityHashCode(queue));
                    tube.complete();
                    LOGGER.debug("EventConsumer finally block: tube.complete() returned for queue {}", System.identityHashCode(queue));
                } else {
                    LOGGER.debug("EventConsumer finally block: completed=true, skipping tube.complete() for queue {}", System.identityHashCode(queue));
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

    public void close() {
        // Close the queue to stop the polling loop in consumeAll()
        // This will cause EventQueueClosedException and exit the while(true) loop
        LOGGER.debug("EventConsumer closing queue {}", System.identityHashCode(queue));
        queue.close();
    }
}
