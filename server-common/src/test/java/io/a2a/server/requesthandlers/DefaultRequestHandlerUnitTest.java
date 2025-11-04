package io.a2a.server.requesthandlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.a2a.server.ServerCallContext;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.auth.UnauthenticatedUser;
import io.a2a.server.events.EventQueue;
import io.a2a.server.events.QueueManager;
import io.a2a.server.tasks.PushNotificationConfigStore;
import io.a2a.server.tasks.PushNotificationSender;
import io.a2a.server.tasks.TaskStore;
import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.InternalError;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.Message;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskNotCancelableError;
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TextPart;
import io.a2a.spec.UnsupportedOperationError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for DefaultRequestHandler using Mockito.
 * These tests focus on individual methods and edge cases, particularly the history truncation logic.
 */
public class DefaultRequestHandlerUnitTest {

    @Mock
    private AgentExecutor agentExecutor;

    @Mock
    private TaskStore taskStore;

    @Mock
    private QueueManager queueManager;

    @Mock
    private PushNotificationConfigStore pushConfigStore;

    @Mock
    private PushNotificationSender pushSender;

    private Executor executor;
    private DefaultRequestHandler requestHandler;
    private ServerCallContext serverCallContext;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        executor = Executors.newCachedThreadPool();
        requestHandler = new DefaultRequestHandler(
                agentExecutor,
                taskStore,
                queueManager,
                pushConfigStore,
                pushSender,
                executor
        );
        serverCallContext = new ServerCallContext(UnauthenticatedUser.INSTANCE, Map.of(), Set.of());
    }

    @Nested
    class OnGetTaskTests {

        @Test
        void testGetTask_TaskNotFound() {
            // Given
            TaskQueryParams params = new TaskQueryParams("non-existent-task", 0);
            when(taskStore.get("non-existent-task")).thenReturn(null);

            // When/Then
            assertThrows(TaskNotFoundError.class, ()
                    -> requestHandler.onGetTask(params, serverCallContext)
            );
        }

        @Test
        void testGetTask_NoHistoryTruncation_WhenHistoryLengthIsZero() throws Exception {
            // Given
            List<Message> fullHistory = createMessageList(5);
            Task task = createTaskWithHistory("task-1", fullHistory);
            TaskQueryParams params = new TaskQueryParams("task-1", 0);

            when(taskStore.get("task-1")).thenReturn(task);

            // When
            Task result = requestHandler.onGetTask(params, serverCallContext);

            // Then
            assertNotNull(result);
            assertEquals(5, result.getHistory().size());
            assertEquals(fullHistory, result.getHistory());
        }

        @Test
        void testGetTask_NoHistoryTruncation_WhenHistoryLengthEqualsHistorySize() throws Exception {
            // Given
            List<Message> fullHistory = createMessageList(5);
            Task task = createTaskWithHistory("task-1", fullHistory);
            TaskQueryParams params = new TaskQueryParams("task-1", 5);

            when(taskStore.get("task-1")).thenReturn(task);

            // When
            Task result = requestHandler.onGetTask(params, serverCallContext);

            // Then
            assertNotNull(result);
            assertEquals(5, result.getHistory().size());
            assertEquals(fullHistory, result.getHistory());
        }

        @Test
        void testGetTask_NoHistoryTruncation_WhenHistoryLengthGreaterThanHistorySize() throws Exception {
            // Given
            List<Message> fullHistory = createMessageList(5);
            Task task = createTaskWithHistory("task-1", fullHistory);
            TaskQueryParams params = new TaskQueryParams("task-1", 10);

            when(taskStore.get("task-1")).thenReturn(task);

            // When
            Task result = requestHandler.onGetTask(params, serverCallContext);

            // Then
            assertNotNull(result);
            assertEquals(5, result.getHistory().size());
            assertEquals(fullHistory, result.getHistory());
        }

        @Test
        void testGetTask_HistoryTruncation_ReturnsLastNMessages() throws Exception {
            // Given - 10 messages in history
            List<Message> fullHistory = createMessageList(10);
            Task task = createTaskWithHistory("task-1", fullHistory);
            TaskQueryParams params = new TaskQueryParams("task-1", 3);

            when(taskStore.get("task-1")).thenReturn(task);

            // When - request last 3 messages
            Task result = requestHandler.onGetTask(params, serverCallContext);

            // Then - should get messages at indices 7, 8, 9 (the last 3)
            assertNotNull(result);
            assertEquals(3, result.getHistory().size());
            assertEquals("msg-7", result.getHistory().get(0).getMessageId());
            assertEquals("msg-8", result.getHistory().get(1).getMessageId());
            assertEquals("msg-9", result.getHistory().get(2).getMessageId());
        }

        @Test
        void testGetTask_HistoryTruncation_IncludesMostRecentMessage() throws Exception {
            // Given - This tests the bug fix where size()-1 was excluding the last message
            List<Message> fullHistory = createMessageList(10);
            Task task = createTaskWithHistory("task-1", fullHistory);
            TaskQueryParams params = new TaskQueryParams("task-1", 1);

            when(taskStore.get("task-1")).thenReturn(task);

            // When - request last 1 message
            Task result = requestHandler.onGetTask(params, serverCallContext);

            // Then - should get the LAST message (msg-9), not msg-8
            assertNotNull(result);
            assertEquals(1, result.getHistory().size());
            assertEquals("msg-9", result.getHistory().get(0).getMessageId());
        }

        @Test
        void testGetTask_NullHistory_ReturnsTaskAsIs() throws Exception {
            // Given
            Task task = new Task.Builder()
                    .id("task-1")
                    .contextId("ctx-1")
                    .status(new TaskStatus(TaskState.COMPLETED))
                    .build();
            TaskQueryParams params = new TaskQueryParams("task-1", 3);

            when(taskStore.get("task-1")).thenReturn(task);

            // When
            Task result = requestHandler.onGetTask(params, serverCallContext);

            // Then
            assertNotNull(result);
            assertEquals(task, result);
        }

        @Test
        void testGetTask_EmptyHistory_ReturnsTaskAsIs() throws Exception {
            // Given
            Task task = createTaskWithHistory("task-1", new ArrayList<>());
            TaskQueryParams params = new TaskQueryParams("task-1", 3);

            when(taskStore.get("task-1")).thenReturn(task);

            // When
            Task result = requestHandler.onGetTask(params, serverCallContext);

            // Then
            assertNotNull(result);
            assertEquals(0, result.getHistory().size());
        }

        private List<Message> createMessageList(int count) {
            List<Message> messages = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                messages.add(new Message.Builder()
                        .messageId("msg-" + i)
                        .role(Message.Role.USER)
                        .parts(new TextPart("Message " + i))
                        .build());
            }
            return messages;
        }

        private Task createTaskWithHistory(String taskId, List<Message> history) {
            return new Task.Builder()
                    .id(taskId)
                    .contextId("ctx-1")
                    .status(new TaskStatus(TaskState.COMPLETED))
                    .history(history)
                    .build();
        }
    }

    @Nested
    class OnCancelTaskTests {

        @Test
        void testCancelTask_TaskNotFound() {
            // Given
            TaskIdParams params = new TaskIdParams("non-existent-task");
            when(taskStore.get("non-existent-task")).thenReturn(null);

            // When/Then
            assertThrows(TaskNotFoundError.class, ()
                    -> requestHandler.onCancelTask(params, serverCallContext)
            );
        }

        @Test
        void testCancelTask_TaskAlreadyCompleted() {
            // Given
            Task completedTask = new Task.Builder()
                    .id("task-1")
                    .contextId("ctx-1")
                    .status(new TaskStatus(TaskState.COMPLETED))
                    .build();
            TaskIdParams params = new TaskIdParams("task-1");

            when(taskStore.get("task-1")).thenReturn(completedTask);

            // When/Then
            TaskNotCancelableError exception = assertThrows(TaskNotCancelableError.class, ()
                    -> requestHandler.onCancelTask(params, serverCallContext)
            );

            assertEquals(-32002 , exception.getCode());
            assertTrue(exception.getMessage().contains("completed"));
        }

        @Test
        void testCancelTask_TaskAlreadyCanceled() {
            // Given
            Task canceledTask = new Task.Builder()
                    .id("task-1")
                    .contextId("ctx-1")
                    .status(new TaskStatus(TaskState.CANCELED))
                    .build();
            TaskIdParams params = new TaskIdParams("task-1");

            when(taskStore.get("task-1")).thenReturn(canceledTask);

            // When/Then
            TaskNotCancelableError exception = assertThrows(TaskNotCancelableError.class, ()
                    -> requestHandler.onCancelTask(params, serverCallContext)
            );

            assertEquals(-32002 , exception.getCode());
            assertTrue(exception.getMessage().contains("canceled"));
        }

        @Test
        void testCancelTask_TaskAlreadyFailed() {
            // Given
            Task failedTask = new Task.Builder()
                    .id("task-1")
                    .contextId("ctx-1")
                    .status(new TaskStatus(TaskState.FAILED))
                    .build();
            TaskIdParams params = new TaskIdParams("task-1");

            when(taskStore.get("task-1")).thenReturn(failedTask);

            // When/Then
            TaskNotCancelableError exception = assertThrows(TaskNotCancelableError.class, ()
                    -> requestHandler.onCancelTask(params, serverCallContext)
            );

            assertEquals(-32002 , exception.getCode());
            assertTrue(exception.getMessage().contains("failed"));
        }

        @Test
        void testCancelTask_TaskAlreadyRejected() {
            // Given
            Task rejectedTask = new Task.Builder()
                    .id("task-1")
                    .contextId("ctx-1")
                    .status(new TaskStatus(TaskState.REJECTED))
                    .build();
            TaskIdParams params = new TaskIdParams("task-1");

            when(taskStore.get("task-1")).thenReturn(rejectedTask);

            // When/Then
            TaskNotCancelableError exception = assertThrows(TaskNotCancelableError.class, ()
                    -> requestHandler.onCancelTask(params, serverCallContext)
            );


            assertEquals(-32002 , exception.getCode());
            assertTrue(exception.getMessage().contains("rejected"));
        }
    }

    @Nested
    class PushNotificationConfigTests {

        @Test
        void testSetTaskPushNotificationConfig_NoConfigStore() {
            // Given
            requestHandler = new DefaultRequestHandler(
                    agentExecutor, taskStore, queueManager, null, pushSender, executor
            );
            TaskPushNotificationConfig params = new TaskPushNotificationConfig(
                    "task-1",
                    new PushNotificationConfig.Builder().id("config-1").url("http://localhost:8080").build()
            );

            // When/Then
            assertThrows(UnsupportedOperationError.class, ()
                    -> requestHandler.onSetTaskPushNotificationConfig(params, serverCallContext)
            );
        }

        @Test
        void testSetTaskPushNotificationConfig_TaskNotFound() {
            // Given
            TaskPushNotificationConfig params = new TaskPushNotificationConfig(
                    "non-existent-task",
                    new PushNotificationConfig.Builder().id("config-1").url("http://localhost:8080").build()
            );
            when(taskStore.get("non-existent-task")).thenReturn(null);

            // When/Then
            assertThrows(TaskNotFoundError.class, ()
                    -> requestHandler.onSetTaskPushNotificationConfig(params, serverCallContext)
            );
        }

        @Test
        void testSetTaskPushNotificationConfig_Success() throws Exception {
            // Given
            Task task = new Task.Builder()
                    .id("task-1")
                    .contextId("ctx-1")
                    .status(new TaskStatus(TaskState.WORKING))
                    .build();
            PushNotificationConfig config = new PushNotificationConfig.Builder()
                    .id("config-1")
                    .url("http://localhost:8080")
                    .build();
            TaskPushNotificationConfig params = new TaskPushNotificationConfig("task-1", config);

            when(taskStore.get("task-1")).thenReturn(task);
            when(pushConfigStore.setInfo("task-1", config)).thenReturn(config);

            // When
            TaskPushNotificationConfig result = requestHandler.onSetTaskPushNotificationConfig(params, serverCallContext);

            // Then
            assertNotNull(result);
            assertEquals("task-1", result.taskId());
            assertEquals("config-1", result.pushNotificationConfig().id());
            verify(pushConfigStore).setInfo("task-1", config);
        }

        @Test
        void testGetTaskPushNotificationConfig_NoConfigStore() {
            // Given
            requestHandler = new DefaultRequestHandler(
                    agentExecutor, taskStore, queueManager, null, pushSender, executor
            );
            GetTaskPushNotificationConfigParams params = new GetTaskPushNotificationConfigParams("task-1", null);

            // When/Then
            assertThrows(UnsupportedOperationError.class, ()
                    -> requestHandler.onGetTaskPushNotificationConfig(params, serverCallContext)
            );
        }

        @Test
        void testGetTaskPushNotificationConfig_TaskNotFound() {
            // Given
            GetTaskPushNotificationConfigParams params = new GetTaskPushNotificationConfigParams("non-existent-task", null);
            when(taskStore.get("non-existent-task")).thenReturn(null);

            // When/Then
            assertThrows(TaskNotFoundError.class, ()
                    -> requestHandler.onGetTaskPushNotificationConfig(params, serverCallContext)
            );
        }

        @Test
        void testGetTaskPushNotificationConfig_NoConfigFound() {
            // Given
            Task task = new Task.Builder()
                    .id("task-1")
                    .contextId("ctx-1")
                    .status(new TaskStatus(TaskState.WORKING))
                    .build();
            GetTaskPushNotificationConfigParams params = new GetTaskPushNotificationConfigParams("task-1", null);

            when(taskStore.get("task-1")).thenReturn(task);
            when(pushConfigStore.getInfo("task-1")).thenReturn(null);

            // When/Then
            assertThrows(InternalError.class, ()
                    -> requestHandler.onGetTaskPushNotificationConfig(params, serverCallContext)
            );
        }

        @Test
        void testGetTaskPushNotificationConfig_EmptyConfigList() {
            // Given
            Task task = new Task.Builder()
                    .id("task-1")
                    .contextId("ctx-1")
                    .status(new TaskStatus(TaskState.WORKING))
                    .build();
            GetTaskPushNotificationConfigParams params = new GetTaskPushNotificationConfigParams("task-1", null);

            when(taskStore.get("task-1")).thenReturn(task);
            when(pushConfigStore.getInfo("task-1")).thenReturn(new ArrayList<>());

            // When/Then
            assertThrows(InternalError.class, ()
                    -> requestHandler.onGetTaskPushNotificationConfig(params, serverCallContext)
            );
        }

        @Test
        void testGetTaskPushNotificationConfig_ReturnsFirstConfig_WhenNoIdSpecified() throws Exception {
            // Given
            Task task = new Task.Builder()
                    .id("task-1")
                    .contextId("ctx-1")
                    .status(new TaskStatus(TaskState.WORKING))
                    .build();
            PushNotificationConfig config1 = new PushNotificationConfig.Builder().id("config-1").url("http://localhost:8080").build();
            PushNotificationConfig config2 = new PushNotificationConfig.Builder().id("config-2").url("http://localhost:8080").build();
            List<PushNotificationConfig> configs = List.of(config1, config2);
            GetTaskPushNotificationConfigParams params = new GetTaskPushNotificationConfigParams("task-1", null);

            when(taskStore.get("task-1")).thenReturn(task);
            when(pushConfigStore.getInfo("task-1")).thenReturn(configs);

            // When
            TaskPushNotificationConfig result = requestHandler.onGetTaskPushNotificationConfig(params, serverCallContext);

            // Then
            assertNotNull(result);
            assertEquals("task-1", result.taskId());
            assertEquals("config-1", result.pushNotificationConfig().id());
        }

        @Test
        void testGetTaskPushNotificationConfig_ReturnsSpecificConfig_WhenIdSpecified() throws Exception {
            // Given
            Task task = new Task.Builder()
                    .id("task-1")
                    .contextId("ctx-1")
                    .status(new TaskStatus(TaskState.WORKING))
                    .build();
            PushNotificationConfig config1 = new PushNotificationConfig.Builder().id("config-1").url("http://localhost:8080").build();
            PushNotificationConfig config2 = new PushNotificationConfig.Builder().id("config-2").url("http://localhost:8080").build();
            List<PushNotificationConfig> configs = List.of(config1, config2);
            GetTaskPushNotificationConfigParams params = new GetTaskPushNotificationConfigParams("task-1", "config-2");

            when(taskStore.get("task-1")).thenReturn(task);
            when(pushConfigStore.getInfo("task-1")).thenReturn(configs);

            // When
            TaskPushNotificationConfig result = requestHandler.onGetTaskPushNotificationConfig(params, serverCallContext);

            // Then
            assertNotNull(result);
            assertEquals("task-1", result.taskId());
            assertEquals("config-2", result.pushNotificationConfig().id());
        }

        @Test
        void testListTaskPushNotificationConfig_NoConfigStore() {
            // Given
            requestHandler = new DefaultRequestHandler(
                    agentExecutor, taskStore, queueManager, null, pushSender, executor
            );
            ListTaskPushNotificationConfigParams params = new ListTaskPushNotificationConfigParams("task-1");

            // When/Then
            assertThrows(UnsupportedOperationError.class, ()
                    -> requestHandler.onListTaskPushNotificationConfig(params, serverCallContext)
            );
        }

        @Test
        void testListTaskPushNotificationConfig_TaskNotFound() {
            // Given
            ListTaskPushNotificationConfigParams params = new ListTaskPushNotificationConfigParams("non-existent-task");
            when(taskStore.get("non-existent-task")).thenReturn(null);

            // When/Then
            assertThrows(TaskNotFoundError.class, ()
                    -> requestHandler.onListTaskPushNotificationConfig(params, serverCallContext)
            );
        }

        @Test
        void testListTaskPushNotificationConfig_ReturnsEmptyList_WhenNoConfigs() throws Exception {
            // Given
            Task task = new Task.Builder()
                    .id("task-1")
                    .contextId("ctx-1")
                    .status(new TaskStatus(TaskState.WORKING))
                    .build();
            ListTaskPushNotificationConfigParams params = new ListTaskPushNotificationConfigParams("task-1");

            when(taskStore.get("task-1")).thenReturn(task);
            when(pushConfigStore.getInfo("task-1")).thenReturn(null);

            // When
            List<TaskPushNotificationConfig> result = requestHandler.onListTaskPushNotificationConfig(params, serverCallContext);

            // Then
            assertNotNull(result);
            assertEquals(0, result.size());
        }

        @Test
        void testListTaskPushNotificationConfig_ReturnsAllConfigs() throws Exception {
            // Given
            Task task = new Task.Builder()
                    .id("task-1")
                    .contextId("ctx-1")
                    .status(new TaskStatus(TaskState.WORKING))
                    .build();
            PushNotificationConfig config1 = new PushNotificationConfig.Builder().id("config-1").url("http://localhost:8080").build();
            PushNotificationConfig config2 = new PushNotificationConfig.Builder().id("config-2").url("http://localhost:8080").build();
            List<PushNotificationConfig> configs = List.of(config1, config2);
            ListTaskPushNotificationConfigParams params = new ListTaskPushNotificationConfigParams("task-1");

            when(taskStore.get("task-1")).thenReturn(task);
            when(pushConfigStore.getInfo("task-1")).thenReturn(configs);

            // When
            List<TaskPushNotificationConfig> result = requestHandler.onListTaskPushNotificationConfig(params, serverCallContext);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("config-1", result.get(0).pushNotificationConfig().id());
            assertEquals("config-2", result.get(1).pushNotificationConfig().id());
        }

        @Test
        void testDeleteTaskPushNotificationConfig_NoConfigStore() {
            // Given
            requestHandler = new DefaultRequestHandler(
                    agentExecutor, taskStore, queueManager, null, pushSender, executor
            );
            DeleteTaskPushNotificationConfigParams params = new DeleteTaskPushNotificationConfigParams("task-1", "config-1");

            // When/Then
            assertThrows(UnsupportedOperationError.class, ()
                    -> requestHandler.onDeleteTaskPushNotificationConfig(params, serverCallContext)
            );
        }

        @Test
        void testDeleteTaskPushNotificationConfig_TaskNotFound() {
            // Given
            DeleteTaskPushNotificationConfigParams params = new DeleteTaskPushNotificationConfigParams("non-existent-task", "config-1");
            when(taskStore.get("non-existent-task")).thenReturn(null);

            // When/Then
            assertThrows(TaskNotFoundError.class, ()
                    -> requestHandler.onDeleteTaskPushNotificationConfig(params, serverCallContext)
            );
        }

        @Test
        void testDeleteTaskPushNotificationConfig_Success() {
            // Given
            Task task = new Task.Builder()
                    .id("task-1")
                    .contextId("ctx-1")
                    .status(new TaskStatus(TaskState.WORKING))
                    .build();
            DeleteTaskPushNotificationConfigParams params = new DeleteTaskPushNotificationConfigParams("task-1", "config-1");

            when(taskStore.get("task-1")).thenReturn(task);

            // When
            requestHandler.onDeleteTaskPushNotificationConfig(params, serverCallContext);

            // Then
            verify(pushConfigStore).deleteInfo("task-1", "config-1");
        }
    }

    @Nested
    class OnResubscribeToTaskTests {

        @Test
        void testResubscribeToTask_TaskNotFound() {
            // Given
            TaskIdParams params = new TaskIdParams("non-existent-task");
            when(taskStore.get("non-existent-task")).thenReturn(null);

            // When/Then
            assertThrows(TaskNotFoundError.class, ()
                    -> requestHandler.onResubscribeToTask(params, serverCallContext)
            );
        }

        @Test
        void testResubscribeToTask_QueueNotFound_FinalTask() {
            // Given
            Task finalTask = new Task.Builder()
                    .id("task-1")
                    .contextId("ctx-1")
                    .status(new TaskStatus(TaskState.COMPLETED))
                    .build();
            TaskIdParams params = new TaskIdParams("task-1");

            when(taskStore.get("task-1")).thenReturn(finalTask);
            when(queueManager.tap("task-1")).thenReturn(null);

            // When/Then - Should throw TaskNotFoundError for final tasks with no queue
            assertThrows(TaskNotFoundError.class, ()
                    -> requestHandler.onResubscribeToTask(params, serverCallContext)
            );
        }

        @Test
        void testResubscribeToTask_QueueNotFound_NonFinalTask_CreatesNewQueue() throws Exception {
            // Given
            Task workingTask = new Task.Builder()
                    .id("task-1")
                    .contextId("ctx-1")
                    .status(new TaskStatus(TaskState.WORKING))
                    .build();
            TaskIdParams params = new TaskIdParams("task-1");
            EventQueue newQueue = mock(EventQueue.class);

            when(taskStore.get("task-1")).thenReturn(workingTask);
            when(queueManager.tap("task-1")).thenReturn(null);
            when(queueManager.createOrTap("task-1")).thenReturn(newQueue);

            // When
            var result = requestHandler.onResubscribeToTask(params, serverCallContext);

            // Then
            assertNotNull(result);
            verify(queueManager).createOrTap("task-1");
        }

        @Test
        void testResubscribeToTask_QueueExists_ReturnsPublisher() throws Exception {
            // Given
            Task workingTask = new Task.Builder()
                    .id("task-1")
                    .contextId("ctx-1")
                    .status(new TaskStatus(TaskState.WORKING))
                    .build();
            TaskIdParams params = new TaskIdParams("task-1");
            EventQueue existingQueue = mock(EventQueue.class);

            when(taskStore.get("task-1")).thenReturn(workingTask);
            when(queueManager.tap("task-1")).thenReturn(existingQueue);

            // When
            var result = requestHandler.onResubscribeToTask(params, serverCallContext);

            // Then
            assertNotNull(result);
            verify(queueManager).tap("task-1");
            verify(queueManager, never()).createOrTap(anyString());
        }
    }
}
