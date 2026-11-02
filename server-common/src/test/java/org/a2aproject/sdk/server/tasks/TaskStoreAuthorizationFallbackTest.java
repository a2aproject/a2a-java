package org.a2aproject.sdk.server.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.AuthenticatedUser;
import org.a2aproject.sdk.server.auth.SimpleTaskAuthorizationProvider;
import org.a2aproject.sdk.server.auth.TaskAuthorizationProvider;
import org.a2aproject.sdk.server.auth.TaskOperation;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class TaskStoreAuthorizationFallbackTest {

    private final ServerCallContext context =
            new ServerCallContext(new AuthenticatedUser("alice"), Map.of(), Set.of());

    @Test
    void sparseAuthorizationReturnsTaskAfterThreeHundredDeniedCandidates() {
        TaskStore store = new OpaqueTokenTaskStore(301);
        TaskAuthorizationProvider provider = allowing("task-300"::equals);

        ListTasksResult result =
                store.listAuthorized(new ListTasksParams(1, null), context, provider);

        assertEquals(List.of("task-300"), result.tasks().stream().map(Task::id).toList());
        assertEquals(1, result.totalSize());
        assertNull(result.nextPageToken());
    }

    @Test
    void entirelyDeniedCandidatesReturnEmptyPageWithoutPageScan() {
        OpaqueTokenTaskStore store = new OpaqueTokenTaskStore(301);

        ListTasksResult result =
                store.listAuthorized(new ListTasksParams(1, null), context, allowing(id -> false));

        assertEquals(List.of(), result.tasks());
        assertEquals(0, result.totalSize());
        assertNull(result.nextPageToken());
        assertEquals(4, store.queries);
    }

    @Test
    void sparsePagesPreserveOpaqueBoundaryBeforeLaterReadableTask() {
        TaskStore store = new OpaqueTokenTaskStore(702);
        TaskAuthorizationProvider provider = allowing(Set.of("task-300", "task-701")::contains);

        ListTasksResult first =
                store.listAuthorized(new ListTasksParams(1, null), context, provider);
        ListTasksResult second =
                store.listAuthorized(
                        new ListTasksParams(1, first.nextPageToken()), context, provider);

        assertEquals(List.of("task-300"), first.tasks().stream().map(Task::id).toList());
        assertEquals(2, first.totalSize());
        assertEquals("cursor:301", first.nextPageToken());
        assertEquals(List.of("task-701"), second.tasks().stream().map(Task::id).toList());
        assertEquals(2, second.totalSize());
        assertNull(second.nextPageToken());
    }

    @Test
    void partialBatchDoesNotSkipLaterReadableTasks() {
        TaskStore store = new OpaqueTokenTaskStore(301);
        TaskAuthorizationProvider provider =
                allowing(Set.of("task-20", "task-21", "task-300")::contains);

        ListTasksResult first =
                store.listAuthorized(new ListTasksParams(1, null), context, provider);
        ListTasksResult second =
                store.listAuthorized(
                        new ListTasksParams(1, first.nextPageToken()), context, provider);
        ListTasksResult third =
                store.listAuthorized(
                        new ListTasksParams(1, second.nextPageToken()), context, provider);

        assertEquals(List.of("task-20"), first.tasks().stream().map(Task::id).toList());
        assertEquals("cursor:21", first.nextPageToken());
        assertEquals(List.of("task-21"), second.tasks().stream().map(Task::id).toList());
        assertEquals("cursor:22", second.nextPageToken());
        assertEquals(List.of("task-300"), third.tasks().stream().map(Task::id).toList());
        assertEquals(3, third.totalSize());
        assertNull(third.nextPageToken());
    }

    @Test
    void countAtScanLimitStillReturnsFirstPage() {
        TaskStore store = new OpaqueTokenTaskStore(20_000);

        ListTasksResult result =
                store.listAuthorized(new ListTasksParams(1, null), context, allowing(id -> true));

        assertEquals(20_000, result.totalSize());
        assertEquals(List.of("task-0"), result.tasks().stream().map(Task::id).toList());
        assertEquals("cursor:1", result.nextPageToken());
    }

    @Test
    void countAtScanLimitAllowsPartialFinalBatchAndDeniedLookahead() {
        TaskStore store = new OpaqueTokenTaskStore(20_000);

        ListTasksResult result =
                store.listAuthorized(
                        new ListTasksParams(1, null), context, allowing("task-19998"::equals));

        assertEquals(1, result.totalSize());
        assertEquals(List.of("task-19998"), result.tasks().stream().map(Task::id).toList());
        assertNull(result.nextPageToken());
    }

    @Test
    void countAtScanLimitAllowsPartialFinalBatchAndReadableLookahead() {
        TaskStore store = new OpaqueTokenTaskStore(20_000);
        TaskAuthorizationProvider provider = allowing(Set.of("task-19998", "task-19999")::contains);

        ListTasksResult first =
                store.listAuthorized(new ListTasksParams(1, null), context, provider);
        ListTasksResult second =
                store.listAuthorized(
                        new ListTasksParams(1, first.nextPageToken()), context, provider);

        assertEquals(2, first.totalSize());
        assertEquals(List.of("task-19998"), first.tasks().stream().map(Task::id).toList());
        assertEquals("cursor:19999", first.nextPageToken());
        assertEquals(2, second.totalSize());
        assertEquals(List.of("task-19999"), second.tasks().stream().map(Task::id).toList());
        assertNull(second.nextPageToken());
    }

    @Test
    void countOverScanLimitFailsInsteadOfReturningPartialCount() {
        TaskStore store = new OpaqueTokenTaskStore(20_001);

        assertThrows(
                TaskStoreException.class,
                () ->
                        store.listAuthorized(
                                new ListTasksParams(1, null), context, allowing(id -> true)));
    }

    @Test
    void queryLimitStillBoundsStoresReturningUndersizedBatches() {
        TaskStore store =
                new OpaqueTokenTaskStore(301) {
                    @Override
                    public ListTasksResult list(
                            ListTasksParams params, @Nullable ServerCallContext callContext) {
                        return super.list(new ListTasksParams(1, params.pageToken()), callContext);
                    }
                };

        assertThrows(
                TaskStoreException.class,
                () ->
                        store.listAuthorized(
                                new ListTasksParams(1, null), context, allowing(id -> true)));
    }

    @Test
    void repeatedTokenFailsInsteadOfLooping() {
        TaskStore store =
                new OpaqueTokenTaskStore(301) {
                    @Override
                    public ListTasksResult list(
                            ListTasksParams params, @Nullable ServerCallContext callContext) {
                        ListTasksResult result = super.list(params, callContext);
                        return new ListTasksResult(
                                result.tasks(),
                                result.totalSize(),
                                result.pageSize(),
                                "cursor:100");
                    }
                };

        assertThrows(
                TaskStoreException.class,
                () ->
                        store.listAuthorized(
                                new ListTasksParams(1, null), context, allowing(id -> true)));
    }

    @Test
    void exhaustedNormalQueryBudgetStillRecoversOpaqueBoundary() {
        OpaqueTokenTaskStore store =
                new OpaqueTokenTaskStore(19_800) {
                    @Override
                    public ListTasksResult list(
                            ListTasksParams params, @Nullable ServerCallContext callContext) {
                        return super.list(
                                new ListTasksParams(
                                        Math.min(66, params.getEffectivePageSize()),
                                        params.pageToken()),
                                callContext);
                    }
                };
        TaskAuthorizationProvider provider = allowing(Set.of("task-19798", "task-19799")::contains);

        ListTasksResult first =
                store.listAuthorized(new ListTasksParams(1, null), context, provider);

        assertEquals(List.of("task-19798"), first.tasks().stream().map(Task::id).toList());
        assertEquals(2, first.totalSize());
        assertEquals("cursor:19799", first.nextPageToken());
        assertEquals(601, store.queries);
        ListTasksResult second =
                store.listAuthorized(
                        new ListTasksParams(1, first.nextPageToken()), context, provider);
        assertEquals(List.of("task-19799"), second.tasks().stream().map(Task::id).toList());
        assertEquals(2, second.totalSize());
        assertNull(second.nextPageToken());
        assertEquals(902, store.queries);
    }

    @Test
    void shrinkingRecoveryResponsesPreserveBoundary() {
        OpaqueTokenTaskStore store = recoveryStore(false, false, false);
        ListTasksResult first =
                store.listAuthorized(
                        new ListTasksParams(1, null),
                        context,
                        allowing(Set.of("task-98", "task-99")::contains));
        assertEquals("cursor:99", first.nextPageToken());
        assertEquals(101, store.queries);
    }

    @Test
    void recoveryCanUseItsEntireHundredQueryAllowance() {
        OpaqueTokenTaskStore store = recoveryStore(false, false, true);
        ListTasksResult first =
                store.listAuthorized(
                        new ListTasksParams(1, null),
                        context,
                        allowing(Set.of("task-98", "task-99")::contains));
        assertEquals(List.of("task-98"), first.tasks().stream().map(Task::id).toList());
        assertEquals(2, first.totalSize());
        assertEquals("cursor:99", first.nextPageToken());
        assertEquals(102, store.queries);
    }

    @Test
    void recoveryAllowanceCannotBecomeAnUnboundedScan() {
        OpaqueTokenTaskStore store = recoveryStore(true, false, false);
        TaskStoreException failure =
                assertThrows(
                        TaskStoreException.class,
                        () ->
                                store.listAuthorized(
                                        new ListTasksParams(1, null),
                                        context,
                                        allowing(Set.of("task-98", "task-99")::contains)));
        assertTrue(failure.getMessage().contains("recovery"));
        assertEquals(102, store.queries);
    }

    @Test
    void repeatedRecoveryTokenFailsExplicitly() {
        OpaqueTokenTaskStore store = recoveryStore(true, true, false);
        TaskStoreException failure =
                assertThrows(
                        TaskStoreException.class,
                        () ->
                                store.listAuthorized(
                                        new ListTasksParams(1, null),
                                        context,
                                        allowing(Set.of("task-98", "task-99")::contains)));
        assertTrue(failure.getMessage().contains("repeated"));
        assertEquals(4, store.queries);
    }

    private static OpaqueTokenTaskStore recoveryStore(
            boolean emptyRecovery, boolean repeated, boolean initialEmpty) {
        return new OpaqueTokenTaskStore(100) {
            private int calls;

            @Override
            public ListTasksResult list(
                    ListTasksParams params, @Nullable ServerCallContext callContext) {
                calls++;
                if (initialEmpty && calls == 3) {
                    super.list(new ListTasksParams(1, null), callContext);
                    return new ListTasksResult(List.of(), 100, 0, "cursor:0");
                }
                if (calls <= 2) {
                    return super.list(params, callContext);
                }
                ListTasksResult result =
                        super.list(
                                new ListTasksParams(1, emptyRecovery ? null : params.pageToken()),
                                callContext);
                if (emptyRecovery) {
                    return new ListTasksResult(
                            List.of(), 100, 0, repeated ? "recovery:same" : "recovery:" + calls);
                }
                return result;
            }
        };
    }

    private static TaskAuthorizationProvider allowing(Predicate<String> readable) {
        return new SimpleTaskAuthorizationProvider() {
            @Override
            public boolean checkRead(
                    ServerCallContext context, String taskId, TaskOperation operation) {
                return readable.test(taskId);
            }
        };
    }

    private static class OpaqueTokenTaskStore implements TaskStore {
        private final List<Task> tasks;
        private int queries;

        private OpaqueTokenTaskStore(int totalTasks) {
            TaskStatus status = new TaskStatus(TaskState.TASK_STATE_COMPLETED);
            tasks =
                    IntStream.range(0, totalTasks)
                            .mapToObj(
                                    index ->
                                            Task.builder()
                                                    .id("task-" + index)
                                                    .contextId("context")
                                                    .status(status)
                                                    .build())
                            .toList();
        }

        @Override
        public void save(Task task, boolean isReplicated) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @Nullable Task get(String taskId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(String taskId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ListTasksResult list(ListTasksParams params, @Nullable ServerCallContext context) {
            queries++;
            int start =
                    params.pageToken() == null
                            ? 0
                            : Integer.parseInt(params.pageToken().substring("cursor:".length()));
            int end = Math.min(start + params.getEffectivePageSize(), tasks.size());
            List<Task> page = tasks.subList(start, end);
            return new ListTasksResult(
                    page, tasks.size(), page.size(), end < tasks.size() ? "cursor:" + end : null);
        }
    }
}
