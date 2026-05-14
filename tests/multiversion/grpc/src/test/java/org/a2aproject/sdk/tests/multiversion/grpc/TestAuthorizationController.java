package org.a2aproject.sdk.tests.multiversion.grpc;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.security.spi.runtime.AuthorizationController;

/**
 * Disables authorization for regular gRPC tests.
 * <p>
 * The {@code @Authenticated} CDI interceptor checks {@link AuthorizationController#isAuthorizationEnabled()}
 * before enforcing. When disabled, {@code @Authenticated} becomes a no-op, allowing
 * regular tests to run without credentials.
 * <p>
 * {@link AuthTestProfile} sets {@code test.authorization.enabled=true} to enforce
 * real authentication for auth-specific tests.
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
