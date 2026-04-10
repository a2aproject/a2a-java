package org.a2aproject.sdk.grpc.mapper;

import org.a2aproject.sdk.spec.TaskQueryParams;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link org.a2aproject.sdk.grpc.GetTaskRequest} and {@link org.a2aproject.sdk.spec.TaskQueryParams}.
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
    TaskQueryParams fromProto(org.a2aproject.sdk.grpc.GetTaskRequest proto);

    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "id", source = "id")
    @Mapping(target = "historyLength", source = "historyLength")
    @Mapping(target = "tenant", source = "tenant")
    org.a2aproject.sdk.grpc.GetTaskRequest toProto(TaskQueryParams domain);
}
