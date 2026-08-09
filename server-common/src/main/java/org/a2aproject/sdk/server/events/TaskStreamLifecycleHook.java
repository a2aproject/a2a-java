package org.a2aproject.sdk.server.events;

import org.a2aproject.sdk.spec.Event;

/**
 * Hook for observing task stream lifecycle events and controlling stream resources.
 * <p>
 * Implementations are notified when clients subscribe/unsubscribe to a task's event stream
 * and when events are processed for a task. The {@link StreamCloseHandle} passed to each
 * callback can be used to gracefully close all active streams for the task.
 * </p>
 *
 * <h2>Ordering guarantees</h2>
 * <ul>
 *   <li>{@code onSubscribe} is called synchronously during {@code MainQueue.tap()}, after
 *       the ChildQueue has been added to the children list but before the ChildQueue is
 *       returned to the caller. The subscriber count visible via
 *       {@link StreamCloseHandle#getActiveSubscriberCount()} includes the new subscriber.</li>
 *   <li>{@code onEvent} is called on the {@code MainEventBusProcessor} thread <em>after</em>
 *       the event has been persisted and distributed to all ChildQueues.
 *       Implementations must return promptly to avoid stalling event distribution
 *       for other tasks.</li>
 *   <li>Because {@code onSubscribe} and {@code onEvent} run on different threads, a fast
 *       event emission may cause {@code onEvent} to fire concurrently with or even before
 *       {@code onSubscribe} returns. Stateful hook implementations must be thread-safe.</li>
 *   <li>{@code onUnsubscribe} is called synchronously inside {@code ChildQueue.close()},
 *       on whichever thread closes the child (EventConsumer, hook via
 *       {@link StreamCloseHandle#closeStreams()}, or the client's transport layer).</li>
 * </ul>
 *
 * <h2>Hook binding lifetime</h2>
 * <p>
 * The hook is set on a MainQueue when it is first created (in
 * {@code InMemoryQueueManager.createOrTap()}). If the MainQueue already exists (e.g., a
 * second client subscribes to the same task), the existing hook reference is retained.
 * The hook instance is effectively bound for the lifetime of the MainQueue.
 * </p>
 * <p>
 * The default implementation is a no-op. To provide custom behavior (e.g., closing streams
 * after a timeout), implement this interface and register it as a CDI alternative:
 * </p>
 * <pre>{@code
 * @ApplicationScoped
 * @Alternative
 * @Priority(1)
 * public class MyStreamHook implements TaskStreamLifecycleHook {
 *     // ...
 * }
 * }</pre>
 *
 * @see StreamCloseHandle
 */
public interface TaskStreamLifecycleHook {

    /**
     * Called when a new ChildQueue is created for a task (a client subscribes to the stream).
     *
     * <p>If this method calls {@link StreamCloseHandle#closeStreams()}, the ChildQueue being
     * created will be closed before {@code tap()} returns it to the caller. The caller will
     * receive a closed queue whose {@code dequeueEventItem()} throws
     * {@code EventQueueClosedException} immediately.</p>
     *
     * @param taskId the task identifier
     * @param handle handle to close streams and query subscriber count
     */
    void onSubscribe(String taskId, StreamCloseHandle handle);

    /**
     * Called when a ChildQueue closes for a task (a client disconnects or streams are closed).
     *
     * <p>Calling {@link StreamCloseHandle#closeStreams()} from within this callback is safe
     * but has no effect — a reentrancy guard prevents recursive iteration over the children
     * list. To close remaining streams in response to an unsubscription, schedule the call
     * asynchronously or handle it in {@link #onEvent}.</p>
     *
     * @param taskId the task identifier
     * @param handle handle to close streams and query subscriber count
     */
    void onUnsubscribe(String taskId, StreamCloseHandle handle);

    /**
     * Called after an event has been persisted and distributed to all ChildQueues.
     *
     * @param taskId the task identifier
     * @param event the event that was processed
     * @param handle handle to close streams and query subscriber count
     */
    void onEvent(String taskId, Event event, StreamCloseHandle handle);
}
