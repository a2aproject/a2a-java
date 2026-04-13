package io.a2a.spec;


import io.a2a.util.Assert;

/**
 * Declares a combination of a target URL and a transport protocol for interacting with the agent.
 */

public record AgentInterface(String transport, String url) {
    public AgentInterface {
        Assert.checkNotNullParam("transport", transport);
        Assert.checkNotNullParam("url", url);
    }
}
