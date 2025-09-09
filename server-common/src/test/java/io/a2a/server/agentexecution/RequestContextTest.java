package io.a2a.server.agentexecution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.Task;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskState;
import io.a2a.spec.TextPart;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RequestContextTest {

    @Test
    public void testInitWithoutParams() {
        RequestContext context = new RequestContext(null, null, null, null, null, null);
        assertNull(context.getMessage());
        assertNull(context.getTaskId());
        assertNull(context.getContextId());
        assertNull(context.getTask());
        assertTrue(context.getRelatedTasks().isEmpty());
    }

    @Test
    public void testInitWithParamsNoIds() {
        var mockMessage = new Message.Builder().role(Message.Role.USER).parts(List.of(new TextPart(""))).build();
        var mockParams = new MessageSendParams.Builder().message(mockMessage).build();

        UUID taskId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID contextId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        try (MockedStatic<UUID> mockedUUID = mockStatic(UUID.class)) {
            mockedUUID.when(UUID::randomUUID)
                    .thenReturn(taskId)
                    .thenReturn(contextId);

            RequestContext context = new RequestContext(mockParams, null, null, null, null, null);

            assertEquals(mockParams.message(), context.getMessage());
            assertEquals(taskId.toString(), context.getTaskId());
            assertEquals(mockParams.message().getTaskId(), taskId.toString());
            assertEquals(contextId.toString(), context.getContextId());
            assertEquals(mockParams.message().getContextId(), contextId.toString());
        }
    }

    @Test
    public void testInitWithTaskId() {
        String taskId = "task-123";
        var mockMessage = new Message.Builder().role(Message.Role.USER).parts(List.of(new TextPart(""))).taskId(taskId).build();
        var mockParams = new MessageSendParams.Builder().message(mockMessage).build();

        RequestContext context = new RequestContext(mockParams, taskId, null, null, null, null);

        assertEquals(taskId, context.getTaskId());
        assertEquals(taskId, mockParams.message().getTaskId());
    }

    @Test
    public void testInitWithContextId() {
        String contextId = "context-456";
        var mockMessage = new Message.Builder().role(Message.Role.USER).parts(List.of(new TextPart(""))).contextId(contextId).build();
        var mockParams = new MessageSendParams.Builder().message(mockMessage).build();
        RequestContext context = new RequestContext(mockParams, null, contextId, null, null, null);

        assertEquals(contextId, context.getContextId());
        assertEquals(contextId, mockParams.message().getContextId());
    }

    @Test
    public void testInitWithBothIds() {
        String taskId = "task-123";
        String contextId = "context-456";
        var mockMessage = new Message.Builder().role(Message.Role.USER).parts(List.of(new TextPart(""))).taskId(taskId).contextId(contextId).build();
        var mockParams = new MessageSendParams.Builder().message(mockMessage).build();
        RequestContext context = new RequestContext(mockParams, taskId, contextId, null, null, null);

        assertEquals(taskId, context.getTaskId());
        assertEquals(taskId, mockParams.message().getTaskId());
        assertEquals(contextId, context.getContextId());
        assertEquals(contextId, mockParams.message().getContextId());
    }

    @Test
    public void testInitWithTask() {
        var mockMessage = new Message.Builder().role(Message.Role.USER).parts(List.of(new TextPart(""))).build();
        var mockTask = new Task.Builder().id("task-123").contextId("context-456").status(new TaskStatus(TaskState.COMPLETED)).build();
        var mockParams = new MessageSendParams.Builder().message(mockMessage).build();

        RequestContext context = new RequestContext(mockParams, null, null, mockTask, null, null);

        assertEquals(mockTask, context.getTask());
    }

    @Test
    public void testGetUserInputNoParams() {
        RequestContext context = new RequestContext(null, null, null, null, null, null);
        assertEquals("", context.getUserInput(null));
    }

    @Test
    public void testAttachRelatedTask() {
        var mockTask = new Task.Builder().id("task-123").contextId("context-456").status(new TaskStatus(TaskState.COMPLETED)).build();

        RequestContext context = new RequestContext(null, null, null, null, null, null);
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
        var mockMessage = new Message.Builder().role(Message.Role.USER).parts(List.of(new TextPart(""))).taskId(existingId).build();
        var mockParams = new MessageSendParams.Builder().message(mockMessage).build();

        RequestContext context = new RequestContext(mockParams, null, null, null, null, null);

        assertEquals(existingId, context.getTaskId());
        assertEquals(existingId, mockParams.message().getTaskId());
    }

    @Test
    public void testCheckOrGenerateContextIdWithExistingContextId() {
        String existingId = "existing-context-id";

        var mockMessage = new Message.Builder().role(Message.Role.USER).parts(List.of(new TextPart(""))).contextId(existingId).build();
        var mockParams = new MessageSendParams.Builder().message(mockMessage).build();

        RequestContext context = new RequestContext(mockParams, null, null, null, null, null);

        assertEquals(existingId, context.getContextId());
        assertEquals(existingId, mockParams.message().getContextId());
    }

    @Test
    public void testInitRaisesErrorOnTaskIdMismatch() {
        var mockMessage = new Message.Builder().role(Message.Role.USER).parts(List.of(new TextPart(""))).taskId("task-123").build();
        var mockParams = new MessageSendParams.Builder().message(mockMessage).build();
        var mockTask = new Task.Builder().id("task-123").contextId("context-456").status(new TaskStatus(TaskState.COMPLETED)).build();

        InvalidParamsError error = assertThrows(InvalidParamsError.class, () ->
                new RequestContext(mockParams, "wrong-task-id", null, mockTask, null, null));

        assertTrue(error.getMessage().contains("bad task id"));
    }

    @Test
    public void testInitRaisesErrorOnContextIdMismatch() {
        var mockMessage = new Message.Builder().role(Message.Role.USER).parts(List.of(new TextPart(""))).taskId("task-123").contextId("context-456").build();
        var mockParams = new MessageSendParams.Builder().message(mockMessage).build();
        var mockTask = new Task.Builder().id("task-123").contextId("context-456").status(new TaskStatus(TaskState.COMPLETED)).build();

        InvalidParamsError error = assertThrows(InvalidParamsError.class, () ->
                new RequestContext(mockParams, mockTask.getId(), "wrong-context-id", mockTask, null, null));

        assertTrue(error.getMessage().contains("bad context id"));
    }

    @Test
    public void testWithRelatedTasksProvided() {
        var mockTask = new Task.Builder().id("task-123").contextId("context-456").status(new TaskStatus(TaskState.COMPLETED)).build();

        List<Task> relatedTasks = new ArrayList<>();
        relatedTasks.add(mockTask);
        relatedTasks.add(mock(Task.class));

        RequestContext context = new RequestContext(null, null, null, null, relatedTasks, null);

        assertEquals(relatedTasks, context.getRelatedTasks());
        assertEquals(2, context.getRelatedTasks().size());
    }

    @Test
    public void testMessagePropertyWithoutParams() {
        RequestContext context = new RequestContext(null, null, null, null, null, null);
        assertNull(context.getMessage());
    }

    @Test
    public void testMessagePropertyWithParams() {
        var mockMessage = new Message.Builder().role(Message.Role.USER).parts(List.of(new TextPart(""))).build();
        var mockParams = new MessageSendParams.Builder().message(mockMessage).build();

        RequestContext context = new RequestContext(mockParams, null, null, null, null, null);
        assertEquals(mockParams.message(), context.getMessage());
    }

    @Test
    public void testInitWithExistingIdsInMessage() {
        String existingTaskId = "existing-task-id";
        String existingContextId = "existing-context-id";

        var mockMessage = new Message.Builder().role(Message.Role.USER).parts(List.of(new TextPart("")))
                .taskId(existingTaskId).contextId(existingContextId).build();
        var mockParams = new MessageSendParams.Builder().message(mockMessage).build();

        RequestContext context = new RequestContext(mockParams, null, null, null, null, null);

        assertEquals(existingTaskId, context.getTaskId());
        assertEquals(existingContextId, context.getContextId());
    }

    @Test
    public void testInitWithTaskIdAndExistingTaskIdMatch() {
        var mockMessage = new Message.Builder().role(Message.Role.USER).parts(List.of(new TextPart(""))).taskId("task-123").contextId("context-456").build();
        var mockParams = new MessageSendParams.Builder().message(mockMessage).build();
        var mockTask = new Task.Builder().id("task-123").contextId("context-456").status(new TaskStatus(TaskState.COMPLETED)).build();


        RequestContext context = new RequestContext(mockParams, mockTask.getId(), null, mockTask, null, null);

        assertEquals(mockTask.getId(), context.getTaskId());
        assertEquals(mockTask, context.getTask());
    }

    @Test
    public void testInitWithContextIdAndExistingContextIdMatch() {
        var mockMessage = new Message.Builder().role(Message.Role.USER).parts(List.of(new TextPart(""))).taskId("task-123").contextId("context-456").build();
        var mockParams = new MessageSendParams.Builder().message(mockMessage).build();
        var mockTask = new Task.Builder().id("task-123").contextId("context-456").status(new TaskStatus(TaskState.COMPLETED)).build();


        RequestContext context = new RequestContext(mockParams, mockTask.getId(), mockTask.getContextId(), mockTask, null, null);

        assertEquals(mockTask.getContextId(), context.getContextId());
        assertEquals(mockTask, context.getTask());
    }
}
