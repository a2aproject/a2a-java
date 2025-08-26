package io.a2a.server.apps.quarkus;

import io.a2a.server.apps.common.AbstractA2AServerTest;
import io.a2a.spec.TransportProtocol;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class QuarkusA2AJSONRPCTest extends AbstractA2AServerTest {

    public QuarkusA2AJSONRPCTest() {
        super(8081);
    }

    @Override
    protected String getTransportProtocol() {
        return TransportProtocol.JSONRPC.asString();
    }

    @Override
    protected String getTransportUrl() {
        return "http://localhost:8081";
    }
}
