package org.a2aproject.sdk.extras.taskstore.database.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.TaskAuthorizationProvider;
import org.a2aproject.sdk.server.auth.TaskOperation;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.junit.jupiter.api.Test;

@QuarkusTest
class JpaTaskAuthorizationQueryTest {
    @Inject JpaDatabaseTaskStore store;
    @Inject JpaTaskOwnershipRepository repository;
    @Inject EntityManager em;

    @Test
    void exactIdentityIsolationWithEqualTimestampPagesAndFallback() {
        JpaTaskAuthorizationProvider provider = new JpaTaskAuthorizationProvider(repository);
        for (String[] pair :
                new String[][] {
                    {"Alice", "alice"}, {"Jose", "José"}, {"é", "e\u0301"}, {"alice", "alice "}
                }) {
            String group = UUID.randomUUID().toString();
            for (int i = 0; i < 8; i++) {
                String owner = pair[i % 2];
                String id = group + i;
                persist(
                        id,
                        group,
                        TaskState.TASK_STATE_WORKING,
                        OffsetDateTime.parse("2026-01-01T00:00:00Z"));
                provider.recordOwnership(
                        JpaTaskOwnershipTest.context(owner), id, TaskOperation.LIST_TASKS);
            }
            for (int subject = 0; subject < 2; subject++) {
                ServerCallContext context = JpaTaskOwnershipTest.context(pair[subject]);
                List<String> expected = new ArrayList<>();
                for (int i = subject; i < 8; i += 2) {
                    expected.add(group + i);
                }
                assertEquals(expected, pages(group, context, provider));
                JpaTaskAuthorizationProvider observed = org.mockito.Mockito.spy(provider);
                var handler =
                        org.a2aproject.sdk.server.requesthandlers.DefaultRequestHandler.builder()
                                .taskStore(store)
                                .agentExecutor(
                                        mock(
                                                org.a2aproject.sdk.server.agentexecution
                                                        .AgentExecutor.class))
                                .queueManager(
                                        mock(org.a2aproject.sdk.server.events.QueueManager.class))
                                .mainEventBusProcessor(
                                        mock(
                                                org.a2aproject.sdk.server.events
                                                        .MainEventBusProcessor.class))
                                .executor(Runnable::run)
                                .eventConsumerExecutor(Runnable::run)
                                .authorizationProvider(observed)
                                .build();
                var handlerPage =
                        handler.onListTasks(
                                new ListTasksParams(group, null, 1, null, null, null, null, null),
                                context);
                assertEquals(4, handlerPage.totalSize());
                assertEquals(expected.get(0), handlerPage.tasks().get(0).id());
                org.mockito.Mockito.verify(observed)
                        .checkRead(context, "", TaskOperation.LIST_TASKS);
                org.mockito.Mockito.verify(observed)
                        .checkRead(context, expected.get(0), TaskOperation.LIST_TASKS);
                org.mockito.Mockito.verify(observed, org.mockito.Mockito.times(2))
                        .checkRead(
                                org.mockito.ArgumentMatchers.eq(context),
                                org.mockito.ArgumentMatchers.anyString(),
                                org.mockito.ArgumentMatchers.eq(TaskOperation.LIST_TASKS));
                TaskAuthorizationProvider wrapper = legacy(provider);
                assertEquals(expected, pages(group, context, wrapper));
            }
        }
    }

    private List<String> pages(
            String group, ServerCallContext context, TaskAuthorizationProvider provider) {
        String token = null;
        List<String> ids = new ArrayList<>();
        do {
            ListTasksResult page =
                    store.listAuthorized(
                            new ListTasksParams(group, null, 1, token, null, null, null, null),
                            context,
                            provider);
            assertEquals(4, page.totalSize());
            assertEquals(1, page.tasks().size());
            ids.add(page.tasks().get(0).id());
            token = page.nextPageToken();
        } while (token != null);
        return ids;
    }

    @Test
    void ordinaryFiltersAndAllNoneScopesApplyBeforeCountAndPage() {
        String group = UUID.randomUUID().toString();
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        persist(group + "1", group, TaskState.TASK_STATE_WORKING, timestamp);
        persist(group + "2", group, TaskState.TASK_STATE_SUBMITTED, timestamp);
        persist(group + "3", group, TaskState.TASK_STATE_WORKING, timestamp.minusDays(1));
        ListTasksParams params =
                new ListTasksParams(
                        group,
                        TaskState.TASK_STATE_WORKING,
                        1,
                        null,
                        null,
                        timestamp.minusHours(1).toInstant(),
                        null,
                        null);
        ServerCallContext context = JpaTaskOwnershipTest.context("filter-owner");
        TaskAuthorizationProvider all = scoped(new JpaTaskReadScope.All());
        ListTasksResult page = store.listAuthorized(params, context, all);
        assertEquals(1, page.totalSize());
        assertEquals(group + "1", page.tasks().get(0).id());
        assertNull(page.nextPageToken());
        ListTasksResult none =
                store.listAuthorized(params, context, scoped(new JpaTaskReadScope.None()));
        assertEquals(0, none.totalSize());
        assertTrue(none.tasks().isEmpty());
        JpaTaskAuthorizationProvider owned = new JpaTaskAuthorizationProvider(repository);
        owned.recordOwnership(context, group + "1", TaskOperation.LIST_TASKS);
        assertEquals(1, store.listAuthorized(params, context, owned).totalSize());
        assertEquals(
                0,
                store.listAuthorized(params, JpaTaskOwnershipTest.context("someone else"), owned)
                        .totalSize());
    }

