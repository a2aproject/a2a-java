package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An error indicating that the JSON sent is not a valid Request object.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class InvalidRequestError extends JSONRPCError {

    public final static Integer DEFAULT_CODE = -32600;

    public InvalidRequestError() {
        this(null, null, null);
    }

    @JsonCreator
    public InvalidRequestError(
            @JsonProperty("code") Integer code,
            @JsonProperty("message") String message,
            @JsonProperty("data") Object data) {
        super(
                defaultIfNull(code, DEFAULT_CODE),
                defaultIfNull(message, "Request payload validation error"),
                data);
    }

    public InvalidRequestError(String message) {
        this(null, message, null);
    }
}
