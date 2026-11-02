package org.a2aproject.sdk.extras.taskstore.database.jpa;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.hibernate.orm.PersistenceUnit;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionManager;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.auth.TaskAuthorizationProvider;
import org.a2aproject.sdk.server.auth.TaskOperation;
import org.a2aproject.sdk.server.auth.User;
import org.a2aproject.sdk.server.events.MainEventBusProcessor;
import org.a2aproject.sdk.server.events.QueueManager;
import org.a2aproject.sdk.server.requesthandlers.DefaultRequestHandler;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Identical security assertions run against each production database dialect. */
abstract class JpaTaskAuthorizationDatabaseTest {
    private static final List<String> SUBJECTS =
            List.of("Alice", "alice", "Jose", "José", "é", "e\u0301", "alice ");
    private static final OffsetDateTime TIMESTAMP = OffsetDateTime.parse("2025-01-01T00:00:00Z");

    @Inject JpaTaskAuthorizationProvider provider;
    @Inject JpaDatabaseTaskStore store;
    @Inject TransactionManager transactionManager;

    @Inject
    @PersistenceUnit("a2a-java")
    EntityManagerFactory factory;

    @Inject
    @DataSource("a2a-java")
    AgroalDataSource dataSource;

    protected abstract boolean mysql();

    @BeforeEach
    void clearDatabase() throws Exception {
        inTransaction(
                em -> {
                    em.createQuery("DELETE FROM JpaTaskOwner").executeUpdate();
                    em.createQuery("DELETE FROM JpaTask").executeUpdate();
                });
    }

    @Test
    void identitiesStaySeparateForChecksOptimizedQueriesHandlerAndFallback() throws Exception {
        for (int subject = 0; subject < SUBJECTS.size(); subject++) {
            for (int page = 0; page < 3; page++) {
                String id = taskId(subject, page);
                store.save(task(id), false);
                provider.recordOwnership(
                        context(SUBJECTS.get(subject)), id, TaskOperation.MESSAGE_SEND);
            }
        }
        // Separate transactions and contexts force every subsequent check to reload persisted
        // bytes.
        inTransaction(EntityManager::clear);
        TaskAuthorizationProvider fallback = new UnsupportedCapabilityProvider(provider);
        for (int subject = 0; subject < SUBJECTS.size(); subject++) {
            ServerCallContext context = context(SUBJECTS.get(subject));
            List<String> expected =
                    List.of(taskId(subject, 0), taskId(subject, 1), taskId(subject, 2));
            for (int other = 0; other < SUBJECTS.size(); other++) {
                String id = taskId(other, 0);
                assertEquals(
                        subject == other, provider.checkRead(context, id, TaskOperation.GET_TASK));
                assertEquals(
                        subject == other,
                        provider.checkWrite(context, id, TaskOperation.CANCEL_TASK));
                provider.recordOwnership(context, id, TaskOperation.MESSAGE_SEND);
            }
            assertPages(
                    expected,
                    context,
                    (params, call) -> store.listAuthorized(params, call, provider));
            assertPages(expected, context, store::list);
            assertPages(expected, context, handler(provider)::onListTasks);
            assertPages(
                    expected,
                    context,
                    (params, call) -> store.listAuthorized(params, call, fallback));
            assertPages(expected, context, handler(fallback)::onListTasks);
        }
        // Attempts to re-record another subject's task cannot transfer ownership.
        for (int subject = 0; subject < SUBJECTS.size(); subject++) {
            assertArrayEquals(
                    SUBJECTS.get(subject).getBytes(StandardCharsets.UTF_8),
                    owner(taskId(subject, 0)));
        }
    }

