package org.a2aproject.sdk.compat03.client.transport.spi;

import java.util.List;
import java.util.function.Consumer;

import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallContext_v0_3;
import org.a2aproject.sdk.compat03.spec.A2AClientException_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.EventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.ListTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.MessageSendParams_v0_3;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskIdParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskPushNotificationConfig_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskQueryParams_v0_3;
import org.jspecify.annotations.Nullable;

/**
 * Interface for a client transport.
 */
public interface ClientTransport_v0_3 {

    /**
     * Send a non-streaming message request to the agent.
     *
     * @param request the message send parameters
     * @param context optional client call context for the request (may be {@code null})
     * @return the response, either a Task or Message
     * @throws A2AClientException_v0_3 if sending the message fails for any reason
     */
    EventKind_v0_3 sendMessage(MessageSendParams_v0_3 request, @Nullable ClientCallContext_v0_3 context)
            throws A2AClientException_v0_3;

    /**
     * Send a streaming message request to the agent and receive responses as they arrive.
     *
     * @param request       the message send parameters
     * @param eventConsumer consumer that will receive streaming events as they arrive
     * @param errorConsumer consumer that will be called if an error occurs during streaming
     * @param context       optional client call context for the request (may be {@code null})
     * @throws A2AClientException_v0_3 if setting up the streaming connection fails
     */
    void sendMessageStreaming(MessageSendParams_v0_3 request, Consumer<StreamingEventKind_v0_3> eventConsumer,
                              Consumer<Throwable> errorConsumer, @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3;

    /**
     * Retrieve the current state and history of a specific task.
     *
     * @param request the task query parameters specifying which task to retrieve
     * @param context optional client call context for the request (may be {@code null})
     * @return the task
     * @throws A2AClientException_v0_3 if retrieving the task fails for any reason
     */
    Task_v0_3 getTask(TaskQueryParams_v0_3 request, @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3;

    /**
     * Request the agent to cancel a specific task.
     *
     * @param request the task ID parameters specifying which task to cancel
     * @param context optional client call context for the request (may be {@code null})
     * @return the cancelled task
     * @throws A2AClientException_v0_3 if cancelling the task fails for any reason
     */
    Task_v0_3 cancelTask(TaskIdParams_v0_3 request, @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3;

    /**
     * Set or update the push notification configuration for a specific task.
     *
     * @param request the push notification configuration to set for the task
     * @param context optional client call context for the request (may be {@code null})
     * @return the configured TaskPushNotificationConfig
     * @throws A2AClientException_v0_3 if setting the task push notification configuration fails for any reason
     */
    TaskPushNotificationConfig_v0_3 setTaskPushNotificationConfiguration(TaskPushNotificationConfig_v0_3 request,
                                                                         @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3;

    /**
     * Retrieve the push notification configuration for a specific task.
     *
     * @param request the parameters specifying which task's notification config to retrieve
     * @param context optional client call context for the request (may be {@code null})
     * @return the task push notification config
     * @throws A2AClientException_v0_3 if getting the task push notification config fails for any reason
     */
    TaskPushNotificationConfig_v0_3 getTaskPushNotificationConfiguration(
            GetTaskPushNotificationConfigParams_v0_3 request,
            @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3;

    /**
     * Retrieve the list of push notification configurations for a specific task.
     *
     * @param request the parameters specifying which task's notification configs to retrieve
     * @param context optional client call context for the request (may be {@code null})
     * @return the list of task push notification configs
     * @throws A2AClientException_v0_3 if getting the task push notification configs fails for any reason
     */
    List<TaskPushNotificationConfig_v0_3> listTaskPushNotificationConfigurations(
            ListTaskPushNotificationConfigParams_v0_3 request,
            @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3;

    /**
     * Delete the list of push notification configurations for a specific task.
     *
     * @param request the parameters specifying which task's notification configs to delete
     * @param context optional client call context for the request (may be {@code null})
     * @throws A2AClientException_v0_3 if deleting the task push notification configs fails for any reason
     */
    void deleteTaskPushNotificationConfigurations(
            DeleteTaskPushNotificationConfigParams_v0_3 request,
            @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3;

    /**
     * Reconnect to get task updates for an existing task.
     *
     * @param request       the task ID parameters specifying which task to resubscribe to
     * @param eventConsumer consumer that will receive streaming events as they arrive
     * @param errorConsumer consumer that will be called if an error occurs during streaming
     * @param context       optional client call context for the request (may be {@code null})
     * @throws A2AClientException_v0_3 if resubscribing to the task fails for any reason
     */
    void resubscribe(TaskIdParams_v0_3 request, Consumer<StreamingEventKind_v0_3> eventConsumer,
                     Consumer<Throwable> errorConsumer, @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3;

    /**
     * Retrieve the AgentCard.
     *
     * @param context optional client call context for the request (may be {@code null})
     * @return the AgentCard
     * @throws A2AClientException_v0_3 if retrieving the agent card fails for any reason
     */
    AgentCard_v0_3 getAgentCard(@Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3;

    /**
     * Close the transport and release any associated resources.
     */
    void close();

}
