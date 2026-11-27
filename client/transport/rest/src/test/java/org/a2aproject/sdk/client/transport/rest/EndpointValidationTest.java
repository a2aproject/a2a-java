package org.a2aproject.sdk.client.transport.rest;

import org.a2aproject.sdk.client.http.AbstractHttpEndpointValidationTest;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.AgentInterface;

class EndpointValidationTest extends AbstractHttpEndpointValidationTest {
    @Override
    protected void createTransport(String url) {
        new RestTransport(null, null, new AgentInterface("HTTP+JSON", url), null);
    }

    @Override
    protected void createThroughProvider(String url) throws A2AClientException {
        new RestTransportProvider().create(null, null, new AgentInterface("HTTP+JSON", url));
    }

    @Override
    protected Class<? extends Exception> providerException() {
        return A2AClientException.class;
    }
}
