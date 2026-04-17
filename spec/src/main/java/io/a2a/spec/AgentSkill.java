package io.a2a.spec;

import java.util.List;
import java.util.Map;

import io.a2a.util.Assert;

/**
 * The set of skills, or distinct capabilities, that the agent can perform.
 *
 * @param id          a unique identifier for the skill
 * @param name        the human-readable name of the skill
 * @param description a human-readable description of the skill
 * @param tags        tags for categorizing or discovering the skill
 * @param examples    example prompts or use cases for the skill
 * @param inputModes  the content modes accepted as input by the skill
 * @param outputModes the content modes produced as output by the skill
 * @param security    optional security requirements specific to this skill
 */
public record AgentSkill(String id, String name, String description, List<String> tags,
        List<String> examples, List<String> inputModes, List<String> outputModes,
        List<Map<String, List<String>>> security) {

    public AgentSkill {
        Assert.checkNotNullParam("description", description);
        Assert.checkNotNullParam("id", id);
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("tags", tags);
    }

    public static class Builder {

        private String id;
        private String name;
        private String description;
        private List<String> tags;
        private List<String> examples;
        private List<String> inputModes;
        private List<String> outputModes;
        private List<Map<String, List<String>>> security;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        public Builder examples(List<String> examples) {
            this.examples = examples;
            return this;
        }

        public Builder inputModes(List<String> inputModes) {
            this.inputModes = inputModes;
            return this;
        }

        public Builder outputModes(List<String> outputModes) {
            this.outputModes = outputModes;
            return this;
        }

        public Builder security(List<Map<String, List<String>>> security) {
            this.security = security;
            return this;
        }

        public AgentSkill build() {
            return new AgentSkill(id, name, description, tags, examples, inputModes, outputModes, security);
        }
    }
}
