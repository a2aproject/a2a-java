package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Mapper between {@link io.a2a.spec.OAuth2SecurityScheme} and {@link io.a2a.grpc.OAuth2SecurityScheme}.
 */
@Mapper(config = ProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        uses = {OAuthFlowsMapper.class})
public interface OAuth2SecuritySchemeMapper {

    OAuth2SecuritySchemeMapper INSTANCE = Mappers.getMapper(OAuth2SecuritySchemeMapper.class);

    @Mapping(target = "description", source = "description", conditionExpression = "java(domain.getDescription() != null)")
    @Mapping(target = "oauth2MetadataUrl", source = "oauth2MetadataUrl", conditionExpression = "java(domain.getOauth2MetadataUrl() != null)")
    io.a2a.grpc.OAuth2SecurityScheme toProto(io.a2a.spec.OAuth2SecurityScheme domain);
}
