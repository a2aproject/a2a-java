package org.a2aproject.sdk.server.apps.common;

import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Unremovable;
import io.quarkus.arc.properties.IfBuildProperty;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.TaskAuthorizationProvider;
import org.a2aproject.sdk.server.auth.TaskOperation;

@ApplicationScoped
@Unremovable
@IfBuildProperty(name = "test.task-authorization.enabled", stringValue = "true", enableIfMissing = false)
public class TestTaskAuthorizationProvider implements TaskAuthorizationProvider {

    private final ConcurrentHashMap<String, String> taskOwners = new ConcurrentHashMap<>();

    @Override
    public boolean checkRead(ServerCallContext context, String taskId, TaskOperation operation) {
        String owner = taskOwners.get(taskId);
        // Intentionally fail-open for testing; production implementations should fail-closed (deny unknown tasks)
        return owner == null || owner.equals(context.getUser().getUsername());
    }

    @Override
    public boolean checkWrite(ServerCallContext context, String taskId, TaskOperation operation) {
        String owner = taskOwners.get(taskId);
        // Intentionally fail-open for testing; production implementations should fail-closed (deny unknown tasks)
        return owner == null || owner.equals(context.getUser().getUsername());
    }

    @Override
    public boolean checkCreate(ServerCallContext context, TaskOperation operation) {
        return context.getUser().isAuthenticated();
    }

    @Override
    public boolean isTaskRecorded(String taskId) {
        return taskOwners.containsKey(taskId);
    }

    @Override
    public void recordOwnership(ServerCallContext context, String taskId, TaskOperation operation) {
        taskOwners.putIfAbsent(taskId, context.getUser().getUsername());
    }
}
