package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Mapper between {@link io.a2a.spec.AgentCardSignature} and {@link io.a2a.grpc.AgentCardSignature}.
 * <p>
 * Uses CommonFieldMapper for struct conversion (header field).
 */
@Mapper(config = ProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        uses = {CommonFieldMapper.class})
public interface AgentCardSignatureMapper {

    AgentCardSignatureMapper INSTANCE = Mappers.getMapper(AgentCardSignatureMapper.class);

    /**
     * Converts domain AgentCardSignature to proto AgentCardSignature.
     * <p>
     * Maps protectedHeader → protected field and header via struct conversion.
     */
    @Mapping(source = "protectedHeader", target = "protected")
    @Mapping(target = "header", source = "header", conditionExpression = "java(domain.header() != null)", qualifiedByName = "mapToStruct")
    io.a2a.grpc.AgentCardSignature toProto(io.a2a.spec.AgentCardSignature domain);
}
