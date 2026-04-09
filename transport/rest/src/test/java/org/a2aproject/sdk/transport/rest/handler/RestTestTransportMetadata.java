package org.a2aproject.sdk.transport.rest.handler;

import org.a2aproject.sdk.server.TransportMetadata;
import org.a2aproject.sdk.spec.TransportProtocol;

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
