package io.a2a.extras.taskstore.database.jpa;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

import io.a2a.extras.common.events.TaskFinalizedEvent;
import io.a2a.jsonrpc.common.json.JsonProcessingException;
import io.a2a.jsonrpc.common.wrappers.ListTasksResult;
import io.a2a.server.config.A2AConfigProvider;
import io.a2a.server.tasks.TaskStateProvider;
import io.a2a.server.tasks.TaskStore;
import io.a2a.spec.Artifact;
import io.a2a.spec.ListTasksParams;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Alternative
@Priority(50)
public class JpaDatabaseTaskStore implements TaskStore, TaskStateProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaDatabaseTaskStore.class);
    private static final String A2A_REPLICATION_GRACE_PERIOD_SECONDS = "a2a.replication.grace-period-seconds";

    @PersistenceContext(unitName = "a2a-java")
    EntityManager em;

    @Inject
    Event<TaskFinalizedEvent> taskFinalizedEvent;

    @Inject
    A2AConfigProvider configProvider;

    /**
     * Grace period for task finalization in replicated scenarios (seconds).
     * After a task reaches a final state, this is the minimum time to wait before cleanup
     * to allow replicated events to arrive and be processed.
     * <p>
     * Property: {@code a2a.replication.grace-period-seconds}<br>
     * Default: 15<br>
     * Note: Property override requires a configurable {@link A2AConfigProvider} on the classpath.
     */
    long gracePeriodSeconds;

    @PostConstruct
    void initConfig() {
        gracePeriodSeconds = Long.parseLong(configProvider.getValue(A2A_REPLICATION_GRACE_PERIOD_SECONDS));
    }

    @Transactional
    @Override
    public void save(Task task) {
        LOGGER.debug("Saving task with ID: {}", task.id());
        try {
            JpaTask jpaTask = JpaTask.createFromTask(task);
            em.merge(jpaTask);
            LOGGER.debug("Persisted/updated task with ID: {}", task.id());

            if (task.status() != null && task.status().state() != null && task.status().state().isFinal()) {
                // Fire CDI event if task reached final state
                // IMPORTANT: The event will be delivered AFTER transaction commits (AFTER_SUCCESS observers)
                // This ensures the task's final state is durably stored before the QueueClosedEvent poison pill is sent
                LOGGER.debug("Task {} is in final state, firing TaskFinalizedEvent", task.id());
                taskFinalizedEvent.fire(new TaskFinalizedEvent(task.id()));
            }
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to serialize task with ID: {}", task.id(), e);
            throw new RuntimeException("Failed to serialize task with ID: " + task.id(), e);
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
     * <p>A task is active if:</p>
     * <ul>
     *   <li>Its state is not final, OR</li>
     *   <li>Its state is final but it was finalized within the grace period</li>
     * </ul>
     * <p>
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
            if (task.status() == null || task.status().state() == null || !task.status().state().isFinal()) {
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
            boolean isFinalized = task.status() != null
                && task.status().state() != null
                && task.status().state().isFinal();

            LOGGER.debug("Task {} finalization check: {}", taskId, isFinalized);
            return isFinalized;

        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to deserialize task with ID: {}, considering not finalized", taskId, e);
            return false;
        }
    }

    @Transactional
    @Override
    public ListTasksResult list(ListTasksParams params) {
        LOGGER.debug("Listing tasks with params: contextId={}, status={}, pageSize={}, pageToken={}",
                params.contextId(), params.status(), params.pageSize(), params.pageToken());

        // Parse pageToken once at the beginning
        Instant tokenTimestamp = null;
        String tokenId = null;
        if (params.pageToken() != null && !params.pageToken().isEmpty()) {
            String[] tokenParts = params.pageToken().split(":", 2);
            if (tokenParts.length == 2) {
                try {
                    long timestampMillis = Long.parseLong(tokenParts[0]);
                    tokenId = tokenParts[1];
                    tokenTimestamp = Instant.ofEpochMilli(timestampMillis);
                } catch (NumberFormatException e) {
                    throw new io.a2a.spec.InvalidParamsError(null,
                        "Invalid pageToken format: timestamp must be numeric milliseconds", null);
                }
            } else {
                throw new io.a2a.spec.InvalidParamsError(null, "Invalid pageToken format: expected 'timestamp:id'", null);
            }
        }

        // Build dynamic JPQL query with WHERE clauses for filtering
        StringBuilder queryBuilder = new StringBuilder("SELECT t FROM JpaTask t WHERE 1=1");
        StringBuilder countQueryBuilder = new StringBuilder("SELECT COUNT(t) FROM JpaTask t WHERE 1=1");

        // Apply contextId filter using denormalized column
        if (params.contextId() != null) {
            queryBuilder.append(" AND t.contextId = :contextId");
            countQueryBuilder.append(" AND t.contextId = :contextId");
        }

        // Apply status filter using denormalized column
        if (params.status() != null) {
            queryBuilder.append(" AND t.state = :state");
            countQueryBuilder.append(" AND t.state = :state");
        }

        // Apply statusTimestampAfter filter using denormalized timestamp column
        if (params.statusTimestampAfter() != null) {
            queryBuilder.append(" AND t.statusTimestamp > :statusTimestampAfter");
            countQueryBuilder.append(" AND t.statusTimestamp > :statusTimestampAfter");
        }

        // Apply pagination cursor using keyset pagination for composite sort (timestamp DESC, id ASC)
        if (tokenTimestamp != null) {
            // Keyset pagination: get tasks where timestamp < tokenTimestamp OR (timestamp = tokenTimestamp AND id > tokenId)
            queryBuilder.append(" AND (t.statusTimestamp < :tokenTimestamp OR (t.statusTimestamp = :tokenTimestamp AND t.id > :tokenId))");
        }

        // Sort by status timestamp descending (most recent first), then by ID for stable ordering
        queryBuilder.append(" ORDER BY t.statusTimestamp DESC, t.id ASC");

        // Create and configure the main query
        TypedQuery<JpaTask> query = em.createQuery(queryBuilder.toString(), JpaTask.class);

        // Set filter parameters
        if (params.contextId() != null) {
            query.setParameter("contextId", params.contextId());
        }
        if (params.status() != null) {
            query.setParameter("state", params.status().asString());
        }
        if (params.statusTimestampAfter() != null) {
            query.setParameter("statusTimestampAfter", params.statusTimestampAfter());
        }
        if (tokenTimestamp != null) {
            query.setParameter("tokenTimestamp", tokenTimestamp);
            query.setParameter("tokenId", tokenId);
        }

        // Apply page size limit (+1 to check for next page)
        int pageSize = params.getEffectivePageSize();
        query.setMaxResults(pageSize + 1);

        // Execute query and deserialize tasks
        List<JpaTask> jpaTasksPage = query.getResultList();

        // Determine if there are more results
        boolean hasMore = jpaTasksPage.size() > pageSize;
        if (hasMore) {
            jpaTasksPage = jpaTasksPage.subList(0, pageSize);
        }

        // Get total count of matching tasks
        TypedQuery<Long> countQuery = em.createQuery(countQueryBuilder.toString(), Long.class);
        if (params.contextId() != null) {
            countQuery.setParameter("contextId", params.contextId());
        }
        if (params.status() != null) {
            countQuery.setParameter("state", params.status().asString());
        }
        if (params.statusTimestampAfter() != null) {
            countQuery.setParameter("statusTimestampAfter", params.statusTimestampAfter());
        }
        int totalSize = countQuery.getSingleResult().intValue();

        // Deserialize tasks from JSON
        List<Task> tasks = new ArrayList<>();
        for (JpaTask jpaTask : jpaTasksPage) {
            try {
                tasks.add(jpaTask.getTask());
            } catch (JsonProcessingException e) {
                LOGGER.error("Failed to deserialize task with ID: {}", jpaTask.getId(), e);
                throw new RuntimeException("Failed to deserialize task with ID: " + jpaTask.getId(), e);
            }
        }

        // Determine next page token (timestamp:ID of last task if there are more results)
        // Format: "timestamp_millis:taskId" for keyset pagination
        String nextPageToken = null;
        if (hasMore && !tasks.isEmpty()) {
            Task lastTask = tasks.get(tasks.size() - 1);
            // All tasks have timestamps (TaskStatus canonical constructor ensures this)
            long timestampMillis = lastTask.status().timestamp().toInstant().toEpochMilli();
            nextPageToken = timestampMillis + ":" + lastTask.id();
        }

        // Apply post-processing transformations (history limiting, artifact removal)
        int historyLength = params.getEffectiveHistoryLength();
        boolean includeArtifacts = params.shouldIncludeArtifacts();

        List<Task> transformedTasks = tasks.stream()
                .map(task -> transformTask(task, historyLength, includeArtifacts))
                .toList();

        LOGGER.debug("Returning {} tasks out of {} total", transformedTasks.size(), totalSize);
        return new ListTasksResult(transformedTasks, totalSize, transformedTasks.size(), nextPageToken);
    }

    private Task transformTask(Task task, int historyLength, boolean includeArtifacts) {
        // Limit history if needed (keep most recent N messages)
        List<Message> history = task.history();
        if (historyLength == 0) {
            // When historyLength is 0, return empty history
            history = List.of();
        } else if (historyLength > 0 && history != null && history.size() > historyLength) {
            history = history.subList(history.size() - historyLength, history.size());
        }

        // Remove artifacts if not requested
        List<Artifact> artifacts = includeArtifacts ? task.artifacts() : List.of();

        // If no transformation needed, return original task
        if (history == task.history() && artifacts == task.artifacts()) {
            return task;
        }

        // Build new task with transformed data
        return Task.builder(task)
                .artifacts(artifacts)
                .history(history)
                .build();
    }
}
