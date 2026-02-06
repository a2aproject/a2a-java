package io.a2a.grpc.mapper;

import io.a2a.spec.TaskQueryParams;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link io.a2a.grpc.GetTaskRequest} and {@link io.a2a.spec.TaskQueryParams}.
 * <p>
 * Extracts task ID from resource name format "tasks/{id}" using {@link ResourceNameParser}.
 */
@Mapper(config = A2AProtoMapperConfig.class)
public interface TaskQueryParamsMapper {

    TaskQueryParamsMapper INSTANCE = A2AMappers.getMapper(TaskQueryParamsMapper.class);

    /**
     * Converts proto GetTaskRequest to domain TaskQueryParams.
     * Extracts task ID from the resource name.
     */
    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "id", source = "id")
    @Mapping(target = "historyLength", source = "historyLength")
    TaskQueryParams fromProto(io.a2a.grpc.GetTaskRequest proto);

    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "id", source = "id")
    @Mapping(target = "historyLength", source = "historyLength")
    @Mapping(target = "tenant", source = "tenant")
    io.a2a.grpc.GetTaskRequest toProto(TaskQueryParams domain);
}
