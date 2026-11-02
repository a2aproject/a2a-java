package org.a2aproject.sdk.compat03.server.grpc.quarkus;

import io.quarkus.security.spi.runtime.AuthorizationController;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Alternative
@Priority(Interceptor.Priority.LIBRARY_AFTER + 1)
@Singleton
public class TestAuthorizationController_v0_3 extends AuthorizationController {

    @ConfigProperty(name = "test.authorization.enabled", defaultValue = "false")
    boolean enabled;

    @Override
    public boolean isAuthorizationEnabled() {
        return enabled;
    }
}
