package io.a2a.grpc.mapper;

import io.a2a.spec.ListTasksResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link io.a2a.spec.ListTasksResult} and {@link io.a2a.grpc.ListTasksResponse}.
 * <p>
 * Handles conversion with null handling for nextPageToken field.
 * Uses ADDER_PREFERRED strategy to avoid ProtocolMessageList instantiation issues.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = org.mapstruct.CollectionMappingStrategy.ADDER_PREFERRED,
        uses = {TaskMapper.class})
public interface ListTasksResultMapper {

    ListTasksResultMapper INSTANCE = A2AMappers.getMapper(ListTasksResultMapper.class);

    /**
     * Converts domain ListTasksResult to proto ListTasksResponse.
     * Protobuf builders don't accept null, so nextPageToken is conditionally mapped.
     */
    @Mapping(target = "nextPageToken", source = "nextPageToken", conditionExpression = "java(domain.nextPageToken() != null)")
    io.a2a.grpc.ListTasksResponse toProto(ListTasksResult domain);

    @Mapping(source = "nextPageToken", target = "nextPageToken", conditionExpression = "java(proto.getNextPageToken() != null)")
    ListTasksResult fromProto(io.a2a.grpc.ListTasksResponse proto);
}
