package io.a2a.grpc.mapper;


import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link io.a2a.spec.AuthenticationInfo} and {@link io.a2a.grpc.AuthenticationInfo}.
 * <p>
 * Maps between domain AuthenticationInfo (schemes as List of String) and proto AuthenticationInfo (scheme as String).
 * The proto scheme field is mapped to/from the first element of the domain schemes list.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface AuthenticationInfoMapper {

    AuthenticationInfoMapper INSTANCE = A2AMappers.getMapper(AuthenticationInfoMapper.class);

    /**
     * Converts domain AuthenticationInfo to proto AuthenticationInfo.
     * Takes the first scheme from the schemes list.
     */
    @Mapping(target = "credentials", source = "credentials", conditionExpression = "java(domain.credentials() != null)")
    io.a2a.grpc.AuthenticationInfo toProto(io.a2a.spec.AuthenticationInfo domain);

    /**
     * Converts proto AuthenticationInfo to domain AuthenticationInfo.
     * Wraps the single scheme in a list.
     */
    io.a2a.spec.AuthenticationInfo fromProto(io.a2a.grpc.AuthenticationInfo proto);


}
