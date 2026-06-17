package org.a2aproject.sdk.server.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.TaskAuthorizationProvider;
import org.a2aproject.sdk.server.auth.TaskOperation;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InMemoryTaskStoreAuthorizationTest {

    @Mock
    private TaskAuthorizationProvider authorizationProvider;

    @Mock
    private ServerCallContext context;

    private InMemoryTaskStore store;

    private static Task testTask(String id) {
        return Task.builder()
                .id(id)
                .contextId("ctx-1")
                .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED))
                .history(Collections.emptyList())
                .artifacts(Collections.emptyList())
                .build();
    }

    @BeforeEach
    void setUp() {
        store = new InMemoryTaskStore(authorizationProvider);
    }

    @Test
    void list_filtersUnauthorizedTasks() throws Exception {
        store.save(testTask("task-1"), false);
        store.save(testTask("task-2"), false);
        store.save(testTask("task-3"), false);

        when(authorizationProvider.checkRead(eq(context), eq("task-1"), eq(TaskOperation.LIST_TASKS)))
                .thenReturn(true);
        when(authorizationProvider.checkRead(eq(context), eq("task-2"), eq(TaskOperation.LIST_TASKS)))
                .thenReturn(false);
        when(authorizationProvider.checkRead(eq(context), eq("task-3"), eq(TaskOperation.LIST_TASKS)))
                .thenReturn(true);

        ListTasksParams params = new ListTasksParams();
        ListTasksResult result = store.list(params, context);

        assertEquals(2, result.tasks().size());
        assertEquals(2, result.totalSize());
        assertTrue(result.tasks().stream().anyMatch(t -> t.id().equals("task-1")));
        assertTrue(result.tasks().stream().anyMatch(t -> t.id().equals("task-3")));
        assertTrue(result.tasks().stream().noneMatch(t -> t.id().equals("task-2")));
    }

    @Test
    void list_noProvider_returnsAllTasks() throws Exception {
        InMemoryTaskStore storeNoAuth = new InMemoryTaskStore((TaskAuthorizationProvider) null);
        storeNoAuth.save(testTask("task-1"), false);
        storeNoAuth.save(testTask("task-2"), false);

        ListTasksParams params = new ListTasksParams();
        ListTasksResult result = storeNoAuth.list(params, context);

        assertEquals(2, result.tasks().size());
    }

    @Test
    void list_paginationCorrectWithFiltering() throws Exception {
        for (int i = 1; i <= 5; i++) {
            store.save(testTask("task-" + i), false);
        }

        when(authorizationProvider.checkRead(eq(context), eq("task-1"), eq(TaskOperation.LIST_TASKS)))
                .thenReturn(true);
        when(authorizationProvider.checkRead(eq(context), eq("task-2"), eq(TaskOperation.LIST_TASKS)))
                .thenReturn(false);
        when(authorizationProvider.checkRead(eq(context), eq("task-3"), eq(TaskOperation.LIST_TASKS)))
                .thenReturn(true);
        when(authorizationProvider.checkRead(eq(context), eq("task-4"), eq(TaskOperation.LIST_TASKS)))
                .thenReturn(false);
        when(authorizationProvider.checkRead(eq(context), eq("task-5"), eq(TaskOperation.LIST_TASKS)))
                .thenReturn(true);

        ListTasksParams params = new ListTasksParams();
        ListTasksResult result = store.list(params, context);

        assertEquals(3, result.totalSize());
        assertEquals(3, result.pageSize());
    }
}
