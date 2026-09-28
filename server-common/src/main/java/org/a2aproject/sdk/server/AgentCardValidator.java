package org.a2aproject.sdk.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Instance;

import org.a2aproject.sdk.server.multitenancy.AgentCardRouter;
import org.a2aproject.sdk.server.multitenancy.TenantNotFoundException;
import org.a2aproject.sdk.server.util.CdiUtils;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.jspecify.annotations.Nullable;

/**
 * Validates AgentCard transport configuration against available transport endpoints.
 */
public class AgentCardValidator {

    private static final Logger LOGGER = Logger.getLogger(AgentCardValidator.class.getName());

    static final String NO_AGENT_CARD_MESSAGE =
            "No agent card configured. Provide either a @PublicAgentCard or @ExtendedAgentCard bean.";

    // Properties to turn off validation globally, or per known transport
    public static final String SKIP_PROPERTY = "org.a2aproject.sdk.transport.skipValidation";
    public static final String SKIP_JSONRPC_PROPERTY = "org.a2aproject.sdk.transport.jsonrpc.skipValidation";
    public static final String SKIP_GRPC_PROPERTY = "org.a2aproject.sdk.transport.grpc.skipValidation";
    public static final String SKIP_REST_PROPERTY = "org.a2aproject.sdk.transport.rest.skipValidation";

    /**
     * Creates a new thread-safe set for tracking which {@link AgentCard} instances have already
     * been validated. Each distinct card is validated at most once; subsequent requests for the
     * same card skip validation.
     *
     * @return a concurrent set suitable for use as the {@code validatedCards} parameter
     */
    public static Set<AgentCard> newValidatedCardsSet() {
        return ConcurrentHashMap.newKeySet();
    }

    /**
     * Resolves an {@link AgentCard} from the given {@link Instance} and validates its transport
     * configuration once per distinct card using the default
     * {@link #validateTransportConfiguration} check. On failure the card is removed from the
     * set so validation can be retried on the next call.
     *
     * @param agentCardInstance the CDI instance holding the agent card
     * @param validatedCards set tracking which cards have already been validated
     * @return the resolved agent card
     */
    public static AgentCard resolveAndValidateOnce(Instance<AgentCard> agentCardInstance,
            Set<AgentCard> validatedCards) {
        return resolveAndValidateOnce(agentCardInstance::get, validatedCards,
                AgentCardValidator::validateTransportConfiguration);
    }

    /**
     * Obtains an {@link AgentCard} from the given supplier and applies the provided validator
     * once per distinct card. On failure the card is removed from the set so validation can be
     * retried on the next call.
     *
     * @param agentCardSupplier supplier that produces the agent card
     * @param validatedCards set tracking which cards have already been validated
     * @param validator validation logic to apply on first access
     * @return the resolved agent card
     */
    public static AgentCard resolveAndValidateOnce(Supplier<AgentCard> agentCardSupplier,
            Set<AgentCard> validatedCards,
            Consumer<AgentCard> validator) {
        AgentCard card = agentCardSupplier.get();
        if (validatedCards.add(card)) {
            try {
                validator.accept(card);
            } catch (RuntimeException e) {
                validatedCards.remove(card);
                throw e;
            }
        }
        return card;
    }

    /**
     * Resolves an agent card from the public instance, falling back to the extended instance
     * if the public one is absent. Throws if neither is available.
     *
     * @param publicCard the CDI instance for the {@code @PublicAgentCard}
     * @param extendedCard the CDI instance for the {@code @ExtendedAgentCard}, may be {@code null}
     * @param validatedCards set tracking which cards have already been validated
     * @return the resolved agent card
     * @throws IllegalStateException if neither card is available
     */
    public static AgentCard resolveWithFallback(Instance<AgentCard> publicCard,
            @Nullable Instance<AgentCard> extendedCard,
            Set<AgentCard> validatedCards) {
        return resolveWithFallback(publicCard, extendedCard, null, null, validatedCards);
    }

    /**
     * Convenience overload that uses the default {@link #validateTransportConfiguration} validator.
     *
     * @see #resolveWithFallback(Instance, Instance, AgentCardRouter, String, Set, Consumer)
     */
    public static AgentCard resolveWithFallback(Instance<AgentCard> publicCard,
            @Nullable Instance<AgentCard> extendedCard,
            @Nullable AgentCardRouter agentCardRouter,
            @Nullable String tenant,
            Set<AgentCard> validatedCards) {
        return resolveWithFallback(publicCard, extendedCard, agentCardRouter, tenant, validatedCards,
                AgentCardValidator::validateTransportConfiguration);
    }

