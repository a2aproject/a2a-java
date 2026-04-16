package org.a2aproject.sdk.compat03.spec;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.a2aproject.sdk.util.Assert;

/**
 * Represents the status of a task at a specific point in time.
 */
public record TaskStatus_v0_3(TaskState_v0_3 state, Message_v0_3 message,
                              OffsetDateTime timestamp) {

    public TaskStatus_v0_3 {
        Assert.checkNotNullParam("state", state);
        timestamp = timestamp == null ? OffsetDateTime.now(ZoneOffset.UTC) : timestamp;
    }

    public TaskStatus_v0_3(TaskState_v0_3 state) {
        this(state, null, null);
    }

    /**
     * Constructor for testing purposes.
     * @param state the task state
     * @param timestamp timestamp generation
     */
    TaskStatus_v0_3(TaskState_v0_3 state, OffsetDateTime timestamp) {
        this(state, null, timestamp);
    }
}
