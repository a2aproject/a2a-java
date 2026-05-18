package org.a2aproject.sdk.client.http;

import static org.a2aproject.sdk.util.Assert.checkNotNullParam;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import org.a2aproject.sdk.grpc.utils.JSONRPCUtils;
import org.a2aproject.sdk.grpc.utils.ProtoUtils;
import org.a2aproject.sdk.jsonrpc.common.json.JsonProcessingException;
import org.a2aproject.sdk.spec.A2AClientError;
import org.a2aproject.sdk.spec.A2AClientJSONError;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
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
 * // Basic usage - fetch the well-known agent card from a base URL
 * A2ACardResolver resolver = A2ACardResolver.builder()
 *     .baseUrl("http://localhost:9999")
 *     .build();
 * AgentCard card = resolver.getWellKnownAgentCard();
 *
 * // With tenant path
 * A2ACardResolver resolver = A2ACardResolver.builder()
 *     .baseUrl("http://localhost:9999")
 *     .tenant("my-tenant")
 *     .build();
 * AgentCard card = resolver.getWellKnownAgentCard();
 *
 * // With custom HTTP client and authentication
 * A2AHttpClient httpClient = A2AHttpClientFactory.create();
 * A2ACardResolver resolver = A2ACardResolver.builder()
 *     .httpClient(httpClient)
 *     .baseUrl("http://localhost:9999")
 *     .tenant("my-tenant")
 *     .authHeader("Authorization", "Bearer token")
 *     .build();
 * AgentCard card = resolver.getWellKnownAgentCard();
 *
 * // With a custom agent card path
 * A2ACardResolver resolver = A2ACardResolver.builder()
 *     .baseUrl("http://localhost:9999")
 *     .agentCardPath("/custom/agent.json")
 *     .build();
 * AgentCard card = resolver.getConfiguredAgentCard();
 *
 * // Using a complete URL (e.g., with path prefix like /spec03)
 * A2ACardResolver resolver = A2ACardResolver.builder()
 *     .baseUrl("https://example.com/spec03")
 *     .build();
 * AgentCard card = resolver.getWellKnownAgentCard();
 * }</pre>
 *
 * @see AgentCard
 * @see A2AHttpClient
 */
public class A2ACardResolver {

    private static final String DEFAULT_AGENT_CARD_PATH = "/.well-known/agent-card.json";
    private static final Logger LOGGER = LoggerFactory.getLogger(A2ACardResolver.class);

    private final A2AHttpClient httpClient;
    /**
     * URL used by {@link #getWellKnownAgentCard()}.
     */
    private final String wellKnownUrl;
    /**
     * URL used by {@link #getConfiguredAgentCard()}.
     */
    private final String configuredAgentCardUrl;
    private final @Nullable Map<String, String> authHeaders;

