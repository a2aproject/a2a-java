package io.a2a.server.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TaskStoreException}.
 * <p>
 * Tests the base exception class for TaskStore persistence layer failures,
 * verifying all constructor variants and field behavior.
 */
class TaskStoreExceptionTest extends AbstractTaskStoreExceptionTest<TaskStoreException> {

    @Override
    protected TaskStoreException createException(String taskId, String message) {
        return new TaskStoreException(taskId, message);
    }

    @Override
    protected TaskStoreException createException(String taskId, String message, Throwable cause) {
        return new TaskStoreException(taskId, message, cause);
    }

    // ========== Constructor Tests ==========

    @Test
    void testConstructor_noArgs() {
        TaskStoreException exception = new TaskStoreException();
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
        assertNull(exception.getTaskId());
    }

    @Test
    void testConstructor_messageOnly() {
        TaskStoreException exception = new TaskStoreException("Failed to persist task");
        assertEquals("Failed to persist task", exception.getMessage());
        assertNull(exception.getCause());
        assertNull(exception.getTaskId());
    }

    @Test
    void testConstructor_causeOnly() {
        RuntimeException cause = new RuntimeException("Database error");
        TaskStoreException exception = new TaskStoreException(cause);

        assertNotNull(exception.getMessage());
        // Exception message should contain cause class name
        assert exception.getMessage().contains("RuntimeException");
        assertSame(cause, exception.getCause());
        assertNull(exception.getTaskId());
    }

    @Test
    void testConstructor_messageAndCause() {
        RuntimeException cause = new RuntimeException("Database error");
        TaskStoreException exception = new TaskStoreException("Persistence failed", cause);

        assertEquals("Persistence failed", exception.getMessage());
        assertSame(cause, exception.getCause());
        assertNull(exception.getTaskId());
    }

    @Test
    void testConstructor_taskIdAndMessage() {
        TaskStoreException exception = new TaskStoreException("task-123", "Failed to save");

        assertEquals("Failed to save", exception.getMessage());
        assertEquals("task-123", exception.getTaskId());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructor_taskIdMessageAndCause() {
        RuntimeException cause = new RuntimeException("Connection timeout");
        TaskStoreException exception = new TaskStoreException("task-456", "Save operation failed", cause);

        assertEquals("Save operation failed", exception.getMessage());
        assertEquals("task-456", exception.getTaskId());
        assertSame(cause, exception.getCause());
    }

    // ========== Task ID Edge Cases ==========

    @Test
    void testTaskId_uuid() {
        String uuid = "550e8400-e29b-41d4-a716-446655440000";
        TaskStoreException exception = new TaskStoreException(uuid, "Test message");
        assertEquals(uuid, exception.getTaskId());
    }

    @Test
    void testTaskId_numericString() {
        TaskStoreException exception = new TaskStoreException("12345", "Test message");
        assertEquals("12345", exception.getTaskId());
    }

    @Test
    void testTaskId_specialCharacters() {
        String taskId = "task-123_v2.0";
        TaskStoreException exception = new TaskStoreException(taskId, "Test message");
        assertEquals(taskId, exception.getTaskId());
    }

    // ========== Inheritance Verification ==========

    @Test
    void testInheritance_extendsA2AServerException() {
        TaskStoreException exception = new TaskStoreException("test", "Test message");
        assertNotNull(exception);
        // TaskStoreException extends A2AServerException (verified by compilation)
    }

    @Test
    void testInheritance_isException() {
        TaskStoreException exception = new TaskStoreException("test", "Test message");
        Exception e = exception;
        assertNotNull(e);
    }

    // ========== Message Quality Tests ==========

    @Test
    void testMessage_descriptive() {
        TaskStoreException exception = new TaskStoreException(
                "task-123",
                "Failed to persist task to database: connection timeout after 30s");

        assertNotNull(exception.getMessage());
        assertMessageContains(exception, "Failed to persist");
        assertMessageContains(exception, "database");
        assertMessageContains(exception, "connection timeout");
    }

    @Test
    void testMessage_actionable() {
        TaskStoreException exception = new TaskStoreException(
                "task-123",
                "Task not found in store. Verify taskId and retry operation.");

        assertNotNull(exception.getMessage());
        assertMessageContains(exception, "Task not found");
        assertMessageContains(exception, "Verify taskId");
    }

    // ========== Cause Chain Preservation ==========

    @Test
    void testCausePreservation_multipleWrapping() {
        RuntimeException rootCause = new RuntimeException("Disk full");
        IllegalStateException level1 = new IllegalStateException("Write failed", rootCause);
        IllegalArgumentException level2 = new IllegalArgumentException("Validation failed", level1);
        TaskStoreException exception = new TaskStoreException("task-789", "Complete failure", level2);

        // Verify full chain
        assertEquals("Complete failure", exception.getMessage());
        assertEquals("task-789", exception.getTaskId());
        assertSame(level2, exception.getCause());
        assertSame(level1, exception.getCause().getCause());
        assertSame(rootCause, exception.getCause().getCause().getCause());
    }

    // ========== Null Safety Tests ==========

    @Test
    void testNullSafety_nullMessage() {
        TaskStoreException exception = new TaskStoreException("task-123", (String) null);
        assertEquals("task-123", exception.getTaskId());
        assertNull(exception.getMessage());
    }

    @Test
    void testNullSafety_nullCause() {
        TaskStoreException exception = new TaskStoreException("task-123", "Message", (Throwable) null);
        assertEquals("task-123", exception.getTaskId());
        assertEquals("Message", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testNullSafety_allNulls() {
        TaskStoreException exception = new TaskStoreException(null, (String) null, (Throwable) null);
        assertNull(exception.getTaskId());
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }
}
