package org.a2aproject.sdk.extras.opentelemetry.server;

import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.ERROR_TYPE;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.EXTRACT_REQUEST_SYS_PROPERTY;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.EXTRACT_RESPONSE_SYS_PROPERTY;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_REQUEST;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_RESPONSE;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.STREAMING_END_SUFFIX;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.STREAMING_EVENT_SUFFIX;
import static org.a2aproject.sdk.extras.opentelemetry.OtelSpanMocks.mockClosingSpanBuilder;
import static org.a2aproject.sdk.extras.opentelemetry.OtelSpanMocks.mockEventSpanBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.DoubleHistogramBuilder;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;
import org.a2aproject.sdk.extras.opentelemetry.ProtocolLogEmitter;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.TaskOperation;
import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.A2AMethods;
import org.a2aproject.sdk.spec.CancelTaskParams;
import org.a2aproject.sdk.spec.DeleteTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.EventKind;
import org.a2aproject.sdk.spec.GetTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.InvalidRequestError;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsParams;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsResult;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.StreamingEventKind;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskIdParams;
import org.a2aproject.sdk.spec.TaskNotFoundError;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.a2aproject.sdk.spec.TextPart;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_PROTOCOL_VERSION;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_TRANSPORT;

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
    private SpanContext spanContext;

    @Mock
    private ServerCallContext context;

    @Mock
    private RequestHandler delegate;

    @Mock
    private Meter meter;

    @Mock
    private DoubleHistogramBuilder histogramBuilder;

    @Mock
    private DoubleHistogram streamingHistogram;

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
        lenient().when(spanBuilder.addLink(any(SpanContext.class))).thenReturn(spanBuilder);
        lenient().when(spanBuilder.startSpan()).thenReturn(span);
        lenient().when(span.makeCurrent()).thenReturn(scope);
        lenient().when(span.getSpanContext()).thenReturn(spanContext);
        lenient().when(span.setAttribute(anyString(), anyString())).thenReturn(span);
        lenient().when(span.setStatus(any(StatusCode.class))).thenReturn(span);
        lenient().when(span.setStatus(any(StatusCode.class), anyString())).thenReturn(span);

        // Set up meter mock chain
        lenient().when(meter.histogramBuilder(anyString())).thenReturn(histogramBuilder);
        lenient().when(histogramBuilder.setUnit(anyString())).thenReturn(histogramBuilder);
        lenient().when(histogramBuilder.setDescription(anyString())).thenReturn(histogramBuilder);
        lenient().when(histogramBuilder.build()).thenReturn(streamingHistogram);

        // Return an empty state map by default so applyTransportProtocol doesn't NPE
        lenient().when(context.getState()).thenReturn(new HashMap<>());

        // Create decorator with mocked dependencies
        decorator = new TestableOpenTelemetryRequestHandlerDecorator(delegate, tracer, meter);
    }

    /**
     * Concrete test implementation of the abstract decorator for testing purposes.
     */
    static class TestableOpenTelemetryRequestHandlerDecorator extends OpenTelemetryRequestHandlerDecorator {
        public TestableOpenTelemetryRequestHandlerDecorator(RequestHandler delegate, Tracer tracer) {
            super(delegate, tracer);
        }

        public TestableOpenTelemetryRequestHandlerDecorator(RequestHandler delegate, Tracer tracer, Meter meter) {
            super(delegate, tracer, meter);
        }

        public TestableOpenTelemetryRequestHandlerDecorator(RequestHandler delegate, Tracer tracer, Meter meter,
                ProtocolLogEmitter emitter) {
            super(delegate, tracer, meter, emitter);
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
                    .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED))
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
            CancelTaskParams params = new CancelTaskParams("task-123");
            Task result = Task.builder()
                    .id("task-123")
                    .contextId("ctx-1")
                    .status(new TaskStatus(TaskState.TASK_STATE_CANCELED))
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
            CancelTaskParams params = new CancelTaskParams("task-123");
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
        void onMessageSend_ignoresFailureInResponseLogging() throws A2AError {
            MessageSendParams params = new MessageSendParams(Message.builder().role(Message.Role.ROLE_USER)
                    .parts(List.of(new TextPart("message"))).messageId("id").build(), null, null, "");
            EventKind response = mock(EventKind.class);
            when(delegate.onMessageSend(params, context)).thenReturn(response);
            TestableOpenTelemetryRequestHandlerDecorator withLogs = new TestableOpenTelemetryRequestHandlerDecorator(delegate, tracer, meter,
                    mock(ProtocolLogEmitter.class));

            assertEquals(response, withLogs.onMessageSend(params, context));
        }

        @Test
        void onMessageSend_emitsRequestAndResponseWhenEnabled() throws A2AError {
            Message message = Message.builder().role(Message.Role.ROLE_USER)
                    .parts(List.of(new TextPart("message"))).messageId("id").build();
            MessageSendParams params = new MessageSendParams(message, null, null, "");
            when(delegate.onMessageSend(params, context)).thenReturn(message);
            ProtocolLogEmitter emitter = mock(ProtocolLogEmitter.class);
            TestableOpenTelemetryRequestHandlerDecorator withLogs = new TestableOpenTelemetryRequestHandlerDecorator(delegate, tracer, meter, emitter);

            assertEquals(message, withLogs.onMessageSend(params, context));

            verify(emitter).emit("a2a.task.request", A2AMethods.SEND_MESSAGE_METHOD, "inbound", null, params);
            verify(emitter).emitResponse("a2a.task.response", A2AMethods.SEND_MESSAGE_METHOD, "outbound", message);
        }

        @Test
        void onMessageSend_createsSpanAndDelegatesToHandler() throws A2AError {
            Message message = Message.builder()
                    .role(Message.Role.ROLE_USER)
                    .parts(List.of(new TextPart("test message")))
                    .messageId("msg-123")
                    .contextId("ctx-1")
                    .taskId("task-123")
                    .build();
            MessageSendParams params = new MessageSendParams(message, null, null, "");
            EventKind result = Task.builder()
                    .id("task-123")
                    .contextId("ctx-1")
                    .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED))
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
                    .role(Message.Role.ROLE_USER)
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
        @SuppressWarnings("unchecked")
        void streamEmitsEventAndOnlyOneTerminalLog() throws A2AError {
            Message message = Message.builder().role(Message.Role.ROLE_USER)
                    .parts(List.of(new TextPart("message"))).messageId("id").build();
            MessageSendParams params = new MessageSendParams(message, null, null, "");
            Flow.Publisher<StreamingEventKind> publisher = mock(Flow.Publisher.class);
            when(delegate.onMessageSendStream(params, context)).thenReturn(publisher);
            mockEventSpanBuilder(tracer, A2AMethods.SEND_STREAMING_MESSAGE_METHOD + STREAMING_EVENT_SUFFIX, mock(Span.class));
            mockClosingSpanBuilder(tracer, A2AMethods.SEND_STREAMING_MESSAGE_METHOD + STREAMING_END_SUFFIX, mock(Span.class));
            ProtocolLogEmitter emitter = mock(ProtocolLogEmitter.class);
            TestableOpenTelemetryRequestHandlerDecorator withLogs = new TestableOpenTelemetryRequestHandlerDecorator(delegate, tracer, meter, emitter);

            withLogs.onMessageSendStream(params, context).subscribe(mock(Flow.Subscriber.class));
            ArgumentCaptor<Flow.Subscriber> subscriber = ArgumentCaptor.forClass(Flow.Subscriber.class);
            verify(publisher).subscribe(subscriber.capture());
            subscriber.getValue().onNext(message);
            subscriber.getValue().onComplete();
            subscriber.getValue().onComplete();

            verify(emitter).emit("a2a.task.request", A2AMethods.SEND_STREAMING_MESSAGE_METHOD, "inbound", null, params);
            verify(emitter).emit("a2a.task.event", A2AMethods.SEND_STREAMING_MESSAGE_METHOD, "outbound", message.kind(), message);
            verify(emitter, times(1)).emit("a2a.task.stream.end", A2AMethods.SEND_STREAMING_MESSAGE_METHOD,
                    "outbound", "complete", null);
        }

        @Test
        void onMessageSendStream_createsSpanAndWrapsPublisher() throws A2AError {
            Message message = Message.builder()
                    .role(Message.Role.ROLE_USER)
                    .parts(List.of(new TextPart("test message")))
                    .messageId("msg-123")
                    .contextId("ctx-1")
                    .taskId("task-123")
                    .build();
            MessageSendParams params = new MessageSendParams(message, null, null, "");
            Flow.Publisher<StreamingEventKind> publisher = mock(Flow.Publisher.class);
            when(delegate.onMessageSendStream(params, context)).thenReturn(publisher);

            Flow.Publisher<StreamingEventKind> actualResult = decorator.onMessageSendStream(params, context);

            assertNotNull(actualResult);
            verify(tracer).spanBuilder(A2AMethods.SEND_STREAMING_MESSAGE_METHOD);
            verify(spanBuilder).setAttribute(GENAI_REQUEST, params.toString());
            verify(span).setAttribute(GENAI_RESPONSE, "Stream publisher created");
            verify(span).setStatus(StatusCode.OK);
            verify(span).end();
        }

        @Test
        void onMessageSendStream_withError_setsErrorStatus() throws A2AError {
            Message message = Message.builder()
                    .role(Message.Role.ROLE_USER)
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
            verify(streamingHistogram).record(anyDouble(), any(Attributes.class));
        }

        @Test
        @SuppressWarnings("unchecked")
        void onMessageSendStream_closingSpan_createdOnComplete() throws A2AError {
            Message message = Message.builder()
                    .role(Message.Role.ROLE_USER)
                    .parts(List.of(new TextPart("test message")))
                    .messageId("msg-123")
                    .build();
            MessageSendParams params = new MessageSendParams(message, null, null, "");
            Flow.Publisher<StreamingEventKind> publisher = mock(Flow.Publisher.class);
            when(delegate.onMessageSendStream(params, context)).thenReturn(publisher);

            Span closingSpan = mock(Span.class);
            SpanBuilder closingSpanBuilder = mockClosingSpanBuilder(tracer,
                    A2AMethods.SEND_STREAMING_MESSAGE_METHOD + STREAMING_END_SUFFIX, closingSpan);

            Flow.Publisher<StreamingEventKind> result = decorator.onMessageSendStream(params, context);

            Flow.Subscriber<StreamingEventKind> testSubscriber = mock(Flow.Subscriber.class);
            result.subscribe(testSubscriber);

            ArgumentCaptor<Flow.Subscriber> subscriberCaptor = ArgumentCaptor.forClass(Flow.Subscriber.class);
            verify(publisher).subscribe(subscriberCaptor.capture());
            Flow.Subscriber<StreamingEventKind> otelSubscriber = (Flow.Subscriber<StreamingEventKind>) subscriberCaptor.getValue();

            otelSubscriber.onComplete();

            verify(tracer).spanBuilder(A2AMethods.SEND_STREAMING_MESSAGE_METHOD + STREAMING_END_SUFFIX);
            verify(closingSpanBuilder).setParent(any(Context.class));
            verify(closingSpan).setStatus(StatusCode.OK);
            verify(closingSpan).end();
            verify(testSubscriber).onComplete();
            verify(streamingHistogram).record(anyDouble(), any(Attributes.class));
        }

        @Test
        @SuppressWarnings("unchecked")
        void onMessageSendStream_closingSpan_recordsEventsAndEndsOnError() throws A2AError {
            Message message = Message.builder()
                    .role(Message.Role.ROLE_USER)
                    .parts(List.of(new TextPart("test message")))
                    .messageId("msg-123")
                    .build();
            MessageSendParams params = new MessageSendParams(message, null, null, "");
            Flow.Publisher<StreamingEventKind> publisher = mock(Flow.Publisher.class);
            when(delegate.onMessageSendStream(params, context)).thenReturn(publisher);
            StreamingEventKind eventItem = Message.builder()
                    .role(Message.Role.ROLE_AGENT)
                    .parts(List.of(new TextPart("event-data")))
                    .messageId("event-msg-1")
                    .build();

            Span eventSpan = mock(Span.class);
            SpanBuilder eventSpanBuilder = mockEventSpanBuilder(tracer,
                    A2AMethods.SEND_STREAMING_MESSAGE_METHOD + STREAMING_EVENT_SUFFIX, eventSpan);

            Span closingSpan = mock(Span.class);
            mockClosingSpanBuilder(tracer, A2AMethods.SEND_STREAMING_MESSAGE_METHOD + STREAMING_END_SUFFIX, closingSpan);

            Flow.Publisher<StreamingEventKind> result = decorator.onMessageSendStream(params, context);

            Flow.Subscriber<StreamingEventKind> testSubscriber = mock(Flow.Subscriber.class);
            result.subscribe(testSubscriber);

            ArgumentCaptor<Flow.Subscriber> subscriberCaptor = ArgumentCaptor.forClass(Flow.Subscriber.class);
            verify(publisher).subscribe(subscriberCaptor.capture());
            Flow.Subscriber<StreamingEventKind> otelSubscriber = (Flow.Subscriber<StreamingEventKind>) subscriberCaptor.getValue();

            otelSubscriber.onNext(eventItem);
            Throwable error = new RuntimeException("stream failure");
            otelSubscriber.onError(error);

            verify(testSubscriber).onNext(eventItem);
            verify(tracer).spanBuilder(A2AMethods.SEND_STREAMING_MESSAGE_METHOD + STREAMING_EVENT_SUFFIX);
            verify(eventSpanBuilder).setSpanKind(SpanKind.SERVER);
            verify(eventSpan).end();
            verify(closingSpan).setAttribute(ERROR_TYPE, RuntimeException.class.getName());
            verify(closingSpan).setStatus(StatusCode.ERROR, "stream failure");
            verify(closingSpan).end();
            verify(testSubscriber).onError(error);
            verify(streamingHistogram).record(anyDouble(), any(Attributes.class));
        }

        @Test
        @SuppressWarnings("unchecked")
        void onMessageSendStream_closingSpan_createdOnSubscriptionCancel() throws A2AError {
            Message message = Message.builder()
                    .role(Message.Role.ROLE_USER)
                    .parts(List.of(new TextPart("test message")))
                    .messageId("msg-123")
                    .build();
            MessageSendParams params = new MessageSendParams(message, null, null, "");
            Flow.Publisher<StreamingEventKind> publisher = mock(Flow.Publisher.class);
            when(delegate.onMessageSendStream(params, context)).thenReturn(publisher);

            Span closingSpan = mock(Span.class);
            mockClosingSpanBuilder(tracer, A2AMethods.SEND_STREAMING_MESSAGE_METHOD + STREAMING_END_SUFFIX, closingSpan);

            Flow.Publisher<StreamingEventKind> result = decorator.onMessageSendStream(params, context);

            Flow.Subscriber<StreamingEventKind> testSubscriber = mock(Flow.Subscriber.class);
            result.subscribe(testSubscriber);

            ArgumentCaptor<Flow.Subscriber> subscriberCaptor = ArgumentCaptor.forClass(Flow.Subscriber.class);
            verify(publisher).subscribe(subscriberCaptor.capture());
            Flow.Subscriber<StreamingEventKind> otelSubscriber = (Flow.Subscriber<StreamingEventKind>) subscriberCaptor.getValue();

            // Simulate onSubscribe — the OTel subscriber wraps the subscription before passing it down
            Flow.Subscription rawSubscription = mock(Flow.Subscription.class);
            otelSubscriber.onSubscribe(rawSubscription);

            // Capture the wrapped subscription that was forwarded to the downstream subscriber
            ArgumentCaptor<Flow.Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Flow.Subscription.class);
            verify(testSubscriber).onSubscribe(subscriptionCaptor.capture());

            // Cancel via the wrapped subscription — this must produce the terminal span and histogram
            subscriptionCaptor.getValue().cancel();

            verify(tracer).spanBuilder(A2AMethods.SEND_STREAMING_MESSAGE_METHOD + STREAMING_END_SUFFIX);
            verify(closingSpan).setStatus(StatusCode.OK);
            verify(closingSpan).end();
            verify(streamingHistogram).record(anyDouble(), any(Attributes.class));
            // The underlying subscription must also be cancelled
            verify(rawSubscription).cancel();
        }

        @Test
        @SuppressWarnings("unchecked")
        void onMessageSendStream_closingSpan_cancelledTwiceOnlyEmitsOneSpan() throws A2AError {
            Message message = Message.builder()
                    .role(Message.Role.ROLE_USER)
                    .parts(List.of(new TextPart("test message")))
                    .messageId("msg-123")
                    .build();
            MessageSendParams params = new MessageSendParams(message, null, null, "");
            Flow.Publisher<StreamingEventKind> publisher = mock(Flow.Publisher.class);
            when(delegate.onMessageSendStream(params, context)).thenReturn(publisher);

            Span closingSpan = mock(Span.class);
            mockClosingSpanBuilder(tracer, A2AMethods.SEND_STREAMING_MESSAGE_METHOD + STREAMING_END_SUFFIX, closingSpan);

            Flow.Publisher<StreamingEventKind> result = decorator.onMessageSendStream(params, context);

            Flow.Subscriber<StreamingEventKind> testSubscriber = mock(Flow.Subscriber.class);
            result.subscribe(testSubscriber);

            ArgumentCaptor<Flow.Subscriber> subscriberCaptor = ArgumentCaptor.forClass(Flow.Subscriber.class);
            verify(publisher).subscribe(subscriberCaptor.capture());
            Flow.Subscriber<StreamingEventKind> otelSubscriber = (Flow.Subscriber<StreamingEventKind>) subscriberCaptor.getValue();

            Flow.Subscription rawSubscription = mock(Flow.Subscription.class);
            otelSubscriber.onSubscribe(rawSubscription);

            ArgumentCaptor<Flow.Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Flow.Subscription.class);
            verify(testSubscriber).onSubscribe(subscriptionCaptor.capture());

            subscriptionCaptor.getValue().cancel();
            subscriptionCaptor.getValue().cancel();

            // Terminal span created exactly once despite two cancels
            verify(closingSpan, times(1)).end();
            verify(streamingHistogram, times(1)).record(anyDouble(), any(Attributes.class));
            verify(rawSubscription, times(2)).cancel();
        }

        @Test
        @SuppressWarnings("unchecked")
        void onMessageSendStream_closingSpan_recordsMultipleEventsOnComplete() throws A2AError {
            Message message = Message.builder()
                    .role(Message.Role.ROLE_USER)
                    .parts(List.of(new TextPart("test message")))
                    .messageId("msg-123")
                    .build();
            MessageSendParams params = new MessageSendParams(message, null, null, "");
            Flow.Publisher<StreamingEventKind> publisher = mock(Flow.Publisher.class);
            when(delegate.onMessageSendStream(params, context)).thenReturn(publisher);
            StreamingEventKind event1 = Message.builder()
                    .role(Message.Role.ROLE_AGENT)
                    .parts(List.of(new TextPart("first event")))
                    .messageId("event-1")
                    .build();
            StreamingEventKind event2 = Message.builder()
                    .role(Message.Role.ROLE_AGENT)
                    .parts(List.of(new TextPart("second event")))
                    .messageId("event-2")
                    .build();

            Span eventSpan = mock(Span.class);
            SpanBuilder eventSpanBuilder = mockEventSpanBuilder(tracer,
                    A2AMethods.SEND_STREAMING_MESSAGE_METHOD + STREAMING_EVENT_SUFFIX, eventSpan);

            Span closingSpan = mock(Span.class);
            mockClosingSpanBuilder(tracer, A2AMethods.SEND_STREAMING_MESSAGE_METHOD + STREAMING_END_SUFFIX, closingSpan);

            Flow.Publisher<StreamingEventKind> result = decorator.onMessageSendStream(params, context);

            Flow.Subscriber<StreamingEventKind> testSubscriber = mock(Flow.Subscriber.class);
            result.subscribe(testSubscriber);

            ArgumentCaptor<Flow.Subscriber> subscriberCaptor = ArgumentCaptor.forClass(Flow.Subscriber.class);
            verify(publisher).subscribe(subscriberCaptor.capture());
            Flow.Subscriber<StreamingEventKind> otelSubscriber = (Flow.Subscriber<StreamingEventKind>) subscriberCaptor.getValue();

            otelSubscriber.onNext(event1);
            otelSubscriber.onNext(event2);
            otelSubscriber.onComplete();

            verify(testSubscriber).onNext(event1);
            verify(testSubscriber).onNext(event2);
            verify(tracer, times(2)).spanBuilder(A2AMethods.SEND_STREAMING_MESSAGE_METHOD + STREAMING_EVENT_SUFFIX);
            verify(eventSpanBuilder, times(2)).setSpanKind(SpanKind.SERVER);
            verify(eventSpan, times(2)).end();
            verify(closingSpan).setStatus(StatusCode.OK);
            verify(closingSpan).end();
            verify(testSubscriber).onComplete();
            verify(streamingHistogram).record(anyDouble(), any(Attributes.class));
        }

        @Test
        @SuppressWarnings("unchecked")
        void onMessageSendStream_closingSpan_delegateThrowOnComplete_recordsExceptionAndSetsErrorStatus() throws A2AError {
            Message message = Message.builder()
                    .role(Message.Role.ROLE_USER)
                    .parts(List.of(new TextPart("test message")))
                    .messageId("msg-ex")
                    .build();
            MessageSendParams params = new MessageSendParams(message, null, null, "");
            Flow.Publisher<StreamingEventKind> publisher = mock(Flow.Publisher.class);
            when(delegate.onMessageSendStream(params, context)).thenReturn(publisher);

            Span closingSpan = mock(Span.class);
            mockClosingSpanBuilder(tracer, A2AMethods.SEND_STREAMING_MESSAGE_METHOD + STREAMING_END_SUFFIX, closingSpan);

            Flow.Publisher<StreamingEventKind> result = decorator.onMessageSendStream(params, context);

            Flow.Subscriber<StreamingEventKind> testSubscriber = mock(Flow.Subscriber.class);
            result.subscribe(testSubscriber);

            ArgumentCaptor<Flow.Subscriber> subscriberCaptor = ArgumentCaptor.forClass(Flow.Subscriber.class);
            verify(publisher).subscribe(subscriberCaptor.capture());
            Flow.Subscriber<StreamingEventKind> otelSubscriber = (Flow.Subscriber<StreamingEventKind>) subscriberCaptor.getValue();

            RuntimeException delegateError = new RuntimeException("delegate onComplete failure");
            doThrow(delegateError).when(testSubscriber).onComplete();

            assertThrows(RuntimeException.class, otelSubscriber::onComplete);

            verify(closingSpan).recordException(delegateError);
            verify(closingSpan).setStatus(StatusCode.ERROR);
            verify(closingSpan).end();
        }
    }

    @Nested
    class SetTaskPushNotificationConfigTests {
        @Test
        void onSetTaskPushNotificationConfig_emitsOutboundCompletion() throws A2AError {
            TaskPushNotificationConfig params = TaskPushNotificationConfig.builder()
                    .id("config-1").taskId("task-123").url("http://example.com").build();
            when(delegate.onCreateTaskPushNotificationConfig(params, context)).thenReturn(params);
            ProtocolLogEmitter emitter = mock(ProtocolLogEmitter.class);
            TestableOpenTelemetryRequestHandlerDecorator withLogs =
                    new TestableOpenTelemetryRequestHandlerDecorator(delegate, tracer, meter, emitter);

            withLogs.onCreateTaskPushNotificationConfig(params, context);

            verify(emitter).emitMetadata("a2a.notification.subscription",
                    A2AMethods.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, "outbound", "created", params);
        }

        @Test
        void onSetTaskPushNotificationConfig_createsSpanAndDelegatesToHandler() throws A2AError {
            TaskPushNotificationConfig params = TaskPushNotificationConfig.builder()
                    .id("config-1").taskId("task-123").url("http://example.com").build();
            TaskPushNotificationConfig result = TaskPushNotificationConfig.builder()
                    .id("config-1").taskId("task-123").url("http://example.com").build();
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
            TaskPushNotificationConfig params = TaskPushNotificationConfig.builder()
                    .id("config-1").taskId("task-123").url("http://example.com").build();
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
            TaskPushNotificationConfig result = TaskPushNotificationConfig.builder()
                    .id("config-1").taskId("task-123").url("http://example.com").build();
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
        void onResubscribeToTask_createsSpanAndWrapsPublisher() throws A2AError {
            TaskIdParams params = new TaskIdParams("task-123");
            Flow.Publisher<StreamingEventKind> publisher = mock(Flow.Publisher.class);
            when(delegate.onSubscribeToTask(params, context)).thenReturn(publisher);

            Flow.Publisher<StreamingEventKind> actualResult = decorator.onSubscribeToTask(params, context);

            assertNotNull(actualResult);
            verify(tracer).spanBuilder(A2AMethods.SUBSCRIBE_TO_TASK_METHOD);
            verify(spanBuilder).setAttribute(GENAI_REQUEST, params.toString());
            verify(span).setAttribute(GENAI_RESPONSE, "Stream publisher created");
            verify(span).setStatus(StatusCode.OK);
            verify(span).end();
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
            verify(streamingHistogram).record(anyDouble(), any(Attributes.class));
        }

        @Test
        @SuppressWarnings("unchecked")
        void onResubscribeToTask_closingSpan_createdOnComplete() throws A2AError {
            TaskIdParams params = new TaskIdParams("task-123");
            Flow.Publisher<StreamingEventKind> publisher = mock(Flow.Publisher.class);
            when(delegate.onSubscribeToTask(params, context)).thenReturn(publisher);

            Span closingSpan = mock(Span.class);
            SpanBuilder closingSpanBuilder = mockClosingSpanBuilder(tracer,
                    A2AMethods.SUBSCRIBE_TO_TASK_METHOD + STREAMING_END_SUFFIX, closingSpan);

            Flow.Publisher<StreamingEventKind> result = decorator.onSubscribeToTask(params, context);

            Flow.Subscriber<StreamingEventKind> testSubscriber = mock(Flow.Subscriber.class);
            result.subscribe(testSubscriber);

            ArgumentCaptor<Flow.Subscriber> subscriberCaptor = ArgumentCaptor.forClass(Flow.Subscriber.class);
            verify(publisher).subscribe(subscriberCaptor.capture());
            Flow.Subscriber<StreamingEventKind> otelSubscriber = (Flow.Subscriber<StreamingEventKind>) subscriberCaptor.getValue();

            otelSubscriber.onComplete();

            verify(tracer).spanBuilder(A2AMethods.SUBSCRIBE_TO_TASK_METHOD + STREAMING_END_SUFFIX);
            verify(closingSpanBuilder).setParent(any(Context.class));
            verify(closingSpan).setStatus(StatusCode.OK);
            verify(closingSpan).end();
            verify(testSubscriber).onComplete();
            verify(streamingHistogram).record(anyDouble(), any(Attributes.class));
        }

        @Test
        @SuppressWarnings("unchecked")
        void onResubscribeToTask_closingSpan_recordsEventsOnCompleteAndError() throws A2AError {
            TaskIdParams params = new TaskIdParams("task-123");
            Flow.Publisher<StreamingEventKind> publisher = mock(Flow.Publisher.class);
            when(delegate.onSubscribeToTask(params, context)).thenReturn(publisher);
            StreamingEventKind eventItem = Message.builder()
                    .role(Message.Role.ROLE_AGENT)
                    .parts(List.of(new TextPart("streamed-event")))
                    .messageId("evt-1")
                    .build();

            Span eventSpan = mock(Span.class);
            SpanBuilder eventSpanBuilder = mockEventSpanBuilder(tracer,
                    A2AMethods.SUBSCRIBE_TO_TASK_METHOD + STREAMING_EVENT_SUFFIX, eventSpan);

            Span closingSpan = mock(Span.class);
            mockClosingSpanBuilder(tracer, A2AMethods.SUBSCRIBE_TO_TASK_METHOD + STREAMING_END_SUFFIX, closingSpan);

            Flow.Publisher<StreamingEventKind> result = decorator.onSubscribeToTask(params, context);

            Flow.Subscriber<StreamingEventKind> testSubscriber = mock(Flow.Subscriber.class);
            result.subscribe(testSubscriber);

            ArgumentCaptor<Flow.Subscriber> subscriberCaptor = ArgumentCaptor.forClass(Flow.Subscriber.class);
            verify(publisher).subscribe(subscriberCaptor.capture());
            Flow.Subscriber<StreamingEventKind> otelSubscriber = (Flow.Subscriber<StreamingEventKind>) subscriberCaptor.getValue();

            otelSubscriber.onNext(eventItem);
            Throwable error = new RuntimeException("subscribe failure");
            otelSubscriber.onError(error);

            verify(testSubscriber).onNext(eventItem);
            verify(tracer).spanBuilder(A2AMethods.SUBSCRIBE_TO_TASK_METHOD + STREAMING_EVENT_SUFFIX);
            verify(eventSpanBuilder).setSpanKind(SpanKind.SERVER);
            verify(eventSpan).end();
            verify(closingSpan).setAttribute(ERROR_TYPE, RuntimeException.class.getName());
            verify(closingSpan).setStatus(StatusCode.ERROR, "subscribe failure");
            verify(closingSpan).end();
            verify(testSubscriber).onError(error);
            verify(streamingHistogram).record(anyDouble(), any(Attributes.class));
        }

        @Test
        @SuppressWarnings("unchecked")
        void onResubscribeToTask_closingSpan_createdOnSubscriptionCancel() throws A2AError {
            TaskIdParams params = new TaskIdParams("task-123");
            Flow.Publisher<StreamingEventKind> publisher = mock(Flow.Publisher.class);
            when(delegate.onSubscribeToTask(params, context)).thenReturn(publisher);

            Span closingSpan = mock(Span.class);
            mockClosingSpanBuilder(tracer, A2AMethods.SUBSCRIBE_TO_TASK_METHOD + STREAMING_END_SUFFIX, closingSpan);

            Flow.Publisher<StreamingEventKind> result = decorator.onSubscribeToTask(params, context);

            Flow.Subscriber<StreamingEventKind> testSubscriber = mock(Flow.Subscriber.class);
            result.subscribe(testSubscriber);

            ArgumentCaptor<Flow.Subscriber> subscriberCaptor = ArgumentCaptor.forClass(Flow.Subscriber.class);
            verify(publisher).subscribe(subscriberCaptor.capture());
            Flow.Subscriber<StreamingEventKind> otelSubscriber = (Flow.Subscriber<StreamingEventKind>) subscriberCaptor.getValue();

            // Simulate onSubscribe — the OTel subscriber wraps the subscription before passing it down
            Flow.Subscription rawSubscription = mock(Flow.Subscription.class);
            otelSubscriber.onSubscribe(rawSubscription);

            // Capture the wrapped subscription forwarded to the downstream subscriber
            ArgumentCaptor<Flow.Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Flow.Subscription.class);
            verify(testSubscriber).onSubscribe(subscriptionCaptor.capture());

            // Cancel via the wrapped subscription — this must produce the terminal span and histogram
            subscriptionCaptor.getValue().cancel();

            verify(tracer).spanBuilder(A2AMethods.SUBSCRIBE_TO_TASK_METHOD + STREAMING_END_SUFFIX);
            verify(closingSpan).setStatus(StatusCode.OK);
            verify(closingSpan).end();
            verify(streamingHistogram).record(anyDouble(), any(Attributes.class));
            verify(rawSubscription).cancel();
        }
    }

    @Nested
    class ListTaskPushNotificationConfigsTests {
        @Test
        void onListTaskPushNotificationConfigs_createsSpanAndDelegatesToHandler() throws A2AError {
            ListTaskPushNotificationConfigsParams params = new ListTaskPushNotificationConfigsParams("task-123");
            ListTaskPushNotificationConfigsResult result = new ListTaskPushNotificationConfigsResult(Collections.emptyList(), null);
            when(delegate.onListTaskPushNotificationConfigs(params, context)).thenReturn(result);

            ListTaskPushNotificationConfigsResult actualResult = decorator.onListTaskPushNotificationConfigs(params, context);

            assertEquals(result, actualResult);
            verify(tracer).spanBuilder(A2AMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
            verify(spanBuilder).setAttribute(GENAI_REQUEST, params.toString());
            verify(span).setAttribute(GENAI_RESPONSE, result.toString());
            verify(span).setStatus(StatusCode.OK);
            verify(span).end();
        }

        @Test
        void onListTaskPushNotificationConfigs_withError_setsErrorStatus() throws A2AError {
            ListTaskPushNotificationConfigsParams params = new ListTaskPushNotificationConfigsParams("task-123");
            A2AError error = new InvalidRequestError("Invalid request");
            when(delegate.onListTaskPushNotificationConfigs(params, context)).thenThrow(error);

            assertThrows(InvalidRequestError.class, () -> decorator.onListTaskPushNotificationConfigs(params, context));

            verify(span).setAttribute(ERROR_TYPE, error.getMessage());
            verify(span).setStatus(StatusCode.ERROR, error.getMessage());
            verify(span).end();
        }
    }

    @Nested
    class DeleteTaskPushNotificationConfigTests {
        @Test
        void onDeleteTaskPushNotificationConfig_emitsOutboundCompletion() throws A2AError {
            DeleteTaskPushNotificationConfigParams params =
                    new DeleteTaskPushNotificationConfigParams("task-123", "config-123");
            ProtocolLogEmitter emitter = mock(ProtocolLogEmitter.class);
            TestableOpenTelemetryRequestHandlerDecorator withLogs =
                    new TestableOpenTelemetryRequestHandlerDecorator(delegate, tracer, meter, emitter);

            withLogs.onDeleteTaskPushNotificationConfig(params, context);

            verify(emitter).emitMetadata("a2a.notification.subscription",
                    A2AMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, "outbound", "deleted", params);
        }

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
    class AuthorizeTaskAccessTests {
        @Test
        void authorizeTaskAccess_emitsAllowWhenGranted() throws A2AError {
            ProtocolLogEmitter emitter = mock(ProtocolLogEmitter.class);
            TestableOpenTelemetryRequestHandlerDecorator withLogs =
                    new TestableOpenTelemetryRequestHandlerDecorator(delegate, tracer, meter, emitter);

            withLogs.authorizeTaskAccess("task-123", context, TaskOperation.GET_TASK);

            verify(delegate).authorizeTaskAccess("task-123", context, TaskOperation.GET_TASK);
            verify(emitter).emitAuthorization("GET_TASK", "task-123", null);
        }

        @Test
        void authorizeTaskAccess_emitsDenyWithErrorTypeAndRethrows() throws A2AError {
            ProtocolLogEmitter emitter = mock(ProtocolLogEmitter.class);
            TestableOpenTelemetryRequestHandlerDecorator withLogs =
                    new TestableOpenTelemetryRequestHandlerDecorator(delegate, tracer, meter, emitter);
            doThrow(new TaskNotFoundError()).when(delegate)
                    .authorizeTaskAccess("task-123", context, TaskOperation.CANCEL_TASK);

            assertThrows(TaskNotFoundError.class,
                    () -> withLogs.authorizeTaskAccess("task-123", context, TaskOperation.CANCEL_TASK));

            verify(emitter).emitAuthorization("CANCEL_TASK", "task-123", "TaskNotFoundError");
        }

        @Test
        void authorizeTaskAccess_emitsDecisionForOperationsWithoutATask() throws A2AError {
            ProtocolLogEmitter emitter = mock(ProtocolLogEmitter.class);
            TestableOpenTelemetryRequestHandlerDecorator withLogs =
                    new TestableOpenTelemetryRequestHandlerDecorator(delegate, tracer, meter, emitter);

            withLogs.authorizeTaskAccess(null, context, TaskOperation.LIST_TASKS);

            verify(emitter).emitAuthorization("LIST_TASKS", null, null);
        }

        @Test
        void authorizeTaskAccess_withoutEmitterStillDelegates() throws A2AError {
            decorator.authorizeTaskAccess("task-123", context, TaskOperation.GET_TASK);

            verify(delegate).authorizeTaskAccess("task-123", context, TaskOperation.GET_TASK);
        }
    }

    @Nested
    class NullMeterTests {
        @Test
        @SuppressWarnings("unchecked")
        void onMessageSendStream_withNullMeter_doesNotRecordHistogram() throws A2AError {
            TestableOpenTelemetryRequestHandlerDecorator decoratorNoMeter =
                    new TestableOpenTelemetryRequestHandlerDecorator(delegate, tracer);

            Message message = Message.builder()
                    .role(Message.Role.ROLE_USER)
                    .parts(List.of(new TextPart("test message")))
                    .messageId("msg-123")
                    .build();
            MessageSendParams params = new MessageSendParams(message, null, null, "");
            Flow.Publisher<StreamingEventKind> publisher = mock(Flow.Publisher.class);
            when(delegate.onMessageSendStream(params, context)).thenReturn(publisher);

            Span closingSpan = mock(Span.class);
            mockClosingSpanBuilder(tracer, A2AMethods.SEND_STREAMING_MESSAGE_METHOD + STREAMING_END_SUFFIX, closingSpan);

            Flow.Publisher<StreamingEventKind> result = decoratorNoMeter.onMessageSendStream(params, context);

            Flow.Subscriber<StreamingEventKind> testSubscriber = mock(Flow.Subscriber.class);
            result.subscribe(testSubscriber);

            ArgumentCaptor<Flow.Subscriber> subscriberCaptor = ArgumentCaptor.forClass(Flow.Subscriber.class);
            verify(publisher).subscribe(subscriberCaptor.capture());
            Flow.Subscriber<StreamingEventKind> otelSubscriber = (Flow.Subscriber<StreamingEventKind>) subscriberCaptor.getValue();

            otelSubscriber.onComplete();

            verify(closingSpan).setStatus(StatusCode.OK);
            verify(closingSpan).end();
            verify(testSubscriber).onComplete();
            verify(streamingHistogram, never()).record(anyDouble(), any(Attributes.class));
        }
    }

    @Nested
    class TransportProtocolTests {
        @Test
        void onMessageSend_setsGenaiProtocolAttribute_whenTransportKeyPresent() throws A2AError {
            Map<String, Object> state = new HashMap<>();
            state.put(ServerCallContext.TRANSPORT_KEY, TransportProtocol.JSONRPC);
            when(context.getState()).thenReturn(state);
            when(context.getRequestedProtocolVersion()).thenReturn(null);

            Message message = Message.builder()
                    .role(Message.Role.ROLE_USER)
                    .parts(List.of(new TextPart("test")))
                    .messageId("msg-1")
                    .build();
            MessageSendParams params = new MessageSendParams(message, null, null, "");
            when(delegate.onMessageSend(params, context)).thenReturn(mock(EventKind.class));

            decorator.onMessageSend(params, context);

            verify(spanBuilder).setAttribute(GENAI_TRANSPORT, TransportProtocol.JSONRPC.asString());
            verify(spanBuilder, never()).setAttribute(eq(GENAI_PROTOCOL_VERSION), anyString());
        }

        @Test
        void onMessageSend_setsGenaiVersionAttribute_whenVersionPresent() throws A2AError {
            when(context.getRequestedProtocolVersion()).thenReturn("1.0");

            Message message = Message.builder()
                    .role(Message.Role.ROLE_USER)
                    .parts(List.of(new TextPart("test")))
                    .messageId("msg-1")
                    .build();
            MessageSendParams params = new MessageSendParams(message, null, null, "");
            when(delegate.onMessageSend(params, context)).thenReturn(mock(EventKind.class));

            decorator.onMessageSend(params, context);

            verify(spanBuilder).setAttribute(GENAI_PROTOCOL_VERSION, "1.0");
        }

        @Test
        void onMessageSend_doesNotSetGenaiProtocolAttribute_whenTransportKeyAbsent() throws A2AError {
            // context.getState() returns empty HashMap and getRequestedProtocolVersion() returns null from setUp

            Message message = Message.builder()
                    .role(Message.Role.ROLE_USER)
                    .parts(List.of(new TextPart("test")))
                    .messageId("msg-1")
                    .build();
            MessageSendParams params = new MessageSendParams(message, null, null, "");
            when(delegate.onMessageSend(params, context)).thenReturn(mock(EventKind.class));

            decorator.onMessageSend(params, context);

            verify(spanBuilder, never()).setAttribute(eq(GENAI_TRANSPORT), anyString());
            verify(spanBuilder, never()).setAttribute(eq(GENAI_PROTOCOL_VERSION), anyString());
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
                    .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED))
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
                    .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED))
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
