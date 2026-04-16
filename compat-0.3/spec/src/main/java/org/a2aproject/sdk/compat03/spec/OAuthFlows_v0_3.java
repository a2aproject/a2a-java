package org.a2aproject.sdk.compat03.spec;

/**
 * Defines the configuration for the supported OAuth 2.0 flows.
 */
public record OAuthFlows_v0_3(AuthorizationCodeOAuthFlow_v0_3 authorizationCode, ClientCredentialsOAuthFlow_v0_3 clientCredentials,
                              ImplicitOAuthFlow_v0_3 implicit, PasswordOAuthFlow_v0_3 password) {

    public static class Builder {
        private AuthorizationCodeOAuthFlow_v0_3 authorizationCode;
        private ClientCredentialsOAuthFlow_v0_3 clientCredentials;
        private ImplicitOAuthFlow_v0_3 implicit;
        private PasswordOAuthFlow_v0_3 password;

        public Builder authorizationCode(AuthorizationCodeOAuthFlow_v0_3 authorizationCode) {
            this.authorizationCode = authorizationCode;
            return this;
        }

        public Builder clientCredentials(ClientCredentialsOAuthFlow_v0_3 clientCredentials) {
            this.clientCredentials = clientCredentials;
            return this;
        }

        public Builder implicit(ImplicitOAuthFlow_v0_3 implicit) {
            this.implicit = implicit;
            return this;
        }

        public Builder password(PasswordOAuthFlow_v0_3 password) {
            this.password = password;
            return this;
        }

        public OAuthFlows_v0_3 build() {
            return new OAuthFlows_v0_3(authorizationCode, clientCredentials, implicit, password);
        }
    }
}
