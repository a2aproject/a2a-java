package org.a2aproject.sdk.server.events;

import org.a2aproject.sdk.spec.Event;
import org.a2aproject.sdk.util.Assert;

/**
 * Represents a locally-generated event in the queue.
 * <p>
 * Local events are those enqueued directly by the agent executor on this node,
 * as opposed to events received via replication from other nodes.
 * </p>
 */
class LocalEventQueueItem implements EventQueueItem {

    private final Event event;

    LocalEventQueueItem(Event event) {
        Assert.checkNotNullParam("event", event);
        this.event = event;
    }

    @Override
    public Event getEvent() {
        return event;
    }

    @Override
    public boolean isReplicated() {
        return false;
    }
}
