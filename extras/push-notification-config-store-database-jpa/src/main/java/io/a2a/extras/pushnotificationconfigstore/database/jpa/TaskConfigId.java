package io.a2a.extras.pushnotificationconfigstore.database.jpa;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class TaskConfigId implements Serializable {

    @Column(name = "task_id")
    private String taskId;

    @Column(name = "config_id")
    private String configId;

    // No-argument constructor (required by JPA)
    public TaskConfigId() {
    }

    public TaskConfigId(String taskId, String configId) {
        this.taskId = taskId;
        this.configId = configId;
    }

    // Getters and setters...

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getConfigId() {
        return configId;
    }

    public void setConfigId(String configId) {
        this.configId = configId;
    }

    // hashCode() and equals() implementations
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskConfigId that = (TaskConfigId) o;
        return Objects.equals(taskId, that.taskId) &&
               Objects.equals(configId, that.configId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, configId);
    }
}
