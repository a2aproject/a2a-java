package org.a2aproject.sdk.compat03.client.transport.jsonrpc;

import org.a2aproject.sdk.client.http.AbstractHttpEndpointValidationTest;
import org.a2aproject.sdk.compat03.spec.A2AClientException_v0_3;

class EndpointValidationTest extends AbstractHttpEndpointValidationTest {
    @Override
    protected void createTransport(String url) {
        new JSONRPCTransport_v0_3(null, null, url, null);
    }

    @Override
    protected void createThroughProvider(String url) throws A2AClientException_v0_3 {
        new JSONRPCTransportProvider_v0_3().create(null, null, url);
    }

    @Override
    protected Class<? extends Exception> providerException() {
        return A2AClientException_v0_3.class;
    }
}
