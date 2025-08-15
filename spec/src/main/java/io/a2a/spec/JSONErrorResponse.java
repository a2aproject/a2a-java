package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A simple record representing a JSON error response structure.
 * 
 * This record encapsulates an error message in a response format,
 * providing a lightweight wrapper around error information.
 * It's used for cases where only the error message needs to be
 * represented in JSON format.
 * 
 * @param error the error message string containing error details
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record JSONErrorResponse(String error) {
}
