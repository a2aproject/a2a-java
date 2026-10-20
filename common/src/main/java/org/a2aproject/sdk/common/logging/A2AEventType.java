package org.a2aproject.sdk.common.logging;

public enum A2AEventType {
    TASK_REQUEST("task.request"),
    TASK_STATE_CHANGE("task.state_change"),
    TASK_ARTIFACT("task.artifact"),
    TASK_RESULT("task.result"),
    NEGOTIATION("negotiation"),
    AUTHORIZATION("authorization"),
    NOTIFICATION_SUBSCRIPTION("notification.subscription"),
    NOTIFICATION_DELIVERY("notification.delivery"),
    STREAMING_EVENT("streaming.event");

    private final String value;
    A2AEventType(String value) { this.value = value; }
    public String value() { return value; }
    @Override public String toString() { return value; }
}
