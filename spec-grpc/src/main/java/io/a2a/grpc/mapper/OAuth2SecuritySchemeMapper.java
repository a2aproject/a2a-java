package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link io.a2a.spec.OAuth2SecurityScheme} and {@link io.a2a.grpc.OAuth2SecurityScheme}.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        uses = {OAuthFlowsMapper.class})
public interface OAuth2SecuritySchemeMapper {

    OAuth2SecuritySchemeMapper INSTANCE = A2AMappers.getMapper(OAuth2SecuritySchemeMapper.class);

    @Mapping(target = "description", source = "description", conditionExpression = "java(domain.getDescription() != null)")
    @Mapping(target = "oauth2MetadataUrl", source = "oauth2MetadataUrl", conditionExpression = "java(domain.getOauth2MetadataUrl() != null)")
    io.a2a.grpc.OAuth2SecurityScheme toProto(io.a2a.spec.OAuth2SecurityScheme domain);
}
