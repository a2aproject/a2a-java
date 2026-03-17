package io.a2a.spec;

/**
 * Defines the lifecycle states of a {@link Task} in the A2A Protocol.
 * <p>
 * TaskState represents the discrete states a task can be in during its execution lifecycle.
 * States are categorized as either transitional (non-final) or terminal (final), where
 * terminal states indicate that the task has reached its end state and will not transition further.
 * A subset of transitional states are also marked as interrupted, indicating the task execution
 * has paused and requires external action before proceeding.
 * <p>
 * <b>Active Transitional States:</b>
 * <ul>
 *   <li><b>TASK_STATE_SUBMITTED:</b> Task has been received by the agent and is queued for processing</li>
 *   <li><b>TASK_STATE_WORKING:</b> Agent is actively processing the task and may produce incremental results</li>
 * </ul>
 * <p>
 * <b>Interrupted States:</b>
 * <ul>
 *   <li><b>TASK_STATE_INPUT_REQUIRED:</b> Agent needs additional input from the user to continue</li>
 *   <li><b>TASK_STATE_AUTH_REQUIRED:</b> Agent requires authentication or authorization before proceeding</li>
 * </ul>
 * <p>
 * <b>Terminal States:</b>
 * <ul>
 *   <li><b>TASK_STATE_COMPLETED:</b> Task finished successfully with all requested work done</li>
 *   <li><b>TASK_STATE_CANCELED:</b> Task was explicitly canceled by the user or system</li>
 *   <li><b>TASK_STATE_FAILED:</b> Task failed due to an error during execution</li>
 *   <li><b>TASK_STATE_REJECTED:</b> Task was rejected by the agent (e.g., invalid request, policy violation)</li>
 *   <li><b>UNRECOGNIZED:</b> Task state cannot be determined (error recovery state)</li>
 * </ul>
 * <p>
 * The {@link #isFinal()} method can be used to determine if a state is terminal, which is
 * important for event queue management and client polling logic. The {@link #isInterrupted()}
 * method identifies states where the task is paused awaiting external action.
 *
 * @see TaskStatus
 * @see Task
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public enum TaskState {
    /** Task has been received and is queued for processing (transitional state). */
    TASK_STATE_SUBMITTED(false, false),

    /** Agent is actively processing the task (transitional state). */
    TASK_STATE_WORKING(false, false),

    /** Agent requires additional input from the user to continue (interrupted state). */
    TASK_STATE_INPUT_REQUIRED(false, true),

    /** Agent requires authentication or authorization to proceed (interrupted state). */
    TASK_STATE_AUTH_REQUIRED(false, true),

    /** Task completed successfully (terminal state). */
    TASK_STATE_COMPLETED(true, false),

    /** Task was canceled by user or system (terminal state). */
    TASK_STATE_CANCELED(true, false),

    /** Task failed due to an error (terminal state). */
    TASK_STATE_FAILED(true, false),

    /** Task was rejected by the agent (terminal state). */
    TASK_STATE_REJECTED(true, false),

    /** Task state is unknown or cannot be determined (terminal state). */
    UNRECOGNIZED(true, false);

    private final boolean isFinal;
    private final boolean isInterrupted;

    TaskState(boolean isFinal, boolean isInterrupted) {
        this.isFinal = isFinal;
        this.isInterrupted = isInterrupted;
    }

    /**
     * Determines whether this state is a terminal (final) state.
     * <p>
     * Terminal states indicate that the task has completed its lifecycle and will
     * not transition to any other state. This is used by the event queue system
     * to determine when to close queues and by clients to know when to stop polling.
     * <p>
     * Terminal states: COMPLETED, FAILED, CANCELED, REJECTED, UNRECOGNIZED.
     *
     * @return {@code true} if this is a terminal state, {@code false} else.
     */
    public boolean isFinal(){
        return isFinal;
    }

    /**
     * Determines whether this state is an interrupted state.
     * <p>
     * Interrupted states indicate that the task execution has paused and requires
     * external action before proceeding. The task may resume after the required
     * action is provided. Interrupted states are NOT terminal - streams should
     * remain open to deliver state updates.
     * <p>
     * Interrupted states: INPUT_REQUIRED, AUTH_REQUIRED.
     * <p>
     * Per A2A Protocol Specification 4.1.3 (TaskState):
     * "TASK_STATE_INPUT_REQUIRED: This is an interrupted state."
     * "TASK_STATE_AUTH_REQUIRED: This is an interrupted state."
     *
     * @return {@code true} if this is an interrupted state, {@code false} else.
     */
    public boolean isInterrupted() {
        return isInterrupted;
    }
}