package org.a2aproject.sdk.client.http;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.a2aproject.sdk.util.Assert.checkNotNullParam;

import org.a2aproject.sdk.grpc.utils.JSONRPCUtils;
import org.a2aproject.sdk.grpc.utils.ProtoUtils;
import org.a2aproject.sdk.jsonrpc.common.json.JsonProcessingException;
import org.a2aproject.sdk.spec.A2AClientError;
import org.a2aproject.sdk.spec.A2AClientJSONError;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.util.Utils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for fetching agent cards from A2A agents.
 *
 * <p>
 * Retrieves agent cards from the standard {@code /.well-known/agent-card.json} endpoint
 * with support for tenant-specific paths and authentication headers.
 *
 * <h2>Features</h2>
 * <ul>
 * <li>Standard agent card endpoint discovery ({@code /.well-known/agent-card.json})</li>
 * <li>Tenant-specific path support ({@code /tenant/.well-known/agent-card.json})</li>
 * <li>Custom card path support for non-standard agent card locations</li>
 * <li>Custom authentication header injection</li>
 * <li>Pluggable HTTP client via {@link A2AHttpClientFactory}</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Basic usage - fetch the agent card from a base URL
 * A2ACardResolver resolver = A2ACardResolver.builder()
 *     .baseUrl("http://localhost:9999")
 *     .build();
 * AgentCard card = resolver.getAgentCard();
 *
 * // With tenant path
 * A2ACardResolver resolver = A2ACardResolver.builder()
 *     .baseUrl("http://localhost:9999")
 *     .tenant("my-tenant")
 *     .build();
 * AgentCard card = resolver.getAgentCard();
 *
 * // With custom HTTP client and authentication
 * A2AHttpClient httpClient = A2AHttpClientFactory.create();
 * A2ACardResolver resolver = A2ACardResolver.builder()
 *     .httpClient(httpClient)
 *     .baseUrl("http://localhost:9999")
 *     .tenant("my-tenant")
 *     .authHeader("Authorization", "Bearer token")
 *     .build();
 * AgentCard card = resolver.getAgentCard();
 *
 * // With a custom agent card path
 * A2ACardResolver resolver = A2ACardResolver.builder()
 *     .baseUrl("http://localhost:9999")
 *     .agentCardPath("/custom/agent.json")
 *     .build();
 * AgentCard card = resolver.getAgentCard();
 *
 * // Using a complete URL (e.g., with path prefix like /spec03)
 * A2ACardResolver resolver = A2ACardResolver.builder()
 *     .baseUrl("https://example.com/spec03")
 *     .build();
 * AgentCard card = resolver.getAgentCard();
 * }</pre>
 *
 * @see AgentCard
 * @see A2AHttpClient
 */
