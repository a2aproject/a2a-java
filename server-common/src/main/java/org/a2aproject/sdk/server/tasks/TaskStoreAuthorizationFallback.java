package org.a2aproject.sdk.server.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.TaskAuthorizationProvider;
import org.a2aproject.sdk.server.auth.TaskOperation;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.a2aproject.sdk.spec.Task;
import org.jspecify.annotations.Nullable;

/** Bounded authorization fallback for stores that do not override listAuthorized. */
final class TaskStoreAuthorizationFallback {

    private static final int SCAN_BATCH_SIZE = 100;
    private static final int MAX_SCAN_BATCHES = 300;
    private static final int MAX_RETURNED_TASKS = 20_000;

    private TaskStoreAuthorizationFallback() {
    }

    static ListTasksResult listAuthorized(TaskStore store, ListTasksParams params, ServerCallContext context,
            TaskAuthorizationProvider provider) {
        ScanBudget countBudget = new ScanBudget(store);
        Map<String, Boolean> readAccess = new HashMap<>();
        int totalSize = 0;
        String token = null;
        Set<String> scannedTokens = new HashSet<>();
        do {
            ListTasksResult batch = countBudget.list(listPage(params, SCAN_BATCH_SIZE, token), context);
            for (Task task : batch.tasks()) {
                if (readAccess.computeIfAbsent(task.id(), id -> isReadAllowed(provider, context, id))) {
                    totalSize++;
                }
            }
            token = advanceToken(token, batch.nextPageToken(), scannedTokens);
        } while (token != null && !token.isEmpty());

        int requestedPageSize = params.getEffectivePageSize();
        // These tasks were already counted. Give page retrieval its own bounded scan
        // so a complete count at the limit can still return a page.
        ScanBudget pageBudget = new ScanBudget(store);
        List<Task> page = new ArrayList<>(requestedPageSize);
        token = params.pageToken();
        Set<String> pageTokens = tokensStartingAt(token);
        while (page.size() < requestedPageSize) {
            // Store tokens may be opaque; do not fetch beyond the last task on this page.
            ListTasksResult batch = pageBudget.list(listPage(params, requestedPageSize - page.size(), token), context);
            for (Task task : batch.tasks()) {
                if (readAccess.computeIfAbsent(task.id(), id -> isReadAllowed(provider, context, id))) {
                    page.add(task);
                }
            }
            token = advanceToken(token, batch.nextPageToken(), pageTokens);
            if (token == null || token.isEmpty()) {
                break;
            }
        }

        String nextPageToken = null;
        if (page.size() == requestedPageSize) {
            String lookAheadToken = token;
            Set<String> lookAheadTokens = tokensStartingAt(lookAheadToken);
            while (lookAheadToken != null && !lookAheadToken.isEmpty()) {
                ListTasksResult batch = pageBudget.list(listPage(params, SCAN_BATCH_SIZE, lookAheadToken), context);
                if (batch.tasks().stream().anyMatch(task -> readAccess.computeIfAbsent(task.id(),
                        id -> isReadAllowed(provider, context, id)))) {
                    nextPageToken = token;
                    break;
                }
                lookAheadToken = advanceToken(lookAheadToken, batch.nextPageToken(), lookAheadTokens);
            }
        }
        return new ListTasksResult(page, totalSize, page.size(), nextPageToken);
    }

    private static Set<String> tokensStartingAt(@Nullable String token) {
        Set<String> tokens = new HashSet<>();
        if (token != null && !token.isEmpty()) {
            tokens.add(token);
        }
        return tokens;
    }

    private static @Nullable String advanceToken(@Nullable String currentToken, @Nullable String nextToken,
            Set<String> seenTokens) {
        if (nextToken == null || nextToken.isEmpty()) {
            return nextToken;
        }
        if (nextToken.equals(currentToken) || !seenTokens.add(nextToken)) {
            throw new TaskStoreException("Task store returned a repeated page token during authorized listing");
        }
        return nextToken;
    }

    private static boolean isReadAllowed(TaskAuthorizationProvider provider, ServerCallContext context, String taskId) {
        return TaskAuthorizationProvider.checkReadAccess(provider, context, taskId, TaskOperation.LIST_TASKS);
    }

    private static ListTasksParams listPage(ListTasksParams params, int pageSize, @Nullable String pageToken) {
        return new ListTasksParams(params.contextId(), params.status(), pageSize, pageToken,
                params.historyLength(), params.statusTimestampAfter(), params.includeArtifacts(), params.tenant());
    }

    private static final class ScanBudget {
        private final TaskStore store;
        private int batches;
        private int returnedTasks;

        private ScanBudget(TaskStore store) {
            this.store = store;
        }

        private ListTasksResult list(ListTasksParams params, ServerCallContext context) {
            if (batches >= MAX_SCAN_BATCHES || returnedTasks >= MAX_RETURNED_TASKS) {
                throw new TaskStoreException("Authorized task listing exceeded its scan limit");
            }
            batches++;
            ListTasksResult result = store.list(params, context);
            returnedTasks += result.tasks().size();
            if (returnedTasks > MAX_RETURNED_TASKS) {
                throw new TaskStoreException("Authorized task listing exceeded its scan limit");
            }
            return result;
        }
    }
}
