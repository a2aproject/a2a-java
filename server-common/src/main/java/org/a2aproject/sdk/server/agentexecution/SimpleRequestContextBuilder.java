package org.a2aproject.sdk.server.agentexecution;

import java.util.ArrayList;
import java.util.List;

import org.a2aproject.sdk.server.tasks.TaskStore;
import org.a2aproject.sdk.spec.Task;

public class SimpleRequestContextBuilder extends RequestContext.Builder {
    private final TaskStore taskStore;
    private final boolean shouldPopulateReferredTasks;

    public SimpleRequestContextBuilder(TaskStore taskStore, boolean shouldPopulateReferredTasks) {
        this.taskStore = taskStore;
        this.shouldPopulateReferredTasks = shouldPopulateReferredTasks;
    }

    @Override
    public RequestContext build() {
        List<Task> relatedTasks = null;
        if (taskStore != null && shouldPopulateReferredTasks && getParams() != null
                && getParams().message().referenceTaskIds() != null) {
            relatedTasks = new ArrayList<>();
            for (String taskId : getParams().message().referenceTaskIds()) {
                Task task = taskStore.get(taskId);
                if (task != null) {
                    relatedTasks.add(task);
                }
            }
        }

        super.setRelatedTasks(relatedTasks);
        return super.build();
    }
}
