package io.a2a.server.events;

import java.util.concurrent.atomic.AtomicInteger;

public class EventQueueUtil {
    // Counter for generating unique test taskIds
    private static final AtomicInteger TASK_ID_COUNTER = new AtomicInteger(0);

    /**
     * Get an EventQueue builder pre-configured with the shared test MainEventBus and a unique taskId.
     * <p>
     * Note: Returns MainQueue - tests should call .tap() if they need to consume events.
     * </p>
     *
     * @return builder with TEST_EVENT_BUS and unique taskId already set
     */
    public static EventQueue.EventQueueBuilder getEventQueueBuilder(MainEventBus eventBus) {
        return EventQueue.builder(eventBus)
                .taskId("test-task-" + TASK_ID_COUNTER.incrementAndGet());
    }

    /**
     * Start a MainEventBusProcessor instance.
     *
     * @param processor the processor to start
     */
    public static void start(MainEventBusProcessor processor) {
        processor.start();
    }

    /**
     * Stop a MainEventBusProcessor instance.
     *
     * @param processor the processor to stop
     */
    public static void stop(MainEventBusProcessor processor) {
        processor.stop();
    }
}
