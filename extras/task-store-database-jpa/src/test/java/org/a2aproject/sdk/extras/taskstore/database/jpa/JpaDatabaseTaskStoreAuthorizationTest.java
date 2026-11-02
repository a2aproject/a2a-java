package org.a2aproject.sdk.extras.taskstore.database.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.inject.Instance;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.TaskAuthorizationProvider;
import org.a2aproject.sdk.server.auth.TaskOperation;
import org.a2aproject.sdk.server.tasks.TaskStoreException;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.junit.jupiter.api.Test;

class JpaDatabaseTaskStoreAuthorizationTest {

    @Test
    @SuppressWarnings("unchecked")
    void listFailsClosedBeforeParsingTokenWhenContextIsNull() {
        TaskAuthorizationProvider authProvider = mock(TaskAuthorizationProvider.class);
        Instance<TaskAuthorizationProvider> instance = mock(Instance.class);
        when(instance.isResolvable()).thenReturn(true);
        when(instance.get()).thenReturn(authProvider);
        JpaDatabaseTaskStore store = new JpaDatabaseTaskStore(instance);

        ListTasksResult result = store.list(new ListTasksParams(1, "invalid-token"), null);

        assertEquals(List.of(), result.tasks());
        assertEquals(0, result.totalSize());
        assertNull(result.nextPageToken());
    }

