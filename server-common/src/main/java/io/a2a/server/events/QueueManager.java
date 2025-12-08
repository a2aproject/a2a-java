package io.a2a.server.events;

import org.jspecify.annotations.Nullable;

/**
 * Manages {@link EventQueue} lifecycle for task-based event routing and consumption.
 * <p>
 * The QueueManager is responsible for creating, storing, and managing event queues that
 * coordinate asynchronous communication between agent executors (producers) and transport
 * consumers. It supports both simple in-memory queuing and sophisticated patterns like
 * queue tapping for resubscription and distributed event replication.
 * </p>
 *
 * <h2>Queue Architecture</h2>
 * <ul>
 *   <li><b>MainQueue:</b> Primary queue for a task, created by {@link #createOrTap(String)}</li>
 *   <li><b>ChildQueue:</b> Subscriber view created by {@link #tap(String)}, receives copies of parent events</li>
 *   <li>One MainQueue per task, multiple ChildQueues for concurrent consumers (resubscription, cancellation)</li>
 *   <li>Events enqueued to MainQueue are automatically distributed to all active ChildQueues</li>
 * </ul>
 *
 * <h2>Queue Lifecycle</h2>
 * <ol>
 *   <li><b>Creation:</b> {@link #createOrTap(String)} creates MainQueue for new task or taps existing for resubscription</li>
 *   <li><b>Population:</b> Agent enqueues events to MainQueue via {@link EventQueue#enqueueEvent(io.a2a.spec.Event)}</li>
 *   <li><b>Distribution:</b> Events automatically copied to all ChildQueues</li>
 *   <li><b>Consumption:</b> Consumers poll events from their queues (Main or Child)</li>
 *   <li><b>Closure:</b> Queue closes on final event (COMPLETED/FAILED/CANCELED) or explicit close</li>
 *   <li><b>Cleanup:</b> MainQueue removed from manager when all ChildQueues close and task is finalized</li>
 * </ol>
 *
 * <h2>Default Implementation</h2>
 * {@link InMemoryQueueManager} provides the standard implementation:
 * <ul>
 *   <li>Stores queues in thread-safe {@link java.util.concurrent.ConcurrentHashMap}</li>
 *   <li>Integrates with {@link io.a2a.server.tasks.TaskStateProvider} for cleanup decisions</li>
 *   <li>Removes queues when tasks enter final state (COMPLETED/FAILED/CANCELED)</li>
 *   <li>Supports queue tapping for resubscription scenarios</li>
 * </ul>
 *
 * <h2>Alternative Implementations</h2>
 * <ul>
 *   <li><b>extras/queue-manager-replicated:</b> Kafka-based replication for multi-instance deployments</li>
 * </ul>
 * Replicated implementations enable event distribution across server instances for high
 * availability and load balancing.
 *
 * <h2>Tapping Pattern (Resubscription)</h2>
 * Tapping creates a ChildQueue that receives future events from an ongoing task:
 * <pre>{@code
 * // Client disconnects and later reconnects
 * EventQueue childQueue = queueManager.tap(taskId);
 * if (childQueue != null) {
 *     // Receive events from this point forward
 *     // (Historical events before tap are not replayed)
 * }
 * }</pre>
 * Use cases:
 * <ul>
 *   <li>Resubscribing to ongoing tasks after disconnect</li>
 *   <li>Canceling tasks while still receiving status updates</li>
 *   <li>Multiple concurrent consumers of the same task</li>
 * </ul>
 *
 * <h2>CDI Extension Pattern</h2>
 * <pre>{@code
 * @ApplicationScoped
 * @Alternative
 * @Priority(50)  // Higher than default InMemoryQueueManager
 * public class KafkaQueueManager implements QueueManager {
 *     // Custom implementation with event replication
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * All methods must be thread-safe. Multiple threads may call {@code createOrTap()},
 * {@code tap()}, and {@code close()} concurrently for different tasks.
 *
 * @see EventQueue
 * @see InMemoryQueueManager
 * @see io.a2a.server.tasks.TaskStateProvider
 * @see io.a2a.server.requesthandlers.DefaultRequestHandler
 */
public interface QueueManager {

    /**
     * Adds a queue to the manager with the given task ID.
     * <p>
     * Throws {@link TaskQueueExistsException} if a queue already exists for this task.
     * Typically used internally - prefer {@link #createOrTap(String)} for most use cases.
     * </p>
     *
     * @param taskId the task identifier
     * @param queue the queue to add
     * @throws TaskQueueExistsException if queue already exists for this task ID
     */
    void add(String taskId, EventQueue queue);

