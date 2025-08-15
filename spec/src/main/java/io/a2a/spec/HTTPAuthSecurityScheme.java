package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.a2a.util.Assert;

/**
 * Represents an HTTP authentication security scheme as defined in the A2A specification.
 * This corresponds to the HTTPAuthSecurityScheme interface in the TypeScript definitions.
 * 
 * HTTP Authentication security schemes define how clients should authenticate using
 * standard HTTP authentication methods as specified in RFC7235. The scheme name
 * should be registered in the IANA Authentication Scheme registry.
 * 
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7235">RFC7235 - HTTP Authentication</a>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class HTTPAuthSecurityScheme implements SecurityScheme {

    /** The type identifier for HTTP authentication schemes */
    public static final String HTTP = "http";
    
    /** A hint to identify how the bearer token is formatted (optional) */
    private final String bearerFormat;
    
    /** The name of the HTTP Authentication scheme (e.g., "Basic", "Bearer") */
    private final String scheme;
    
    /** Description of this security scheme (optional) */
    private final String description;
    
    /** The type of security scheme, must be "http" */
    private final String type;

    /**
     * Convenience constructor that defaults the type to "http".
     * 
     * @param bearerFormat A hint to identify how the bearer token is formatted (optional)
     * @param scheme The name of the HTTP Authentication scheme (required)
     * @param description Description of this security scheme (optional)
     */
    public HTTPAuthSecurityScheme(String bearerFormat, String scheme, String description) {
        this(bearerFormat, scheme, description, HTTP);
    }

    /**
     * Full constructor for HTTP authentication security scheme.
     * Used by Jackson for JSON deserialization.
     * 
     * @param bearerFormat A hint to identify how the bearer token is formatted (optional)
     * @param scheme The name of the HTTP Authentication scheme (required, case-insensitive)
     * @param description Description of this security scheme (optional)
     * @param type The type of security scheme, must be "http"
     * @throws IllegalArgumentException if type is not "http"
     */
    @JsonCreator
    public HTTPAuthSecurityScheme(@JsonProperty("bearerFormat") String bearerFormat, @JsonProperty("scheme") String scheme,
                                  @JsonProperty("description") String description, @JsonProperty("type") String type) {
        Assert.checkNotNullParam("scheme", scheme);
        if (! type.equals(HTTP)) {
            throw new IllegalArgumentException("Invalid type for HTTPAuthSecurityScheme");
        }
        this.bearerFormat = bearerFormat;
        this.scheme = scheme;
        this.description = description;
        this.type = type;
    }

    /**
     * Returns the description of this security scheme.
     * 
     * @return the description, or null if not specified
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Returns the bearer format hint for this security scheme.
     * This is primarily for documentation purposes to help identify
     * how bearer tokens are formatted.
     * 
     * @return the bearer format hint, or null if not specified
     */
    public String getBearerFormat() {
        return bearerFormat;
    }

    /**
     * Returns the HTTP authentication scheme name.
     * This should be a registered scheme in the IANA Authentication Scheme registry.
     * Common values include "Basic", "Bearer", "Digest", etc.
     * 
     * @return the scheme name (case-insensitive)
     */
    public String getScheme() {
        return scheme;
    }

    /**
     * Returns the type of this security scheme.
     * For HTTPAuthSecurityScheme, this is always "http".
     * 
     * @return "http"
     */
    public String getType() {
        return type;
    }

    /**
     * Builder class for constructing HTTPAuthSecurityScheme instances.
     * Provides a fluent API for setting optional parameters.
     */
    public static class Builder {
        private String bearerFormat;
        private String scheme;
        private String description;

        /**
         * Sets the bearer format hint.
         * 
         * @param bearerFormat A hint to identify how bearer tokens are formatted
         * @return this builder instance for method chaining
         */
        public Builder bearerFormat(String bearerFormat) {
            this.bearerFormat = bearerFormat;
            return this;
        }

        /**
         * Sets the HTTP authentication scheme name.
         * 
         * @param scheme The name of the HTTP Authentication scheme (required)
         * @return this builder instance for method chaining
         */
        public Builder scheme(String scheme) {
            this.scheme = scheme;
            return this;
        }

        /**
         * Sets the description of this security scheme.
         * 
         * @param description Description of this security scheme
         * @return this builder instance for method chaining
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Builds the HTTPAuthSecurityScheme instance.
         * 
         * @return a new HTTPAuthSecurityScheme instance
         * @throws IllegalArgumentException if required parameters are missing
         */
        public HTTPAuthSecurityScheme build() {
            return new HTTPAuthSecurityScheme(bearerFormat, scheme, description);
        }
    }
}
