package io.a2a.server.tasks;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.spec.Artifact;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;

public class TaskUpdater {
    private final EventQueue eventQueue;
    private final String taskId;
    private final String contextId;
    private final AtomicBoolean terminalStateReached = new AtomicBoolean(false);
    private final Object stateLock = new Object();

    public TaskUpdater(RequestContext context, EventQueue eventQueue) {
        this.eventQueue = eventQueue;
        this.taskId = context.getTaskId();
        this.contextId = context.getContextId();
    }

    private void updateStatus(TaskState taskState) {
        updateStatus(taskState, null, taskState.isFinal());
    }

    public void updateStatus(TaskState taskState, Message message) {
        updateStatus(taskState, message, taskState.isFinal());
    }

    public void updateStatus(TaskState state, Message message, boolean isFinal) {
        synchronized (stateLock) {
            // Check if we're already in a terminal state
            if (terminalStateReached.get()) {
                throw new IllegalStateException("Cannot update task status - terminal state already reached");
            }
            
            // If this is a final state, set the flag
            if (isFinal) {
                terminalStateReached.set(true);
            }
            
            TaskStatusUpdateEvent event = new TaskStatusUpdateEvent.Builder()
                    .taskId(taskId)
                    .contextId(contextId)
                    .isFinal(isFinal)
                    .status(new TaskStatus(state, message, null))
                    .build();
            eventQueue.enqueueEvent(event);
        }
    }

    public String getContextId() {
        return this.contextId;
    }

    public String getTaskId() {
        return this.taskId;
    }

    public void addArtifact(List<Part<?>> parts) {
        addArtifact(parts, null, null, null);
    }

    public void addArtifact(List<Part<?>> parts, String artifactId, String name, Map<String, Object> metadata) {
        addArtifact(parts, artifactId, name, metadata, null, null);
    }

    public void addArtifact(List<Part<?>> parts, String artifactId, String name, Map<String, Object> metadata, 
                            Boolean append, Boolean lastChunk) {
        if (artifactId == null) {
            artifactId = UUID.randomUUID().toString();
        }
        TaskArtifactUpdateEvent event = new TaskArtifactUpdateEvent.Builder()
                .taskId(taskId)
                .contextId(contextId)
                .artifact(
                        new Artifact.Builder()
                                .artifactId(artifactId)
                                .name(name)
                                .parts(parts)
                                .metadata(metadata)
                                .build()
                )
                .append(append)
                .lastChunk(lastChunk)
                .build();
        eventQueue.enqueueEvent(event);
    }

    public void complete() {
        complete(null);
    }

    public void complete(Message message) {
        updateStatus(TaskState.COMPLETED, message);
    }

    public void fail() {
        fail(null);
    }

    public void fail(Message message) {
        updateStatus(TaskState.FAILED, message);
    }

    public void submit() {
        submit(null);
    }

    public void submit(Message message) {
        updateStatus(TaskState.SUBMITTED, message);
    }

    public void startWork() {
        startWork(null);
    }

    public void startWork(Message message) {
        updateStatus(TaskState.WORKING, message);
    }

    public void cancel() {
        cancel(null);
    }

    public void cancel(Message message) {
        updateStatus(TaskState.CANCELED, message);
    }

    public void reject() {
        reject(null);
    }

    public void reject(Message message) {
        updateStatus(TaskState.REJECTED, message);
    }

    public void requiresInput() {
        requiresInput(null, false);
    }

    public void requiresInput(Message message) {
        requiresInput(message, false);
    }

    public void requiresInput(boolean isFinal) {
        requiresInput(null, isFinal);
    }

    public void requiresInput(Message message, boolean isFinal) {
        updateStatus(TaskState.INPUT_REQUIRED, message, isFinal);
    }

    public void requiresAuth() {
        requiresAuth(null, false);
    }

    public void requiresAuth(Message message) {
        requiresAuth(message, false);
    }

    public void requiresAuth(boolean isFinal) {
        requiresAuth(null, isFinal);
    }

    public void requiresAuth(Message message, boolean isFinal) {
        updateStatus(TaskState.AUTH_REQUIRED, message, isFinal);
    }

    public Message newAgentMessage(List<Part<?>> parts, Map<String, Object> metadata) {
        return Message.builder()
                .role(Message.Role.AGENT)
                .taskId(taskId)
                .contextId(contextId)
                .messageId(UUID.randomUUID().toString())
                .metadata(metadata)
                .parts(parts)
                .build();
    }

}
