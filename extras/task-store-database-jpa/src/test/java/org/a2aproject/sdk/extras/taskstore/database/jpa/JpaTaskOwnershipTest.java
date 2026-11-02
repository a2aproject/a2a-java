package org.a2aproject.sdk.extras.taskstore.database.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.TransactionManager;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.TaskOperation;
import org.a2aproject.sdk.server.auth.User;
import org.junit.jupiter.api.Test;

@QuarkusTest
class JpaTaskOwnershipTest {
    @Inject JpaTaskOwnershipRepository repository;
    @Inject EntityManager em;
    @Inject EntityManagerFactory factory;
    @Inject TransactionManager manager;
    @Inject JpaDatabaseTaskStore store;

    @Inject @jakarta.enterprise.inject.Any
    jakarta.enterprise.inject.Instance<org.a2aproject.sdk.server.auth.TaskAuthorizationProvider>
            providers;

    static ServerCallContext context(String name) {
        return new ServerCallContext(
                new User() {
                    public boolean isAuthenticated() {
                        return true;
                    }

                    public String getUsername() {
                        return name;
                    }
                },
                Map.of(),
                Set.of());
    }

    @Test
    void providerRemainsOptIn() {
        assertTrue(providers.isUnsatisfied());
    }

    @Test
    void persistedExactIdentitiesAndRestart() {
        JpaTaskAuthorizationProvider provider = new JpaTaskAuthorizationProvider(repository);
        for (String[] pair :
                new String[][] {
                    {"Alice", "alice"},
                    {"Jose", "José"},
                    {"é", "e\u0301"},
                    {"alice", "alice "},
                    {"😀", "😁"}
                }) {
            String first = UUID.randomUUID().toString();
            String second = UUID.randomUUID().toString();
            provider.recordOwnership(context(pair[0]), first, TaskOperation.LIST_TASKS);
            provider.recordOwnership(context(pair[1]), second, TaskOperation.LIST_TASKS);
            QuarkusTransaction.requiringNew().run(() -> em.clear());
            JpaTaskAuthorizationProvider restarted =
                    new JpaTaskAuthorizationProvider(
                            JpaTaskOwnershipRepository.create(factory, manager));
            assertTrue(restarted.checkRead(context(pair[0]), first, TaskOperation.LIST_TASKS));
            assertTrue(restarted.checkWrite(context(pair[1]), second, TaskOperation.LIST_TASKS));
            assertFalse(restarted.checkRead(context(pair[1]), first, TaskOperation.LIST_TASKS));
            assertFalse(restarted.checkWrite(context(pair[0]), second, TaskOperation.LIST_TASKS));
            restarted.recordOwnership(context(pair[1]), first, TaskOperation.LIST_TASKS);
            assertTrue(restarted.checkRead(context(pair[0]), first, TaskOperation.LIST_TASKS));
            assertTrue(restarted.isTaskRecorded(first));
            // Ownership exists before the task and is removed even when no task row exists.
            store.delete(first);
            assertFalse(restarted.isTaskRecorded(first));
        }
        assertFalse(provider.checkRead(context("Alice"), "unknown", TaskOperation.LIST_TASKS));
        assertTrue(provider.checkRead(context("Alice"), "", TaskOperation.LIST_TASKS));
        User anonymous = mock(User.class);
        ServerCallContext anonymousContext = new ServerCallContext(anonymous, Map.of(), Set.of());
        assertFalse(provider.checkCreate(anonymousContext, TaskOperation.LIST_TASKS));
        assertFalse(provider.checkRead(anonymousContext, "", TaskOperation.LIST_TASKS));
        assertTrue(provider.getCapability(String.class).isEmpty());
        assertTrue(provider.getCapability(JpaTaskAuthorizationCapability.class).isPresent());
    }

