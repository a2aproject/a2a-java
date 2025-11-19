package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.a2a.util.Assert;

import static io.a2a.spec.HTTPAuthSecurityScheme.HTTP;

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
 * HTTPAuthSecurityScheme scheme = new HTTPAuthSecurityScheme.Builder()
 *     .scheme("bearer")
 *     .bearerFormat("JWT")
 *     .description("JWT bearer token authentication")
 *     .build();
 * }</pre>
 *
 * @see SecurityScheme for the base interface
 * @see <a href="https://spec.openapis.org/oas/v3.0.0#security-scheme-object">OpenAPI Security Scheme</a>
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7235">RFC 7235 - HTTP Authentication</a>
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
@JsonTypeName(HTTP)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class HTTPAuthSecurityScheme implements SecurityScheme {

    public static final String HTTP = "http";
    private final String bearerFormat;
    private final String scheme;
    private final String description;
    private final String type;

    public HTTPAuthSecurityScheme(String bearerFormat, String scheme, String description) {
        this(bearerFormat, scheme, description, HTTP);
    }

    @JsonCreator
    public HTTPAuthSecurityScheme(@JsonProperty("bearerFormat") String bearerFormat, @JsonProperty("scheme") String scheme,
                                  @JsonProperty("description") String description, @JsonProperty("type") String type) {
        Assert.checkNotNullParam("scheme", scheme);
        Assert.checkNotNullParam("type", type);
        if (! type.equals(HTTP)) {
            throw new IllegalArgumentException("Invalid type for HTTPAuthSecurityScheme");
        }
        this.bearerFormat = bearerFormat;
        this.scheme = scheme;
        this.description = description;
        this.type = type;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public String getBearerFormat() {
        return bearerFormat;
    }

    public String getScheme() {
        return scheme;
    }

    public String getType() {
        return type;
    }

    /**
     * Builder for constructing {@link HTTPAuthSecurityScheme} instances.
     * <p>
     * Provides a fluent API for creating HTTP authentication security schemes.
     * The {@code scheme} parameter is required and must be set before calling {@code build()}.
     */
    public static class Builder {
        private String bearerFormat;
        private String scheme;
        private String description;

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
            return new HTTPAuthSecurityScheme(bearerFormat, scheme, description);
        }
    }
}
