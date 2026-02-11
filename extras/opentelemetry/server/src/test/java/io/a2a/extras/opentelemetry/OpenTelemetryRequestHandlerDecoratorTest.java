package io.a2a.extras.opentelemetry;

import static io.a2a.extras.opentelemetry.A2AObservabilityNames.ERROR_TYPE;
import static io.a2a.extras.opentelemetry.A2AObservabilityNames.EXTRACT_REQUEST_SYS_PROPERTY;
import static io.a2a.extras.opentelemetry.A2AObservabilityNames.EXTRACT_RESPONSE_SYS_PROPERTY;
import static io.a2a.extras.opentelemetry.A2AObservabilityNames.GENAI_REQUEST;
import static io.a2a.extras.opentelemetry.A2AObservabilityNames.GENAI_RESPONSE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.a2a.jsonrpc.common.wrappers.ListTasksResult;
import io.a2a.server.ServerCallContext;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.spec.*;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Flow;

@ExtendWith(MockitoExtension.class)
class OpenTelemetryRequestHandlerDecoratorTest {

    @Mock
    private Tracer tracer;

    @Mock
    private Span span;

    @Mock
    private SpanBuilder spanBuilder;

    @Mock
    private Scope scope;

    @Mock
    private ServerCallContext context;

    @Mock
    private RequestHandler delegate;

    private TestableOpenTelemetryRequestHandlerDecorator decorator;

    @BeforeEach
    void setUp() {
        // Set system properties for extracting request/response
        System.setProperty(EXTRACT_REQUEST_SYS_PROPERTY, "true");
        System.setProperty(EXTRACT_RESPONSE_SYS_PROPERTY, "true");

        // Set up the mock chain
        lenient().when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.setSpanKind(any(SpanKind.class))).thenReturn(spanBuilder);
        lenient().when(spanBuilder.setAttribute(anyString(), anyString())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.setAttribute(anyString(), anyLong())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.startSpan()).thenReturn(span);
        lenient().when(span.makeCurrent()).thenReturn(scope);
        lenient().when(span.setAttribute(anyString(), anyString())).thenReturn(span);
        lenient().when(span.setStatus(any(StatusCode.class))).thenReturn(span);
        lenient().when(span.setStatus(any(StatusCode.class), anyString())).thenReturn(span);

