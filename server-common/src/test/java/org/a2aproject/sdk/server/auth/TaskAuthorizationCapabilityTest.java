package org.a2aproject.sdk.server.auth;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class TaskAuthorizationCapabilityTest {

    @Test
    void existingProviderHasNoCapabilities() {
        TaskAuthorizationProvider provider = new SimpleTaskAuthorizationProvider();

        assertTrue(provider.getCapability(Runnable.class).isEmpty());
    }

    @Test
    void overridingProviderExposesOnlySupportedTypedCapability() {
        Runnable capability = () -> {};
        TaskAuthorizationProvider provider =
                new SimpleTaskAuthorizationProvider() {
                    @Override
                    public <T> Optional<T> getCapability(Class<T> capabilityType) {
                        return capabilityType == Runnable.class
                                ? Optional.of(capabilityType.cast(capability))
                                : Optional.empty();
                    }
                };

        assertSame(capability, provider.getCapability(Runnable.class).orElseThrow());
        assertTrue(provider.getCapability(AutoCloseable.class).isEmpty());
    }
}
