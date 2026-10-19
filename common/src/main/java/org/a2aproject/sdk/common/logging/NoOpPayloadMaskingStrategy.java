package org.a2aproject.sdk.common.logging;

/**
 * No-op masking strategy that returns the payload unchanged.
 * <p>
 * Use this when sensitive data masking is disabled or when you want to preserve
 * original payloads for debugging in secure environments.
 * </p>
 */
public class NoOpPayloadMaskingStrategy implements PayloadMaskingStrategy {

    @Override
    public String mask(String jsonPayload) {
        return jsonPayload;
    }
}
