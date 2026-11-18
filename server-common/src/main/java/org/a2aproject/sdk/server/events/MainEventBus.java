package org.a2aproject.sdk.server.events;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MainEventBus {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainEventBus.class);
    private final BlockingQueue<Object> queue;

    public MainEventBus() {
        this.queue = new LinkedBlockingDeque<>();
    }

    void submit(String taskId, EventQueue.MainQueue mainQueue, EventQueueItem item) {
        try {
            queue.put(new MainEventBusContext(taskId, mainQueue, item));
            LOGGER.debug("Submitted event for task {} to MainEventBus (queue size: {})",
                        taskId, queue.size());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted submitting to MainEventBus", e);
        }
    }

    void submit(Runnable action) {
        queue.add(action);
    }

    Object take() throws InterruptedException {
        LOGGER.debug("MainEventBus: Waiting to take event (current queue size: {})...", queue.size());
        Object context = queue.take();
        LOGGER.debug("MainEventBus: Took item (remaining queue size: {})", queue.size());
        return context;
    }

    public int size() {
        return queue.size();
    }
}
