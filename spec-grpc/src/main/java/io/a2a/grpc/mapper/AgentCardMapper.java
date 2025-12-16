package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link io.a2a.spec.AgentCard} and {@link io.a2a.grpc.AgentCard}.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        uses = {
            AgentProviderMapper.class,
            AgentCapabilitiesMapper.class,
            AgentSkillMapper.class,
            SecuritySchemeMapper.class,
            SecurityMapper.class,
            AgentInterfaceMapper.class,
            AgentCardSignatureMapper.class
        })
public interface AgentCardMapper {

    AgentCardMapper INSTANCE = A2AMappers.getMapper(AgentCardMapper.class);

    // Deprecated proto fields - not present in spec API (removed in 1.0.0)
    @Mapping(target = "url", ignore = true)
    @Mapping(target = "preferredTransport", ignore = true)
    @Mapping(target = "additionalInterfaces", ignore = true)
    @Mapping(target = "provider", source = "provider", conditionExpression = "java(domain.provider() != null)")
    @Mapping(target = "documentationUrl", source = "documentationUrl", conditionExpression = "java(domain.documentationUrl() != null)")
    @Mapping(target = "iconUrl", source = "iconUrl", conditionExpression = "java(domain.iconUrl() != null)")
    io.a2a.grpc.AgentCard toProto(io.a2a.spec.AgentCard domain);

    @Mapping(target = "provider", source = "provider", conditionExpression = "java(proto.hasProvider())")
    @Mapping(target = "documentationUrl", source = "documentationUrl", conditionExpression = "java(!proto.getDocumentationUrl().isEmpty())")
    @Mapping(target = "iconUrl", source = "iconUrl", conditionExpression = "java(!proto.getIconUrl().isEmpty())")
    io.a2a.spec.AgentCard fromProto(io.a2a.grpc.AgentCard proto);
}
