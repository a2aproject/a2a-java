package org.a2aproject.sdk.extras.multitenancy.tests;

import io.quarkus.security.spi.runtime.AuthorizationController;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Disables authorization for multitenancy tests, across every transport.
 * <p>
 * The {@code @Authenticated} CDI interceptor checks {@link AuthorizationController#isAuthorizationEnabled()}
 * before enforcing. CDI resolves at most one {@code AuthorizationController} bean; declaring this
 * {@code @Alternative} with a priority above the built-in controller makes CDI pick it instead, so
 * when disabled, {@code @Authenticated} becomes a no-op and tests can call endpoints without credentials.
 * <p>
 * The default is {@code false}. To exercise real authentication in a specific test profile,
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
