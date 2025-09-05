package io.a2a.transport.rest.handler;

import io.a2a.server.TransportMetadata;
import io.a2a.spec.TransportProtocol;

public class RestTestTransportMetadata implements TransportMetadata {
    @Override
    public String getTransportProtocol() {
        return TransportProtocol.HTTP_JSON.asString();
    }

}
