package org.a2aproject.sdk.common;

/** Standard error message constants for A2A authentication and authorization failures. */
public final class A2AErrorMessages {

    private A2AErrorMessages() {
    }

    /** Error message returned when client credentials are missing or invalid. */
    public static final String AUTHENTICATION_FAILED = "Authentication failed: Client credentials are missing or invalid";
    /** Error message returned when the client lacks permission for the requested operation. */
    public static final String AUTHORIZATION_FAILED = "Authorization failed: Client does not have permission for the operation";
}
