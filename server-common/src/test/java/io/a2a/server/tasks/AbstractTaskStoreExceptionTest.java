package io.a2a.server.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * Base test class for TaskStore exception validation.
 * <p>
 * Provides reusable test patterns for exception construction, field verification,
 * and message formatting. Subclasses must implement {@link #createException} methods
 * to test specific exception types.
 * <p>
 * This class is designed to be extended by implementation tests (e.g., InMemoryTaskStore tests)
 * to ensure consistent exception behavior across all TaskStore implementations.
 *
 * @param <T> the exception type being tested (must extend TaskStoreException)
 */
public abstract class AbstractTaskStoreExceptionTest<T extends TaskStoreException> {

    /**
     * Creates an exception with a message and taskId.
     * Used for testing basic exception construction with task context.
     *
     * @param taskId the task identifier
     * @param message the exception message
     * @return the constructed exception
     */
    protected abstract T createException(String taskId, String message);

    /**
     * Creates an exception with a taskId, message, and cause.
     * Used for testing exception chaining with task context.
     *
     * @param taskId the task identifier
     * @param message the exception message
     * @param cause the underlying cause
     * @return the constructed exception
     */
    protected abstract T createException(String taskId, String message, Throwable cause);

    // ========== Task ID Field Tests ==========

    @Test
    void testTaskIdField_withTaskId() {
        T exception = createException("task-123", "Test message");
        assertEquals("task-123", exception.getTaskId());
    }

    @Test
    void testTaskIdField_nullTaskId() {
        T exception = createException(null, "Test message");
        assertNull(exception.getTaskId());
    }

    @Test
    void testTaskIdField_emptyTaskId() {
        T exception = createException("", "Test message");
        assertEquals("", exception.getTaskId());
    }

    // ========== Message Field Tests ==========

    @Test
    void testMessageField_nonNull() {
        T exception = createException("task-123", "Failed to save task");
        assertNotNull(exception.getMessage());
        assertEquals("Failed to save task", exception.getMessage());
    }

    @Test
    void testMessageField_withContext() {
        T exception = createException("task-123", "Database connection timeout");
        assertNotNull(exception.getMessage());
        assertEquals("Database connection timeout", exception.getMessage());
    }

    // ========== Cause Chain Tests ==========

    @Test
    void testCauseChain_withCause() {
        RuntimeException cause = new RuntimeException("Root cause");
        T exception = createException("task-123", "Wrapper message", cause);

        assertNotNull(exception.getCause());
        assertSame(cause, exception.getCause());
        assertEquals("Root cause", exception.getCause().getMessage());
    }

    @Test
    void testCauseChain_multipleLevels() {
        RuntimeException rootCause = new RuntimeException("Database error");
        IllegalStateException intermediateCause = new IllegalStateException("Transaction failed", rootCause);
        T exception = createException("task-123", "Save failed", intermediateCause);

        assertNotNull(exception.getCause());
        assertSame(intermediateCause, exception.getCause());
        assertNotNull(exception.getCause().getCause());
        assertSame(rootCause, exception.getCause().getCause());
    }

    // ========== Exception Inheritance Tests ==========

    @Test
    void testInheritance_isTaskStoreException() {
        T exception = createException("task-123", "Test message");
        assertNotNull(exception);
        // Verified by generic type constraint: T extends TaskStoreException
    }

    @Test
    void testInheritance_isThrowable() {
        T exception = createException("task-123", "Test message");
        Throwable throwable = exception;
        assertNotNull(throwable);
    }

    // ========== Message Clarity Tests ==========

    /**
     * Verifies that exception messages are clear and actionable.
     * Subclasses should override this to test domain-specific message patterns.
     */
    @Test
    void testMessageClarity_basicPattern() {
        T exception = createException("task-123", "Operation failed");
        String message = exception.getMessage();

        assertNotNull(message);
        // Message should not be empty
        assert !message.trim().isEmpty() : "Exception message should not be empty";
        // Message should not be too short (less than 5 characters is typically not helpful)
        assert message.length() >= 5 : "Exception message should be descriptive";
    }

    // ========== Helper Assertions for Subclasses ==========

    /**
     * Asserts that an exception message contains expected context information.
     * Useful for implementation tests to verify TaskStore-specific message patterns.
     *
     * @param exception the exception to check
     * @param expectedSubstring the expected substring in the message
     */
    protected void assertMessageContains(T exception, String expectedSubstring) {
        assertNotNull(exception.getMessage());
        assert exception.getMessage().contains(expectedSubstring)
                : String.format("Expected message to contain '%s' but was: %s",
                        expectedSubstring, exception.getMessage());
    }

    /**
     * Asserts that an exception has both taskId and cause properly set.
     * Useful for implementation tests to verify complete exception context.
     *
     * @param exception the exception to check
     * @param expectedTaskId the expected task ID
     * @param expectedCause the expected cause
     */
    protected void assertFullContext(T exception, String expectedTaskId, Throwable expectedCause) {
        assertEquals(expectedTaskId, exception.getTaskId());
        assertSame(expectedCause, exception.getCause());
        assertNotNull(exception.getMessage());
    }
}
