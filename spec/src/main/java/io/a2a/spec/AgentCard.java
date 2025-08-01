package io.a2a.spec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.a2a.util.Assert;

/**
 * The AgentCard is a self-describing manifest for an agent. It provides essential
 * metadata including the agent's identity, capabilities, skills, supported
 * communication methods, and security requirements.
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
    private static final String DEFAULT_PROTOCOL_VERSION = "0.3.0";
    private static final TransportProtocol DEFAULT_TRANSPORT = TransportProtocol.JSONRPC;

    public AgentCard {
        Assert.checkNotNullParam("capabilities", capabilities);
        Assert.checkNotNullParam("defaultInputModes", defaultInputModes);
        Assert.checkNotNullParam("defaultOutputModes", defaultOutputModes);
        Assert.checkNotNullParam("description", description);
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("skills", skills);
        Assert.checkNotNullParam("url", url);
        Assert.checkNotNullParam("version", version);
        if (protocolVersion == null) {
            protocolVersion = DEFAULT_PROTOCOL_VERSION;
        }
        if (preferredTransport == null) {
            preferredTransport = DEFAULT_TRANSPORT.asString();
        }
    }

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

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder provider(AgentProvider provider) {
            this.provider = provider;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder documentationUrl(String documentationUrl) {
            this.documentationUrl = documentationUrl;
            return this;
        }

        public Builder capabilities(AgentCapabilities capabilities) {
            this.capabilities = capabilities;
            return this;
        }

        public Builder defaultInputModes(List<String> defaultInputModes) {
            this.defaultInputModes = defaultInputModes;
            return this;
        }

        public Builder defaultOutputModes(List<String> defaultOutputModes) {
            this.defaultOutputModes = defaultOutputModes;
            return this;
        }

        public Builder skills(List<AgentSkill> skills) {
            this.skills = skills;
            return this;
        }

        public Builder supportsAuthenticatedExtendedCard(boolean supportsAuthenticatedExtendedCard) {
            this.supportsAuthenticatedExtendedCard = supportsAuthenticatedExtendedCard;
            return this;
        }

        public Builder securitySchemes(Map<String, SecurityScheme> securitySchemes) {
            this.securitySchemes = securitySchemes;
            return this;
        }

        public Builder security(List<Map<String, List<String>>> security) {
            this.security = security;
            return this;
        }

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
            if (preferredTransport == null) {
                preferredTransport = DEFAULT_TRANSPORT.asString();
            }
            if (additionalInterfaces == null) {
                // should include an entry matching the main 'url' and 'preferredTransport'
                additionalInterfaces = new ArrayList<>();
                additionalInterfaces.add(new AgentInterface(preferredTransport, url));
            }
            return new AgentCard(name, description, url, provider, version, documentationUrl,
                    capabilities, defaultInputModes, defaultOutputModes, skills,
                    supportsAuthenticatedExtendedCard, securitySchemes, security, iconUrl,
                    additionalInterfaces, preferredTransport, protocolVersion);
        }
    }
}
