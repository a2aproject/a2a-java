package io.a2a.transport.spi.client;

import io.a2a.spec.*;

import java.util.List;
import java.util.function.Consumer;

public interface Transport {

    /**
     * Send a message to the remote agent.
     *
     * @param requestId the request ID to use
     * @param messageSendParams the parameters for the message to be sent
     * @return the response, may contain a message or a task
     * @throws A2AServerException if sending the message fails for any reason
     */
    EventKind sendMessage(String requestId, MessageSendParams messageSendParams) throws A2AServerException;

    /**
     * Retrieve the generated artifacts for a task.
     *
     * @param requestId the request ID to use
     * @param taskQueryParams the params for the task to be queried
     * @return the response containing the task
     * @throws A2AServerException if retrieving the task fails for any reason
     */
    Task getTask(String requestId, TaskQueryParams taskQueryParams) throws A2AServerException;

    /**
     * Cancel a task that was previously submitted to the A2A server.
     *
     * @param requestId the request ID to use
     * @param taskIdParams the params for the task to be cancelled
     * @return the response indicating if the task was cancelled
     * @throws A2AServerException if retrieving the task fails for any reason
     */
    Task cancelTask(String requestId, TaskIdParams taskIdParams) throws A2AServerException;

    /**
     * Get the push notification configuration for a task.
     *
     * @param requestId the request ID to use
     * @param getTaskPushNotificationConfigParams the params for the task
     * @return the response containing the push notification configuration
     * @throws A2AServerException if getting the push notification configuration fails for any reason
     */
    TaskPushNotificationConfig getTaskPushNotificationConfig(String requestId, GetTaskPushNotificationConfigParams getTaskPushNotificationConfigParams) throws A2AServerException;

    /**
     * Set push notification configuration for a task.
     *
     * @param requestId the request ID to use
     * @param taskId the task ID
     * @param pushNotificationConfig the push notification configuration
     * @return the response indicating whether setting the task push notification configuration succeeded
     * @throws A2AServerException if setting the push notification configuration fails for any reason
     */
    TaskPushNotificationConfig setTaskPushNotificationConfig(String requestId, String taskId,
                                                                               PushNotificationConfig pushNotificationConfig) throws A2AServerException;

    /**
     * Retrieves the push notification configurations for a specified task.
     *
     * @param requestId the request ID to use
     * @param listTaskPushNotificationConfigParams the params for retrieving the push notification configuration
     * @return the response containing the push notification configuration
     * @throws A2AServerException if getting the push notification configuration fails for any reason
     */
    List<TaskPushNotificationConfig> listTaskPushNotificationConfig(String requestId,
                                                                    ListTaskPushNotificationConfigParams listTaskPushNotificationConfigParams) throws A2AServerException;

    /**
     * Delete the push notification configuration for a specified task.
     *
     * @param requestId the request ID to use
     * @param deleteTaskPushNotificationConfigParams the params for deleting the push notification configuration
     * @throws A2AServerException if deleting the push notification configuration fails for any reason
     */
    void deleteTaskPushNotificationConfig(String requestId,
                                                                                     DeleteTaskPushNotificationConfigParams deleteTaskPushNotificationConfigParams) throws A2AServerException;

    /**
     * Send a streaming message to the remote agent.
     *
     * @param requestId the request ID to use
     * @param messageSendParams the parameters for the message to be sent
     * @param eventHandler a consumer that will be invoked for each event received from the remote agent
     * @param errorHandler a consumer that will be invoked if the remote agent returns an error
     * @param failureHandler a consumer that will be invoked if a failure occurs when processing events
     * @throws A2AServerException if sending the streaming message fails for any reason
     */
    void sendStreamingMessage(String requestId, MessageSendParams messageSendParams, Consumer<StreamingEventKind> eventHandler,
                                     Consumer<JSONRPCError> errorHandler, Runnable failureHandler) throws A2AServerException;

    /**
     * Resubscribe to an ongoing task.
     *
     * @param requestId the request ID to use
     * @param taskIdParams the params for the task to resubscribe to
     * @param eventHandler a consumer that will be invoked for each event received from the remote agent
     * @param errorHandler a consumer that will be invoked if the remote agent returns an error
     * @param failureHandler a consumer that will be invoked if a failure occurs when processing events
     * @throws A2AServerException if resubscribing to the task fails for any reason
     */
    void resubscribeToTask(String requestId, TaskIdParams taskIdParams, Consumer<StreamingEventKind> eventHandler,
                                  Consumer<JSONRPCError> errorHandler, Runnable failureHandler) throws A2AServerException;

}
