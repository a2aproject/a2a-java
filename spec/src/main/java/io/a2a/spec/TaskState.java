package io.a2a.spec;

/**
 * Defines the lifecycle states of a Task.
 */
public enum TaskState {
    SUBMITTED("submitted"),
    WORKING("working"),
    INPUT_REQUIRED("input-required"),
    AUTH_REQUIRED("auth-required"),
    COMPLETED("completed", true),
    CANCELED("canceled", true),
    FAILED("failed", true),
    REJECTED("rejected", true),
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