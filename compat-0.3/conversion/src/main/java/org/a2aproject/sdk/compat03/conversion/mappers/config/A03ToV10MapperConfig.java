package org.a2aproject.sdk.compat03.conversion.mappers.config;

import org.mapstruct.MapperConfig;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * Global MapStruct configuration for converting between A2A Protocol v0.3 and v1.0 types.
 * <p>
 * This configuration interface provides shared settings and default conversion methods
 * used by all mappers in the compat-0.3 conversion layer. It ensures consistent handling
 * of unmapped fields, null values, and component instantiation across the codebase.
 * <p>
 * Key Configuration:
 * <ul>
 *   <li><b>unmappedTargetPolicy = ERROR:</b> Compile-time validation ensures no fields are missed</li>
 *   <li><b>componentModel = "default":</b> Uses singleton pattern via {@link A2AMappers_v0_3} factory</li>
 *   <li><b>nullValuePropertyMappingStrategy = IGNORE:</b> Skip null source properties during mapping</li>
 * </ul>
 *
 * @see A2AMappers_v0_3
 */
@MapperConfig(
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    componentModel = "default",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface A03ToV10MapperConfig {

    /**
     * Default tenant value for conversions from 0.3 to 1.0.
     * <p>
     * The 1.0 protocol adds a tenant field that doesn't exist in 0.3.
     * When converting from 0.3 to 1.0, we use an empty string as the default tenant,
     * matching the 1.0 MessageSendParams convenience constructor default.
     *
     * @return empty string as default tenant
     */
    default String tenantDefault() {
        return "";
    }
}
