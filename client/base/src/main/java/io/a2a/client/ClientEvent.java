package io.a2a.client;

public sealed interface ClientEvent permits MessageEvent, TaskEvent, TaskUpdateEvent {
}
