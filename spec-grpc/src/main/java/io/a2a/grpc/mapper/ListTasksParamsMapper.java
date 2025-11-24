package io.a2a.grpc.mapper;

import io.a2a.grpc.ListTasksRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * MapStruct mapper for ListTasksParams ↔ ListTasksRequest conversions.
 * <p>
 * Handles the conversion between domain ListTasksParams and protobuf ListTasksRequest,
 * with special handling for optional fields and timestamp conversions.
 */
@Mapper(config = ProtoMapperConfig.class, uses = {TaskStateMapper.class, CommonFieldMapper.class})
public interface ListTasksParamsMapper {

    ListTasksParamsMapper INSTANCE = Mappers.getMapper(ListTasksParamsMapper.class);

    /**
     * Converts domain ListTasksParams to protobuf ListTasksRequest.
     *
     * @param params the domain ListTasksParams
     * @return protobuf ListTasksRequest
     */
    @Mapping(target = "contextId", source = "contextId", conditionExpression = "java(params.contextId() != null)")
    @Mapping(target = "status", source = "status", conditionExpression = "java(params.status() != null)")
    @Mapping(target = "pageSize", source = "pageSize", conditionExpression = "java(params.pageSize() != null)")
    @Mapping(target = "pageToken", source = "pageToken", conditionExpression = "java(params.pageToken() != null)")
    @Mapping(target = "historyLength", source = "historyLength", conditionExpression = "java(params.historyLength() != null)")
    @Mapping(target = "lastUpdatedAfter", source = "lastUpdatedAfter", qualifiedByName = "instantToMillis")
    @Mapping(target = "includeArtifacts", source = "includeArtifacts", conditionExpression = "java(params.includeArtifacts() != null)")
    @Mapping(target = "metadata", source = "metadata", qualifiedByName = "metadataToProto")
    ListTasksRequest toProto(io.a2a.spec.ListTasksParams params);

    /**
     * Converts protobuf ListTasksRequest to domain ListTasksParams.
     *
     * @param request the protobuf ListTasksRequest
     * @return domain ListTasksParams
     */
    @Mapping(target = "contextId", source = "contextId", qualifiedByName = "emptyToNull")
    @Mapping(target = "status", source = "status", qualifiedByName = "taskStateOrNull")
    @Mapping(target = "pageSize", source = "pageSize", qualifiedByName = "zeroToNull")
    @Mapping(target = "pageToken", source = "pageToken", qualifiedByName = "emptyToNull")
    @Mapping(target = "historyLength", source = "historyLength", qualifiedByName = "zeroToNull")
    @Mapping(target = "lastUpdatedAfter", source = "lastUpdatedAfter", qualifiedByName = "millisToInstant")
    @Mapping(target = "includeArtifacts", source = "includeArtifacts", qualifiedByName = "falseToNull")
    @Mapping(target = "metadata", source = "metadata", qualifiedByName = "metadataFromProto")
    io.a2a.spec.ListTasksParams fromProto(ListTasksRequest request);
}
