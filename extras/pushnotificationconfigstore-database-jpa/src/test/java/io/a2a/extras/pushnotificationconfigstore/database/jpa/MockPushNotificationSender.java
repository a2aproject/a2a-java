package io.a2a.extras.pushnotificationconfigstore.database.jpa;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import io.a2a.server.tasks.PushNotificationSender;
import io.a2a.spec.Task;

/**
 * Mock implementation of PushNotificationSender for integration testing.
 * Captures notifications in a thread-safe queue for test verification.
 */
@ApplicationScoped
@Alternative
@Priority(100)
public class MockPushNotificationSender implements PushNotificationSender {

    private final Queue<Task> capturedTasks = new ConcurrentLinkedQueue<>();

    @Override
    public void sendNotification(Task task) {
        capturedTasks.add(task);
    }

    public Queue<Task> getCapturedTasks() {
        return capturedTasks;
    }

    public void clear() {
        capturedTasks.clear();
    }
}