    /**
     * Resolves an agent card using one of two resolution strategies depending on whether a
     * tenant-scoped request is being made, and validates using the supplied validator.
     *
     * @param publicCard the CDI instance for the {@code @PublicAgentCard}
     * @param extendedCard the CDI instance for the {@code @ExtendedAgentCard}, may be {@code null}
     * @param agentCardRouter optional router for tenant-specific card resolution
     * @param tenant the tenant identifier, may be {@code null}
     * @param validatedCards set tracking which cards have already been validated
     * @param validator validation logic to apply on first access of each distinct card
     * @return the resolved agent card
     * @throws TenantNotFoundException if a non-blank tenant is specified, a router is available,
     *         but neither a public nor extended card is registered for that tenant
     * @throws IllegalStateException if no card can be resolved (non-tenant-scoped path)
     * @see #resolveTenantScoped(AgentCardRouter, String, Set, Consumer)
     * @see #resolveDefaultWithRouterFallback(Instance, Instance, AgentCardRouter, String, Set, Consumer)
     */
    public static AgentCard resolveWithFallback(Instance<AgentCard> publicCard,
            @Nullable Instance<AgentCard> extendedCard,
            @Nullable AgentCardRouter agentCardRouter,
            @Nullable String tenant,
            Set<AgentCard> validatedCards,
            Consumer<AgentCard> validator) {
        if (tenant != null && !tenant.isBlank() && agentCardRouter != null) {
            return resolveTenantScoped(agentCardRouter, tenant, validatedCards, validator);
        }
        return resolveDefaultWithRouterFallback(publicCard, extendedCard, agentCardRouter, tenant,
                validatedCards, validator);
    }

    /**
     * Tenant-scoped resolution: only the {@link AgentCardRouter} is consulted. CDI default beans
     * are <em>not</em> used as fallbacks — doing so would let the request proceed against the
     * wrong tenant's card.
     * <ol>
     *   <li>Tenant-specific public card via the router</li>
     *   <li>Tenant-specific extended card via the router</li>
     *   <li>{@link TenantNotFoundException} if neither is registered</li>
     * </ol>
     */
    private static AgentCard resolveTenantScoped(AgentCardRouter agentCardRouter, String tenant,
            Set<AgentCard> validatedCards, Consumer<AgentCard> validator) {
        AgentCard routerCard = agentCardRouter.resolvePublicCard(tenant);
        if (routerCard != null) {
            return resolveAndValidateOnce(() -> routerCard, validatedCards, validator);
        }
        AgentCard routerExtCard = agentCardRouter.resolveExtendedCard(tenant);
        if (routerExtCard != null) {
            return resolveAndValidateOnce(() -> routerExtCard, validatedCards, validator);
        }
        throw new TenantNotFoundException(tenant);
    }

    /**
     * Non-tenant-scoped resolution with optional router fallback:
     * <ol>
     *   <li>Unqualified {@code @PublicAgentCard} CDI bean</li>
     *   <li>Router's public card (when no default bean exists)</li>
     *   <li>Unqualified {@code @ExtendedAgentCard} CDI bean</li>
     *   <li>Router's extended card (when no default bean exists)</li>
     *   <li>{@link IllegalStateException} if nothing resolves</li>
     * </ol>
     */
    private static AgentCard resolveDefaultWithRouterFallback(Instance<AgentCard> publicCard,
            @Nullable Instance<AgentCard> extendedCard,
            @Nullable AgentCardRouter agentCardRouter,
            @Nullable String tenant,
            Set<AgentCard> validatedCards,
            Consumer<AgentCard> validator) {
        AgentCard resolved = CdiUtils.resolveDefault(publicCard);
        if (resolved != null) {
            return resolveAndValidateOnce(() -> resolved, validatedCards, validator);
        }
        if (agentCardRouter != null) {
            AgentCard routerCard = agentCardRouter.resolvePublicCard(tenant);
            if (routerCard != null) {
                return resolveAndValidateOnce(() -> routerCard, validatedCards, validator);
            }
        }
        AgentCard extResolved = CdiUtils.resolveDefault(extendedCard);
        if (extResolved != null) {
            return resolveAndValidateOnce(() -> extResolved, validatedCards, validator);
        }
        if (agentCardRouter != null) {
            AgentCard routerExtCard = agentCardRouter.resolveExtendedCard(tenant);
            if (routerExtCard != null) {
                return resolveAndValidateOnce(() -> routerExtCard, validatedCards, validator);
            }
        }
        throw new IllegalStateException(NO_AGENT_CARD_MESSAGE);
    }

    /**
     * Returns the first non-null agent card, preferring the public card.
     * Throws if both are {@code null}.
     *
     * @param publicCard the public agent card, may be {@code null}
     * @param extendedCard the extended agent card, may be {@code null}
     * @return the first non-null card
     * @throws IllegalStateException if both cards are {@code null}
     */
    public static AgentCard requireFirst(@Nullable AgentCard publicCard, @Nullable AgentCard extendedCard) {
        if (publicCard != null) {
            return publicCard;
        }
        if (extendedCard != null) {
            return extendedCard;
        }
        throw new IllegalStateException(NO_AGENT_CARD_MESSAGE);
    }

