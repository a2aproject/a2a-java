package io.a2a.server.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TaskSerializationException}.
 * <p>
 * Tests the exception class for task serialization/deserialization failures,
 * verifying all constructor variants.
 */
class TaskSerializationExceptionTest extends AbstractTaskStoreExceptionTest<TaskSerializationException> {

    @Override
    protected TaskSerializationException createException(String taskId, String message) {
        return new TaskSerializationException(taskId, message);
    }

    @Override
    protected TaskSerializationException createException(String taskId, String message, Throwable cause) {
        return new TaskSerializationException(taskId, message, cause);
    }

    // ========== Constructor Tests ==========

    @Test
    void testConstructor_noArgs() {
        TaskSerializationException exception = new TaskSerializationException();
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
        assertNull(exception.getTaskId());
    }

    @Test
    void testConstructor_messageOnly() {
        TaskSerializationException exception = new TaskSerializationException("JSON parsing failed");
        assertEquals("JSON parsing failed", exception.getMessage());
        assertNull(exception.getCause());
        assertNull(exception.getTaskId());
    }

    @Test
    void testConstructor_causeOnly() {
        RuntimeException cause = new RuntimeException("Invalid JSON format");
        TaskSerializationException exception = new TaskSerializationException(cause);

        assertNotNull(exception.getMessage());
        // Exception message should contain cause class name
        assert exception.getMessage().contains("RuntimeException");
        assertSame(cause, exception.getCause());
        assertNull(exception.getTaskId());
    }

    @Test
    void testConstructor_messageAndCause() {
        RuntimeException cause = new RuntimeException("Unexpected field type");
        TaskSerializationException exception = new TaskSerializationException("Deserialization failed", cause);

        assertEquals("Deserialization failed", exception.getMessage());
        assertSame(cause, exception.getCause());
        assertNull(exception.getTaskId());
    }

