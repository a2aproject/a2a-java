package org.a2aproject.sdk.server.tasks;

import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsParams;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsResult;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.jspecify.annotations.Nullable;

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
 *   <li>If not provided in {@link TaskPushNotificationConfig}, defaults to the task ID</li>
 *   <li>Multiple configs per task require unique IDs (e.g., "webhook-1", "webhook-2")</li>
 *   <li>Used for retrieval and deletion of specific configurations</li>
 * </ul>
 *
 * <h2>Pagination Support</h2>
 * {@link #getInfo(ListTaskPushNotificationConfigsParams)} supports pagination for tasks
 * with many push notification configurations:
 * <ul>
 *   <li><b>pageSize:</b> Maximum number of configs to return (0 = unlimited)</li>
 *   <li><b>pageToken:</b> Continuation token from previous response</li>
 *   <li>Returns {@link ListTaskPushNotificationConfigsResult} with configs and next page token</li>
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
 *     public TaskPushNotificationConfig setInfo(TaskPushNotificationConfig config) {
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
 * @see org.a2aproject.sdk.spec.TaskPushNotificationConfig
 */
public interface PushNotificationConfigStore {

    /**
     * Sets or updates the push notification configuration for a task.
     * <p>
     * If {@code notificationConfig.id()} is null or empty, it's set to the task ID.
     * If a config with the same ID already exists for this task, it's replaced.
     * </p>
     *
     * @param notificationConfig the task push notification configuration
     * @return the potentially updated configuration (with ID set if it was null)
     */
    TaskPushNotificationConfig setInfo(TaskPushNotificationConfig notificationConfig);

    /**
     * Sets or updates the push notification configuration for a task, along with the
     * protocol version that registered it.
     * <p>
     * This merged method ensures the protocol version is stored using the normalized
     * config ID (after {@link #setInfo(TaskPushNotificationConfig)} applies defaults).
     * </p>
     *
     * @param notificationConfig the task push notification configuration
     * @param protocolVersion the protocol version string, or null to use {@link org.a2aproject.sdk.spec.AgentInterface#CURRENT_PROTOCOL_VERSION}
     * @return the potentially updated configuration (with ID set if it was null)
     */
    default TaskPushNotificationConfig setInfo(TaskPushNotificationConfig notificationConfig, @Nullable String protocolVersion) {
        return setInfo(notificationConfig);
    }

    /**
     * Resolves the protocol version, defaulting null to the current protocol version.
     */
    static String resolveProtocolVersion(@Nullable String protocolVersion) {
        return protocolVersion != null ? protocolVersion : org.a2aproject.sdk.spec.AgentInterface.CURRENT_PROTOCOL_VERSION;
    }

    /**
     * Retrieves push notification configurations for a task with pagination support.
     * <p>
     * Returns all configs if {@code params.pageSize()} is 0. Otherwise, returns up to
     * {@code pageSize} configs and a continuation token for the next page.
     * <p>
     * <b>Pagination Example:</b>
     * <pre>{@code
     * // First page
     * ListTaskPushNotificationConfigsParams params =
     *     new ListTaskPushNotificationConfigsParams(taskId, 10, null, tenant);
     * ListTaskPushNotificationConfigsResult result = store.getInfo(params);
     *
     * // Next page
     * if (result.nextPageToken() != null) {
     *     params = new ListTaskPushNotificationConfigsParams(
     *         taskId, 10, result.nextPageToken(), tenant);
     *     result = store.getInfo(params);
     * }
     * }</pre>
     *
     * @param params the query parameters including task ID, page size, and page token
     * @return the configurations for this task (empty list if none found)
     */
    ListTaskPushNotificationConfigsResult getInfo(ListTaskPushNotificationConfigsParams params);

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

    /**
     * Gets the protocol version associated with a push notification configuration.
     *
     * @param taskId the task ID
     * @param configId the push notification configuration ID
     * @return the protocol version string, defaults to the current protocol version if not set
     */
    default String getProtocolVersion(String taskId, String configId) {
        return resolveProtocolVersion(null);
    }

    /**
     * Gets all protocol versions for a task's push notification configurations in a single call.
     *
     * @param taskId the task ID
     * @return a map of config ID to protocol version (only includes configs with a version set)
     */
    default java.util.Map<String, String> getProtocolVersions(String taskId) {
        return java.util.Map.of();
    }

}
