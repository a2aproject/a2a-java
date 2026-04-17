package org.a2aproject.sdk.compat03.client;

public sealed interface ClientEvent permits MessageEvent, TaskEvent, TaskUpdateEvent {
}
