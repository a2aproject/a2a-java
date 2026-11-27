package org.a2aproject.sdk.compat03.conversion;

/** Default endpoint for Quarkus tests that serve gRPC on the HTTP test port. */
public final class GrpcTestEndpoint {
    public static final int PORT = 8081;
    public static final String AUTHORITY = "localhost:" + PORT;

    private GrpcTestEndpoint() {
    }
}
