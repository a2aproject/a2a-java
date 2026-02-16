package io.a2a.server.agentexecution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendConfiguration;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TextPart;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class RequestContextTest {

    private static MessageSendConfiguration defaultConfiguration() {
        return MessageSendConfiguration.builder()
                .acceptedOutputModes(List.of())
                .blocking(false)
                .build();
    }

    @Test
    public void testInitWithoutParams() {
        RequestContext context = new RequestContext.Builder().build();

        assertNull(context.getMessage());
        assertNotNull(context.getTaskId()); // Generated UUID
        assertNotNull(context.getContextId()); // Generated UUID
        assertNull(context.getTask());
        assertTrue(context.getRelatedTasks().isEmpty());
    }

    @Test
    public void testInitWithParamsNoIds() {
        var mockMessage = Message.builder().role(Message.Role.USER).parts(List.of(new TextPart(""))).build();
        var mockParams = MessageSendParams.builder().message(mockMessage).configuration(defaultConfiguration()).build();

        UUID taskId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID contextId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        try (MockedStatic<UUID> mockedUUID = mockStatic(UUID.class)) {
            mockedUUID.when(UUID::randomUUID)
                    .thenReturn(taskId)
                    .thenReturn(contextId);

            RequestContext context = new RequestContext.Builder()
                    .setParams(mockParams)
                    .build();

            // getMessage() returns a new Message with generated IDs, not the original
            assertNotNull(context.getMessage());
            assertEquals(taskId.toString(), context.getTaskId());
            assertEquals(taskId.toString(), context.getMessage().taskId());
            assertEquals(contextId.toString(), context.getContextId());
            assertEquals(contextId.toString(), context.getMessage().contextId());
        }
    }

    @Test
    public void testInitWithTaskId() {
        String taskId = "task-123";
        var mockMessage = Message.builder().role(Message.Role.USER).parts(List.of(new TextPart(""))).taskId(taskId).build();
        var mockParams = MessageSendParams.builder().message(mockMessage).configuration(defaultConfiguration()).build();

        RequestContext context = new RequestContext.Builder()
                .setParams(mockParams)
                .setTaskId(taskId)
                .build();

        assertEquals(taskId, context.getTaskId());
        assertEquals(taskId, mockParams.message().taskId());
    }

    @Test
    public void testInitWithContextId() {
        String contextId = "context-456";
        var mockMessage = Message.builder().role(Message.Role.USER).parts(List.of(new TextPart(""))).contextId(contextId).build();
        var mockParams = MessageSendParams.builder().message(mockMessage).configuration(defaultConfiguration()).build();

        RequestContext context = new RequestContext.Builder()
                .setParams(mockParams)
                .setContextId(contextId)
                .build();

        assertEquals(contextId, context.getContextId());
        assertEquals(contextId, mockParams.message().contextId());
    }

    @Test
    public void testInitWithBothIds() {
        String taskId = "task-123";
        String contextId = "context-456";
        var mockMessage = Message.builder().role(Message.Role.USER).parts(List.of(new TextPart(""))).taskId(taskId).contextId(contextId).build();
        var mockParams = MessageSendParams.builder().message(mockMessage).configuration(defaultConfiguration()).build();

        RequestContext context = new RequestContext.Builder()
                .setParams(mockParams)
                .setTaskId(taskId)
                .setContextId(contextId)
                .build();

        assertEquals(taskId, context.getTaskId());
        assertEquals(taskId, mockParams.message().taskId());
        assertEquals(contextId, context.getContextId());
        assertEquals(contextId, mockParams.message().contextId());
    }

    @Test
    public void testInitWithTask() {
        var mockMessage = Message.builder().role(Message.Role.USER).parts(List.of(new TextPart(""))).build();
        var mockTask = Task.builder().id("task-123").contextId("context-456").status(new TaskStatus(TaskState.TASK_STATE_COMPLETED)).build();
        var mockParams = MessageSendParams.builder().message(mockMessage).configuration(defaultConfiguration()).build();

        RequestContext context = new RequestContext.Builder()
                .setParams(mockParams)
                .setTask(mockTask)
                .build();

        assertEquals(mockTask, context.getTask());
    }

    @Test
    public void testGetUserInputNoParams() {
        RequestContext context = new RequestContext.Builder().build();
        assertEquals("", context.getUserInput(null));
    }

    @Test
    public void testAttachRelatedTask() {
        var mockTask = Task.builder().id("task-123").contextId("context-456").status(new TaskStatus(TaskState.TASK_STATE_COMPLETED)).build();

        RequestContext context = new RequestContext.Builder().build();
        assertEquals(0, context.getRelatedTasks().size());

        context.attachRelatedTask(mockTask);
        assertEquals(1, context.getRelatedTasks().size());
        assertEquals(mockTask, context.getRelatedTasks().get(0));

        Task anotherTask = mock(Task.class);
        context.attachRelatedTask(anotherTask);
        assertEquals(2, context.getRelatedTasks().size());
        assertEquals(anotherTask, context.getRelatedTasks().get(1));
    }

    @Test
    public void testCheckOrGenerateTaskIdWithExistingTaskId() {
        String existingId = "existing-task-id";
        var mockMessage = Message.builder().role(Message.Role.USER).parts(List.of(new TextPart(""))).taskId(existingId).build();
        var mockParams = MessageSendParams.builder().message(mockMessage).configuration(defaultConfiguration()).build();

        RequestContext context = new RequestContext.Builder()
                .setParams(mockParams)
                .build();

        assertEquals(existingId, context.getTaskId());
        assertEquals(existingId, mockParams.message().taskId());
    }

    @Test
    public void testCheckOrGenerateContextIdWithExistingContextId() {
        String existingId = "existing-context-id";

        var mockMessage = Message.builder().role(Message.Role.USER).parts(List.of(new TextPart(""))).contextId(existingId).build();
        var mockParams = MessageSendParams.builder().message(mockMessage).configuration(defaultConfiguration()).build();

        RequestContext context = new RequestContext.Builder()
                .setParams(mockParams)
                .build();

        assertEquals(existingId, context.getContextId());
        assertEquals(existingId, mockParams.message().contextId());
    }

    @Test
    public void testInitRaisesErrorOnTaskIdMismatch() {
        var mockMessage = Message.builder().role(Message.Role.USER).parts(List.of(new TextPart(""))).taskId("task-123").build();
        var mockParams = MessageSendParams.builder().message(mockMessage).configuration(defaultConfiguration()).build();
        var mockTask = Task.builder().id("task-123").contextId("context-456").status(new TaskStatus(TaskState.TASK_STATE_COMPLETED)).build();

        InvalidParamsError error = assertThrows(InvalidParamsError.class, () ->
                new RequestContext.Builder()
                        .setParams(mockParams)
                        .setTaskId("wrong-task-id")
                        .setTask(mockTask)
                        .build());

        assertTrue(error.getMessage().contains("bad task id"));
    }

    @Test
    public void testInitRaisesErrorOnContextIdMismatch() {
        var mockMessage = Message.builder().role(Message.Role.USER).parts(List.of(new TextPart(""))).taskId("task-123").contextId("context-456").build();
        var mockParams = MessageSendParams.builder().message(mockMessage).configuration(defaultConfiguration()).build();
        var mockTask = Task.builder().id("task-123").contextId("context-456").status(new TaskStatus(TaskState.TASK_STATE_COMPLETED)).build();

        InvalidParamsError error = assertThrows(InvalidParamsError.class, () ->
                new RequestContext.Builder()
                        .setParams(mockParams)
                        .setTaskId(mockTask.id())
                        .setContextId("wrong-context-id")
                        .setTask(mockTask)
                        .build());

        assertTrue(error.getMessage().contains("bad context id"));
    }

    @Test
    public void testWithRelatedTasksProvided() {
        var mockTask = Task.builder().id("task-123").contextId("context-456").status(new TaskStatus(TaskState.TASK_STATE_COMPLETED)).build();

        List<Task> relatedTasks = new ArrayList<>();
        relatedTasks.add(mockTask);
        relatedTasks.add(mock(Task.class));

        RequestContext context = new RequestContext.Builder()
                .setRelatedTasks(relatedTasks)
                .build();

        assertEquals(relatedTasks, context.getRelatedTasks());
        assertEquals(2, context.getRelatedTasks().size());
    }

    @Test
    public void testMessagePropertyWithoutParams() {
        RequestContext context = new RequestContext.Builder().build();
        assertNull(context.getMessage());
    }

    @Test
    public void testMessagePropertyWithParams() {
        var mockMessage = Message.builder().role(Message.Role.USER).parts(List.of(new TextPart(""))).build();
        var mockParams = MessageSendParams.builder().message(mockMessage).configuration(defaultConfiguration()).build();

        RequestContext context = new RequestContext.Builder()
                .setParams(mockParams)
                .build();

        // getMessage() returns a new Message with generated IDs, not the original
        assertNotNull(context.getMessage());
        assertEquals(mockMessage.role(), context.getMessage().role());
        assertEquals(mockMessage.parts(), context.getMessage().parts());
    }

    @Test
    public void testInitWithExistingIdsInMessage() {
        String existingTaskId = "existing-task-id";
        String existingContextId = "existing-context-id";

        var mockMessage = Message.builder().role(Message.Role.USER).parts(List.of(new TextPart("")))
                .taskId(existingTaskId).contextId(existingContextId).build();
        var mockParams = MessageSendParams.builder().message(mockMessage).configuration(defaultConfiguration()).build();

        RequestContext context = new RequestContext.Builder()
                .setParams(mockParams)
                .build();

        assertEquals(existingTaskId, context.getTaskId());
        assertEquals(existingContextId, context.getContextId());
    }

    @Test
    public void testInitWithTaskIdAndExistingTaskIdMatch() {
        var mockMessage = Message.builder().role(Message.Role.USER).parts(List.of(new TextPart(""))).taskId("task-123").contextId("context-456").build();
        var mockParams = MessageSendParams.builder().message(mockMessage).configuration(defaultConfiguration()).build();
        var mockTask = Task.builder().id("task-123").contextId("context-456").status(new TaskStatus(TaskState.TASK_STATE_COMPLETED)).build();

        RequestContext context = new RequestContext.Builder()
                .setParams(mockParams)
                .setTaskId(mockTask.id())
                .setTask(mockTask)
                .build();

        assertEquals(mockTask.id(), context.getTaskId());
        assertEquals(mockTask, context.getTask());
    }

    @Test
    public void testInitWithContextIdAndExistingContextIdMatch() {
        var mockMessage = Message.builder().role(Message.Role.USER).parts(List.of(new TextPart(""))).taskId("task-123").contextId("context-456").build();
        var mockParams = MessageSendParams.builder().message(mockMessage).configuration(defaultConfiguration()).build();
        var mockTask = Task.builder().id("task-123").contextId("context-456").status(new TaskStatus(TaskState.TASK_STATE_COMPLETED)).build();

        RequestContext context = new RequestContext.Builder()
                .setParams(mockParams)
                .setTaskId(mockTask.id())
                .setContextId(mockTask.contextId())
                .setTask(mockTask)
                .build();

        assertEquals(mockTask.contextId(), context.getContextId());
        assertEquals(mockTask, context.getTask());
    }

    @Test
    void testMessageBuilderGeneratesId() {
        var mockMessage = Message.builder().role(Message.Role.USER).parts(List.of(new TextPart(""))).build();
        var mockParams = MessageSendParams.builder().message(mockMessage).configuration(defaultConfiguration()).build();

        RequestContext context = new RequestContext.Builder()
                .setParams(mockParams)
                .build();

        assertNotNull(mockMessage.messageId());
        assertFalse(mockMessage.messageId().isEmpty());
    }

    @Test
    void testMessageBuilderUsesProvidedId() {
        var mockMessage = Message.builder().messageId("123").role(Message.Role.USER).parts(List.of(new TextPart(""))).build();
        var mockParams = MessageSendParams.builder().message(mockMessage).configuration(defaultConfiguration()).build();

        RequestContext context = new RequestContext.Builder()
                .setParams(mockParams)
                .build();

        assertEquals("123", mockMessage.messageId());
    }

    @Test
    public void testBuilderGeneratesIdsWhenNoParams() {
        RequestContext context = new RequestContext.Builder()
                .build();

        assertNotNull(context.getTaskId());
        assertNotNull(context.getContextId());
        assertFalse(context.getTaskId().isEmpty());
        assertFalse(context.getContextId().isEmpty());
    }

    @Test
    public void testBuilderPreservesProvidedIdsWhenNoParams() {
        String providedTaskId = "my-task-id";
        String providedContextId = "my-context-id";

        RequestContext context = new RequestContext.Builder()
                .setTaskId(providedTaskId)
                .setContextId(providedContextId)
                .build();

        assertEquals(providedTaskId, context.getTaskId());
        assertEquals(providedContextId, context.getContextId());
    }

    @Test
    public void testBuilderUpdatesMessageWithBuilderIds() {
        // Regression test for Gemini review: ensure message gets updated when builder provides IDs
        String builderTaskId = "builder-task-id";
        String builderContextId = "builder-context-id";

        // Message has no IDs, but builder provides them
        var mockMessage = Message.builder().role(Message.Role.USER).parts(List.of(new TextPart(""))).build();
        var mockParams = MessageSendParams.builder().message(mockMessage).configuration(defaultConfiguration()).build();

        RequestContext context = new RequestContext.Builder()
                .setParams(mockParams)
                .setTaskId(builderTaskId)
                .setContextId(builderContextId)
                .build();

        // Both context and message should have the builder IDs
        assertEquals(builderTaskId, context.getTaskId());
        assertEquals(builderContextId, context.getContextId());
        assertEquals(builderTaskId, context.getMessage().taskId());  // KEY: message must be updated
        assertEquals(builderContextId, context.getMessage().contextId());  // KEY: message must be updated
    }

    @Test
    public void testMessageIdsTakePrecedenceWhenBothPresent() {
        // When both builder and message provide IDs, they must match (or throw)
        String sharedTaskId = "shared-task-id";
        String sharedContextId = "shared-context-id";

        var mockMessage = Message.builder()
                .role(Message.Role.USER)
                .parts(List.of(new TextPart("")))
                .taskId(sharedTaskId)
                .contextId(sharedContextId)
                .build();
        var mockParams = MessageSendParams.builder().message(mockMessage).configuration(defaultConfiguration()).build();

        RequestContext context = new RequestContext.Builder()
                .setParams(mockParams)
                .setTaskId(sharedTaskId)  // Same as message
                .setContextId(sharedContextId)  // Same as message
                .build();

        assertEquals(sharedTaskId, context.getTaskId());
        assertEquals(sharedContextId, context.getContextId());
        assertEquals(sharedTaskId, context.getMessage().taskId());
        assertEquals(sharedContextId, context.getMessage().contextId());
    }

    @Test
    public void testBuilderPreservesTenantWhenUpdatingMessage() {
        // Regression test for Gemini review: ensure tenant is preserved when message is updated
        String tenantId = "customer-123";
        String builderTaskId = "builder-task-id";

        var mockMessage = Message.builder().role(Message.Role.USER).parts(List.of(new TextPart(""))).build();
        var mockParams = MessageSendParams.builder()
                .message(mockMessage)
                .configuration(defaultConfiguration())
                .tenant(tenantId)
                .build();

        RequestContext context = new RequestContext.Builder()
                .setParams(mockParams)
                .setTaskId(builderTaskId)  // Forces message update
                .build();

        // Verify the message was updated with builder's task ID
        assertNotNull(context.getMessage());
        assertEquals(builderTaskId, context.getMessage().taskId());

        // KEY: Verify tenant wasn't lost during message update
        assertEquals(tenantId, context.getTenant());
    }
}
