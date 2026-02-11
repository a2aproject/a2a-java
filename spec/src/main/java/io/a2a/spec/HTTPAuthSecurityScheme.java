package io.a2a.spec;

import io.a2a.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * HTTP authentication security scheme for agent authentication.
 * <p>
 * This security scheme uses HTTP authentication mechanisms, supporting both basic authentication
 * and bearer token authentication (e.g., JWT). The {@code scheme} parameter specifies the
 * HTTP authentication scheme name as defined in RFC 7235.
 * <p>
 * Common schemes:
 * <ul>
 *   <li>{@code basic} - HTTP Basic authentication (RFC 7617)</li>
 *   <li>{@code bearer} - Bearer token authentication (RFC 6750), typically used with OAuth 2.0</li>
 * </ul>
 * <p>
 * For bearer tokens, the {@code bearerFormat} field can provide additional information about
 * the token format (e.g., "JWT").
 * <p>
 * Example usage:
 * <pre>{@code
 * HTTPAuthSecurityScheme scheme = HTTPAuthSecurityScheme.builder()
 *     .scheme("bearer")
 *     .bearerFormat("JWT")
 *     .description("JWT bearer token authentication")
 *     .build();
 * }</pre>
 *
 * @param bearerFormat the bearer token format (optional)
 * @param scheme the authentication scheme (required)
 * @param description the scheme description (optional)
 * @see SecurityScheme for the base interface
 * @see <a href="https://spec.openapis.org/oas/v3.0.0#security-scheme-object">OpenAPI Security Scheme</a>
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7235">RFC 7235 - HTTP Authentication</a>
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record HTTPAuthSecurityScheme(@Nullable String bearerFormat, String scheme, @Nullable String description) implements SecurityScheme {

    /** The HTTP security scheme type identifier. */
    public static final String TYPE = "httpAuthSecurityScheme";

    /**
     * Compact constructor with validation.
     *
     * @param bearerFormat the bearer token format (optional)
     * @param scheme the authentication scheme (required)
     * @param description the scheme description (optional)
     * @throws IllegalArgumentException if scheme is null
     */
    public HTTPAuthSecurityScheme {
        Assert.checkNotNullParam("scheme", scheme);
    }

    @Override
    public String type() {
        return TYPE;
    }

    /**
     * Create a new Builder
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link HTTPAuthSecurityScheme} instances.
     * <p>
     * Provides a fluent API for creating HTTP authentication security schemes.
     * The {@code scheme} parameter is required and must be set before calling {@code build()}.
     */
    public static class Builder {
        private @Nullable String bearerFormat;
        private @Nullable String scheme;
        private @Nullable String description;

        /**
         * Creates a new Builder with all fields unset.
         */
        private Builder() {
        }

        /**
         * Sets the bearer token format hint.
         *
         * @param bearerFormat hint to the client about the format of bearer tokens (e.g., "JWT")
         * @return this builder instance
         */
        public Builder bearerFormat(String bearerFormat) {
            this.bearerFormat = bearerFormat;
            return this;
        }

        /**
         * Sets the HTTP authentication scheme name.
         *
         * @param scheme the scheme name (required, e.g., "basic" or "bearer")
         * @return this builder instance
         */
        public Builder scheme(String scheme) {
            this.scheme = scheme;
            return this;
        }

        /**
         * Sets an optional description of the security scheme.
         *
         * @param description human-readable description
         * @return this builder instance
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Builds the {@link HTTPAuthSecurityScheme} instance.
         *
         * @return a new immutable HTTPAuthSecurityScheme
         * @throws IllegalArgumentException if required fields are missing
         */
        public HTTPAuthSecurityScheme build() {
            return new HTTPAuthSecurityScheme(bearerFormat, Assert.checkNotNullParam("scheme", scheme), description);
        }
    }
}
