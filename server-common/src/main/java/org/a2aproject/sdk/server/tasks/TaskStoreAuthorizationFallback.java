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

    private TaskStoreAuthorizationFallback() {}

    static ListTasksResult listAuthorized(
            TaskStore store,
            ListTasksParams params,
            ServerCallContext context,
            TaskAuthorizationProvider provider) {
        ScanBudget countBudget = new ScanBudget(store);
        Map<String, Boolean> readAccess = new HashMap<>();
        int totalSize = 0;
        String token = null;
        Set<String> scannedTokens = new HashSet<>();
        do {
            ListTasksResult batch =
                    countBudget.list(listPage(params, SCAN_BATCH_SIZE, token), context);
            for (Task task : batch.tasks()) {
                if (readAccess.computeIfAbsent(
                        task.id(), id -> isReadAllowed(provider, context, id))) {
                    totalSize++;
                }
            }
            token = advanceToken(token, batch.nextPageToken(), scannedTokens);
        } while (token != null && !token.isEmpty());

        if (totalSize == 0) {
            return new ListTasksResult(List.of(), 0, 0, null);
        }
        int requestedPageSize = params.getEffectivePageSize();
        ScanBudget pageBudget = new ScanBudget(store);
        List<Task> page = new ArrayList<>(requestedPageSize);
        token = params.pageToken();
        Set<String> pageTokens = tokensStartingAt(token);
        int readableSeen = 0;
        while (true) {
            String batchStart = token;
            ListTasksResult batch =
                    pageBudget.list(listPage(params, SCAN_BATCH_SIZE, token), context);
            token = advanceToken(token, batch.nextPageToken(), pageTokens);
            for (int index = 0; index < batch.tasks().size(); index++) {
                Task task = batch.tasks().get(index);
                if (readAccess.computeIfAbsent(
                        task.id(), id -> isReadAllowed(provider, context, id))) {
                    page.add(task);
                    readableSeen++;
                    if (page.size() == requestedPageSize) {
                        // On the first page the completed count can prove termination.
                        boolean terminal =
                                (params.pageToken() == null || params.pageToken().isEmpty())
                                        && readableSeen == totalSize;
                        boolean hasMore =
                                !terminal
                                        && hasReadableAfter(
                                                batch,
                                                index + 1,
                                                token,
                                                pageBudget,
                                                params,
                                                context,
                                                provider,
                                                readAccess);
                        String nextPageToken = null;
                        if (hasMore) {
                            nextPageToken =
                                    index + 1 == batch.tasks().size()
                                            ? token
                                            : recoverToken(
                                                    batch,
                                                    index + 1,
                                                    batchStart,
                                                    pageBudget,
                                                    params,
                                                    context);
                        }
                        return new ListTasksResult(page, totalSize, page.size(), nextPageToken);
                    }
                }
            }
            if (token == null || token.isEmpty()) {
                return new ListTasksResult(page, totalSize, page.size(), null);
            }
        }
    }

    private static boolean hasReadableAfter(
            ListTasksResult finalBatch,
            int start,
            @Nullable String token,
            ScanBudget budget,
            ListTasksParams params,
            ServerCallContext context,
            TaskAuthorizationProvider provider,
            Map<String, Boolean> readAccess) {
        for (int index = start; index < finalBatch.tasks().size(); index++) {
            String id = finalBatch.tasks().get(index).id();
            if (readAccess.computeIfAbsent(id, key -> isReadAllowed(provider, context, key))) {
                return true;
            }
        }
        Set<String> seenTokens = tokensStartingAt(token);
        while (token != null && !token.isEmpty()) {
            ListTasksResult batch = budget.list(listPage(params, SCAN_BATCH_SIZE, token), context);
            token = advanceToken(token, batch.nextPageToken(), seenTokens);
            for (Task task : batch.tasks()) {
                if (readAccess.computeIfAbsent(
                        task.id(), id -> isReadAllowed(provider, context, id))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static @Nullable String recoverToken(
            ListTasksResult finalBatch,
            int prefixLength,
            @Nullable String token,
            ScanBudget budget,
            ListTasksParams params,
            ServerCallContext context) {
        Set<String> seenTokens = tokensStartingAt(token);
        int recovered = 0;
        while (recovered < prefixLength) {
            ListTasksResult batch =
                    budget.recover(listPage(params, prefixLength - recovered, token), context);
            if (batch.tasks().size() > prefixLength - recovered) {
                throw new TaskStoreException(
                        "Task store exceeded the authorized listing recovery prefix");
            }
            for (Task task : batch.tasks()) {
                if (!task.id().equals(finalBatch.tasks().get(recovered).id())) {
                    throw new TaskStoreException(
                            "Task store changed the authorized listing recovery prefix");
                }
                recovered++;
            }
            token = advanceToken(token, batch.nextPageToken(), seenTokens);
            if (token == null || token.isEmpty()) {
                throw new TaskStoreException(
                        "Task store ended before the authorized listing recovery boundary");
            }
        }
        return token;
    }

    private static Set<String> tokensStartingAt(@Nullable String token) {
        Set<String> tokens = new HashSet<>();
        if (token != null && !token.isEmpty()) {
            tokens.add(token);
        }
        return tokens;
    }

    private static @Nullable String advanceToken(
            @Nullable String currentToken, @Nullable String nextToken, Set<String> seenTokens) {
        if (nextToken == null || nextToken.isEmpty()) {
            return nextToken;
        }
        if (nextToken.equals(currentToken) || !seenTokens.add(nextToken)) {
            throw new TaskStoreException(
                    "Task store returned a repeated page token during authorized listing");
        }
        return nextToken;
    }

    private static boolean isReadAllowed(
            TaskAuthorizationProvider provider, ServerCallContext context, String taskId) {
        return TaskAuthorizationProvider.checkReadAccess(
                provider, context, taskId, TaskOperation.LIST_TASKS);
    }

    private static ListTasksParams listPage(
            ListTasksParams params, int pageSize, @Nullable String pageToken) {
        return new ListTasksParams(
                params.contextId(),
                params.status(),
                pageSize,
                pageToken,
                params.historyLength(),
                params.statusTimestampAfter(),
                params.includeArtifacts(),
                params.tenant());
    }

    private static final class ScanBudget {
        private final TaskStore store;
        private int batches;
        private int recoveryQueries;
        private final Set<String> returnedTasks = new HashSet<>();

        private ScanBudget(TaskStore store) {
            this.store = store;
        }

        private ListTasksResult list(ListTasksParams params, ServerCallContext context) {
            if (batches >= MAX_SCAN_BATCHES) {
                throw new TaskStoreException("Authorized task listing exceeded its scan limit");
            }
            batches++;
            return fetch(params, context);
        }

        private ListTasksResult recover(ListTasksParams params, ServerCallContext context) {
            if (recoveryQueries >= SCAN_BATCH_SIZE) {
                throw new TaskStoreException(
                        "Authorized task listing exceeded its recovery query limit");
            }
            recoveryQueries++;
            return fetch(params, context);
        }

        private ListTasksResult fetch(ListTasksParams params, ServerCallContext context) {
            ListTasksResult result = store.list(params, context);
            for (Task task : result.tasks()) {
                returnedTasks.add(task.id());
            }
            if (returnedTasks.size() > MAX_RETURNED_TASKS) {
                throw new TaskStoreException("Authorized task listing exceeded its scan limit");
            }
            return result;
        }
    }
}