    @Test
    void optimizedQueryExceedsTenThousandCandidatesWithoutIndividualChecks() {
        String group = UUID.randomUUID().toString();
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        QuarkusTransaction.requiringNew()
                .run(
                        () -> {
                            for (int i = 0; i < 10003; i++) {
                                try {
                                    em.persist(
                                            JpaTask.createFromTask(
                                                    task(
                                                            group + String.format("%05d", i),
                                                            group,
                                                            TaskState.TASK_STATE_WORKING,
                                                            timestamp)));
                                } catch (Exception e) {
                                    throw new IllegalStateException(e);
                                }
                                if (i % 100 == 0) {
                                    em.flush();
                                    em.clear();
                                }
                            }
                        });
        String subject = "large-owner";
        for (int i = 10000; i < 10003; i++) {
            repository.record(group + i, JpaSubjectKey.encode(subject));
        }
        TaskAuthorizationProvider provider = scoped(new JpaTaskReadScope.OwnedBy(subject));
        ListTasksParams params = new ListTasksParams(group, null, 1, null, null, null, null, null);
        ListTasksResult first =
                store.listAuthorized(params, JpaTaskOwnershipTest.context(subject), provider);
        assertEquals(3, first.totalSize());
        assertEquals(group + "10000", first.tasks().get(0).id());
        assertNotNull(first.nextPageToken());
        ListTasksResult second =
                store.listAuthorized(
                        new ListTasksParams(
                                group, null, 1, first.nextPageToken(), null, null, null, null),
                        JpaTaskOwnershipTest.context(subject),
                        provider);
        assertEquals(3, second.totalSize());
        assertEquals(group + "10001", second.tasks().get(0).id());
        ListTasksResult third =
                store.listAuthorized(
                        new ListTasksParams(
                                group, null, 1, second.nextPageToken(), null, null, null, null),
                        JpaTaskOwnershipTest.context(subject),
                        provider);
        assertEquals(3, third.totalSize());
        assertEquals(group + "10002", third.tasks().get(0).id());
        assertNull(third.nextPageToken());
        org.mockito.Mockito.verify(provider, org.mockito.Mockito.never())
                .checkRead(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any());
        assertThrows(
                org.a2aproject.sdk.server.tasks.TaskStoreException.class,
                () ->
                        store.listAuthorized(
                                params,
                                JpaTaskOwnershipTest.context(subject),
                                legacy(new JpaTaskAuthorizationProvider(repository))));
    }

    @Test
    void capabilityLookupAndScopeFailuresPropagate() {
        TaskAuthorizationProvider provider = mock(TaskAuthorizationProvider.class);
        when(provider.getCapability(JpaTaskAuthorizationCapability.class))
                .thenThrow(new IllegalStateException("lookup"));
        assertThrows(
                IllegalStateException.class,
                () ->
                        store.listAuthorized(
                                new ListTasksParams(),
                                JpaTaskOwnershipTest.context("x"),
                                provider));
        org.mockito.Mockito.reset(provider);
        when(provider.getCapability(JpaTaskAuthorizationCapability.class))
                .thenReturn(
                        Optional.of(
                                (context, operation) -> {
                                    throw new IllegalStateException("scope");
                                }));
        assertThrows(
                IllegalStateException.class,
                () ->
                        store.listAuthorized(
                                new ListTasksParams(),
                                JpaTaskOwnershipTest.context("x"),
                                provider));
    }

    private static TaskAuthorizationProvider scoped(JpaTaskReadScope scope) {
        TaskAuthorizationProvider provider = mock(TaskAuthorizationProvider.class);
        when(provider.getCapability(JpaTaskAuthorizationCapability.class))
                .thenReturn(Optional.of((context, operation) -> scope));
        return provider;
    }

    private static TaskAuthorizationProvider legacy(JpaTaskAuthorizationProvider delegate) {
        return new TaskAuthorizationProvider() {
            public boolean checkRead(ServerCallContext c, String id, TaskOperation op) {
                return delegate.checkRead(c, id, op);
            }

            public boolean checkWrite(ServerCallContext c, String id, TaskOperation op) {
                return delegate.checkWrite(c, id, op);
            }

            public boolean checkCreate(ServerCallContext c, TaskOperation op) {
                return delegate.checkCreate(c, op);
            }

            public boolean isTaskRecorded(String id) {
                return delegate.isTaskRecorded(id);
            }

            public void recordOwnership(ServerCallContext c, String id, TaskOperation op) {
                delegate.recordOwnership(c, id, op);
            }
        };
    }

    private void persist(String id, String group, TaskState state, OffsetDateTime timestamp) {
        QuarkusTransaction.requiringNew()
                .run(
                        () -> {
                            try {
                                em.persist(
                                        JpaTask.createFromTask(task(id, group, state, timestamp)));
                            } catch (Exception e) {
                                throw new IllegalStateException(e);
                            }
                        });
    }

    private static Task task(String id, String group, TaskState state, OffsetDateTime timestamp) {
        return Task.builder()
                .id(id)
                .contextId(group)
                .status(new TaskStatus(state, null, timestamp))
                .build();
    }
}
