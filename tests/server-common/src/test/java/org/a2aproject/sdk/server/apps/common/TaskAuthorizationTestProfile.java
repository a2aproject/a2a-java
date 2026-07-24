package org.a2aproject.sdk.server.apps.common;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class TaskAuthorizationTestProfile extends AuthTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        Map<String, String> config = new HashMap<>(super.getConfigOverrides());
        config.put("quarkus.security.users.embedded.users.userB", "passB");
        config.put("quarkus.security.users.embedded.roles.userB", "user");
        config.put("test.task-authorization.enabled", "true");
        return config;
    }
}
