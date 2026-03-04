package io.a2a.server.grpc.quarkus;

import io.a2a.server.TransportMetadata;
import io.a2a.spec.TransportProtocol;

/**
 * Transport metadata provider for the Quarkus gRPC reference implementation.
 *
 * <p>This class identifies the transport protocol used by the gRPC server implementation.
 * It is automatically discovered by the A2A server framework through CDI to provide
 * protocol-specific metadata to components that need to distinguish between different
 * transport implementations.
 *
 * <h2>CDI Integration</h2>
 * <p>This bean is automatically registered and can be injected where transport
 * protocol information is needed:
 * <pre>{@code
 * @Inject
 * TransportMetadata transportMetadata;
 *
 * public void logProtocol() {
 *     String protocol = transportMetadata.getTransportProtocol();
 *     // Returns "grpc" for this implementation
 * }
 * }</pre>
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>Identifying the active transport protocol in multi-transport deployments</li>
 *   <li>Conditional logic based on transport capabilities</li>
 *   <li>Logging and metrics collection with transport-specific tags</li>
 *   <li>Protocol-specific error handling or feature detection</li>
 * </ul>
 *
 * @see io.a2a.server.TransportMetadata
 * @see io.a2a.spec.TransportProtocol
 */
public class QuarkusGrpcTransportMetadata implements TransportMetadata {
    /**
     * Returns the transport protocol identifier for gRPC.
     *
     * @return the string "grpc" identifying this transport implementation
     */
    @Override
    public String getTransportProtocol() {
        return TransportProtocol.GRPC.asString();
    }
}
