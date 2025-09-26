package io.a2a.extras.taskstore.database.jpa;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.a2a.server.tasks.TaskStore;
import io.a2a.spec.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Alternative
@Priority(50)
public class JpaDatabaseTaskStore implements TaskStore {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(JpaDatabaseTaskStore.class);
    
    @PersistenceContext(unitName = "a2a-java")
    EntityManager em;

    @Transactional
    @Override
    public void save(Task task) {
        LOGGER.debug("Saving task with ID: {}", task.getId());
        try {
            JpaTask jpaTask = JpaTask.createFromTask(task);
            em.merge(jpaTask);
            LOGGER.debug("Persisted/updated task with ID: {}", task.getId());
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to serialize task with ID: {}", task.getId(), e);
            throw new RuntimeException("Failed to serialize task with ID: " + task.getId(), e);
        }
    }

    @Transactional
    @Override
    public Task get(String taskId) {
        LOGGER.debug("Retrieving task with ID: {}", taskId);
        JpaTask jpaTask = em.find(JpaTask.class, taskId);
        if (jpaTask == null) {
            LOGGER.debug("Task not found with ID: {}", taskId);
            return null;
        }

        try {
            Task task = jpaTask.getTask();
            LOGGER.debug("Successfully retrieved task with ID: {}", taskId);
            return task;
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to deserialize task with ID: {}", taskId, e);
            throw new RuntimeException("Failed to deserialize task with ID: " + taskId, e);
        }
    }

    @Transactional
    @Override
    public void delete(String taskId) {
        LOGGER.debug("Deleting task with ID: {}", taskId);
        JpaTask jpaTask = em.find(JpaTask.class, taskId);
        if (jpaTask != null) {
            em.remove(jpaTask);
            LOGGER.debug("Successfully deleted task with ID: {}", taskId);
        } else {
            LOGGER.debug("Task not found for deletion with ID: {}", taskId);
        }
    }
}
