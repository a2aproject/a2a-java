package org.a2aproject.sdk.grpc.mapper;


import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link org.a2aproject.sdk.spec.AuthenticationInfo} and {@link org.a2aproject.sdk.grpc.AuthenticationInfo}.
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
    org.a2aproject.sdk.grpc.AuthenticationInfo toProto(org.a2aproject.sdk.spec.AuthenticationInfo domain);

    /**
     * Converts proto AuthenticationInfo to domain AuthenticationInfo.
     * Wraps the single scheme in a list.
     */
    org.a2aproject.sdk.spec.AuthenticationInfo fromProto(org.a2aproject.sdk.grpc.AuthenticationInfo proto);


}
