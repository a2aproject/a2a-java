package io.a2a.client.transport.spi.interceptors.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.a2a.client.transport.spi.interceptors.ClientCallContext;

/**
 * A simple in-memory store for session-keyed credentials.
 * This class uses the 'sessionId' from the {@code ClientCallContext} state to
 * store and retrieve credentials
 */
public class InMemoryContextCredentialService implements CredentialService {

    private static final String SESSION_ID = "sessionId";

    // maps a sessionId to a map of security scheme names to credentials
    private final ConcurrentMap<String, ConcurrentMap<String, String>> credentialStore;

    public InMemoryContextCredentialService() {
        credentialStore = new ConcurrentHashMap<>();
    }

    @Override
    public String getCredential(String securitySchemeName,
                                ClientCallContext clientCallContext) {
        if (clientCallContext == null || !clientCallContext.getState().containsKey(SESSION_ID)) {
            // no credential to retrieve
            return null;
        }

        String sessionId = (String) clientCallContext.getState().get(SESSION_ID);
        Map<String, String> sessionCredentials = credentialStore.get(sessionId);
        if (sessionCredentials == null) {
            return null;
        }
        return sessionCredentials.get(securitySchemeName);
    }

    /**
     * Method to populate the in-memory credential service.
     *
     * @param sessionId the session ID
     * @param securitySchemeName the name of the security scheme
     * @param credential the credential string
     */
    public void setCredential(String sessionId, String securitySchemeName, String credential) {
        credentialStore.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>()).put(securitySchemeName, credential);
    }
}
