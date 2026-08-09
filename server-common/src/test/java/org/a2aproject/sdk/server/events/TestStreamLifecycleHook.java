package org.a2aproject.sdk.server.events;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.a2aproject.sdk.spec.Event;
import org.jspecify.annotations.Nullable;

/**
 * Test utility for creating {@link TaskStreamLifecycleHook} instances without
 * boilerplate. Unspecified callbacks default to no-ops.
 */
final class TestStreamLifecycleHook implements TaskStreamLifecycleHook {

    @FunctionalInterface
    interface EventCallback {
        void accept(String taskId, Event event, StreamCloseHandle handle);
    }

    private final BiConsumer<String, StreamCloseHandle> subscribeCallback;
    private final BiConsumer<String, StreamCloseHandle> unsubscribeCallback;
    private final EventCallback eventCallback;

    private TestStreamLifecycleHook(
            BiConsumer<String, StreamCloseHandle> subscribeCallback,
            BiConsumer<String, StreamCloseHandle> unsubscribeCallback,
            EventCallback eventCallback) {
        this.subscribeCallback = subscribeCallback;
        this.unsubscribeCallback = unsubscribeCallback;
        this.eventCallback = eventCallback;
    }

    static TestStreamLifecycleHook onSubscribe(BiConsumer<String, StreamCloseHandle> callback) {
        return new TestStreamLifecycleHook(callback, (t, h) -> {}, (t, e, h) -> {});
    }

    static TestStreamLifecycleHook onUnsubscribe(BiConsumer<String, StreamCloseHandle> callback) {
        return new TestStreamLifecycleHook((t, h) -> {}, callback, (t, e, h) -> {});
    }

    static TestStreamLifecycleHook onEvent(EventCallback callback) {
        return new TestStreamLifecycleHook((t, h) -> {}, (t, h) -> {}, callback);
    }

    static CaptureHook capturing() {
        return new CaptureHook();
    }

    @Override
    public void onSubscribe(String taskId, StreamCloseHandle handle) {
        subscribeCallback.accept(taskId, handle);
    }

    @Override
    public void onUnsubscribe(String taskId, StreamCloseHandle handle) {
        unsubscribeCallback.accept(taskId, handle);
    }

    @Override
    public void onEvent(String taskId, Event event, StreamCloseHandle handle) {
        eventCallback.accept(taskId, event, handle);
    }

    /**
     * A hook that records all callback invocations for later assertion.
     */
    static final class CaptureHook implements TaskStreamLifecycleHook {
        private final List<String> subscribedTaskIds = new ArrayList<>();
        private final List<String> unsubscribedTaskIds = new ArrayList<>();
        private final List<Event> receivedEvents = new ArrayList<>();
        private volatile @Nullable StreamCloseHandle lastHandle;

        List<String> subscribedTaskIds() {
            return subscribedTaskIds;
        }

        List<String> unsubscribedTaskIds() {
            return unsubscribedTaskIds;
        }

        List<Event> receivedEvents() {
            return receivedEvents;
        }

        @Nullable StreamCloseHandle lastHandle() {
            return lastHandle;
        }

        @Override
        public void onSubscribe(String taskId, StreamCloseHandle handle) {
            subscribedTaskIds.add(taskId);
            lastHandle = handle;
        }

        @Override
        public void onUnsubscribe(String taskId, StreamCloseHandle handle) {
            unsubscribedTaskIds.add(taskId);
            lastHandle = handle;
        }

        @Override
        public void onEvent(String taskId, Event event, StreamCloseHandle handle) {
            receivedEvents.add(event);
            lastHandle = handle;
        }
    }
}
