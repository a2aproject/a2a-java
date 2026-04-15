package io.a2a.spec;

import java.util.List;

import io.a2a.util.Assert;

/**
 * The authentication info for an agent.
 *
 * @param schemes the list of authentication scheme identifiers
 * @param credentials optional credentials string for the authentication scheme
 */
public record AuthenticationInfo(List<String> schemes, String credentials) {

    public AuthenticationInfo {
        Assert.checkNotNullParam("schemes", schemes);
    }
}
