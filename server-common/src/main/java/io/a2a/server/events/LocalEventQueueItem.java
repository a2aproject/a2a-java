package io.a2a.server.events;

import io.a2a.spec.Event;
import io.a2a.util.Assert;

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
