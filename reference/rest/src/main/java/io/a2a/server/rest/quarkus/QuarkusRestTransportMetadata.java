package io.a2a.server.rest.quarkus;

import io.a2a.server.TransportMetadata;
import io.a2a.spec.TransportProtocol;

public class QuarkusRestTransportMetadata implements TransportMetadata {
    @Override
    public String getTransportProtocol() {
        return TransportProtocol.HTTP_JSON.asString();
    }
}
