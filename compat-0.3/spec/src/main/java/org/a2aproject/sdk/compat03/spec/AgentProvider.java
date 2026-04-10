package org.a2aproject.sdk.compat03.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.a2aproject.sdk.util.Assert;

/**
 * Represents the service provider of an agent.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentProvider(String organization, String url) {

    public AgentProvider {
        Assert.checkNotNullParam("organization", organization);
        Assert.checkNotNullParam("url", url);
    }
}
