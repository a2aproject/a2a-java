package io.a2a.server.tasks;

import io.a2a.spec.Task;

/**
 * Interface for delivering push notifications containing task state updates to external systems.
 * <p>
 * Push notifications enable asynchronous, out-of-band communication of task progress to
 * configured webhook URLs or messaging systems. This allows clients to receive updates
 * without maintaining persistent connections or polling.
 * </p>
 *
 * <h2>Invocation Context</h2>
 * Called by {@link io.a2a.server.requesthandlers.DefaultRequestHandler} after:
 * <ul>
 *   <li>Task events are persisted to {@link TaskStore}</li>
 *   <li>Events are returned/streamed to the requesting client</li>
 *   <li>For streaming: after each event emission to the client</li>
 *   <li>For blocking: after the initial response is returned</li>
 * </ul>
 * <p>
 * Push notifications are always sent AFTER the task state is persisted and the client
 * has received the event, ensuring consistency.
 * </p>
 *
 * <h2>Default Implementation</h2>
 * {@link BasePushNotificationSender} provides HTTP webhook delivery:
 * <ul>
 *   <li>Retrieves webhook URLs from {@link PushNotificationConfigStore}</li>
 *   <li>Sends HTTP POST requests with task JSON payload</li>
 *   <li>Logs errors but doesn't fail the request</li>
 * </ul>
 *
 * <h2>Alternative Implementations</h2>
 * Custom implementations can deliver notifications via:
 * <ul>
 *   <li>Kafka topics for event streaming</li>
 *   <li>AWS SNS/SQS for cloud messaging</li>
 *   <li>WebSockets for real-time browser updates</li>
 *   <li>Custom messaging protocols</li>
 * </ul>
 *
 * <h2>CDI Extension Pattern</h2>
 * <pre>{@code
 * @ApplicationScoped
 * @Alternative
 * @Priority(100)
 * public class KafkaPushNotificationSender implements PushNotificationSender {
 *     @Inject
 *     KafkaProducer<String, Task> producer;
 *
 *     @Override
 *     public void sendNotification(Task task) {
 *         producer.send("task-updates", task.id(), task);
 *     }
 * }
 * }</pre>
 *
 * <h2>Error Handling</h2>
 * Implementations should handle errors gracefully:
 * <ul>
 *   <li>Log failures but don't throw exceptions (notifications are best-effort)</li>
 *   <li>Consider retry logic for transient failures</li>
 *   <li>Don't block on network I/O - execute asynchronously if needed</li>
 *   <li>Circuit breaker patterns for repeatedly failing endpoints</li>
 * </ul>
 * Throwing exceptions from this method will not fail the client request, but will
 * be logged as errors.
 *
 * <h2>Thread Safety</h2>
 * May be called from multiple threads concurrently for different tasks.
 * Implementations must be thread-safe.
 *
 * @see PushNotificationConfigStore
 * @see BasePushNotificationSender
 * @see io.a2a.spec.PushNotificationConfig
 */
public interface PushNotificationSender {

    /**
     * Sends a push notification containing the latest task state.
     * <p>
     * Called after the task has been persisted to {@link TaskStore}. Retrieve push
     * notification URLs or messaging configurations from {@link PushNotificationConfigStore}
     * using {@code task.id()}.
     * </p>
     * <p>
     * <b>Error Handling:</b> Log errors but don't throw exceptions. Notifications are
     * best-effort and should not fail the primary request.
     * </p>
     *
     * @param task the task with current state and artifacts to send
     */
    void sendNotification(Task task);
}
