package io.a2a.spec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.a2a.util.Assert;

/**
 * The AgentCard is a self-describing manifest for an agent in the A2A Protocol.
 * <p>
 * An AgentCard provides essential metadata about an agent, including its identity, capabilities,
 * supported skills, communication methods, and security requirements. It serves as the primary
 * discovery mechanism for clients to understand what an agent can do and how to interact with it.
 * <p>
 * The AgentCard corresponds to the {@code AgentCard} type in the A2A Protocol specification,
 * defining the contract between clients and agents for capability advertisement.
 * <p>
 * This class is immutable and uses the Builder pattern for construction to handle the mix of
 * required and optional fields defined by the specification.
 *
 * @param name the human-readable name of the agent (required)
 * @param description a brief description of the agent's purpose and functionality (required)
 * @param provider information about the organization or entity providing the agent (optional)
 * @param version the version of the agent implementation (required)
 * @param documentationUrl URL to human-readable documentation for the agent (optional)
 * @param capabilities the capabilities supported by this agent (required)
 * @param defaultInputModes list of supported input modes, e.g., "text", "audio" (required)
 * @param defaultOutputModes list of supported output modes, e.g., "text", "audio" (required)
 * @param skills list of skills that this agent can perform (required)
 * @param supportsAuthenticatedExtendedCard whether the agent supports authenticated extended card retrieval (optional, defaults to false)
 * @param securitySchemes map of security scheme names to their definitions (optional)
 * @param security list of security requirements for accessing the agent (optional)
 * @param iconUrl URL to an icon representing the agent (optional)
 * @param supportedInterfaces ordered list of supported interfaces. First entry is preferred. (required)
 * @param protocolVersion the version of the A2A Protocol this agent implements (defaults to {@link #DEFAULT_PROTOCOL_VERSION})
 * @param signatures digital signatures verifying the authenticity of the agent card (optional)
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentCard(
        String name,
        String description,
        AgentProvider provider,
        String version,
        String documentationUrl,
        AgentCapabilities capabilities,
        List<String> defaultInputModes,
        List<String> defaultOutputModes,
        List<AgentSkill> skills,
        boolean supportsAuthenticatedExtendedCard,
        Map<String, SecurityScheme> securitySchemes,
        List<Map<String, List<String>>> security,
        String iconUrl,
        List<AgentInterface> supportedInterfaces,
        String protocolVersion,
        List<AgentCardSignature> signatures) {

    /** The default A2A Protocol version used when not explicitly specified. */
    public static final String DEFAULT_PROTOCOL_VERSION = "1.0.0";

    /**
     * Compact constructor that validates required fields and sets defaults.
     *
     * @throws IllegalArgumentException if any required field is null
     */
    public AgentCard {
        Assert.checkNotNullParam("capabilities", capabilities);
        Assert.checkNotNullParam("defaultInputModes", defaultInputModes);
        Assert.checkNotNullParam("defaultOutputModes", defaultOutputModes);
        Assert.checkNotNullParam("description", description);
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("skills", skills);
        Assert.checkNotNullParam("supportedInterfaces", supportedInterfaces);
        Assert.checkNotNullParam("version", version);

        if (protocolVersion == null) {
            protocolVersion = DEFAULT_PROTOCOL_VERSION;
        }
    }

    /**
     * Returns the list of additional interfaces.
     *
     * @return the list of supported interfaces
     * @deprecated Use {@link #supportedInterfaces()} instead. This field has been renamed to 'supportedInterfaces'.
     */
    @Deprecated(since = "0.4.0", forRemoval = true)
    public List<AgentInterface> additionalInterfaces() {
        return supportedInterfaces;
    }

    /**
     * Builder for constructing immutable {@link AgentCard} instances.
     * <p>
     * The Builder pattern is used to enforce immutability of AgentCard objects while providing
     * a fluent API for setting required and optional fields. This approach ensures that once
     * an AgentCard is created, its state cannot be modified, which is important for thread-safety
     * and protocol correctness.
     * <p>
     * Example usage:
     * <pre>{@code
     * AgentCard card = new AgentCard.Builder()
     *     .name("Weather Agent")
     *     .description("Provides weather information")
     *     .supportedInterfaces(List.of(
     *         new AgentInterface("JSONRPC", "http://localhost:9999")))
     *     .version("1.0.0")
     *     .capabilities(new AgentCapabilities.Builder()
     *         .streaming(true)
     *         .build())
     *     .defaultInputModes(List.of("text"))
     *     .defaultOutputModes(List.of("text"))
     *     .skills(List.of(
     *         new AgentSkill.Builder()
     *             .id("weather_query")
     *             .name("Weather Queries")
     *             .build()
     *     ))
     *     .build();
     * }</pre>
     */
    public static class Builder {
        private String name;
        private String description;
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
        private List<AgentInterface> supportedInterfaces;
        private String protocolVersion;
        private List<AgentCardSignature> signatures;

        /**
         * Creates a new Builder with all fields unset.
         */
        public Builder() {

        }

        /**
         * Creates a new Builder initialized with values from an existing AgentCard.
         * <p>
         * This constructor creates defensive copies of mutable collections to ensure
         * that modifications to the builder do not affect the original AgentCard.
         *
         * @param card the AgentCard to copy values from
         */
        public Builder(AgentCard card) {
            this.name = card.name;
            this.description = card.description;
            this.provider = card.provider;
            this.version = card.version;
            this.documentationUrl = card.documentationUrl;
            this.capabilities = card.capabilities;
            this.defaultInputModes = card.defaultInputModes != null ? new ArrayList<>(card.defaultInputModes) : null;
            this.defaultOutputModes = card.defaultOutputModes != null ? new ArrayList<>(card.defaultOutputModes) : null;
            this.skills = card.skills != null ? new ArrayList<>(card.skills) : null;
            this.supportsAuthenticatedExtendedCard = card.supportsAuthenticatedExtendedCard;
            this.securitySchemes = card.securitySchemes != null ? Map.copyOf(card.securitySchemes) : null;
            this.security = card.security != null ? new ArrayList<>(card.security) : null;
            this.iconUrl = card.iconUrl;
            this.supportedInterfaces = card.supportedInterfaces != null ? new ArrayList<>(card.supportedInterfaces) : null;
            this.protocolVersion = card.protocolVersion;
            this.signatures = card.signatures != null ? new ArrayList<>(card.signatures) : null;
        }

        /**
         * Sets the human-readable name of the agent.
         *
         * @param name the agent name (required)
         * @return this builder for method chaining
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets a brief description of the agent's purpose and functionality.
         *
         * @param description the agent description (required)
         * @return this builder for method chaining
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }


        /**
         * Sets information about the organization or entity providing the agent.
         *
         * @param provider the agent provider (optional)
         * @return this builder for method chaining
         */
        public Builder provider(AgentProvider provider) {
            this.provider = provider;
            return this;
        }

        /**
         * Sets the version of the agent implementation.
         *
         * @param version the agent version (required)
         * @return this builder for method chaining
         */
        public Builder version(String version) {
            this.version = version;
            return this;
        }

        /**
         * Sets the URL to human-readable documentation for the agent.
         *
         * @param documentationUrl the documentation URL (optional)
         * @return this builder for method chaining
         */
        public Builder documentationUrl(String documentationUrl) {
            this.documentationUrl = documentationUrl;
            return this;
        }

        /**
         * Sets the capabilities supported by this agent.
         * <p>
         * Capabilities define optional features such as streaming responses,
         * push notifications, and state transition history.
         *
         * @param capabilities the agent capabilities (required)
         * @return this builder for method chaining
         * @see AgentCapabilities
         */
        public Builder capabilities(AgentCapabilities capabilities) {
            this.capabilities = capabilities;
            return this;
        }

        /**
         * Sets the list of supported input modes.
         * <p>
         * Input modes define the formats the agent can accept, such as "text", "audio", or "image".
         *
         * @param defaultInputModes the list of input modes (required, must not be empty)
         * @return this builder for method chaining
         */
        public Builder defaultInputModes(List<String> defaultInputModes) {
            this.defaultInputModes = defaultInputModes;
            return this;
        }

        /**
         * Sets the list of supported output modes.
         * <p>
         * Output modes define the formats the agent can produce, such as "text", "audio", or "image".
         *
         * @param defaultOutputModes the list of output modes (required, must not be empty)
         * @return this builder for method chaining
         */
        public Builder defaultOutputModes(List<String> defaultOutputModes) {
            this.defaultOutputModes = defaultOutputModes;
            return this;
        }

        /**
         * Sets the list of skills that this agent can perform.
         * <p>
         * Skills represent distinct capabilities or operations the agent can execute,
         * such as "weather_query" or "language_translation".
         *
         * @param skills the list of agent skills (required, must not be empty)
         * @return this builder for method chaining
         * @see AgentSkill
         */
        public Builder skills(List<AgentSkill> skills) {
            this.skills = skills;
            return this;
        }

        /**
         * Sets whether the agent supports authenticated extended card retrieval.
         * <p>
         * When true, the agent can provide additional information through the
         * {@code GetAuthenticatedExtendedCard} method, which may include private
         * or user-specific details not in the public card.
         *
         * @param supportsAuthenticatedExtendedCard true if supported, false otherwise
         * @return this builder for method chaining
         */
        public Builder supportsAuthenticatedExtendedCard(boolean supportsAuthenticatedExtendedCard) {
            this.supportsAuthenticatedExtendedCard = supportsAuthenticatedExtendedCard;
            return this;
        }

        /**
         * Sets the map of security scheme definitions.
         * <p>
         * Security schemes define authentication and authorization methods supported
         * by the agent, such as OAuth2, API keys, or HTTP authentication.
         *
         * @param securitySchemes map of scheme names to definitions (optional)
         * @return this builder for method chaining
         * @see SecurityScheme
         */
        public Builder securitySchemes(Map<String, SecurityScheme> securitySchemes) {
            this.securitySchemes = securitySchemes;
            return this;
        }

        /**
         * Sets the list of security requirements for accessing the agent.
         * <p>
         * Each entry in the list represents an alternative security requirement,
         * where each map contains scheme names and their required scopes.
         *
         * @param security the list of security requirements (optional)
         * @return this builder for method chaining
         */
        public Builder security(List<Map<String, List<String>>> security) {
            this.security = security;
            return this;
        }

        /**
         * Sets the URL to an icon representing the agent.
         *
         * @param iconUrl the icon URL (optional)
         * @return this builder for method chaining
         */
        public Builder iconUrl(String iconUrl) {
            this.iconUrl = iconUrl;
            return this;
        }

        /**
         * Sets the ordered list of supported interfaces (first entry is preferred).
         * <p>
         * Each interface defines a combination of protocol binding and URL for accessing the agent.
         *
         * @param supportedInterfaces the ordered list of supported interfaces (optional)
         * @return this builder for method chaining
         * @see AgentInterface
         */
        public Builder supportedInterfaces(List<AgentInterface> supportedInterfaces) {
            this.supportedInterfaces = supportedInterfaces;
            return this;
        }

        /**
         * Sets the list of additional transport interfaces.
         * <p>
         * Additional interfaces allow the agent to be accessed through multiple
         * transport protocols beyond the primary url/preferredTransport combination.
         *
         * @param additionalInterfaces the list of additional interfaces (optional)
         * @return this builder for method chaining
         * @see AgentInterface
         * @deprecated Use {@link #supportedInterfaces(List)} instead. This field has been renamed to 'supportedInterfaces'.
         */
        @Deprecated(since = "0.4.0", forRemoval = true)
        public Builder additionalInterfaces(List<AgentInterface> additionalInterfaces) {
            this.supportedInterfaces = additionalInterfaces;
            return this;
        }


        /**
         * Sets the version of the A2A Protocol this agent implements.
         * <p>
         * If not set, defaults to {@link AgentCard#DEFAULT_PROTOCOL_VERSION}.
         *
         * @param protocolVersion the protocol version string
         * @return this builder for method chaining
         */
        public Builder protocolVersion(String protocolVersion) {
            this.protocolVersion = protocolVersion;
            return this;
        }

        /**
         * Sets the digital signatures verifying the authenticity of the agent card.
         * <p>
         * Signatures provide cryptographic proof that the agent card was issued by
         * a trusted authority and has not been tampered with.
         *
         * @param signatures the list of signatures (optional)
         * @return this builder for method chaining
         * @see AgentCardSignature
         */
        public Builder signatures(List<AgentCardSignature> signatures) {
            this.signatures = signatures;
            return this;
        }

        /**
         * Builds an immutable {@link AgentCard} from the current builder state.
         * <p>
         * This method applies default values for optional fields.
         *
         * @return a new AgentCard instance
         * @throws IllegalArgumentException if any required field is null
         */
        public AgentCard build() {
            return new AgentCard(name, description, provider, version, documentationUrl,
                    capabilities, defaultInputModes, defaultOutputModes, skills,
                    supportsAuthenticatedExtendedCard, securitySchemes, security, iconUrl,
                    supportedInterfaces, protocolVersion, signatures);
        }
    }
}
