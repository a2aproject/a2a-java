package io.a2a.grpc.mapper;

import io.a2a.grpc.ListTasksRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for ListTasksParams ↔ ListTasksRequest conversions.
 * <p>
 * Handles the conversion between domain ListTasksParams and protobuf ListTasksRequest,
 * with special handling for optional fields and timestamp conversions.
 */
@Mapper(config = A2AProtoMapperConfig.class, uses = {TaskStateMapper.class, A2ACommonFieldMapper.class})
public interface ListTasksParamsMapper {

    ListTasksParamsMapper INSTANCE = A2AMappers.getMapper(ListTasksParamsMapper.class);

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
    @Mapping(target = "statusTimestampAfter", source = "statusTimestampAfter", qualifiedByName = "instantToProtoTimestamp")
    @Mapping(target = "includeArtifacts", source = "includeArtifacts", conditionExpression = "java(params.includeArtifacts() != null)")
    ListTasksRequest toProto(io.a2a.spec.ListTasksParams params);

    /**
     * Converts protobuf ListTasksRequest to domain ListTasksParams.
     *
     * @param request the protobuf ListTasksRequest
     * @return domain ListTasksParams
     */
    @Mapping(target = "contextId", source = "contextId", qualifiedByName = "emptyToNull")
    @Mapping(target = "status", source = "status", qualifiedByName = "taskStateOrNull")
    // pageSize: Check if field is set using hasPageSize() to distinguish unset (null) from explicit 0 (validation error)
    @Mapping(target = "pageSize", expression = "java(request.hasPageSize() ? request.getPageSize() : null)")
    @Mapping(target = "pageToken", source = "pageToken", qualifiedByName = "emptyToNull")
    // historyLength: Check if field is set using hasHistoryLength() for consistency with pageSize
    @Mapping(target = "historyLength", expression = "java(request.hasHistoryLength() ? request.getHistoryLength() : null)")
    @Mapping(target = "statusTimestampAfter", source = "statusTimestampAfter", qualifiedByName = "protoTimestampToInstant")
    @Mapping(target = "includeArtifacts", source = "includeArtifacts", qualifiedByName = "falseToNull")
    io.a2a.spec.ListTasksParams fromProto(ListTasksRequest request);
}
