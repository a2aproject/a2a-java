package org.a2aproject.sdk.tests.multiversion.rest;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Unremovable;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
@Unremovable
@IfBuildProperty(name = "test.identity.auto-auth", stringValue = "true", enableIfMissing = true)
public class TestIdentityProvider implements SecurityIdentityAugmentor {

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
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
