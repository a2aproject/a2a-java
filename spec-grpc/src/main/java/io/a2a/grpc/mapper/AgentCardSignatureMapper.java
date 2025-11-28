package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link io.a2a.spec.AgentCardSignature} and {@link io.a2a.grpc.AgentCardSignature}.
 * <p>
 * Uses CommonFieldMapper for struct conversion (header field).
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        uses = {A2ACommonFieldMapper.class})
public interface AgentCardSignatureMapper {

    AgentCardSignatureMapper INSTANCE = A2AMappers.getMapper(AgentCardSignatureMapper.class);

    /**
     * Converts domain AgentCardSignature to proto AgentCardSignature.
     * <p>
     * Maps protectedHeader → protected field and header via struct conversion.
     */
    @Mapping(source = "protectedHeader", target = "protected")
    @Mapping(target = "header", source = "header", conditionExpression = "java(domain.header() != null)", qualifiedByName = "mapToStruct")
    io.a2a.grpc.AgentCardSignature toProto(io.a2a.spec.AgentCardSignature domain);

    /**
     * Converts proto AgentCardSignature to domain AgentCardSignature.
     * <p>
     * Maps protected field → protectedHeader and header from struct to map.
     */
    @Mapping(source = "protected", target = "protectedHeader")
    @Mapping(target = "header", source = "header", qualifiedByName = "structToMap")
    io.a2a.spec.AgentCardSignature fromProto(io.a2a.grpc.AgentCardSignature proto);
}
