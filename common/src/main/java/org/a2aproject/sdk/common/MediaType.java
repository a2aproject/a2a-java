package org.a2aproject.sdk.common;

/** Common MIME type constants. */
public interface MediaType {

    /** MIME type for JSON content: {@code application/json}. */
    String APPLICATION_JSON = "application/json";

    /** MIME type accepted from HTTP+JSON clients alongside {@code application/json}: {@code application/a2a+json}. */
    String APPLICATION_A2A_JSON = "application/a2a+json";
}
