package io.a2a.spec;

import io.a2a.util.Assert;
import org.jspecify.annotations.Nullable;

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
 * OpenIdConnectSecurityScheme scheme = OpenIdConnectSecurityScheme.builder()
 *     .openIdConnectUrl("https://example.com/.well-known/openid-configuration")
 *     .description("OpenID Connect authentication")
 *     .build();
 * }</pre>
 *
 * @param openIdConnectUrl URL to the OpenID Connect Discovery document (required)
 * @param description optional description of the security scheme
 * @see SecurityScheme for the base interface
 * @see <a href="https://spec.openapis.org/oas/v3.0.0#security-scheme-object">OpenAPI Security Scheme</a>
 * @see <a href="https://openid.net/specs/openid-connect-discovery-1_0.html">OpenID Connect Discovery</a>
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record OpenIdConnectSecurityScheme(String openIdConnectUrl, @Nullable String description) implements SecurityScheme {

    /**
     * The type identifier for OpenID Connect security schemes: "openIdConnect".
     */
    public static final String TYPE = "openIdConnectSecurityScheme";

    /**
     * Compact constructor with validation.
     *
     * @param openIdConnectUrl URL to the OpenID Connect Discovery document (required)
     * @param description optional description of the security scheme
     * @throws IllegalArgumentException if openIdConnectUrl is null
     */
    public OpenIdConnectSecurityScheme {
        Assert.checkNotNullParam("openIdConnectUrl", openIdConnectUrl);
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
     * Builder for constructing immutable {@link OpenIdConnectSecurityScheme} instances.
     * <p>
     * Provides a fluent API for creating OpenID Connect security schemes.
     * The {@code openIdConnectUrl} parameter is required.
     */
    public static class Builder {
        private @Nullable String openIdConnectUrl;
        private @Nullable String description;

        /**
         * Creates a new Builder with all fields unset.
         */
        private Builder() {
        }

        /**
         * Sets the OpenID Connect Discovery URL.
         *
         * @param openIdConnectUrl URL to the OpenID Connect Discovery document (required)
         * @return this builder for method chaining
         */
        public Builder openIdConnectUrl(String openIdConnectUrl) {
            this.openIdConnectUrl = openIdConnectUrl;
            return this;
        }

        /**
         * Sets the human-readable description of the security scheme.
         *
         * @param description the description (optional)
         * @return this builder for method chaining
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Builds a new immutable {@link OpenIdConnectSecurityScheme} from the current builder state.
         *
         * @return a new OpenIdConnectSecurityScheme instance
         * @throws IllegalArgumentException if openIdConnectUrl is null
         */
        public OpenIdConnectSecurityScheme build() {
            return new OpenIdConnectSecurityScheme(Assert.checkNotNullParam("openIdConnectUrl", openIdConnectUrl), description);
        }
    }
}
