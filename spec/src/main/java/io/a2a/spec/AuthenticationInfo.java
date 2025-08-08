package io.a2a.spec;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.a2a.util.Assert;

/**
 * Represents authentication information for an A2A agent.
 * <p>
 * This record contains the authentication schemes supported by the agent
 * and any associated credentials. The authentication information is used
 * to establish secure communication between agents in the A2A protocol.
 * </p>
 *
 * @param schemes a list of supported authentication scheme names
 * @param credentials the authentication credentials (optional)
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthenticationInfo(List<String> schemes, String credentials) {

    /**
     * Compact constructor that validates the authentication information.
     * <p>
     * This constructor ensures that the schemes list is not null, as at least
     * one authentication scheme must be specified.
     * </p>
     *
     * @throws IllegalArgumentException if schemes is null
     */
    public AuthenticationInfo {
        Assert.checkNotNullParam("schemes", schemes);
    }
}
