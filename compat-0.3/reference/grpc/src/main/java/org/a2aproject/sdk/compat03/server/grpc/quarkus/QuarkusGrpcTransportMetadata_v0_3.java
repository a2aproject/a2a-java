package org.a2aproject.sdk.compat03.server.grpc.quarkus;

import org.a2aproject.sdk.server.TransportMetadata;
import org.a2aproject.sdk.compat03.spec.TransportProtocol_v0_3;

public class QuarkusGrpcTransportMetadata_v0_3 implements TransportMetadata {
    @Override
    public String getTransportProtocol() {
        return TransportProtocol_v0_3.GRPC.asString();
    }
}