    @Test
    void concurrentFirstWriterDoesNotPoisonCallerTransactions() throws Exception {
        String id = UUID.randomUUID().toString();
        CyclicBarrier start = new CyclicBarrier(2);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var futures =
                    java.util.stream.Stream.of("first", "second")
                            .map(
                                    subject ->
                                            executor.submit(
                                                    () -> {
                                                        start.await(20, TimeUnit.SECONDS);
                                                        QuarkusTransaction.requiringNew()
                                                                .run(
                                                                        () -> {
                                                                            new JpaTaskAuthorizationProvider(
                                                                                            repository)
                                                                                    .recordOwnership(
                                                                                            context(
                                                                                                    subject),
                                                                                            id,
                                                                                            TaskOperation
                                                                                                    .LIST_TASKS);
                                                                            em.createNativeQuery(
                                                                                            "INSERT"
                                                                                                + " INTO"
                                                                                                + " a2a_tasks(task_id,"
                                                                                                + " task_data)"
                                                                                                + " VALUES"
                                                                                                + " (:id,"
                                                                                                + " '{}')")
                                                                                    .setParameter(
                                                                                            "id",
                                                                                            id
                                                                                                    + subject)
                                                                                    .executeUpdate();
                                                                        });
                                                        return subject;
                                                    }))
                            .toList();
            for (var future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
            JpaTaskAuthorizationProvider provider = new JpaTaskAuthorizationProvider(repository);
            assertTrue(
                    provider.checkRead(context("first"), id, TaskOperation.LIST_TASKS)
                            ^ provider.checkRead(context("second"), id, TaskOperation.LIST_TASKS));
            QuarkusTransaction.requiringNew()
                    .run(
                            () ->
                                    assertEquals(
                                            2L,
                                            em.createQuery(
                                                            "SELECT COUNT(t) FROM JpaTask t WHERE"
                                                                    + " t.id LIKE :prefix",
                                                            Long.class)
                                                    .setParameter("prefix", id + "%")
                                                    .getSingleResult()));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void invalidSubjectsFailBeforeAnyPersistenceAccess() {
        JpaTaskOwnershipRepository untouched = mock(JpaTaskOwnershipRepository.class);
        JpaTaskAuthorizationProvider provider = new JpaTaskAuthorizationProvider(untouched);
        for (String subject : new String[] {"\ud800", "é".repeat(513)}) {
            var context = context(subject);
            assertThrows(
                    IllegalArgumentException.class,
                    () -> provider.checkRead(context, "", TaskOperation.LIST_TASKS));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> provider.checkRead(context, "id", TaskOperation.LIST_TASKS));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> provider.checkWrite(context, "id", TaskOperation.LIST_TASKS));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> provider.checkCreate(context, TaskOperation.LIST_TASKS));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> provider.recordOwnership(context, "id", TaskOperation.LIST_TASKS));
        }
        org.mockito.Mockito.verifyNoInteractions(untouched);
    }

    @Test
    void programmaticFailureRollsBackAndRestoresCaller() throws Exception {
        EntityManagerFactory failingFactory = mock(EntityManagerFactory.class);
        TransactionManager transactions = mock(TransactionManager.class);
        jakarta.transaction.Transaction caller = mock(jakarta.transaction.Transaction.class);
        jakarta.transaction.Transaction inner = mock(jakarta.transaction.Transaction.class);
        when(transactions.suspend()).thenReturn(caller);
        when(transactions.getTransaction()).thenReturn(inner);
        when(failingFactory.createEntityManager())
                .thenThrow(new IllegalStateException("no persistence context"));
        JpaTaskOwnershipRepository programmatic =
                JpaTaskOwnershipRepository.create(failingFactory, transactions);
        assertThrows(IllegalStateException.class, () -> programmatic.owner("id"));
        org.mockito.InOrder order = org.mockito.Mockito.inOrder(transactions);
        order.verify(transactions).suspend();
        order.verify(transactions).begin();
        order.verify(transactions).getTransaction();
        order.verify(transactions).rollback();
        order.verify(transactions).resume(caller);
    }

    @Test
    void missingOwnerAndFailedConfirmationPreserveInsertFailure() {
        JpaTaskOwnershipTransactions transactions = mock(JpaTaskOwnershipTransactions.class);
        RuntimeException failure = new IllegalStateException("insert failure");
        org.mockito.Mockito.doThrow(failure)
                .when(transactions)
                .insert(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any());
        JpaTaskOwnershipRepository failing = new JpaTaskOwnershipRepository(transactions);
        assertEquals(
                failure,
                assertThrows(RuntimeException.class, () -> failing.record("id", new byte[] {1})));
        when(transactions.owner("id")).thenThrow(new IllegalStateException("confirmation failure"));
        assertEquals(
                failure,
                assertThrows(RuntimeException.class, () -> failing.record("id", new byte[] {1})));
        assertEquals(1, failure.getSuppressed().length);
    }
}
