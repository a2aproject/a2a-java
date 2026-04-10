package org.a2aproject.sdk.compat03.client;

import org.a2aproject.sdk.compat03.spec.Message;

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
}


