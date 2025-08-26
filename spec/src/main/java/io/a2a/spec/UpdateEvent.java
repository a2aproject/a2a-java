package io.a2a.spec;

public sealed interface UpdateEvent permits TaskStatusUpdateEvent, TaskArtifactUpdateEvent {
}
