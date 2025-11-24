package io.a2a.grpc.mapper;

import io.a2a.spec.TaskIdParams;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Mapper for {@link io.a2a.spec.TaskIdParams} from various gRPC request types.
 * <p>
 * Extracts task ID from resource name format "tasks/{id}" using {@link ResourceNameParser}.
 */
@Mapper(config = ProtoMapperConfig.class)
public interface TaskIdParamsMapper {

    TaskIdParamsMapper INSTANCE = Mappers.getMapper(TaskIdParamsMapper.class);

    /**
     * Converts proto CancelTaskRequest to domain TaskIdParams.
     * Extracts task ID from the resource name.
     */
    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "id", expression = "java(ResourceNameParser.extractTaskId(proto.getName()))")
    @Mapping(target = "metadata", ignore = true)
    TaskIdParams fromProtoCancelTaskRequest(io.a2a.grpc.CancelTaskRequest proto);

    /**
     * Converts proto SubscribeToTaskRequest to domain TaskIdParams.
     * Extracts task ID from the resource name.
     */
    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "id", expression = "java(ResourceNameParser.extractTaskId(proto.getName()))")
    @Mapping(target = "metadata", ignore = true)
    TaskIdParams fromProtoSubscribeToTaskRequest(io.a2a.grpc.SubscribeToTaskRequest proto);
}
