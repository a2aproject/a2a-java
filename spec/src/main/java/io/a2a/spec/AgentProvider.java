package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.a2a.util.Assert;

/**
 * Represents information about the organization or entity providing an agent.
 * <p>
 * AgentProvider contains metadata about who is responsible for operating and maintaining
 * an agent. This information helps users understand the source and trustworthiness of
 * the agent, and provides contact or documentation references.
 * <p>
 * Provider information is included in the {@link AgentCard} to identify the organization
 * behind the agent service.
 * <p>
 * This class is immutable.
 *
 * @param organization the name of the organization providing the agent (required)
 * @param url the URL to the provider's website or information page (required)
 * @see AgentCard
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentProvider(String organization, String url) {

    public AgentProvider {
        Assert.checkNotNullParam("organization", organization);
        Assert.checkNotNullParam("url", url);
    }
}
