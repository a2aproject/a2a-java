package org.a2aproject.sdk.server.events;

public class EventQueueUtil {
    public static void start(MainEventBusProcessor processor) {
        processor.start();
    }

    public static void stop(MainEventBusProcessor processor) {
        processor.stop();
    }
}
