package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Defines a security scheme using OpenID Connect.
 */
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

    public static class Builder {
        private String openIdConnectUrl;
        private String description;

        public Builder openIdConnectUrl(String openIdConnectUrl) {
            this.openIdConnectUrl = openIdConnectUrl;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public OpenIdConnectSecurityScheme build() {
            return new OpenIdConnectSecurityScheme(openIdConnectUrl, description);
        }
    }

}
