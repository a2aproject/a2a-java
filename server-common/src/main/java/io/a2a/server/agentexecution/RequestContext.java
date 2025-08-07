package io.a2a.server.agentexecution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

public class RequestContext {

    private MessageSendParams params;
    private String taskId;
    private String contextId;
    private Task task;
    private List<Task> relatedTasks;
    private final ServerCallContext callContext;

    public RequestContext(
            MessageSendParams params,
            String taskId,
            String contextId,
            Task task,
            List<Task> relatedTasks,
            ServerCallContext callContext) throws InvalidParamsError {
        this.params = params;
        this.taskId = taskId;
        this.contextId = contextId;
        this.task = task;
        this.relatedTasks = relatedTasks == null ? new ArrayList<>() : relatedTasks;
        this.callContext = callContext;

        // if the taskId and contextId were specified, they must match the params
        if (params != null) {
            if (taskId != null && !taskId.equals(params.message().getTaskId())) {
                throw new InvalidParamsError("bad task id");
            } else {
                checkOrGenerateTaskId();
            }
            if (contextId != null && !contextId.equals(params.message().getContextId())) {
                throw new InvalidParamsError("bad context id");
            } else {
                checkOrGenerateContextId();
            }
        }
    }

    public MessageSendParams getParams() {
        return params;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getContextId() {
        return contextId;
    }

    public Task getTask() {
        return task;
    }

    public List<Task> getRelatedTasks() {
        return Collections.unmodifiableList(relatedTasks);
    }

    public Message getMessage() {
        return params != null ? params.message() : null;
    }

    public MessageSendConfiguration getConfiguration() {
        return params != null ? params.configuration() : null;
    }

    public ServerCallContext getCallContext() {
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
        if (taskId == null && params.message().getTaskId() == null) {
            params.message().setTaskId(UUID.randomUUID().toString());
        }
        if (params.message().getTaskId() != null) {
            this.taskId = params.message().getTaskId();
        }
    }

    private void checkOrGenerateContextId() {
        if (params == null) {
            return;
        }
        if (contextId == null && params.message().getContextId() == null) {
            params.message().setContextId(UUID.randomUUID().toString());
        }
        if (params.message().getContextId() != null) {
            this.contextId = params.message().getContextId();
        }
    }

    private String getMessageText(Message message, String delimiter) {
        List<String> textParts = getTextParts(message.getParts());
        return String.join(delimiter, textParts);
    }

    private List<String> getTextParts(List<Part<?>> parts) {
        return parts.stream()
                .filter(part -> part.getKind() == Part.Kind.TEXT)
                .map(part -> (TextPart) part)
                .map(TextPart::getText)
                .collect(Collectors.toList());
    }

    public static class Builder {
        private MessageSendParams params;
        private String taskId;
        private String contextId;
        private Task task;
        private List<Task> relatedTasks;
        private ServerCallContext serverCallContext;

        public Builder setParams(MessageSendParams params) {
            this.params = params;
            return this;
        }

        public Builder setTaskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        public Builder setContextId(String contextId) {
            this.contextId = contextId;
            return this;
        }

        public Builder setTask(Task task) {
            this.task = task;
            return this;
        }

        public Builder setRelatedTasks(List<Task> relatedTasks) {
            this.relatedTasks = relatedTasks;
            return this;
        }

        public Builder setServerCallContext(ServerCallContext serverCallContext) {
            this.serverCallContext = serverCallContext;
            return this;
        }

        public MessageSendParams getParams() {
            return params;
        }

        public String getTaskId() {
            return taskId;
        }

        public String getContextId() {
            return contextId;
        }

        public Task getTask() {
            return task;
        }

        public List<Task> getRelatedTasks() {
            return relatedTasks;
        }

        public ServerCallContext getServerCallContext() {
            return serverCallContext;
        }

        public RequestContext build() {
            return new RequestContext(params, taskId, contextId, task, relatedTasks, serverCallContext);
        }
    }

}