    /**
     * Retrieves the MainQueue for a task, if it exists.
     * <p>
     * Returns the primary queue for the task. Does not create a new queue if none exists.
     * </p>
     *
     * @param taskId the task identifier
     * @return the MainQueue, or null if no queue exists for this task
     */
    @Nullable EventQueue get(String taskId);

    /**
     * Creates a ChildQueue that receives copies of events from the MainQueue.
     * <p>
     * Use this for:
     * <ul>
     *   <li>Resubscribing to an ongoing task (receive future events)</li>
     *   <li>Canceling a task while still receiving status updates</li>
     *   <li>Multiple concurrent consumers of the same task</li>
     * </ul>
     * <p>
     * The ChildQueue receives events enqueued AFTER it's created. Historical events
     * are not replayed.
     *
     * @param taskId the task identifier
     * @return a ChildQueue that receives future events, or null if the MainQueue doesn't exist
     */
    @Nullable EventQueue tap(String taskId);

    /**
     * Closes and removes the queue for a task.
     * <p>
     * This closes the MainQueue and all ChildQueues, then removes it from the manager.
     * Called during cleanup after task completion or error conditions.
     * </p>
     *
     * @param taskId the task identifier
     */
    void close(String taskId);

    /**
     * Creates a MainQueue if none exists, or taps the existing queue to create a ChildQueue.
     * <p>
     * This is the primary method used by {@link io.a2a.server.requesthandlers.DefaultRequestHandler}:
     * <ul>
     *   <li><b>New task:</b> Creates and returns a MainQueue</li>
     *   <li><b>Resubscription:</b> Taps existing MainQueue and returns a ChildQueue</li>
     * </ul>
     *
     * @param taskId the task identifier
     * @return a MainQueue (if new task) or ChildQueue (if tapping existing)
     */
    EventQueue createOrTap(String taskId);

    /**
     * Waits for the queue's consumer polling to start.
     * <p>
     * Used internally to ensure the consumer is ready before the agent starts
     * enqueueing events, avoiding race conditions where events might be enqueued
     * before the consumer begins polling.
     * </p>
     *
     * @param eventQueue the queue to wait for
     * @throws InterruptedException if interrupted while waiting
     */
    void awaitQueuePollerStart(EventQueue eventQueue) throws InterruptedException;

    /**
     * Returns an EventQueueBuilder for creating queues with task-specific configuration.
     * <p>
     * Implementations can override to provide custom queue configurations per task,
     * such as different capacities, hooks, or event processors.
     * </p>
     * <p>
     * Default implementation returns a standard builder with no customization.
     * </p>
     *
     * @param taskId the task ID for context (may be used to customize queue configuration)
     * @return a builder for creating event queues
     */
    default EventQueue.EventQueueBuilder getEventQueueBuilder(String taskId) {
        throw new UnsupportedOperationException(
            "QueueManager implementations must override getEventQueueBuilder() to provide MainEventBus"
        );
    }

    /**
     * Creates a base EventQueueBuilder with standard configuration for this QueueManager.
     * This method provides the foundation for creating event queues with proper configuration
     * (MainEventBus, TaskStateProvider, cleanup callbacks, etc.).
     * <p>
     * QueueManager implementations that use custom factories can call this method directly
     * to get the base builder without going through the factory (which could cause infinite
     * recursion if the factory delegates back to getEventQueueBuilder()).
     * </p>
     * <p>
     * Callers can then add additional configuration (hooks, callbacks) before building the queue.
     * </p>
     *
     * @param taskId the task ID for the queue
     * @return a builder with base configuration specific to this QueueManager implementation
     */
    default EventQueue.EventQueueBuilder createBaseEventQueueBuilder(String taskId) {
        throw new UnsupportedOperationException(
            "QueueManager implementations must override createBaseEventQueueBuilder() to provide MainEventBus"
        );
    }

    /**
     * Returns the number of active ChildQueues for a task.
     * <p>
     * Used for testing to verify reference counting and queue lifecycle management.
     * In production, indicates how many consumers are actively subscribed to a task's events.
     * </p>
     *
     * @param taskId the task ID
     * @return number of active child queues, or -1 if the MainQueue doesn't exist
     */
    int getActiveChildQueueCount(String taskId);
}
