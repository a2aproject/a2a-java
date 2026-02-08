package io.a2a.server.agentexecution;

import java.util.ArrayList;
import java.util.List;

import io.a2a.server.tasks.TaskStore;
import io.a2a.server.util.IdGenerator;
import io.a2a.spec.Task;
import jakarta.inject.Inject;

public class SimpleRequestContextBuilder extends RequestContext.Builder {
    private final TaskStore taskStore;
    private final boolean shouldPopulateReferredTasks;

    public SimpleRequestContextBuilder(TaskStore taskStore, boolean shouldPopulateReferredTasks, IdGenerator idGenerator) {
        this.taskStore = taskStore;
        this.shouldPopulateReferredTasks = shouldPopulateReferredTasks;
        setIdGenerator(idGenerator);
    }

    @Override
    public RequestContext build() {
        List<Task> relatedTasks = new ArrayList<>();
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
