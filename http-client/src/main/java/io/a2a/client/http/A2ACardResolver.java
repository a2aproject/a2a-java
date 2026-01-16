package io.a2a.client.http;


import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import io.a2a.grpc.utils.JSONRPCUtils;
import io.a2a.grpc.utils.ProtoUtils;
import io.a2a.jsonrpc.common.json.JsonProcessingException;
import io.a2a.spec.A2AClientError;
import io.a2a.spec.A2AClientJSONError;
import io.a2a.spec.AgentCard;
import io.a2a.spec.A2AError;
import org.jspecify.annotations.Nullable;

import static io.a2a.util.Assert.checkNotNullParam;

import io.a2a.spec.AgentInterface;

public class A2ACardResolver {
    private final A2AHttpClient httpClient;
    private final String url;
    private final @Nullable Map<String, String> authHeaders;

    private static final String DEFAULT_AGENT_CARD_PATH = "/.well-known/agent-card.json";

    /**
     * Get the agent card for an A2A agent. An HTTP client will be auto-selected via {@link A2AHttpClientFactory}.
     *
     * @param baseUrl the base URL for the agent whose agent card we want to retrieve, must not be null
     * @throws A2AClientError if the URL for the agent is invalid
     * @throws IllegalArgumentException if baseUrl is null
     */
    public A2ACardResolver(String baseUrl) throws A2AClientError {
        this(A2AHttpClientFactory.create(), baseUrl, null, null);
    }

    /**
     * Get the agent card for an A2A agent. An HTTP client will be auto-selected via {@link A2AHttpClientFactory}.
     *
     * @param baseUrl the base URL for the agent whose agent card we want to retrieve, must not be null
     * @param tenant the tenant path to use when fetching the agent card, may be null for no tenant
     * @throws A2AClientError if the URL for the agent is invalid
     * @throws IllegalArgumentException if baseUrl is null
     */
    public A2ACardResolver(String baseUrl, @Nullable String tenant) throws A2AClientError {
        this(A2AHttpClientFactory.create(), baseUrl, tenant, null);
    }

    /**
     * Constructs an A2ACardResolver with a specific HTTP client and base URL.
     *
     * @param httpClient the HTTP client to use for fetching the agent card, must not be null
     * @param baseUrl the base URL for the agent whose agent card we want to retrieve, must not be null
     * @param tenant the tenant path to use when fetching the agent card, may be null for no tenant
     * @throws A2AClientError if the URL for the agent is invalid
     * @throws IllegalArgumentException if httpClient or baseUrl is null
     */
    public A2ACardResolver(A2AHttpClient httpClient, String baseUrl, @Nullable String tenant) throws A2AClientError {
        this(httpClient, baseUrl, tenant, null);
    }

    /**
     * Constructs an A2ACardResolver with a specific HTTP client, base URL, and custom agent card path.
     *
     * @param httpClient the HTTP client to use for fetching the agent card, must not be null
     * @param baseUrl the base URL for the agent whose agent card we want to retrieve, must not be null
     * @param tenant the tenant path to use when fetching the agent card, may be null for no tenant
     * @param agentCardPath optional path to the agent card endpoint relative to the base
     *                      agent URL, defaults to "/.well-known/agent-card.json" if null or empty
     * @throws A2AClientError if the URL for the agent is invalid
     * @throws IllegalArgumentException if httpClient or baseUrl is null
     */
    public A2ACardResolver(A2AHttpClient httpClient, String baseUrl, @Nullable String tenant, @Nullable String agentCardPath) throws A2AClientError {
        this(httpClient, baseUrl, tenant, agentCardPath, null);
    }

    /**
     * Constructs an A2ACardResolver with full configuration including authentication headers.
     *
     * @param httpClient the HTTP client to use for fetching the agent card, must not be null
     * @param baseUrl the base URL for the agent whose agent card we want to retrieve, must not be null
     * @param tenant the tenant path to use when fetching the agent card, may be null for no tenant
     * @param agentCardPath optional path to the agent card endpoint relative to the base
     *                      agent URL, defaults to "/.well-known/agent-card.json" if null or empty
     * @param authHeaders the HTTP authentication headers to use, may be null
     * @throws A2AClientError if the URL for the agent is invalid
     * @throws IllegalArgumentException if httpClient or baseUrl is null
     */
    public A2ACardResolver(A2AHttpClient httpClient, String baseUrl, @Nullable String tenant, @Nullable String agentCardPath,
                           @Nullable Map<String, String> authHeaders) throws A2AClientError {
        checkNotNullParam("httpClient", httpClient);
        checkNotNullParam("baseUrl", baseUrl);

        this.httpClient = httpClient;
        String effectiveAgentCardPath = (agentCardPath == null || agentCardPath.isEmpty()) ? DEFAULT_AGENT_CARD_PATH : agentCardPath;
        try {
            this.url = new URI(io.a2a.util.Utils.buildBaseUrl(new AgentInterface("JSONRPC", baseUrl, ""), tenant)).resolve(effectiveAgentCardPath).toString();
        } catch (URISyntaxException e) {
            throw new A2AClientError("Invalid agent URL", e);
        }
        this.authHeaders = authHeaders;
    }

    /**
     * Get the agent card for the configured A2A agent.
     *
     * @return the agent card
     * @throws A2AClientError If an HTTP error occurs fetching the card
     * @throws A2AClientJSONError If the response body cannot be decoded as JSON or validated against the AgentCard schema
     */
    public AgentCard getAgentCard() throws A2AClientError, A2AClientJSONError {
        A2AHttpClient.GetBuilder builder = httpClient.createGet()
                .url(url)
                .addHeader("Content-Type", "application/json");

        if (authHeaders != null) {
            for (Map.Entry<String, String> entry : authHeaders.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        String body;
        try {
            A2AHttpResponse response = builder.get();
            if (!response.success()) {
                throw new A2AClientError("Failed to obtain agent card: " + response.status());
            }
            body = response.body();
        } catch (IOException | InterruptedException e) {
            throw new A2AClientError("Failed to obtain agent card", e);
        }

        try {
            io.a2a.grpc.AgentCard.Builder agentCardBuilder = io.a2a.grpc.AgentCard.newBuilder();
            JSONRPCUtils.parseJsonString(body, agentCardBuilder, "");
            return ProtoUtils.FromProto.agentCard(agentCardBuilder);
        } catch (A2AError | JsonProcessingException e) {
            throw new A2AClientJSONError("Could not unmarshal agent card response", e);
        }
    }
}
