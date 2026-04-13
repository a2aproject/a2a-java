package org.a2aproject.sdk.compat03.conversion.mappers.params;

import org.a2aproject.sdk.compat03.conversion.mappers.config.A03Mappers;
import org.a2aproject.sdk.compat03.conversion.mappers.config.A03ToV10MapperConfig;
import org.a2aproject.sdk.compat03.conversion.mappers.domain.MessageMapper;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.mapstruct.Mapper;

/**
 * Bidirectional mapper for converting MessageSendParams between A2A Protocol v0.3 and v1.0.
 * <p>
 * Key differences:
 * <ul>
 *   <li>v0.3: {@code MessageSendParams(message, configuration, metadata)} - no tenant field</li>
 *   <li>v1.0: {@code MessageSendParams(message, configuration, metadata, tenant)} - adds tenant field</li>
 * </ul>
 * <p>
 * Conversion strategy:
 * <ul>
 *   <li>v0.3 → v1.0: Add tenant field with default value ""</li>
 *   <li>v1.0 → v0.3: Drop tenant field</li>
 * </ul>
 * <p>
 * Uses {@link MessageMapper} and {@link MessageSendConfigurationMapper} for nested conversions.
 */
@Mapper(config = A03ToV10MapperConfig.class, uses = {MessageMapper.class, MessageSendConfigurationMapper.class})
public interface MessageSendParamsMapper {

    /**
     * Singleton instance accessed via {@link A03Mappers} factory.
     */
    MessageSendParamsMapper INSTANCE = A03Mappers.getMapper(MessageSendParamsMapper.class);

    /**
     * Converts v0.3 MessageSendParams to v1.0 MessageSendParams.
     * <p>
     * Adds the tenant field with default value "".
     *
     * @param v03 the v0.3 message send params
     * @return the equivalent v1.0 message send params
     */
    default MessageSendParams toV10(org.a2aproject.sdk.compat03.spec.MessageSendParams v03) {
        if (v03 == null) {
            return null;
        }

        return new MessageSendParams(
            MessageMapper.INSTANCE.toV10(v03.message()),
            MessageSendConfigurationMapper.INSTANCE.toV10(v03.configuration()),
            v03.metadata(),
            ""  // Default tenant
        );
    }

    /**
     * Converts v1.0 MessageSendParams to v0.3 MessageSendParams.
     * <p>
     * Drops the tenant field.
     *
     * @param v10 the v1.0 message send params
     * @return the equivalent v0.3 message send params
     */
    default org.a2aproject.sdk.compat03.spec.MessageSendParams fromV10(MessageSendParams v10) {
        if (v10 == null) {
            return null;
        }

        return new org.a2aproject.sdk.compat03.spec.MessageSendParams(
            MessageMapper.INSTANCE.fromV10(v10.message()),
            MessageSendConfigurationMapper.INSTANCE.fromV10(v10.configuration()),
            v10.metadata()
        );
    }
}
