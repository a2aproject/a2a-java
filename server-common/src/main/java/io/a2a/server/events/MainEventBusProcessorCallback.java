package io.a2a.server.events;

import io.a2a.spec.Event;

/**
 * Callback interface for MainEventBusProcessor events.
 * <p>
 * This interface is primarily intended for testing, allowing tests to synchronize
 * with the asynchronous MainEventBusProcessor. Production code should not rely on this.
 * </p>
 * Usage in tests:
 * <pre>
 * {@code
 * @Inject
 * MainEventBusProcessor processor;
 *
 * @BeforeEach
 * void setUp() {
 *     CountDownLatch latch = new CountDownLatch(3);
 *     processor.setCallback(new MainEventBusProcessorCallback() {
 *         public void onEventProcessed(String taskId, Event event) {
 *             latch.countDown();
 *         }
 *     });
 * }
 *
 * @AfterEach
 * void tearDown() {
 *     processor.setCallback(null); // Reset to NOOP
 * }
 * }
 * </pre>
 */
public interface MainEventBusProcessorCallback {

    /**
     * Called after an event has been fully processed (persisted, notification sent, distributed to children).
     *
     * @param taskId the task ID
     * @param event the event that was processed
     */
    void onEventProcessed(String taskId, Event event);

    /**
     * Called when a task reaches a final state (COMPLETED, FAILED, CANCELED, REJECTED).
     *
     * @param taskId the task ID that was finalized
     */
    void onTaskFinalized(String taskId);

    /**
     * No-op implementation that does nothing.
     * Used as the default callback to avoid null checks.
     */
    MainEventBusProcessorCallback NOOP = new MainEventBusProcessorCallback() {
        @Override
        public void onEventProcessed(String taskId, Event event) {
            // No-op
        }

        @Override
        public void onTaskFinalized(String taskId) {
            // No-op
        }
    };
}
