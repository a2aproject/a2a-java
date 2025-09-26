package io.a2a.extras.taskstore.database.jpa;

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
    }

    static JpaTask createFromTask(Task task) throws JsonProcessingException {
        String json = Utils.OBJECT_MAPPER.writeValueAsString(task);
        JpaTask jpaTask = new JpaTask(task.getId(), json);
        jpaTask.task = task;
        return jpaTask;
    }
}
