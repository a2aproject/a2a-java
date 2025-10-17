package io.a2a.extras.taskstore.database.jpa;

import java.time.Duration;
import java.time.Instant;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.a2a.extras.common.events.TaskFinalizedEvent;
import io.a2a.server.tasks.TaskStateProvider;
import io.a2a.server.tasks.TaskStore;
import io.a2a.spec.Task;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Alternative
@Priority(50)
public class JpaDatabaseTaskStore implements TaskStore, TaskStateProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaDatabaseTaskStore.class);

    @PersistenceContext(unitName = "a2a-java")
    EntityManager em;

    @Inject
    Event<TaskFinalizedEvent> taskFinalizedEvent;

    @ConfigProperty(name = "a2a.replication.grace-period-seconds", defaultValue = "15")
    long gracePeriodSeconds;

    @Transactional
    @Override
    public void save(Task task) {
        LOGGER.debug("Saving task with ID: {}", task.getId());
        try {
            JpaTask jpaTask = JpaTask.createFromTask(task);
            em.merge(jpaTask);
            LOGGER.debug("Persisted/updated task with ID: {}", task.getId());

            if (task.getStatus() != null && task.getStatus().state() != null && task.getStatus().state().isFinal()) {
                // Fire CDI event if task reached final state
                // IMPORTANT: The event will be delivered AFTER transaction commits (AFTER_SUCCESS observers)
                // This ensures the task's final state is durably stored before the QueueClosedEvent poison pill is sent
                LOGGER.debug("Task {} is in final state, firing TaskFinalizedEvent", task.getId());
                taskFinalizedEvent.fire(new TaskFinalizedEvent(task.getId()));
            }
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

    /**
     * Determines if a task is considered active for queue management purposes.
     * <p>
     * A task is active if:
     * <ul>
     *   <li>Its state is not final, OR</li>
     *   <li>Its state is final but it was finalized within the grace period</li>
     * </ul>
     * The grace period handles the race condition where events are published to Kafka
     * while a task is active, but consumed on a replica node after the task is finalized.
     * </p>
     *
     * @param taskId the task ID to check
     * @return true if the task is active (or recently finalized within grace period), false otherwise
     */
    @Transactional
    @Override
    public boolean isTaskActive(String taskId) {
        LOGGER.debug("Checking if task is active: {}", taskId);

        JpaTask jpaTask = em.find(JpaTask.class, taskId);
        if (jpaTask == null) {
            LOGGER.debug("Task not found, considering inactive: {}", taskId);
            return false;
        }

        try {
            Task task = jpaTask.getTask();

            // Task is active if not in final state
            if (task.getStatus() == null || task.getStatus().state() == null || !task.getStatus().state().isFinal()) {
                LOGGER.debug("Task is not in final state, considering active: {}", taskId);
                return true;
            }

            // Task is in final state - check grace period
            Instant finalizedAt = jpaTask.getFinalizedAt();
            if (finalizedAt == null) {
                // Should not happen, but defensive: if final state but no timestamp, consider inactive
                LOGGER.warn("Task {} is in final state but has no finalizedAt timestamp, considering inactive", taskId);
                return false;
            }

            Instant gracePeriodEnd = finalizedAt.plus(Duration.ofSeconds(gracePeriodSeconds));
            Instant now = Instant.now();

            boolean withinGracePeriod = now.isBefore(gracePeriodEnd);
            LOGGER.debug("Task {} is final. FinalizedAt: {}, GracePeriodEnd: {}, Now: {}, Active: {}",
                    taskId, finalizedAt, gracePeriodEnd, now, withinGracePeriod);

            return withinGracePeriod;

        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to deserialize task with ID: {}, considering inactive", taskId, e);
            return false;
        }
    }

    /**
     * Determines if a task is in a final state, ignoring the grace period.
     * <p>
     * This method performs an immediate check: returns true only if the task
     * is in a final state (COMPLETED, CANCELED, FAILED, etc.), regardless of when
     * it was finalized.
     * </p>
     * <p>
     * This method is used by the MainQueue.onClose callback to decide whether
     * to publish the QueueClosedEvent "poison pill". By ignoring the grace period,
     * it ensures that subscribers are terminated immediately when the task is done,
     * providing responsive UX.
     * </p>
     *
     * @param taskId the task ID to check
     * @return true if the task is in a final state (ignoring grace period), false otherwise
     */
    @Transactional
    @Override
    public boolean isTaskFinalized(String taskId) {
        LOGGER.debug("Checking if task is finalized: {}", taskId);

        JpaTask jpaTask = em.find(JpaTask.class, taskId);
        if (jpaTask == null) {
            LOGGER.debug("Task not found, considering not finalized: {}", taskId);
            return false;
        }

        try {
            Task task = jpaTask.getTask();

            // Task is finalized if in final state (ignore grace period)
            boolean isFinalized = task.getStatus() != null
                && task.getStatus().state() != null
                && task.getStatus().state().isFinal();

            LOGGER.debug("Task {} finalization check: {}", taskId, isFinalized);
            return isFinalized;

        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to deserialize task with ID: {}, considering not finalized", taskId, e);
            return false;
        }
    }
}
