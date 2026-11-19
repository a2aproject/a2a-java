package org.a2aproject.sdk.extras.opentelemetry.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.a2aproject.sdk.client.Client;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfig;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import org.a2aproject.sdk.client.transport.spi.ClientTransport;
import org.a2aproject.sdk.extras.opentelemetry.ProtocolLogEmitter;
import org.a2aproject.sdk.extras.opentelemetry.ProtocolPayloadFormatter;
import org.a2aproject.sdk.extras.opentelemetry.client.OpenTelemetryClientTransport;
import org.a2aproject.sdk.extras.opentelemetry.client.OpenTelemetryClientTransportWrapper;
import org.a2aproject.sdk.extras.opentelemetry.server.OpenTelemetryRequestHandlerDecorator;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.TaskOperation;
import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.DataPart;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.StreamingEventKind;
import org.a2aproject.sdk.spec.TaskNotFoundError;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProtocolLogExportTest {
    @Test
    void clientBuilderExportsNegotiatedProtocol() throws Exception {
        InMemoryLogRecordExporter exporter = InMemoryLogRecordExporter.create();
        try (SdkLoggerProvider logs = SdkLoggerProvider.builder()
                    .addLogRecordProcessor(SimpleLogRecordProcessor.create(exporter)).build();
             SdkTracerProvider traces = SdkTracerProvider.builder().build()) {
            OpenTelemetrySdk telemetry = OpenTelemetrySdk.builder()
                    .setLoggerProvider(logs).setTracerProvider(traces).build();
            AgentCard card = mock(AgentCard.class);
            when(card.supportedInterfaces()).thenReturn(List.of(new AgentInterface("JSONRPC", "http://localhost:9999")));
            JSONRPCTransportConfig config = new JSONRPCTransportConfigBuilder().build();
            config.setParameters(Map.of(
                    OpenTelemetryClientTransportWrapper.OTEL_TRACER_KEY, telemetry.getTracer("a2a-test"),
                    OpenTelemetryClientTransportWrapper.OTEL_LOGGER_KEY, telemetry.getLogsBridge().get("a2a-test")));

            Client.builder(card).withTransport(JSONRPCTransport.class, config).build();

            List<LogRecordData> records = exporter.getFinishedLogRecordItems();
            assertEquals(1, records.size());
            assertEquals("a2a.negotiation", records.get(0).getEventName());
            assertEquals("JSONRPC", records.get(0).getAttributes().get(AttributeKey.stringKey("a2a.event.kind")));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void exportsSanitizedStreamingLogsWithTraceContext() throws Exception {
        InMemoryLogRecordExporter exporter = InMemoryLogRecordExporter.create();
        try (SdkLoggerProvider logs = SdkLoggerProvider.builder()
                    .addLogRecordProcessor(SimpleLogRecordProcessor.create(exporter)).build();
             SdkTracerProvider traces = SdkTracerProvider.builder().build()) {
            OpenTelemetrySdk telemetry = OpenTelemetrySdk.builder()
                    .setLoggerProvider(logs).setTracerProvider(traces).build();
            ClientTransport delegate = mock(ClientTransport.class);
            ProtocolLogEmitter emitter = new ProtocolLogEmitter(telemetry.getLogsBridge().get("a2a-test"),
                    new ProtocolPayloadFormatter(4096, value -> value));
            OpenTelemetryClientTransport client = new OpenTelemetryClientTransport(delegate, telemetry.getTracer("a2a-test"), null, emitter);
            Message message = Message.builder().messageId("id").role(Message.Role.ROLE_USER)
                    .parts(List.of(new DataPart(Map.of("api_key", "private-secret", "message", "hello")))).build();
            client.sendMessageStreaming(new MessageSendParams(message, null, null, ""), event -> {}, error -> {}, null);
            ArgumentCaptor<Consumer<StreamingEventKind>> events = ArgumentCaptor.forClass(Consumer.class);
            ArgumentCaptor<Consumer<Throwable>> terminal = ArgumentCaptor.forClass(Consumer.class);
            verify(delegate).sendMessageStreaming(any(), events.capture(), terminal.capture(), any());
            CompletableFuture.runAsync(() -> {
                events.getValue().accept(message);
                terminal.getValue().accept(null);
            }).get();

            List<LogRecordData> records = exporter.getFinishedLogRecordItems();
            assertEquals(3, records.size());
            LogRecordData request = records.get(0);
            LogRecordData event = records.get(1);
            assertEquals("a2a.task.request", request.getEventName());
            assertEquals("a2a.task.event", event.getEventName());
            assertEquals(Severity.DEBUG, event.getSeverity());
            assertTrue(event.getSpanContext().isValid());
            assertEquals(request.getSpanContext().getTraceId(), event.getSpanContext().getTraceId());
            assertNotEquals(request.getSpanContext().getSpanId(), event.getSpanContext().getSpanId());
            assertEquals(request.getSpanContext().getTraceId(), records.get(2).getSpanContext().getTraceId());
            assertEquals("inbound", event.getAttributes().get(AttributeKey.stringKey("a2a.log.direction")));
            assertFalse(event.getBody().asString().contains("private-secret"));
            assertTrue(event.getBody().asString().contains("[redacted]"));
        }
    }

    @Test
    void exportsAuthorizationDecisionsWithinTheRequestTrace() throws Exception {
        InMemoryLogRecordExporter exporter = InMemoryLogRecordExporter.create();
        try (SdkLoggerProvider logs = SdkLoggerProvider.builder()
                    .addLogRecordProcessor(SimpleLogRecordProcessor.create(exporter)).build();
             SdkTracerProvider traces = SdkTracerProvider.builder().build()) {
            OpenTelemetrySdk telemetry = OpenTelemetrySdk.builder()
                    .setLoggerProvider(logs).setTracerProvider(traces).build();
            RequestHandler delegate = mock(RequestHandler.class);
            ServerCallContext context = mock(ServerCallContext.class);
            Tracer tracer = telemetry.getTracer("a2a-test");
            RequestHandler handler = new AuthorizationLoggingHandler(delegate, tracer,
                    new ProtocolLogEmitter(telemetry.getLogsBridge().get("a2a-test")));
            doThrow(new TaskNotFoundError()).when(delegate)
                    .authorizeTaskAccess("task-2", context, TaskOperation.CANCEL_TASK);

            Span request = tracer.spanBuilder("authorize").startSpan();
            try (Scope scope = request.makeCurrent()) {
                handler.authorizeTaskAccess("task-1", context, TaskOperation.GET_TASK);
                assertThrows(TaskNotFoundError.class,
                        () -> handler.authorizeTaskAccess("task-2", context, TaskOperation.CANCEL_TASK));
            } finally {
                request.end();
            }

            List<LogRecordData> records = exporter.getFinishedLogRecordItems();
            assertEquals(2, records.size());
            LogRecordData allow = records.get(0);
            LogRecordData deny = records.get(1);

            assertEquals("a2a.authorization", allow.getEventName());
            assertEquals("allow", allow.getAttributes().get(AttributeKey.stringKey("a2a.event.kind")));
            assertEquals("GET_TASK", allow.getAttributes().get(AttributeKey.stringKey("a2a.operation.name")));
            assertEquals("task-1", allow.getAttributes().get(AttributeKey.stringKey("a2a.task.id")));
            assertEquals("inbound", allow.getAttributes().get(AttributeKey.stringKey("a2a.log.direction")));
            assertNull(allow.getAttributes().get(AttributeKey.stringKey("error.type")));

            assertEquals("deny", deny.getAttributes().get(AttributeKey.stringKey("a2a.event.kind")));
            assertEquals("TaskNotFoundError", deny.getAttributes().get(AttributeKey.stringKey("error.type")));

            assertEquals(request.getSpanContext().getTraceId(), allow.getSpanContext().getTraceId());
            assertEquals(request.getSpanContext().getSpanId(), deny.getSpanContext().getSpanId());
        }
    }

    /** Minimal concrete decorator: the CDI decorator is abstract and only authorization is exercised. */
    private static final class AuthorizationLoggingHandler extends OpenTelemetryRequestHandlerDecorator {
        AuthorizationLoggingHandler(RequestHandler delegate, Tracer tracer, ProtocolLogEmitter emitter) {
            super(delegate, tracer, null, emitter);
        }
    }
}
