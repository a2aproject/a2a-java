package io.a2a.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.TransportProtocol;

/**
 * Validates AgentCard transport configuration against available transport endpoints.
 */
public class AgentCardValidator {
    
    private static final Logger LOGGER = Logger.getLogger(AgentCardValidator.class.getName());

    // Properties to turn off validation globally, or per known transport
    public static final String SKIP_PROPERTY = "io.a2a.transport.skipValidation";
    public static final String SKIP_JSONRPC_PROPERTY = "io.a2a.transport.jsonrpc.skipValidation";
    public static final String SKIP_GRPC_PROPERTY = "io.a2a.transport.grpc.skipValidation";
    public static final String SKIP_REST_PROPERTY = "io.a2a.transport.rest.skipValidation";

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
