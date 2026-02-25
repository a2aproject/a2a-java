package io.a2a.server.agentexecution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import io.a2a.server.ServerCallContext;
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
 * public void execute(RequestContext context, AgentEmitter emitter) {
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

    private final @Nullable MessageSendParams params;
    private final String taskId;
    private final String contextId;
    private final @Nullable Task task;
    private final List<Task> relatedTasks;
    private final @Nullable ServerCallContext callContext;

    /**
     * Constructor with all fields already validated and initialized.
     * <p>
     * <b>Note:</b> Use {@link Builder} instead of calling this constructor directly.
     * The builder handles ID generation and validation.
     * </p>
     *
     * @param params the message send parameters (can be null for cancel operations)
     * @param taskId the task identifier (must not be null)
     * @param contextId the context identifier (must not be null)
     * @param task the existing task state (null for new conversations)
     * @param relatedTasks other tasks in the same context (must not be null, can be empty)
     * @param callContext the server call context (can be null)
     */
    private RequestContext(
            @Nullable MessageSendParams params,
            String taskId,
            String contextId,
            @Nullable Task task,
            List<Task> relatedTasks,
            @Nullable ServerCallContext callContext) {
        this.params = params;
        this.taskId = taskId;
        this.contextId = contextId;
        this.task = task;
        this.relatedTasks = relatedTasks;
        this.callContext = callContext;
    }

    /**
     * Returns the task identifier.
     * <p>
     * This is auto-generated (UUID) by the builder if not provided by the client
     * in the message parameters. This value is never null.
     * </p>
     *
     * @return the task ID (never null)
     */
    public String getTaskId() {
        return taskId;
    }

    /**
     * Returns the conversation context identifier.
     * <p>
     * Conversation contexts group related tasks together (e.g., multiple tasks
     * in the same user session). This is auto-generated (UUID) by the builder if
     * not provided by the client in the message parameters. This value is never null.
     * </p>
     *
     * @return the context ID (never null)
     */
    public String getContextId() {
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
     * Returns the request metadata.
     *
     * @return the metadata, or null if not available
     */
    public @Nullable Map<String, Object> getMetadata() {
        return (params != null && params.metadata() != null) ? Collections.unmodifiableMap(params.metadata()) : null;
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
     * Returns the tenant identifier from the request parameters.
     * <p>
     * The tenant is used in multi-tenant environments to identify which
     * customer or organization the request belongs to.
     * </p>
     *
     * @return the tenant identifier, or null if no params or tenant not set
     */
    public @Nullable String getTenant() {
        return params != null ? params.tenant() : null;
    }

    /**
     * Extracts all text content from the message and joins with the newline delimiter.
     * <p>
     * This is a convenience method for getting text input from messages that may contain
     * multiple text parts. Non-text parts (images, etc.) are ignored.
     * <p>
     * <b>Examples:</b>
     * <pre>{@code
     * // Join with newlines (common for multi-paragraph input)
     * String text = context.getUserInput();
     * }</pre>
     *
     * @return all text parts joined with newline, or empty string if no message
     */
    public String getUserInput() {
        return getUserInput("\n");
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
     * String text = context.getUserInput();
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
    public String getUserInput(@Nullable String delimiter) {
        if (params == null) {
            return "";
        }
        return getMessageText(params.message(), delimiter == null ? "\n" : delimiter);
    }

    /**
     * Attaches a related task to this context.
     * <p>
     * This is primarily used by the framework to populate related tasks after
     * construction. Agent implementations should use {@link #getRelatedTasks()}
     * to access related tasks.
     * </p>
     *
     * @param task the task to attach
     */
    public void attachRelatedTask(Task task) {
        relatedTasks.add(task);
    }

    private String getMessageText(Message message, String delimiter) {
        List<String> textParts = getTextParts(message.parts());
        return String.join(delimiter, textParts);
    }

    private List<String> getTextParts(List<Part<?>> parts) {
        return parts.stream()
                .filter(part -> part instanceof TextPart)
                .map(part -> (TextPart) part)
                .map(TextPart::text)
                .collect(Collectors.toList());
    }

    /**
     * Builder for creating {@link RequestContext} instances.
     * <p>
     * The builder handles ID generation and validation automatically:
     * </p>
     * <ul>
     *   <li>TaskId and ContextId are auto-generated (UUID) if not provided</li>
     *   <li>IDs are validated against message parameters if both are present</li>
     *   <li>Message parameters are updated with generated IDs</li>
     *   <li>Related tasks list is initialized to empty list if null</li>
     * </ul>
     */
    public static class Builder {
        private @Nullable MessageSendParams params;
        private @Nullable String taskId;
        private @Nullable String contextId;
        private @Nullable Task task;
        private @Nullable List<Task> relatedTasks;
        private @Nullable ServerCallContext serverCallContext;

        public Builder setParams(@Nullable MessageSendParams params) {
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

        public Builder setRelatedTasks(@Nullable List<Task> relatedTasks) {
            this.relatedTasks = relatedTasks;
            return this;
        }

        public Builder setServerCallContext(@Nullable ServerCallContext serverCallContext) {
            this.serverCallContext = serverCallContext;
            return this;
        }

        public @Nullable MessageSendParams getParams() {
            return params;
        }

        public @Nullable String getTaskId() {
            return taskId;
        }

        public @Nullable String getContextId() {
            return contextId;
        }

        public @Nullable Task getTask() {
            return task;
        }

        public @Nullable List<Task> getRelatedTasks() {
            return relatedTasks;
        }

        public @Nullable ServerCallContext getServerCallContext() {
            return serverCallContext;
        }

        /**
         * Builds the RequestContext with ID generation and validation.
         *
         * @return the constructed RequestContext
         * @throws InvalidParamsError if taskId or contextId don't match message parameters
         */
        public RequestContext build() throws InvalidParamsError {
            // 1. Initialize relatedTasks to empty list if null
            List<Task> finalRelatedTasks = relatedTasks != null ? relatedTasks : new ArrayList<>();

            // 2. Extract message IDs upfront (or null if no params)
            String messageTaskId = params != null ? params.message().taskId() : null;
            String messageContextId = params != null ? params.message().contextId() : null;

            // 3. Validate: if both builder and message provide an ID, they must match
            if (taskId != null && messageTaskId != null && !taskId.equals(messageTaskId)) {
                throw new InvalidParamsError("bad task id");
            }
            if (contextId != null && messageContextId != null && !contextId.equals(messageContextId)) {
                throw new InvalidParamsError("bad context id");
            }

            // 4. Determine final IDs using coalesce pattern: builder → message → generate
            String finalTaskId = taskId != null ? taskId :
                                  messageTaskId != null ? messageTaskId :
                                  UUID.randomUUID().toString();

            String finalContextId = contextId != null ? contextId :
                                    messageContextId != null ? messageContextId :
                                    UUID.randomUUID().toString();

            // 5. Update params if message needs to be updated with final IDs
            MessageSendParams finalParams = params;
            if (params != null && (!finalTaskId.equals(messageTaskId) || !finalContextId.equals(messageContextId))) {
                Message updatedMessage = Message.builder(params.message())
                        .taskId(finalTaskId)
                        .contextId(finalContextId)
                        .build();
                // Preserve all original fields including tenant
                finalParams = MessageSendParams.builder()
                        .message(updatedMessage)
                        .configuration(params.configuration())
                        .metadata(params.metadata())
                        .tenant(params.tenant())
                        .build();
            }

            // 6. Call constructor with finalized values (IDs guaranteed non-null)
            return new RequestContext(finalParams, finalTaskId, finalContextId,
                    task, finalRelatedTasks, serverCallContext);
        }
    }

}
