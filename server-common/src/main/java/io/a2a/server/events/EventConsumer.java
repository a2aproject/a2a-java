package io.a2a.server.events;

import java.util.concurrent.Flow;

import io.a2a.spec.A2AError;
import io.a2a.spec.A2AServerException;
import io.a2a.spec.Event;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatusUpdateEvent;
import mutiny.zero.BackpressureStrategy;
import mutiny.zero.TubeConfiguration;
import mutiny.zero.ZeroPublisher;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventConsumer.class);
    private final EventQueue queue;
    private volatile @Nullable Throwable error;
    private volatile boolean cancelled = false;
    private volatile boolean agentCompleted = false;
    private volatile int pollTimeoutsAfterAgentCompleted = 0;

    private static final String ERROR_MSG = "Agent did not return any response";
    private static final int NO_WAIT = -1;
    private static final int QUEUE_WAIT_MILLISECONDS = 500;
    // In replicated scenarios, events can arrive hundreds of milliseconds after local agent completes
    // Grace period allows Kafka replication to deliver late-arriving events
    // 3 timeouts * 500ms = 1500ms grace period for replication delays
    private static final int MAX_POLL_TIMEOUTS_AFTER_AGENT_COMPLETED = 3;

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
                    // Check if cancelled by client disconnect
                    if (cancelled) {
                        LOGGER.debug("EventConsumer detected cancellation, exiting polling loop for queue {}", System.identityHashCode(queue));
                        completed = true;
                        tube.complete();
                        return;
                    }

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
                        LOGGER.debug("EventConsumer polling queue {} (error={}, agentCompleted={})",
                            System.identityHashCode(queue), error, agentCompleted);
                        item = queue.dequeueEventItem(QUEUE_WAIT_MILLISECONDS);
                        if (item == null) {
                            int queueSize = queue.size();
                            LOGGER.debug("EventConsumer poll timeout (null item), agentCompleted={}, queue.size()={}, timeoutCount={}",
                                agentCompleted, queueSize, pollTimeoutsAfterAgentCompleted);
                            // If agent completed, a poll timeout means no more events are coming
                            // MainEventBusProcessor has 500ms to distribute events from MainEventBus
                            // If we timeout with agentCompleted=true, all events have been distributed
                            //
                            // IMPORTANT: In replicated scenarios, remote events may arrive AFTER local agent completes!
                            // Use grace period to allow for Kafka replication delays (can be 400-500ms)
                            if (agentCompleted && queueSize == 0) {
                                pollTimeoutsAfterAgentCompleted++;
                                if (pollTimeoutsAfterAgentCompleted >= MAX_POLL_TIMEOUTS_AFTER_AGENT_COMPLETED) {
                                    LOGGER.debug("Agent completed with {} consecutive poll timeouts and empty queue, closing for graceful completion (queue={})",
                                        pollTimeoutsAfterAgentCompleted, System.identityHashCode(queue));
                                    queue.close();
                                    completed = true;
                                    tube.complete();
                                    return;
                                } else {
                                    LOGGER.debug("Agent completed but grace period active ({}/{} timeouts), continuing to poll (queue={})",
                                        pollTimeoutsAfterAgentCompleted, MAX_POLL_TIMEOUTS_AFTER_AGENT_COMPLETED, System.identityHashCode(queue));
                                }
                            } else if (agentCompleted && queueSize > 0) {
                                LOGGER.debug("Agent completed but queue has {} pending events, resetting timeout counter and continuing to poll (queue={})",
                                    queueSize, System.identityHashCode(queue));
                                pollTimeoutsAfterAgentCompleted = 0; // Reset counter when events arrive
                            }
                            continue;
                        }
                        // Event received - reset timeout counter
                        pollTimeoutsAfterAgentCompleted = 0;
                        event = item.getEvent();
                        LOGGER.debug("EventConsumer received event: {} (queue={})",
                            event.getClass().getSimpleName(), System.identityHashCode(queue));

                        // Defensive logging for error handling
                        if (event instanceof Throwable thr) {
                            LOGGER.debug("EventConsumer detected Throwable event: {} - triggering tube.fail()",
                                    thr.getClass().getSimpleName());
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
                            isFinalEvent = isStreamTerminatingTask(task);
                        } else if (event instanceof QueueClosedEvent) {
                            // Poison pill event - signals queue closure from remote node
                            // Do NOT send to subscribers - just close the queue
                            LOGGER.debug("Received QueueClosedEvent for task {}, treating as final event",
                                ((QueueClosedEvent) event).getTaskId());
                            isFinalEvent = true;
                        } else if (event instanceof A2AError) {
                            // A2AError events are terminal - they trigger automatic FAILED state transition
                            LOGGER.debug("Received A2AError event, treating as final event");
                            isFinalEvent = true;
                        }

                        // Only send event if it's not a QueueClosedEvent
                        // QueueClosedEvent is an internal coordination event used for replication
                        // and should not be exposed to API consumers
                        boolean isFinalSent = false;
                        if (!(event instanceof QueueClosedEvent)) {
                            tube.send(item);
                            isFinalSent = isFinalEvent;
                        }

                        if (isFinalEvent) {
                            LOGGER.debug("Final or interrupted event detected, closing queue and breaking loop for queue {}", System.identityHashCode(queue));
                            queue.close();
                            LOGGER.debug("Queue closed, breaking loop for queue {}", System.identityHashCode(queue));

                            // CRITICAL: Allow tube buffer to flush before calling tube.complete()
                            // tube.send() buffers events asynchronously. If we call tube.complete() immediately,
                            // the stream-end signal can reach the client BEFORE the buffered final event,
                            // causing the client to close the connection and never receive the final event.
                            // This is especially important in replicated scenarios where events arrive via Kafka
                            // and timing is less deterministic. A small delay ensures the buffer flushes.
                            if (isFinalSent) {
                                try {
                                    Thread.sleep(50);  // 50ms to allow SSE buffer flush
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            }
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

    /**
     * Determines if a task is in a state for terminating the stream.
     * <p>A task is terminating if:</p>
     * <ul>
     *   <li>Its state is final (e.g., completed, canceled, rejected, failed), OR</li>
     *   <li>Its state is interrupted (e.g., input-required)</li>
     * </ul>
     * @param task the task to check
     * @return true if the task has a final state or an interrupted state, false otherwise
     */
    private boolean isStreamTerminatingTask(Task task) {
        TaskState state = task.status().state();
        return state.isFinal() || state == TaskState.TASK_STATE_INPUT_REQUIRED;
    }

    public EnhancedRunnable.DoneCallback createAgentRunnableDoneCallback() {
        return agentRunnable -> {
            LOGGER.debug("EventConsumer: Agent done callback invoked (hasError={}, queue={})",
                agentRunnable.getError() != null, System.identityHashCode(queue));
            if (agentRunnable.getError() != null) {
                error = agentRunnable.getError();
                LOGGER.debug("EventConsumer: Set error field from agent callback");
            } else {
                agentCompleted = true;
                LOGGER.debug("EventConsumer: Agent completed successfully, set agentCompleted=true, will close queue after draining");
            }
        };
    }

    public void cancel() {
        // Set cancellation flag to stop polling loop
        // Called when client disconnects without completing stream
        LOGGER.debug("EventConsumer cancelled (client disconnect), stopping polling for queue {}", System.identityHashCode(queue));
        cancelled = true;
    }

    public void close() {
        // Close the queue to stop the polling loop in consumeAll()
        // This will cause EventQueueClosedException and exit the while(true) loop
        LOGGER.debug("EventConsumer closing queue {}", System.identityHashCode(queue));
        queue.close();
    }
}
