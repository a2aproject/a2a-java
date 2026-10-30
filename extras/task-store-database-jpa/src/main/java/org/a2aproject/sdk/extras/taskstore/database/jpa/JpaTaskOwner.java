package org.a2aproject.sdk.extras.taskstore.database.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** Ownership may precede task persistence, so this entity deliberately has no task foreign key. */
@Entity
@Table(name = "a2a_task_owners", indexes = @Index(name = "a2a_task_owner_subject", columnList = "subject_key, task_id"))
public class JpaTaskOwner {
    @Id
    @Column(name = "task_id")
    private String taskId;

    @Column(name = "subject_key", nullable = false, length = 1024)
    private byte[] subjectKey;

    protected JpaTaskOwner() {
    }

    public JpaTaskOwner(String taskId, byte[] subjectKey) {
        this.taskId = taskId;
        this.subjectKey = subjectKey.clone();
    }
}
