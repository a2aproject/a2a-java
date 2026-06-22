package org.a2aproject.sdk.server.auth;

import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.spec.A2AError;

/**
 * SPI for per-user task authorization.
 * <p>
 * Implementers provide a CDI bean ({@code @ApplicationScoped}) implementing this interface
 * to control which users can read, write, or create tasks. When no implementation is provided,
 * all operations are permitted.
 *
 * <h2>Providing an implementation</h2>
 * <p>
 * Create an {@code @ApplicationScoped} CDI bean that implements this interface. The SDK
 * automatically discovers it and wires it into the request pipeline — no additional
 * configuration is required.
 *
 * <pre>{@code
 * @ApplicationScoped
 * public class MyTaskAuthorizationProvider implements TaskAuthorizationProvider {
 *
 *     @Override
 *     public boolean checkRead(ServerCallContext context, String taskId, TaskOperation op) {
 *         User user = context.getUser();
 *         // look up ownership in your backing store
 *         return isOwner(user, taskId);
 *     }
 *
 *     @Override
 *     public boolean checkWrite(ServerCallContext context, String taskId, TaskOperation op) {
 *         return checkRead(context, taskId, op); // same rule
 *     }
 *
 *     @Override
 *     public boolean checkCreate(ServerCallContext context, TaskOperation op) {
 *         return context.getUser().isAuthenticated();
 *     }
 *
 *     @Override
 *     public boolean isTaskRecorded(String taskId) {
 *         return ownershipStore.contains(taskId);
 *     }
 *
 *     @Override
 *     public void recordOwnership(ServerCallContext context, String taskId, TaskOperation op) {
 *         ownershipStore.put(taskId, context.getUser().getUsername());
 *     }
 * }
 * }</pre>
 *
 * <h2>Behavior</h2>
 * <p>
 * When a provider is present, the SDK enforces authorization as follows:
 * <ul>
 *   <li>{@code onGetTask}, {@code onSubscribeToTask}, {@code onGetTaskPushNotificationConfig},
 *       {@code onListTaskPushNotificationConfigs} — call {@link #checkRead}</li>
 *   <li>{@code onCancelTask}, {@code onCreateTaskPushNotificationConfig},
 *       {@code onDeleteTaskPushNotificationConfig} — call {@link #checkWrite}</li>
 *   <li>{@code onMessageSend}, {@code onMessageSendStream} — call {@link #checkWrite} if an
 *       existing task ID is provided, otherwise call {@link #checkCreate}; after the delegate
 *       returns, call {@link #recordOwnership} if a new task was created</li>
 *   <li>{@code onListTasks} — filtering is pushed down to the {@code TaskStore}, which calls
 *       {@link #checkRead} per task to exclude unauthorized entries</li>
 * </ul>
 * Denied operations throw {@code TaskNotFoundError} — the caller cannot distinguish
 * "does not exist" from "not authorized", preventing information leakage.
 *
 * <h2>Thread safety</h2>
 * <p>
 * Implementations must be thread-safe. Methods will be called concurrently from multiple
 * requests.
 *
 * <h2>Ownership recording</h2>
 * <p>
 * {@link #recordOwnership} is only triggered by {@code onMessageSend} and
 * {@code onMessageSendStream} — the methods that can create tasks.
 * Other methods ({@code onGetTask}, {@code onCancelTask}, etc.) do not trigger recording.
 * {@link #checkRead}/{@link #checkWrite} may be called for tasks the provider has no ownership
 * data for (e.g., legacy tasks created before the provider was enabled). For production
 * deployments, a <b>fail-closed</b> policy is recommended: deny access when no ownership
 * data exists. An {@code owner == null → allow} policy is only appropriate for testing or
 * single-user deployments. If enabling the provider on an existing deployment, consider a
 * migration step to backfill ownership for pre-existing tasks.
 *
 * <h2>Common pitfalls</h2>
 * <ul>
 *   <li><b>TOCTOU race on ownership recording:</b> The {@link #isTaskRecorded} →
 *       {@link #recordOwnership} sequence is not atomic. Two concurrent {@code onMessageSend}
 *       calls for the same new task can both see {@code isTaskRecorded()} return {@code false}
 *       and both call {@code recordOwnership}. Implementations must use atomic-insert patterns
 *       (e.g., {@code ConcurrentMap.putIfAbsent}, {@code INSERT ... ON CONFLICT DO NOTHING})
 *       so the first writer wins and the second is a harmless no-op.</li>
 *   <li><b>CDI injection requirement:</b> When task authorization is required, always obtain
 *       {@code RequestHandler} through CDI injection. Manual instantiation via
 *       {@code DefaultRequestHandler.create()} bypasses the
 *       {@code AuthorizationRequestHandlerDecorator}.</li>
 * </ul>
 *
 * @see TaskOperation
 */
public interface TaskAuthorizationProvider {

    /**
     * Check whether the current user is allowed to read the given task.
     *
     * @param context the server call context containing the authenticated user
     * @param taskId the task being accessed
     * @param operation which RequestHandler method triggered the check
     * @return {@code true} to allow, {@code false} to deny
     * @throws A2AError if the authorization check itself fails
     */
    boolean checkRead(ServerCallContext context, String taskId, TaskOperation operation) throws A2AError;

    /**
     * Check whether the current user is allowed to write to the given task.
     *
     * @param context the server call context containing the authenticated user
     * @param taskId the task being accessed
     * @param operation which RequestHandler method triggered the check
     * @return {@code true} to allow, {@code false} to deny
     * @throws A2AError if the authorization check itself fails
     */
    boolean checkWrite(ServerCallContext context, String taskId, TaskOperation operation) throws A2AError;

    /**
     * Check whether the current user is allowed to create a new task.
     *
     * @param context the server call context containing the authenticated user
     * @param operation which RequestHandler method triggered the check
     * @return {@code true} to allow, {@code false} to deny
     * @throws A2AError if the authorization check itself fails
     */
    boolean checkCreate(ServerCallContext context, TaskOperation operation) throws A2AError;

    /**
     * Check whether the given task is already known to this provider.
     * Used to avoid redundant {@link #recordOwnership} calls.
     *
     * @param taskId the task to check
     * @return {@code true} if ownership has already been recorded for this task
     * @throws A2AError if the check itself fails
     */
    boolean isTaskRecorded(String taskId) throws A2AError;

    /**
     * Record that the current user owns the given task. Called after task creation
     * via {@code onMessageSend} or {@code onMessageSendStream}.
     * <p>
     * <b>Must be idempotent.</b> Concurrent requests for the same unrecorded task may both
     * call this method before either completes.
     *
     * @param context the server call context containing the authenticated user
     * @param taskId the newly created task
     * @param operation which RequestHandler method triggered the recording
     * @throws A2AError if recording fails
     */
    void recordOwnership(ServerCallContext context, String taskId, TaskOperation operation) throws A2AError;
}
