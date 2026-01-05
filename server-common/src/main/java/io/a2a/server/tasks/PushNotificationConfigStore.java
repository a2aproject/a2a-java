package io.a2a.server.tasks;

import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigResult;
import io.a2a.spec.PushNotificationConfig;

/**
 * Interface for storing and retrieving push notification configurations for tasks.
 * <p>
 * Push notification configurations specify where and how to deliver task state updates
 * to external systems (webhook URLs, authentication headers, etc.). A task can have
 * multiple push notification configurations for different endpoints or use cases.
 * </p>
 *
 * <h2>Configuration ID Semantics</h2>
 * Each push notification config has an ID:
 * <ul>
 *   <li>If not provided in {@link PushNotificationConfig}, defaults to the task ID</li>
 *   <li>Multiple configs per task require unique IDs (e.g., "webhook-1", "webhook-2")</li>
 *   <li>Used for retrieval and deletion of specific configurations</li>
 * </ul>
 *
 * <h2>Pagination Support</h2>
 * {@link #getInfo(ListTaskPushNotificationConfigParams)} supports pagination for tasks
 * with many push notification configurations:
 * <ul>
 *   <li><b>pageSize:</b> Maximum number of configs to return (0 = unlimited)</li>
 *   <li><b>pageToken:</b> Continuation token from previous response</li>
 *   <li>Returns {@link ListTaskPushNotificationConfigResult} with configs and next page token</li>
 * </ul>
 *
 * <h2>Default Implementation</h2>
 * {@link InMemoryPushNotificationConfigStore}:
 * <ul>
 *   <li>Stores configs in {@link java.util.concurrent.ConcurrentHashMap}</li>
 *   <li>Thread-safe for concurrent operations</li>
 *   <li>Configs lost on application restart</li>
 * </ul>
 *
 * <h2>Alternative Implementations</h2>
 * <ul>
 *   <li><b>extras/push-notification-config-store-database-jpa:</b> Database persistence with JPA</li>
 * </ul>
 * Database implementations survive restarts and enable config sharing across server instances.
 *
 * <h2>CDI Extension Pattern</h2>
 * <pre>{@code
 * @ApplicationScoped
 * @Alternative
 * @Priority(50)
 * public class JpaDatabasePushNotificationConfigStore implements PushNotificationConfigStore {
 *     @PersistenceContext
 *     EntityManager em;
 *
 *     @Transactional
 *     public PushNotificationConfig setInfo(String taskId, PushNotificationConfig config) {
 *         // JPA persistence logic
 *     }
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * Implementations must be thread-safe. Multiple threads may call methods concurrently
 * for the same or different tasks.
 *
 * @see PushNotificationSender
 * @see InMemoryPushNotificationConfigStore
 * @see io.a2a.spec.PushNotificationConfig
 */
public interface PushNotificationConfigStore {

    /**
     * Sets or updates the push notification configuration for a task.
     * <p>
     * If {@code notificationConfig.id()} is null or empty, it's set to the task ID.
     * If a config with the same ID already exists for this task, it's replaced.
     * </p>
     *
     * @param taskId the task ID
     * @param notificationConfig the push notification configuration
     * @return the potentially updated configuration (with ID set if it was null)
     */
    PushNotificationConfig setInfo(String taskId, PushNotificationConfig notificationConfig);

    /**
     * Retrieves push notification configurations for a task with pagination support.
     * <p>
     * Returns all configs if {@code params.pageSize()} is 0. Otherwise, returns up to
     * {@code pageSize} configs and a continuation token for the next page.
     * <p>
     * <b>Pagination Example:</b>
     * <pre>{@code
     * // First page
     * ListTaskPushNotificationConfigParams params =
     *     new ListTaskPushNotificationConfigParams(taskId, 10, null, tenant);
     * ListTaskPushNotificationConfigResult result = store.getInfo(params);
     *
     * // Next page
     * if (result.nextPageToken() != null) {
     *     params = new ListTaskPushNotificationConfigParams(
     *         taskId, 10, result.nextPageToken(), tenant);
     *     result = store.getInfo(params);
     * }
     * }</pre>
     *
     * @param params the query parameters including task ID, page size, and page token
     * @return the configurations for this task (empty list if none found)
     */
    ListTaskPushNotificationConfigResult getInfo(ListTaskPushNotificationConfigParams params);

    /**
     * Deletes a push notification configuration for a task.
     * <p>
     * If {@code configId} is null, it defaults to the task ID (deletes the default config).
     * If no config exists with the given ID, this method returns normally (idempotent).
     * </p>
     *
     * @param taskId the task ID
     * @param configId the push notification configuration ID (null = use task ID)
     */
    void deleteInfo(String taskId, String configId);

}
