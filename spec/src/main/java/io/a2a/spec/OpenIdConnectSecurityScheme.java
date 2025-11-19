package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.a2a.util.Assert;

import static io.a2a.spec.OpenIdConnectSecurityScheme.OPENID_CONNECT;

/**
 * OpenID Connect security scheme for agent authentication.
 * <p>
 * This security scheme uses OpenID Connect Discovery to automatically configure
 * authentication. The {@code openIdConnectUrl} must point to an OpenID Connect
 * Discovery document that describes the provider's configuration, including
 * authorization and token endpoints.
 * <p>
 * OpenID Connect builds on OAuth 2.0 to provide identity layer functionality,
 * enabling clients to verify user identity and obtain basic profile information.
 * <p>
 * Example usage:
 * <pre>{@code
 * OpenIdConnectSecurityScheme scheme = new OpenIdConnectSecurityScheme.Builder()
 *     .openIdConnectUrl("https://example.com/.well-known/openid-configuration")
 *     .description("OpenID Connect authentication")
 *     .build();
 * }</pre>
 *
 * @see SecurityScheme for the base interface
 * @see <a href="https://spec.openapis.org/oas/v3.0.0#security-scheme-object">OpenAPI Security Scheme</a>
 * @see <a href="https://openid.net/specs/openid-connect-discovery-1_0.html">OpenID Connect Discovery</a>
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
@JsonTypeName(OPENID_CONNECT)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class OpenIdConnectSecurityScheme implements SecurityScheme {

    public static final String OPENID_CONNECT = "openIdConnect";
    private final String openIdConnectUrl;
    private final String description;
    private final String type;

    public OpenIdConnectSecurityScheme(String openIdConnectUrl, String description) {
        this(openIdConnectUrl, description, OPENID_CONNECT);
    }

    @JsonCreator
    public OpenIdConnectSecurityScheme(@JsonProperty("openIdConnectUrl") String openIdConnectUrl,
                                       @JsonProperty("description") String description, @JsonProperty("type") String type) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("openIdConnectUrl", openIdConnectUrl);
        if (!type.equals(OPENID_CONNECT)) {
            throw new IllegalArgumentException("Invalid type for OpenIdConnectSecurityScheme");
        }
        this.openIdConnectUrl = openIdConnectUrl;
        this.description = description;
        this.type = type;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public String getOpenIdConnectUrl() {
        return openIdConnectUrl;
    }

    public String getType() {
        return type;
    }

    /**
     * Builder for constructing {@link OpenIdConnectSecurityScheme} instances.
     * <p>
     * Provides a fluent API for creating OpenID Connect security schemes.
     * The {@code openIdConnectUrl} parameter is required.
     */
    public static class Builder {
        private String openIdConnectUrl;
        private String description;

        /**
         * Sets the OpenID Connect Discovery URL.
         *
         * @param openIdConnectUrl URL to the OpenID Connect Discovery document (required)
         * @return this builder instance
         */
        public Builder openIdConnectUrl(String openIdConnectUrl) {
            this.openIdConnectUrl = openIdConnectUrl;
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
         * Builds the {@link OpenIdConnectSecurityScheme} instance.
         *
         * @return a new immutable OpenIdConnectSecurityScheme
         * @throws IllegalArgumentException if openIdConnectUrl is null
         */
        public OpenIdConnectSecurityScheme build() {
            return new OpenIdConnectSecurityScheme(openIdConnectUrl, description);
        }
    }

}
