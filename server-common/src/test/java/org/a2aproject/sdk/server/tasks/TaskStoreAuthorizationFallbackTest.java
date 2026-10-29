package org.a2aproject.sdk.server.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.IntStream;

import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.TaskAuthorizationProvider;
import org.a2aproject.sdk.server.auth.TaskOperation;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.junit.jupiter.api.Test;

class TaskStoreAuthorizationFallbackTest {

    @Test
    void countAtScanLimitStillReturnsFirstPage() {
        int totalTasks = 20_000;
        TaskStore store = mock(TaskStore.class, CALLS_REAL_METHODS);
        ServerCallContext context = mock(ServerCallContext.class);
        TaskAuthorizationProvider provider = mock(TaskAuthorizationProvider.class);
        TaskStatus status = new TaskStatus(TaskState.TASK_STATE_COMPLETED);
        when(provider.checkRead(eq(context), anyString(), eq(TaskOperation.LIST_TASKS))).thenReturn(true);
        when(store.list(any(ListTasksParams.class), eq(context))).thenAnswer(invocation -> {
            ListTasksParams query = invocation.getArgument(0);
            int start = query.pageToken() == null ? 0 : Integer.parseInt(query.pageToken());
            int end = Math.min(start + query.getEffectivePageSize(), totalTasks);
            List<Task> tasks = IntStream.range(start, end)
                    .mapToObj(index -> Task.builder().id("task-" + index).contextId("context")
                            .status(status).build())
                    .toList();
            return new ListTasksResult(tasks, totalTasks, tasks.size(),
                    end < totalTasks ? Integer.toString(end) : null);
        });

        ListTasksResult result = store.listAuthorized(new ListTasksParams(1, null), context, provider);

        assertEquals(20_000, result.totalSize());
        assertEquals(List.of("task-0"), result.tasks().stream().map(Task::id).toList());
        assertEquals("1", result.nextPageToken());
    }
}
