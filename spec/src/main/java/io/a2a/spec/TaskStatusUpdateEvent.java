package io.a2a.spec;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.a2a.util.Assert;

/**
 * Represents a task status update event in the A2A protocol.
 * 
 * This event is emitted when a task's status changes during execution,
 * providing real-time updates about task progress, completion, or failure.
 * It implements both {@link EventKind} and {@link StreamingEventKind} to support
 * different event processing patterns in the A2A framework.
 * 
 * The event contains information about which task was updated, its new status,
 * the context it belongs to, whether this is a final status change, and any
 * additional metadata associated with the status update.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class TaskStatusUpdateEvent implements EventKind, StreamingEventKind {

    public static final String STATUS_UPDATE = "status-update";
    /**
     * The unique identifier of the task whose status was updated.
     */
    private final String taskId;
    
    /**
     * The new status of the task after the update.
     */
    private final TaskStatus status;
    
    /**
     * The identifier of the context/conversation this task belongs to.
     */
    private final String contextId;
    
    /**
     * Indicates whether this status update represents a final state.
     * If true, no further status updates are expected for this task.
     */
    private final boolean isFinal;
    
    /**
     * Additional metadata associated with this status update.
     * Can contain implementation-specific information about the status change.
     */
    private final Map<String, Object> metadata;
    
    /**
     * The kind/type of this event, always "status-update" for TaskStatusUpdateEvent instances.
     */
    private final String kind;


    /**
     * Constructs a new TaskStatusUpdateEvent with the specified properties.
     *
     * @param taskId the unique identifier of the task whose status was updated
     * @param status the new status of the task after the update
     * @param contextId the identifier of the context/conversation this task belongs to
     * @param isFinal whether this status update represents a final state
     * @param metadata additional metadata associated with this status update
     */
    public TaskStatusUpdateEvent(String taskId, TaskStatus status, String contextId, boolean isFinal,
                                 Map<String, Object> metadata) {
        this(taskId, status, contextId, isFinal, metadata, STATUS_UPDATE);
    }

    /**
     * JSON constructor for creating TaskStatusUpdateEvent instances from JSON data.
     *
     * @param taskId the unique identifier of the task whose status was updated
     * @param status the new status of the task after the update
     * @param contextId the identifier of the context/conversation this task belongs to
     * @param isFinal whether this status update represents a final state
     * @param metadata additional metadata associated with this status update
     * @param kind the kind/type of this event
     */
    @JsonCreator
    public TaskStatusUpdateEvent(@JsonProperty("taskId") String taskId, @JsonProperty("status") TaskStatus status,
                                 @JsonProperty("contextId") String contextId, @JsonProperty("final") boolean isFinal,
                                 @JsonProperty("metadata") Map<String, Object> metadata, @JsonProperty("kind") String kind) {
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
     * Gets the unique identifier of the task whose status was updated.
     *
     * @return the task ID
     */
    public String getTaskId() {
        return taskId;
    }

    /**
     * Gets the new status of the task after the update.
     *
     * @return the task status
     */
    public TaskStatus getStatus() {
        return status;
    }

    /**
     * Gets the identifier of the context/conversation this task belongs to.
     *
     * @return the context ID
     */
    public String getContextId() {
        return contextId;
    }

    /**
     * Indicates whether this status update represents a final state.
     *
     * @return true if this is a final status update, false otherwise
     */
    @JsonProperty("final")
    public boolean isFinal() {
        return isFinal;
    }

    /**
     * Gets the additional metadata associated with this status update.
     *
     * @return the metadata map
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Gets the kind/type of this event.
     *
     * @return the event kind, always "status-update" for TaskStatusUpdateEvent instances
     */
    @Override
    public String getKind() {
        return kind;
    }

    /**
     * Builder class for constructing {@link TaskStatusUpdateEvent} instances.
     */
    public static class Builder {
        private String taskId;
        private TaskStatus status;
        private String contextId;
        private boolean isFinal;
        private Map<String, Object> metadata;

        /**
         * Sets the unique identifier of the task whose status was updated.
         *
         * @param id the task ID
         * @return this builder instance
         */
        public Builder taskId(String id) {
            this.taskId = id;
            return this;
        }

        /**
         * Sets the new status of the task after the update.
         *
         * @param status the task status
         * @return this builder instance
         */
        public Builder status(TaskStatus status) {
            this.status = status;
            return this;
        }

        /**
         * Sets the identifier of the context/conversation this task belongs to.
         *
         * @param contextId the context ID
         * @return this builder instance
         */
        public Builder contextId(String contextId) {
            this.contextId = contextId;
            return this;
        }

        /**
         * Sets whether this status update represents a final state.
         *
         * @param isFinal true if this is a final status update, false otherwise
         * @return this builder instance
         */
        public Builder isFinal(boolean isFinal) {
            this.isFinal = isFinal;
            return this;
        }

        /**
         * Sets the additional metadata associated with this status update.
         *
         * @param metadata the metadata map
         * @return this builder instance
         */
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * Builds and returns a new {@link TaskStatusUpdateEvent} instance with the configured properties.
         *
         * @return a new TaskStatusUpdateEvent instance
         */
        public TaskStatusUpdateEvent build() {
            return new TaskStatusUpdateEvent(taskId, status, contextId, isFinal, metadata);
        }
    }
}
