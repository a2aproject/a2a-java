package org.a2aproject.sdk.server.requesthandlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.TaskAuthorizationProvider;
import org.a2aproject.sdk.server.auth.TaskOperation;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.CancelTaskParams;
import org.a2aproject.sdk.spec.DeleteTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.EventKind;
import org.a2aproject.sdk.spec.StreamingEventKind;
import org.a2aproject.sdk.spec.GetTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsParams;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskIdParams;
import org.a2aproject.sdk.spec.TaskNotFoundError;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;
import org.a2aproject.sdk.spec.TextPart;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthorizationRequestHandlerDecoratorTest {

    @Mock
    private RequestHandler delegate;

    @Mock
    private ServerCallContext context;

    @Mock
    private TaskAuthorizationProvider authorizationProvider;

    private static Task testTask(String id) {
        return Task.builder()
                .id(id)
                .contextId("ctx-1")
                .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED))
                .history(Collections.emptyList())
                .artifacts(Collections.emptyList())
                .build();
    }

    @Nested
    class NoProviderTests {

        private AuthorizationRequestHandlerDecorator decorator;

        @BeforeEach
        void setUp() {
            decorator = new AuthorizationRequestHandlerDecorator(delegate, null);
        }

        @Test
        void onGetTask_delegatesWithoutChecks() throws A2AError {
            TaskQueryParams params = new TaskQueryParams("task-1", null, null);
            Task expected = testTask("task-1");
            when(delegate.onGetTask(params, context)).thenReturn(expected);

            Task result = decorator.onGetTask(params, context);

            assertEquals(expected, result);
            verify(delegate).onGetTask(params, context);
        }

        @Test
        void onListTasks_delegatesWithoutChecks() throws A2AError {
            ListTasksParams params = new ListTasksParams();
            ListTasksResult expected = new ListTasksResult(Collections.emptyList(), 0, 0, null);
            when(delegate.onListTasks(params, context)).thenReturn(expected);

            ListTasksResult result = decorator.onListTasks(params, context);

            assertEquals(expected, result);
            verify(delegate).onListTasks(params, context);
        }

        @Test
        void onCancelTask_delegatesWithoutChecks() throws A2AError {
            CancelTaskParams params = new CancelTaskParams("task-1");
            Task expected = testTask("task-1");
            when(delegate.onCancelTask(params, context)).thenReturn(expected);

            Task result = decorator.onCancelTask(params, context);

            assertEquals(expected, result);
            verify(delegate).onCancelTask(params, context);
        }
    }

    @Nested
    class CheckReadTests {

        private AuthorizationRequestHandlerDecorator decorator;

        @BeforeEach
        void setUp() {
            decorator = new AuthorizationRequestHandlerDecorator(delegate, authorizationProvider);
        }

        @Test
        void onGetTask_allowed() throws A2AError {
            TaskQueryParams params = new TaskQueryParams("task-1", null, null);
            Task expected = testTask("task-1");
            when(authorizationProvider.checkRead(context, "task-1", TaskOperation.GET_TASK)).thenReturn(true);
            when(delegate.onGetTask(params, context)).thenReturn(expected);

            Task result = decorator.onGetTask(params, context);

            assertEquals(expected, result);
        }

        @Test
        void onGetTask_denied() throws A2AError {
            TaskQueryParams params = new TaskQueryParams("task-1", null, null);
            when(authorizationProvider.checkRead(context, "task-1", TaskOperation.GET_TASK)).thenReturn(false);

            assertThrows(TaskNotFoundError.class, () -> decorator.onGetTask(params, context));
            verifyNoInteractions(delegate);
        }

        @Test
        void onSubscribeToTask_denied() throws A2AError {
            TaskIdParams params = new TaskIdParams("task-1");
            when(authorizationProvider.checkRead(context, "task-1", TaskOperation.SUBSCRIBE_TO_TASK)).thenReturn(false);

            assertThrows(TaskNotFoundError.class, () -> decorator.onSubscribeToTask(params, context));
            verifyNoInteractions(delegate);
        }

        @Test
        void onGetTaskPushNotificationConfig_denied() throws A2AError {
            GetTaskPushNotificationConfigParams params = new GetTaskPushNotificationConfigParams("task-1", "config-1");
            when(authorizationProvider.checkRead(context, "task-1", TaskOperation.GET_TASK_PUSH_NOTIFICATION_CONFIG))
                    .thenReturn(false);

            assertThrows(TaskNotFoundError.class,
                    () -> decorator.onGetTaskPushNotificationConfig(params, context));
            verifyNoInteractions(delegate);
        }

        @Test
        void onListTaskPushNotificationConfigs_denied() throws A2AError {
            ListTaskPushNotificationConfigsParams params = new ListTaskPushNotificationConfigsParams("task-1");
            when(authorizationProvider.checkRead(context, "task-1", TaskOperation.LIST_TASK_PUSH_NOTIFICATION_CONFIGS))
                    .thenReturn(false);

            assertThrows(TaskNotFoundError.class,
                    () -> decorator.onListTaskPushNotificationConfigs(params, context));
            verifyNoInteractions(delegate);
        }

        @Test
        void spiException_propagates() throws A2AError {
            TaskQueryParams params = new TaskQueryParams("task-1", null, null);
            A2AError spiError = new A2AError(-32000, "Authorization service unavailable", null) {};
            when(authorizationProvider.checkRead(context, "task-1", TaskOperation.GET_TASK)).thenThrow(spiError);

            A2AError thrown = assertThrows(A2AError.class, () -> decorator.onGetTask(params, context));
            assertEquals(spiError, thrown);
            verifyNoInteractions(delegate);
        }
    }

    @Nested
    class CheckWriteTests {

        private AuthorizationRequestHandlerDecorator decorator;

        @BeforeEach
        void setUp() {
            decorator = new AuthorizationRequestHandlerDecorator(delegate, authorizationProvider);
        }

        @Test
        void onCancelTask_allowed() throws A2AError {
            CancelTaskParams params = new CancelTaskParams("task-1");
            Task expected = testTask("task-1");
            when(authorizationProvider.checkWrite(context, "task-1", TaskOperation.CANCEL_TASK)).thenReturn(true);
            when(delegate.onCancelTask(params, context)).thenReturn(expected);

            Task result = decorator.onCancelTask(params, context);

            assertEquals(expected, result);
        }

        @Test
        void onCancelTask_denied() throws A2AError {
            CancelTaskParams params = new CancelTaskParams("task-1");
            when(authorizationProvider.checkWrite(context, "task-1", TaskOperation.CANCEL_TASK)).thenReturn(false);

            assertThrows(TaskNotFoundError.class, () -> decorator.onCancelTask(params, context));
            verifyNoInteractions(delegate);
        }

        @Test
        void onCreateTaskPushNotificationConfig_denied() throws A2AError {
            TaskPushNotificationConfig params = TaskPushNotificationConfig.builder()
                    .id("config-1").taskId("task-1").url("https://example.com/webhook").build();
            when(authorizationProvider.checkWrite(context, "task-1",
                    TaskOperation.CREATE_TASK_PUSH_NOTIFICATION_CONFIG)).thenReturn(false);

            assertThrows(TaskNotFoundError.class,
                    () -> decorator.onCreateTaskPushNotificationConfig(params, context));
            verifyNoInteractions(delegate);
        }

        @Test
        void onDeleteTaskPushNotificationConfig_denied() throws A2AError {
            DeleteTaskPushNotificationConfigParams params =
                    new DeleteTaskPushNotificationConfigParams("task-1", "config-1");
            when(authorizationProvider.checkWrite(context, "task-1",
                    TaskOperation.DELETE_TASK_PUSH_NOTIFICATION_CONFIG)).thenReturn(false);

            assertThrows(TaskNotFoundError.class,
                    () -> decorator.onDeleteTaskPushNotificationConfig(params, context));
            verifyNoInteractions(delegate);
        }

        @Test
        void onMessageSend_existingTask_denied() throws A2AError {
            Message message = Message.builder().messageId("m-1").role(Message.Role.ROLE_USER)
                    .taskId("task-1").parts(new TextPart("hello")).build();
            MessageSendParams params = new MessageSendParams(message, null, null, null);
            when(authorizationProvider.checkWrite(context, "task-1", TaskOperation.MESSAGE_SEND)).thenReturn(false);

            assertThrows(TaskNotFoundError.class, () -> decorator.onMessageSend(params, context));
            verifyNoInteractions(delegate);
        }

        @Test
        void onMessageSendStream_existingTask_denied() throws A2AError {
            Message message = Message.builder().messageId("m-1").role(Message.Role.ROLE_USER)
                    .taskId("task-1").parts(new TextPart("hello")).build();
            MessageSendParams params = new MessageSendParams(message, null, null, null);
            when(authorizationProvider.checkWrite(context, "task-1", TaskOperation.MESSAGE_SEND_STREAM))
                    .thenReturn(false);

            assertThrows(TaskNotFoundError.class, () -> decorator.onMessageSendStream(params, context));
            verifyNoInteractions(delegate);
        }
    }

    @Nested
    class CheckCreateAndOwnershipTests {

        private AuthorizationRequestHandlerDecorator decorator;

        @BeforeEach
        void setUp() {
            decorator = new AuthorizationRequestHandlerDecorator(delegate, authorizationProvider);
        }

        private MessageSendParams newTaskParams() {
            Message message = Message.builder().messageId("m-1").role(Message.Role.ROLE_USER)
                    .parts(new TextPart("hello")).build();
            return new MessageSendParams(message, null, null, null);
        }

        @Test
        void onMessageSend_newTask_checkCreateDenied() throws A2AError {
            when(authorizationProvider.checkCreate(context, TaskOperation.MESSAGE_SEND)).thenReturn(false);

            assertThrows(TaskNotFoundError.class, () -> decorator.onMessageSend(newTaskParams(), context));
            verifyNoInteractions(delegate);
        }

        @Test
        void onMessageSend_newTask_createsTask_recordsOwnership() throws A2AError {
            Task createdTask = testTask("new-task-1");
            when(authorizationProvider.checkCreate(context, TaskOperation.MESSAGE_SEND)).thenReturn(true);
            when(delegate.onMessageSend(any(), eq(context))).thenReturn(createdTask);
            when(authorizationProvider.isTaskRecorded("new-task-1")).thenReturn(false);

            EventKind result = decorator.onMessageSend(newTaskParams(), context);

            assertEquals(createdTask, result);
            verify(authorizationProvider).recordOwnership(context, "new-task-1", TaskOperation.MESSAGE_SEND);
        }

        @Test
        void onMessageSend_newTask_alreadyRecorded_skipsOwnership() throws A2AError {
            Task createdTask = testTask("new-task-1");
            when(authorizationProvider.checkCreate(context, TaskOperation.MESSAGE_SEND)).thenReturn(true);
            when(delegate.onMessageSend(any(), eq(context))).thenReturn(createdTask);
            when(authorizationProvider.isTaskRecorded("new-task-1")).thenReturn(true);

            decorator.onMessageSend(newTaskParams(), context);

            verify(authorizationProvider, never()).recordOwnership(any(), any(), any());
        }

        @Test
        void onMessageSend_returnsMessage_noOwnershipRecording() throws A2AError {
            Message response = Message.builder().messageId("resp-1").role(Message.Role.ROLE_AGENT)
                    .parts(new TextPart("response")).build();
            when(authorizationProvider.checkCreate(context, TaskOperation.MESSAGE_SEND)).thenReturn(true);
            when(delegate.onMessageSend(any(), eq(context))).thenReturn(response);

            EventKind result = decorator.onMessageSend(newTaskParams(), context);

            assertEquals(response, result);
            verify(authorizationProvider, never()).isTaskRecorded(any());
            verify(authorizationProvider, never()).recordOwnership(any(), any(), any());
        }

        @Test
        void onMessageSend_existingTask_allowed_recordsOwnershipIfNeeded() throws A2AError {
            Message message = Message.builder().messageId("m-1").role(Message.Role.ROLE_USER)
                    .taskId("existing-task").parts(new TextPart("hello")).build();
            MessageSendParams params = new MessageSendParams(message, null, null, null);
            Task resultTask = testTask("existing-task");
            when(authorizationProvider.checkWrite(context, "existing-task", TaskOperation.MESSAGE_SEND))
                    .thenReturn(true);
            when(delegate.onMessageSend(params, context)).thenReturn(resultTask);
            when(authorizationProvider.isTaskRecorded("existing-task")).thenReturn(false);

            decorator.onMessageSend(params, context);

            verify(authorizationProvider).recordOwnership(context, "existing-task", TaskOperation.MESSAGE_SEND);
        }
    }

    @Nested
    class StreamingOwnershipTests {

        private AuthorizationRequestHandlerDecorator decorator;

        @BeforeEach
        void setUp() {
            decorator = new AuthorizationRequestHandlerDecorator(delegate, authorizationProvider);
        }

        private MessageSendParams newTaskStreamParams() {
            Message message = Message.builder().messageId("m-1").role(Message.Role.ROLE_USER)
                    .parts(new TextPart("hello")).build();
            return new MessageSendParams(message, null, null, null);
        }

        @Test
        void onMessageSendStream_newTask_recordsOwnershipOnFirstTaskEvent() throws Exception {
            when(authorizationProvider.checkCreate(context, TaskOperation.MESSAGE_SEND_STREAM)).thenReturn(true);
            when(authorizationProvider.isTaskRecorded("stream-task-1")).thenReturn(false);

            TaskStatusUpdateEvent statusEvent = new TaskStatusUpdateEvent(
                    "stream-task-1", new TaskStatus(TaskState.TASK_STATE_WORKING), "ctx-1", null);

            Flow.Publisher<StreamingEventKind> sourcePublisher = subscriber -> {
                subscriber.onSubscribe(new Flow.Subscription() {
                    @Override
                    public void request(long n) {
                        subscriber.onNext(statusEvent);
                        subscriber.onComplete();
                    }

                    @Override
                    public void cancel() {
                    }
                });
            };
            when(delegate.onMessageSendStream(any(), eq(context))).thenReturn(sourcePublisher);

            Flow.Publisher<StreamingEventKind> result = decorator.onMessageSendStream(newTaskStreamParams(), context);

            List<StreamingEventKind> received = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);
            result.subscribe(new Flow.Subscriber<>() {
                @Override
                public void onSubscribe(Flow.Subscription s) {
                    s.request(10);
                }

                @Override
                public void onNext(StreamingEventKind item) {
                    received.add(item);
                }

                @Override
                public void onError(Throwable t) {
                    latch.countDown();
                }

                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });

            assertTrue(latch.await(5, TimeUnit.SECONDS));
            assertEquals(1, received.size());
            assertEquals(statusEvent, received.get(0));
            verify(authorizationProvider).recordOwnership(context, "stream-task-1", TaskOperation.MESSAGE_SEND_STREAM);
        }

        @Test
        void onMessageSendStream_messageOnly_noOwnershipRecording() throws Exception {
            when(authorizationProvider.checkCreate(context, TaskOperation.MESSAGE_SEND_STREAM)).thenReturn(true);

            Message response = Message.builder().messageId("resp-1").role(Message.Role.ROLE_AGENT)
                    .parts(new TextPart("response")).build();

            Flow.Publisher<StreamingEventKind> sourcePublisher = subscriber -> {
                subscriber.onSubscribe(new Flow.Subscription() {
                    @Override
                    public void request(long n) {
                        subscriber.onNext(response);
                        subscriber.onComplete();
                    }

                    @Override
                    public void cancel() {
                    }
                });
            };
            when(delegate.onMessageSendStream(any(), eq(context))).thenReturn(sourcePublisher);

            Flow.Publisher<StreamingEventKind> result = decorator.onMessageSendStream(newTaskStreamParams(), context);

            CountDownLatch latch = new CountDownLatch(1);
            result.subscribe(new Flow.Subscriber<>() {
                @Override
                public void onSubscribe(Flow.Subscription s) {
                    s.request(10);
                }

                @Override
                public void onNext(StreamingEventKind item) {
                }

                @Override
                public void onError(Throwable t) {
                    latch.countDown();
                }

                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });

            assertTrue(latch.await(5, TimeUnit.SECONDS));
            verify(authorizationProvider, never()).isTaskRecorded(any());
            verify(authorizationProvider, never()).recordOwnership(any(), any(), any());
        }
    }
}
