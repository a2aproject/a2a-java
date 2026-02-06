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
 * @param protocolVersion the version of the A2A protocol this interface exposes (e.g., "1.0", "0.3") (required)
 * @see AgentCard
 * @see TransportProtocol
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record AgentInterface(String protocolBinding, String url, String tenant, String protocolVersion) {

    /** The default A2A Protocol version used when not explicitly specified. */
    public static final String CURRENT_PROTOCOL_VERSION = "1.0";

    /**
     * Compact constructor that validates required fields.
     *
     * @param protocolBinding the protocolBinding parameter (see class-level JavaDoc)
     * @param url the url parameter (see class-level JavaDoc)
     * @param tenant the tenant parameter (see class-level JavaDoc)
     * @param protocolVersion the protocolVersion parameter (see class-level JavaDoc)
     * @throws IllegalArgumentException if protocolBinding, url, or protocolVersion is null
     */
    public AgentInterface   {
        Assert.checkNotNullParam("protocolBinding", protocolBinding);
        Assert.checkNotNullParam("url", url);
        Assert.checkNotNullParam("tenant", tenant);

        if (protocolVersion == null || protocolVersion.isEmpty()) {
            protocolVersion = CURRENT_PROTOCOL_VERSION;
        }
    }

    /**
     * Convenience constructor for creating an AgentInterface with specified tenant and default protocol version.
     *
     * @param protocolBinding the protocol binding (see class-level JavaDoc)
     * @param url the endpoint URL (see class-level JavaDoc)
     * @param tenant the tenant (see class-level JavaDoc)
     */
    public AgentInterface(String protocolBinding, String url, String tenant) {
        this(protocolBinding, url, tenant, CURRENT_PROTOCOL_VERSION);
    }

    /**
     * Convenience constructor for creating an AgentInterface with default protocol version and no tenant.
     *
     * @param protocolBinding the protocol binding (see class-level JavaDoc)
     * @param url the endpoint URL (see class-level JavaDoc)
     */
    public AgentInterface(String protocolBinding, String url) {
        this(protocolBinding, url, "", CURRENT_PROTOCOL_VERSION);
    }
}
