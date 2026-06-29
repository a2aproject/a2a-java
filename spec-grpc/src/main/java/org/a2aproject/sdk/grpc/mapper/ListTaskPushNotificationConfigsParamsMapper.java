package org.a2aproject.sdk.grpc.mapper;

import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsParams;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsRequest} and {@link org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsParams}.
 */
@Mapper(config = A2AProtoMapperConfig.class, uses = A2ACommonFieldMapper.class)
public interface ListTaskPushNotificationConfigsParamsMapper {

    ListTaskPushNotificationConfigsParamsMapper INSTANCE = A2AMappers.getMapper(ListTaskPushNotificationConfigsParamsMapper.class);

    /**
     * Converts proto ListTaskPushNotificationConfigsRequest to domain ListTaskPushNotificationConfigsParams.
     */
    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "id", source = "taskId")
    @Mapping(target = "tenant", source = "tenant", qualifiedByName = "emptyToNull")
    ListTaskPushNotificationConfigsParams fromProto(org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsRequest proto);

    /**
     * Converts domain ListTaskPushNotificationConfigsParams to proto ListTaskPushNotificationConfigsRequest.
     */
    @Mapping(target = "taskId", source = "id")
    @Mapping(target = "tenant", source = "tenant", conditionExpression = "java(domain.tenant() != null)")
    org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsRequest toProto(ListTaskPushNotificationConfigsParams domain);
}
