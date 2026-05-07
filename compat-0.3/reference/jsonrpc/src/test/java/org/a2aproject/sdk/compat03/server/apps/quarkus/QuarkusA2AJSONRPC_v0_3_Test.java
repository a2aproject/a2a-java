package org.a2aproject.sdk.compat03.server.apps.quarkus;

import org.a2aproject.sdk.compat03.client.ClientBuilder_v0_3;
import org.a2aproject.sdk.compat03.conversion.AbstractA2AServerServerTest_v0_3;
import org.a2aproject.sdk.compat03.spec.TransportProtocol_v0_3;

public abstract class QuarkusA2AJSONRPC_v0_3_Test extends AbstractA2AServerServerTest_v0_3 {

    public QuarkusA2AJSONRPC_v0_3_Test() {
        super(8081);
    }

    @Override
    protected String getTransportProtocol() {
        return TransportProtocol_v0_3.JSONRPC.asString();
    }

    @Override
    protected String getTransportUrl() {
        return "http://localhost:8081";
    }

    @Override
    protected abstract void configureTransport(ClientBuilder_v0_3 builder);
}
