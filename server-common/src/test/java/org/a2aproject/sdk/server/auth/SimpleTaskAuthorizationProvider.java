package org.a2aproject.sdk.server.auth;

import java.util.concurrent.ConcurrentHashMap;

import org.a2aproject.sdk.server.ServerCallContext;

/**
 * Owner-based {@link TaskAuthorizationProvider} for tests.
 */
public class SimpleTaskAuthorizationProvider implements TaskAuthorizationProvider {
    private final ConcurrentHashMap<String, String> owners = new ConcurrentHashMap<>();

    @Override
    public boolean checkRead(ServerCallContext context, String taskId, TaskOperation operation) {
        String owner = owners.get(taskId);
        return owner == null || owner.equals(context.getUser().getUsername());
    }

    @Override
    public boolean checkWrite(ServerCallContext context, String taskId, TaskOperation operation) {
        return checkRead(context, taskId, operation);
    }

    @Override
    public boolean checkCreate(ServerCallContext context, TaskOperation operation) {
        return context.getUser().isAuthenticated();
    }

    // Only returns true after an explicit recordOwnership call — unrecorded tasks are allowed by checkRead/checkWrite.
    @Override
    public boolean isTaskRecorded(String taskId) {
        return owners.containsKey(taskId);
    }

    @Override
    public void recordOwnership(ServerCallContext context, String taskId, TaskOperation operation) {
        owners.putIfAbsent(taskId, context.getUser().getUsername());
    }
}
