package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An error indicating that the requested method does not exist or is not available.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MethodNotFoundError extends JSONRPCError {

    public final static Integer DEFAULT_CODE = -32601;

    @JsonCreator
    public MethodNotFoundError(
            @JsonProperty("code") Integer code,
            @JsonProperty("message") String message,
            @JsonProperty("data") Object data) {
        super(
                defaultIfNull(code, DEFAULT_CODE),
                defaultIfNull(message, "Method not found"),
                data);
    }

    public MethodNotFoundError() {
        this(DEFAULT_CODE, null, null);
    }
}
