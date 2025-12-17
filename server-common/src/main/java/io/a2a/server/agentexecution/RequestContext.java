package io.a2a.server.agentexecution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import io.a2a.server.ServerCallContext;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendConfiguration;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TextPart;

public class RequestContext {

    private @Nullable MessageSendParams params;
    private @Nullable String taskId;
    private @Nullable String contextId;
    private @Nullable Task task;
    private List<Task> relatedTasks;
    private final @Nullable ServerCallContext callContext;

    public RequestContext(
            @Nullable MessageSendParams params,
            @Nullable String taskId,
            @Nullable String contextId,
            @Nullable Task task,
            @Nullable List<Task> relatedTasks,
            @Nullable ServerCallContext callContext) throws InvalidParamsError {
        this.params = params;
        this.taskId = taskId;
        this.contextId = contextId;
        this.task = task;
        this.relatedTasks = relatedTasks == null ? new ArrayList<>() : relatedTasks;
        this.callContext = callContext;

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

    public List<Task> getRelatedTasks() {
        return Collections.unmodifiableList(relatedTasks);
    }

    public @Nullable Message getMessage() {
        return params != null ? params.message() : null;
    }

    public @Nullable MessageSendConfiguration getConfiguration() {
        return params != null ? params.configuration() : null;
    }

    public @Nullable ServerCallContext getCallContext() {
        return callContext;
    }

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
            String generatedTaskId = UUID.randomUUID().toString();
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
            String generatedContextId = UUID.randomUUID().toString();
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
                .filter(part -> part.getKind() == Part.Kind.TEXT)
                .map(part -> (TextPart) part)
                .map(TextPart::text)
                .collect(Collectors.toList());
    }

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

        public RequestContext build() {
            return new RequestContext(params, taskId, contextId, task, relatedTasks, serverCallContext);
        }
    }

}
