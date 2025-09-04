package io.a2a.server.apps.quarkus;

import io.a2a.server.TransportMetadata;
import io.a2a.spec.TransportProtocol;

public class QuarkusJSONRPCTransportMetadata implements TransportMetadata {

    @Override
    public String getTransportProtocol() {
        return TransportProtocol.JSONRPC.asString();
    }
}