        // Create decorator with mocked dependencies
        decorator = new TestableOpenTelemetryRequestHandlerDecorator(delegate, tracer);
    }

    /**
     * Concrete test implementation of the abstract decorator for testing purposes.
     */
    static class TestableOpenTelemetryRequestHandlerDecorator extends OpenTelemetryRequestHandlerDecorator {
        public TestableOpenTelemetryRequestHandlerDecorator(RequestHandler delegate, Tracer tracer) {
            super(delegate, tracer);
        }
    }

    @Nested
    class GetTaskTests {
        @Test
        void onGetTask_createsSpanAndDelegatesToHandler() throws A2AError {
            TaskQueryParams params = new TaskQueryParams("task-123", null);
            Task result = Task.builder()
                    .id("task-123")
                    .contextId("ctx-1")
                    .status(new TaskStatus(TaskState.COMPLETED))
                    .history(Collections.emptyList())
                    .artifacts(Collections.emptyList())
                    .build();
            when(delegate.onGetTask(params, context)).thenReturn(result);

            Task actualResult = decorator.onGetTask(params, context);

            assertEquals(result, actualResult);
            verify(tracer).spanBuilder(A2AMethods.GET_TASK_METHOD);
            verify(spanBuilder).setSpanKind(SpanKind.SERVER);
            verify(spanBuilder).setAttribute(GENAI_REQUEST, params.toString());
            verify(spanBuilder).startSpan();
            verify(span).makeCurrent();
            verify(span).setAttribute(GENAI_RESPONSE, result.toString());
            verify(span).setStatus(StatusCode.OK);
            verify(span).end();
            verify(delegate).onGetTask(params, context);
        }

        @Test
        void onGetTask_withError_setsErrorStatusAndRethrows() throws A2AError {
            TaskQueryParams params = new TaskQueryParams("task-123", null);
            A2AError error = new TaskNotFoundError();
            when(delegate.onGetTask(params, context)).thenThrow(error);

            assertThrows(TaskNotFoundError.class, () -> decorator.onGetTask(params, context));

            verify(span).setAttribute(ERROR_TYPE, error.getMessage());
            verify(span).setStatus(StatusCode.ERROR, error.getMessage());
            verify(span).end();
        }
    }

    @Nested
    class ListTasksTests {
        @Test
        void onListTasks_createsSpanAndDelegatesToHandler() throws A2AError {
            ListTasksParams params = new ListTasksParams(null, null, null, null, null, null, null, "test-tenant");
            ListTasksResult result = new ListTasksResult(Collections.emptyList(), 0, 0, null);
            when(delegate.onListTasks(params, context)).thenReturn(result);

            ListTasksResult actualResult = decorator.onListTasks(params, context);

            assertEquals(result, actualResult);
            verify(tracer).spanBuilder(A2AMethods.LIST_TASK_METHOD);
            verify(spanBuilder).setAttribute(GENAI_REQUEST, params.toString());
            verify(span).setAttribute(GENAI_RESPONSE, result.toString());
            verify(span).setStatus(StatusCode.OK);
            verify(span).end();
        }

        @Test
        void onListTasks_withError_setsErrorStatus() throws A2AError {
            ListTasksParams params = new ListTasksParams(null, null, null, null, null, null, null, "test-tenant");
            A2AError error = new InvalidRequestError("Invalid parameters");
            when(delegate.onListTasks(params, context)).thenThrow(error);

            assertThrows(InvalidRequestError.class, () -> decorator.onListTasks(params, context));

            verify(span).setAttribute(ERROR_TYPE, error.getMessage());
            verify(span).setStatus(StatusCode.ERROR, error.getMessage());
            verify(span).end();
        }
    }

    @Nested
    class CancelTaskTests {
        @Test
        void onCancelTask_createsSpanAndDelegatesToHandler() throws A2AError {
            TaskIdParams params = new TaskIdParams("task-123");
            Task result = Task.builder()
                    .id("task-123")
                    .contextId("ctx-1")
                    .status(new TaskStatus(TaskState.CANCELED))
                    .history(Collections.emptyList())
                    .artifacts(Collections.emptyList())
                    .build();
            when(delegate.onCancelTask(params, context)).thenReturn(result);

            Task actualResult = decorator.onCancelTask(params, context);

            assertEquals(result, actualResult);
            verify(tracer).spanBuilder(A2AMethods.CANCEL_TASK_METHOD);
            verify(spanBuilder).setAttribute(GENAI_REQUEST, params.toString());
            verify(span).setAttribute(GENAI_RESPONSE, result.toString());
            verify(span).setStatus(StatusCode.OK);
            verify(span).end();
        }

        @Test
        void onCancelTask_withError_setsErrorStatus() throws A2AError {
            TaskIdParams params = new TaskIdParams("task-123");
            A2AError error = new TaskNotFoundError();
            when(delegate.onCancelTask(params, context)).thenThrow(error);

            assertThrows(TaskNotFoundError.class, () -> decorator.onCancelTask(params, context));

            verify(span).setAttribute(ERROR_TYPE, error.getMessage());
            verify(span).setStatus(StatusCode.ERROR, error.getMessage());
            verify(span).end();
        }
    }

    @Nested
    class MessageSendTests {
        @Test
        void onMessageSend_createsSpanAndDelegatesToHandler() throws A2AError {
            Message message = Message.builder()
                    .role(Message.Role.USER)
                    .parts(List.of(new TextPart("test message")))
                    .messageId("msg-123")
                    .contextId("ctx-1")
                    .taskId("task-123")
                    .build();
            MessageSendParams params = new MessageSendParams(message, null, null, "");
            EventKind result = Task.builder()
                    .id("task-123")
                    .contextId("ctx-1")
                    .status(new TaskStatus(TaskState.COMPLETED))
                    .history(Collections.emptyList())
                    .artifacts(Collections.emptyList())
                    .build();
            when(delegate.onMessageSend(params, context)).thenReturn(result);

            EventKind actualResult = decorator.onMessageSend(params, context);

            assertEquals(result, actualResult);
            verify(tracer).spanBuilder(A2AMethods.SEND_MESSAGE_METHOD);
            verify(spanBuilder).setAttribute(GENAI_REQUEST, params.toString());
            verify(span).setAttribute(GENAI_RESPONSE, result.toString());
            verify(span).setStatus(StatusCode.OK);
        }

        @Test
        void onMessageSend_withError_setsErrorStatus() throws A2AError {
            Message message = Message.builder()
                    .role(Message.Role.USER)
                    .parts(List.of(new TextPart("test message")))
                    .messageId("msg-123")
                    .contextId("ctx-1")
                    .taskId("task-123")
                    .build();
            MessageSendParams params = new MessageSendParams(message, null, null, "");
            A2AError error = new InvalidRequestError("Invalid message");
            when(delegate.onMessageSend(params, context)).thenThrow(error);

            assertThrows(InvalidRequestError.class, () -> decorator.onMessageSend(params, context));

            verify(span).setAttribute(ERROR_TYPE, error.getMessage());
            verify(span).setStatus(StatusCode.ERROR, error.getMessage());
            verify(span).end();
        }
    }

    @Nested
    class MessageSendStreamTests {
        @Test
        void onMessageSendStream_createsSpanWithSpecialMessage() throws A2AError {
            Message message = Message.builder()
                    .role(Message.Role.USER)
                    .parts(List.of(new TextPart("test message")))
                    .messageId("msg-123")
                    .contextId("ctx-1")
                    .taskId("task-123")
                    .build();
            MessageSendParams params = new MessageSendParams(message, null, null, "");
            Flow.Publisher<StreamingEventKind> publisher = mock(Flow.Publisher.class);
            when(delegate.onMessageSendStream(params, context)).thenReturn(publisher);

            Flow.Publisher<StreamingEventKind> actualResult = decorator.onMessageSendStream(params, context);

            assertEquals(publisher, actualResult);
            verify(tracer).spanBuilder(A2AMethods.SEND_STREAMING_MESSAGE_METHOD);
            verify(spanBuilder).setAttribute(GENAI_REQUEST, params.toString());
            verify(span).setAttribute(GENAI_RESPONSE, "Stream publisher created");
            verify(span).setStatus(StatusCode.OK);
        }

        @Test
        void onMessageSendStream_withError_setsErrorStatus() throws A2AError {
            Message message = Message.builder()
                    .role(Message.Role.USER)
                    .parts(List.of(new TextPart("test message")))
                    .messageId("msg-123")
                    .contextId("ctx-1")
                    .taskId("task-123")
                    .build();
            MessageSendParams params = new MessageSendParams(message, null, null, "");
            A2AError error = new InvalidRequestError("Stream error");
            when(delegate.onMessageSendStream(params, context)).thenThrow(error);

            assertThrows(InvalidRequestError.class, () -> decorator.onMessageSendStream(params, context));

            verify(span).setAttribute(ERROR_TYPE, error.getMessage());
            verify(span).setStatus(StatusCode.ERROR, error.getMessage());
            verify(span).end();
        }
    }

    @Nested
    class SetTaskPushNotificationConfigTests {
        @Test
        void onSetTaskPushNotificationConfig_createsSpanAndDelegatesToHandler() throws A2AError {
            PushNotificationConfig config = new PushNotificationConfig("http://example.com", null, null, "config-1");
            TaskPushNotificationConfig params = new TaskPushNotificationConfig("task-123", config, null);
            TaskPushNotificationConfig result = new TaskPushNotificationConfig("task-123", config, null);
            when(delegate.onCreateTaskPushNotificationConfig(params, context)).thenReturn(result);

            TaskPushNotificationConfig actualResult = decorator.onCreateTaskPushNotificationConfig(params, context);

            assertEquals(result, actualResult);
            verify(tracer).spanBuilder(A2AMethods.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
            verify(spanBuilder).setAttribute(GENAI_REQUEST, params.toString());
            verify(span).setAttribute(GENAI_RESPONSE, result.toString());
            verify(span).setStatus(StatusCode.OK);
            verify(span).end();
        }

        @Test
        void onSetTaskPushNotificationConfig_withError_setsErrorStatus() throws A2AError {
            PushNotificationConfig config = new PushNotificationConfig("http://example.com", null, null, "config-1");
            TaskPushNotificationConfig params = new TaskPushNotificationConfig("task-123", config, null);
            A2AError error = new InvalidRequestError("Invalid config");
            when(delegate.onCreateTaskPushNotificationConfig(params, context)).thenThrow(error);

            assertThrows(InvalidRequestError.class, () -> decorator.onCreateTaskPushNotificationConfig(params, context));

            verify(span).setAttribute(ERROR_TYPE, error.getMessage());
            verify(span).setStatus(StatusCode.ERROR, error.getMessage());
            verify(span).end();
        }
    }

    @Nested
    class GetTaskPushNotificationConfigTests {
        @Test
        void onGetTaskPushNotificationConfig_createsSpanAndDelegatesToHandler() throws A2AError {
            GetTaskPushNotificationConfigParams params = new GetTaskPushNotificationConfigParams("task-123", "config-1");
            PushNotificationConfig config = new PushNotificationConfig("http://example.com", null, null, "config-1");
            TaskPushNotificationConfig result = new TaskPushNotificationConfig("task-123", config, null);
            when(delegate.onGetTaskPushNotificationConfig(params, context)).thenReturn(result);

            TaskPushNotificationConfig actualResult = decorator.onGetTaskPushNotificationConfig(params, context);

            assertEquals(result, actualResult);
            verify(tracer).spanBuilder(A2AMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
            verify(spanBuilder).setAttribute(GENAI_REQUEST, params.toString());
            verify(span).setAttribute(GENAI_RESPONSE, result.toString());
            verify(span).setStatus(StatusCode.OK);
            verify(span).end();
        }

        @Test
        void onGetTaskPushNotificationConfig_withError_setsErrorStatus() throws A2AError {
            GetTaskPushNotificationConfigParams params = new GetTaskPushNotificationConfigParams("task-123", "");
            A2AError error = new TaskNotFoundError();
            when(delegate.onGetTaskPushNotificationConfig(params, context)).thenThrow(error);

            assertThrows(TaskNotFoundError.class, () -> decorator.onGetTaskPushNotificationConfig(params, context));

            verify(span).setAttribute(ERROR_TYPE, error.getMessage());
            verify(span).setStatus(StatusCode.ERROR, error.getMessage());
            verify(span).end();
        }
    }

    @Nested
    class ResubscribeToTaskTests {
        @Test
        void onResubscribeToTask_createsSpanWithSpecialMessage() throws A2AError {
            TaskIdParams params = new TaskIdParams("task-123");
            Flow.Publisher<StreamingEventKind> publisher = mock(Flow.Publisher.class);
            when(delegate.onSubscribeToTask(params, context)).thenReturn(publisher);

            Flow.Publisher<StreamingEventKind> actualResult = decorator.onSubscribeToTask(params, context);

            assertEquals(publisher, actualResult);
            verify(tracer).spanBuilder(A2AMethods.SUBSCRIBE_TO_TASK_METHOD);
            verify(spanBuilder).setAttribute(GENAI_REQUEST, params.toString());
            verify(span).setAttribute(GENAI_RESPONSE, "Stream publisher created");
            verify(span).setStatus(StatusCode.OK);
        }

        @Test
        void onResubscribeToTask_withError_setsErrorStatus() throws A2AError {
            TaskIdParams params = new TaskIdParams("task-123");
            A2AError error = new TaskNotFoundError();
            when(delegate.onSubscribeToTask(params, context)).thenThrow(error);

            assertThrows(TaskNotFoundError.class, () -> decorator.onSubscribeToTask(params, context));

            verify(span).setAttribute(ERROR_TYPE, error.getMessage());
            verify(span).setStatus(StatusCode.ERROR, error.getMessage());
            verify(span).end();
        }
    }

    @Nested
    class ListTaskPushNotificationConfigTests {
        @Test
        void onListTaskPushNotificationConfig_createsSpanAndDelegatesToHandler() throws A2AError {
            ListTaskPushNotificationConfigParams params = new ListTaskPushNotificationConfigParams("task-123");
            ListTaskPushNotificationConfigResult result = new ListTaskPushNotificationConfigResult(Collections.emptyList(), null);
            when(delegate.onListTaskPushNotificationConfig(params, context)).thenReturn(result);

            ListTaskPushNotificationConfigResult actualResult = decorator.onListTaskPushNotificationConfig(params, context);

            assertEquals(result, actualResult);
            verify(tracer).spanBuilder(A2AMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
            verify(spanBuilder).setAttribute(GENAI_REQUEST, params.toString());
            verify(span).setAttribute(GENAI_RESPONSE, result.toString());
            verify(span).setStatus(StatusCode.OK);
            verify(span).end();
        }

        @Test
        void onListTaskPushNotificationConfig_withError_setsErrorStatus() throws A2AError {
            ListTaskPushNotificationConfigParams params = new ListTaskPushNotificationConfigParams("task-123");
            A2AError error = new InvalidRequestError("Invalid request");
            when(delegate.onListTaskPushNotificationConfig(params, context)).thenThrow(error);

            assertThrows(InvalidRequestError.class, () -> decorator.onListTaskPushNotificationConfig(params, context));

            verify(span).setAttribute(ERROR_TYPE, error.getMessage());
            verify(span).setStatus(StatusCode.ERROR, error.getMessage());
            verify(span).end();
        }
    }

    @Nested
    class DeleteTaskPushNotificationConfigTests {
        @Test
        void onDeleteTaskPushNotificationConfig_createsSpanAndDelegatesToHandler() throws A2AError {
            DeleteTaskPushNotificationConfigParams params = new DeleteTaskPushNotificationConfigParams("task-123", "config-123");
            doNothing().when(delegate).onDeleteTaskPushNotificationConfig(params, context);

            decorator.onDeleteTaskPushNotificationConfig(params, context);

            verify(tracer).spanBuilder(A2AMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
            verify(spanBuilder).setAttribute(GENAI_REQUEST, params.toString());
            verify(span).setStatus(StatusCode.OK);
            verify(span, never()).setAttribute(eq(GENAI_RESPONSE), anyString());
            verify(span).end();
        }

        @Test
        void onDeleteTaskPushNotificationConfig_withError_setsErrorStatus() throws A2AError {
            DeleteTaskPushNotificationConfigParams params = new DeleteTaskPushNotificationConfigParams("task-123", "config-123");
            A2AError error = new TaskNotFoundError();
            doThrow(error).when(delegate).onDeleteTaskPushNotificationConfig(params, context);

            assertThrows(TaskNotFoundError.class, () -> decorator.onDeleteTaskPushNotificationConfig(params, context));

            verify(span).setAttribute(ERROR_TYPE, error.getMessage());
            verify(span).setStatus(StatusCode.ERROR, error.getMessage());
            verify(span).end();
        }
    }

    @Nested
    class SpanLifecycleTests {
        @Test
        void allMethods_createAndEndSpans() throws A2AError {
            TaskQueryParams params = new TaskQueryParams("task-123", null);
            Task result = Task.builder()
                    .id("task-123")
                    .contextId("ctx-1")
                    .status(new TaskStatus(TaskState.COMPLETED))
                    .history(Collections.emptyList())
                    .artifacts(Collections.emptyList())
                    .build();
            when(delegate.onGetTask(params, context)).thenReturn(result);

            decorator.onGetTask(params, context);

            verify(span, times(1)).makeCurrent();
            verify(span, times(1)).end();
        }

        @Test
        void spanAttributes_setCorrectly() throws A2AError {
            TaskQueryParams params = new TaskQueryParams("task-123", null);
            Task result = Task.builder()
                    .id("task-123")
                    .contextId("ctx-1")
                    .status(new TaskStatus(TaskState.COMPLETED))
                    .history(Collections.emptyList())
                    .artifacts(Collections.emptyList())
                    .build();
            when(delegate.onGetTask(params, context)).thenReturn(result);

            decorator.onGetTask(params, context);

            verify(tracer).spanBuilder(A2AMethods.GET_TASK_METHOD);
            verify(spanBuilder).setSpanKind(SpanKind.SERVER);
            verify(spanBuilder).setAttribute(GENAI_REQUEST, params.toString());
            verify(span).setAttribute(GENAI_RESPONSE, result.toString());
            verify(span).setStatus(StatusCode.OK);
        }
    }
}
