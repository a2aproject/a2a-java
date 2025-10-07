package io.a2a.server.events;

/**
 * Utils to access package private methods in the io.a2a.server.events package
 */
public class EventQueueTestHelper {
    public static EventQueue tapQueue(EventQueue queue) {
        return queue.tap();
    }
}
