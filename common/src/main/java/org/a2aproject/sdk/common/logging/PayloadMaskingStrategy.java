package org.a2aproject.sdk.common.logging;

/**
 * Strategy for masking sensitive data in payload logs.
 * <p>
 * Implementations can provide custom logic to redact or replace sensitive fields
 * (e.g., tokens, credentials, URLs) before payloads are written to logs.
 * </p>
 * <p>
 * Users can configure a custom strategy via the system property
 * {@code a2a.logging.payload.masking.strategy} (fully qualified class name)
 * or programmatically via {@link A2APayloadLoggingConfig.Builder}.
 * </p>
 */
public interface PayloadMaskingStrategy {

    /**
     * Masks sensitive data in the given JSON payload string.
     *
     * @param jsonPayload the original JSON payload, may be null
     * @return the masked payload, or original if masking not applicable. Must not return null
     *         if input is non-null; return original string on any error to avoid log loss.
     */
    String mask(String jsonPayload);
}
