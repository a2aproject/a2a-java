package io.a2a.spec;

import io.a2a.util.Assert;

/**
 * Represents the service provider of an agent.
 */
public record AgentProvider(String organization, String url) {

    public AgentProvider {
        Assert.checkNotNullParam("organization", organization);
        Assert.checkNotNullParam("url", url);
    }
}
