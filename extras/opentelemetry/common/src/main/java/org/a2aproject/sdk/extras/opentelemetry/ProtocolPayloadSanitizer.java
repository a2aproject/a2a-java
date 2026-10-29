package org.a2aproject.sdk.extras.opentelemetry;

/** Additional application-specific redaction for protocol payload strings. */
@FunctionalInterface
public interface ProtocolPayloadSanitizer {
    /** Returns a sanitized value. Throwing an exception omits the entire payload. */
    String sanitize(String value);
}
