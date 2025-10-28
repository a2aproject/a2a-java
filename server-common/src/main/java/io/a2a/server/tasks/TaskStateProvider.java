package io.a2a.server.tasks;

/**
 * Provider interface for determining the active state of a task.
 * <p>
 * This interface decouples the queue lifecycle management from the task state persistence layer.
 * Implementations should check the authoritative task state to determine whether a task is
 * considered "active" for queue management purposes.
 * </p>
 * <p>
 * A task is typically considered active if:
 * </p>
 * <ul>
 *     <li>Its state is not final (e.g., not COMPLETED, CANCELED, FAILED, etc.), OR</li>
 *     <li>Its state is final but it was finalized recently (within a configurable grace period)</li>
 * </ul>
 * <p>
 * The grace period mechanism handles race conditions in both replicated and non-replicated environments
 * where events may be in-flight when a task finalizes.
 * </p>
 */
public interface TaskStateProvider {

    /**
     * Determines whether a task is considered active for queue management purposes.
     * <p>
     * This method includes the grace period in its check. A task is considered active if:
     * </p>
     * <ul>
     *     <li>Its state is not final, OR</li>
     *     <li>Its state is final but finalized within the grace period (now &lt; finalizedAt + gracePeriod)</li>
     * </ul>
     * <p>
     * This method is used to decide whether to process late-arriving events.
     * </p>
     *
     * @param taskId the ID of the task to check
     * @return {@code true} if the task is active (or recently finalized within grace period),
     *         {@code false} otherwise
     */
    boolean isTaskActive(String taskId);

    /**
     * Determines whether a task is in a final state, ignoring the grace period.
     * <p>
     * This method performs an immediate check: returns {@code true} only if the task
     * is in a final state (COMPLETED, CANCELED, FAILED, etc.), regardless of when
     * it was finalized.
     * </p>
     * <p>
     * This method is used by cleanup callbacks and MainQueue closing logic to decide whether
     * a queue can be closed and removed. By ignoring the grace period, it ensures responsive
     * cleanup while late in-flight events are still handled by the grace period mechanism
     * for {@code isTaskActive}.
     * </p>
     *
     * @param taskId the ID of the task to check
     * @return {@code true} if the task is in a final state (ignoring grace period),
     *         {@code false} otherwise
     */
    boolean isTaskFinalized(String taskId);
}
