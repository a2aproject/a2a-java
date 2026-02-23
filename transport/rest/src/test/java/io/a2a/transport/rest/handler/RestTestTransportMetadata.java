package io.a2a.transport.rest.handler;

import io.a2a.server.TransportMetadata;
import io.a2a.spec.TransportProtocol;

/**
 * Test implementation of TransportMetadata for REST transport testing.
 */
public class RestTestTransportMetadata implements TransportMetadata {

    /**
     * Returns the transport protocol used for REST communication.
     *
     * @return the HTTP JSON transport protocol identifier
     */
    @Override
    public String getTransportProtocol() {
        return TransportProtocol.HTTP_JSON.asString();
    }

}
