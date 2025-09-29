package io.a2a.common;

public final class A2AErrorMessages {

    private A2AErrorMessages() {
        // prevent instantiation
    }

    public static final String AUTHENTICATION_FAILED = "Authentication failed: Client credentials are missing or invalid";
    public static final String AUTHORIZATION_FAILED = "Authorization failed: Client does not have permission for the operation";
}
