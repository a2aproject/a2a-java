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