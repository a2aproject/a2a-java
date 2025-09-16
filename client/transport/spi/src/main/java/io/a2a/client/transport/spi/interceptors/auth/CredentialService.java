package io.a2a.client.transport.spi.interceptors.auth;

import io.a2a.client.transport.spi.interceptors.ClientCallContext;

/**
 * Used to retrieve credentials.
 */
public interface CredentialService {

    /**
     * Retrieves a credential (e.g., token) for a security scheme.
     *
     * @param securitySchemeName the name of the security scheme
     * @param clientCallContext the client call context, which may be {@code null}.
     * @return the credential or {@code null} if the credential could not be retrieved
     */
    String getCredential(String securitySchemeName, ClientCallContext clientCallContext);
}
