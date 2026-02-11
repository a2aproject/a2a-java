package io.a2a.spec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.a2a.util.Assert;
import java.util.Collections;
import org.jspecify.annotations.Nullable;

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
 * <p>
 * <b>Important:</b> The {@link #supportedInterfaces()} field specifies how clients can
 * communicate with the agent. Each entry combines a protocol binding (e.g., "JSONRPC",
 * "GRPC") with a URL endpoint. The first entry in the list is the preferred interface.
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
 * @param securitySchemes map of security scheme names to their definitions (optional)
 * @param securityRequirements list of security requirements for accessing the agent (optional)
 * @param iconUrl URL to an icon representing the agent (optional)
 * @param supportedInterfaces ordered list of protocol+URL interface combinations; first entry is preferred (required)
 * @param signatures digital signatures verifying the authenticity of the agent card (optional)
 * @see AgentInterface
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record AgentCard(
        String name,
        String description,
        @Nullable AgentProvider provider,
        String version,
        @Nullable String documentationUrl,
        AgentCapabilities capabilities,
        List<String> defaultInputModes,
        List<String> defaultOutputModes,
        List<AgentSkill> skills,
        @Nullable Map<String, SecurityScheme> securitySchemes,
        @Nullable List<Map<String, List<String>>> securityRequirements,
        @Nullable String iconUrl,
        List<AgentInterface> supportedInterfaces,
        @Nullable List<AgentCardSignature> signatures) {

    /**
     * Compact constructor that validates required fields.
     *
     * @param name the name parameter (see class-level JavaDoc)
     * @param description the description parameter (see class-level JavaDoc)
     * @param provider the provider parameter (see class-level JavaDoc)
     * @param version the version parameter (see class-level JavaDoc)
     * @param documentationUrl the documentationUrl parameter (see class-level JavaDoc)
     * @param capabilities the capabilities parameter (see class-level JavaDoc)
     * @param defaultInputModes the defaultInputModes parameter (see class-level JavaDoc)
     * @param defaultOutputModes the defaultOutputModes parameter (see class-level JavaDoc)
     * @param skills the skills parameter (see class-level JavaDoc)
     * @param securitySchemes the securitySchemes parameter (see class-level JavaDoc)
     * @param securityRequirements the security parameter (see class-level JavaDoc)
     * @param iconUrl the iconUrl parameter (see class-level JavaDoc)
     * @param supportedInterfaces the supportedInterfaces parameter (see class-level JavaDoc)
     * @param signatures the signatures parameter (see class-level JavaDoc)
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
    }

    /**
     * Create a new Builder
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a new Builder initialized with values from an existing AgentCard.
     * <p>
     * This builder creates defensive copies of mutable collections to ensure
     * that modifications to the builder do not affect the original AgentCard.
     *
     * @param card the AgentCard to copy values from
     * @return the builder
     */
    public static Builder builder(AgentCard card) {
        return new Builder(card);
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
     * AgentCard card = AgentCard.builder()
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

        private @Nullable String name;
        private @Nullable String description;
        private @Nullable AgentProvider provider;
        private @Nullable String version;
        private @Nullable String documentationUrl;
        private @Nullable AgentCapabilities capabilities;
        private @Nullable List<String> defaultInputModes;
        private @Nullable List<String> defaultOutputModes;
        private @Nullable List<AgentSkill> skills;
        private @Nullable Map<String, SecurityScheme> securitySchemes;
        private @Nullable List<Map<String, List<String>>> securityRequirements;
        private @Nullable String iconUrl;
        private @Nullable List<AgentInterface> supportedInterfaces;
        private @Nullable List<AgentCardSignature> signatures;

        /**
         * Creates a new Builder with all fields unset.
         */
        private Builder() {
        }

        /**
         * Creates a new Builder initialized with values from an existing AgentCard.
         * <p>
         * This constructor creates defensive copies of mutable collections to ensure
         * that modifications to the builder do not affect the original AgentCard.
         *
         * @param card the AgentCard to copy values from
         */
        private Builder(AgentCard card) {
            this.name = card.name;
            this.description = card.description;
            this.provider = card.provider;
            this.version = card.version;
            this.documentationUrl = card.documentationUrl;
            this.capabilities = card.capabilities;
            this.defaultInputModes = card.defaultInputModes != null ? new ArrayList<>(card.defaultInputModes) : Collections.emptyList();
            this.defaultOutputModes = card.defaultOutputModes != null ? new ArrayList<>(card.defaultOutputModes) : Collections.emptyList();
            this.skills = card.skills != null ? new ArrayList<>(card.skills) : Collections.emptyList();
            this.securitySchemes = card.securitySchemes != null ? Map.copyOf(card.securitySchemes) : Collections.emptyMap();
            this.securityRequirements = card.securityRequirements != null ? new ArrayList<>(card.securityRequirements) :Collections.emptyList();
            this.iconUrl = card.iconUrl;
            this.supportedInterfaces = card.supportedInterfaces != null ? new ArrayList<>(card.supportedInterfaces) : Collections.emptyList();
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
         * @param securityRequirements the list of security requirements (optional)
         * @return this builder for method chaining
         */
        public Builder securityRequirements(List<Map<String, List<String>>> securityRequirements) {
            this.securityRequirements = securityRequirements;
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
         * Sets the ordered list of supported protocol interfaces (first entry is preferred).
         * <p>
         * Each interface defines a combination of protocol binding (e.g., "JSONRPC", "GRPC", "REST")
         * and URL endpoint for accessing the agent. This is the primary field for declaring how
         * clients can communicate with the agent as of protocol version 1.0.0.
         * <p>
         * Example:
         * <pre>{@code
         * .supportedInterfaces(List.of(
         *     new AgentInterface("JSONRPC", "http://localhost:9999"),
         *     new AgentInterface("GRPC", "grpc://localhost:9090")
         * ))
         * }</pre>
         *
         * @param supportedInterfaces the ordered list of supported interfaces (required)
         * @return this builder for method chaining
         * @see AgentInterface
         */
        public Builder supportedInterfaces(List<AgentInterface> supportedInterfaces) {
            this.supportedInterfaces = supportedInterfaces;
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
            return new AgentCard(
                    Assert.checkNotNullParam("name", name),
                    Assert.checkNotNullParam("description", description),
                    provider,
                    Assert.checkNotNullParam("version", version),
                    documentationUrl,
                    Assert.checkNotNullParam("capabilities", capabilities),
                    Assert.checkNotNullParam("defaultInputModes", defaultInputModes),
                    Assert.checkNotNullParam("defaultOutputModes", defaultOutputModes),
                    Assert.checkNotNullParam("skills", skills),
                    securitySchemes,
                    securityRequirements,
                    iconUrl,
                    Assert.checkNotNullParam("supportedInterfaces", supportedInterfaces),
                    signatures);
        }
    }
}
