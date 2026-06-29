package org.a2aproject.sdk.compat03.conversion;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class TaskAuthorizationTestProfile_v0_3 extends AuthTestProfile_v0_3 {

    @Override
    public Map<String, String> getConfigOverrides() {
        Map<String, String> config = new HashMap<>(super.getConfigOverrides());
        config.put("quarkus.security.users.embedded.users.userB", "passB");
        config.put("quarkus.security.users.embedded.roles.userB", "user");
        config.put("test.task-authorization.enabled", "true");
        return config;
    }
}
