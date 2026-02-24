package io.a2a.server.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TaskPersistenceException}.
 * <p>
 * Tests the exception class for database/storage system failures,
 * with special focus on the {@code isTransient} flag for retry logic.
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

    // ========== Constructor Tests - Default Non-Transient ==========

    @Test
    void testConstructor_noArgs_defaultsToNonTransient() {
        TaskPersistenceException exception = new TaskPersistenceException();
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
        assertNull(exception.getTaskId());
        assertFalse(exception.isTransient(), "Default should be non-transient");
    }

    @Test
    void testConstructor_messageOnly_defaultsToNonTransient() {
        TaskPersistenceException exception = new TaskPersistenceException("Database error");
        assertEquals("Database error", exception.getMessage());
        assertNull(exception.getCause());
        assertNull(exception.getTaskId());
        assertFalse(exception.isTransient(), "Default should be non-transient");
    }

    @Test
    void testConstructor_causeOnly_defaultsToNonTransient() {
        RuntimeException cause = new RuntimeException("Connection failed");
        TaskPersistenceException exception = new TaskPersistenceException(cause);

        assertNotNull(exception.getMessage());
        assertSame(cause, exception.getCause());
        assertNull(exception.getTaskId());
        assertFalse(exception.isTransient(), "Default should be non-transient");
    }

    @Test
    void testConstructor_messageAndCause_defaultsToNonTransient() {
        RuntimeException cause = new RuntimeException("Timeout");
        TaskPersistenceException exception = new TaskPersistenceException("Operation failed", cause);

        assertEquals("Operation failed", exception.getMessage());
        assertSame(cause, exception.getCause());
        assertNull(exception.getTaskId());
        assertFalse(exception.isTransient(), "Default should be non-transient");
    }

    @Test
    void testConstructor_taskIdAndMessage_defaultsToNonTransient() {
        TaskPersistenceException exception = new TaskPersistenceException("task-123", "Save failed");

        assertEquals("Save failed", exception.getMessage());
        assertEquals("task-123", exception.getTaskId());
        assertNull(exception.getCause());
        assertFalse(exception.isTransient(), "Default should be non-transient");
    }

    @Test
    void testConstructor_taskIdMessageAndCause_defaultsToNonTransient() {
        RuntimeException cause = new RuntimeException("Disk error");
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-456", "Persistence failed", cause);

        assertEquals("Persistence failed", exception.getMessage());
        assertEquals("task-456", exception.getTaskId());
        assertSame(cause, exception.getCause());
        assertFalse(exception.isTransient(), "Default should be non-transient");
    }

    // ========== Constructor Tests - Explicit Transient Flag ==========

    @Test
    void testConstructor_messageWithTransientTrue() {
        TaskPersistenceException exception = new TaskPersistenceException("Connection timeout", true);

        assertEquals("Connection timeout", exception.getMessage());
        assertTrue(exception.isTransient(), "Should be transient when explicitly set");
    }

    @Test
    void testConstructor_messageWithTransientFalse() {
        TaskPersistenceException exception = new TaskPersistenceException("Disk full", false);

        assertEquals("Disk full", exception.getMessage());
        assertFalse(exception.isTransient(), "Should be non-transient when explicitly set");
    }

    @Test
    void testConstructor_messageAndCauseWithTransientTrue() {
        RuntimeException cause = new RuntimeException("Network partition");
        TaskPersistenceException exception = new TaskPersistenceException(
                "Database unreachable", cause, true);

        assertEquals("Database unreachable", exception.getMessage());
        assertSame(cause, exception.getCause());
        assertTrue(exception.isTransient(), "Should be transient");
    }

    @Test
    void testConstructor_messageAndCauseWithTransientFalse() {
        RuntimeException cause = new RuntimeException("Constraint violation");
        TaskPersistenceException exception = new TaskPersistenceException(
                "Duplicate key error", cause, false);

        assertEquals("Duplicate key error", exception.getMessage());
        assertSame(cause, exception.getCause());
        assertFalse(exception.isTransient(), "Should be non-transient");
    }

    @Test
    void testConstructor_taskIdMessageWithTransientTrue() {
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-123", "Lock timeout", true);

        assertEquals("task-123", exception.getTaskId());
        assertEquals("Lock timeout", exception.getMessage());
        assertTrue(exception.isTransient(), "Should be transient");
    }

    @Test
    void testConstructor_taskIdMessageWithTransientFalse() {
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-456", "Storage quota exceeded", false);

        assertEquals("task-456", exception.getTaskId());
        assertEquals("Storage quota exceeded", exception.getMessage());
        assertFalse(exception.isTransient(), "Should be non-transient");
    }

    @Test
    void testConstructor_fullArgsWithTransientTrue() {
        RuntimeException cause = new RuntimeException("Deadlock detected");
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-789", "Transaction rolled back", cause, true);

        assertEquals("task-789", exception.getTaskId());
        assertEquals("Transaction rolled back", exception.getMessage());
        assertSame(cause, exception.getCause());
        assertTrue(exception.isTransient(), "Should be transient");
    }

    @Test
    void testConstructor_fullArgsWithTransientFalse() {
        RuntimeException cause = new RuntimeException("Permission denied");
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-abc", "Access control violation", cause, false);

        assertEquals("task-abc", exception.getTaskId());
        assertEquals("Access control violation", exception.getMessage());
        assertSame(cause, exception.getCause());
        assertFalse(exception.isTransient(), "Should be non-transient");
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
        TaskPersistenceException exception = new TaskPersistenceException("task-xyz", "Test message", true);
        // Should be accessible via TaskStoreException parent class
        assertEquals("task-xyz", exception.getTaskId());
    }

    // ========== Transient Flag Semantics Tests ==========

    @Test
    void testTransientFlag_indicatesRetryability() {
        TaskPersistenceException transientError = new TaskPersistenceException(
                "task-123", "Connection timeout", true);
        TaskPersistenceException permanentError = new TaskPersistenceException(
                "task-456", "Disk full", false);

        assertTrue(transientError.isTransient(), "Timeout errors should be retryable");
        assertFalse(permanentError.isTransient(), "Disk full is not retryable");
    }

    // ========== Real-World Scenario Tests - Transient Failures ==========

    @Test
    void testScenario_transient_connectionTimeout() {
        RuntimeException timeout = new RuntimeException("Connection timeout after 30s");
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-123", "Database connection timeout", timeout, true);

        assertFullContext(exception, "task-123", timeout);
        assertTrue(exception.isTransient(), "Connection timeouts are transient");
        assertMessageContains(exception, "timeout");
    }

    @Test
    void testScenario_transient_deadlock() {
        RuntimeException deadlock = new RuntimeException("Deadlock detected");
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-456", "Transaction deadlock, retry recommended", deadlock, true);

        assertTrue(exception.isTransient(), "Deadlocks are transient");
        assertMessageContains(exception, "deadlock");
        assertMessageContains(exception, "retry");
    }

    @Test
    void testScenario_transient_lockTimeout() {
        RuntimeException lockTimeout = new RuntimeException("Lock wait timeout exceeded");
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-789", "Failed to acquire row lock", lockTimeout, true);

        assertTrue(exception.isTransient(), "Lock timeouts are transient");
        assertMessageContains(exception, "lock");
    }

    @Test
    void testScenario_transient_networkPartition() {
        RuntimeException networkError = new RuntimeException("Network unreachable");
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-abc", "Database host unreachable due to network partition", networkError, true);

        assertTrue(exception.isTransient(), "Network errors are transient");
        assertMessageContains(exception, "network");
    }

    @Test
    void testScenario_transient_poolExhausted() {
        RuntimeException poolError = new RuntimeException("Connection pool exhausted");
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-def", "No database connections available", poolError, true);

        assertTrue(exception.isTransient(), "Pool exhaustion is transient");
        assertMessageContains(exception, "connections available");
    }

    // ========== Real-World Scenario Tests - Non-Transient Failures ==========

    @Test
    void testScenario_nonTransient_diskFull() {
        RuntimeException diskError = new RuntimeException("No space left on device");
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-123", "Cannot write task: disk full", diskError, false);

        assertFalse(exception.isTransient(), "Disk full is not transient");
        assertMessageContains(exception, "disk full");
    }

    @Test
    void testScenario_nonTransient_uniqueConstraint() {
        RuntimeException constraintError = new RuntimeException("Duplicate entry for key 'PRIMARY'");
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-456", "Task ID already exists", constraintError, false);

        assertFalse(exception.isTransient(), "Unique constraint violations are not transient");
        assertMessageContains(exception, "already exists");
    }

    @Test
    void testScenario_nonTransient_foreignKeyViolation() {
        RuntimeException fkError = new RuntimeException("Cannot add or update child row");
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-789", "Foreign key constraint violation", fkError, false);

        assertFalse(exception.isTransient(), "Foreign key violations are not transient");
        assertMessageContains(exception, "constraint");
    }

    @Test
    void testScenario_nonTransient_permissionDenied() {
        RuntimeException permError = new RuntimeException("Access denied for user 'app'");
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-abc", "Insufficient database permissions", permError, false);

        assertFalse(exception.isTransient(), "Permission errors are not transient");
        assertMessageContains(exception, "permission");
    }

    @Test
    void testScenario_nonTransient_schemaIncompatibility() {
        RuntimeException schemaError = new RuntimeException("Column 'new_field' does not exist");
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-def", "Database schema incompatible with application version", schemaError, false);

        assertFalse(exception.isTransient(), "Schema incompatibilities are not transient");
        assertMessageContains(exception, "schema");
    }

    @Test
    void testScenario_nonTransient_quotaExceeded() {
        RuntimeException quotaError = new RuntimeException("Storage quota exceeded");
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-ghi", "Database storage limit reached", quotaError, false);

        assertFalse(exception.isTransient(), "Quota violations are not transient");
        assertMessageContains(exception, "storage limit");
    }

    // ========== Message Quality Tests - Persistence Context ==========

    @Test
    void testMessage_connectionError() {
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-123",
                "Failed to connect to database at jdbc:postgresql://localhost:5432/a2a",
                true);

        assertMessageContains(exception, "connect to database");
        assertMessageContains(exception, "jdbc:postgresql");
    }

    @Test
    void testMessage_transactionRollback() {
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-123",
                "Transaction rolled back due to deadlock. Retry operation.",
                true);

        assertMessageContains(exception, "Transaction rolled back");
        assertMessageContains(exception, "Retry operation");
    }

    @Test
    void testMessage_constraintViolation() {
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-123",
                "Unique constraint violation: task_id 'task-123' already exists in table 'tasks'",
                false);

        assertMessageContains(exception, "Unique constraint");
        assertMessageContains(exception, "already exists");
    }

    // ========== Message Actionability Tests ==========

    @Test
    void testMessageActionable_transientWithRetryGuidance() {
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-123",
                "Connection timeout. Retry with exponential backoff (attempt 1 of 3).",
                true);

        assertMessageContains(exception, "Retry with exponential backoff");
        assertMessageContains(exception, "attempt 1 of 3");
    }

    @Test
    void testMessageActionable_nonTransientWithResolution() {
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-123",
                "Disk full. Free up space or increase storage capacity to continue.",
                false);

        assertMessageContains(exception, "Free up space");
        assertMessageContains(exception, "increase storage");
    }

    // ========== Cause Chain for Retry Logic ==========

    @Test
    void testCauseChain_transientErrorWithContext() {
        RuntimeException sqlError = new RuntimeException("SQL state: 08006 - Connection failure");
        RuntimeException jdbcError = new RuntimeException("JDBC connection error", sqlError);
        TaskPersistenceException exception = new TaskPersistenceException(
                "task-xyz", "Database operation failed", jdbcError, true);

        assertTrue(exception.isTransient());
        assertEquals("task-xyz", exception.getTaskId());
        assertSame(jdbcError, exception.getCause());
        assertSame(sqlError, exception.getCause().getCause());
    }

    // ========== Null Safety ==========

    @Test
    void testNullSafety_nullTaskIdWithTransient() {
        TaskPersistenceException exception = new TaskPersistenceException(
                null, "Generic database error", true);

        assertNull(exception.getTaskId());
        assertTrue(exception.isTransient());
        assertEquals("Generic database error", exception.getMessage());
    }

    @Test
    void testNullSafety_allNullsWithTransient() {
        TaskPersistenceException exception = new TaskPersistenceException(
                null, (String) null, (Throwable) null, true);

        assertNull(exception.getTaskId());
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
        assertTrue(exception.isTransient());
    }
}
