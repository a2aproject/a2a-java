package org.a2aproject.sdk.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link org.a2aproject.sdk.spec.OAuth2SecurityScheme} and {@link org.a2aproject.sdk.grpc.OAuth2SecurityScheme}.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        uses = {OAuthFlowsMapper.class})
public interface OAuth2SecuritySchemeMapper {

    OAuth2SecuritySchemeMapper INSTANCE = A2AMappers.getMapper(OAuth2SecuritySchemeMapper.class);

    @Mapping(target = "description", source = "description", conditionExpression = "java(domain.description() != null)")
    @Mapping(target = "oauth2MetadataUrl", source = "oauth2MetadataUrl", conditionExpression = "java(domain.oauth2MetadataUrl() != null)")
    org.a2aproject.sdk.grpc.OAuth2SecurityScheme toProto(org.a2aproject.sdk.spec.OAuth2SecurityScheme domain);

    default org.a2aproject.sdk.spec.OAuth2SecurityScheme fromProto(org.a2aproject.sdk.grpc.OAuth2SecurityScheme proto) {
        if (proto == null) {
            return null;
        }

        org.a2aproject.sdk.spec.OAuthFlows flows = OAuthFlowsMapper.INSTANCE.fromProto(proto.getFlows());
        String description = proto.getDescription().isEmpty() ? null : proto.getDescription();
        String oauth2MetadataUrl = proto.getOauth2MetadataUrl().isEmpty() ? null : proto.getOauth2MetadataUrl();

        return new org.a2aproject.sdk.spec.OAuth2SecurityScheme(flows, description, oauth2MetadataUrl);
    }
}
