package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.a2a.util.Assert;

/**
 * Declares a combination of a target URL and transport protocol for accessing an agent.
 * <p>
 * AgentInterface defines how clients can connect to and communicate with an agent using
 * a specific transport mechanism (such as JSON-RPC, gRPC, or REST) at a particular endpoint.
 * <p>
 * Agents may support multiple interfaces to allow flexibility in how clients communicate.
 * The {@link AgentCard} includes a primary interface (url + preferredTransport) and may
 * list additional interfaces for alternative access methods.
 * <p>
 * This class is immutable.
 *
 * @param transport the transport protocol name (e.g., "jsonrpc", "grpc", "rest") (required)
 * @param url the endpoint URL for this transport interface (required)
 * @see AgentCard
 * @see TransportProtocol
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentInterface(String transport, String url) {

    public AgentInterface {
        Assert.checkNotNullParam("transport", transport);
        Assert.checkNotNullParam("url", url);
    }
}
