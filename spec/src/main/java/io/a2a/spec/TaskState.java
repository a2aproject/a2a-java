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
 *   <li><b>SUBMITTED:</b> Task has been received by the agent and is queued for processing</li>
 *   <li><b>WORKING:</b> Agent is actively processing the task and may produce incremental results</li>
 *   <li><b>INPUT_REQUIRED:</b> Agent needs additional input from the user to continue</li>
 *   <li><b>AUTH_REQUIRED:</b> Agent requires authentication or authorization before proceeding</li>
 * </ul>
 * <p>
 * <b>Terminal States:</b>
 * <ul>
 *   <li><b>COMPLETED:</b> Task finished successfully with all requested work done</li>
 *   <li><b>CANCELED:</b> Task was explicitly canceled by the user or system</li>
 *   <li><b>FAILED:</b> Task failed due to an error during execution</li>
 *   <li><b>REJECTED:</b> Task was rejected by the agent (e.g., invalid request, policy violation)</li>
 *   <li><b>UNKNOWN:</b> Task state cannot be determined (error recovery state)</li>
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
    SUBMITTED("submitted"),

    /** Agent is actively processing the task (transitional state). */
    WORKING("working"),

    /** Agent requires additional input from the user to continue (transitional state). */
    INPUT_REQUIRED("input-required"),

    /** Agent requires authentication or authorization to proceed (transitional state). */
    AUTH_REQUIRED("auth-required"),

    /** Task completed successfully (terminal state). */
    COMPLETED("completed", true),

    /** Task was canceled by user or system (terminal state). */
    CANCELED("canceled", true),

    /** Task failed due to an error (terminal state). */
    FAILED("failed", true),

    /** Task was rejected by the agent (terminal state). */
    REJECTED("rejected", true),

    /** Task state is unknown or cannot be determined (terminal state). */
    UNKNOWN("unknown", true);

    private final String state;
    private final boolean isFinal;

    TaskState(String state) {
        this(state, false);
    }

    TaskState(String state, boolean isFinal) {
        this.state = state;
        this.isFinal = isFinal;
    }

    /**
     * Returns the string representation of this task state for JSON serialization.
     * <p>
     * This method is used to serialize TaskState values to their
     * wire format (e.g., "working", "completed").
     *
     * @return the string representation of this state
     */
    public String asString() {
        return state;
    }

    /**
     * Determines whether this state is a terminal (final) state.
     * <p>
     * Terminal states indicate that the task has completed its lifecycle and will
     * not transition to any other state. This is used by the event queue system
     * to determine when to close queues and by clients to know when to stop polling.
     *
     * @return true if this is a terminal state (COMPLETED, CANCELED, FAILED, REJECTED, UNKNOWN),
     *         false for transitional states (SUBMITTED, WORKING, INPUT_REQUIRED, AUTH_REQUIRED)
     */
    public boolean isFinal(){
        return isFinal;
    }

    /**
     * Deserializes a string value into a TaskState enum constant.
     * <p>
     * This method is used to deserialize TaskState values from their
     * wire format during JSON parsing.
     *
     * @param state the string representation of the state
     * @return the corresponding TaskState enum constant
     * @throws IllegalArgumentException if the state string is not recognized
     */
    public static TaskState fromString(String state) {
        return switch (state) {
            case "submitted" -> SUBMITTED;
            case "working" -> WORKING;
            case "input-required" -> INPUT_REQUIRED;
            case "auth-required" -> AUTH_REQUIRED;
            case "completed" -> COMPLETED;
            case "canceled" -> CANCELED;
            case "failed" -> FAILED;
            case "rejected" -> REJECTED;
            case "unknown" -> UNKNOWN;
            default -> throw new IllegalArgumentException("Invalid TaskState: " + state);
        };
    }
}