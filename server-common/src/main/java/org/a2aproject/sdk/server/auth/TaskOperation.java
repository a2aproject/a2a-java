package org.a2aproject.sdk.server.auth;

/**
 * Identifies which {@link org.a2aproject.sdk.server.requesthandlers.RequestHandler} operation
 * triggered an authorization check.
 */
public enum TaskOperation {
    GET_TASK,
    LIST_TASKS,
    CANCEL_TASK,
    MESSAGE_SEND,
    MESSAGE_SEND_STREAM,
    SUBSCRIBE_TO_TASK,
    CREATE_TASK_PUSH_NOTIFICATION_CONFIG,
    GET_TASK_PUSH_NOTIFICATION_CONFIG,
    LIST_TASK_PUSH_NOTIFICATION_CONFIGS,
    DELETE_TASK_PUSH_NOTIFICATION_CONFIG
}
