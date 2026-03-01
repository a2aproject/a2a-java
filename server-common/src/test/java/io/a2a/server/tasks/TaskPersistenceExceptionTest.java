package io.a2a.server.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TaskPersistenceException}.
 * <p>
 * Tests the exception class for database/storage system failures.
 */
class TaskPersistenceExceptionTest extends AbstractTaskStoreExceptionTest<TaskPersistenceException> {

    @Override
    protected TaskPersistenceException createException(String taskId, String message) {
        return new TaskPersistenceException(taskId, message);
    }

    @Override
    protected TaskPersistenceException createException(String taskId, String message, Throwable cause) {
        return new TaskPersistenceException(taskId, message, cause);
    }

    // ========== Constructor Tests ==========

    @Test
    void testConstructor_noArgs() {
        TaskPersistenceException exception = new TaskPersistenceException();
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
        assertNull(exception.getTaskId());
    }

    @Test
    void testConstructor_messageOnly() {
        TaskPersistenceException exception = new TaskPersistenceException("Database error");
        assertEquals("Database error", exception.getMessage());
        assertNull(exception.getCause());
        assertNull(exception.getTaskId());
    }

    @Test
    void testConstructor_causeOnly() {
        RuntimeException cause = new RuntimeException("Connection failed");
        TaskPersistenceException exception = new TaskPersistenceException(cause);

        assertNotNull(exception.getMessage());
        assertSame(cause, exception.getCause());
        assertNull(exception.getTaskId());
    }

    @Test
    void testConstructor_messageAndCause() {
        RuntimeException cause = new RuntimeException("Timeout");
        TaskPersistenceException exception = new TaskPersistenceException("Operation failed", cause);

        assertEquals("Operation failed", exception.getMessage());
        assertSame(cause, exception.getCause());
        assertNull(exception.getTaskId());
    }

    @Test
    void testConstructor_taskIdAndMessage() {
        TaskPersistenceException exception = new TaskPersistenceException("task-123", "Save failed");

        assertEquals("Save failed", exception.getMessage());
        assertEquals("task-123", exception.getTaskId());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructor_taskIdMessageAndCause() {
        RuntimeException cause = new RuntimeException("Disk error");
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-456", "Persistence failed", cause);

        assertEquals("Persistence failed", exception.getMessage());
        assertEquals("task-456", exception.getTaskId());
        assertSame(cause, exception.getCause());
    }

    // ========== Inheritance Verification ==========

    @Test
    void testInheritance_extendsTaskStoreException() {
        TaskPersistenceException exception = new TaskPersistenceException("test", "Test message");
        TaskStoreException baseException = exception;
        assertNotNull(baseException);
    }

    @Test
    void testInheritance_taskIdFieldAccessible() {
        TaskPersistenceException exception = new TaskPersistenceException("task-xyz", "Test message");
        // Should be accessible via TaskStoreException parent class
        assertEquals("task-xyz", exception.getTaskId());
    }

    // ========== Real-World Scenario Tests ==========

    @Test
    void testScenario_connectionTimeout() {
        RuntimeException timeout = new RuntimeException("Connection timeout after 30s");
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-123", "Database connection timeout", timeout);

        assertFullContext(exception, "task-123", timeout);
        assertMessageContains(exception, "timeout");
    }

    @Test
    void testScenario_deadlock() {
        RuntimeException deadlock = new RuntimeException("Deadlock detected");
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-456", "Transaction deadlock", deadlock);

        assertMessageContains(exception, "deadlock");
    }

    @Test
    void testScenario_lockTimeout() {
        RuntimeException lockTimeout = new RuntimeException("Lock wait timeout exceeded");
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-789", "Failed to acquire row lock", lockTimeout);

        assertMessageContains(exception, "lock");
    }

    @Test
    void testScenario_networkPartition() {
        RuntimeException networkError = new RuntimeException("Network unreachable");
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-abc", "Database host unreachable due to network partition", networkError);

        assertMessageContains(exception, "network");
    }

    @Test
    void testScenario_poolExhausted() {
        RuntimeException poolError = new RuntimeException("Connection pool exhausted");
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-def", "No database connections available", poolError);

        assertMessageContains(exception, "connections available");
    }

    @Test
    void testScenario_diskFull() {
        RuntimeException diskError = new RuntimeException("No space left on device");
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-123", "Cannot write task: disk full", diskError);

        assertMessageContains(exception, "disk full");
    }

    @Test
    void testScenario_uniqueConstraint() {
        RuntimeException constraintError = new RuntimeException("Duplicate entry for key 'PRIMARY'");
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-456", "Task ID already exists", constraintError);

        assertMessageContains(exception, "already exists");
    }

    @Test
    void testScenario_foreignKeyViolation() {
        RuntimeException fkError = new RuntimeException("Cannot add or update child row");
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-789", "Foreign key constraint violation", fkError);

        assertMessageContains(exception, "constraint");
    }

    @Test
    void testScenario_permissionDenied() {
        RuntimeException permError = new RuntimeException("Access denied for user 'app'");
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-abc", "Insufficient database permissions", permError);

        assertMessageContains(exception, "permission");
    }

    @Test
    void testScenario_schemaIncompatibility() {
        RuntimeException schemaError = new RuntimeException("Column 'new_field' does not exist");
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-def", "Database schema incompatible with application version", schemaError);

        assertMessageContains(exception, "schema");
    }

    @Test
    void testScenario_quotaExceeded() {
        RuntimeException quotaError = new RuntimeException("Storage quota exceeded");
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-ghi", "Database storage limit reached", quotaError);

        assertMessageContains(exception, "storage limit");
    }

    // ========== Message Quality Tests ==========

    @Test
    void testMessage_connectionError() {
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-123",
                "Failed to connect to database at jdbc:postgresql://localhost:5432/a2a");

        assertMessageContains(exception, "connect to database");
        assertMessageContains(exception, "jdbc:postgresql");
    }

    @Test
    void testMessage_transactionRollback() {
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-123",
                "Transaction rolled back due to deadlock");

        assertMessageContains(exception, "Transaction rolled back");
    }

    @Test
    void testMessage_constraintViolation() {
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-123",
                "Unique constraint violation: task_id 'task-123' already exists in table 'tasks'");

        assertMessageContains(exception, "Unique constraint");
        assertMessageContains(exception, "already exists");
    }

    // ========== Cause Chain Tests ==========

    @Test
    void testCauseChain_withContext() {
        RuntimeException sqlError = new RuntimeException("SQL state: 08006 - Connection failure");
        RuntimeException jdbcError = new RuntimeException("JDBC connection error", sqlError);
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-xyz", "Database operation failed", jdbcError);

        assertEquals("task-xyz", exception.getTaskId());
        assertSame(jdbcError, exception.getCause());
        assertSame(sqlError, exception.getCause().getCause());
    }

    // ========== Null Safety ==========

    @Test
    void testNullSafety_nullTaskId() {
        TaskPersistenceException exception = new TaskPersistenceException(
                null, "Generic database error");

        assertNull(exception.getTaskId());
        assertEquals("Generic database error", exception.getMessage());
    }
}
