package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.a2a.util.Assert;

/**
 * Represents an OAuth 2.0 security scheme configuration for the A2A protocol.
 * This class defines the OAuth 2.0 authentication requirements and supported flows
 * for accessing an A2A agent's endpoints.
 *
 * <p>OAuth 2.0 is a widely-used authorization framework that enables applications
 * to obtain limited access to user accounts. In the A2A context, it allows agents
 * to securely authenticate and authorize access to their services.</p>
 *
 * <p>The security scheme includes:</p>
 * <ul>
 *   <li>Supported OAuth flows (authorization code, client credentials, implicit, password)</li>
 *   <li>Optional description of the authentication requirements</li>
 *   <li>Type identifier (always "oauth2" for OAuth 2.0 schemes)</li>
 * </ul>
 *
 * <p>This implementation follows the OpenAPI Security Scheme Object specification
 * for OAuth 2.0 security schemes, ensuring compatibility with standard OAuth 2.0
 * implementations and tooling.</p>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class OAuth2SecurityScheme implements SecurityScheme {

    public static final String OAUTH2 = "oauth2";
    
    /**
     * The OAuth flows supported by this security scheme.
     * Defines the available OAuth 2.0 grant types and their configurations.
     */
    private final OAuthFlows flows;
    
    /**
     * Optional description of the OAuth 2.0 security scheme.
     * Provides human-readable information about the authentication requirements.
     */
    private final String description;
    
    /**
     * The type of security scheme, always "oauth2" for OAuth 2.0.
     */
    private final String type;

    /**
     * Constructs an OAuth 2.0 security scheme with flows and description.
     *
     * @param flows the OAuth flows configuration (must not be null)
     * @param description optional description of the security scheme (can be null)
     */
    public OAuth2SecurityScheme(OAuthFlows flows, String description) {
        this(flows, description, OAUTH2);
    }

    /**
     * Constructs an OAuth 2.0 security scheme with all parameters.
     * This constructor is used for JSON deserialization.
     *
     * @param flows the OAuth flows configuration (must not be null)
     * @param description optional description of the security scheme (can be null)
     * @param type the security scheme type (must be "oauth2")
     * @throws IllegalArgumentException if flows is null or type is not "oauth2"
     */
    @JsonCreator
    public OAuth2SecurityScheme(@JsonProperty("flows") OAuthFlows flows, @JsonProperty("description") String description,
                                @JsonProperty("type") String type) {
        Assert.checkNotNullParam("flows", flows);
        if (!type.equals(OAUTH2)) {
            throw new IllegalArgumentException("Invalid type for OAuth2SecurityScheme");
        }
        this.flows = flows;
        this.description = description;
        this.type = type;
    }

    /**
     * Returns the optional description of this security scheme.
     *
     * @return the description, or null if not provided
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Returns the OAuth flows configuration for this security scheme.
     *
     * @return the OAuth flows configuration
     */
    public OAuthFlows getFlows() {
        return flows;
    }

    /**
     * Returns the type of this security scheme.
     *
     * @return always "oauth2" for OAuth 2.0 security schemes
     */
    public String getType() {
        return type;
    }

    /**
     * Builder class for constructing OAuth2SecurityScheme instances.
     * Provides a fluent API for setting optional properties.
     */
    public static class Builder {
        /**
         * The OAuth flows configuration for the security scheme.
         */
        private OAuthFlows flows;
        
        /**
         * Optional description of the security scheme.
         */
        private String description;

        /**
         * Sets the OAuth flows configuration.
         *
         * @param flows the OAuth flows configuration
         * @return this builder instance for method chaining
         */
        public Builder flows(OAuthFlows flows) {
            this.flows = flows;
            return this;
        }

        /**
         * Sets the optional description for the security scheme.
         *
         * @param description the description of the security scheme
         * @return this builder instance for method chaining
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Builds and returns a new OAuth2SecurityScheme instance.
         *
         * @return a new OAuth2SecurityScheme with the configured properties
         * @throws IllegalArgumentException if flows is null
         */
        public OAuth2SecurityScheme build() {
            return new OAuth2SecurityScheme(flows, description);
        }
    }
}
