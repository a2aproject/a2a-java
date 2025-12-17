package io.a2a.client;

import io.a2a.spec.Message;

/**
 * A message event received by a client.
 */
public final class MessageEvent implements ClientEvent {

    private final Message message;

    /**
     * A message event.
     *
     * @param message the message received
     */
    public MessageEvent(Message message) {
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }

    @Override
    public String toString() {
        String messageAsString = "{"
                + "role=" + message.role()
                + ", parts=" + message.parts()
                + ", messageId=" + message.messageId()
                + ", contextId=" + message.contextId()
                + ", taskId=" + message.taskId()
                + ", metadata=" + message.metadata()
                + ", kind=" + message.kind()
                + ", referenceTaskIds=" + message.referenceTaskIds()
                + ", extensions=" + message.extensions() + '}';
        return "MessageEvent{" + "message=" + messageAsString + '}';
    }
}
