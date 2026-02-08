package io.a2a.server.agentexecution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.a2a.server.ServerCallContext;
import io.a2a.server.util.IdGenerator;
import io.a2a.server.util.UUIDIdGenerator;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendConfiguration;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TextPart;
import org.jspecify.annotations.Nullable;

/**
 * Container for request parameters and task state provided to {@link AgentExecutor}.
 * <p>
 * This class encapsulates all the information an agent needs to process a request:
 * the user's message, existing task state (for continuing conversations), configuration,
 * and server call context. It's the primary way agents access request data.
 * </p>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li><b>Message:</b> The user's input message with parts (text, images, etc.)</li>
 *   <li><b>Task:</b> Existing task state for continuing conversations (null for new conversations)</li>
 *   <li><b>TaskId/ContextId:</b> Identifiers for the task and conversation (auto-generated if not provided)</li>
 *   <li><b>Configuration:</b> Request settings (blocking mode, push notifications, etc.)</li>
 *   <li><b>Related Tasks:</b> Other tasks in the same conversation context</li>
 * </ul>
 *
 * <h2>Common Usage Patterns</h2>
 * <pre>{@code
 * public void execute(RequestContext context, EventQueue queue) {
 *     // Check if this is a new conversation or continuation
 *     Task existingTask = context.getTask();
 *     if (existingTask == null) {
 *         // New conversation - initialize
 *     } else {
 *         // Continuing conversation - access history
 *         List<Message> history = existingTask.history();
 *     }
 *
 *     // Extract user input
 *     String userMessage = context.getUserInput("\n");
 *
 *     // Access configuration if needed
 *     MessageSendConfiguration config = context.getConfiguration();
 *     boolean isBlocking = config != null && config.blocking();
 *
 *     // Process and respond...
 * }
 * }</pre>
 *
 * <h2>Text Extraction Helper</h2>
 * The {@link #getUserInput(String)} method is a convenient way to extract text from
 * message parts:
 * <pre>{@code
 * // Get all text parts joined with newlines
 * String text = context.getUserInput("\n");
 *
 * // Get all text parts joined with spaces
 * String text = context.getUserInput(" ");
 * }</pre>
 *
 * @see AgentExecutor
 * @see Message
 * @see Task
 * @see MessageSendConfiguration
 */
public class RequestContext {

    private @Nullable MessageSendParams params;
    private @Nullable String taskId;
    private @Nullable String contextId;
    private @Nullable Task task;
    private List<Task> relatedTasks = new ArrayList<>();
    private final @Nullable ServerCallContext callContext;
    private final IdGenerator idGenerator;

    public RequestContext(
            @Nullable MessageSendParams params,
            @Nullable String taskId,
            @Nullable String contextId,
            @Nullable Task task,
            @Nullable List<Task> relatedTasks,
            @Nullable ServerCallContext callContext) throws InvalidParamsError {
        this(params, taskId, contextId, task, relatedTasks, callContext, new UUIDIdGenerator());
    }

    public RequestContext(
            @Nullable MessageSendParams params,
            @Nullable String taskId,
            @Nullable String contextId,
            @Nullable Task task,
            @Nullable List<Task> relatedTasks,
            @Nullable ServerCallContext callContext,
            IdGenerator idGenerator) throws InvalidParamsError {
        this.params = params;
        this.taskId = taskId;
        this.contextId = contextId;
        this.task = task;
        this.relatedTasks = relatedTasks == null ? new ArrayList<>() : relatedTasks;
        this.callContext = callContext;
        this.idGenerator = idGenerator;

        // If the taskId and contextId were specified, they must match the params
        if (params != null) {
            if (taskId != null && !taskId.equals(params.message().taskId())) {
                throw new InvalidParamsError("bad task id");
            } else {
                checkOrGenerateTaskId();
            }
            if (contextId != null && !contextId.equals(params.message().contextId())) {
                throw new InvalidParamsError("bad context id");
            } else {
                checkOrGenerateContextId();
            }
        }
    }

    /**
     * Returns the task identifier.
     * <p>
     * This is auto-generated (UUID) if not provided by the client in the message parameters.
     * It can be null if the context was not created from message parameters.
     * </p>
     *
     * @return the task ID
     */
    public @Nullable String getTaskId() {
        return taskId;
    }

    /**
     * Returns the conversation context identifier.
     * <p>
     * Conversation contexts group related tasks together (e.g., multiple tasks
     * in the same user session). This is auto-generated (UUID) if not provided by the client
     * in the message parameters. It can be null if the context was not created from message parameters.
     * </p>
     *
     * @return the context ID
     */
    public @Nullable String getContextId() {
        return contextId;
    }

    /**
     * Returns the existing task state, if this is a continuation of a conversation.
     * <p>
     * For new conversations, this is null. For continuing conversations, contains
     * the full task state including history, artifacts, and status.
     * <p>
     * <b>Common Pattern:</b>
     * <pre>{@code
     * if (context.getTask() == null) {
     *     // New conversation - initialize state
     * } else {
     *     // Continuing - access previous messages
     *     List<Message> history = context.getTask().history();
     * }
     * }</pre>
     *
     * @return the existing task, or null if this is a new conversation
     */
    public @Nullable Task getTask() {
        return task;
    }

