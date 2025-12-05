package io.a2a.grpc.mapper;

import java.util.Base64;
import java.util.Map;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ObjectFactory;

import io.a2a.spec.DataPart;
import io.a2a.spec.FileWithBytes;
import io.a2a.spec.FileWithUri;
import io.a2a.spec.InvalidRequestError;

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
    @Mapping(target = "data", source = "data", conditionExpression = "java(domain.getData() != null)", qualifiedByName = "mapToStruct")
    io.a2a.grpc.DataPart toProto(io.a2a.spec.DataPart domain);

    /**
     * Converts proto DataPart to domain DataPart.
     */
    default io.a2a.spec.DataPart fromProto(io.a2a.grpc.DataPart proto) {
        if (proto == null) {
            return null;
        }

        Map<String, Object> data = A2ACommonFieldMapper.INSTANCE.structToMap(proto.getData());
        return new DataPart(data);
    }

    /**
     * Object factory for creating DataPart instances.
     * <p>
     * Resolves constructor ambiguity by explicitly using the single-arg constructor.
     * The metadata field will be null (not part of proto definition).
     *
     * @param proto the proto DataPart
     * @return new DataPart instance using single-arg constructor
     */
    @ObjectFactory
    default io.a2a.spec.DataPart createDataPart(io.a2a.grpc.DataPart proto) {
        java.util.Map<String, Object> data = A2ACommonFieldMapper.INSTANCE.structToMap(proto.getData());
        return new io.a2a.spec.DataPart(data);
    }
}
