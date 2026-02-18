package io.a2a.spec;

import static java.util.Collections.unmodifiableMap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.a2a.util.Assert;

/**
 * Represents a security requirement in the A2A Protocol.
 * <p>
 * A SecurityRequirement defines which security schemes must be satisfied to access an agent or skill.
 * It maps security scheme names to lists of required scopes. When multiple scheme entries are present
 * in a single SecurityRequirement, ALL must be satisfied (AND relationship). When multiple
 * SecurityRequirements are present in a list, any ONE may be satisfied (OR relationship).
 * <p>
 * This class corresponds to the {@code SecurityRequirement} type in the A2A Protocol specification,
 * which contains a {@code schemes} field mapping scheme names to scope arrays.
 * <p>
 * Example usage:
 * <pre>{@code
 * // Single OAuth2 requirement with specific scopes
 * SecurityRequirement oauth2Req = SecurityRequirement.builder()
 *     .scheme("oauth2", List.of("read", "write"))
 *     .build();
 *
 * // API key requirement with no scopes
 * SecurityRequirement apiKeyReq = SecurityRequirement.builder()
 *     .scheme("apiKey", List.of())
 *     .build();
 *
 * // Combined requirement: both OAuth2 AND API key required
 * SecurityRequirement combinedReq = SecurityRequirement.builder()
 *     .scheme("oauth2", List.of("profile"))
 *     .scheme("apiKey", List.of())
 *     .build();
 * }</pre>
 *
 * @param schemes map of security scheme names to lists of required scopes
 * @see SecurityScheme
 * @see AgentCard#securityRequirements()
 * @see AgentSkill#securityRequirements()
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record SecurityRequirement(Map<String, List<String>> schemes) {

    /**
     * Creates a SecurityRequirement with the specified schemes map.
     *
     * @param schemes map of security scheme names to lists of required scopes
     */
    public SecurityRequirement {
        Assert.checkNotNullParam("schemes", schemes);
        schemes = unmodifiableMap(new LinkedHashMap<>(schemes));
    }

    /**
     * Creates a new Builder for constructing SecurityRequirement instances.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing immutable {@link SecurityRequirement} instances.
     * <p>
     * Example usage:
     * <pre>{@code
     * SecurityRequirement requirement = SecurityRequirement.builder()
     *     .scheme("oauth2", List.of("read", "write"))
     *     .scheme("apiKey", List.of())
     *     .build();
     * }</pre>
     */
    public static class Builder {

        private final Map<String, List<String>> schemes = new LinkedHashMap<>();

        /**
         * Creates a new Builder with an empty schemes map.
         */
        private Builder() {
        }

        /**
         * Adds a security scheme with its required scopes.
         *
         * @param schemeName the name of the security scheme (must match a key in securitySchemes)
         * @param scopes the list of required scopes for this scheme (empty list for no specific scopes)
         * @return this builder for method chaining
         */
        public Builder scheme(String schemeName, List<String> scopes) {
            this.schemes.put(schemeName, List.copyOf(scopes));
            return this;
        }

        /**
         * Builds an immutable {@link SecurityRequirement} from the current builder state.
         *
         * @return a new SecurityRequirement instance
         */
        public SecurityRequirement build() {
            return new SecurityRequirement(schemes);
        }
    }
}
