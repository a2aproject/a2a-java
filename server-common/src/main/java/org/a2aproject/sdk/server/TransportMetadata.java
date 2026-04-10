package org.a2aproject.sdk.server;


/**
 * Interface for transport endpoint implementations to provide metadata about their transport.
 * This is used by the validation system to discover available transports on the classpath.
 */
public interface TransportMetadata {
    
    /**
     * Returns the transport protocol this endpoint supports.
     * 
     * @return the transport protocol
     */
    String getTransportProtocol();

    /**
     * Checks if this transport endpoint is currently available/functional.
     * This can be used for runtime availability checks beyond just classpath presence.
     *
     * @return true if the transport is available, false otherwise
     */
    default boolean isAvailable() {
        return true;
    }
}