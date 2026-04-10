package org.a2aproject.sdk.compat03.spec;

public sealed interface UpdateEvent permits TaskStatusUpdateEvent, TaskArtifactUpdateEvent {
}
