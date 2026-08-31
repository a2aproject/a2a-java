package org.a2aproject.sdk.server.multitenancy;

/**
 * Thrown when a tenant-specific public agent card is requested but no card is registered
 * for that tenant. Callers should map this to an HTTP 404 response.
 */
public class TenantNotFoundException extends RuntimeException {

    private static final String MESSAGE_PREFIX = "No public agent card registered for tenant: ";

    private final String tenant;

    public TenantNotFoundException(String tenant) {
        super(MESSAGE_PREFIX + tenant);
        this.tenant = tenant;
    }

    public String getTenant() {
        return tenant;
    }

    /**
     * Returns the response body text for this exception (guaranteed non-null).
     *
     * @return the response message
     */
    public String getResponseMessage() {
        return MESSAGE_PREFIX + tenant;
    }
}