    @Test
    void listWithProviderPreservesAuthorizedTasksBeyondFirstPage() throws Exception {
        TaskAuthorizationProvider authProvider = mock(TaskAuthorizationProvider.class);
        ServerCallContext context = mock(ServerCallContext.class);
        Task first = task("task-1", OffsetDateTime.of(2025, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC));
        Task second = task("task-2", OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        when(authProvider.checkRead(context, first.id(), TaskOperation.LIST_TASKS)).thenReturn(true);
        when(authProvider.checkRead(context, second.id(), TaskOperation.LIST_TASKS)).thenReturn(true);
        JpaDatabaseTaskStore store = storeWithProvider(authProvider,
                entityManagerReturning(first, second), 10);

        ListTasksResult firstPage = store.list(new ListTasksParams(1, null), context);

        assertEquals(List.of(first), firstPage.tasks());
        assertEquals(2, firstPage.totalSize());
        assertEquals(1, firstPage.pageSize());
        assertNotNull(firstPage.nextPageToken());

        ListTasksResult secondPage = store.list(new ListTasksParams(1, firstPage.nextPageToken()), context);

        assertEquals(List.of(second), secondPage.tasks());
        assertEquals(2, secondPage.totalSize());
        assertNull(secondPage.nextPageToken());
    }

    @Test
    void listWithProviderSkipsDeniedTasksWithoutReturningAnEmptyPage() throws Exception {
        TaskAuthorizationProvider authProvider = mock(TaskAuthorizationProvider.class);
        ServerCallContext context = mock(ServerCallContext.class);
        Task firstDenied = task("task-1", OffsetDateTime.of(2025, 1, 3, 0, 0, 0, 0, ZoneOffset.UTC));
        Task secondDenied = task("task-2", OffsetDateTime.of(2025, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC));
        Task authorized = task("task-3", OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        when(authProvider.checkRead(context, firstDenied.id(), TaskOperation.LIST_TASKS)).thenReturn(false);
        when(authProvider.checkRead(context, secondDenied.id(), TaskOperation.LIST_TASKS)).thenReturn(false);
        when(authProvider.checkRead(context, authorized.id(), TaskOperation.LIST_TASKS)).thenReturn(true);
        JpaDatabaseTaskStore store = storeWithProvider(authProvider,
                entityManagerReturning(firstDenied, secondDenied, authorized), 10);

        ListTasksResult firstPage = store.list(new ListTasksParams(1, null), context);

        assertEquals(List.of(authorized), firstPage.tasks());
        assertEquals(1, firstPage.totalSize());
        assertNull(firstPage.nextPageToken());
    }

    @Test
    void listAuthorizedReturnsPageForBuilderConstructedStore() throws Exception {
        TaskAuthorizationProvider authProvider = mock(TaskAuthorizationProvider.class);
        ServerCallContext context = mock(ServerCallContext.class);
        Task first = task("task-1", OffsetDateTime.of(2025, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC));
        Task second = task("task-2", OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        when(authProvider.checkRead(context, first.id(), TaskOperation.LIST_TASKS)).thenReturn(true);
        when(authProvider.checkRead(context, second.id(), TaskOperation.LIST_TASKS)).thenReturn(true);
        JpaDatabaseTaskStore store = new JpaDatabaseTaskStore();
        setField(store, "em", entityManagerReturning(first, second));
        setField(store, "maxScanned", 10L);

        ListTasksResult result = store.listAuthorized(new ListTasksParams(1, null), context, authProvider);

        assertEquals(List.of(first), result.tasks());
        assertEquals(2, result.totalSize());
        assertNotNull(result.nextPageToken());
    }

    @Test
    void listAuthorizedFailsWhenExactCountExceedsScanLimit() throws Exception {
        TaskAuthorizationProvider authProvider = mock(TaskAuthorizationProvider.class);
        ServerCallContext context = mock(ServerCallContext.class);
        Task first = task("task-1", OffsetDateTime.of(2025, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC));
        Task second = task("task-2", OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        when(authProvider.checkRead(context, first.id(), TaskOperation.LIST_TASKS)).thenReturn(true);
        when(authProvider.checkRead(context, second.id(), TaskOperation.LIST_TASKS)).thenReturn(false);
        JpaDatabaseTaskStore store = storeWithProvider(authProvider, entityManagerReturning(first, second), 1);

        assertThrows(TaskStoreException.class,
                () -> store.listAuthorized(new ListTasksParams(1, null), context, authProvider));
    }

    @Test
    void listWithProviderDoesNotAdvertiseTokenForFullTerminalBatch() throws Exception {
        TaskAuthorizationProvider authProvider = mock(TaskAuthorizationProvider.class);
        ServerCallContext context = mock(ServerCallContext.class);
        Task task = task("task-1", OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        when(authProvider.checkRead(context, task.id(), TaskOperation.LIST_TASKS)).thenReturn(true);
        JpaDatabaseTaskStore store = storeWithProvider(authProvider, entityManagerReturning(task), 10);
        setField(store, "authorizationScanBatchSize", 1);

        ListTasksResult result = store.list(new ListTasksParams(1, null), context);

        assertEquals(List.of(task), result.tasks());
        assertEquals(1, result.totalSize());
        assertNull(result.nextPageToken());
    }

    @SuppressWarnings("unchecked")
    private static JpaDatabaseTaskStore storeWithProvider(TaskAuthorizationProvider provider,
            EntityManager entityManager, long maxScanned) throws Exception {
        Instance<TaskAuthorizationProvider> instance = mock(Instance.class);
        when(instance.isResolvable()).thenReturn(true);
        when(instance.get()).thenReturn(provider);
        JpaDatabaseTaskStore store = new JpaDatabaseTaskStore(instance);
        setField(store, "em", entityManager);
        setField(store, "maxScanned", maxScanned);
        return store;
    }

    @SuppressWarnings("unchecked")
    private static EntityManager entityManagerReturning(Task... tasks) throws Exception {
        List<JpaTask> jpaTasks = new java.util.ArrayList<>(tasks.length);
        for (Task task : tasks) {
            jpaTasks.add(JpaTask.createFromTask(task));
        }
        EntityManager entityManager = mock(EntityManager.class);
        when(entityManager.createQuery(org.mockito.ArgumentMatchers.anyString(), eq(JpaTask.class))).thenAnswer(invocation -> {
            AtomicInteger maxResults = new AtomicInteger();
            AtomicReference<java.time.Instant> timestamp = new AtomicReference<>();
            AtomicReference<String> id = new AtomicReference<>();
            TypedQuery<JpaTask> query = mock(TypedQuery.class);
            when(query.setMaxResults(anyInt())).thenAnswer(call -> {
                maxResults.set(call.getArgument(0));
                return query;
            });
            when(query.setParameter(eq("tokenTimestamp"), org.mockito.ArgumentMatchers.any(java.time.Instant.class)))
                    .thenAnswer(call -> {
                        timestamp.set(call.getArgument(1));
                        return query;
                    });
            when(query.setParameter(eq("tokenId"), org.mockito.ArgumentMatchers.anyString())).thenAnswer(call -> {
                id.set(call.getArgument(1));
                return query;
            });
            when(query.getResultList()).thenAnswer(call -> jpaTasks.stream()
                    .filter(task -> timestamp.get() == null
                            || task.getStatusTimestamp().isBefore(timestamp.get())
                            || (task.getStatusTimestamp().equals(timestamp.get())
                                    && task.getId().compareTo(id.get()) > 0))
                    .sorted((left, right) -> {
                        int timestampOrder = right.getStatusTimestamp().compareTo(left.getStatusTimestamp());
                        return timestampOrder != 0 ? timestampOrder : left.getId().compareTo(right.getId());
                    })
                    .limit(maxResults.get())
                    .toList());
            return query;
        });
        return entityManager;
    }

    private static Task task(String id, OffsetDateTime timestamp) {
        return Task.builder().id(id).contextId("context")
                .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED, null, timestamp)).build();
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = JpaDatabaseTaskStore.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
