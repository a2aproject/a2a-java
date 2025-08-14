package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Defines the configuration for the supported OAuth 2.0 flows.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record OAuthFlows(AuthorizationCodeOAuthFlow authorizationCode, ClientCredentialsOAuthFlow clientCredentials,
                         ImplicitOAuthFlow implicit, PasswordOAuthFlow password) {

    public static class Builder {
        private AuthorizationCodeOAuthFlow authorizationCode;
        private ClientCredentialsOAuthFlow clientCredentials;
        private ImplicitOAuthFlow implicit;
        private PasswordOAuthFlow password;

        public Builder authorizationCode(AuthorizationCodeOAuthFlow authorizationCode) {
            this.authorizationCode = authorizationCode;
            return this;
        }

        public Builder clientCredentials(ClientCredentialsOAuthFlow clientCredentials) {
            this.clientCredentials = clientCredentials;
            return this;
        }

        public Builder implicit(ImplicitOAuthFlow implicit) {
            this.implicit = implicit;
            return this;
        }

        public Builder password(PasswordOAuthFlow password) {
            this.password = password;
            return this;
        }

        public OAuthFlows build() {
            return new OAuthFlows(authorizationCode, clientCredentials, implicit, password);
        }
    }
}
