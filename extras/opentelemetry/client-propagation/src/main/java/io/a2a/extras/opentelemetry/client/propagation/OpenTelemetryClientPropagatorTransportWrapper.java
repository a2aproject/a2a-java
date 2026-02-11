package io.a2a.extras.opentelemetry.client.propagation;

import io.a2a.client.transport.spi.ClientTransport;
import io.a2a.client.transport.spi.ClientTransportConfig;
import io.a2a.client.transport.spi.ClientTransportWrapper;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

/**
 * OpenTelemetry client transport wrapper that adds opentelemetry propagation to A2A client calls.
 *
 * <p>This wrapper is automatically discovered via Java's ServiceLoader mechanism.
 * To enable tracing, add a {@link Tracer} instance to the transport configuration:
 * <pre>{@code
 * ClientTransportConfig config = new JSONRPCTransportConfig();
 * config.setParameters(Map.of(
 *     OpenTelemetryClientTransportFactory.OTEL_TRACER_KEY,
 *     openTelemetry.getTracer("my-service"),
 *     OpenTelemetryClientTransportFactory.OTEL_OPEN_TELEMETRY_KEY,
 *     openTelemetry
 * ));
 * }</pre>
 */
public class OpenTelemetryClientPropagatorTransportWrapper implements ClientTransportWrapper {

    /**
     * Configuration key for the OpenTelemetry Tracer instance.
     * Value must be of type {@link Tracer}.
     */
    public static final String OTEL_TRACER_KEY = "io.a2a.extras.opentelemetry.Tracer";
    public static final String OTEL_OPEN_TELEMETRY_KEY = "io.a2a.extras.opentelemetry.OpenTelemetry";

    @Override
    public ClientTransport wrap(ClientTransport transport, ClientTransportConfig<?> config) {
        Object openTelemetryObj = config.getParameters().get(OTEL_OPEN_TELEMETRY_KEY);
        if (openTelemetryObj != null && openTelemetryObj instanceof OpenTelemetry openTelemetry) {
            return new OpenTelemetryClientPropagatorTransport(transport, openTelemetry);
        }
        // No tracer configured, return unwrapped transport
        return transport;
    }

    @Override
    public int priority() {
        // Observability/tracing should be in the middle priority range
        // so it can observe other wrappers but doesn't interfere with security
        return 500;
    }
}
