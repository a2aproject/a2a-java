package org.a2aproject.sdk.extras.opentelemetry.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import java.util.Map;
import org.a2aproject.sdk.client.transport.spi.ClientTransportConfig;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Answers;

class ProtocolNegotiationLogTest {
    @ParameterizedTest
    @EnumSource(TransportProtocol.class)
    void reportsSelectedProtocolWithoutAddress(TransportProtocol protocol) {
        ClientTransportConfig<?> config = mock(ClientTransportConfig.class);
        Logger logger = mock(Logger.class);
        LogRecordBuilder record = mock(LogRecordBuilder.class, Answers.RETURNS_SELF);
        when(logger.isEnabled(any(Severity.class), any(Context.class))).thenReturn(true);
        when(logger.logRecordBuilder()).thenReturn(record);
        doReturn(Map.of(
                OpenTelemetryClientTransportWrapper.OTEL_TRACER_KEY, mock(Tracer.class),
                OpenTelemetryClientTransportWrapper.OTEL_LOGGER_KEY, logger)).when(config).getParameters();
        AgentInterface selected = mock(AgentInterface.class);
        when(selected.protocolBinding()).thenReturn(protocol.asString());

        new OpenTelemetryClientTransportWrapper().onTransportSelected(selected, config);

        verify(record).setEventName("a2a.negotiation");
        verify(record).setAttribute("a2a.event.kind", protocol.asString());
        verify(selected, never()).url();
        verify(record).emit();
    }
}