public class A2ACardResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(A2ACardResolver.class);

    private final A2AHttpClient httpClient;
    private final String cardUrl;
    private final @Nullable Map<String, String> authHeaders;

    private A2ACardResolver(A2AHttpClient httpClient, String baseUrl, @Nullable String tenant, @Nullable String agentCardPath, @Nullable Map<String, String> authHeaders) throws A2AClientError {
        checkNotNullParam("httpClient", httpClient);
        checkNotNullParam("baseUrl", baseUrl);
        this.httpClient = httpClient;
        try {
            // Strip any well-known suffix from baseUrl before appending the tenant,
            // so that a full card URL like https://host/.well-known/agent-card.json + tenant
            // doesn't produce a malformed path.
            String cleanBase = Utils.stripWellKnownSuffix(baseUrl);
            String baseUrlWithTenant = Utils.buildBaseUrl(cleanBase, tenant);
            Utils.validateAbsoluteUrl(baseUrlWithTenant);
            this.cardUrl = (agentCardPath == null || agentCardPath.isEmpty())
                    ? Utils.buildCardUrl(baseUrlWithTenant, Utils.DEFAULT_AGENT_CARD_PATH)
                    : Utils.buildCardUrl(baseUrlWithTenant, agentCardPath);
        } catch (URISyntaxException e) {
            throw new A2AClientError("Invalid agent URL", e);
        }
        this.authHeaders = authHeaders != null ? Map.copyOf(authHeaders) : null;
        LOGGER.debug("Initialized A2ACardResolver with cardUrl={}", cardUrl);
    }

    /**
     * Creates a new builder for constructing an A2ACardResolver.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating A2ACardResolver instances with a fluent API.
     */
    public static class Builder {

        private @Nullable A2AHttpClient httpClient;
        private @Nullable String baseUrl;
        private @Nullable String tenant;
        private @Nullable String agentCardPath;
        private @Nullable Map<String, String> authHeaders;

        private Builder() {
        }

        /**
         * Sets the HTTP client to use for fetching agent cards.
         * If not called, a default client is created via {@link A2AHttpClientFactory#create()}.
         *
         * @param httpClient the HTTP client to use
         * @return this builder
         */
        public Builder httpClient(A2AHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * Sets the base URL for the agent.
         *
         * @param baseUrl the base URL, must not be null
         * @return this builder
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * Sets the tenant path to use when fetching the agent card.
         *
         * @param tenant the tenant path, may be null for no tenant
         * @return this builder
         */
        public Builder tenant(@Nullable String tenant) {
            this.tenant = tenant;
            return this;
        }

        /**
         * Sets a custom agent card path relative to the base URL.
         *
         * @param agentCardPath the custom agent card path; if null or empty, defaults to
         *                      {@code /.well-known/agent-card.json}
         * @return this builder
         */
        public Builder agentCardPath(@Nullable String agentCardPath) {
            this.agentCardPath = agentCardPath;
            return this;
        }

        /**
         * Sets the authentication headers to use when fetching the agent card.
         *
         * @param authHeaders the authentication headers, may be null
         * @return this builder
         */
        public Builder authHeaders(@Nullable Map<String, String> authHeaders) {
            if (authHeaders != null) {
                this.authHeaders = new HashMap<>(authHeaders);
            }
            return this;
        }

        /**
         * Adds a single authentication header.
         *
         * @param name the header name
         * @param value the header value
         * @return this builder
         */
        public Builder authHeader(String name, String value) {
            if (this.authHeaders == null) {
                this.authHeaders = new HashMap<>();
            }
            this.authHeaders.put(name, value);
            return this;
        }

        /**
         * Builds the A2ACardResolver instance.
         *
         * @return a new A2ACardResolver
         * @throws A2AClientError if the configuration is invalid
         * @throws IllegalArgumentException if baseUrl is null
         */
        public A2ACardResolver build() throws A2AClientError {
            A2AHttpClient client = httpClient != null ? httpClient : A2AHttpClientFactory.create();
            if (baseUrl == null) {
                throw new IllegalArgumentException("baseUrl must not be null");
            }
            return new A2ACardResolver(client, baseUrl, tenant, agentCardPath, authHeaders);
        }
    }

    /**
     * Fetches the agent card for this resolver's configured agent.
     *
     * <p>Fetches from the custom {@code agentCardPath} when one was supplied, otherwise fetches
     * from the standard {@code /.well-known/agent-card.json} endpoint. No automatic fallback
     * is performed; errors are propagated directly to the caller.
     *
     * @return the agent card
     * @throws A2AClientError If an HTTP or network error occurs fetching the card
     * @throws A2AClientJSONError If the response body cannot be decoded as JSON or validated
     * against the AgentCard schema
     */
    public AgentCard getAgentCard() throws A2AClientError, A2AClientJSONError {
        LOGGER.debug("Fetching agent card from URL: {}", cardUrl);

        A2AHttpClient.GetBuilder builder = httpClient.createGet()
                .url(cardUrl)
                .addHeader("Content-Type", "application/json");

        if (authHeaders != null) {
            builder.addHeaders(authHeaders);
        }

        String body;
        try {
            A2AHttpResponse response = builder.get();
            if (!response.success()) {
                LOGGER.debug("Failed to fetch agent card from {}, status: {}", cardUrl, response.status());
                throw new A2AClientError("Failed to obtain agent card: " + response.status());
            }
            body = response.body();
            LOGGER.debug("Successfully fetched agent card from {}", cardUrl);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new A2AClientError("Failed to obtain agent card", e);
        } catch (IOException e) {
            throw new A2AClientError("Failed to obtain agent card", e);
        }

        try {
            org.a2aproject.sdk.grpc.AgentCard.Builder agentCardBuilder = org.a2aproject.sdk.grpc.AgentCard.newBuilder();
            JSONRPCUtils.parseJsonString(body, agentCardBuilder, "", true);
            return ProtoUtils.FromProto.agentCard(agentCardBuilder);
        } catch (JsonProcessingException e) {
            throw new A2AClientJSONError("Could not unmarshal agent card response", e);
        }
    }
}
