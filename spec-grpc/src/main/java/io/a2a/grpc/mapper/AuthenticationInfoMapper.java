package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link io.a2a.spec.AuthenticationInfo} and {@link io.a2a.grpc.AuthenticationInfo}.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface AuthenticationInfoMapper {

    AuthenticationInfoMapper INSTANCE = A2AMappers.getMapper(AuthenticationInfoMapper.class);

    @Mapping(target = "credentials", source = "credentials", conditionExpression = "java(domain.credentials() != null)")
    io.a2a.grpc.AuthenticationInfo toProto(io.a2a.spec.AuthenticationInfo domain);

    io.a2a.spec.AuthenticationInfo fromProto(io.a2a.grpc.AuthenticationInfo proto);
}
