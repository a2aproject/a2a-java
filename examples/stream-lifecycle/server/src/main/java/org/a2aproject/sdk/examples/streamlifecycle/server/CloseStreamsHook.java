package org.a2aproject.sdk.examples.streamlifecycle.server;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import org.a2aproject.sdk.server.events.StreamCloseHandle;
import org.a2aproject.sdk.server.events.TaskStreamLifecycleHook;
import org.a2aproject.sdk.spec.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Demonstrates closing all streams for a task when the subscriber count reaches a threshold.
 * <p>
 * When 3 subscribers are connected to the same task, this hook calls
 * {@link StreamCloseHandle#closeStreams()} to gracefully close all active streams.
 * The agent executor continues running, but disconnected clients can resubscribe later.
 * </p>
 */
@ApplicationScoped
@Alternative
@Priority(1)
public class CloseStreamsHook implements TaskStreamLifecycleHook {

    private static final Logger LOG = LoggerFactory.getLogger(CloseStreamsHook.class);
    private static final int MAX_SUBSCRIBERS = 3;

    @Override
    public void onSubscribe(String taskId, StreamCloseHandle handle) {
        int count = handle.getActiveSubscriberCount();
        LOG.info("[HOOK] Subscriber added for task {}. Active subscribers: {}", taskId, count);

        if (count >= MAX_SUBSCRIBERS) {
            LOG.info("[HOOK] Subscriber count reached {} for task {} — closing all streams",
                    MAX_SUBSCRIBERS, taskId);
            handle.closeStreams();
        }
    }

    @Override
    public void onUnsubscribe(String taskId, StreamCloseHandle handle) {
        LOG.info("[HOOK] Subscriber removed for task {}. Active subscribers: {}",
                taskId, handle.getActiveSubscriberCount());
    }

    @Override
    public void onEvent(String taskId, Event event, StreamCloseHandle handle) {
        LOG.info("[HOOK] Event distributed for task {}: {} (subscribers: {})",
                taskId, event.getClass().getSimpleName(), handle.getActiveSubscriberCount());
    }
}
