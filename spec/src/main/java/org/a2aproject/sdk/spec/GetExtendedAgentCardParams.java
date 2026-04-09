package org.a2aproject.sdk.spec;

import org.jspecify.annotations.Nullable;

/**
 * Parameters to get the extended agent card.
 *
 * @param tenant optional tenant, provided as a path parameter.
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record GetExtendedAgentCardParams(@Nullable String tenant) {
}
