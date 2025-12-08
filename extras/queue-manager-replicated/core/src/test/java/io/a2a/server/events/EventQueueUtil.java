package io.a2a.server.events;

public class EventQueueUtil {
    // Since EventQueue.builder() is package protected, add a method to expose it
    public static EventQueue.EventQueueBuilder getEventQueueBuilder() {
        return EventQueue.builder();
    }

    public static void start(MainEventBusProcessor processor) {
        processor.start();
    }

    public static void stop(MainEventBusProcessor processor) {
        processor.stop();
    }
}
