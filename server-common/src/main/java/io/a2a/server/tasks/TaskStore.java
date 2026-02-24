package io.a2a.server.tasks;

import io.a2a.jsonrpc.common.wrappers.ListTasksResult;
import io.a2a.spec.ListTasksParams;
import io.a2a.spec.Task;
import org.jspecify.annotations.Nullable;

/**
 * Storage interface for managing task persistence across the task lifecycle.
 * <p>
 * TaskStore is responsible for persisting task state including status updates, artifacts,
 * message history, and metadata. It's called by {@link io.a2a.server.requesthandlers.DefaultRequestHandler}
 * and {@link TaskManager} to save task state as agents process requests and generate events.
 * </p>
 *
 * <h2>Persistence Guarantees</h2>
 * Tasks are persisted:
 * <ul>
 *   <li>After each status update event (SUBMITTED, WORKING, COMPLETED, etc.)</li>
 *   <li>After each artifact is added</li>
 *   <li>Before events are distributed to clients (ensures consistency)</li>
 *   <li>Before push notifications are sent</li>
 * </ul>
 * Persistence happens synchronously before responses are returned, ensuring clients
 * always see committed state.
 *
 * <h2>Default Implementation</h2>
 * {@link InMemoryTaskStore}:
 * <ul>
 *   <li>Stores tasks in {@link java.util.concurrent.ConcurrentHashMap}</li>
 *   <li>Also implements {@link TaskStateProvider} for queue lifecycle decisions</li>
 *   <li>Thread-safe for concurrent operations</li>
 *   <li>Tasks lost on application restart</li>
 * </ul>
 *
 * <h2>Alternative Implementations</h2>
 * <ul>
 *   <li><b>extras/task-store-database-jpa:</b> {@code JpaDatabaseTaskStore} with PostgreSQL/MySQL persistence</li>
 * </ul>
 * Database implementations:
 * <ul>
 *   <li>Survive application restarts</li>
 *   <li>Enable task sharing across server instances</li>
 *   <li>Typically also implement {@link TaskStateProvider} for integrated state queries</li>
 *   <li>Support transaction boundaries for consistency</li>
 * </ul>
 *
 * <h2>Relationship to TaskStateProvider</h2>
 * Many TaskStore implementations also implement {@link TaskStateProvider} to provide
 * queue lifecycle management with task state information:
 * <pre>{@code
 * @ApplicationScoped
 * public class InMemoryTaskStore implements TaskStore, TaskStateProvider {
 *     // Provides both persistence and state queries
 *     public boolean isTaskFinalized(String taskId) {
 *         Task task = tasks.get(taskId);
 *         return task != null && task.status().state().isFinal();
 *     }
 * }
 * }</pre>
 *
 * <h2>CDI Extension Pattern</h2>
 * <pre>{@code
 * @ApplicationScoped
 * @Alternative
 * @Priority(50)  // Higher than default InMemoryTaskStore
 * public class JpaDatabaseTaskStore implements TaskStore, TaskStateProvider {
 *     @PersistenceContext
 *     EntityManager em;
 *
 *     @Transactional
 *     public void save(Task task) {
 *         TaskEntity entity = toEntity(task);
 *         em.merge(entity);
 *     }
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * Implementations must be thread-safe. Multiple threads will call methods concurrently
 * for different tasks. Concurrent {@code save()} calls for the same task must handle
 * conflicts appropriately (last-write-wins, optimistic locking, etc.).
 *
 * <h2>List Operation Performance</h2>
 * The {@link #list(io.a2a.spec.ListTasksParams)} method may need to scan and filter
 * many tasks. Database implementations should:
 * <ul>
 *   <li>Use indexes on contextId, status, lastUpdatedAt</li>
 *   <li>Implement efficient pagination with stable ordering</li>
 *   <li>Consider caching for frequently-accessed task lists</li>
 * </ul>
 *
 * <h2>Exception Contract</h2>
 * All TaskStore methods may throw {@link TaskStoreException} or its subclasses to indicate
 * persistence failures. Implementers must choose the appropriate exception type based on
 * the failure cause:
 * <ul>
 *   <li>{@link TaskSerializationException} - JSON/data format errors (non-transient)</li>
 *   <li>{@link TaskPersistenceException} - Database/storage failures (transient or non-transient)</li>
 * </ul>
 *
 * <h3>When to Throw TaskSerializationException</h3>
 * Use when task data cannot be serialized or deserialized:
 * <ul>
 *   <li>JSON parsing errors during {@code get()} operations</li>
 *   <li>JSON serialization errors during {@code save()} operations</li>
 *   <li>Invalid enum values or missing required fields</li>
 *   <li>Schema version mismatches after upgrades</li>
 * </ul>
 * These failures are <b>always non-transient</b> - retry will not help. They require data
 * migration, schema updates, or manual intervention.
 *
 * <h3>When to Throw TaskPersistenceException</h3>
 * Use when the storage system fails:
 * <ul>
 *   <li>Database connection timeouts (transient)</li>
 *   <li>Transaction deadlocks (transient)</li>
 *   <li>Connection pool exhausted (transient)</li>
 *   <li>Disk full / quota exceeded (non-transient)</li>
 *   <li>Database constraint violations (non-transient)</li>
 *   <li>Insufficient permissions (non-transient)</li>
 * </ul>
 * Set the {@code isTransient} flag appropriately:
 * <ul>
 *   <li>{@code true} - Temporary failure, retry may succeed (network, timeout, deadlock)</li>
 *   <li>{@code false} - Permanent failure, requires intervention (disk full, constraints)</li>
 * </ul>
 *
 * <h3>Implementer Example</h3>
 * <pre>{@code
 * @Override
 * public void save(Task task, boolean isReplicated) {
 *     try {
 *         String json = objectMapper.writeValueAsString(task);
 *     } catch (JsonProcessingException e) {
 *         throw new TaskSerializationException(task.id(), "Failed to serialize task", e);
 *     }
 *
 *     try {
 *         entityManager.merge(toEntity(json));
 *     } catch (PersistenceException e) {
 *         boolean transient = isTransientDatabaseError(e);
 *         throw new TaskPersistenceException(task.id(), "Database save failed", e, transient);
 *     }
 * }
 *
 * @Override
 * public Task get(String taskId) {
 *     String json = database.retrieve(taskId);
 *     try {
 *         return objectMapper.readValue(json, Task.class);
 *     } catch (JsonProcessingException e) {
 *         throw new TaskSerializationException(taskId, "Failed to deserialize task", e);
 *     }
 * }
 * }</pre>
 *
 * <h3>Caller Exception Handling</h3>
 * Callers should distinguish between transient and permanent failures:
 * <pre>{@code
 * try {
 *     taskStore.save(task, false);
 * } catch (TaskSerializationException e) {
 *     // Non-transient: Log error, notify operations team
 *     logger.error("Task data corruption for {}: {}", e.getTaskId(), e.getMessage(), e);
 *     alerting.sendAlert("Task serialization failure", e);
 *     // DO NOT RETRY - requires manual data repair
 *
 * } catch (TaskPersistenceException e) {
 *     if (e.isTransient()) {
 *         // Transient: Retry with exponential backoff
 *         logger.warn("Transient persistence failure for {}: {}", e.getTaskId(), e.getMessage());
 *         retryWithBackoff(() -> taskStore.save(task, false));
 *     } else {
 *         // Non-transient: Log error, alert operations
 *         logger.error("Permanent persistence failure for {}: {}", e.getTaskId(), e.getMessage(), e);
 *         alerting.sendAlert("Database capacity/constraint issue", e);
 *         // DO NOT RETRY - requires manual intervention
 *     }
 * } catch (TaskStoreException e) {
 *     // Generic fallback - treat as non-transient
 *     logger.error("TaskStore failure for {}: {}", e.getTaskId(), e.getMessage(), e);
 *     // DO NOT RETRY by default
 * }
 * }</pre>
 *
 * <h3>Current Exception Handling</h3>
 * {@link io.a2a.server.events.MainEventBusProcessor} currently catches all TaskStore
 * exceptions and wraps them in {@link io.a2a.spec.InternalError} events for client
 * distribution. Future enhancements may distinguish transient failures for retry logic.
 *
 * <h3>Method-Specific Notes</h3>
 * <ul>
 *   <li>{@code delete()} typically does not throw TaskSerializationException (no deserialization required)</li>
 *   <li>{@code list()} may encounter serialization errors for any task in the result set</li>
 * </ul>
 *
 * @see TaskManager
 * @see TaskStateProvider
 * @see TaskStoreException
 * @see TaskSerializationException
 * @see TaskPersistenceException
 * @see InMemoryTaskStore
 * @see io.a2a.server.requesthandlers.DefaultRequestHandler
 * @see io.a2a.server.events.MainEventBusProcessor
 */
