package org.a2aproject.sdk.compat03.server.grpc.quarkus;

import org.a2aproject.sdk.server.TransportMetadata;
import org.a2aproject.sdk.compat03.spec.TransportProtocol;

public class QuarkusGrpcTransportMetadata implements TransportMetadata {
    @Override
    public String getTransportProtocol() {
        return TransportProtocol.GRPC.asString();
    }
}
