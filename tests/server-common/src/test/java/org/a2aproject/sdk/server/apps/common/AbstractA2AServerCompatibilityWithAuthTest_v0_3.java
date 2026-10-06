package org.a2aproject.sdk.server.apps.common;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.a2aproject.sdk.client.Client;
import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.config.ClientConfig;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** Authentication support for v1 compatibility clients talking to v0.3 servers. */
public abstract class AbstractA2AServerCompatibilityWithAuthTest_v0_3
        extends AbstractA2AServerCompatibilityTest_v0_3 {

    protected static final String TEST_USERNAME = "testuser";
    protected static final String TEST_PASSWORD = "testpass";
    protected static final String BASIC_AUTH_SCHEME_NAME = "basicAuth";

    protected static String getEncodedCredentials() {
        return Base64.getEncoder().encodeToString(
                (TEST_USERNAME + ":" + TEST_PASSWORD).getBytes(StandardCharsets.UTF_8));
    }

    private Client authenticatedClient;
    private Client unauthenticatedClient;

    protected AbstractA2AServerCompatibilityWithAuthTest_v0_3(int serverPort) {
        super(serverPort);
    }

    protected abstract void configureTransportWithAuth(ClientBuilder builder);

    @Override
    protected Client createClient(boolean streaming) throws A2AClientException {
        return createAuthenticatedClient(streaming, false);
    }

    @Override
    protected Client createPollingClient() throws A2AClientException {
        return createAuthenticatedClient(false, true);
    }

    protected Client createAuthenticatedClient() throws A2AClientException {
        return createAuthenticatedClient(false, false);
    }

    private Client createAuthenticatedClient(boolean streaming, boolean polling) throws A2AClientException {
        ClientBuilder builder = Client.builder(getAgentCard())
                .clientConfig(new ClientConfig.Builder().setStreaming(streaming).setPolling(polling).build());
        configureTransportWithAuth(builder);
        Client created = builder.build();
        registerCreatedClient(created);
        return created;
    }

    protected Client createUnauthenticatedClient() throws A2AClientException {
        ClientBuilder builder = Client.builder(getAgentCard())
                .clientConfig(new ClientConfig.Builder().setStreaming(false).build());
        configureTransport(builder);
        Client created = builder.build();
        registerCreatedClient(created);
        return created;
    }

    protected Client getAuthenticatedClient() throws A2AClientException {
        if (authenticatedClient == null) {
            authenticatedClient = createAuthenticatedClient();
        }
        return authenticatedClient;
    }

    protected Client getUnauthenticatedClient() throws A2AClientException {
        if (unauthenticatedClient == null) {
            unauthenticatedClient = createUnauthenticatedClient();
        }
        return unauthenticatedClient;
    }

    @Test
    public void testGetTaskRequiresAuthenticationUnauthenticated() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            A2AClientException error = org.junit.jupiter.api.Assertions.assertThrows(A2AClientException.class,
                    () -> getUnauthenticatedClient().getTask(new TaskQueryParams(MINIMAL_TASK.id())));
            assertTrue(error.getMessage().contains("Authentication failed")
                    || error.getMessage().contains("401")
                    || error.getMessage().contains("Unauthorized"), error.getMessage());
        } finally {
            deleteTaskInTaskStore(MINIMAL_TASK.id());
        }
    }

    @Test
    public void testGetTaskWithAuthentication() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            assertNotNull(getAuthenticatedClient().getTask(new TaskQueryParams(MINIMAL_TASK.id())));
        } finally {
            deleteTaskInTaskStore(MINIMAL_TASK.id());
        }
    }

    @Test
    public void testGetAgentCardIsPublic() {
        assertNotNull(getAgentCard());
        assertNotNull(getAgentCard().supportedInterfaces());
    }

    @Override
    void closeCreatedClients() {
        super.closeCreatedClients();
        authenticatedClient = null;
        unauthenticatedClient = null;
    }

}
