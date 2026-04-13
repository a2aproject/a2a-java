package org.a2aproject.sdk.compat03.spec;


import org.a2aproject.sdk.util.Assert;

/**
 * Declares a combination of a target URL and a transport protocol for interacting with the agent.
 */

public record AgentInterface(String transport, String url) {
    public AgentInterface {
        Assert.checkNotNullParam("transport", transport);
        Assert.checkNotNullParam("url", url);
    }
}
