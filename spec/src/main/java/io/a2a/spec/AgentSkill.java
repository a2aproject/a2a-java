package io.a2a.spec;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.a2a.util.Assert;

/**
 * Represents a specific skill or capability that an agent offers.
 * Skills define what the agent can do, including the types of input it accepts,
 * the output it produces, and examples of how to use the skill.
 *
 * @param id unique identifier for this skill
 * @param name human-readable name of the skill
 * @param description detailed description of what this skill does and how to use it
 * @param tags list of tags for categorizing and discovering this skill
 * @param examples list of example inputs or use cases for this skill
 * @param inputModes MIME types this skill accepts as input (overrides agent defaults)
 * @param outputModes MIME types this skill produces as output (overrides agent defaults)
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentSkill(String id, String name, String description, List<String> tags,
                         List<String> examples, List<String> inputModes, List<String> outputModes) {

    public AgentSkill {
        Assert.checkNotNullParam("description", description);
        Assert.checkNotNullParam("id", id);
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("tags", tags);
    }

    /**
     * Builder class for constructing {@link AgentSkill} instances.
     */
    public static class Builder {

        private String id;
        private String name;
        private String description;
        private List<String> tags;
        private List<String> examples;
        private List<String> inputModes;
        private List<String> outputModes;

        /**
         * Sets the unique identifier for this skill.
         *
         * @param id the skill ID
         * @return this builder instance
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the human-readable name of the skill.
         *
         * @param name the skill name
         * @return this builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the detailed description of what this skill does.
         *
         * @param description the skill description
         * @return this builder instance
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the tags for categorizing and discovering this skill.
         *
         * @param tags the list of tags
         * @return this builder instance
         */
        public Builder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        /**
         * Sets the example inputs or use cases for this skill.
         *
         * @param examples the list of examples
         * @return this builder instance
         */
        public Builder examples(List<String> examples) {
            this.examples = examples;
            return this;
        }

        /**
         * Sets the MIME types this skill accepts as input.
         *
         * @param inputModes the list of input MIME types
         * @return this builder instance
         */
        public Builder inputModes(List<String> inputModes) {
            this.inputModes = inputModes;
            return this;
        }

        /**
         * Sets the MIME types this skill produces as output.
         *
         * @param outputModes the list of output MIME types
         * @return this builder instance
         */
        public Builder outputModes(List<String> outputModes) {
            this.outputModes = outputModes;
            return this;
        }

        /**
         * Builds and returns a new {@link AgentSkill} instance with the configured properties.
         *
         * @return a new AgentSkill instance
         * @throws IllegalArgumentException if required fields (id, name, description, tags) are null
         */
        public AgentSkill build() {
            return new AgentSkill(id, name, description, tags, examples, inputModes, outputModes);
        }
    }
}
