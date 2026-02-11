package io.a2a.spec;

import java.time.OffsetDateTime;

import io.a2a.util.Assert;
import java.time.ZoneOffset;
import org.jspecify.annotations.Nullable;

/**
 * Represents the status of a task at a specific point in time in the A2A Protocol.
 * <p>
 * TaskStatus encapsulates the current state of a task along with an optional message
 * providing additional context and a timestamp indicating when the status was created.
 * This information is essential for tracking task lifecycle and communicating progress
 * to clients.
 * <p>
 * The status transitions through various states as the agent processes the task:
 * <ul>
 *   <li><b>SUBMITTED:</b> Task has been received and queued</li>
 *   <li><b>WORKING:</b> Agent is actively processing the task</li>
 *   <li><b>INPUT_REQUIRED:</b> Agent needs additional input from the user</li>
 *   <li><b>AUTH_REQUIRED:</b> Agent requires authentication or authorization</li>
 *   <li><b>COMPLETED:</b> Task finished successfully (final state)</li>
 *   <li><b>CANCELED:</b> Task was canceled by the user or system (final state)</li>
 *   <li><b>FAILED:</b> Task failed due to an error (final state)</li>
 *   <li><b>REJECTED:</b> Task was rejected by the agent (final state)</li>
 *   <li><b>UNKNOWN:</b> Task state cannot be determined (final state)</li>
 * </ul>
 * <p>
 * TaskStatus is immutable and automatically generates a UTC timestamp if none is provided.
 * This class is used within {@link Task} objects and {@link TaskStatusUpdateEvent} instances
 * to communicate state changes.
 *
 * @param state the current state of the task (required)
 * @param message an optional message providing additional context about the status (optional)
 * @param timestamp the time when this status was created, in UTC (defaults to current time if not provided)
 * @see TaskState
 * @see Task
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record TaskStatus(TaskState state, @Nullable Message message, OffsetDateTime timestamp) {

    /**
     * Compact constructor for validation and timestamp initialization.
     * Validates that the state is not null and sets the timestamp to current UTC time if not provided.
     *
     * @param state the task state
     * @param message optional status message
     * @param timestamp the status timestamp
     */
    public TaskStatus(TaskState state, @Nullable Message message, @Nullable OffsetDateTime timestamp){
        this.state = Assert.checkNotNullParam("state", state);
        this.timestamp = timestamp == null ? OffsetDateTime.now(ZoneOffset.UTC) : timestamp;
        this.message = message;
    }

    public OffsetDateTime timestamp() {
        return timestamp;
    }
    /**
     * Creates a TaskStatus with only a state, using the current UTC time as the timestamp.
     * <p>
     * This is a convenience constructor for creating status updates without
     * an accompanying message.
     *
     * @param state the task state (required)
     */
    public TaskStatus(TaskState state) {
        this(state, null, OffsetDateTime.now(ZoneOffset.UTC));
    }
}
