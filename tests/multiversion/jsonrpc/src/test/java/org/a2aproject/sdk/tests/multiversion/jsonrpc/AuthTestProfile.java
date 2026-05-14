package org.a2aproject.sdk.tests.multiversion.jsonrpc;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class AuthTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.ofEntries(
                Map.entry("test.identity.auto-auth", "false"),
                Map.entry("test.agent.security.enabled", "true"),
                Map.entry("quarkus.security.users.embedded.enabled", "true"),
                Map.entry("quarkus.security.users.embedded.plain-text", "true"),
                Map.entry("quarkus.security.users.embedded.users.testuser", "testpass"),
                Map.entry("quarkus.security.users.embedded.roles.testuser", "user"),
                Map.entry("quarkus.http.auth.basic", "true"),
                Map.entry("quarkus.http.auth.proactive", "true")
        );
    }

    @Override
    public String getConfigProfile() {
        return "test";
    }
}
