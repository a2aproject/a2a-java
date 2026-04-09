package org.a2aproject.sdk.server;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.jspecify.annotations.Nullable;

import org.a2aproject.sdk.jsonrpc.common.json.JsonProcessingException;
import org.a2aproject.sdk.jsonrpc.common.json.JsonUtil;
import org.a2aproject.sdk.server.config.A2AConfigProvider;
import org.a2aproject.sdk.spec.AgentCard;

/**
 * Provides HTTP caching metadata for Agent Card responses.
 *
 * <p>This bean computes and caches HTTP caching headers (Cache-Control, ETag, Last-Modified)
 * for the Agent Card endpoint as specified in the A2A protocol specification section 8.6.
 *
 * <p>The metadata is computed once at initialization:
 * <ul>
 *   <li><b>Cache-Control:</b> Configured via {@code a2a.agent-card.cache.max-age} (default: 3600 seconds)</li>
 *   <li><b>ETag:</b> MD5 hash of the serialized Agent Card JSON</li>
 *   <li><b>Last-Modified:</b> Timestamp when the bean was initialized (RFC 1123 format)</li>
 * </ul>
 *
 * <p>Since the Agent Card is {@code @ApplicationScoped}, these values remain stable
 * throughout the application lifecycle unless the application is restarted.
 *
 * @see <a href="https://github.com/a2aproject/A2A/blob/main/docs/specification.md#86-caching">A2A Specification - Agent Card Caching</a>
 */
@ApplicationScoped
public class AgentCardCacheMetadata {

    private static final String CONFIG_KEY_MAX_AGE = "a2a.agent-card.cache.max-age";
    private static final String DEFAULT_MAX_AGE = "3600"; // 1 hour
    private static final DateTimeFormatter RFC_1123_FORMATTER = DateTimeFormatter.RFC_1123_DATE_TIME;

    @Inject
    @PublicAgentCard
    Instance<AgentCard> agentCardInstance;

    @Inject
    Instance<A2AConfigProvider> configInstance;

    private @Nullable AgentCard agentCard;
    private @Nullable A2AConfigProvider config;

    @SuppressWarnings("NullAway") // Initialized in @PostConstruct when agentCard is available
    private String etag;
    @SuppressWarnings("NullAway") // Initialized in @PostConstruct when agentCard is available
    private String lastModified;
    @SuppressWarnings("NullAway") // Initialized in @PostConstruct when agentCard is available
    private String cacheControl;

    /**
     * Package-private no-arg constructor for CDI.
     */
    AgentCardCacheMetadata() {
        // For CDI
    }

    /**
     * Public constructor for testing purposes.
     *
     * @param agentCard the agent card
     * @param config the configuration provider
     */
    public AgentCardCacheMetadata(AgentCard agentCard, A2AConfigProvider config) {
        this.agentCard = agentCard;
        this.config = config;
        init();
    }

    @PostConstruct
    @SuppressWarnings("NullAway") // agentCard and config are guaranteed non-null in both paths
    void init() {
        // Handle two initialization paths:
        // 1. CDI injection: get beans from Instance if available
        // 2. Direct constructor: agentCard and config already set

        if (agentCard == null && agentCardInstance != null) {
            // CDI path - only initialize if AgentCard bean is available
            if (agentCardInstance.isUnsatisfied() || configInstance.isUnsatisfied()) {
                return;
            }
            this.agentCard = agentCardInstance.get();
            this.config = configInstance.get();
        }

        // At this point, agentCard and config should be set (either via CDI or constructor)
        if (agentCard == null || config == null) {
            return;
        }

        // Calculate ETag from the serialized JSON representation
        this.etag = calculateETag(agentCard);

        // Set Last-Modified to the initialization time
        this.lastModified = RFC_1123_FORMATTER.format(Instant.now().atZone(ZoneOffset.UTC));

        // Configure Cache-Control with max-age directive
        String maxAge = config.getOptionalValue(CONFIG_KEY_MAX_AGE).orElse(DEFAULT_MAX_AGE);
        this.cacheControl = "public, max-age=" + maxAge;
    }

    /**
     * Returns the ETag header value for the Agent Card.
     *
     * <p>The ETag is an MD5 hash of the serialized Agent Card JSON, quoted per HTTP specification.
     *
     * @return the ETag header value (e.g., {@code "a1b2c3d4..."})
     */
    public String getETag() {
        return etag;
    }

    /**
     * Returns the Last-Modified header value for the Agent Card.
     *
     * <p>The timestamp represents when the bean was initialized, in RFC 1123 format.
     *
     * @return the Last-Modified header value (e.g., {@code "Mon, 17 Mar 2025 10:00:00 GMT"})
     */
    public String getLastModified() {
        return lastModified;
    }

    /**
     * Returns the Cache-Control header value for the Agent Card.
     *
     * <p>The value includes {@code public} and a {@code max-age} directive configured
     * via {@code a2a.agent-card.cache.max-age} (default: 3600 seconds).
     *
     * @return the Cache-Control header value (e.g., {@code "public, max-age=3600"})
     */
    public String getCacheControl() {
        return cacheControl;
    }

    /**
     * Calculates an MD5 hash of the Agent Card JSON for use as an ETag.
     *
     * @param card the agent card to hash
     * @return the hex-encoded MD5 hash, quoted per HTTP specification
     */
    private String calculateETag(AgentCard card) {
        try {
            String json = JsonUtil.toJson(card);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(json.getBytes(StandardCharsets.UTF_8));
            return "\"" + HexFormat.of().formatHex(hash) + "\"";
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available", e);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize Agent Card for ETag calculation", e);
        }
    }

    /**
     * Populates a map with header names and header values stored in this instance.
     *
     * @return a map of the headers
     */
    public Map<String, String> getHttpHeadersMap() {
        Map<String, String> headers = new HashMap<>();
        if (cacheControl != null) {
            headers.put("Cache-Control", cacheControl);
        }
        if (lastModified != null) {
            headers.put("Last-Modified", lastModified);
        }
        if (etag != null) {
            headers.put("ETag", etag);
        }
        return headers;
    }
}