    @Test
    void generatedSchemaUsesCompleteIndexedVariableLengthBinaryIdentity() throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            String columnSql =
                    mysql()
                            ? "SELECT DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE FROM"
                                  + " information_schema.columns WHERE TABLE_SCHEMA=DATABASE() AND"
                                  + " TABLE_NAME='a2a_task_owners' AND COLUMN_NAME='subject_key'"
                            : "SELECT data_type, character_maximum_length, is_nullable FROM"
                                    + " information_schema.columns WHERE table_schema='public' AND"
                                    + " table_name='a2a_task_owners' AND column_name='subject_key'";
            try (ResultSet result = statement.executeQuery(columnSql)) {
                assertTrue(result.next());
                assertEquals(mysql() ? "varbinary" : "bytea", result.getString(1));
                if (mysql()) {
                    assertEquals(1024, result.getInt(2));
                }
                assertEquals("NO", result.getString(3));
                assertFalse(result.next());
            }
            if (mysql()) {
                try (ResultSet result =
                        statement.executeQuery(
                                "SELECT COLUMN_NAME, SUB_PART FROM information_schema.statistics"
                                        + " WHERE TABLE_SCHEMA=DATABASE() AND"
                                        + " TABLE_NAME='a2a_task_owners' AND"
                                        + " INDEX_NAME='a2a_task_owner_subject' ORDER BY"
                                        + " SEQ_IN_INDEX")) {
                    assertTrue(result.next());
                    assertEquals("subject_key", result.getString(1));
                    assertNull(result.getObject(2), "Subject identity must not use a prefix index");
                    assertTrue(result.next());
                    assertEquals("task_id", result.getString(1));
                    assertNull(result.getObject(2));
                    assertFalse(result.next());
                }
            } else {
                try (ResultSet result =
                        statement.executeQuery(
                                "SELECT indexdef FROM pg_indexes WHERE schemaname='public' AND"
                                        + " tablename='a2a_task_owners' AND"
                                        + " indexname='a2a_task_owner_subject'")) {
                    assertTrue(result.next());
                    assertTrue(
                            result.getString(1).contains("(subject_key, task_id)"),
                            result.getString(1));
                    assertFalse(result.next());
                }
            }
            System.out.println(
                    "Authorization database: "
                            + connection.getMetaData().getDatabaseProductName()
                            + " "
                            + connection.getMetaData().getDatabaseProductVersion());
        }
    }

    @Test
    void cdiRecoveryUsesNewSnapshotAndLeavesRepeatableReadCallerCommittable() throws Exception {
        staleCallerSnapshot(provider, "cdi");
    }

    @Test
    void programmaticRecoveryUsesNewSnapshotAndLeavesRepeatableReadCallerCommittable()
            throws Exception {
        JpaTaskAuthorizationProvider programmatic =
                new JpaTaskAuthorizationProvider(
                        JpaTaskOwnershipRepository.create(factory, transactionManager));
        staleCallerSnapshot(programmatic, "programmatic");
    }

    private void staleCallerSnapshot(JpaTaskAuthorizationProvider testedProvider, String suffix)
            throws Exception {
        String taskId = "race-" + suffix;
        String markerId = "marker-" + suffix;
        CountDownLatch snapshotEstablished = new CountDownLatch(1);
        CountDownLatch winnerCommitted = new CountDownLatch(1);
        ExecutorService worker = Executors.newSingleThreadExecutor();
        Future<?> winner =
                worker.submit(
                        () -> {
                            await(snapshotEstablished);
                            testedProvider.recordOwnership(
                                    context("winner"), taskId, TaskOperation.MESSAGE_SEND);
                            winnerCommitted.countDown();
                        });
        transactionManager.begin();
        try (EntityManager caller = factory.createEntityManager()) {
            caller.joinTransaction();
            caller.unwrap(Session.class)
                    .doWork(
                            connection -> {
                                connection.setTransactionIsolation(
                                        Connection.TRANSACTION_REPEATABLE_READ);
                                assertEquals(
                                        Connection.TRANSACTION_REPEATABLE_READ,
                                        connection.getTransactionIsolation());
                            });
            assertNull(owner(caller, taskId)); // Establish A's repeatable-read snapshot.
            caller.persist(JpaTask.createFromTask(task(markerId)));
            caller.flush();
            snapshotEstablished.countDown();
            await(winnerCommitted);
            winner.get(30, TimeUnit.SECONDS);
            caller.clear();
            assertNull(
                    owner(caller, taskId),
                    "A must retain an actually stale snapshot after B commits");
            assertEquals(Status.STATUS_ACTIVE, transactionManager.getStatus());
            // The REQUIRES_NEW insert loses to B. Failed insertion must roll back before
            // confirmation; neither failed insert context nor A's stale context can confirm B.
            testedProvider.recordOwnership(context("loser"), taskId, TaskOperation.MESSAGE_SEND);
            assertEquals(Status.STATUS_ACTIVE, transactionManager.getStatus());
            caller.unwrap(Session.class)
                    .doWork(
                            connection ->
                                    assertEquals(
                                            Connection.TRANSACTION_REPEATABLE_READ,
                                            connection.getTransactionIsolation()));
            caller.clear();
            assertNull(owner(caller, taskId), "Recovery must not replace or alter A's snapshot");
            transactionManager.commit();
        } finally {
            if (transactionManager.getTransaction() != null) {
                transactionManager.rollback();
            }
            worker.shutdownNow();
        }
        assertNotNull(
                store.get(markerId),
                "Caller marker must commit despite losing ownership insertion");
        assertArrayEquals("winner".getBytes(StandardCharsets.UTF_8), owner(taskId));
        assertFalse(testedProvider.checkRead(context("loser"), taskId, TaskOperation.GET_TASK));
        assertFalse(testedProvider.checkWrite(context("loser"), taskId, TaskOperation.CANCEL_TASK));
        System.out.println(
                "Ownership stale snapshot: "
                        + suffix
                        + ", caller isolation=REPEATABLE_READ, marker committed");
    }

    @Test
    void ordinaryConcurrentRecordingPreservesOneOwner() throws Exception {
        int expectedIsolation =
                mysql()
                        ? Connection.TRANSACTION_REPEATABLE_READ
                        : Connection.TRANSACTION_READ_COMMITTED;
        try (Connection connection = dataSource.getConnection()) {
            assertEquals(
                    expectedIsolation,
                    connection.getTransactionIsolation(),
                    "Verify real database default isolation");
        }
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<?> first =
                    workers.submit(() -> concurrentRecord("one", ready, start, expectedIsolation));
            Future<?> second =
                    workers.submit(() -> concurrentRecord("two", ready, start, expectedIsolation));
            await(ready);
            start.countDown();
            first.get(30, TimeUnit.SECONDS);
            second.get(30, TimeUnit.SECONDS);
        } finally {
            workers.shutdownNow();
        }
        boolean firstOwner =
                provider.checkRead(context("one"), "concurrent", TaskOperation.GET_TASK);
        boolean secondOwner =
                provider.checkRead(context("two"), "concurrent", TaskOperation.GET_TASK);
        assertTrue(firstOwner ^ secondOwner);
    }

    private void concurrentRecord(
            String subject, CountDownLatch ready, CountDownLatch start, int isolation) {
        try {
            transactionManager.begin();
            try (EntityManager caller = factory.createEntityManager()) {
                caller.joinTransaction();
                caller.unwrap(Session.class)
                        .doWork(
                                connection ->
                                        assertEquals(
                                                isolation, connection.getTransactionIsolation()));
                ready.countDown();
                await(start);
                provider.recordOwnership(
                        context(subject), "concurrent", TaskOperation.MESSAGE_SEND);
                assertEquals(Status.STATUS_ACTIVE, transactionManager.getStatus());
                transactionManager.commit();
            } finally {
                if (transactionManager.getTransaction() != null) {
                    transactionManager.rollback();
                }
            }
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    protected void textColumnNegativeControl() throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            try (ResultSet result =
                    statement.executeQuery(
                            "SELECT DEFAULT_COLLATION_NAME FROM information_schema.schemata "
                                    + "WHERE SCHEMA_NAME=DATABASE()")) {
                assertTrue(result.next());
                assertEquals("utf8mb4_0900_ai_ci", result.getString(1));
            }
            statement.execute(
                    "CREATE TEMPORARY TABLE a2a_text_owner_mutation (task_id VARCHAR(255) PRIMARY"
                        + " KEY, subject_key VARCHAR(1024) NOT NULL) CHARACTER SET utf8mb4 COLLATE"
                        + " utf8mb4_0900_ai_ci");
            try {
                statement.execute(
                        "INSERT INTO a2a_text_owner_mutation VALUES"
                                + " ('case-1','Alice'),('case-2','alice'),"
                                + "('accent-1','Jose'),('accent-2','José')");
                for (String subject : List.of("Alice", "Jose")) {
                    try (var query =
                            connection.prepareStatement(
                                    "SELECT COUNT(*) FROM a2a_text_owner_mutation WHERE"
                                            + " subject_key=?")) {
                        query.setString(1, subject);
                        try (ResultSet result = query.executeQuery()) {
                            assertTrue(result.next());
                            assertEquals(
                                    2,
                                    result.getInt(1),
                                    "Text mutation must expose overbroad identity equality");
                        }
                    }
                }
            } finally {
                statement.execute("DROP TEMPORARY TABLE a2a_text_owner_mutation");
            }
        }
    }

    private byte[] owner(String id) throws Exception {
        final byte[][] result = new byte[1][];
        inTransaction(em -> result[0] = owner(em, id));
        return result[0];
    }

    private static byte[] owner(EntityManager em, String id) {
        return em.createQuery(
                        "SELECT o.subjectKey FROM JpaTaskOwner o WHERE o.taskId=:id", byte[].class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    private void inTransaction(java.util.function.Consumer<EntityManager> action) throws Exception {
        transactionManager.begin();
        try (EntityManager em = factory.createEntityManager()) {
            em.joinTransaction();
            action.accept(em);
            transactionManager.commit();
        } finally {
            if (transactionManager.getTransaction() != null) {
                transactionManager.rollback();
            }
        }
    }

    private static void assertPages(
            List<String> expected,
            ServerCallContext context,
            BiFunction<ListTasksParams, ServerCallContext, ListTasksResult> listing) {
        List<String> actual = new ArrayList<>();
        String token = null;
        for (int page = 0; page < expected.size(); page++) {
            ListTasksResult result = listing.apply(new ListTasksParams(1, token), context);
            assertEquals(expected.size(), result.totalSize());
            assertEquals(1, result.tasks().size());
            actual.add(result.tasks().get(0).id());
            token = result.nextPageToken();
            if (page < expected.size() - 1) {
                assertNotNull(token);
            } else {
                assertNull(token);
            }
        }
        assertEquals(expected, actual);
    }

    private DefaultRequestHandler handler(TaskAuthorizationProvider authorization) {
        return DefaultRequestHandler.builder()
                .agentExecutor(mock(AgentExecutor.class))
                .taskStore(store)
                .queueManager(mock(QueueManager.class))
                .mainEventBusProcessor(mock(MainEventBusProcessor.class))
                .executor(Runnable::run)
                .eventConsumerExecutor(Runnable::run)
                .authorizationProvider(authorization)
                .build();
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(30, TimeUnit.SECONDS), "Transaction coordination timed out");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
    }

    private static String taskId(int subject, int page) {
        return "identity-" + subject + "-" + page;
    }

    private static Task task(String id) {
        return Task.builder()
                .id(id)
                .contextId("identity-context")
                .status(new TaskStatus(TaskState.TASK_STATE_WORKING, null, TIMESTAMP))
                .build();
    }

    private static ServerCallContext context(String subject) {
        return new ServerCallContext(
                new User() {
                    @Override
                    public boolean isAuthenticated() {
                        return true;
                    }

                    @Override
                    public String getUsername() {
                        return subject;
                    }
                },
                Map.of(),
                Set.of());
    }

    private record UnsupportedCapabilityProvider(TaskAuthorizationProvider delegate)
            implements TaskAuthorizationProvider {
        @Override
        public boolean checkRead(ServerCallContext context, String id, TaskOperation operation) {
            return delegate.checkRead(context, id, operation);
        }

        @Override
        public boolean checkWrite(ServerCallContext context, String id, TaskOperation operation) {
            return delegate.checkWrite(context, id, operation);
        }

        @Override
        public boolean checkCreate(ServerCallContext context, TaskOperation operation) {
            return delegate.checkCreate(context, operation);
        }

        @Override
        public boolean isTaskRecorded(String id) {
            return delegate.isTaskRecorded(id);
        }

        @Override
        public void recordOwnership(ServerCallContext context, String id, TaskOperation operation) {
            delegate.recordOwnership(context, id, operation);
        }
    }
}
