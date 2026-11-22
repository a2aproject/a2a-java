package org.a2aproject.sdk.server.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.a2aproject.sdk.server.tasks.InMemoryTaskStore;
import org.a2aproject.sdk.server.tasks.PushNotificationSender;
import org.a2aproject.sdk.spec.Artifact;
import org.a2aproject.sdk.spec.Event;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskArtifactUpdateEvent;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;
import org.a2aproject.sdk.spec.TextPart;
import org.junit.jupiter.api.Test;

class MainEventBusTaskSnapshotTest {
    @Test
    void firstStatusOrArtifactUpdatePreservesOpeningMessage() throws Exception {
        InMemoryTaskStore store = new InMemoryTaskStore();
        MainEventBus bus = new MainEventBus();
        InMemoryQueueManager queues = new InMemoryQueueManager(store, bus);
        MainEventBusProcessor processor = new MainEventBusProcessor(bus, store, PushNotificationSender.NOOP, queues);
        EventQueueUtil.start(processor);
        try {
            Message opening = Message.builder().messageId("opening").role(Message.Role.ROLE_USER)
                    .parts(new TextPart("hello")).build();
            List<Event> events = List.of(
                    TaskStatusUpdateEvent.builder().taskId("status").contextId("context")
                            .status(new TaskStatus(TaskState.TASK_STATE_WORKING)).build(),
                    TaskArtifactUpdateEvent.builder().taskId("artifact").contextId("context")
                            .artifact(Artifact.builder().artifactId("output").parts(new TextPart("result")).build())
                            .build());
            List<String> ids = List.of("status", "artifact");
            for (int i = 0; i < ids.size(); i++) {
                String id = ids.get(i);
                processor.registerInitialMessage(id, opening);
                EventQueue queue = EventQueueUtil.getEventQueueBuilder(bus).taskId(id).build().tap();
                queue.enqueueEvent(events.get(i));
                queue.dequeueEventItem(5000);
                assertEquals(List.of(opening), store.get(id).history());
            }
        } finally {
            EventQueueUtil.stop(processor);
        }
    }

    @Test
    void persistedTaskIsDispatchedReplicatedAndNotified() throws Exception {
        String id = "task-snapshot";
        InMemoryTaskStore store = new InMemoryTaskStore();
        MainEventBus bus = new MainEventBus();
        InMemoryQueueManager queues = new InMemoryQueueManager(store, bus);
        CountDownLatch delivered = new CountDownLatch(1);
        AtomicReference<Task> notified = new AtomicReference<>();
        AtomicReference<Task> replicated = new AtomicReference<>();
        PushNotificationSender sender = (event, snapshot) -> {
            notified.set((Task) event);
            delivered.countDown();
            return true;
        };
        MainEventBusProcessor processor = new MainEventBusProcessor(bus, store, sender, queues);
        EventQueueUtil.start(processor);
        try {
            Message opening = Message.builder().messageId("opening").role(Message.Role.ROLE_USER)
                    .parts(new TextPart("hello")).build();
            processor.registerInitialMessage(id, opening);
            EventQueue queue = EventQueueUtil.getEventQueueBuilder(bus).taskId(id)
                    .hook(item -> replicated.set((Task) item.getEvent())).build().tap();
            queue.enqueueEvent(Task.builder().id(id).contextId("context")
                    .status(new TaskStatus(TaskState.TASK_STATE_WORKING)).build());
            Event event = queue.dequeueEventItem(5000).getEvent();
            assertTrue(delivered.await(5, TimeUnit.SECONDS));
            Task stored = store.get(id);
            assertEquals(List.of(opening), stored.history());
            assertEquals(stored, event);
            assertEquals(stored, replicated.get());
            assertEquals(stored, notified.get());
        } finally {
            EventQueueUtil.stop(processor);
        }
    }
    @Test
    void finalTaskSnapshotStillTriggersFinalizationCallback() throws Exception {
        String id = "final-task-snapshot";
        InMemoryTaskStore store = new InMemoryTaskStore();
        MainEventBus bus = new MainEventBus();
        InMemoryQueueManager queues = new InMemoryQueueManager(store, bus);
        MainEventBusProcessor processor = new MainEventBusProcessor(bus, store, PushNotificationSender.NOOP, queues);
        CountDownLatch finalized = new CountDownLatch(1);
        processor.setCallback(new MainEventBusProcessorCallback() {
            public void onEventProcessed(String taskId, Event event) {
            }
            public void onTaskFinalized(String taskId) {
                finalized.countDown();
            }
        });
        EventQueueUtil.start(processor);
        try {
            Message opening = Message.builder().messageId("opening").role(Message.Role.ROLE_USER)
                    .parts(new TextPart("hello")).build();
            processor.registerInitialMessage(id, opening);
            EventQueue queue = EventQueueUtil.getEventQueueBuilder(bus).taskId(id).build().tap();
            queue.enqueueEvent(Task.builder().id(id).contextId("context")
                    .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED)).build());
            assertTrue(finalized.await(5, TimeUnit.SECONDS));
            assertEquals(List.of(opening), store.get(id).history());
        } finally {
            EventQueueUtil.stop(processor);
        }
    }
}
