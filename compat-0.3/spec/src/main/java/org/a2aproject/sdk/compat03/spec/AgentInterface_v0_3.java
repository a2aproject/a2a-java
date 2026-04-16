package org.a2aproject.sdk.compat03.spec;


import org.a2aproject.sdk.util.Assert;

/**
 * Declares a combination of a target URL and a transport protocol for interacting with the agent.
 */

public record AgentInterface_v0_3(String transport, String url) {
    public AgentInterface_v0_3 {
        Assert.checkNotNullParam("transport", transport);
        Assert.checkNotNullParam("url", url);
    }
}
