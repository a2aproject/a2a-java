package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumeration representing the possible states of a task in the A2A protocol.
 * Tasks progress through various states from submission to completion or termination.
 */
public enum TaskState {
    /** Task has been submitted and is waiting to be processed */
    SUBMITTED("submitted"),
    
    /** Task is currently being processed by the agent */
    WORKING("working"),
    
    /** Task requires additional input from the user to continue */
    INPUT_REQUIRED("input-required"),
    
    /** Task requires authentication or authorization to proceed */
    AUTH_REQUIRED("auth-required"),
    
    /** Task has been successfully completed */
    COMPLETED("completed", true),
    
    /** Task has been canceled by the user or system */
    CANCELED("canceled", true),
    
    /** Task has failed due to an error or exception */
    FAILED("failed", true),
    
    /** Task has been rejected by the agent (e.g., invalid request) */
    REJECTED("rejected", true),
    
    /** Task state is unknown or could not be determined */
    UNKNOWN("unknown", true);

    /** The string representation of this state for JSON serialization */
    private final String state;
    
    /** Whether this state represents a final/terminal state */
    private final boolean isFinal;

    /**
     * Constructs a TaskState with the specified string representation.
     * The state is considered non-final by default.
     *
     * @param state the string representation of this state
     */
    TaskState(String state) {
        this(state, false);
    }

    /**
     * Constructs a TaskState with the specified string representation and finality.
     *
     * @param state the string representation of this state
     * @param isFinal whether this state is final/terminal
     */
    TaskState(String state, boolean isFinal) {
        this.state = state;
        this.isFinal = isFinal;
    }

    /**
     * Returns the string representation of this state for JSON serialization.
     *
     * @return the state string
     */
    @JsonValue
    public String asString() {
        return state;
    }

    /**
     * Checks whether this state represents a final/terminal state.
     * Final states indicate that the task has reached its end and will not transition further.
     *
     * @return {@code true} if this is a final state, {@code false} otherwise
     */
    public boolean isFinal(){
        return isFinal;
    }

    /**
     * Creates a TaskState from its string representation.
     * This method is used for JSON deserialization.
     *
     * @param state the string representation of the task state
     * @return the corresponding TaskState enum value
     * @throws IllegalArgumentException if the state string is not recognized
     */
    @JsonCreator
    public static TaskState fromString(String state) {
        switch (state) {
            case "submitted":
                return SUBMITTED;
            case "working":
                return WORKING;
            case "input-required":
                return INPUT_REQUIRED;
            case "auth-required":
                return AUTH_REQUIRED;
            case "completed":
                return COMPLETED;
            case "canceled":
                return CANCELED;
            case "failed":
                return FAILED;
            case "rejected":
                return REJECTED;
            case "unknown":
                return UNKNOWN;
            default:
                throw new IllegalArgumentException("Invalid TaskState: " + state);
        }
    }
}