package org.a2aproject.sdk.grpc.mapper;

import org.a2aproject.sdk.spec.CancelTaskParams;
import org.a2aproject.sdk.spec.TaskIdParams;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper for {@link org.a2aproject.sdk.spec.TaskIdParams} from various gRPC request types.
 * <p>
 * Extracts task ID from resource name format "tasks/{id}" using {@link ResourceNameParser}.
 */
@Mapper(config = A2AProtoMapperConfig.class, uses = A2ACommonFieldMapper.class)
public interface TaskIdParamsMapper {

    TaskIdParamsMapper INSTANCE = A2AMappers.getMapper(TaskIdParamsMapper.class);

    /**
     * Converts proto CancelTaskRequest to domain TaskIdParams.
     * Extracts task ID from the resource name.
     */
    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "id", source = "id")
    @Mapping(target = "metadata", source = "metadata", qualifiedByName = "metadataFromProto")
    CancelTaskParams fromProtoCancelTaskRequest(org.a2aproject.sdk.grpc.CancelTaskRequest proto);
    
     /**
     * Converts proto CancelTaskRequest to domain TaskIdParams.
     * Extracts task ID from the resource name.
     */
    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "id", source = "id")
    @Mapping(target = "metadata", source = "metadata", qualifiedByName = "metadataToProto")
    org.a2aproject.sdk.grpc.CancelTaskRequest toProtoCancelTaskRequest(CancelTaskParams domain);


    /**
     * Converts proto SubscribeToTaskRequest to domain TaskIdParams.
     * Extracts task ID from the resource name.
     */
    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "id", source = "id")
    @Mapping(target = "tenant", source = "tenant")
    TaskIdParams fromProtoSubscribeToTaskRequest(org.a2aproject.sdk.grpc.SubscribeToTaskRequest proto);

    /**
     * Converts domain TaskIdParams to proto SubscribeToTaskRequest.
     * Creates resource name from task ID.
     */
    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "id", source = "id")
    org.a2aproject.sdk.grpc.SubscribeToTaskRequest toProtoSubscribeToTaskRequest(TaskIdParams domain);
}
