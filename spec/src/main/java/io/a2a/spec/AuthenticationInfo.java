package io.a2a.spec;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.a2a.util.Assert;

/**
 * Authentication information for accessing an agent's authenticated endpoints.
 * <p>
 * This record encapsulates the authentication schemes supported by an agent and
 * optionally provides credentials for authentication. It is used when clients need
 * to authenticate to access protected agent resources.
 * <p>
 * The {@code schemes} list identifies which security schemes from the agent's
 * {@link AgentCard#securitySchemes()} should be used for authentication.
 *
 * @param schemes list of security scheme names that should be used for authentication (required)
 * @param credentials optional credentials string for authentication (format depends on scheme)
 * @see AgentCard#securitySchemes() for available security schemes
 * @see SecurityScheme for security scheme definitions
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthenticationInfo(List<String> schemes, String credentials) {

    public AuthenticationInfo {
        Assert.checkNotNullParam("schemes", schemes);
    }
}