    @Test
    void testConstructor_taskIdAndMessage() {
        TaskSerializationException exception = new TaskSerializationException(
                "task-123", "Failed to serialize task");

        assertEquals("Failed to serialize task", exception.getMessage());
        assertEquals("task-123", exception.getTaskId());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructor_taskIdMessageAndCause() {
        RuntimeException cause = new RuntimeException("Missing required field");
        TaskSerializationException exception = new TaskSerializationException(
                "task-456", "Task deserialization failed", cause);

        assertEquals("Task deserialization failed", exception.getMessage());
        assertEquals("task-456", exception.getTaskId());
        assertSame(cause, exception.getCause());
    }

    // ========== Inheritance Verification ==========

    @Test
    void testInheritance_extendsTaskStoreException() {
        TaskSerializationException exception = new TaskSerializationException("test", "Test message");
        TaskStoreException baseException = exception;
        assertNotNull(baseException);
    }

    @Test
    void testInheritance_taskIdFieldAccessible() {
        TaskSerializationException exception = new TaskSerializationException("task-789", "Test message");
        // Should be accessible via TaskStoreException parent class
        assertEquals("task-789", exception.getTaskId());
    }

    // ========== Message Quality Tests - Serialization Context ==========

    @Test
    void testMessage_jsonParsingError() {
        TaskSerializationException exception = new TaskSerializationException(
                "task-123",
                "Failed to deserialize task: unexpected token at line 42, column 15");

        assertNotNull(exception.getMessage());
        assertMessageContains(exception, "deserialize");
        assertMessageContains(exception, "line 42");
    }

    @Test
    void testMessage_invalidFieldType() {
        TaskSerializationException exception = new TaskSerializationException(
                "task-123",
                "Field 'status' expected enum TaskState, got string 'INVALID'");

        assertNotNull(exception.getMessage());
        assertMessageContains(exception, "Field 'status'");
        assertMessageContains(exception, "expected enum");
    }

    @Test
    void testMessage_missingRequiredField() {
        TaskSerializationException exception = new TaskSerializationException(
                "task-123",
                "Missing required field 'id' during task deserialization");

        assertNotNull(exception.getMessage());
        assertMessageContains(exception, "Missing required field");
        assertMessageContains(exception, "'id'");
    }

    @Test
    void testMessage_schemaVersionMismatch() {
        TaskSerializationException exception = new TaskSerializationException(
                "task-123",
                "Task schema version 2.0 not compatible with current version 1.0");

        assertNotNull(exception.getMessage());
        assertMessageContains(exception, "schema version");
        assertMessageContains(exception, "not compatible");
    }

    // ========== Real-World Scenario Tests ==========

    @Test
    void testScenario_jsonProcessingException() {
        // Simulate Jackson JsonProcessingException wrapping
        RuntimeException jsonError = new RuntimeException(
                "Unrecognized field \"unknownField\" (class Task), not marked as ignorable");
        TaskSerializationException exception = new TaskSerializationException(
                "task-abc", "Failed to deserialize task from JSON", jsonError);

        assertFullContext(exception, "task-abc", jsonError);
        assertMessageContains(exception, "Failed to deserialize");
    }

    @Test
    void testScenario_enumConversionError() {
        RuntimeException enumError = new RuntimeException(
                "Cannot deserialize value of type `TaskState` from String \"INVALID_STATE\"");
        TaskSerializationException exception = new TaskSerializationException(
                "task-def", "Invalid enum value in task JSON", enumError);

        assertFullContext(exception, "task-def", enumError);
        assertMessageContains(exception, "Invalid enum value");
    }

    @Test
    void testScenario_nullValueError() {
        TaskSerializationException exception = new TaskSerializationException(
                "task-ghi",
                "Required field 'taskId' cannot be null during deserialization");

        assertEquals("task-ghi", exception.getTaskId());
        assertMessageContains(exception, "cannot be null");
    }

    // ========== Cause Chain for Debugging ==========

    @Test
    void testCauseChain_multiLevelSerializationError() {
        // Simulate nested serialization error with multiple layers
        RuntimeException rootCause = new RuntimeException("Invalid UTF-8 sequence at byte 1024");
        IllegalArgumentException parseError = new IllegalArgumentException("Cannot parse JSON", rootCause);
        TaskSerializationException exception = new TaskSerializationException(
                "task-xyz", "Task deserialization failed", parseError);

        // Verify full chain for debugging
        assertEquals("task-xyz", exception.getTaskId());
        assertSame(parseError, exception.getCause());
        assertSame(rootCause, exception.getCause().getCause());
        assertMessageContains(exception, "deserialization failed");
    }

    // ========== Edge Cases ==========

    @Test
    void testEdgeCase_emptyJsonError() {
        TaskSerializationException exception = new TaskSerializationException(
                "task-empty", "Cannot deserialize empty JSON string");

        assertEquals("task-empty", exception.getTaskId());
        assertMessageContains(exception, "empty JSON");
    }

    @Test
    void testEdgeCase_circularReferenceError() {
        TaskSerializationException exception = new TaskSerializationException(
                "task-circular",
                "Infinite recursion detected during serialization (StackOverflowError)");

        assertEquals("task-circular", exception.getTaskId());
        assertMessageContains(exception, "Infinite recursion");
    }

    @Test
    void testEdgeCase_characterEncodingError() {
        RuntimeException encodingError = new RuntimeException("Invalid character encoding");
        TaskSerializationException exception = new TaskSerializationException(
                "task-encoding", "Failed to encode task JSON", encodingError);

        assertFullContext(exception, "task-encoding", encodingError);
    }

    // ========== Message Actionability Tests ==========

    @Test
    void testMessageActionable_providesContext() {
        TaskSerializationException exception = new TaskSerializationException(
                "task-123",
                "Failed to deserialize task: field 'createdAt' expects ISO-8601 timestamp, got '2024-13-45'");

        // Message should help developer understand the problem
        assertMessageContains(exception, "field 'createdAt'");
        assertMessageContains(exception, "expects ISO-8601");
        assertMessageContains(exception, "got '2024-13-45'");
    }

    @Test
    void testMessageActionable_suggestsResolution() {
        TaskSerializationException exception = new TaskSerializationException(
                "task-123",
                "Schema version mismatch. Run database migration to update task format to v2.0");

        assertMessageContains(exception, "Schema version");
        assertMessageContains(exception, "Run database migration");
    }

    // ========== Null Safety ==========

    @Test
    void testNullSafety_nullTaskIdWithMessage() {
        TaskSerializationException exception = new TaskSerializationException(null, "Generic serialization error");
        assertNull(exception.getTaskId());
        assertEquals("Generic serialization error", exception.getMessage());
    }

    @Test
    void testNullSafety_nullMessageWithTaskId() {
        TaskSerializationException exception = new TaskSerializationException("task-123", (String) null);
        assertEquals("task-123", exception.getTaskId());
        assertNull(exception.getMessage());
    }
}
