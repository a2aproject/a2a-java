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
                + "role=" + message.getRole()
                + ", parts=" + message.getParts()
                + ", messageId=" + message.getMessageId()
                + ", contextId=" + message.getContextId()
                + ", taskId=" + message.getTaskId()
                + ", metadata=" + message.getMetadata()
                + ", kind=" + message.getKind()
                + ", referenceTaskIds=" + message.getReferenceTaskIds()
                + ", extensions=" + message.getExtensions() + '}';
        return "MessageEvent{" + "message=" + messageAsString + '}';
    }
}
