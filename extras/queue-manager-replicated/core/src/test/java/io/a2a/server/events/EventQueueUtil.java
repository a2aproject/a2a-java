package io.a2a.server.events;

public class EventQueueUtil {
    public static void start(MainEventBusProcessor processor) {
        processor.start();
    }

    public static void stop(MainEventBusProcessor processor) {
        processor.stop();
    }
}
