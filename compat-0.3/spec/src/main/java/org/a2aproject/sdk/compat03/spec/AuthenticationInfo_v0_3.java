package org.a2aproject.sdk.compat03.spec;

import java.util.List;

import org.a2aproject.sdk.util.Assert;

/**
 * The authentication info for an agent.
 */
public record AuthenticationInfo_v0_3(List<String> schemes, String credentials) {

    public AuthenticationInfo_v0_3 {
        Assert.checkNotNullParam("schemes", schemes);
    }
}
