package io.a2a.spec;

import java.util.List;
import java.util.Map;

import io.a2a.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * Represents a distinct skill or capability that an agent can perform in the A2A Protocol.
 * <p>
 * An AgentSkill defines a specific operation or category of operations that the agent supports.
 * Skills provide a structured way to advertise what an agent can do, helping clients discover
 * and invoke appropriate functionality. Each skill is uniquely identified and includes metadata
 * about supported input/output modes, examples, and security requirements.
 * <p>
 * <b>Key Components:</b>
 * <ul>
 *   <li><b>Identity:</b> Unique ID and human-readable name for discovery and invocation</li>
 *   <li><b>Documentation:</b> Description, examples, and tags for understanding usage</li>
 *   <li><b>Modes:</b> Supported input/output formats (text, audio, image, etc.)</li>
 *   <li><b>Security:</b> Specific authentication/authorization requirements for this skill</li>
 * </ul>
 * <p>
 * Skills are declared in the {@link AgentCard} and represent the agent's advertised capabilities.
 * Clients can query available skills to understand what operations are supported and how to
 * invoke them. If inputModes/outputModes are not specified, the skill inherits the defaults
 * from the AgentCard.
 * <p>
 * This class is immutable and uses the Builder pattern for construction.
 *
 * @param id unique identifier for the skill (required, e.g., "weather_query", "translate_text")
 * @param name human-readable name of the skill (required, e.g., "Weather Queries")
 * @param description detailed explanation of what the skill does and how to use it (required)
 * @param tags categorization tags for discovery and filtering (required, may be empty list)
 * @param examples example queries or use cases demonstrating the skill (optional)
 * @param inputModes supported input formats for this skill (optional, inherits from AgentCard if not set)
 * @param outputModes supported output formats for this skill (optional, inherits from AgentCard if not set)
 * @param securityRequirements security requirements specific to this skill (optional)
 * @see AgentCard
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record AgentSkill(String id, String name, String description, List<String> tags,
                         @Nullable List<String> examples, @Nullable List<String> inputModes, @Nullable List<String> outputModes,
                         @Nullable List<Map<String, List<String>>> securityRequirements) {

    /**
     * Compact constructor that validates required fields.
     *
     * @param id the id parameter (see class-level JavaDoc)
     * @param name the name parameter (see class-level JavaDoc)
     * @param description the description parameter (see class-level JavaDoc)
     * @param tags the tags parameter (see class-level JavaDoc)
     * @param examples the examples parameter (see class-level JavaDoc)
     * @param inputModes the inputModes parameter (see class-level JavaDoc)
     * @param outputModes the outputModes parameter (see class-level JavaDoc)
     * @param securityRequirements the security parameter (see class-level JavaDoc)
     * @throws IllegalArgumentException if id, name, description, or tags is null
     */
    public AgentSkill {
        Assert.checkNotNullParam("id", id);
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("description", description);
        Assert.checkNotNullParam("tags", tags);
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
     * Builder for constructing immutable {@link AgentSkill} instances.
     * <p>
     * The Builder pattern provides a fluent API for setting skill properties.
     * This approach ensures that once an AgentSkill is created, its state cannot
     * be modified, which is important for protocol correctness and thread-safety.
     * <p>
     * Example usage:
     * <pre>{@code
     * AgentSkill skill = AgentSkill.builder()
     *     .id("weather_query")
     *     .name("Weather Queries")
     *     .description("Get current weather conditions for any location")
     *     .tags(List.of("weather", "information"))
     *     .examples(List.of(
     *         "What's the weather in Tokyo?",
     *         "Current temperature in London"
     *     ))
     *     .inputModes(List.of("text"))
     *     .outputModes(List.of("text"))
     *     .build();
     * }</pre>
     */
    public static class Builder {

        private @Nullable String id;
        private @Nullable String name;
        private @Nullable String description;
        private @Nullable List<String> tags;
        private @Nullable List<String> examples;
        private @Nullable List<String> inputModes;
        private @Nullable List<String> outputModes;
        private @Nullable List<Map<String, List<String>>> securityRequirements;

        /**
         * Creates a new Builder with all fields unset.
         */
        private Builder() {
        }

        /**
         * Sets the unique identifier for the skill.
         * <p>
         * The ID should be a stable identifier that doesn't change across versions,
         * typically in snake_case format (e.g., "weather_query", "translate_text").
         *
         * @param id the skill ID (required)
         * @return this builder for method chaining
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the human-readable name of the skill.
         * <p>
         * The name is displayed to users and should clearly describe the skill's purpose.
         *
         * @param name the skill name (required)
         * @return this builder for method chaining
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets a detailed description of what the skill does and how to use it.
         * <p>
         * The description should provide sufficient information for users to understand
         * when and how to invoke the skill.
         *
         * @param description the skill description (required)
         * @return this builder for method chaining
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets categorization tags for discovery and filtering.
         * <p>
         * Tags help organize skills and enable clients to filter or search for
         * specific categories of functionality.
         *
         * @param tags list of tags (required, may be empty)
         * @return this builder for method chaining
         */
        public Builder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        /**
         * Sets example queries or use cases demonstrating the skill.
         * <p>
         * Examples help users understand how to phrase requests to effectively
         * invoke the skill.
         *
         * @param examples list of example queries (optional)
         * @return this builder for method chaining
         */
        public Builder examples(List<String> examples) {
            this.examples = examples;
            return this;
        }

        /**
         * Sets the supported input formats for this skill.
         * <p>
         * If not specified, the skill inherits the default input modes from the AgentCard.
         * Common values include "text", "audio", "image", "video".
         *
         * @param inputModes list of supported input formats (optional)
         * @return this builder for method chaining
         */
        public Builder inputModes(List<String> inputModes) {
            this.inputModes = inputModes;
            return this;
        }

        /**
         * Sets the supported output formats for this skill.
         * <p>
         * If not specified, the skill inherits the default output modes from the AgentCard.
         * Common values include "text", "audio", "image", "video".
         *
         * @param outputModes list of supported output formats (optional)
         * @return this builder for method chaining
         */
        public Builder outputModes(List<String> outputModes) {
            this.outputModes = outputModes;
            return this;
        }

        /**
         * Sets security requirements specific to this skill.
         * <p>
         * Security requirements override or supplement the agent-level security
         * defined in the AgentCard. Each entry represents an alternative security
         * requirement, where each map contains scheme names and their required scopes.
         *
         * @param securityRequirements list of security requirements (optional)
         * @return this builder for method chaining
         */
        public Builder securityRequirements(List<Map<String, List<String>>> securityRequirements) {
            this.securityRequirements = securityRequirements;
            return this;
        }

        /**
         * Builds an immutable {@link AgentSkill} from the current builder state.
         *
         * @return a new AgentSkill instance
         * @throws IllegalArgumentException if any required field (id, name, description, tags) is null
         */
        public AgentSkill build() {
            return new AgentSkill(
                    Assert.checkNotNullParam("id", id),
                    Assert.checkNotNullParam("name", name),
                    Assert.checkNotNullParam("description", description),
                    Assert.checkNotNullParam("tags", tags), 
                    examples,
                    inputModes,
                    outputModes,
                    securityRequirements);
        }
    }
}
