package io.a2a.client.transport.spi;

/**
 * Service provider interface for wrapping client transports with additional functionality.
 * Implementations can add cross-cutting concerns like tracing, metrics, logging, etc.
 *
 * <p>Wrappers are discovered via Java's ServiceLoader mechanism. To register a wrapper,
 * create a file {@code META-INF/services/io.a2a.client.transport.spi.ClientTransportWrapper}
 * containing the fully qualified class name of your implementation.
 *
 * <p>Wrappers are sorted by priority in descending order (highest priority first).
 * This interface implements {@link Comparable} to enable natural sorting.
 *
 * <p>Example implementation:
 * <pre>{@code
 * public class TracingWrapper implements ClientTransportWrapper {
 *     @Override
 *     public ClientTransport wrap(ClientTransport transport, ClientTransportConfig<?> config) {
 *         if (config.getParameters().containsKey("tracer")) {
 *             return new TracingTransport(transport, (Tracer) config.getParameters().get("tracer"));
 *         }
 *         return transport;
 *     }
 *
 *     @Override
 *     public int priority() {
 *         return 100; // Higher priority = wraps earlier (outermost)
 *     }
 * }
 * }</pre>
 */
public interface ClientTransportWrapper extends Comparable<ClientTransportWrapper> {

    /**
     * Wraps the given transport with additional functionality.
     *
     * <p>Implementations should check the configuration to determine if they should
     * actually wrap the transport. If the wrapper is not applicable (e.g., required
     * configuration is missing), return the original transport unchanged.
     *
     * @param transport the transport to wrap
     * @param config the transport configuration, may contain wrapper-specific parameters
     * @return the wrapped transport, or the original if wrapping is not applicable
     */
    ClientTransport wrap(ClientTransport transport, ClientTransportConfig<?> config);

    /**
     * Returns the priority of this wrapper. Higher priority wrappers are applied first
     * (wrap the transport earlier, resulting in being the outermost wrapper).
     *
     * <p>Default priority is 0. Suggested ranges:
     * <ul>
     *   <li>1000+ : Critical infrastructure (security, authentication)
     *   <li>500-999: Observability (tracing, metrics, logging)
     *   <li>100-499: Enhancement (caching, retry logic)
     *   <li>0-99: Optional features
     * </ul>
     *
     * @return the priority value, higher values = higher priority
     */
    default int priority() {
        return 0;
    }

    /**
     * Compares this wrapper with another based on priority.
     * Returns a negative integer, zero, or a positive integer as this wrapper
     * has higher priority than, equal to, or lower priority than the specified wrapper.
     *
     * <p>Note: This comparison is reversed (higher priority comes first) to enable
     * natural sorting in descending priority order.
     *
     * @param other the wrapper to compare to
     * @return negative if this has higher priority, positive if lower, zero if equal
     */
    @Override
    default int compareTo(ClientTransportWrapper other) {
        // Reverse comparison: higher priority should come first
        return Integer.compare(other.priority(), this.priority());
    }
}
