package io.a2a.server.tasks;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.spec.A2AError;
import io.a2a.spec.Artifact;
import io.a2a.spec.Event;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import org.jspecify.annotations.Nullable;

/**
 * Helper for emitting events from AgentExecutor implementations.
 *
 * <p>AgentEmitter provides a simplified API for agents to communicate with clients through
 * the A2A protocol. It handles both task lifecycle management and direct message sending,
 * automatically populating events with correct task and context IDs from the RequestContext.
 *
 * <h2>Core Capabilities</h2>
 * <ul>
 *   <li><b>Task Lifecycle:</b> {@link #submit()}, {@link #startWork()}, {@link #complete()},
 *       {@link #fail()}, {@link #cancel()}, {@link #reject()}</li>
 *   <li><b>Message Sending:</b> {@link #sendMessage(String)}, {@link #sendMessage(List)},
 *       {@link #sendMessage(List, Map)}</li>
 *   <li><b>Artifact Streaming:</b> {@link #addArtifact(List)}, {@link #addArtifact(List, String, String, Map)}</li>
 *   <li><b>Auth/Input Requirements:</b> {@link #requiresAuth()}, {@link #requiresInput()}</li>
 *   <li><b>Custom Events:</b> {@link #taskBuilder()}, {@link #messageBuilder()}, {@link #addTask(Task)}, {@link #emitEvent(Event)}</li>
 * </ul>
 *
 * <h2>Usage Patterns</h2>
 *
 * <h3>Simple Message Response (No Task)</h3>
 * <pre>{@code
 * public void execute(RequestContext context, AgentEmitter emitter) {
 *     String response = processRequest(context.getUserInput("\n"));
 *     emitter.sendMessage(response);
 * }
 * }</pre>
 *
 * <h3>Task Lifecycle with Artifacts</h3>
 * <pre>{@code
 * public void execute(RequestContext context, AgentEmitter emitter) {
 *     if (context.getTask() == null) {
 *         emitter.submit();  // Create task in SUBMITTED state
 *     }
 *     emitter.startWork();  // Transition to WORKING
 *
 *     // Process and stream results
 *     List<Part<?>> results = doWork(context.getUserInput("\n"));
 *     emitter.addArtifact(results);
 *
 *     emitter.complete();  // Mark as COMPLETED
 * }
 * }</pre>
 *
 * <h3>Streaming Response (LLM)</h3>
 * <pre>{@code
 * public void execute(RequestContext context, AgentEmitter emitter) {
 *     emitter.startWork();
 *
 *     for (String chunk : llmService.stream(context.getUserInput("\n"))) {
 *         emitter.addArtifact(List.of(new TextPart(chunk)));
 *     }
 *
 *     emitter.complete();
 * }
 * }</pre>
 *
 * <h2>Event ID Management</h2>
 * All emitted events are automatically populated with:
 * <ul>
 *   <li><b>taskId:</b> From RequestContext (may be null for message-only responses)</li>
 *   <li><b>contextId:</b> From RequestContext</li>
 *   <li><b>messageId:</b> Generated UUID for messages</li>
 *   <li><b>artifactId:</b> Generated UUID for artifacts (unless explicitly provided)</li>
 * </ul>
 *
 * Events are validated by the EventQueue to ensure taskId correctness.
 *
 * @see io.a2a.server.agentexecution.AgentExecutor
 * @see RequestContext
 * @see EventQueue
 * @since 1.0.0
 */
public class AgentEmitter {
    private final EventQueue eventQueue;
    private final String taskId;
    private final String contextId;
    private final AtomicBoolean terminalStateReached = new AtomicBoolean(false);

    /**
     * Creates a new AgentEmitter for the given request context and event queue.
     *
     * @param context the request context containing task and context IDs
     * @param eventQueue the event queue for enqueueing events
     */
    public AgentEmitter(RequestContext context, EventQueue eventQueue) {
        this.eventQueue = eventQueue;
        this.taskId = context.getTaskId();
        this.contextId = context.getContextId();
    }

