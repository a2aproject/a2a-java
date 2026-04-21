package org.a2aproject.sdk.compat03.spec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.a2aproject.sdk.util.Assert;

/**
 * The AgentCard is a self-describing manifest for an agent. It provides essential
 * metadata including the agent's identity, capabilities, skills, supported
 * communication methods, and security requirements.
 */
public record AgentCard_v0_3(String name, String description, String url, AgentProvider_v0_3 provider,
                             String version, String documentationUrl, AgentCapabilities_v0_3 capabilities,
                             List<String> defaultInputModes, List<String> defaultOutputModes, List<AgentSkill_v0_3> skills,
                             boolean supportsAuthenticatedExtendedCard, Map<String, SecurityScheme_v0_3> securitySchemes,
                             List<Map<String, List<String>>> security, String iconUrl, List<AgentInterface_v0_3> additionalInterfaces,
                             String preferredTransport, String protocolVersion, List<AgentCardSignature_v0_3> signatures) {

    private static final String DEFAULT_PROTOCOL_VERSION = "0.3.0";
    private static final TransportProtocol_v0_3 DEFAULT_TRANSPORT = TransportProtocol_v0_3.JSONRPC;

    public AgentCard_v0_3 {
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
        private AgentProvider_v0_3 provider;
        private String version;
        private String documentationUrl;
        private AgentCapabilities_v0_3 capabilities;
        private List<String> defaultInputModes;
        private List<String> defaultOutputModes;
        private List<AgentSkill_v0_3> skills;
        private boolean supportsAuthenticatedExtendedCard = false;
        private Map<String, SecurityScheme_v0_3> securitySchemes;
        private List<Map<String, List<String>>> security;
        private String iconUrl;
        private List<AgentInterface_v0_3> additionalInterfaces;
        private String preferredTransport;
        private String protocolVersion;
        private List<AgentCardSignature_v0_3> signatures;

        /**
         * Creates a new Builder.
         */
        public Builder() {

        }

        /**
         * Creates a new Builder as a copy of an existing AgentCard.
         *
         * @param card the AgentCard to copy
         */
        public Builder(AgentCard_v0_3 card) {
            this.name = card.name;
            this.description = card.description;
            this.url = card.url;
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
            this.additionalInterfaces = card.additionalInterfaces != null ? new ArrayList<>(card.additionalInterfaces) : null;
            this.preferredTransport = card.preferredTransport;
            this.protocolVersion = card.protocolVersion;
            this.signatures = card.signatures != null ? new ArrayList<>(card.signatures) : null;
        }

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

        public Builder provider(AgentProvider_v0_3 provider) {
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

        public Builder capabilities(AgentCapabilities_v0_3 capabilities) {
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

        public Builder skills(List<AgentSkill_v0_3> skills) {
            this.skills = skills;
            return this;
        }

        public Builder supportsAuthenticatedExtendedCard(boolean supportsAuthenticatedExtendedCard) {
            this.supportsAuthenticatedExtendedCard = supportsAuthenticatedExtendedCard;
            return this;
        }

        public Builder securitySchemes(Map<String, SecurityScheme_v0_3> securitySchemes) {
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

        public Builder additionalInterfaces(List<AgentInterface_v0_3> additionalInterfaces) {
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

        public Builder signatures(List<AgentCardSignature_v0_3> signatures) {
            this.signatures = signatures;
            return this;
        }

        public AgentCard_v0_3 build() {
            if (preferredTransport == null) {
                preferredTransport = DEFAULT_TRANSPORT.asString();
            }
            if (additionalInterfaces == null) {
                // should include an entry matching the main 'url' and 'preferredTransport'
                additionalInterfaces = new ArrayList<>();
                additionalInterfaces.add(new AgentInterface_v0_3(preferredTransport, url));
            }
            return new AgentCard_v0_3(name, description, url, provider, version, documentationUrl,
                    capabilities, defaultInputModes, defaultOutputModes, skills,
                    supportsAuthenticatedExtendedCard, securitySchemes, security, iconUrl,
                    additionalInterfaces, preferredTransport, protocolVersion, signatures);
        }
    }
}
