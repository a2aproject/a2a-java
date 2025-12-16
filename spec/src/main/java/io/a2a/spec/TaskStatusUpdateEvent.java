package io.a2a.spec;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

import io.a2a.util.Assert;

import static io.a2a.spec.TaskStatusUpdateEvent.STATUS_UPDATE;

/**
 * An event sent by the agent to notify the client of a change in a task's status.
 * This is typically used in streaming or subscription models.
 */
public final class TaskStatusUpdateEvent implements EventKind, StreamingEventKind, UpdateEvent {

    /**
     * The kind identifier for status update events: "status-update".
     */
    public static final String STATUS_UPDATE = "status-update";
    private final String taskId;
    private final TaskStatus status;
    private final String contextId;
    @SerializedName("final")
    private final boolean isFinal;
    private final Map<String, Object> metadata;
    private final String kind;


    /**
     * Constructs a TaskStatusUpdateEvent with default kind.
     *
     * @param taskId the task identifier (required)
     * @param status the task status (required)
     * @param contextId the context identifier (required)
     * @param isFinal whether this is a final status
     * @param metadata additional metadata (optional)
     */
    public TaskStatusUpdateEvent(String taskId, TaskStatus status, String contextId, boolean isFinal,
                                 Map<String, Object> metadata) {
        this(taskId, status, contextId, isFinal, metadata, STATUS_UPDATE);
    }

    /**
     * Constructs a TaskStatusUpdateEvent with all parameters.
     *
     * @param taskId the task identifier (required)
     * @param status the task status (required)
     * @param contextId the context identifier (required)
     * @param isFinal whether this is a final status
     * @param metadata additional metadata (optional)
     * @param kind the event kind (must be "status-update")
     */
    public TaskStatusUpdateEvent(String taskId, TaskStatus status, String contextId, boolean isFinal, Map<String, Object> metadata, String kind) {
        Assert.checkNotNullParam("taskId", taskId);
        Assert.checkNotNullParam("status", status);
        Assert.checkNotNullParam("contextId", contextId);
        Assert.checkNotNullParam("kind", kind);
        if (! kind.equals(STATUS_UPDATE)) {
            throw new IllegalArgumentException("Invalid TaskStatusUpdateEvent");
        }
        this.taskId = taskId;
        this.status = status;
        this.contextId = contextId;
        this.isFinal = isFinal;
        this.metadata = metadata;
        this.kind = kind;
    }

    /**
     * Returns the task identifier.
     *
     * @return the task ID
     */
    public String getTaskId() {
        return taskId;
    }

    /**
     * Returns the task status.
     *
     * @return the task status
     */
    public TaskStatus getStatus() {
        return status;
    }

    /**
     * Returns the context identifier.
     *
     * @return the context ID
     */
    public String getContextId() {
        return contextId;
    }

    /**
     * Returns whether this is a final status.
     *
     * @return true if this is a final status
     */
    public boolean isFinal() {
        return isFinal;
    }

    /**
     * Returns the metadata.
     *
     * @return the metadata map
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public String getKind() {
        return kind;
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
        private String taskId;
        private TaskStatus status;
        private String contextId;
        private boolean isFinal;
        private Map<String, Object> metadata;

        private Builder() {
        }

        private Builder(TaskStatusUpdateEvent existingTaskStatusUpdateEvent) {
            this.taskId = existingTaskStatusUpdateEvent.taskId;
            this.status = existingTaskStatusUpdateEvent.status;
            this.contextId = existingTaskStatusUpdateEvent.contextId;
            this.isFinal = existingTaskStatusUpdateEvent.isFinal;
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
         * Sets whether this is a final status.
         *
         * @param isFinal true if this is a final status
         * @return this builder for method chaining
         */
        public Builder isFinal(boolean isFinal) {
            this.isFinal = isFinal;
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
            return new TaskStatusUpdateEvent(taskId, status, contextId, isFinal, metadata);
        }
    }
}