    private void updateStatus(TaskState taskState) {
        updateStatus(taskState, null, taskState.isFinal());
    }

    /**
     * Updates the task status to the given state with an optional message.
     *
     * @param taskState the new task state
     * @param message optional message to include with the status update
     */
    public void updateStatus(TaskState taskState, @Nullable Message message) {
        updateStatus(taskState, message, taskState.isFinal());
    }

    /**
     * Updates the task status to the given state with an optional message and finality flag.
     *
     * @param state the new task state
     * @param message optional message to include with the status update
     * @param isFinal whether this is a final status (prevents further updates)
     */
    private void updateStatus(TaskState state, @Nullable Message message, boolean isFinal) {
        // Check terminal state first (fail fast)
        if (terminalStateReached.get()) {
            throw new IllegalStateException("Cannot update task status - terminal state already reached");
        }
        
        // For final states, atomically set the flag
        if (isFinal) {
            if (!terminalStateReached.compareAndSet(false, true)) {
                throw new IllegalStateException("Cannot update task status - terminal state already reached");
            }
        }

        TaskStatusUpdateEvent event = TaskStatusUpdateEvent.builder()
                .taskId(taskId)
                .contextId(contextId)
                .status(new TaskStatus(state, message, null))
                .build();
        eventQueue.enqueueEvent(event);
    }

    /**
     * Returns the context ID for this emitter.
     *
     * @return the context ID, or null if not available
     */
    public @Nullable String getContextId() {
        return this.contextId;
    }

    /**
     * Returns the task ID for this emitter.
     *
     * @return the task ID, or null if no task is associated
     */
    public @Nullable String getTaskId() {
        return this.taskId;
    }

    /**
     * Adds an artifact with the given parts to the task.
     *
     * @param parts the parts to include in the artifact
     */
    public void addArtifact(List<Part<?>> parts) {
        addArtifact(parts, null, null, null);
    }

    /**
     * Adds an artifact with the given parts, artifact ID, name, and metadata.
     *
     * @param parts the parts to include in the artifact
     * @param artifactId optional artifact ID (generated if null)
     * @param name optional artifact name
     * @param metadata optional metadata map
     */
    public void addArtifact(List<Part<?>> parts, @Nullable String artifactId, @Nullable String name, @Nullable Map<String, Object> metadata) {
        addArtifact(parts, artifactId, name, metadata, null, null);
    }

