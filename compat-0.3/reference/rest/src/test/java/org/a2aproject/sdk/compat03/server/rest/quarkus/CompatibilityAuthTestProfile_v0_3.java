package org.a2aproject.sdk.compat03.server.rest.quarkus;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public final class CompatibilityAuthTestProfile_v0_3 implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.ofEntries(
                Map.entry("test.identity.auto-auth", "false"),
                Map.entry("quarkus.test.security.auth.enabled", "false"),
                Map.entry("test.agent.security.enabled", "true"),
                Map.entry("test.authorization.enabled", "true"),
                Map.entry("quarkus.security.users.embedded.enabled", "true"),
                Map.entry("quarkus.security.users.embedded.plain-text", "true"),
                Map.entry("quarkus.security.users.embedded.users.testuser", "testpass"),
                Map.entry("quarkus.security.users.embedded.roles.testuser", "user"),
                Map.entry("quarkus.http.auth.basic", "true"),
                Map.entry("quarkus.http.auth.proactive", "true"),
                Map.entry(
                        "quarkus.arc.exclude-types",
                        "org.a2aproject.sdk.compat03.conversion.test.AgentExecutorProducer_v0_3"));
    }

    @Override
    public String getConfigProfile() {
        return "test";
    }
}
