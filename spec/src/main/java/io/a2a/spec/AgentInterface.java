package io.a2a.spec;


import io.a2a.util.Assert;

/**
 * Declares a combination of a target URL and a transport protocol for interacting with the agent.
 *
 * @param transport the transport protocol identifier (e.g., "jsonrpc", "grpc")
 * @param url the endpoint URL for this transport
 */
public record AgentInterface(String transport, String url) {
    public AgentInterface {
        Assert.checkNotNullParam("transport", transport);
        Assert.checkNotNullParam("url", url);
    }
}
