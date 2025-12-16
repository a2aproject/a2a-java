package io.a2a.spec;

import io.a2a.util.Assert;

/**
 * Declares a combination of a target URL and protocol binding for accessing an agent.
 * <p>
 * AgentInterface defines how clients can connect to and communicate with an agent using
 * a specific protocol binding at a particular endpoint. The protocol binding is an open-form
 * string that can be extended for other protocol bindings. Core officially supported bindings
 * are JSONRPC, GRPC, and HTTP+JSON.
 * <p>
 * Agents may support multiple interfaces to allow flexibility in how clients communicate.
 * The {@link AgentCard#supportedInterfaces()} field contains an ordered list of interfaces,
 * with the first entry being the preferred method for accessing the agent.
 * <p>
 * This class is immutable.
 *
 * @param protocolBinding the protocol binding supported at this URL (e.g., "JSONRPC", "GRPC", "HTTP+JSON") (required)
 * @param url the endpoint URL where this interface is available; must be a valid absolute HTTPS URL in production
 * (required)
 * @param tenant the tenant to be set in the request when calling the agent.
 * @see AgentCard
 * @see TransportProtocol
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record AgentInterface(String protocolBinding, String url, String tenant) {

    /**
     * Compact constructor that validates required fields.
     *
     * @throws IllegalArgumentException if protocolBinding or url is null
     */
    public AgentInterface   {
        Assert.checkNotNullParam("protocolBinding", protocolBinding);
        Assert.checkNotNullParam("url", url);
        Assert.checkNotNullParam("tenant", tenant);
    }

    /**
     * Convenience constructor for creating an AgentInterface without a tenant.
     *
     * @param protocolBinding the protocol binding (see class-level JavaDoc)
     * @param url the endpoint URL (see class-level JavaDoc)
     */
    public AgentInterface(String protocolBinding, String url) {
        this(protocolBinding, url, "");
    }
}
