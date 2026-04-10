package org.a2aproject.sdk.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;

/**
 * Mapper between {@link org.a2aproject.sdk.spec.AgentSkill} and {@link org.a2aproject.sdk.grpc.AgentSkill}.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        uses = SecurityRequirementMapper.class)
public interface AgentSkillMapper {

    AgentSkillMapper INSTANCE = A2AMappers.getMapper(AgentSkillMapper.class);

    org.a2aproject.sdk.grpc.AgentSkill toProto(org.a2aproject.sdk.spec.AgentSkill domain);

    org.a2aproject.sdk.spec.AgentSkill fromProto(org.a2aproject.sdk.grpc.AgentSkill proto);
}
