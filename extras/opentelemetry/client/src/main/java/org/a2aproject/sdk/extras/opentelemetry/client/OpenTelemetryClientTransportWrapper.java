package org.a2aproject.sdk.extras.opentelemetry.client;

import org.a2aproject.sdk.client.transport.spi.ClientTransport;
import org.a2aproject.sdk.client.transport.spi.ClientTransportConfig;
import org.a2aproject.sdk.client.transport.spi.ClientTransportWrapper;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;

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

    @Override
    public ClientTransport wrap(ClientTransport transport, ClientTransportConfig<?> config) {
        Object tracerObj = config.getParameters().get(OTEL_TRACER_KEY);
        if (tracerObj != null && tracerObj instanceof Tracer tracer) {
            Object meterObj = config.getParameters().get(OTEL_METER_KEY);
            Meter meter = (meterObj instanceof Meter m) ? m : null;
            return new OpenTelemetryClientTransport(transport, tracer, meter);
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
