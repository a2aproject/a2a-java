package io.a2a.spec;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.a2a.util.Assert;

/**
 * Represents the current status of a task in the A2A protocol.
 * Contains the task's current state, an optional status message, and a timestamp
 * indicating when this status was recorded.
 *
 * @param state the current state of the task (e.g., pending, in_progress, completed, failed)
 * @param message an optional message providing additional context about the current status
 * @param timestamp when this status was recorded (automatically set to current time if null)
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskStatus(TaskState state, Message message,
                         @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS") LocalDateTime timestamp) {

    /**
     * Compact constructor that validates and initializes the task status.
     * <p>
     * This constructor ensures that the task state is not null and automatically
     * sets the timestamp to the current time if it is not provided.
     * </p>
     *
     * @throws IllegalArgumentException if state is null
     */
    public TaskStatus {
        Assert.checkNotNullParam("state", state);
        timestamp = timestamp == null ? LocalDateTime.now() : timestamp;
    }

    /**
     * Convenience constructor for creating a TaskStatus with only a state.
     * The message will be null and timestamp will be set to the current time.
     *
     * @param state the current state of the task
     */
    public TaskStatus(TaskState state) {
        this(state, null, null);
    }
}
