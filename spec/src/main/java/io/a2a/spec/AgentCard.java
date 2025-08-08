package io.a2a.spec;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.a2a.util.Assert;

/**
 * A public metadata file that describes an agent's capabilities, skills, endpoint URL, and
 * authentication requirements. Clients use this for discovery.
 * 
 * <p>An AgentCard conveys key information about an A2A Server:
 * <ul>
 * <li>Overall identity and descriptive details</li>
 * <li>Service endpoint URL</li>
 * <li>Supported A2A protocol capabilities (streaming, push notifications)</li>
 * <li>Authentication requirements</li>
 * <li>Default input/output content types (MIME types)</li>
 * <li>A list of specific skills the agent offers</li>
 * </ul>
 * 
 * @param name Human-readable name of the agent (e.g., "Recipe Advisor Agent")
 * @param description A human-readable description of the agent and its general purpose.
 *                   CommonMark MAY be used for rich text formatting.
 * @param url The base URL endpoint for the agent's A2A service (where JSON-RPC requests are sent).
 *           Must be an absolute HTTPS URL for production (e.g., {@code https://agent.example.com/a2a/api}).
 *           HTTP MAY be used for local development/testing only.
 * @param provider Information about the organization or entity providing the agent
 * @param version Version string for the agent or its A2A implementation
 *               (format is defined by the provider, e.g., "1.0.0", "2023-10-26-beta")
 * @param documentationUrl URL pointing to human-readable documentation for the agent
 *                        (e.g., API usage, detailed skill descriptions)
 * @param capabilities Specifies optional A2A protocol features supported by this agent
 * @param defaultInputModes Array of MIME types the agent generally accepts as input across all skills,
 *                         unless overridden by a specific skill
 * @param defaultOutputModes Array of MIME types the agent generally produces as output across all skills,
 *                          unless overridden by a specific skill
 * @param skills An array of specific skills or capabilities the agent offers.
 *              Must contain at least one skill if the agent is expected to perform actions beyond simple presence.
 * @param supportsAuthenticatedExtendedCard If {@code true}, the agent provides an authenticated endpoint
 *                                         ({@code /agent/authenticatedExtendedCard}) relative to the {@code url} field,
 *                                         from which a client can retrieve a potentially more detailed Agent Card
 *                                         after authenticating. Default: {@code false}.
 * @param securitySchemes Security scheme details used for authenticating with this agent.
 *                       {@code null} implies no A2A-advertised auth (not recommended for production)
 * @param security Security requirements for contacting the agent
 * @param iconUrl URL to an icon representing the agent
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentCard(String name, String description, String url, AgentProvider provider,
                        String version, String documentationUrl, AgentCapabilities capabilities,
                        List<String> defaultInputModes, List<String> defaultOutputModes, List<AgentSkill> skills,
                        boolean supportsAuthenticatedExtendedCard, Map<String, SecurityScheme> securitySchemes,
                        List<Map<String, List<String>>> security, String iconUrl, List<AgentInterface> additionalInterfaces,
                        String preferredTransport, String protocolVersion) {

    private static final String TEXT_MODE = "text";

    /**
     * Compact constructor for AgentCard that validates required parameters.
     * This constructor ensures that all essential fields are non-null to maintain
     * the integrity of the agent card data.
     */
    public AgentCard {
        Assert.checkNotNullParam("capabilities", capabilities);
        Assert.checkNotNullParam("defaultInputModes", defaultInputModes);
        Assert.checkNotNullParam("defaultOutputModes", defaultOutputModes);
        Assert.checkNotNullParam("description", description);
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("skills", skills);
        Assert.checkNotNullParam("url", url);
        Assert.checkNotNullParam("version", version);
        Assert.checkNotNullParam("protocolVersion", protocolVersion);
    }

    /**
     * Builder class for constructing {@link AgentCard} instances.
     */
    public static class Builder {
        private String name;
        private String description;
        private String url;
        private AgentProvider provider;
        private String version;
        private String documentationUrl;
        private AgentCapabilities capabilities;
        private List<String> defaultInputModes;
        private List<String> defaultOutputModes;
        private List<AgentSkill> skills;
        private boolean supportsAuthenticatedExtendedCard = false;
        private Map<String, SecurityScheme> securitySchemes;
        private List<Map<String, List<String>>> security;
        private String iconUrl;
        private List<AgentInterface> additionalInterfaces;
        String preferredTransport;
        String protocolVersion;

        /**
         * Sets the human-readable name of the agent.
         * 
         * @param name the agent name (e.g., "Recipe Advisor Agent")
         * @return this builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the human-readable description of the agent and its general purpose.
         * 
         * @param description the agent description (CommonMark MAY be used for rich text formatting)
         * @return this builder instance
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the base URL endpoint for the agent's A2A service.
         * 
         * @param url the service endpoint URL (must be absolute HTTPS URL for production)
         * @return this builder instance
         */
        public Builder url(String url) {
            this.url = url;
            return this;
        }

        /**
         * Sets information about the organization or entity providing the agent.
         * 
         * @param provider the agent provider information
         * @return this builder instance
         */
        public Builder provider(AgentProvider provider) {
            this.provider = provider;
            return this;
        }

        /**
         * Sets the version string for the agent or its A2A implementation.
         * 
         * @param version the version string (format is defined by the provider)
         * @return this builder instance
         */
        public Builder version(String version) {
            this.version = version;
            return this;
        }

        /**
         * Sets the URL pointing to human-readable documentation for the agent.
         * 
         * @param documentationUrl the documentation URL
         * @return this builder instance
         */
        public Builder documentationUrl(String documentationUrl) {
            this.documentationUrl = documentationUrl;
            return this;
        }

        /**
         * Sets the optional A2A protocol features supported by this agent.
         * 
         * @param capabilities the agent capabilities
         * @return this builder instance
         */
        public Builder capabilities(AgentCapabilities capabilities) {
            this.capabilities = capabilities;
            return this;
        }

        /**
         * Sets the MIME types the agent generally accepts as input across all skills.
         * 
         * @param defaultInputModes the list of input MIME types
         * @return this builder instance
         */
        public Builder defaultInputModes(List<String> defaultInputModes) {
            this.defaultInputModes = defaultInputModes;
            return this;
        }

        /**
         * Sets the MIME types the agent generally produces as output across all skills.
         * 
         * @param defaultOutputModes the list of output MIME types
         * @return this builder instance
         */
        public Builder defaultOutputModes(List<String> defaultOutputModes) {
            this.defaultOutputModes = defaultOutputModes;
            return this;
        }

        /**
         * Sets the specific skills or capabilities the agent offers.
         * 
         * @param skills the list of agent skills (must contain at least one skill)
         * @return this builder instance
         */
        public Builder skills(List<AgentSkill> skills) {
            this.skills = skills;
            return this;
        }

        /**
         * Sets whether the agent supports providing an extended agent card when authenticated.
         * 
         * @param supportsAuthenticatedExtendedCard {@code true} if the agent provides an authenticated
         *                                         extended card endpoint
         * @return this builder instance
         */
        public Builder supportsAuthenticatedExtendedCard(boolean supportsAuthenticatedExtendedCard) {
            this.supportsAuthenticatedExtendedCard = supportsAuthenticatedExtendedCard;
            return this;
        }

        /**
         * Sets the security scheme details used for authenticating with this agent.
         * 
         * @param securitySchemes the security schemes map
         * @return this builder instance
         */
        public Builder securitySchemes(Map<String, SecurityScheme> securitySchemes) {
            this.securitySchemes = securitySchemes;
            return this;
        }

        /**
         * Sets the security requirements for contacting the agent.
         * 
         * @param security the security requirements
         * @return this builder instance
         */
        public Builder security(List<Map<String, List<String>>> security) {
            this.security = security;
            return this;
        }

        /**
         * Sets the URL to an icon representing the agent.
         * 
         * @param iconUrl the icon URL
         * @return this builder instance
         */
        public Builder iconUrl(String iconUrl) {
            this.iconUrl = iconUrl;
            return this;
        }

        public Builder additionalInterfaces(List<AgentInterface> additionalInterfaces) {
            this.additionalInterfaces = additionalInterfaces;
            return this;
        }

        public Builder preferredTransport(String preferredTransport) {
            this.preferredTransport = preferredTransport;
            return this;
        }

        public Builder protocolVersion(String protocolVersion) {
            this.protocolVersion = protocolVersion;
            return this;
        }
      
        public AgentCard build() {
            return new AgentCard(name, description, url, provider, version, documentationUrl,
                    capabilities, defaultInputModes, defaultOutputModes, skills,
                    supportsAuthenticatedExtendedCard, securitySchemes, security, iconUrl,
                    additionalInterfaces, preferredTransport, protocolVersion);
        }
    }
}
