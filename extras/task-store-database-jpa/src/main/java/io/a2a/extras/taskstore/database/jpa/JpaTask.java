package io.a2a.extras.taskstore.database.jpa;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import io.a2a.json.JsonProcessingException;
import io.a2a.json.JsonUtil;
import io.a2a.spec.Task;

@Entity
@Table(name = "a2a_tasks")
public class JpaTask {
    @Id
    @Column(name = "task_id")
    private String id;

    @Column(name = "context_id")
    private String contextId;

    @Column(name = "state")
    private String state;

    @Column(name = "status_timestamp")
    private Instant statusTimestamp;

    @Column(name = "task_data", columnDefinition = "TEXT", nullable = false)
    private String taskJson;

    @Column(name = "finalized_at")
    private Instant finalizedAt;

    @Transient
    private Task task;

    // Default constructor required by JPA
    public JpaTask() {
    }

    public JpaTask(String id, String taskJson) {
        this.id = id;
        this.taskJson = taskJson;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContextId() {
        return contextId;
    }

    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Instant getStatusTimestamp() {
        return statusTimestamp;
    }

    public void setStatusTimestamp(Instant statusTimestamp) {
        this.statusTimestamp = statusTimestamp;
    }

    public String getTaskJson() {
        return taskJson;
    }

    public void setTaskJson(String taskJson) {
        this.taskJson = taskJson;
    }

    public Instant getFinalizedAt() {
        return finalizedAt;
    }

    /**
     * Sets the finalized timestamp for this task.
     * <p>
     * This method is idempotent - it only sets the timestamp on the first transition
     * to a final state, as a defense-in-depth measure complementing the existing
     * application logic that prevents modifications to final tasks.
     * </p>
     *
     * @param finalizedAt the timestamp when the task was finalized
     * @param isFinalState whether the current task state is final
     */
    public void setFinalizedAt(Instant finalizedAt, boolean isFinalState) {
        if (this.finalizedAt == null && isFinalState) {
            this.finalizedAt = finalizedAt;
        }
    }

    public Task getTask() throws JsonProcessingException {
        if (task == null) {
            this.task = JsonUtil.fromJson(taskJson, Task.class);
        }
        return task;
    }

    public void setTask(Task task) throws JsonProcessingException {
        taskJson = JsonUtil.toJson(task);
        if (id == null) {
            id = task.id();
        }
        this.task = task;
        updateDenormalizedFields(task);
        updateFinalizedTimestamp(task);
    }

    static JpaTask createFromTask(Task task) throws JsonProcessingException {
        String json = JsonUtil.toJson(task);
        JpaTask jpaTask = new JpaTask(task.id(), json);
        jpaTask.task = task;
        jpaTask.updateDenormalizedFields(task);
        jpaTask.updateFinalizedTimestamp(task);
        return jpaTask;
    }

    /**
     * Updates denormalized fields (contextId, state, statusTimestamp) from the task object.
     * These fields are duplicated from the JSON to enable efficient querying.
     *
     * @param task the task to extract fields from
     */
    private void updateDenormalizedFields(Task task) {
        this.contextId = task.contextId();
        if (task.status() != null) {
            io.a2a.spec.TaskState taskState = task.status().state();
            this.state = (taskState != null) ? taskState.asString() : null;
            // Extract status timestamp for efficient querying and sorting
            // Truncate to milliseconds for keyset pagination consistency (pageToken uses millis)
            this.statusTimestamp = (task.status().timestamp() != null)
                    ? task.status().timestamp().toInstant().truncatedTo(java.time.temporal.ChronoUnit.MILLIS)
                    : null;
        } else {
            this.state = null;
            this.statusTimestamp = null;
        }
    }

    /**
     * Updates the finalizedAt timestamp if the task is in a final state.
     * This method is idempotent and only sets the timestamp on first finalization.
     *
     * @param task the task to check for finalization
     */
    private void updateFinalizedTimestamp(Task task) {
        if (task.status() != null && task.status().state() != null) {
            setFinalizedAt(Instant.now(), task.status().state().isFinal());
        }
    }
}
