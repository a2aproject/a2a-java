package org.a2aproject.sdk.extras.taskstore.database.jpa;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

import org.a2aproject.sdk.extras.common.events.TaskFinalizedEvent;
import org.a2aproject.sdk.jsonrpc.common.json.JsonProcessingException;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.TaskAuthorizationProvider;
import org.a2aproject.sdk.server.auth.TaskOperation;
import org.a2aproject.sdk.server.config.A2AConfigProvider;
import org.a2aproject.sdk.server.tasks.TaskPersistenceException;
import org.a2aproject.sdk.server.tasks.TaskSerializationException;
import org.a2aproject.sdk.server.tasks.TaskStateProvider;
import org.a2aproject.sdk.server.tasks.TaskStore;
import org.a2aproject.sdk.server.tasks.TaskStoreException;
import org.a2aproject.sdk.server.util.CdiUtils;
import org.a2aproject.sdk.spec.Artifact;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.util.PageToken;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task persistence in the {@code a2a-java} JTA persistence unit. Register both
 * {@link JpaTask} and {@link JpaTaskOwner} and apply the application-managed schema
 * migration described in this module's README, including the exact binary ownership index.
 *
 * <p>Authorized listings request {@link JpaTaskAuthorizationCapability} from the configured
 * provider. Supported scopes restrict both database count and page queries; only the page
 * query uses the cursor. Unsupported providers use bounded per-task scanning and fail when
 * an exact count exceeds the configured scan ceiling. Missing call context fails closed.
 * Adding this store does not select an authorization provider.
 */
