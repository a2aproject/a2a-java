package org.a2aproject.sdk.compat03.spec;

import java.util.List;

import org.a2aproject.sdk.util.Assert;

/**
 * The authentication info for an agent.
 */
public record AuthenticationInfo(List<String> schemes, String credentials) {

    public AuthenticationInfo {
        Assert.checkNotNullParam("schemes", schemes);
    }
}
