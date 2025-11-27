package io.a2a.extras.pushnotificationconfigstore.database.jpa;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.a2a.spec.StreamingEventKind;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import io.a2a.server.tasks.PushNotificationSender;

/**
 * Mock implementation of PushNotificationSender for integration testing.
 * Captures notifications in a thread-safe queue for test verification.
 */
@ApplicationScoped
@Alternative
@Priority(100)
public class MockPushNotificationSender implements PushNotificationSender {

    private final Queue<StreamingEventKind> capturedEvents = new ConcurrentLinkedQueue<>();

    @Override
    public void sendNotification(StreamingEventKind kind) {
        capturedEvents.add(kind);
    }

    public Queue<StreamingEventKind> getCapturedEvents() {
        return capturedEvents;
    }

    public void clear() {
        capturedEvents.clear();
    }
}
