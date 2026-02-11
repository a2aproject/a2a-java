
package io.a2a.spec;

/**
 * Constants defining the A2A protocol method names.
 */
public interface A2AMethods {
    /** Method name for canceling a task. */
    String CANCEL_TASK_METHOD = "CancelTask";
    /** Method name for deleting task push notification configuration. */
    String DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD = "DeleteTaskPushNotificationConfig";
    /** Method name for getting extended agent card information. */
    String GET_EXTENDED_AGENT_CARD_METHOD = "GetExtendedAgentCard";
    /** Method name for getting a task. */
    String GET_TASK_METHOD = "GetTask";
    /** Method name for getting task push notification configuration. */
    String GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD = "GetTaskPushNotificationConfig";
    /** Method name for listing tasks. */
    String LIST_TASK_METHOD = "ListTasks";
    /** Method name for listing task push notification configurations. */
    String LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD = "ListTaskPushNotificationConfig";
    /** Method name for sending a message. */
    String SEND_MESSAGE_METHOD = "SendMessage";
    /** Method name for sending a streaming message. */
    String SEND_STREAMING_MESSAGE_METHOD = "SendStreamingMessage";
    /** Method name for setting task push notification configuration. */
    String SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD = "CreateTaskPushNotificationConfig";
    /** Method name for subscribing to task events. */
    String SUBSCRIBE_TO_TASK_METHOD = "SubscribeToTask";
    

}
