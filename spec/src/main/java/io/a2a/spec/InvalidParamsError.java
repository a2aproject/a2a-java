package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An error indicating that the method parameters are invalid.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class InvalidParamsError extends JSONRPCError {

    public final static Integer DEFAULT_CODE = -32602;

    @JsonCreator
    public InvalidParamsError(
            @JsonProperty("code") Integer code,
            @JsonProperty("message") String message,
            @JsonProperty("data") Object data) {
        super(
                defaultIfNull(code, DEFAULT_CODE),
                defaultIfNull(message, "Invalid parameters"),
                data);
    }

    public InvalidParamsError(String message) {
        this(null, message, null);
    }

    public InvalidParamsError() {
        this(null, null, null);
    }
}
