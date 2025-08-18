package io.a2a.server.grpc.quarkus;

import io.a2a.server.apps.common.AbstractA2AServerTest;
import io.a2a.spec.TransportProtocol;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class QuarkusA2AGrpcTest extends AbstractA2AServerTest {

    public QuarkusA2AGrpcTest() {
        super(8081); // HTTP server port for utility endpoints
    }

    @Override
    protected String getTransportProtocol() {
        return TransportProtocol.GRPC.asString();
    }

    @Override
    protected String getTransportUrl() {
        return "localhost:9001"; // gRPC server runs on port 9001
    }
}