    /**
     * Adds an artifact with all optional parameters.
     *
     * @param parts the parts to include in the artifact
     * @param artifactId optional artifact ID (generated if null)
     * @param name optional artifact name
     * @param metadata optional metadata map
     * @param append whether to append to an existing artifact
     * @param lastChunk whether this is the last chunk in a streaming sequence
     */
    public void addArtifact(List<Part<?>> parts, @Nullable String artifactId, @Nullable String name, @Nullable Map<String, Object> metadata,
                            @Nullable Boolean append, @Nullable Boolean lastChunk) {
        if (artifactId == null) {
            artifactId = UUID.randomUUID().toString();
        }
        TaskArtifactUpdateEvent event = TaskArtifactUpdateEvent.builder()
                .taskId(taskId)
                .contextId(contextId)
                .artifact(
                        Artifact.builder()
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

    /**
     * Marks the task as COMPLETED.
     */
    public void complete() {
        complete(null);
    }

    /**
     * Marks the task as COMPLETED with an optional message.
     *
     * @param message optional message to include with completion
     */
    public void complete(@Nullable Message message) {
        updateStatus(TaskState.TASK_STATE_COMPLETED, message);
    }

    /**
     * Marks the task as FAILED.
     */
    public void fail() {
        fail((Message) null);
    }

    /**
     * Marks the task as FAILED with an optional message.
     *
     * @param message optional message to include with failure
     */
    public void fail(@Nullable Message message) {
        updateStatus(TaskState.TASK_STATE_FAILED, message);
    }

    /**
     * Enqueues an A2A error event which will automatically transition the task to FAILED.
     * <p>
     * Use this when you need to fail the task with a specific A2A error (such as
     * {@link io.a2a.spec.UnsupportedOperationError}, {@link io.a2a.spec.InvalidRequestError},
     * {@link io.a2a.spec.TaskNotFoundError}, etc.) that should be sent to the client.
     * </p>
     * <p>
     * The error event is enqueued and the MainEventBusProcessor will automatically transition
     * the task to FAILED state. This ensures thread-safe state transitions without race conditions,
     * as the single-threaded MainEventBusProcessor handles all state updates.
     * </p>
     * <p>
     * Error events are terminal (stop event consumption) and trigger automatic FAILED state transition.
     * The error details are sent to the originating client only, while the FAILED status is replicated
     * to all nodes in multi-instance deployments.
     * </p>
     * <p>Example usage:
     * <pre>{@code
     * public void execute(RequestContext context, AgentEmitter emitter) {
     *     if (!isSupported(context.getMessage())) {
     *         emitter.fail(new UnsupportedOperationError("Feature not supported"));
     *         return;
     *     }
     *     // ... normal processing
     * }
     * }</pre>
     *
     * @param error the A2A error to enqueue and send to the client
     * @since 1.0.0
     */
    public void fail(A2AError error) {
        // Set terminal state flag BEFORE enqueueing error
        // This prevents race conditions where agent calls fail(error) then complete()
        if (!terminalStateReached.compareAndSet(false, true)) {
            throw new IllegalStateException("Cannot update task status - terminal state already reached");
        }
        
        eventQueue.enqueueEvent(error);
        // Status transition happens automatically in MainEventBusProcessor
        // The error event is terminal and will trigger FAILED state transition
    }

    /**
     * Marks the task as SUBMITTED.
     */
    public void submit() {
        submit(null);
    }

    /**
     * Marks the task as SUBMITTED with an optional message.
     *
     * @param message optional message to include
     */
    public void submit(@Nullable Message message) {
        updateStatus(TaskState.TASK_STATE_SUBMITTED, message);
    }

    /**
     * Marks the task as WORKING (actively being processed).
     */
    public void startWork() {
        startWork(null);
    }

    /**
     * Marks the task as WORKING with an optional message.
     *
     * @param message optional message to include
     */
    public void startWork(@Nullable Message message) {
        updateStatus(TaskState.TASK_STATE_WORKING, message);
    }

    /**
     * Marks the task as CANCELED.
     */
    public void cancel() {
        cancel(null);
    }

    /**
     * Marks the task as CANCELED with an optional message.
     *
     * @param message optional message to include
     */
    public void cancel(@Nullable Message message) {
        updateStatus(TaskState.TASK_STATE_CANCELED, message);
    }

    /**
     * Marks the task as REJECTED.
     */
    public void reject() {
        reject(null);
    }

    /**
     * Marks the task as REJECTED with an optional message.
     *
     * @param message optional message to include
     */
    public void reject(@Nullable Message message) {
        updateStatus(TaskState.TASK_STATE_REJECTED, message);
    }

    /**
     * Marks the task as INPUT_REQUIRED, indicating the agent needs user input to continue.
     */
    public void requiresInput() {
        requiresInput(null, false);
    }

    /**
     * Marks the task as INPUT_REQUIRED with an optional message.
     *
     * @param message optional message to include
     */
    public void requiresInput(@Nullable Message message) {
        requiresInput(message, false);
    }

    /**
     * Marks the task as INPUT_REQUIRED with a finality flag.
     *
     * @param isFinal whether this is a final status (prevents further updates)
     */
    public void requiresInput(boolean isFinal) {
        requiresInput(null, isFinal);
    }

    /**
     * Marks the task as INPUT_REQUIRED with an optional message and finality flag.
     *
     * @param message optional message to include
     * @param isFinal whether this is a final status (prevents further updates)
     */
    public void requiresInput(@Nullable Message message, boolean isFinal) {
        updateStatus(TaskState.TASK_STATE_INPUT_REQUIRED, message, isFinal);
    }

    /**
     * Marks the task as AUTH_REQUIRED, indicating the agent needs authentication to continue.
     */
    public void requiresAuth() {
        requiresAuth(null, false);
    }

    /**
     * Marks the task as AUTH_REQUIRED with an optional message.
     *
     * @param message optional message to include
     */
    public void requiresAuth(@Nullable Message message) {
        requiresAuth(message, false);
    }

    /**
     * Marks the task as AUTH_REQUIRED with a finality flag.
     *
     * @param isFinal whether this is a final status (prevents further updates)
     */
    public void requiresAuth(boolean isFinal) {
        requiresAuth(null, isFinal);
    }

    /**
     * Marks the task as AUTH_REQUIRED with an optional message and finality flag.
     *
     * @param message optional message to include
     * @param isFinal whether this is a final status (prevents further updates)
     */
    public void requiresAuth(@Nullable Message message, boolean isFinal) {
        updateStatus(TaskState.TASK_STATE_AUTH_REQUIRED, message, isFinal);
    }

    /**
     * Creates a new agent message with the given parts and metadata.
     * Pre-populates the message with agent role, task ID, context ID, and a generated message ID.
     *
     * @param parts the parts to include in the message
     * @param metadata optional metadata to attach to the message
     * @return a new Message object ready to be sent
     */
    public Message newAgentMessage(List<Part<?>> parts, @Nullable Map<String, Object> metadata) {
        return Message.builder()
                .role(Message.Role.ROLE_AGENT)
                .taskId(taskId)
                .contextId(contextId)
                .messageId(UUID.randomUUID().toString())
                .metadata(metadata)
                .parts(parts)
                .build();
    }

    /**
     * Sends a simple text message to the client.
     * Convenience method for agents that respond with plain text without creating a task.
     *
     * @param text the text content to send
     */
    public void sendMessage(String text) {
        sendMessage(List.of(new TextPart(text)));
    }

    /**
     * Sends a message with custom parts (text, images, etc.) to the client.
     * Use this for rich responses that don't require task lifecycle management.
     *
     * @param parts the message parts to send
     */
    public void sendMessage(List<Part<?>> parts) {
        sendMessage(parts, null);
    }

    /**
     * Sends a message with parts and metadata to the client.
     * Creates an agent message with the current task and context IDs (if available)
     * and enqueues it to the event queue.
     *
     * @param parts the message parts to send
     * @param metadata optional metadata to attach to the message
     */
    public void sendMessage(List<Part<?>> parts, @Nullable Map<String, Object> metadata) {
        Message message = newAgentMessage(parts, metadata);
        eventQueue.enqueueEvent(message);
    }

    /**
     * Sends an existing Message object directly to the client.
     * <p>
     * Use this when you need to forward or echo an existing message without creating a new one.
     * The message is enqueued as-is, preserving its messageId, metadata, and all other fields.
     * </p>
     * <p>
     * <b>Note:</b> This is typically used for forwarding user messages or preserving specific
     * message properties. For most cases, prefer {@link #sendMessage(String)} or
     * {@link #sendMessage(List)} which create new agent messages with generated IDs.
     * </p>
     * <p>Example usage:
     * <pre>{@code
     * public void execute(RequestContext context, AgentEmitter emitter) {
     *     // Echo the user's message back
     *     emitter.sendMessage(context.getMessage());
     * }
     * }</pre>
     *
     * @param message the message to send to the client
     * @since 1.0.0
     */
    public void sendMessage(Message message) {
        eventQueue.enqueueEvent(message);
    }

    /**
     * Adds a custom Task object to be sent to the client.
     * <p>
     * Use this when you need to create a Task with specific fields (history, artifacts, etc.)
     * that the convenience methods like {@link #submit()}, {@link #startWork()}, or
     * {@link #complete()} don't provide.
     * </p>
     * <p>
     * <b>Typical usage pattern:</b> Build a task with {@link #taskBuilder()}, customize it,
     * then add it with this method.
     * </p>
     * <p>Example usage:
     * <pre>{@code
     * public void execute(RequestContext context, AgentEmitter emitter) {
     *     // Create a task with specific status and history
     *     Task task = emitter.taskBuilder()
     *         .status(new TaskStatus(TaskState.SUBMITTED))
     *         .history(List.of(context.getMessage()))
     *         .build();
     *     emitter.addTask(task);
     * }
     * }</pre>
     *
     * @param task the task to add
     * @since 1.0.0
     */
    public void addTask(Task task) {
        eventQueue.enqueueEvent(task);
    }

    /**
     * Emits a custom Event object to the client.
     * <p>
     * This is a general-purpose method for emitting any Event type. Most agents should use the
     * convenience methods ({@link #sendMessage(String)}, {@link #addTask(Task)},
     * {@link #addArtifact(List)}, {@link #complete()}, etc.), but this method provides flexibility
     * for agents that need to create and emit custom events using the event builders.
     * </p>
     * <p>Example usage:
     * <pre>{@code
     * public void execute(RequestContext context, AgentEmitter emitter) {
     *     // Create a custom TaskStatusUpdateEvent
     *     TaskStatusUpdateEvent event = TaskStatusUpdateEvent.builder()
     *         .taskId(context.getTaskId())
     *         .contextId(context.getContextId())
     *         .status(new TaskStatus(TaskState.WORKING))
     *         .isFinal(false)
     *         .build();
     *     emitter.emitEvent(event);
     * }
     * }</pre>
     *
     * @param event the event to emit
     * @since 1.0.0
     */
    public void emitEvent(Event event) {
        eventQueue.enqueueEvent(event);
    }

    /**
     * Creates a Task.Builder pre-populated with the correct task and context IDs.
     * Agents can customize other Task fields (status, artifacts, etc.) before calling build().
     *
     * <p>Example usage:
     * <pre>{@code
     * Task task = emitter.taskBuilder()
     *     .status(new TaskStatus(TaskState.WORKING))
     *     .build();
     * }</pre>
     *
     * @return a Task.Builder with id and contextId already set
     */
    public Task.Builder taskBuilder() {
        return Task.builder()
                .id(taskId)
                .contextId(contextId);
    }

    /**
     * Creates a Message.Builder pre-populated with agent defaults.
     * Sets taskId only if non-null (messages can exist independently of tasks).
     *
     * <p>Pre-populated fields:
     * <ul>
     *   <li>taskId - set only if this AgentEmitter has a non-null taskId</li>
     *   <li>contextId - current context ID</li>
     *   <li>role - Message.Role.AGENT</li>
     *   <li>messageId - generated UUID</li>
     * </ul>
     *
     * <p>Example usage:
     * <pre>{@code
     * Message msg = emitter.messageBuilder()
     *     .parts(List.of(new TextPart("Hello")))
     *     .metadata(Map.of("key", "value"))
     *     .build();
     * }</pre>
     *
     * @return a Message.Builder with common agent fields already set
     */
    public Message.Builder messageBuilder() {
        Message.Builder builder = Message.builder()
                .contextId(contextId)
                .role(Message.Role.ROLE_AGENT)
                .messageId(UUID.randomUUID().toString());

        // Only set taskId if present (messages can exist without tasks)
        if (taskId != null) {
            builder.taskId(taskId);
        }

        return builder;
    }

}
