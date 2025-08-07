package io.a2a.server.agentexecution;

import java.util.ArrayList;
import java.util.List;

import io.a2a.server.tasks.TaskStore;
import io.a2a.spec.Task;

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
                && getParams().message().getReferenceTaskIds() != null) {
            relatedTasks = new ArrayList<>();
            for (String taskId : getParams().message().getReferenceTaskIds()) {
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
