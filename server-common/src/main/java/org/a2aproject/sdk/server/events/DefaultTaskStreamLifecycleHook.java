package org.a2aproject.sdk.server.events;

import jakarta.enterprise.context.ApplicationScoped;

import org.a2aproject.sdk.spec.Event;

/**
 * Default no-op implementation of {@link TaskStreamLifecycleHook}.
 * Override with a CDI {@code @Alternative @Priority} bean to provide custom stream lifecycle behavior.
 */
@ApplicationScoped
public class DefaultTaskStreamLifecycleHook implements TaskStreamLifecycleHook {

    @Override
    public void onSubscribe(String taskId, StreamCloseHandle handle) {
    }

    @Override
    public void onUnsubscribe(String taskId, StreamCloseHandle handle) {
    }

    @Override
    public void onEvent(String taskId, Event event, StreamCloseHandle handle) {
    }
}
