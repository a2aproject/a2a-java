package org.a2aproject.sdk.compat03.spec;

import org.a2aproject.sdk.util.Assert;

/**
 * Represents the service provider of an agent.
 */
public record AgentProvider_v0_3(String organization, String url) {

    public AgentProvider_v0_3 {
        Assert.checkNotNullParam("organization", organization);
        Assert.checkNotNullParam("url", url);
    }
}