    /**
     * Validates the transport configuration of an AgentCard against available transports found on the classpath.
     * Logs warnings for missing transports and errors for unsupported transports.
     *
     * @param agentCard the agent card to validate
     */
    public static void validateTransportConfiguration(AgentCard agentCard) {
        validateTransportConfiguration(agentCard, getAvailableTransports());
    }

    /**
     * Validates the transport configuration of an AgentCard against a given set of available transports.
     * This method is package-private for testability.
     *
     * @param agentCard the agent card to validate
     * @param availableTransports the set of available transport protocols
     */
    static void validateTransportConfiguration(AgentCard agentCard, Set<String> availableTransports) {
        boolean skip = Boolean.getBoolean(SKIP_PROPERTY);
        if (skip) {
            return;
        }

        Set<String> agentCardTransports = getAgentCardTransports(agentCard);
        Set<String> filteredAvailableTransports = filterSkippedTransports(availableTransports);
        Set<String> filteredAgentCardTransports = filterSkippedTransports(agentCardTransports);
        
        // Check for missing transports (warn if AgentCard doesn't include all available transports)
        Set<String> missingTransports = filteredAvailableTransports.stream()
                .filter(transport -> !filteredAgentCardTransports.contains(transport))
                .collect(Collectors.toSet());
        
        if (!missingTransports.isEmpty()) {
            LOGGER.warning(String.format(
                "AgentCard does not include all available transports. Missing: %s. " +
                "Available transports: %s. AgentCard transports: %s",
                formatTransports(missingTransports),
                formatTransports(filteredAvailableTransports),
                formatTransports(filteredAgentCardTransports)
            ));
        }
        
        // Check for unsupported transports (error if AgentCard specifies unavailable transports)
        Set<String> unsupportedTransports = filteredAgentCardTransports.stream()
                .filter(transport -> !filteredAvailableTransports.contains(transport))
                .collect(Collectors.toSet());
        
        if (!unsupportedTransports.isEmpty()) {
            String errorMessage = String.format(
                "AgentCard specifies transport interfaces for unavailable transports: %s. " +
                "Available transports: %s. Consider removing these interfaces or adding the required transport dependencies.",
                formatTransports(unsupportedTransports),
                formatTransports(filteredAvailableTransports)
            );
            LOGGER.severe(errorMessage);
            
            // Following the GitHub issue suggestion to use an error instead of warning
            throw new IllegalStateException(errorMessage);
        }

        // Validation no longer needed - supportedInterfaces is now the single source of truth
        // The first entry in supportedInterfaces is the preferred interface
    }
    
    /**
     * Extracts all transport protocols specified in the AgentCard.
     *
     * @param agentCard the agent card to analyze
     * @return set of transport protocols specified in the agent card
     */
    private static Set<String> getAgentCardTransports(AgentCard agentCard) {
        List<String> transportStrings = new ArrayList<>();

        // Get all transports from supportedInterfaces
        if (agentCard.supportedInterfaces() != null) {
            for (AgentInterface agentInterface : agentCard.supportedInterfaces()) {
                if (agentInterface.protocolBinding() != null) {
                    transportStrings.add(agentInterface.protocolBinding());
                }
            }
        }
        
        return new HashSet<>(transportStrings);
    }
    
    /**
     * Formats a set of transport protocols for logging.
     * 
     * @param transports the transport protocols to format
     * @return formatted string representation
     */
    private static String formatTransports(Set<String> transports) {
        return transports.stream()
                .collect(Collectors.joining(", ", "[", "]"));
    }
    
    /**
     * Filters out transports that have been configured to skip validation.
     *
     * @param transports the set of transport protocols to filter
     * @return filtered set with skipped transports removed
     */
    private static Set<String> filterSkippedTransports(Set<String> transports) {
        return transports.stream()
                .filter(transport -> !isTransportSkipped(transport))
                .collect(Collectors.toSet());
    }

    /**
     * Checks if validation should be skipped for a specific transport.
     *
     * @param transport the transport protocol to check
     * @return true if validation should be skipped for this transport
     */
    private static boolean isTransportSkipped(String transport) {
        if (transport.equals(TransportProtocol.JSONRPC.asString())) {
            return Boolean.getBoolean(SKIP_JSONRPC_PROPERTY);
        } else if (transport.equals(TransportProtocol.GRPC.asString())){
            return Boolean.getBoolean(SKIP_GRPC_PROPERTY);
        } else if (transport.equals(TransportProtocol.HTTP_JSON.asString())) {
            return Boolean.getBoolean(SKIP_REST_PROPERTY);
        }
        return false;
    }

    /**
     * Discovers available transport endpoints using ServiceLoader.
     * This searches the classpath for implementations of TransportMetadata.
     * 
     * @return set of available transport protocols
     */
    private static Set<String> getAvailableTransports() {
        return ServiceLoader.load(TransportMetadata.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .filter(TransportMetadata::isAvailable)
                .map(TransportMetadata::getTransportProtocol)
                .collect(Collectors.toSet());
    }
}
