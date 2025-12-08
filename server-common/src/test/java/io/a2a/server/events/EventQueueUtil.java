package io.a2a.server.events;

import io.a2a.server.tasks.InMemoryTaskStore;
import io.a2a.server.tasks.PushNotificationSender;
import io.a2a.server.tasks.TaskStateProvider;
import java.util.concurrent.atomic.AtomicInteger;

public class EventQueueUtil {
    // Shared MainEventBus for all tests (to avoid creating one per test)
    private static final MainEventBus TEST_EVENT_BUS = new MainEventBus();

    // Shared MainEventBusProcessor for all tests (automatically processes events)
    private static final MainEventBusProcessor TEST_PROCESSOR;

    static {
        // Initialize and start the processor once for all tests
        InMemoryTaskStore testTaskStore = new InMemoryTaskStore();
        PushNotificationSender testPushSender = taskId -> {}; // No-op for tests
        TEST_PROCESSOR = new MainEventBusProcessor(TEST_EVENT_BUS, testTaskStore, testPushSender);
        TEST_PROCESSOR.start();  // Start background thread

        // Register shutdown hook to stop processor
        Runtime.getRuntime().addShutdownHook(new Thread(() -> TEST_PROCESSOR.stop()));
    }

    // Counter for generating unique test taskIds
    private static final AtomicInteger TASK_ID_COUNTER = new AtomicInteger(0);

    // Since EventQueue.builder() is package protected, add a method to expose it
    // Note: Now includes MainEventBus requirement and default taskId
    // Returns MainQueue - tests should call .tap() if they need to consume events
    public static EventQueue.EventQueueBuilder getEventQueueBuilder() {
        return new EventQueueBuilderWrapper(
                EventQueue.builder(TEST_EVENT_BUS)
                        .taskId("test-task-" + TASK_ID_COUNTER.incrementAndGet())
        );
    }

    // Get the shared test MainEventBus instance
    public static MainEventBus getTestEventBus() {
        return TEST_EVENT_BUS;
    }

    public static void start(MainEventBusProcessor processor) {
        processor.start();
    }

    public static void stop(MainEventBusProcessor processor) {
        processor.stop();
    }

    // Wrapper that delegates to actual builder
    private static class EventQueueBuilderWrapper extends EventQueue.EventQueueBuilder {
        private final EventQueue.EventQueueBuilder delegate;

        EventQueueBuilderWrapper(EventQueue.EventQueueBuilder delegate) {
            this.delegate = delegate;
        }

        @Override
        public EventQueue.EventQueueBuilder queueSize(int queueSize) {
            delegate.queueSize(queueSize);
            return this;
        }

        @Override
        public EventQueue.EventQueueBuilder hook(EventEnqueueHook hook) {
            delegate.hook(hook);
            return this;
        }

        @Override
        public EventQueue.EventQueueBuilder taskId(String taskId) {
            delegate.taskId(taskId);
            return this;
        }

        @Override
        public EventQueue.EventQueueBuilder addOnCloseCallback(Runnable onCloseCallback) {
            delegate.addOnCloseCallback(onCloseCallback);
            return this;
        }

        @Override
        public EventQueue.EventQueueBuilder taskStateProvider(TaskStateProvider taskStateProvider) {
            delegate.taskStateProvider(taskStateProvider);
            return this;
        }

        @Override
        public EventQueue.EventQueueBuilder mainEventBus(MainEventBus mainEventBus) {
            delegate.mainEventBus(mainEventBus);
            return this;
        }

        @Override
        public EventQueue build() {
            // Return MainQueue directly - tests should call .tap() if they need ChildQueue
            return delegate.build();
        }
    }
}
