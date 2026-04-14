package io.a2a.spec;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import io.a2a.util.Assert;

/**
 * Represents the status of a task at a specific point in time.
 */
public record TaskStatus(TaskState state, Message message,
        OffsetDateTime timestamp) {

    public TaskStatus {
        Assert.checkNotNullParam("state", state);
        timestamp = timestamp == null ? OffsetDateTime.now(ZoneOffset.UTC) : timestamp;
    }

    public TaskStatus(TaskState state) {
        this(state, null, null);
    }

    /**
     * Constructor for testing purposes.
     * 
     * @param state     the task state
     * @param timestamp timestamp generation
     */
    TaskStatus(TaskState state, OffsetDateTime timestamp) {
        this(state, null, timestamp);
    }
}
