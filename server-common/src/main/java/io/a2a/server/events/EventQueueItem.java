package io.a2a.server.events;

import io.a2a.spec.Event;

/**
 * Represents an item that can be enqueued/dequeued in an EventQueue.
 * <p>
 * This abstraction allows the queue to track whether events originated locally
 * or were received from replication, enabling proper handling of the replication loop
 * prevention mechanism.
 * </p>
 */
public interface EventQueueItem {

    /**
     * Gets the event contained in this queue item.
     *
     * @return the event
     */
    Event getEvent();

    /**
     * Checks if this item represents a replicated event.
     * <p>
     * Replicated events are those received from other nodes in the cluster via
     * the replication mechanism (e.g., Kafka). Local events are those enqueued
     * directly by the local agent execution.
     * </p>
     * <p>
     * This distinction is important to prevent replication loops - replicated events
     * should not be re-replicated to other nodes.
     * </p>
     *
     * @return true if this event was replicated from another node, false if it's a local event
     */
    boolean isReplicated();
}
