package io.a2a.server.apps.quarkus;

import io.a2a.server.TransportMetadata;
import io.a2a.spec.TransportProtocol;

/**
 * Transport metadata provider for the Quarkus JSON-RPC reference implementation.
 *
 * <p>This class identifies the transport protocol used by the JSON-RPC server implementation.
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
 *     // Returns "jsonrpc" for this implementation
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
public class QuarkusJSONRPCTransportMetadata implements TransportMetadata {

    /**
     * Returns the transport protocol identifier for JSON-RPC.
     *
     * @return the string "jsonrpc" identifying this transport implementation
     */
    @Override
    public String getTransportProtocol() {
        return TransportProtocol.JSONRPC.asString();
    }
}
