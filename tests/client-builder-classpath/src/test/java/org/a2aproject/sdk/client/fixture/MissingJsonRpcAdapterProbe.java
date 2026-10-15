package org.a2aproject.sdk.client.fixture;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import com.sun.net.httpserver.HttpServer;
import org.a2aproject.sdk.client.Client;
import org.a2aproject.sdk.client.http.A2ACardResolver;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.AgentCard;

public final class MissingJsonRpcAdapterProbe {

    private static final String LEGACY_AGENT_CARD = """
            {
              "protocolVersion": "0.3",
              "name": "Legacy agent",
              "description": "Legacy agent for classpath probing",
              "url": "http://127.0.0.1/a2a",
              "preferredTransport": "JSONRPC",
              "version": "1.0.0",
              "capabilities": {"streaming": false, "pushNotifications": false},
              "defaultInputModes": ["text/plain"],
              "defaultOutputModes": ["text/plain"],
              "skills": []
            }""";

    private MissingJsonRpcAdapterProbe() {
    }

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/.well-known/agent-card.json", exchange -> {
            byte[] response = LEGACY_AGENT_CARD.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            AgentCard card = A2ACardResolver.builder()
                    .baseUrl(baseUrl)
                    .supportedProtocolVersions(Set.of("0.3"))
                    .build()
                    .getAgentCard();
            if (card.supportedInterfaces().stream().noneMatch(agentInterface ->
                    "JSONRPC".equals(agentInterface.protocolBinding())
                            && "0.3".equals(agentInterface.protocolVersion()))) {
                throw new AssertionError("The 0.3 card was not resolved through the compatibility parser");
            }

            try {
                Client.builder(card)
                        .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder())
                        .build();
                throw new AssertionError("The missing JSON-RPC compatibility adapter was not reported");
            } catch (A2AClientException expected) {
                if (!expected.getMessage().contains("a2a-java-sdk-compat-0.3-client-adapter-jsonrpc")) {
                    throw new AssertionError("Unexpected diagnostic: " + expected.getMessage(), expected);
                }
                System.out.println(expected.getMessage());
            }
        } finally {
            server.stop(0);
        }
    }
}
