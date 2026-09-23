package org.a2aproject.sdk.compat03.server.apps.quarkus;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public final class CompatibilityTestProfile_v0_3 implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.arc.exclude-types",
                "org.a2aproject.sdk.compat03.conversion.test.AgentExecutorProducer_v0_3");
    }

    @Override
    public String getConfigProfile() {
        return "test";
    }
}
