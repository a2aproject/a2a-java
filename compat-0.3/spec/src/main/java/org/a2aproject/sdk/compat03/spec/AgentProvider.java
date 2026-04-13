package org.a2aproject.sdk.compat03.spec;

import org.a2aproject.sdk.util.Assert;

/**
 * Represents the service provider of an agent.
 */
public record AgentProvider(String organization, String url) {

    public AgentProvider {
        Assert.checkNotNullParam("organization", organization);
        Assert.checkNotNullParam("url", url);
    }
}
