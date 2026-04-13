package org.a2aproject.sdk.compat03.conversion.mappers.domain;

import java.util.List;

import org.a2aproject.sdk.compat03.conversion.mappers.config.A03Mappers;
import org.a2aproject.sdk.compat03.conversion.mappers.config.A03ToV10MapperConfig;
import org.a2aproject.sdk.spec.AuthenticationInfo;
import org.mapstruct.Mapper;

/**
 * Bidirectional mapper for converting AuthenticationInfo between A2A Protocol v0.3 and v1.0.
 * <p>
 * Key differences:
 * <ul>
 *   <li>v0.3: {@code AuthenticationInfo(List<String> schemes, String credentials)} - supports multiple schemes</li>
 *   <li>v1.0: {@code AuthenticationInfo(String scheme, String credentials)} - single scheme only</li>
 * </ul>
 * <p>
 * Conversion strategy:
 * <ul>
 *   <li>v0.3 → v1.0: Takes the first scheme from the list (or empty string if list is empty)</li>
 *   <li>v1.0 → v0.3: Wraps the single scheme in a list</li>
 * </ul>
 * <p>
 * Note: v0.3 also has {@code PushNotificationAuthenticationInfo} which has the same structure
 * as v0.3 {@code AuthenticationInfo}, so this mapper handles both.
 */
@Mapper(config = A03ToV10MapperConfig.class)
public interface AuthenticationInfoMapper {

    /**
     * Singleton instance accessed via {@link A03Mappers} factory.
     */
    AuthenticationInfoMapper INSTANCE = A03Mappers.getMapper(AuthenticationInfoMapper.class);

    /**
     * Converts v0.3 AuthenticationInfo to v1.0 AuthenticationInfo.
     * <p>
     * Takes the first scheme from the v0.3 schemes list. If the list is empty or null,
     * uses an empty string for the v1.0 scheme.
     *
     * @param v03 the v0.3 authentication info
     * @return the equivalent v1.0 authentication info
     */
    default AuthenticationInfo toV10(org.a2aproject.sdk.compat03.spec.AuthenticationInfo v03) {
        if (v03 == null) {
            return null;
        }

        String scheme = (v03.schemes() != null && !v03.schemes().isEmpty())
            ? v03.schemes().get(0)
            : "";

        return new AuthenticationInfo(scheme, v03.credentials());
    }

    /**
     * Converts v0.3 PushNotificationAuthenticationInfo to v1.0 AuthenticationInfo.
     * <p>
     * Takes the first scheme from the v0.3 schemes list. If the list is empty or null,
     * uses an empty string for the v1.0 scheme.
     *
     * @param v03 the v0.3 push notification authentication info
     * @return the equivalent v1.0 authentication info
     */
    default AuthenticationInfo toV10FromPushNotification(
            org.a2aproject.sdk.compat03.spec.PushNotificationAuthenticationInfo v03) {
        if (v03 == null) {
            return null;
        }

        String scheme = (v03.schemes() != null && !v03.schemes().isEmpty())
            ? v03.schemes().get(0)
            : "";

        return new AuthenticationInfo(scheme, v03.credentials());
    }

    /**
     * Converts v1.0 AuthenticationInfo to v0.3 AuthenticationInfo.
     * <p>
     * Wraps the v1.0 single scheme in a list for v0.3.
     *
     * @param v10 the v1.0 authentication info
     * @return the equivalent v0.3 authentication info
     */
    default org.a2aproject.sdk.compat03.spec.AuthenticationInfo fromV10(AuthenticationInfo v10) {
        if (v10 == null) {
            return null;
        }

        return new org.a2aproject.sdk.compat03.spec.AuthenticationInfo(
            List.of(v10.scheme()),
            v10.credentials()
        );
    }

    /**
     * Converts v1.0 AuthenticationInfo to v0.3 PushNotificationAuthenticationInfo.
     * <p>
     * Wraps the v1.0 single scheme in a list for v0.3.
     *
     * @param v10 the v1.0 authentication info
     * @return the equivalent v0.3 push notification authentication info
     */
    default org.a2aproject.sdk.compat03.spec.PushNotificationAuthenticationInfo fromV10ToPushNotification(
            AuthenticationInfo v10) {
        if (v10 == null) {
            return null;
        }

        return new org.a2aproject.sdk.compat03.spec.PushNotificationAuthenticationInfo(
            List.of(v10.scheme()),
            v10.credentials()
        );
    }
}
