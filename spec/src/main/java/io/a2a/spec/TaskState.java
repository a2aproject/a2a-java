package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

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

    @JsonValue
    public String asString() {
        return state;
    }

    public boolean isFinal(){
        return isFinal;
    }

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