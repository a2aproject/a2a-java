package org.a2aproject.sdk.server.apps.common;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.arc.Unremovable;

/**
 * Test identity provider that always returns an authenticated user.
 * <p>
 * This allows regular tests to work with {@code @Authenticated} annotations
 * without requiring actual HTTP Basic Auth credentials.
 * <p>
 * This bean is disabled when using {@link AuthTestProfile}, which tests real authentication.
 */
@ApplicationScoped
@Unremovable
@IfBuildProperty(name = "test.identity.auto-auth", stringValue = "true", enableIfMissing = true)
public class TestIdentityProvider implements SecurityIdentityAugmentor {

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, io.quarkus.security.identity.AuthenticationRequestContext context) {
        // If anonymous, inject a test user
        if (identity.isAnonymous()) {
            return Uni.createFrom().item(QuarkusSecurityIdentity.builder()
                    .setPrincipal(() -> "testuser")
                    .addRole("user")
                    .setAnonymous(false)
                    .build());
        }
        return Uni.createFrom().item(identity);
    }
}
