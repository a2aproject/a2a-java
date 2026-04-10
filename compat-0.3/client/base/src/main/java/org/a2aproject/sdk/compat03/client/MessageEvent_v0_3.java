package org.a2aproject.sdk.compat03.client;

import org.a2aproject.sdk.compat03.spec.Message_v0_3;

/**
 * A message event received by a client.
 */
public final class MessageEvent_v0_3 implements ClientEvent_v0_3 {

    private final Message_v0_3 message;

    /**
     * A message event.
     *
     * @param message the message received
     */
    public MessageEvent_v0_3(Message_v0_3 message) {
        this.message = message;
    }

    public Message_v0_3 getMessage() {
        return message;
    }
}


