package org.a2aproject.sdk.compat03.conversion.mappers.domain;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.a2aproject.sdk.compat03.spec.APIKeySecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCapabilities_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCardSignature_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentExtension_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentInterface_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentProvider_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentSkill_v0_3;
import org.a2aproject.sdk.compat03.spec.SecurityScheme_v0_3;
import org.a2aproject.sdk.spec.APIKeySecurityScheme;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentCardSignature;
import org.a2aproject.sdk.spec.AgentExtension;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.AgentProvider;
import org.a2aproject.sdk.spec.AgentSkill;
import org.a2aproject.sdk.spec.Legacy_0_3_AgentInterface;
import org.a2aproject.sdk.spec.SecurityRequirement;
import org.a2aproject.sdk.spec.SecurityScheme;

/** Converts the protocol card model without depending on server components. */
public final class AgentCardMapper_v0_3 {

    public static final AgentCardMapper_v0_3 INSTANCE = new AgentCardMapper_v0_3();

    private AgentCardMapper_v0_3() {
    }

    public AgentCard toV10(AgentCard_v0_3 source) {
        List<AgentInterface> interfaces = source.additionalInterfaces().stream()
            .map(i -> new AgentInterface(i.transport(), i.url(), null, "0.3"))
            .toList();
        return AgentCard.builder()
            .name(source.name()).description(source.description()).provider(toV10(source.provider()))
            .version(source.version()).documentationUrl(source.documentationUrl())
            .capabilities(toV10Capabilities(source))
            .defaultInputModes(source.defaultInputModes()).defaultOutputModes(source.defaultOutputModes())
            .skills(source.skills().stream().map(this::toV10).toList())
            .securitySchemes(toV10Security(source.securitySchemes()))
            .securityRequirements(toV10Requirements(source.security()))
            .iconUrl(source.iconUrl()).supportedInterfaces(interfaces)
            .signatures(source.signatures() == null ? null : source.signatures().stream().map(this::toV10).toList())
            .url(source.url()).preferredTransport(source.preferredTransport())
            .additionalInterfaces(source.additionalInterfaces().stream()
                .map(i -> new Legacy_0_3_AgentInterface(i.transport(), i.url())).toList())
            .build();
    }

    public AgentCard_v0_3 fromV10(AgentCard source) {
        List<AgentInterface_v0_3> interfaces = source.supportedInterfaces().stream()
            .filter(i -> "0.3".equals(i.protocolVersion()))
            .map(i -> new AgentInterface_v0_3(i.protocolBinding(), i.url())).toList();
        if (interfaces.isEmpty() && source.url() != null && source.preferredTransport() != null) {
            interfaces = List.of(new AgentInterface_v0_3(source.preferredTransport(), source.url()));
        }
        return new AgentCard_v0_3(source.name(), source.description(), source.url(), fromV10(source.provider()),
            source.version(), source.documentationUrl(), fromV10(source.capabilities()), source.defaultInputModes(),
            source.defaultOutputModes(), source.skills().stream().map(this::fromV10).toList(),
            source.capabilities().extendedAgentCard(), fromV03Security(source.securitySchemes()),
            fromV10Requirements(source.securityRequirements()), source.iconUrl(), interfaces,
            source.preferredTransport(), "0.3", source.signatures() == null ? null : source.signatures().stream()
                .map(this::fromV10).toList());
    }

    private AgentProvider toV10(AgentProvider_v0_3 value) {
        return value == null ? null : new AgentProvider(value.organization(), value.url());
    }

    private AgentProvider_v0_3 fromV10(AgentProvider value) {
        return value == null ? null : new AgentProvider_v0_3(value.organization(), value.url());
    }

    private AgentCapabilities toV10Capabilities(AgentCard_v0_3 value) {
        AgentCapabilities_v0_3 capabilities = value.capabilities();
        List<AgentExtension> extensions = capabilities.extensions() == null ? null : capabilities.extensions().stream()
            .map(e -> new AgentExtension(e.description(), e.params(), e.required(), e.uri())).toList();
        return new AgentCapabilities(capabilities.streaming(), capabilities.pushNotifications(),
            value.supportsAuthenticatedExtendedCard(), extensions);
    }

    private AgentCapabilities_v0_3 fromV10(AgentCapabilities value) {
        List<AgentExtension_v0_3> extensions = value.extensions() == null ? null : value.extensions().stream()
            .map(e -> new AgentExtension_v0_3(e.description(), e.params(), e.required(), e.uri())).toList();
        return new AgentCapabilities_v0_3(value.streaming(), value.pushNotifications(), false, extensions);
    }

    private AgentSkill toV10(AgentSkill_v0_3 value) {
        return AgentSkill.builder().id(value.id()).name(value.name()).description(value.description()).tags(value.tags())
            .examples(value.examples()).inputModes(value.inputModes()).outputModes(value.outputModes())
            .securityRequirements(toV10Requirements(value.security())).build();
    }

    private AgentSkill_v0_3 fromV10(AgentSkill value) {
        return new AgentSkill_v0_3(value.id(), value.name(), value.description(), value.tags(), value.examples(),
            value.inputModes(), value.outputModes(), fromV10Requirements(value.securityRequirements()));
    }

    private AgentCardSignature toV10(AgentCardSignature_v0_3 value) {
        return new AgentCardSignature(value.header(), value.protectedHeader(), value.signature());
    }

    private AgentCardSignature_v0_3 fromV10(AgentCardSignature value) {
        return new AgentCardSignature_v0_3(value.header(), value.protectedHeader(), value.signature());
    }

    private Map<String, SecurityScheme> toV10Security(Map<String, SecurityScheme_v0_3> source) {
        if (source == null) return null;
        return source.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> {
            if (e.getValue() instanceof APIKeySecurityScheme_v0_3 api) {
                return new APIKeySecurityScheme(APIKeySecurityScheme.Location.fromString(api.in()), api.name(), api.description());
            }
            throw new IllegalArgumentException("Unsupported 0.3 security scheme: " + e.getValue().type());
        }));
    }

    private Map<String, SecurityScheme_v0_3> fromV03Security(Map<String, SecurityScheme> source) {
        if (source == null) return null;
        return source.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> {
            if (e.getValue() instanceof APIKeySecurityScheme api) {
                return new APIKeySecurityScheme_v0_3(api.location().asString(), api.name(), api.description());
            }
            throw new IllegalArgumentException("Unsupported 1.0 security scheme: " + e.getValue().type());
        }));
    }

    private List<SecurityRequirement> toV10Requirements(List<Map<String, List<String>>> source) {
        return source == null ? null : source.stream().map(SecurityRequirement::new).toList();
    }

    private List<Map<String, List<String>>> fromV10Requirements(List<SecurityRequirement> source) {
        return source == null ? null : source.stream().map(SecurityRequirement::schemes).toList();
    }
}
