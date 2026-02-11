package io.a2a.extras.opentelemetry.client;

import static io.a2a.extras.opentelemetry.A2AObservabilityNames.EXTRACT_REQUEST_SYS_PROPERTY;
import static io.a2a.extras.opentelemetry.A2AObservabilityNames.EXTRACT_RESPONSE_SYS_PROPERTY;
import static io.a2a.extras.opentelemetry.A2AObservabilityNames.GENAI_REQUEST;
import static io.a2a.extras.opentelemetry.A2AObservabilityNames.GENAI_RESPONSE;

import io.a2a.client.transport.spi.ClientTransport;
import io.a2a.client.transport.spi.interceptors.ClientCallContext;
import io.a2a.jsonrpc.common.wrappers.ListTasksResult;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.EventKind;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigResult;
import io.a2a.spec.ListTasksParams;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.TextPart;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.a2a.spec.A2AMethods;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OpenTelemetryClientTransportTest {

    @Mock
    private ClientTransport delegate;

    @Mock
    private Tracer tracer;

    @Mock
    private SpanBuilder spanBuilder;

    @Mock
    private Span span;

    @Mock
    private Scope scope;

    @Mock
    private SpanContext spanContext;

    @Mock
    private ClientCallContext context;

    private OpenTelemetryClientTransport transport;

    @BeforeEach
    void setUp() {
        System.setProperty(EXTRACT_REQUEST_SYS_PROPERTY, "true");
        System.setProperty(EXTRACT_RESPONSE_SYS_PROPERTY, "true");
        when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.setSpanKind(any(SpanKind.class))).thenReturn(spanBuilder);
        when(spanBuilder.setAttribute(anyString(), anyString())).thenReturn(spanBuilder);
        when(spanBuilder.setAttribute(anyString(), anyLong())).thenReturn(spanBuilder);
        when(spanBuilder.addLink(any(SpanContext.class))).thenReturn(spanBuilder);
        when(spanBuilder.startSpan()).thenReturn(span);
        when(span.makeCurrent()).thenReturn(scope);
        when(span.getSpanContext()).thenReturn(spanContext);
        when(context.getHeaders()).thenReturn(new java.util.HashMap<>());

        transport = new OpenTelemetryClientTransport(delegate, tracer);
    }

    @Test
    void testSendMessage_Success() throws A2AClientException {
        MessageSendParams request = mock(MessageSendParams.class);
        EventKind expectedResult = mock(EventKind.class);
        when(request.toString()).thenReturn("request-string");
        when(expectedResult.toString()).thenReturn("response-string");
        when(delegate.sendMessage(eq(request), any(ClientCallContext.class))).thenReturn(expectedResult);

        EventKind result = transport.sendMessage(request, context);

        assertEquals(expectedResult, result);
        verify(tracer).spanBuilder(A2AMethods.SEND_MESSAGE_METHOD);
        verify(spanBuilder).setSpanKind(SpanKind.CLIENT);
        verify(spanBuilder).setAttribute(GENAI_REQUEST, "request-string");
        verify(span).setAttribute(GENAI_RESPONSE, "response-string");
        verify(span).setStatus(StatusCode.OK);
        verify(span).end();
        verify(scope).close();
    }

    @Test
    void testSendMessage_NullResponse() throws A2AClientException {
        MessageSendParams request = mock(MessageSendParams.class);
        when(request.toString()).thenReturn("request-string");
        when(delegate.sendMessage(eq(request), any(ClientCallContext.class))).thenReturn(null);

        EventKind result = transport.sendMessage(request, context);

        assertNull(result);
        verify(spanBuilder).setAttribute(GENAI_REQUEST, "request-string");
        verify(spanBuilder, never()).setAttribute(eq(GENAI_RESPONSE), anyString());
        verify(span, never()).setStatus(StatusCode.OK);
        verify(span).end();
    }

    @Test
    void testSendMessage_ThrowsException() throws A2AClientException {
        MessageSendParams request = mock(MessageSendParams.class);
        when(request.toString()).thenReturn("request-string");
        A2AClientException expectedException = new A2AClientException("Test error");
        when(delegate.sendMessage(eq(request), any(ClientCallContext.class))).thenThrow(expectedException);

        A2AClientException exception = assertThrows(A2AClientException.class,
                () -> transport.sendMessage(request, context));

        assertEquals(expectedException, exception);
        verify(span).setStatus(StatusCode.ERROR, "Test error");
        verify(span).end();
        verify(scope).close();
    }

    @Test
    void testSendMessageStreaming() throws A2AClientException {
        MessageSendParams request = mock(MessageSendParams.class);
        when(request.toString()).thenReturn("request-string");
        Consumer<StreamingEventKind> eventConsumer = mock(Consumer.class);
        Consumer<Throwable> errorConsumer = mock(Consumer.class);

        transport.sendMessageStreaming(request, eventConsumer, errorConsumer, context);

        verify(tracer).spanBuilder(A2AMethods.SEND_STREAMING_MESSAGE_METHOD);
        verify(spanBuilder).setSpanKind(SpanKind.CLIENT);
        verify(spanBuilder).setAttribute(GENAI_REQUEST, "request-string");
        verify(span).setStatus(StatusCode.OK);
        verify(span).end();

        ArgumentCaptor<Consumer<StreamingEventKind>> eventConsumerCaptor = ArgumentCaptor.forClass(Consumer.class);
        ArgumentCaptor<Consumer<Throwable>> errorConsumerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(delegate).sendMessageStreaming(eq(request), eventConsumerCaptor.capture(),
                errorConsumerCaptor.capture(), any(ClientCallContext.class));

        assertNotNull(eventConsumerCaptor.getValue());
        assertNotNull(errorConsumerCaptor.getValue());
    }

    @Test
    void testGetTask_Success() throws A2AClientException {
        TaskQueryParams request = mock(TaskQueryParams.class);
        Task expectedResult = mock(Task.class);
        when(request.toString()).thenReturn("request-string");
        when(expectedResult.toString()).thenReturn("response-string");
        when(delegate.getTask(eq(request), any(ClientCallContext.class))).thenReturn(expectedResult);

        Task result = transport.getTask(request, context);

        assertEquals(expectedResult, result);
        verify(tracer).spanBuilder(A2AMethods.GET_TASK_METHOD);
        verify(spanBuilder).setAttribute(GENAI_REQUEST, "request-string");
        verify(span).setAttribute(GENAI_RESPONSE, "response-string");
        verify(span).setStatus(StatusCode.OK);
        verify(span).end();
    }

    @Test
    void testCancelTask_Success() throws A2AClientException {
        TaskIdParams request = mock(TaskIdParams.class);
        Task expectedResult = mock(Task.class);
        when(request.toString()).thenReturn("request-string");
        when(expectedResult.toString()).thenReturn("response-string");
        when(delegate.cancelTask(eq(request), any(ClientCallContext.class))).thenReturn(expectedResult);

        Task result = transport.cancelTask(request, context);

        assertEquals(expectedResult, result);
        verify(tracer).spanBuilder(A2AMethods.CANCEL_TASK_METHOD);
        verify(spanBuilder).setAttribute(GENAI_REQUEST, "request-string");
        verify(span).setAttribute(GENAI_RESPONSE, "response-string");
        verify(span).setStatus(StatusCode.OK);
        verify(span).end();
    }

    @Test
    void testListTasks_Success() throws A2AClientException {
        ListTasksParams request = mock(ListTasksParams.class);
        ListTasksResult expectedResult = mock(ListTasksResult.class);
        when(request.toString()).thenReturn("request-string");
        when(expectedResult.toString()).thenReturn("response-string");
        when(delegate.listTasks(eq(request), any(ClientCallContext.class))).thenReturn(expectedResult);

        ListTasksResult result = transport.listTasks(request, context);

        assertEquals(expectedResult, result);
        verify(tracer).spanBuilder(A2AMethods.LIST_TASK_METHOD);
        verify(spanBuilder).setAttribute(GENAI_REQUEST, "request-string");
        verify(span).setAttribute(GENAI_RESPONSE, "response-string");
        verify(span).setStatus(StatusCode.OK);
        verify(span).end();
    }

    @Test
    void testCreateTaskPushNotificationConfiguration_Success() throws A2AClientException {
        TaskPushNotificationConfig request = mock(TaskPushNotificationConfig.class);
        TaskPushNotificationConfig expectedResult = mock(TaskPushNotificationConfig.class);
        when(request.toString()).thenReturn("request-string");
        when(expectedResult.toString()).thenReturn("response-string");
        when(delegate.createTaskPushNotificationConfiguration(eq(request), any(ClientCallContext.class))).thenReturn(expectedResult);

        TaskPushNotificationConfig result = transport.createTaskPushNotificationConfiguration(request, context);

        assertEquals(expectedResult, result);
        verify(tracer).spanBuilder(A2AMethods.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
        verify(spanBuilder).setAttribute(GENAI_REQUEST, "request-string");
        verify(span).setAttribute(GENAI_RESPONSE, "response-string");
        verify(span).setStatus(StatusCode.OK);
        verify(span).end();
    }

    @Test
    void testGetTaskPushNotificationConfiguration_Success() throws A2AClientException {
        GetTaskPushNotificationConfigParams request = mock(GetTaskPushNotificationConfigParams.class);
        TaskPushNotificationConfig expectedResult = mock(TaskPushNotificationConfig.class);
        when(request.toString()).thenReturn("request-string");
        when(expectedResult.toString()).thenReturn("response-string");
        when(delegate.getTaskPushNotificationConfiguration(eq(request), any(ClientCallContext.class))).thenReturn(expectedResult);

        TaskPushNotificationConfig result = transport.getTaskPushNotificationConfiguration(request, context);

        assertEquals(expectedResult, result);
        verify(tracer).spanBuilder(A2AMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
        verify(spanBuilder).setAttribute(GENAI_REQUEST, "request-string");
        verify(span).setAttribute(GENAI_RESPONSE, "response-string");
        verify(span).setStatus(StatusCode.OK);
        verify(span).end();
    }

    @Test
    void testListTaskPushNotificationConfigurations_Success() throws A2AClientException {
        ListTaskPushNotificationConfigParams request = mock(ListTaskPushNotificationConfigParams.class);
        TaskPushNotificationConfig config1 = mock(TaskPushNotificationConfig.class);
        TaskPushNotificationConfig config2 = mock(TaskPushNotificationConfig.class);
        when(config1.toString()).thenReturn("config1");
        when(config2.toString()).thenReturn("config2");
        ListTaskPushNotificationConfigResult expectedResult = new ListTaskPushNotificationConfigResult(List.of(config1, config2));
        when(request.toString()).thenReturn("request-string");
        when(delegate.listTaskPushNotificationConfigurations(eq(request), any(ClientCallContext.class))).thenReturn(expectedResult);

        ListTaskPushNotificationConfigResult result = transport.listTaskPushNotificationConfigurations(request, context);

        assertEquals(expectedResult, result);
        verify(tracer).spanBuilder(A2AMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
        verify(spanBuilder).setSpanKind(SpanKind.CLIENT);
        verify(spanBuilder).setAttribute(GENAI_REQUEST, "request-string");
        verify(span).setAttribute(GENAI_RESPONSE, "config1,config2");
        verify(span).setStatus(StatusCode.OK);
        verify(span).end();
    }

    @Test
    void testDeleteTaskPushNotificationConfigurations_Success() throws A2AClientException {
        DeleteTaskPushNotificationConfigParams request = mock(DeleteTaskPushNotificationConfigParams.class);
        when(request.toString()).thenReturn("request-string");
        doNothing().when(delegate).deleteTaskPushNotificationConfigurations(eq(request), any(ClientCallContext.class));

        transport.deleteTaskPushNotificationConfigurations(request, context);

        verify(tracer).spanBuilder(A2AMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
        verify(spanBuilder).setAttribute(GENAI_REQUEST, "request-string");
        verify(span).setStatus(StatusCode.OK);
        verify(span).end();
        verify(delegate).deleteTaskPushNotificationConfigurations(eq(request), any(ClientCallContext.class));
    }

    @Test
    void testSubscribeToTask() throws A2AClientException {
        TaskIdParams request = mock(TaskIdParams.class);
        when(request.toString()).thenReturn("request-string");
        Consumer<StreamingEventKind> eventConsumer = mock(Consumer.class);
        Consumer<Throwable> errorConsumer = mock(Consumer.class);

        transport.subscribeToTask(request, eventConsumer, errorConsumer, context);

        verify(tracer).spanBuilder(A2AMethods.SUBSCRIBE_TO_TASK_METHOD);
        verify(spanBuilder).setSpanKind(SpanKind.CLIENT);
        verify(spanBuilder).setAttribute(GENAI_REQUEST, "request-string");
        verify(span).setStatus(StatusCode.OK);
        verify(span).end();

        ArgumentCaptor<Consumer<StreamingEventKind>> eventConsumerCaptor = ArgumentCaptor.forClass(Consumer.class);
        ArgumentCaptor<Consumer<Throwable>> errorConsumerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(delegate).subscribeToTask(eq(request), eventConsumerCaptor.capture(),
                errorConsumerCaptor.capture(), any(ClientCallContext.class));

        assertNotNull(eventConsumerCaptor.getValue());
        assertNotNull(errorConsumerCaptor.getValue());
    }

    @Test
    void testGetAgentCard_Success() throws A2AClientException {
        AgentCard expectedResult = mock(AgentCard.class);
        when(expectedResult.toString()).thenReturn("response-string");
        when(delegate.getExtendedAgentCard(any(ClientCallContext.class))).thenReturn(expectedResult);

        AgentCard result = transport.getExtendedAgentCard(context);

        assertEquals(expectedResult, result);
        verify(tracer).spanBuilder(A2AMethods.GET_EXTENDED_AGENT_CARD_METHOD);
        verify(spanBuilder).setSpanKind(SpanKind.CLIENT);
        verify(span).setAttribute(GENAI_RESPONSE, "response-string");
        verify(span).setStatus(StatusCode.OK);
        verify(span).end();
    }

    @Test
    void testGetAgentCard_NullResponse() throws A2AClientException {
        when(delegate.getExtendedAgentCard(any(ClientCallContext.class))).thenReturn(null);

        AgentCard result = transport.getExtendedAgentCard(context);

        assertNull(result);
        verify(tracer).spanBuilder(A2AMethods.GET_EXTENDED_AGENT_CARD_METHOD);
        verify(spanBuilder).setSpanKind(SpanKind.CLIENT);
        verify(span, never()).setAttribute(eq(GENAI_RESPONSE), anyString());
        verify(span, never()).setStatus(StatusCode.OK);
        verify(span).end();
    }

    @Test
    void testClose() {
        transport.close();
        verify(delegate).close();
    }

    @Test
    void testEventConsumer_ThroughSendMessageStreaming() throws A2AClientException {
        MessageSendParams request = mock(MessageSendParams.class);
        when(request.toString()).thenReturn("request-string");

        SpanBuilder eventSpanBuilder = mock(SpanBuilder.class);
        Span eventSpan = mock(Span.class);
        when(tracer.spanBuilder(A2AMethods.SEND_STREAMING_MESSAGE_METHOD + "-event")).thenReturn(eventSpanBuilder);
        when(eventSpanBuilder.setSpanKind(any(SpanKind.class))).thenReturn(eventSpanBuilder);
        when(eventSpanBuilder.setAttribute(anyString(), anyString())).thenReturn(eventSpanBuilder);
        when(eventSpanBuilder.addLink(any(SpanContext.class))).thenReturn(eventSpanBuilder);
        when(eventSpanBuilder.startSpan()).thenReturn(eventSpan);

        ArgumentCaptor<Consumer<StreamingEventKind>> eventConsumerCaptor = ArgumentCaptor.forClass(Consumer.class);
        Consumer<StreamingEventKind> originalConsumer = mock(Consumer.class);

        transport.sendMessageStreaming(request, originalConsumer, mock(Consumer.class), context);

        verify(delegate).sendMessageStreaming(eq(request), eventConsumerCaptor.capture(), any(Consumer.class), any(ClientCallContext.class));

        Message event = Message.builder()
                .messageId("test-id")
                .taskId("task-id")
                .role(Message.Role.USER)
                .parts(List.of(new TextPart("test content")))
                .build();

        eventConsumerCaptor.getValue().accept(event);

        verify(tracer).spanBuilder(A2AMethods.SEND_STREAMING_MESSAGE_METHOD + "-event");
        verify(eventSpan).setStatus(StatusCode.OK);
        verify(eventSpan).end();
        verify(originalConsumer).accept(event);
    }

    @Test
    void testErrorConsumer_ThroughSendMessageStreaming() throws A2AClientException {
        MessageSendParams request = mock(MessageSendParams.class);
        when(request.toString()).thenReturn("request-string");

        SpanBuilder errorSpanBuilder = mock(SpanBuilder.class);
        Span errorSpan = mock(Span.class);
        when(tracer.spanBuilder(A2AMethods.SEND_STREAMING_MESSAGE_METHOD + "-error")).thenReturn(errorSpanBuilder);
        when(errorSpanBuilder.setSpanKind(any(SpanKind.class))).thenReturn(errorSpanBuilder);
        when(errorSpanBuilder.addLink(any(SpanContext.class))).thenReturn(errorSpanBuilder);
        when(errorSpanBuilder.startSpan()).thenReturn(errorSpan);

        ArgumentCaptor<Consumer<Throwable>> errorConsumerCaptor = ArgumentCaptor.forClass(Consumer.class);
        Consumer<Throwable> originalConsumer = mock(Consumer.class);

        transport.sendMessageStreaming(request, mock(Consumer.class), originalConsumer, context);

        verify(delegate).sendMessageStreaming(eq(request), any(Consumer.class), errorConsumerCaptor.capture(), any(ClientCallContext.class));

        Throwable error = new RuntimeException("Test error");

        errorConsumerCaptor.getValue().accept(error);

        verify(tracer).spanBuilder(A2AMethods.SEND_STREAMING_MESSAGE_METHOD + "-error");
        verify(errorSpan).setStatus(StatusCode.ERROR, "Test error");
        verify(errorSpan).end();
        verify(originalConsumer).accept(error);
    }

    @Test
    void testErrorConsumer_WithNullThrowable() throws A2AClientException {
        MessageSendParams request = mock(MessageSendParams.class);
        when(request.toString()).thenReturn("request-string");

        ArgumentCaptor<Consumer<Throwable>> errorConsumerCaptor = ArgumentCaptor.forClass(Consumer.class);
        Consumer<Throwable> originalConsumer = mock(Consumer.class);

        transport.sendMessageStreaming(request, mock(Consumer.class), originalConsumer, context);

        verify(delegate).sendMessageStreaming(eq(request), any(Consumer.class), errorConsumerCaptor.capture(), any(ClientCallContext.class));

        errorConsumerCaptor.getValue().accept(null);

        verify(originalConsumer, never()).accept(any());
    }

    @Test
    void testDeleteTaskPushNotificationConfigurations_ThrowsException() throws A2AClientException {
        DeleteTaskPushNotificationConfigParams request = mock(DeleteTaskPushNotificationConfigParams.class);
        when(request.toString()).thenReturn("request-string");
        A2AClientException expectedException = new A2AClientException("Delete failed");
        doThrow(expectedException).when(delegate).deleteTaskPushNotificationConfigurations(eq(request), any(ClientCallContext.class));

        A2AClientException exception = assertThrows(A2AClientException.class,
                () -> transport.deleteTaskPushNotificationConfigurations(request, context));

        assertEquals(expectedException, exception);
        verify(span).setStatus(StatusCode.ERROR, "Delete failed");
        verify(span).end();
    }

    @Test
    void testResubscribe_ThrowsException() throws A2AClientException {
        TaskIdParams request = mock(TaskIdParams.class);
        when(request.toString()).thenReturn("request-string");
        Consumer<StreamingEventKind> eventConsumer = mock(Consumer.class);
        Consumer<Throwable> errorConsumer = mock(Consumer.class);
        A2AClientException expectedException = new A2AClientException("Resubscribe failed");
        doThrow(expectedException).when(delegate).subscribeToTask(any(TaskIdParams.class), any(Consumer.class),
                any(Consumer.class), any(ClientCallContext.class));

        A2AClientException exception = assertThrows(A2AClientException.class,
                () -> transport.subscribeToTask(request, eventConsumer, errorConsumer, context));

        assertEquals(expectedException, exception);
        verify(span).setStatus(StatusCode.ERROR, "Resubscribe failed");
        verify(span).end();
    }

    @Test
    void testSendMessageStreaming_ThrowsException() throws A2AClientException {
        MessageSendParams request = mock(MessageSendParams.class);
        when(request.toString()).thenReturn("request-string");
        Consumer<StreamingEventKind> eventConsumer = mock(Consumer.class);
        Consumer<Throwable> errorConsumer = mock(Consumer.class);
        A2AClientException expectedException = new A2AClientException("Streaming failed");
        doThrow(expectedException).when(delegate).sendMessageStreaming(any(MessageSendParams.class), any(Consumer.class),
                any(Consumer.class), any(ClientCallContext.class));

        A2AClientException exception = assertThrows(A2AClientException.class,
                () -> transport.sendMessageStreaming(request, eventConsumer, errorConsumer, context));

        assertEquals(expectedException, exception);
        verify(span).setStatus(StatusCode.ERROR, "Streaming failed");
        verify(span).end();
    }
}
