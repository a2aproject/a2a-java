package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An A2A-specific error indicating that the requested operation is not supported by the agent.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UnsupportedOperationError extends JSONRPCError {

    public final static Integer DEFAULT_CODE = -32004;

    @JsonCreator
    public UnsupportedOperationError(
            @JsonProperty("code") Integer code,
            @JsonProperty("message") String message,
            @JsonProperty("data") Object data) {
        super(
                defaultIfNull(code, DEFAULT_CODE),
                defaultIfNull(message, "This operation is not supported"),
                data);
    }

    public UnsupportedOperationError() {
        this(null, null, null);
    }
}
