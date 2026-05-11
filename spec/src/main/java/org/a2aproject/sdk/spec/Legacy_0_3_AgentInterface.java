package org.a2aproject.sdk.spec;

import org.a2aproject.sdk.util.Assert;

/**
 * A legacy (v0.3) representation of an agent interface, using the v0.3 field names
 * ({@code transport} and {@code url}) for backward compatibility with older clients.
 * <p>
 * When included in the {@link AgentCard#additionalInterfaces()} field, this record
 * serializes with the JSON shape expected by v0.3 clients:
 * <pre>
 * {"transport": "GRPC", "url": "http://localhost:9999"}
 * </pre>
 *
 * @param transport the transport protocol (e.g., "JSONRPC", "GRPC", "HTTP+JSON")
 * @param url the endpoint URL
 */
public record Legacy_0_3_AgentInterface(String transport, String url) {

    public Legacy_0_3_AgentInterface {
        Assert.checkNotNullParam("transport", transport);
        Assert.checkNotNullParam("url", url);
    }
}
