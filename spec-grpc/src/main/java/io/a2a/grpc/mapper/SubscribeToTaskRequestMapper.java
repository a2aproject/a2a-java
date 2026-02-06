package io.a2a.grpc.mapper;

import io.a2a.jsonrpc.common.wrappers.SubscribeToTaskRequest;
import org.mapstruct.Mapper;

/**
 * Mapper between {@link SubscribeToTaskRequest} and {@link io.a2a.grpc.SubscribeToTaskRequest}.
 * <p>
 * The mapping handles the structural difference between domain and proto representations:
 * <ul>
 * <li>Domain: Full JSONRPC request with id, jsonrpc, method, and params (TaskIdParams)</li>
 * <li>Proto: Simple request with name field in format "tasks/{task_id}"</li>
 * </ul>
 * <p>
 * Note: The domain object is a complete JSONRPC request, while the proto is just the gRPC
 * request parameters. The JSONRPC envelope (id, jsonrpc, method) is handled separately
 * by the transport layer.
 */
@Mapper(config = A2AProtoMapperConfig.class, uses = {TaskIdParamsMapper.class})
public interface SubscribeToTaskRequestMapper {

    SubscribeToTaskRequestMapper INSTANCE = A2AMappers.getMapper(SubscribeToTaskRequestMapper.class);

    /**
     * Converts domain SubscribeToTaskRequest to proto SubscribeToTaskRequest.
     * Extracts the task ID from params and formats it as "tasks/{task_id}".
     *
     * @param domain the domain SubscribeToTaskRequest
     * @return the proto SubscribeToTaskRequest
     */
    default io.a2a.grpc.SubscribeToTaskRequest toProto(SubscribeToTaskRequest domain) {
        if (domain == null || domain.getParams() == null || domain.getParams().id() == null) {
            return null;
        }
        return io.a2a.grpc.SubscribeToTaskRequest.newBuilder()
                .setId(domain.getParams().id())
                .build();
    }

    /**
     * Converts proto SubscribeToTaskRequest to domain SubscribeToTaskRequest.
     * Extracts the task ID from the name field and creates a TaskIdParams.
     *
     * @param proto the proto SubscribeToTaskRequest
     * @return the domain SubscribeToTaskRequest
     */
    default SubscribeToTaskRequest fromProto(io.a2a.grpc.SubscribeToTaskRequest proto) {
        if (proto == null || proto.getId()== null) {
            return null;
        }
        return SubscribeToTaskRequest.builder()
                .params(new io.a2a.spec.TaskIdParams(proto.getId()))
                .build();
    }
}
