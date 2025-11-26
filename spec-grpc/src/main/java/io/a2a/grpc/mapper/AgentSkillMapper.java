package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;

/**
 * Mapper between {@link io.a2a.spec.AgentSkill} and {@link io.a2a.grpc.AgentSkill}.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        uses = SecurityMapper.class)
public interface AgentSkillMapper {

    AgentSkillMapper INSTANCE = A2AMappers.getMapper(AgentSkillMapper.class);

    io.a2a.grpc.AgentSkill toProto(io.a2a.spec.AgentSkill domain);
}
