package io.a2a.spec;

import io.a2a.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * OAuth 2.0 security scheme for agent authentication.
 * <p>
 * This security scheme uses OAuth 2.0 authorization flows to authenticate requests.
 * Supports authorization code, client credentials, implicit, and password flows
 * via the {@link OAuthFlows} configuration.
 * <p>
 * Corresponds to the OpenAPI "oauth2" security scheme type.
 *
 * @param flows the OAuth 2.0 flow configuration (required)
 * @param description optional description of the security scheme
 * @param oauth2MetadataUrl optional URL to OAuth 2.0 metadata (RFC 8414)
 * @see SecurityScheme for the base interface
 * @see OAuthFlows for flow configuration
 * @see <a href="https://spec.openapis.org/oas/v3.0.0#security-scheme-object">OpenAPI Security Scheme</a>
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record OAuth2SecurityScheme(OAuthFlows flows, @Nullable String description, @Nullable String oauth2MetadataUrl)
        implements SecurityScheme {

    /**
     * The type identifier for OAuth 2.0 security schemes: "oauth2SecurityScheme".
     */
    public static final String TYPE = "oauth2SecurityScheme";

    /**
     * Compact constructor with validation.
     *
     * @param flows the OAuth 2.0 flow configuration (required)
     * @param description optional description of the security scheme
     * @param oauth2MetadataUrl optional URL to OAuth 2.0 metadata (RFC 8414)
     * @throws IllegalArgumentException if flows is null
     */
    public OAuth2SecurityScheme {
        Assert.checkNotNullParam("flows", flows);
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
     * Builder for constructing immutable {@link OAuth2SecurityScheme} instances.
     * <p>
     * Example usage:
     * <pre>{@code
     * OAuth2SecurityScheme scheme = OAuth2SecurityScheme.builder()
     *     .flows(flows)
     *     .description("OAuth 2.0 authentication")
     *     .oauth2MetadataUrl("https://example.com/.well-known/oauth-authorization-server")
     *     .build();
     * }</pre>
     */
    public static class Builder {

        private @Nullable OAuthFlows flows;
        private @Nullable String description;
        private @Nullable String oauth2MetadataUrl;

        /**
         * Creates a new Builder with all fields unset.
         */
        private Builder() {
        }

        /**
         * Sets the OAuth flows configuration.
         *
         * @param flows the OAuth 2.0 flow configuration (required)
         * @return this builder for method chaining
         */
        public Builder flows(OAuthFlows flows) {
            this.flows = flows;
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
         * Sets the OAuth 2.0 metadata URL.
         *
         * @param oauth2MetadataUrl URL to OAuth 2.0 metadata document (RFC 8414) (optional)
         * @return this builder for method chaining
         */
        public Builder oauth2MetadataUrl(String oauth2MetadataUrl) {
            this.oauth2MetadataUrl = oauth2MetadataUrl;
            return this;
        }

        /**
         * Builds a new immutable {@link OAuth2SecurityScheme} from the current builder state.
         *
         * @return a new OAuth2SecurityScheme instance
         * @throws IllegalArgumentException if flows is null
         */
        public OAuth2SecurityScheme build() {
            return new OAuth2SecurityScheme(Assert.checkNotNullParam("flows", flows), description, oauth2MetadataUrl);
        }
    }
}
