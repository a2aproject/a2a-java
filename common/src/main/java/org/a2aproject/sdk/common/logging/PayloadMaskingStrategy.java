package org.a2aproject.sdk.common.logging;

public interface PayloadMaskingStrategy {
    String MASKED = "***";
    String mask(String payload);
}
