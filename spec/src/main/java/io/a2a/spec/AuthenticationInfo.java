package io.a2a.spec;

import java.util.List;

import io.a2a.util.Assert;

/**
 * The authentication info for an agent.
 */
public record AuthenticationInfo(List<String> schemes, String credentials) {

    public AuthenticationInfo {
        Assert.checkNotNullParam("schemes", schemes);
    }
}
