package org.a2aproject.sdk.compat03.client.http;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import static org.a2aproject.sdk.util.Assert.checkNotNullParam;

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.A2AHttpClientFactory;
import org.a2aproject.sdk.client.http.A2AHttpResponse;
import org.a2aproject.sdk.compat03.json.JsonProcessingException_v0_3;
import org.a2aproject.sdk.compat03.json.JsonUtil_v0_3;
import org.a2aproject.sdk.compat03.spec.A2AClientError_v0_3;
import org.a2aproject.sdk.compat03.spec.A2AClientJSONError_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.util.Utils;
import org.jspecify.annotations.Nullable;

public class A2ACardResolver_v0_3 {
    private final A2AHttpClient httpClient;
    private final String url;
    private final @Nullable Map<String, String> authHeaders;

    /**
     * Get the agent card for an A2A agent.
     * The HTTP client will be auto-selected via {@link A2AHttpClientFactory}.
     *
     * @param baseUrl the base URL for the agent whose agent card we want to retrieve
     * @throws A2AClientError_v0_3 if the URL for the agent is invalid
     */
    public A2ACardResolver_v0_3(String baseUrl) throws A2AClientError_v0_3 {
        this(A2AHttpClientFactory.create(), baseUrl, null, null);
    }

    /**
     * Constructs an A2ACardResolver with a specific HTTP client and base URL.
     *
     * @param httpClient the http client to use
     * @param baseUrl the base URL for the agent whose agent card we want to retrieve
     * @throws A2AClientError_v0_3 if the URL for the agent is invalid
     */
    public A2ACardResolver_v0_3(A2AHttpClient httpClient, String baseUrl) throws A2AClientError_v0_3 {
        this(httpClient, baseUrl, null, null);
    }

    /**
     * @param httpClient the http client to use
     * @param baseUrl the base URL for the agent whose agent card we want to retrieve
     * @param agentCardPath optional path to the agent card endpoint relative to the base
     *                         agent URL, defaults to ".well-known/agent-card.json"
     * @throws A2AClientError_v0_3 if the URL for the agent is invalid
     */
    public A2ACardResolver_v0_3(A2AHttpClient httpClient, String baseUrl, String agentCardPath) throws A2AClientError_v0_3 {
        this(httpClient, baseUrl, agentCardPath, null);
    }

    /**
     * @param httpClient the http client to use
     * @param baseUrl the base URL for the agent whose agent card we want to retrieve
     * @param agentCardPath optional path to the agent card endpoint relative to the base
     *                         agent URL, defaults to ".well-known/agent-card.json"
     * @param authHeaders the HTTP authentication headers to use. May be {@code null}
     * @throws A2AClientError_v0_3 if the URL for the agent is invalid
     */
    public A2ACardResolver_v0_3(A2AHttpClient httpClient, String baseUrl, @Nullable String agentCardPath,
                                @Nullable Map<String, String> authHeaders) throws A2AClientError_v0_3 {
        checkNotNullParam("httpClient", httpClient);
        checkNotNullParam("baseUrl", baseUrl);
        this.httpClient = httpClient;
        String effectiveAgentCardPath = agentCardPath == null || agentCardPath.isEmpty() ? Utils.DEFAULT_AGENT_CARD_PATH : agentCardPath;
        try {
            Utils.validateAbsoluteUrl(baseUrl);
            this.url = Utils.buildCardUrl(Utils.stripWellKnownSuffix(baseUrl), effectiveAgentCardPath);
        } catch (URISyntaxException e) {
            throw new A2AClientError_v0_3("Invalid agent URL", e);
        }
        this.authHeaders = authHeaders != null ? Map.copyOf(authHeaders) : null;
    }

    /**
     * Get the agent card for the configured A2A agent.
     *
     * @return the agent card
     * @throws A2AClientError_v0_3 If an HTTP error occurs fetching the card
     * @throws A2AClientJSONError_v0_3 If the response body cannot be decoded as JSON or validated against the AgentCard schema
     */
    public AgentCard_v0_3 getAgentCard() throws A2AClientError_v0_3, A2AClientJSONError_v0_3 {
        A2AHttpClient.GetBuilder builder = httpClient.createGet()
                .url(url)
                .addHeader("Content-Type", "application/json");

        if (authHeaders != null) {
            builder.addHeaders(authHeaders);
        }

        String body;
        try {
            A2AHttpResponse response = builder.get();
            if (!response.success()) {
                throw new A2AClientError_v0_3("Failed to obtain agent card: " + response.status());
            }
            body = response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new A2AClientError_v0_3("Failed to obtain agent card", e);
        } catch (IOException e) {
            throw new A2AClientError_v0_3("Failed to obtain agent card", e);
        }

        try {
            return JsonUtil_v0_3.fromJson(body, AgentCard_v0_3.class);
        } catch (JsonProcessingException_v0_3 e) {
            throw new A2AClientJSONError_v0_3("Could not unmarshal agent card response", e);
        }

    }


}
