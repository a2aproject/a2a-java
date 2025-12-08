package io.a2a.server.events;

public class EventQueueUtil {
    // Shared MainEventBus for all tests - ensures events are properly distributed
    private static final MainEventBus TEST_EVENT_BUS = new MainEventBus();

    // Since EventQueue.builder() is package protected, add a method to expose it
    public static EventQueue.EventQueueBuilder getEventQueueBuilder() {
        return EventQueue.builder(TEST_EVENT_BUS);
    }

    public static void start(MainEventBusProcessor processor) {
        processor.start();
    }

    public static void stop(MainEventBusProcessor processor) {
        processor.stop();
    }
}
