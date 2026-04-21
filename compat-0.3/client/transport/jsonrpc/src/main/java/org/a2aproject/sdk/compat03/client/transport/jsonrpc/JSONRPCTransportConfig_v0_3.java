package org.a2aproject.sdk.compat03.client.transport.jsonrpc;

import org.a2aproject.sdk.compat03.client.transport.spi.ClientTransportConfig_v0_3;
import org.a2aproject.sdk.compat03.client.http.A2AHttpClient_v0_3;

public class JSONRPCTransportConfig_v0_3 extends ClientTransportConfig_v0_3<JSONRPCTransport_v0_3> {

    private final A2AHttpClient_v0_3 httpClient;

    public JSONRPCTransportConfig_v0_3() {
        this.httpClient = null;
    }

    public JSONRPCTransportConfig_v0_3(A2AHttpClient_v0_3 httpClient) {
        this.httpClient = httpClient;
    }

    public A2AHttpClient_v0_3 getHttpClient() {
        return httpClient;
    }
}