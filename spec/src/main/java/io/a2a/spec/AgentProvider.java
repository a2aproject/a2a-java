package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.a2a.util.Assert;

/**
 * Represents information about the organization or entity that provides an agent.
 * This includes the organization name and a URL for more information about the provider.
 *
 * @param organization the name of the organization or entity providing the agent
 * @param url a URL pointing to more information about the provider (e.g., company website)
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentProvider(String organization, String url) {

    public AgentProvider {
        Assert.checkNotNullParam("organization", organization);
        Assert.checkNotNullParam("url", url);
    }
}
