package org.a2aproject.sdk.tests.multitenancy.grpc;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.security.spi.runtime.AuthorizationController;

/**
 * Disables authorization for gRPC multitenancy tests.
 * <p>
 * The {@code @Authenticated} CDI interceptor checks {@link AuthorizationController#isAuthorizationEnabled()}
 * before enforcing. When disabled, {@code @Authenticated} becomes a no-op, allowing
 * tests to call gRPC endpoints without credentials.
 * <p>
 * The default is {@code false}. To enforce real authentication in a specific test profile,
 * set {@code test.authorization.enabled=true}.
 */
@Alternative
@Priority(Interceptor.Priority.LIBRARY_AFTER + 1)
@Singleton
public class TestAuthorizationController extends AuthorizationController {

    @ConfigProperty(name = "test.authorization.enabled", defaultValue = "false")
    boolean enabled;

    @Override
    public boolean isAuthorizationEnabled() {
        return enabled;
    }
}
