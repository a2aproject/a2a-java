package org.a2aproject.sdk.server.events;

import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.A2AServerException;
import org.a2aproject.sdk.spec.Event;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;
import mutiny.zero.BackpressureStrategy;
import mutiny.zero.TubeConfiguration;
import mutiny.zero.ZeroPublisher;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventConsumer.class);
    private final EventQueue queue;
    private final Executor executor;
    private volatile @Nullable Throwable error;
    private volatile boolean cancelled = false;
    private volatile boolean agentCompleted = false;
    private volatile int pollTimeoutsAfterAgentCompleted = 0;
    private volatile @Nullable TaskState lastSeenTaskState = null;
    private volatile int pollTimeoutsWhileAwaitingFinal = 0;

    private static final String ERROR_MSG = "Agent did not return any response";
    private static final int NO_WAIT = -1;
    private static final int QUEUE_WAIT_MILLISECONDS = 500;
    // In replicated scenarios, events can arrive hundreds of milliseconds after local agent completes
    // Grace period allows Kafka replication to deliver late-arriving events
    // Calculation: MAX_POLL_TIMEOUTS_AFTER_AGENT_COMPLETED * QUEUE_WAIT_MILLISECONDS = 1500ms
    private static final int MAX_POLL_TIMEOUTS_AFTER_AGENT_COMPLETED = 3;
    // Maximum time to wait for final event when awaitingFinalEvent is set
    // If event doesn't arrive after this many timeouts, assume it won't arrive
    // Calculation uses ceiling division to ensure timeout is at least MAX_AWAITING_FINAL_TIMEOUT_MS
    private static final int MAX_AWAITING_FINAL_TIMEOUT_MS = 3000;
    private static final int MAX_POLL_TIMEOUTS_AWAITING_FINAL =
        (MAX_AWAITING_FINAL_TIMEOUT_MS + QUEUE_WAIT_MILLISECONDS - 1) / QUEUE_WAIT_MILLISECONDS;
    // WORKAROUND: Sleep delay to allow SSE buffer flush before stream completion
    // This is a temporary workaround for a race condition where tube.complete() can arrive
    // before the final event is flushed from the SSE buffer. Ideally, this should be handled
    // at the transport layer (e.g., MultiSseSupport) with proper write completion callbacks.
    // TODO: Move buffer flush handling to transport layer to avoid this latency penalty
    private static final int BUFFER_FLUSH_DELAY_MS = 150;

    public EventConsumer(EventQueue queue, Executor executor) {
        this.queue = queue;
        this.executor = executor;
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
            // CRITICAL: Spawn polling loop on executor to avoid blocking the calling thread
            // The lambda returns immediately, but polling continues on separate thread
            executor.execute(() -> {
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
                                boolean awaitingFinal = queue.isAwaitingFinalEvent();
                                LOGGER.debug("EventConsumer poll timeout (null item), agentCompleted={}, queue.size()={}, awaitingFinalEvent={}, timeoutCount={}, awaitingTimeoutCount={}",
                                    agentCompleted, queueSize, awaitingFinal, pollTimeoutsAfterAgentCompleted, pollTimeoutsWhileAwaitingFinal);
                                // If agent completed, a poll timeout means no more events are coming
                                // MainEventBusProcessor has 500ms to distribute events from MainEventBus
                                // If we timeout with agentCompleted=true, all events have been distributed
                                //
                                // IMPORTANT: In replicated scenarios, remote events may arrive AFTER local agent completes!
                                // Use grace period to allow for Kafka replication delays (can be 400-500ms)
                                //
                                // CRITICAL: Do NOT close if task is in interrupted state (INPUT_REQUIRED, AUTH_REQUIRED)
                                // Per A2A spec, interrupted states are NOT terminal - the stream must stay open
                                // for future state updates even after agent completes (agent will be re-invoked later).
                                //
                                // CRITICAL: Don't start timeout counter if we're awaiting a final event.
                                // The awaitingFinalEvent flag is set when MainQueue enqueues a final event
                                // but it hasn't been distributed to this ChildQueue yet.
                                // HOWEVER: If we've been waiting too long for the final event (>3s), give up and
                                // proceed with normal timeout logic to prevent infinite waiting.
                                boolean isInterruptedState = lastSeenTaskState != null && lastSeenTaskState.isInterrupted();

                                // Track how long we've been waiting for the final event.
                                // Three cases for the awaiting counter:
                                //   awaitingFinal && queueSize == 0: final event enqueued in MainQueue but not yet
                                //     distributed here — increment timeout counter and give up after MAX timeout.
                                //   awaitingFinal && queueSize > 0: events are still in transit, do nothing —
                                //     the counter is reset below once an event is successfully dequeued.
                                //   !awaitingFinal: not waiting for anything — reset the counter (timeout case;
                                //     the successful-dequeue reset happens below at the event-received path).
                                if (awaitingFinal && queueSize == 0) {
                                    pollTimeoutsWhileAwaitingFinal++;
                                    if (pollTimeoutsWhileAwaitingFinal >= MAX_POLL_TIMEOUTS_AWAITING_FINAL) {
                                        LOGGER.debug("Waited {} timeouts for final event but it hasn't arrived - proceeding with normal timeout logic (queue={})",
                                            pollTimeoutsWhileAwaitingFinal, System.identityHashCode(queue));
                                        // Clear the flag on the queue itself, not just the local variable
                                        queue.clearAwaitingFinalEvent();
                                        awaitingFinal = false; // Also update local variable for this iteration
                                    }
                                } else if (!awaitingFinal) {
                                    // Poll timed out and we are not awaiting a final event: reset the counter.
                                    // (The successful-dequeue reset is handled separately below.)
                                    pollTimeoutsWhileAwaitingFinal = 0;
                                }

                                if (agentCompleted && queueSize == 0 && !isInterruptedState && !awaitingFinal) {
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
                                } else if (agentCompleted && isInterruptedState) {
                                    LOGGER.debug("Agent completed but task is in interrupted state ({}), stream must remain open (queue={})",
                                        lastSeenTaskState, System.identityHashCode(queue));
                                    pollTimeoutsAfterAgentCompleted = 0; // Reset counter
                                } else if (agentCompleted && queueSize > 0) {
                                    LOGGER.debug("Agent completed but queue has {} pending events, resetting timeout counter and continuing to poll (queue={})",
                                        queueSize, System.identityHashCode(queue));
                                    pollTimeoutsAfterAgentCompleted = 0; // Reset counter when events arrive
                                } else if (agentCompleted && awaitingFinal) {
                                    LOGGER.debug("Agent completed, awaiting final event (timeout {}/{}), continuing to poll (queue={})",
                                        pollTimeoutsWhileAwaitingFinal, MAX_POLL_TIMEOUTS_AWAITING_FINAL, System.identityHashCode(queue));
                                    pollTimeoutsAfterAgentCompleted = 0; // Reset counter while awaiting final
                                }
                                continue;
                            }
                            // Event received - reset timeout counters
                            pollTimeoutsAfterAgentCompleted = 0;
                            pollTimeoutsWhileAwaitingFinal = 0;
                            event = item.getEvent();
                            LOGGER.debug("EventConsumer received event: {} (queue={})",
                                event.getClass().getSimpleName(), System.identityHashCode(queue));

                            // Track the latest task state for grace period logic
                            if (event instanceof Task task) {
                                lastSeenTaskState = task.status().state();
                            } else if (event instanceof TaskStatusUpdateEvent tue) {
                                lastSeenTaskState = tue.status().state();
                            }

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
                                // and timing is less deterministic.
                                if (isFinalSent) {
                                    try {
                                        Thread.sleep(BUFFER_FLUSH_DELAY_MS);
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
                            completed = true;
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
            // Lambda returns immediately - polling continues on executor thread
        });
    }

    /**
     * Determines if a task is in a state for terminating the stream.
     * <p>
     * Per A2A Protocol Specification 3.1.6 (SubscribeToTask):
     * "The stream MUST terminate when the task reaches a terminal state
     * (completed, failed, canceled, or rejected)."
     * <p>
     * Interrupted states (INPUT_REQUIRED, AUTH_REQUIRED) are NOT terminal.
     * The stream should remain open to deliver future state updates when
     * the task resumes after receiving the required input or authorization.
     *
     * @param task the task to check
     * @return true if the task has a terminal/final state, false otherwise
     */
    private boolean isStreamTerminatingTask(Task task) {
        TaskState state = task.status().state();
        return state.isFinal();
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
