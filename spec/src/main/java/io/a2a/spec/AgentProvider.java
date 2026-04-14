package io.a2a.spec;

import io.a2a.util.Assert;

/**
 * Represents the service provider of an agent.
 *
 * @param organization the name of the organization providing the agent
 * @param url          the URL of the provider's website or documentation
 */
public record AgentProvider(String organization, String url) {

    public AgentProvider {
        Assert.checkNotNullParam("organization", organization);
        Assert.checkNotNullParam("url", url);
    }
}
