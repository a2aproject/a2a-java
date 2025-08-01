package io.a2a.spec;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.a2a.util.Assert;

/**
 * The set of skills, or distinct capabilities, that the agent can perform.
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

    public static class Builder {

        private String id;
        private String name;
        private String description;
        private List<String> tags;
        private List<String> examples;
        private List<String> inputModes;
        private List<String> outputModes;

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

        public AgentSkill build() {
            return new AgentSkill(id, name, description, tags, examples, inputModes, outputModes);
        }
    }
}
