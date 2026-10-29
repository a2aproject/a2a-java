package org.a2aproject.sdk.extras.opentelemetry.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.Scope;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.a2aproject.sdk.client.transport.spi.ClientTransport;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallContext;
import org.a2aproject.sdk.extras.opentelemetry.ProtocolLogEmitter;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.StreamingEventKind;
import org.a2aproject.sdk.spec.TextPart;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;

class ProtocolStreamingLogTest {
    @Test
    @SuppressWarnings("unchecked")
    void streamingCallbackCarriesContextAcrossThreads() throws Exception {
        ClientTransport delegate = mock(ClientTransport.class);
        Tracer tracer = mock(Tracer.class);
        SpanBuilder spans = mock(SpanBuilder.class, Answers.RETURNS_SELF);
        Span span = Span.wrap(SpanContext.create("12345678901234567890123456789012", "1234567890123456",
                TraceFlags.getSampled(), TraceState.getDefault()));
        when(tracer.spanBuilder(anyString())).thenReturn(spans);
        when(spans.startSpan()).thenReturn(span);
        Logger logger = mock(Logger.class);
        LogRecordBuilder record = mock(LogRecordBuilder.class, Answers.RETURNS_SELF);
        when(logger.isEnabled(any(Severity.class), any(Context.class))).thenReturn(true);
        when(logger.logRecordBuilder()).thenReturn(record);
        OpenTelemetryClientTransport transport = new OpenTelemetryClientTransport(delegate, tracer, null, new ProtocolLogEmitter(logger));
        Message message = Message.builder().messageId("id").role(Message.Role.ROLE_USER)
                .parts(List.of(new TextPart("content"))).build();
        MessageSendParams request = new MessageSendParams(message, null, null, "");
        ContextKey<String> key = ContextKey.named("test-context");
        try (Scope ignored = Context.current().with(key, "preserved").makeCurrent()) {
            transport.sendMessageStreaming(request, event -> {}, error -> {}, null);
        }
        ArgumentCaptor<Consumer<StreamingEventKind>> consumer = ArgumentCaptor.forClass(Consumer.class);
        verify(delegate).sendMessageStreaming(any(), consumer.capture(), any(), any(ClientCallContext.class));
        CompletableFuture.runAsync(() -> consumer.getValue().accept(message)).get();

        ArgumentCaptor<Context> contexts = ArgumentCaptor.forClass(Context.class);
        verify(record, times(2)).setContext(contexts.capture());
        Context eventContext = contexts.getAllValues().get(1);
        assertEquals("preserved", eventContext.get(key));
        assertEquals(span.getSpanContext().getTraceId(), Span.fromContext(eventContext).getSpanContext().getTraceId());
        verify(record).setAttribute("a2a.event.name", "a2a.task.event");
    }
}
