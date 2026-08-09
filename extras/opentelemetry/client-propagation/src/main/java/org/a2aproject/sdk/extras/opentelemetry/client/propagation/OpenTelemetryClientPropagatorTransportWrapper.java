package org.a2aproject.sdk.extras.opentelemetry.client.propagation;

import org.a2aproject.sdk.client.transport.spi.ClientTransport;
import org.a2aproject.sdk.client.transport.spi.ClientTransportConfig;
import org.a2aproject.sdk.client.transport.spi.ClientTransportWrapper;
import io.opentelemetry.api.OpenTelemetry;

/**
 * OpenTelemetry client transport wrapper that injects W3C Trace Context headers into outbound A2A client requests.
 *
 * <p>This wrapper is automatically discovered via Java's ServiceLoader mechanism.
 * To enable context propagation, add an {@link OpenTelemetry} instance to the transport configuration:
 * <pre>{@code
 * ClientTransportConfig config = new JSONRPCTransportConfig();
 * config.setParameters(Map.of(
 *     OpenTelemetryClientPropagatorTransportWrapper.OTEL_OPEN_TELEMETRY_KEY,
 *     openTelemetry
 * ));
 * }</pre>
 */
public class OpenTelemetryClientPropagatorTransportWrapper implements ClientTransportWrapper {

    /**
     * Configuration key for the {@link OpenTelemetry} instance used for context propagation.
     */
    public static final String OTEL_OPEN_TELEMETRY_KEY = "org.a2aproject.sdk.extras.opentelemetry.OpenTelemetry";

    @Override
    public ClientTransport wrap(ClientTransport transport, ClientTransportConfig<?> config) {
        Object openTelemetryObj = config.getParameters().get(OTEL_OPEN_TELEMETRY_KEY);
        if (openTelemetryObj != null && openTelemetryObj instanceof OpenTelemetry openTelemetry) {
            return new OpenTelemetryClientPropagatorTransport(transport, openTelemetry);
        }
        // No OpenTelemetry configured, return unwrapped transport
        return transport;
    }

    @Override
    public int priority() {
        // Observability/tracing should be in the middle priority range
        // so it can observe other wrappers but doesn't interfere with security
        return 500;
    }
}
