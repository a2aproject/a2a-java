package io.a2a.server.events;

/**
 * Exception thrown when attempting to dequeue from a closed and empty event queue.
 * This signals to consumers that no more events will be available from the queue.
 */
public class EventQueueClosedException extends Exception {
}
