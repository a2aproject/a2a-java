package org.springframework.a2a.server.autoconfigure;

import io.a2a.server.TransportMetadata;
import io.a2a.spec.TransportProtocol;

public class SpringJSONRPCTransportMetadata implements TransportMetadata {

    @Override
    public String getTransportProtocol() {
        return TransportProtocol.JSONRPC.asString();
    }
}