    /**
     * Returns other tasks in the same conversation context.
     * <p>
     * Useful for multi-task conversations where the agent needs to access
     * state from related tasks.
     * </p>
     *
     * @return unmodifiable list of related tasks (empty if none)
     */
    public List<Task> getRelatedTasks() {
        return Collections.unmodifiableList(relatedTasks);
    }

    /**
     * Returns the user's message.
     * <p>
     * Contains the message parts (text, images, etc.) sent by the client.
     * Use {@link #getUserInput(String)} for convenient text extraction.
     * </p>
     *
     * @return the message, or null if not available
     * @see #getUserInput(String)
     */
    public @Nullable Message getMessage() {
        return params != null ? params.message() : null;
    }

    /**
     * Returns the request configuration.
     * <p>
     * Contains settings like blocking mode, push notification config, etc.
     * </p>
     *
     * @return the configuration, or null if not provided
     */
    public @Nullable MessageSendConfiguration getConfiguration() {
        return params != null ? params.configuration() : null;
    }

    /**
     * Returns the server call context.
     * <p>
     * Contains transport-specific information like authentication, headers, etc.
     * Most agents don't need this.
     * </p>
     *
     * @return the call context, or null if not available
     */
    public @Nullable ServerCallContext getCallContext() {
        return callContext;
    }

    /**
     * Extracts all text content from the message and joins with the specified delimiter.
     * <p>
     * This is a convenience method for getting text input from messages that may contain
     * multiple text parts. Non-text parts (images, etc.) are ignored.
     * <p>
     * <b>Examples:</b>
     * <pre>{@code
     * // Join with newlines (common for multi-paragraph input)
     * String text = context.getUserInput("\n");
     *
     * // Join with spaces (common for single-line input)
     * String text = context.getUserInput(" ");
     *
     * // Default delimiter is newline
     * String text = context.getUserInput(null);  // uses "\n"
     * }</pre>
     *
     * @param delimiter the string to insert between text parts (null defaults to "\n")
     * @return all text parts joined with delimiter, or empty string if no message
     */
    public String getUserInput(String delimiter) {
        if (params == null) {
            return "";
        }
        if (delimiter == null) {
            delimiter = "\n";
        }
        return getMessageText(params.message(), delimiter);
    }

    public void attachRelatedTask(Task task) {
        relatedTasks.add(task);
    }

    private void checkOrGenerateTaskId() {
        if (params == null) {
            return;
        }
        if (taskId == null && params.message().taskId() == null) {
            // Message is immutable, create new one with generated taskId
            String generatedTaskId = idGenerator.generateId();
            Message updatedMessage = Message.builder(params.message())
                    .taskId(generatedTaskId)
                    .build();
            params = new MessageSendParams(updatedMessage, params.configuration(), params.metadata());
            this.taskId = generatedTaskId;
        } else if (params.message().taskId() != null) {
            this.taskId = params.message().taskId();
        }
    }

    private void checkOrGenerateContextId() {
        if (params == null) {
            return;
        }
        if (contextId == null && params.message().contextId() == null) {
            // Message is immutable, create new one with generated contextId
            String generatedContextId = idGenerator.generateId();
            Message updatedMessage = Message.builder(params.message())
                    .contextId(generatedContextId)
                    .build();
            params = new MessageSendParams(updatedMessage, params.configuration(), params.metadata());
            this.contextId = generatedContextId;
        } else if (params.message().contextId() != null) {
            this.contextId = params.message().contextId();
        }
    }

    private String getMessageText(Message message, String delimiter) {
        List<String> textParts = getTextParts(message.parts());
        return String.join(delimiter, textParts);
    }

    private List<String> getTextParts(List<Part<?>> parts) {
        return parts.stream()
                .filter(p -> p instanceof TextPart)
                .map(p -> ((TextPart) p).text())
                .collect(Collectors.toList());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private @Nullable MessageSendParams params;
        private @Nullable String taskId;
        private @Nullable String contextId;
        private @Nullable Task task;
        private List<Task> relatedTasks = new ArrayList<>();
        private @Nullable ServerCallContext callContext;
        private IdGenerator idGenerator = new UUIDIdGenerator();

        public @Nullable MessageSendParams getParams() {
            return params;
        }

        public Builder setParams(MessageSendParams params) {
            this.params = params;
            return this;
        }

        public Builder setTaskId(@Nullable String taskId) {
            this.taskId = taskId;
            return this;
        }

        public Builder setContextId(@Nullable String contextId) {
            this.contextId = contextId;
            return this;
        }

        public Builder setTask(@Nullable Task task) {
            this.task = task;
            return this;
        }

        public Builder setRelatedTasks(List<Task> relatedTasks) {
            this.relatedTasks = relatedTasks;
            return this;
        }

        public Builder setCallContext(ServerCallContext callContext) {
            this.callContext = callContext;
            return this;
        }

        public Builder setIdGenerator(IdGenerator idGenerator) {
            this.idGenerator = idGenerator;
            return this;
        }

        public RequestContext build() throws InvalidParamsError {
            return new RequestContext(params, taskId, contextId, task, relatedTasks, callContext, idGenerator);
        }
    }
}
