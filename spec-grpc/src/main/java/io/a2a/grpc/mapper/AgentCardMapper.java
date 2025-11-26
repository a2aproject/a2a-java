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

    @Mapping(target = "provider", source = "provider", conditionExpression = "java(domain.provider() != null)")
    @Mapping(target = "documentationUrl", source = "documentationUrl", conditionExpression = "java(domain.documentationUrl() != null)")
    @Mapping(target = "iconUrl", source = "iconUrl", conditionExpression = "java(domain.iconUrl() != null)")
    @Mapping(target = "url", source = "url", conditionExpression = "java(domain.url() != null)")
    @Mapping(target = "preferredTransport", source = "preferredTransport", conditionExpression = "java(domain.preferredTransport() != null)")
    @Mapping(source = "additionalInterfaces", target = "supportedInterfaces")
    io.a2a.grpc.AgentCard toProto(io.a2a.spec.AgentCard domain);
}