@ApplicationScoped
@Alternative
@Priority(50)
public class JpaDatabaseTaskStore implements TaskStore, TaskStateProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaDatabaseTaskStore.class);
    private static final String A2A_REPLICATION_GRACE_PERIOD_SECONDS = "a2a.replication.grace-period-seconds";
    private static final String A2A_REPLICATION_MAX_SCANNED_TASKS = "a2a.max-scanned-tasks";
    private static final String A2A_AUTHORIZATION_SCAN_BATCH_SIZE = "a2a.authorization-scan-batch-size";
    private static final int DEFAULT_AUTHORIZATION_SCAN_BATCH_SIZE = 100;

    @PersistenceContext(unitName = "a2a-java")
    private EntityManager em;

    @Inject
    private Event<TaskFinalizedEvent> taskFinalizedEvent;

    @Inject
    private A2AConfigProvider configProvider;

    private final @Nullable TaskAuthorizationProvider authorizationProvider;

    public JpaDatabaseTaskStore() {
        this.authorizationProvider = null;
    }

    @Inject
    public JpaDatabaseTaskStore(@Any Instance<TaskAuthorizationProvider> authorizationProviderInstance) {
        this.authorizationProvider = CdiUtils.getIfResolvable(authorizationProviderInstance);
    }

    /**
     * Grace period for task finalization in replicated scenarios (seconds).
     * After a task reaches a final state, this is the minimum time to wait before cleanup
     * to allow replicated events to arrive and be processed.
     * <p>
     * Property: {@code a2a.replication.grace-period-seconds}<br>
     * Default: 15<br>
     * Note: Property override requires a configurable {@link A2AConfigProvider} on the classpath.
     */
    private long gracePeriodSeconds;
    /**
     * Maximum number of candidate rows inspected by the iterative authorization fallback.
     * Capability-based database queries do not use this limit.
     * An exact authorized count cannot be returned once this limit is exceeded, so the listing
     * fails instead of returning a partial count.
     */
    private long maxScanned;
    /**
     * Number of candidate rows per iterative authorization fallback query only.
     */
    private int authorizationScanBatchSize = DEFAULT_AUTHORIZATION_SCAN_BATCH_SIZE;

    @PostConstruct
    void initConfig() {
        gracePeriodSeconds = Long.parseLong(configProvider.getValue(A2A_REPLICATION_GRACE_PERIOD_SECONDS));
        maxScanned = Long.parseLong(configProvider.getValue(A2A_REPLICATION_MAX_SCANNED_TASKS));
        authorizationScanBatchSize = Integer.parseInt(configProvider.getValue(A2A_AUTHORIZATION_SCAN_BATCH_SIZE));
        if (maxScanned <= 0) {
            throw new TaskStoreException("a2a.max-scanned-tasks must be greater than zero");
        }
        if (authorizationScanBatchSize <= 0) {
            throw new TaskStoreException("a2a.authorization-scan-batch-size must be greater than zero");
        }
    }


    @Transactional
    @Override
    public void save(Task task, boolean isReplicated) {
        LOGGER.debug("Saving task with ID: {} (replicated: {})", task.id(), isReplicated);
        try {
            JpaTask jpaTask = JpaTask.createFromTask(task);
            em.merge(jpaTask);
            LOGGER.debug("Persisted/updated task with ID: {}", task.id());

            // Only fire TaskFinalizedEvent for locally-generated final states, NOT for replicated events
            // This prevents feedback loops where receiving a replicated final task triggers another replication
            if (!isReplicated && task.status() != null && task.status().state() != null && task.status().state().isFinal()) {
                // Fire CDI event if task reached final state
                // IMPORTANT: The event will be delivered AFTER transaction commits (AFTER_SUCCESS observers)
                // This ensures the task's final state is durably stored before the final task and poison pill are sent
                LOGGER.debug("Task {} is in final state, firing TaskFinalizedEvent with full Task", task.id());
                taskFinalizedEvent.fire(new TaskFinalizedEvent(task.id(), task));
            } else if (isReplicated && task.status() != null && task.status().state() != null && task.status().state().isFinal()) {
                LOGGER.debug("Task {} is in final state but from replication - NOT firing TaskFinalizedEvent (prevents feedback loop)", task.id());
            }
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to serialize task with ID: {}", task.id(), e);
            throw new TaskSerializationException(task.id(),
                "Failed to serialize task for persistence", e);
        } catch (PersistenceException e) {
            LOGGER.error("Database save failed for task with ID: {}", task.id(), e);
            throw new TaskPersistenceException(task.id(),
                "Database save failed for task", e);
        }
    }

    @Transactional
    @Override
    public Task get(String taskId) {
        LOGGER.debug("Retrieving task with ID: {}", taskId);
        try {
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
                throw new TaskSerializationException(taskId,
                    "Failed to deserialize task from database", e);
            }

        } catch (PersistenceException e) {
            LOGGER.error("Database retrieval failed for task with ID: {}", taskId, e);
            throw new TaskPersistenceException(taskId,
                "Database retrieval failed for task", e);
        }
    }

    @Transactional
    @Override
    public void delete(String taskId) {
        LOGGER.debug("Deleting task with ID: {}", taskId);
        try {
            em.createQuery("DELETE FROM JpaTaskOwner o WHERE o.taskId = :taskId")
                    .setParameter("taskId", taskId).executeUpdate();
            JpaTask jpaTask = em.find(JpaTask.class, taskId);
            if (jpaTask != null) {
                em.remove(jpaTask);
                LOGGER.debug("Successfully deleted task with ID: {}", taskId);
            } else {
                LOGGER.debug("Task not found for deletion with ID: {}", taskId);
            }
        } catch (PersistenceException e) {
            LOGGER.error("Database deletion failed for task with ID: {}", taskId, e);
            throw new TaskPersistenceException(taskId,
                "Database deletion failed for task", e);
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
    public ListTasksResult list(ListTasksParams params, @Nullable ServerCallContext context) {
        return listWithAuthorization(params, context, authorizationProvider);
    }

    @Transactional
    @Override
    public ListTasksResult listAuthorized(ListTasksParams params, ServerCallContext context,
            TaskAuthorizationProvider provider) {
        return listWithAuthorization(params, context, provider);
    }

    private ListTasksResult listWithAuthorization(ListTasksParams params, @Nullable ServerCallContext context,
            @Nullable TaskAuthorizationProvider provider) {
        LOGGER.debug("Listing tasks with params: contextId={}, status={}, pageSize={}, pageToken={}",
                params.contextId(), params.status(), params.pageSize(), params.pageToken());

        try {
            if (provider != null && context == null) {
                LOGGER.warn("Authorization provider is configured but no ServerCallContext available — "
                        + "returning empty result (fail-closed)");
                return new ListTasksResult(List.of(), 0, 0, null);
            }
            Optional<JpaTaskAuthorizationCapability> capability = provider == null ? Optional.empty()
                    : provider.getCapability(JpaTaskAuthorizationCapability.class);
            JpaTaskReadScope scope = capability.isPresent()
                    ? java.util.Objects.requireNonNull(capability.get().readScope(context, TaskOperation.LIST_TASKS)) : null;
            if (scope instanceof JpaTaskReadScope.None) {
                return new ListTasksResult(List.of(), 0, 0, null);
            }
            byte[] subjectKey = scope instanceof JpaTaskReadScope.OwnedBy owned
                    ? JpaSubjectKey.encode(owned.subjectKey()) : null;
            int pageSize = params.getEffectivePageSize();

            // Build base WHERE clause (without cursor — shared across iterations)
            String baseWhereClause = buildBaseWhereClause(params);
            if (subjectKey != null) {
                baseWhereClause += " AND EXISTS (SELECT 1 FROM JpaTaskOwner o"
                        + " WHERE o.taskId = t.id AND o.subjectKey = :subjectKey)";
            }

            List<Task> tasks;
            boolean hasMore;
            int totalSize;
            PageToken cursor = PageToken.fromString(params.pageToken());
            PageToken scanCursor = cursor;

            if (provider != null && capability.isEmpty()) {
                AuthorizedTaskCount authorizedTaskCount = countAuthorizedTasks(
                        baseWhereClause, params, cursor, context, provider);
                tasks = new ArrayList<>(pageSize);
                boolean dbExhausted = false;
                long totalScanned = 0;

                while (tasks.size() < pageSize && !dbExhausted && totalScanned < maxScanned) {
                    long remaining = maxScanned - totalScanned;
                    int limit = (int) Math.min(remaining, authorizationScanBatchSize);
                    TypedQuery<JpaTask> query = createPageQuery(
                            baseWhereClause, params, scanCursor, limit);
                    List<JpaTask> batch = query.getResultList();

                    dbExhausted = batch.size() < limit;
                    int batchEnd = (int) Math.min(batch.size(), remaining);
                    int processedCount = 0;
                    for (int i = 0; i < batchEnd; i++) {
                        JpaTask jpaTask = batch.get(i);
                        processedCount++;
                        totalScanned++;
                        Task task = deserializeTask(jpaTask);
                        if (isReadAuthorized(provider, context, task.id())) {
                            tasks.add(task);
                            if (tasks.size() == pageSize) {
                                break;
                            }
                        }
                    }
                    if (processedCount > 0) {
                        JpaTask last = batch.get(processedCount - 1);
                        scanCursor = new PageToken(last.getStatusTimestamp(), last.getId());
                    }
                }

                hasMore = authorizedTaskCount.totalSize() > authorizedTaskCount.beforePage() + tasks.size();
                totalSize = authorizedTaskCount.totalSize();
            } else {
                // Supported authorization scopes are enforced by the database.
                totalSize = executeCountQuery(baseWhereClause, params, subjectKey);
                TypedQuery<JpaTask> query = createPageQuery(
                        baseWhereClause, params, cursor, pageSize + 1, subjectKey);
                List<JpaTask> jpaTasksPage = query.getResultList();

                hasMore = jpaTasksPage.size() > pageSize;
                int batchEnd = Math.min(jpaTasksPage.size(), pageSize);

                tasks = new ArrayList<>(batchEnd);
                for (int i = 0; i < batchEnd; i++) {
                    tasks.add(deserializeTask(jpaTasksPage.get(i)));
                }
            }

            // Determine next page token from the last returned task
            String nextPageToken = null;
            if (hasMore && !tasks.isEmpty()) {
                Task lastTask = tasks.get(tasks.size() - 1);
                Instant timestamp = lastTask.status().timestamp().toInstant();
                nextPageToken = new PageToken(timestamp, lastTask.id()).toString();
            } else if (hasMore) {
                nextPageToken = scanCursor == null ? null : scanCursor.toString();
            }

            // Apply post-processing transformations (history limiting, artifact removal)
            int historyLength = params.getEffectiveHistoryLength();
            boolean includeArtifacts = params.shouldIncludeArtifacts();

            List<Task> transformedTasks = tasks.stream()
                    .map(task -> transformTask(task, historyLength, includeArtifacts))
                    .toList();

            LOGGER.debug("Returning {} tasks out of {} total", transformedTasks.size(), totalSize);
            return new ListTasksResult(transformedTasks, totalSize, transformedTasks.size(), nextPageToken);

        } catch (PersistenceException e) {
            LOGGER.error("Database query failed during list operation", e);
            throw new TaskPersistenceException(null,
                "Database query failed during list operation", e);
        }
    }

    private String buildBaseWhereClause(ListTasksParams params) {
        StringBuilder sb = new StringBuilder(" WHERE 1=1");
        if (params.contextId() != null) {
            sb.append(" AND t.contextId = :contextId");
        }
        if (params.status() != null) {
            sb.append(" AND t.state = :state");
        }
        if (params.statusTimestampAfter() != null) {
            sb.append(" AND t.statusTimestamp > :statusTimestampAfter");
        }
        return sb.toString();
    }

    private TypedQuery<JpaTask> createPageQuery(String baseWhereClause,
            ListTasksParams params, @org.jspecify.annotations.Nullable PageToken cursor, int maxResults) {
        return createPageQuery(baseWhereClause, params, cursor, maxResults, null);
    }

    private TypedQuery<JpaTask> createPageQuery(String baseWhereClause,
            ListTasksParams params, @Nullable PageToken cursor, int maxResults, byte @Nullable [] subjectKey) {
        StringBuilder jpql = new StringBuilder("SELECT t FROM JpaTask t").append(baseWhereClause);
        if (cursor != null) {
            jpql.append(" AND (t.statusTimestamp < :tokenTimestamp"
                    + " OR (t.statusTimestamp = :tokenTimestamp AND t.id > :tokenId))");
        }
        jpql.append(" ORDER BY t.statusTimestamp DESC, t.id ASC");

        TypedQuery<JpaTask> query = em.createQuery(jpql.toString(), JpaTask.class);
        setFilterParameters(query, params);
        if (subjectKey != null) { query.setParameter("subjectKey", subjectKey); }
        if (cursor != null) {
            query.setParameter("tokenTimestamp", cursor.timestamp());
            query.setParameter("tokenId", cursor.id());
        }
        query.setMaxResults(maxResults);
        return query;
    }

    private int executeCountQuery(String baseWhereClause, ListTasksParams params, byte @Nullable [] subjectKey) {
        String jpql = "SELECT COUNT(t) FROM JpaTask t" + baseWhereClause;
        TypedQuery<Long> countQuery = em.createQuery(jpql, Long.class);
        setFilterParameters(countQuery, params);
        if (subjectKey != null) { countQuery.setParameter("subjectKey", subjectKey); }
        return countQuery.getSingleResult().intValue();
    }

    /**
     * Counts every authorized candidate and the authorized candidates at or before the requested
     * cursor. The latter lets the page scan determine whether another authorized task exists
     * without advertising a token that can only lead to an empty page.
     */
    private AuthorizedTaskCount countAuthorizedTasks(String baseWhereClause, ListTasksParams params,
            @Nullable PageToken pageCursor, ServerCallContext context, TaskAuthorizationProvider provider) {
        PageToken countCursor = null;
        long scanned = 0;
        int totalSize = 0;
        int beforePage = 0;

        while (true) {
            long remaining = maxScanned - scanned;
            if (remaining <= 0) {
                throw new TaskStoreException("Authorized task listing exceeded its scan limit");
            }
            int batchSize = (int) Math.min(remaining, authorizationScanBatchSize);
            // Fetch a look-ahead row. It distinguishes an exhausted database from a full final
            // batch without inspecting an additional candidate for authorization.
            List<JpaTask> batch = createPageQuery(baseWhereClause, params, countCursor, batchSize + 1)
                    .getResultList();
            int processedCount = Math.min(batch.size(), batchSize);
            for (int i = 0; i < processedCount; i++) {
                JpaTask jpaTask = batch.get(i);
                scanned++;
                if (isReadAuthorized(provider, context, jpaTask.getId())) {
                    totalSize++;
                    if (isAtOrBeforeCursor(jpaTask, pageCursor)) {
                        beforePage++;
                    }
                }
            }
            if (batch.size() <= batchSize) {
                return new AuthorizedTaskCount(totalSize, beforePage);
            }
            JpaTask last = batch.get(processedCount - 1);
            countCursor = new PageToken(last.getStatusTimestamp(), last.getId());
        }
    }

    private static boolean isAtOrBeforeCursor(JpaTask task, @Nullable PageToken cursor) {
        if (cursor == null) {
            return false;
        }
        int timestampComparison = task.getStatusTimestamp().compareTo(cursor.timestamp());
        return timestampComparison > 0
                || (timestampComparison == 0 && task.getId().compareTo(cursor.id()) <= 0);
    }

    private record AuthorizedTaskCount(int totalSize, int beforePage) {
    }

    private void setFilterParameters(TypedQuery<?> query, ListTasksParams params) {
        if (params.contextId() != null) {
            query.setParameter("contextId", params.contextId());
        }
        if (params.status() != null) {
            query.setParameter("state", params.status().name());
        }
        if (params.statusTimestampAfter() != null) {
            query.setParameter("statusTimestampAfter", params.statusTimestampAfter());
        }
    }

    private Task deserializeTask(JpaTask jpaTask) {
        try {
            return jpaTask.getTask();
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to deserialize task with ID: {}", jpaTask.getId(), e);
            throw new TaskSerializationException(jpaTask.getId(),
                    "Failed to deserialize task during list operation", e);
        }
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
