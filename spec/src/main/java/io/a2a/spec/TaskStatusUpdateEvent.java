package io.a2a.spec;

import static io.a2a.spec.TaskState.CANCELED;
import static io.a2a.spec.TaskState.COMPLETED;
import static io.a2a.spec.TaskState.FAILED;
import static io.a2a.spec.TaskState.INPUT_REQUIRED;
import static io.a2a.spec.TaskState.REJECTED;

import java.util.Map;

import com.google.gson.annotations.SerializedName;
import io.a2a.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * An event sent by the agent to notify the client of a change in a task's status.
 * This is typically used in streaming or subscription models.
 *
 * @param taskId the task identifier (required)
 * @param status the task status (required)
 * @param contextId the context identifier (required)
 * @param isFinal whether this is a final status
 * @param metadata additional metadata (optional)
 */
public record TaskStatusUpdateEvent(String taskId, TaskStatus status, String contextId,
        @SerializedName("final") boolean isFinal, @Nullable Map<String, Object> metadata) implements EventKind, StreamingEventKind, UpdateEvent {

    /**
     * The identifier when used in streaming responses
     */
    public static final String STREAMING_EVENT_ID = "statusUpdate";

    /**
     * Compact constructor with validation.
     *
     * @param taskId the task identifier (required)
     * @param status the task status (required)
     * @param contextId the context identifier (required)
     * @param isFinal whether this is a final status
     * @param metadata additional metadata (optional)
     * @throws IllegalArgumentException if taskId, status, or contextId is null
     */
    public TaskStatusUpdateEvent {
        Assert.checkNotNullParam("taskId", taskId);
        Assert.checkNotNullParam("status", status);
        Assert.checkNotNullParam("contextId", contextId);
        if (isFinal != status.state().isFinal()) {
            throw new IllegalArgumentException("isFinal must be the same as the Status state");
        }
    }

    @Override
    public String kind() {
        return STREAMING_EVENT_ID;
    }

    /**
     * Indicates if the task is fianl or waiting for some inputs from the client.
     * @return true if the task is fianl or waiting for some inputs from the client - false otherwise.
     */
    public boolean isFinalOrInterrupted() {
        return status.state() == COMPLETED || status.state() == FAILED || status.state() == CANCELED || status.state() == REJECTED || status.state() == INPUT_REQUIRED;
    }

    /**
     * Create a new Builder
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a new Builder initialized with values from an existing TaskStatusUpdateEvent.
     *
     * @param event the TaskStatusUpdateEvent to copy values from
     * @return the builder
     */
    public static Builder builder(TaskStatusUpdateEvent event) {
        return new Builder(event);
    }

    /**
     * Builder for constructing {@link TaskStatusUpdateEvent} instances.
     */
    public static class Builder {

        private @Nullable String taskId;
        private @Nullable TaskStatus status;
        private @Nullable String contextId;
        private @Nullable Map<String, Object> metadata;

        private Builder() {
        }

        private Builder(TaskStatusUpdateEvent existingTaskStatusUpdateEvent) {
            this.taskId = existingTaskStatusUpdateEvent.taskId;
            this.status = existingTaskStatusUpdateEvent.status;
            this.contextId = existingTaskStatusUpdateEvent.contextId;
            this.metadata = existingTaskStatusUpdateEvent.metadata;
        }

        /**
         * Sets the task identifier.
         *
         * @param id the task ID
         * @return this builder for method chaining
         */
        public Builder taskId(String id) {
            this.taskId = id;
            return this;
        }

        /**
         * Sets the task status.
         *
         * @param status the task status
         * @return this builder for method chaining
         */
        public Builder status(TaskStatus status) {
            this.status = status;
            return this;
        }

        /**
         * Sets the context identifier.
         *
         * @param contextId the context ID
         * @return this builder for method chaining
         */
        public Builder contextId(String contextId) {
            this.contextId = contextId;
            return this;
        }

        /**
         * Sets the metadata.
         *
         * @param metadata the metadata map
         * @return this builder for method chaining
         */
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * Builds the TaskStatusUpdateEvent.
         *
         * @return a new TaskStatusUpdateEvent instance
         */
        public TaskStatusUpdateEvent build() {
            return new TaskStatusUpdateEvent(
                    Assert.checkNotNullParam("taskId", taskId),
                    Assert.checkNotNullParam("status", status),
                    Assert.checkNotNullParam("contextId", contextId),
                    Assert.checkNotNullParam("status", status).state().isFinal(),
                    metadata);
        }
    }
}
