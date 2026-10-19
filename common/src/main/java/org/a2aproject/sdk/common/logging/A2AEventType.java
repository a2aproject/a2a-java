package org.a2aproject.sdk.common.logging;

/**
 * Enumeration of A2A protocol payload event types for structured logging.
 * <p>
 * Each event type corresponds to a category of protocol message that can be
 * logged with structured fields for observability and debugging.
 * </p>
 */
public enum A2AEventType {
    /**
     * Task request: message/send, message/stream, getTask, listTasks, cancelTask, etc.
     */
    TASK_REQUEST("task.request"),

    /**
     * Task state change: TaskStatusUpdateEvent and status transitions.
     */
    STATE_CHANGE("task.state_change"),

    /**
     * Result artifact: TaskArtifactUpdateEvent and artifact payloads.
     */
    ARTIFACT("task.artifact"),

    /**
     * Task result: final Task snapshot returned to client.
     */
    TASK_RESULT("task.result"),

    /**
     * Negotiation payload: extension negotiation, version negotiation, AgentCard capabilities.
     */
    NEGOTIATION("negotiation"),

    /**
     * Authorization decision: allow/deny from TaskAuthorizationProvider.
     */
    AUTHORIZATION("authorization"),

    /**
     * Notification subscription: push config create/get/list/delete, subscribeToTask.
     */
    NOTIFICATION_SUBSCRIPTION("notification.subscription"),

    /**
     * Notification delivery: push webhook dispatch to external URL.
     */
    NOTIFICATION_DELIVERY("notification.delivery"),

    /**
     * Streaming event: per-event SSE streaming payload.
     */
    STREAMING_EVENT("streaming.event"),

    /**
     * Generic payload for any other structured log.
     */
    GENERIC("generic");

    private final String value;

    A2AEventType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