public interface TaskStore {
    /**
     * Saves or updates a task.
     *
     * @param task the task to save
     * @param isReplicated true if this task update came from a replicated event,
     *                     false if it originated locally. Used to prevent feedback loops
     *                     in replicated scenarios (e.g., don't fire TaskFinalizedEvent for replicated updates)
     * @throws TaskSerializationException if the task cannot be serialized to storage format (JSON parsing error,
     *                                    invalid field values, schema mismatch). Non-transient - retry will not help.
     * @throws TaskPersistenceException if the storage system fails (database timeout, connection error, disk full).
     *                                  Check {@link TaskPersistenceException#isTransient()} to determine if retry is appropriate.
     * @throws TaskStoreException for other persistence failures not covered by specific subclasses
     */
    void save(Task task, boolean isReplicated);

    /**
     * Retrieves a task by its ID.
     *
     * @param taskId the task identifier
     * @return the task if found, null otherwise
     * @throws TaskSerializationException if the persisted task data cannot be deserialized (corrupted JSON,
     *                                    schema incompatibility). Non-transient - indicates data corruption.
     * @throws TaskPersistenceException if the storage system fails during retrieval (database connection error,
     *                                  query timeout). Check {@link TaskPersistenceException#isTransient()} for retry guidance.
     * @throws TaskStoreException for other retrieval failures not covered by specific subclasses
     */
    @Nullable Task get(String taskId);

    /**
     * Deletes a task by its ID.
     *
     * @param taskId the task identifier
     * @throws TaskPersistenceException if the storage system fails during deletion (database connection error,
     *                                  transaction timeout, constraint violation). Check {@link TaskPersistenceException#isTransient()}
     *                                  to determine if retry is appropriate.
     * @throws TaskStoreException for other deletion failures not covered by specific subclasses
     */
    void delete(String taskId);

    /**
     * List tasks with optional filtering and pagination.
     *
     * @param params the filtering and pagination parameters
     * @return the list of tasks matching the criteria with pagination info
     * @throws TaskSerializationException if any persisted task data cannot be deserialized during listing
     *                                    (corrupted JSON in database). Non-transient - indicates data corruption affecting
     *                                    one or more tasks.
     * @throws TaskPersistenceException if the storage system fails during the list operation (database query timeout,
     *                                  connection error). Check {@link TaskPersistenceException#isTransient()} for retry guidance.
     * @throws TaskStoreException for other listing failures not covered by specific subclasses
     */
    ListTasksResult list(ListTasksParams params);
}
