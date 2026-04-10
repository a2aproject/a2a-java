package org.a2aproject.sdk.extras.queuemanager.replicated.core;

import org.a2aproject.sdk.spec.Event;

public interface ReplicationStrategy {
    void send(String taskId, Event event);
}