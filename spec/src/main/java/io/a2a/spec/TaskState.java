package io.a2a.spec;

/**
 * Defines the lifecycle states of a {@link Task} in the A2A Protocol.
 * <p>
 * TaskState represents the discrete states a task can be in during its execution lifecycle.
 * States are categorized as either transitional (non-final) or terminal (final), where
 * terminal states indicate that the task has reached its end state and will not transition further.
 * <p>
 * <b>Transitional States:</b>
 * <ul>
 *   <li><b>TASK_STATE_SUBMITTED:</b> Task has been received by the agent and is queued for processing</li>
 *   <li><b>TASK_STATE_WORKING:</b> Agent is actively processing the task and may produce incremental results</li>
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
 * important for event queue management and client polling logic.
 *
 * @see TaskStatus
 * @see Task
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public enum TaskState {
    /** Task has been received and is queued for processing (transitional state). */
    TASK_STATE_SUBMITTED(false),

    /** Agent is actively processing the task (transitional state). */
    TASK_STATE_WORKING(false),

    /** Agent requires additional input from the user to continue (transitional state). */
    TASK_STATE_INPUT_REQUIRED(false),

    /** Agent requires authentication or authorization to proceed (transitional state). */
    TASK_STATE_AUTH_REQUIRED(false),

    /** Task completed successfully (terminal state). */
    TASK_STATE_COMPLETED(true),

    /** Task was canceled by user or system (terminal state). */
    TASK_STATE_CANCELED(true),

    /** Task failed due to an error (terminal state). */
    TASK_STATE_FAILED(true),

    /** Task was rejected by the agent (terminal state). */
    TASK_STATE_REJECTED(true),

    /** Task state is unknown or cannot be determined (terminal state). */
    UNRECOGNIZED(true);

    private final boolean isFinal;

    TaskState(boolean isFinal) {
        this.isFinal = isFinal;
    }

    /**
     * Determines whether this state is a terminal (final) state.
     * <p>
     * Terminal states indicate that the task has completed its lifecycle and will
     * not transition to any other state. This is used by the event queue system
     * to determine when to close queues and by clients to know when to stop polling.
     *
     * @return {@code true} if this is a terminal state, {@code false} else.
     */
    public boolean isFinal(){
        return isFinal;
    }
}