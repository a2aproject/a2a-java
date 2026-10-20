package org.a2aproject.sdk.common.logging;

public class NoOpPayloadMaskingStrategy implements PayloadMaskingStrategy {
    @Override public String mask(String payload) { return payload; }
}