    private A2ACardResolver(A2AHttpClient httpClient, String baseUrl, @Nullable String tenant, @Nullable String agentCardPath, @Nullable Map<String, String> authHeaders) throws A2AClientError {
        checkNotNullParam("httpClient", httpClient);
        checkNotNullParam("baseUrl", baseUrl);
        this.httpClient = httpClient;
        try {
            URI builtBaseUri = new URI(org.a2aproject.sdk.util.Utils.buildBaseUrl(new AgentInterface("JSONRPC", baseUrl, ""), tenant));
            this.wellKnownUrl = resolveAgentCardUrl(builtBaseUri, DEFAULT_AGENT_CARD_PATH).toString();
            this.configuredAgentCardUrl = (agentCardPath == null || agentCardPath.isBlank())
                    ? builtBaseUri.toString()
                    : resolveAgentCardUrl(builtBaseUri, agentCardPath).toString();

            LOGGER.debug("Initialized A2ACardResolver with wellKnownUrl={}, configuredAgentCardUrl={}", wellKnownUrl, configuredAgentCardUrl);
        } catch (URISyntaxException e) {
            throw new A2AClientError("Invalid agent URL", e);
        }
        this.authHeaders = authHeaders;
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

        private @Nullable
        A2AHttpClient httpClient;
        private @Nullable
        String baseUrl;
        private @Nullable
        String tenant;
        private @Nullable
        String agentCardPath;
        private @Nullable
        Map<String, String> authHeaders;

        private Builder() {
        }

        /**
         * Sets the HTTP client to use for fetching agent cards.
         *
         * @param httpClient the HTTP client, if null a default client will be created
         * @return this builder
         */
        public Builder httpClient(@Nullable A2AHttpClient httpClient) {
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
         * @param agentCardPath the custom agent card path, may be null to use base URL as-is
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
            this.authHeaders = authHeaders;
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
                this.authHeaders = new java.util.HashMap<>();
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
     * Fetches the agent card from the standard {@code /.well-known/agent-card.json} endpoint.
     *
     * @return the agent card
     * @throws A2AClientError If an HTTP error occurs fetching the card
     * @throws A2AClientJSONError If the response body cannot be decoded as JSON or validated against the AgentCard
     * schema
     */
    public AgentCard getWellKnownAgentCard() throws A2AClientError, A2AClientJSONError {
        return fetchAgentCard(this.wellKnownUrl);
    }

    /**
     * Fetches the agent card from the configured card URL.
     *
     * <p>
     * Uses the configured agent card URL, which is either:
     * <ul>
     * <li>the configured {@code baseUrl} as-is when no {@code agentCardPath} was supplied, or</li>
     * <li>the {@code agentCardPath} resolved relative to the configured {@code baseUrl}.</li>
     * </ul>
     *
     * @return the agent card
     * @throws A2AClientError If an HTTP error occurs fetching the card
     * @throws A2AClientJSONError If the response body cannot be decoded as JSON or validated against the AgentCard
     * schema
     */
    public AgentCard getConfiguredAgentCard() throws A2AClientError, A2AClientJSONError {
        return fetchAgentCard(this.configuredAgentCardUrl);
    }

    /**
     * @deprecated Use {@link #getConfiguredAgentCard()} for fetching the configured card URL, or
     * {@link #getWellKnownAgentCard()} for the standard well-known endpoint.
     */
    @Deprecated
    public AgentCard getAgentCard() throws A2AClientError, A2AClientJSONError {
        return getConfiguredAgentCard();
    }

    private static URI resolveAgentCardUrl(URI baseUri, String agentCardPath) throws URISyntaxException {
        if (baseUri.getPath() != null && baseUri.getPath().endsWith(DEFAULT_AGENT_CARD_PATH)) {
            LOGGER.debug("Base URI already ends with agent card path, returning as-is: {}", baseUri);
            return baseUri;
        }

        String normalizedBasePath = normalizeBasePath(baseUri.getPath());
        String normalizedAgentCardPath = agentCardPath.startsWith("/") ? agentCardPath : "/" + agentCardPath;
        URI resolvedUri = new URI(
                baseUri.getScheme(),
                baseUri.getAuthority(),
                normalizedBasePath + normalizedAgentCardPath,
                baseUri.getQuery(),
                baseUri.getFragment());

        LOGGER.debug("Resolved agent card URL: baseUri={}, agentCardPath={}, result={}", baseUri, agentCardPath, resolvedUri);

        return resolvedUri;
    }

    private static String normalizeBasePath(@Nullable String path) {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return "";
        }
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    private AgentCard fetchAgentCard(String agentCardUrl) throws A2AClientError, A2AClientJSONError {
        LOGGER.debug("Fetching agent card from URL: {}", agentCardUrl);

        A2AHttpClient.GetBuilder builder = httpClient.createGet()
                .url(agentCardUrl)
                .addHeader("Content-Type", "application/json");

        if (authHeaders != null) {
            builder.addHeaders(authHeaders);
        }

        String body;
        try {
            A2AHttpResponse response = builder.get();
            if (!response.success()) {
                LOGGER.error("Failed to fetch agent card, status: {}", response.status());
                throw new A2AClientError("Failed to obtain agent card: " + response.status());
            }
            body = response.body();
            LOGGER.debug("Successfully fetched agent card from {}", agentCardUrl);
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
        } catch (A2AError | JsonProcessingException e) {
            throw new A2AClientJSONError("Could not unmarshal agent card response", e);
        }
    }
}
