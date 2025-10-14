package io.a2a.client.http;

import static io.a2a.util.Utils.unmarshalFrom;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import io.a2a.spec.A2AClientError;
import io.a2a.spec.A2AClientJSONError;
import io.a2a.spec.AgentCard;
import org.jspecify.annotations.Nullable;

public class A2ACardResolver {
    private final HttpClient httpClient;
    private final @Nullable Map<String, String> authHeaders;
    private final String agentCardPath;
    private static final String DEFAULT_AGENT_CARD_PATH = "/.well-known/agent-card.json";

    private static final TypeReference<AgentCard> AGENT_CARD_TYPE_REFERENCE = new TypeReference<>() {};

    /**
     * Get the agent card for an A2A agent.
     * The {@code HttpClient} will be used to fetch the agent card.
     *
     * @param baseUrl the base URL for the agent whose agent card we want to retrieve
     * @throws A2AClientError if the URL for the agent is invalid
     */
    public A2ACardResolver(String baseUrl) throws A2AClientError {
        this.httpClient = HttpClient.createHttpClient(baseUrl);
        this.authHeaders = null;

        try {
            String agentCardPath = new URI(baseUrl).getPath();

            if (agentCardPath.endsWith("/")) {
                agentCardPath = agentCardPath.substring(0, agentCardPath.length() - 1);
            }

            if (agentCardPath.isEmpty()) {
                this.agentCardPath = DEFAULT_AGENT_CARD_PATH;
            } else if (agentCardPath.endsWith(DEFAULT_AGENT_CARD_PATH)) {
                this.agentCardPath = agentCardPath;
            } else {
                this.agentCardPath = agentCardPath + DEFAULT_AGENT_CARD_PATH;
            }
        } catch (URISyntaxException e) {
            throw new A2AClientError("Invalid agent URL", e);
        }
    }

    /**
     * @param httpClient the http client to use
     * @throws A2AClientError if the URL for the agent is invalid
     */
    A2ACardResolver(HttpClient httpClient) throws A2AClientError {
        this(httpClient, null, null);
    }

    /**
     * @param httpClient the http client to use
     * @param agentCardPath optional path to the agent card endpoint relative to the base
     *                         agent URL, defaults to ".well-known/agent-card.json"
     * @throws A2AClientError if the URL for the agent is invalid
     */
    public A2ACardResolver(HttpClient httpClient, String agentCardPath) throws A2AClientError {
        this(httpClient, agentCardPath, null);
    }

    /**
     * @param httpClient the http client to use
     * @param agentCardPath optional path to the agent card endpoint relative to the base
     *                         agent URL, defaults to ".well-known/agent-card.json"
     * @param authHeaders the HTTP authentication headers to use. May be {@code null}
     * @throws A2AClientError if the URL for the agent is invalid
     */
    public A2ACardResolver(HttpClient httpClient, @Nullable  String agentCardPath,
                @Nullable  Map<String, String> authHeaders) throws A2AClientError {
        this.httpClient = httpClient;
        if (agentCardPath == null || agentCardPath.isEmpty()) {
            this.agentCardPath = DEFAULT_AGENT_CARD_PATH;
        } else if (agentCardPath.endsWith(DEFAULT_AGENT_CARD_PATH)) {
            this.agentCardPath = agentCardPath;
        } else {
            this.agentCardPath = agentCardPath + DEFAULT_AGENT_CARD_PATH;
        }
        this.authHeaders = authHeaders;
    }

    /**
     * Get the agent card for the configured A2A agent.
     *
     * @return the agent card
     * @throws A2AClientError If an HTTP error occurs fetching the card
     * @throws A2AClientJSONError f the response body cannot be decoded as JSON or validated against the AgentCard schema
     */
    public AgentCard getAgentCard() throws A2AClientError, A2AClientJSONError {
        HttpClient.GetRequestBuilder builder = httpClient.get(agentCardPath)
                .addHeader("Content-Type", "application/json");

        if (authHeaders != null) {
            for (Map.Entry<String, String> entry : authHeaders.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        String body;

        try {
            HttpResponse response = builder.send().get();
            if (!response.success()) {
                throw new A2AClientError("Failed to obtain agent card: " + response.statusCode());
            }
            body = response.body().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new A2AClientError("Failed to obtain agent card", e);
        }

        try {
            return unmarshalFrom(body, AGENT_CARD_TYPE_REFERENCE);
        } catch (JsonProcessingException e) {
            throw new A2AClientJSONError("Could not unmarshal agent card response", e);
        }
    }
}
