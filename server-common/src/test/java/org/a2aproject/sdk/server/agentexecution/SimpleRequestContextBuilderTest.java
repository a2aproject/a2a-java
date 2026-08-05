package org.a2aproject.sdk.server.agentexecution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.TaskAuthorizationProvider;
import org.a2aproject.sdk.server.auth.TaskOperation;
import org.a2aproject.sdk.server.tasks.InMemoryTaskStore;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.MessageSendConfiguration;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskNotFoundError;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.a2aproject.sdk.spec.TextPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SimpleRequestContextBuilderTest {

    @Mock
    private TaskAuthorizationProvider authorizationProvider;

    @Mock
    private ServerCallContext callContext;

    private InMemoryTaskStore taskStore;

    @BeforeEach
    void setUp() {
        taskStore = new InMemoryTaskStore();
    }

    private static Task testTask(String id) {
        return Task.builder()
                .id(id)
                .contextId("ctx-1")
                .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED))
                .build();
    }

    private static MessageSendConfiguration defaultConfiguration() {
        return MessageSendConfiguration.builder()
                .acceptedOutputModes(List.of())
                .returnImmediately(true)
                .build();
    }

    private static MessageSendParams paramsWithReferenceTaskIds(List<String> referenceTaskIds) {
        Message message = Message.builder()
                .role(Message.Role.ROLE_USER)
                .parts(List.of(new TextPart("")))
                .referenceTaskIds(referenceTaskIds)
                .build();
        return MessageSendParams.builder()
                .message(message)
                .configuration(defaultConfiguration())
                .build();
    }

    private static MessageSendParams paramsWithoutReferenceTaskIds() {
        Message message = Message.builder()
                .role(Message.Role.ROLE_USER)
                .parts(List.of(new TextPart("")))
                .build();
        return MessageSendParams.builder()
                .message(message)
                .configuration(defaultConfiguration())
                .build();
    }

    @Test
    void populateDisabled_ignoresReferenceTaskIds() {
        taskStore.save(testTask("task-1"), false);

        RequestContext ctx = new SimpleRequestContextBuilder(taskStore, false, authorizationProvider)
                .setParams(paramsWithReferenceTaskIds(List.of("task-1")))
                .setServerCallContext(callContext)
                .build();

        assertTrue(ctx.getRelatedTasks().isEmpty());
        verifyNoInteractions(authorizationProvider);
    }

    @Test
    void noReferenceTaskIds_emptyRelatedTasks() {
        RequestContext ctx = new SimpleRequestContextBuilder(taskStore, true, null)
                .setParams(paramsWithoutReferenceTaskIds())
                .build();

        assertTrue(ctx.getRelatedTasks().isEmpty());
    }

    @Test
    void noAuthorizationProvider_loadsAllReferencedTasks() {
        taskStore.save(testTask("task-1"), false);
        taskStore.save(testTask("task-2"), false);

        RequestContext ctx = new SimpleRequestContextBuilder(taskStore, true, null)
                .setParams(paramsWithReferenceTaskIds(List.of("task-1", "task-2")))
                .build();

        assertEquals(2, ctx.getRelatedTasks().size());
        assertTrue(ctx.getRelatedTasks().stream().anyMatch(t -> t.id().equals("task-1")));
        assertTrue(ctx.getRelatedTasks().stream().anyMatch(t -> t.id().equals("task-2")));
    }

    @Test
    void withAuthorizationProvider_rejectsWhenAnyTaskUnauthorized() {
        taskStore.save(testTask("task-1"), false);
        taskStore.save(testTask("task-2"), false);
        taskStore.save(testTask("task-3"), false);

        when(authorizationProvider.checkRead(eq(callContext), eq("task-1"), eq(TaskOperation.MESSAGE_SEND)))
                .thenReturn(true);
        when(authorizationProvider.checkRead(eq(callContext), eq("task-2"), eq(TaskOperation.MESSAGE_SEND)))
                .thenReturn(false);

        SimpleRequestContextBuilder builder = new SimpleRequestContextBuilder(taskStore, true, authorizationProvider);
        builder.setParams(paramsWithReferenceTaskIds(List.of("task-1", "task-2", "task-3")));
        builder.setServerCallContext(callContext);

        assertThrows(TaskNotFoundError.class, builder::build);
    }

    @Test
    void withAuthorizationProvider_allDenied_throwsTaskNotFoundError() {
        taskStore.save(testTask("task-1"), false);
        taskStore.save(testTask("task-2"), false);

        when(authorizationProvider.checkRead(eq(callContext), eq("task-1"), eq(TaskOperation.MESSAGE_SEND)))
                .thenReturn(false);

        SimpleRequestContextBuilder builder = new SimpleRequestContextBuilder(taskStore, true, authorizationProvider);
        builder.setParams(paramsWithReferenceTaskIds(List.of("task-1", "task-2")));
        builder.setServerCallContext(callContext);

        assertThrows(TaskNotFoundError.class, builder::build);
    }

    @Test
    void nonExistentTask_silentlySkipped() {
        taskStore.save(testTask("task-1"), false);

        RequestContext ctx = new SimpleRequestContextBuilder(taskStore, true, null)
                .setParams(paramsWithReferenceTaskIds(List.of("task-1", "nonexistent")))
                .build();

        assertEquals(1, ctx.getRelatedTasks().size());
        assertEquals("task-1", ctx.getRelatedTasks().get(0).id());
    }

    @Test
    void noServerCallContext_throwsWhenAuthProviderPresent() {
        taskStore.save(testTask("task-1"), false);
        taskStore.save(testTask("task-2"), false);

        SimpleRequestContextBuilder builder = new SimpleRequestContextBuilder(taskStore, true, authorizationProvider);
        builder.setParams(paramsWithReferenceTaskIds(List.of("task-1", "task-2")));

        assertThrows(TaskNotFoundError.class, builder::build);
        verifyNoInteractions(authorizationProvider);
    }

    @Test
    void authorizationCheckUsesCorrectOperation() {
        taskStore.save(testTask("task-1"), false);

        when(authorizationProvider.checkRead(eq(callContext), eq("task-1"), eq(TaskOperation.MESSAGE_SEND)))
                .thenReturn(true);

        new SimpleRequestContextBuilder(taskStore, true, authorizationProvider)
                .setParams(paramsWithReferenceTaskIds(List.of("task-1")))
                .setServerCallContext(callContext)
                .build();

        verify(authorizationProvider).checkRead(callContext, "task-1", TaskOperation.MESSAGE_SEND);
    }
}
