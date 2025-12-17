package io.a2a.grpc.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ObjectFactory;

/**
 * Mapper between {@link io.a2a.spec.DataPart} and {@link io.a2a.grpc.DataPart}.
 * <p>
 * Handles conversion of structured data using Protobuf Struct.
 * Uses CommonFieldMapper for Map ↔ Struct conversion.
 * <p>
 * Note: Proto DataPart only has 'data' field. Domain DataPart also has 'metadata' field
 * (inherited from Part), which is not persisted in the proto and will be null after conversion.
 * Uses @ObjectFactory to resolve constructor ambiguity.
 */
@Mapper(config = A2AProtoMapperConfig.class, uses = {A2ACommonFieldMapper.class})
public interface DataPartMapper {

    DataPartMapper INSTANCE = A2AMappers.getMapper(DataPartMapper.class);

    /**
     * Converts domain DataPart to proto DataPart.
     * Uses CommonFieldMapper for Map → Struct conversion.
     * Metadata is ignored (not part of proto definition).
     */
    @Mapping(target = "data", source = "data", conditionExpression = "java(domain.data() != null)", qualifiedByName = "mapToStruct")
    io.a2a.grpc.DataPart toProto(io.a2a.spec.DataPart domain);

    /**
     * Converts proto DataPart to domain DataPart.
     * Uses CommonFieldMapper for Struct → Map conversion via Builder.
     */
    @Mapping(target = "data", source = "data", qualifiedByName = "structToMap")
    io.a2a.spec.DataPart fromProto(io.a2a.grpc.DataPart proto);
}
