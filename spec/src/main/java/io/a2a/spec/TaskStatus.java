package io.a2a.spec;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.a2a.util.Assert;

/**
 * Represents the status of a task at a specific point in time.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskStatus(TaskState state, Message message,
                         @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS") LocalDateTime timestamp) {

    public TaskStatus {
        Assert.checkNotNullParam("state", state);
        timestamp = timestamp == null ? LocalDateTime.now() : timestamp;
    }

    public TaskStatus(TaskState state) {
        this(state, null, null);
    }
}
