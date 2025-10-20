package io.a2a.extras.taskstore.database.jpa;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.a2a.spec.Task;
import io.a2a.util.Utils;

@Entity
@Table(name = "a2a_tasks")
public class JpaTask {
    @Id
    @Column(name = "task_id")
    private String id;

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
            this.task = Utils.unmarshalFrom(taskJson, Task.TYPE_REFERENCE);
        }
        return task;
    }

    public void setTask(Task task) throws JsonProcessingException {
        taskJson = Utils.OBJECT_MAPPER.writeValueAsString(task);
        if (id == null) {
            id = task.getId();
        }
        this.task = task;
        updateFinalizedTimestamp(task);
    }

    static JpaTask createFromTask(Task task) throws JsonProcessingException {
        String json = Utils.OBJECT_MAPPER.writeValueAsString(task);
        JpaTask jpaTask = new JpaTask(task.getId(), json);
        jpaTask.task = task;
        jpaTask.updateFinalizedTimestamp(task);
        return jpaTask;
    }

    /**
     * Updates the finalizedAt timestamp if the task is in a final state.
     * This method is idempotent and only sets the timestamp on first finalization.
     *
     * @param task the task to check for finalization
     */
    private void updateFinalizedTimestamp(Task task) {
        if (task.getStatus() != null && task.getStatus().state() != null) {
            setFinalizedAt(Instant.now(), task.getStatus().state().isFinal());
        }
    }
}
