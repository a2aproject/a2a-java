package org.a2aproject.sdk.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link org.a2aproject.sdk.spec.AgentCardSignature} and {@link org.a2aproject.sdk.grpc.AgentCardSignature}.
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
    org.a2aproject.sdk.grpc.AgentCardSignature toProto(org.a2aproject.sdk.spec.AgentCardSignature domain);

    /**
     * Converts proto AgentCardSignature to domain AgentCardSignature.
     * <p>
     * Maps protected field → protectedHeader and header from struct to map.
     */
    @Mapping(source = "protected", target = "protectedHeader")
    @Mapping(target = "header", source = "header", qualifiedByName = "structToMap")
    org.a2aproject.sdk.spec.AgentCardSignature fromProto(org.a2aproject.sdk.grpc.AgentCardSignature proto);
}
