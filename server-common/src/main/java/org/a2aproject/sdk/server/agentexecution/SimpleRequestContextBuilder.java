package org.a2aproject.sdk.server.agentexecution;

import java.util.ArrayList;
import java.util.List;

import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.TaskAuthorizationProvider;
import org.a2aproject.sdk.server.auth.TaskOperation;
import org.a2aproject.sdk.server.tasks.TaskStore;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskNotFoundError;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleRequestContextBuilder extends RequestContext.Builder {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleRequestContextBuilder.class);

    private final TaskStore taskStore;
    private final boolean shouldPopulateReferredTasks;
    private final @Nullable TaskAuthorizationProvider authorizationProvider;

    public SimpleRequestContextBuilder(TaskStore taskStore, boolean shouldPopulateReferredTasks,
            @Nullable TaskAuthorizationProvider authorizationProvider) {
        this.taskStore = taskStore;
        this.shouldPopulateReferredTasks = shouldPopulateReferredTasks;
        this.authorizationProvider = authorizationProvider;
    }

    @Override
    public RequestContext build() {
        List<Task> relatedTasks = null;
        if (taskStore != null && shouldPopulateReferredTasks && getParams() != null
                && getParams().message().referenceTaskIds() != null) {
            relatedTasks = new ArrayList<>();
            ServerCallContext callContext = getServerCallContext();
            for (String taskId : getParams().message().referenceTaskIds()) {
                if (authorizationProvider != null) {
                    if (callContext == null
                            || !authorizationProvider.checkRead(callContext, taskId, TaskOperation.MESSAGE_SEND)) {
                        throw new TaskNotFoundError();
                    }
                }
                Task task = taskStore.get(taskId);
                if (task != null) {
                    relatedTasks.add(task);
                } else {
                    LOGGER.warn("Referenced task '{}' not found in TaskStore", taskId);
                }
            }
        }

        super.setRelatedTasks(relatedTasks);
        return super.build();
    }
}
