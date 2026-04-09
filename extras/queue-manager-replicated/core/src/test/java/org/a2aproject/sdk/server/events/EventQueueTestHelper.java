package org.a2aproject.sdk.server.events;

/**
 * Utils to access package private methods in the org.a2aproject.sdk.server.events package
 */
public class EventQueueTestHelper {
    public static EventQueue tapQueue(EventQueue queue) {
        return queue.tap();
    }
}
