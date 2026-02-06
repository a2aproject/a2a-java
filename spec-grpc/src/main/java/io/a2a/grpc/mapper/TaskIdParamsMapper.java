package io.a2a.grpc.mapper;

import io.a2a.spec.TaskIdParams;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper for {@link io.a2a.spec.TaskIdParams} from various gRPC request types.
 * <p>
 * Extracts task ID from resource name format "tasks/{id}" using {@link ResourceNameParser}.
 */
@Mapper(config = A2AProtoMapperConfig.class)
public interface TaskIdParamsMapper {

    TaskIdParamsMapper INSTANCE = A2AMappers.getMapper(TaskIdParamsMapper.class);

    /**
     * Converts proto CancelTaskRequest to domain TaskIdParams.
     * Extracts task ID from the resource name.
     */
    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "id", source = "id")
    TaskIdParams fromProtoCancelTaskRequest(io.a2a.grpc.CancelTaskRequest proto);
    
     /**
     * Converts proto CancelTaskRequest to domain TaskIdParams.
     * Extracts task ID from the resource name.
     */
    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "id", source = "id")
    io.a2a.grpc.CancelTaskRequest toProtoCancelTaskRequest(TaskIdParams domain);


    /**
     * Converts proto SubscribeToTaskRequest to domain TaskIdParams.
     * Extracts task ID from the resource name.
     */
    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "id", source = "id")
    @Mapping(target = "tenant", source = "tenant")
    TaskIdParams fromProtoSubscribeToTaskRequest(io.a2a.grpc.SubscribeToTaskRequest proto);

    /**
     * Converts domain TaskIdParams to proto SubscribeToTaskRequest.
     * Creates resource name from task ID.
     */
    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "id", source = "id")
    io.a2a.grpc.SubscribeToTaskRequest toProtoSubscribeToTaskRequest(TaskIdParams domain);
}
