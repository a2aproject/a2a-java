package org.a2aproject.sdk.extras.opentelemetry.client;

import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.A2A_EVENT_NEGOTIATION;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.A2A_LOG_DIRECTION_OUTBOUND;

import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import org.a2aproject.sdk.client.transport.spi.ClientTransport;
import org.a2aproject.sdk.client.transport.spi.ClientTransportConfig;
import org.a2aproject.sdk.client.transport.spi.ClientTransportWrapper;
import org.a2aproject.sdk.extras.opentelemetry.ProtocolLogEmitter;
import org.a2aproject.sdk.extras.opentelemetry.ProtocolPayloadFormatter;
import org.a2aproject.sdk.spec.AgentInterface;
import org.jspecify.annotations.Nullable;

/**
 * OpenTelemetry client transport wrapper that adds distributed tracing to A2A client calls.
 *
 * <p>This wrapper is automatically discovered via Java's ServiceLoader mechanism.
 * To enable tracing, add a {@link Tracer} instance to the transport configuration:
 * <pre>{@code
 * ClientTransportConfig config = new JSONRPCTransportConfig();
 * config.setParameters(Map.of(
 *     OpenTelemetryClientTransportWrapper.OTEL_TRACER_KEY,
 *     openTelemetry.getTracer("my-service")
 * ));
 * }</pre>
 */
public class OpenTelemetryClientTransportWrapper implements ClientTransportWrapper {

    /**
     * Configuration key for the OpenTelemetry Tracer instance.
     * Value must be of type {@link Tracer}.
     */
    public static final String OTEL_TRACER_KEY = "org.a2aproject.sdk.extras.opentelemetry.Tracer";

    /**
     * Configuration key for the OpenTelemetry Meter instance.
     * Value must be of type {@link Meter}.
     */
    public static final String OTEL_METER_KEY = "org.a2aproject.sdk.extras.opentelemetry.Meter";

    /** Optional OpenTelemetry logger; providing it enables metadata-only protocol logs. */
    public static final String OTEL_LOGGER_KEY = "org.a2aproject.sdk.extras.opentelemetry.Logger";

    /** Optional formatter; supplying one separately enables payload capture. */
    public static final String OTEL_PAYLOAD_FORMATTER_KEY =
            "org.a2aproject.sdk.extras.opentelemetry.PayloadFormatter";

    /** Optional OpenTelemetry severity; defaults to DEBUG. */
    public static final String OTEL_LOG_SEVERITY_KEY =
            "org.a2aproject.sdk.extras.opentelemetry.LogSeverity";

    @Override
    public void onTransportSelected(AgentInterface selected, ClientTransportConfig<?> config) {
        if (!(config.getParameters().get(OTEL_TRACER_KEY) instanceof Tracer)) {
            return;
        }
        ProtocolLogEmitter emitter = createEmitter(config);
        if (emitter != null) {
            emitter.emit(
                    A2A_EVENT_NEGOTIATION,
                    "SelectTransport",
                    A2A_LOG_DIRECTION_OUTBOUND,
                    selected.protocolBinding());
        }
    }

    private static @Nullable ProtocolLogEmitter createEmitter(ClientTransportConfig<?> config) {
        Object loggerObj = config.getParameters().get(OTEL_LOGGER_KEY);
        Object formatterObj = config.getParameters().get(OTEL_PAYLOAD_FORMATTER_KEY);
        ProtocolPayloadFormatter formatter =
                formatterObj instanceof ProtocolPayloadFormatter f ? f : null;
        Object severityObj = config.getParameters().get(OTEL_LOG_SEVERITY_KEY);
        Severity severity = severityObj instanceof Severity s ? s : Severity.DEBUG;
        return loggerObj instanceof Logger logger
                ? new ProtocolLogEmitter(logger, formatter, severity)
                : null;
    }

    @Override
    public ClientTransport wrap(ClientTransport transport, ClientTransportConfig<?> config) {
        if (config.getParameters().get(OTEL_TRACER_KEY) instanceof Tracer tracer) {
            Object meterObj = config.getParameters().get(OTEL_METER_KEY);
            Meter meter = (meterObj instanceof Meter m) ? m : null;
            return new OpenTelemetryClientTransport(transport, tracer, meter, createEmitter(config));
        }
        // No tracer configured, return unwrapped transport
        return transport;
    }

    @Override
    public int priority() {
        // Observability/tracing should be in the middle priority range
        // so it can observe other wrappers but doesn't interfere with security
        return 600;
    }
}
