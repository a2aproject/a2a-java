package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import static io.a2a.util.Utils.defaultIfNull;

/**
 * An error indicating that the server received invalid JSON.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JSONParseError extends JSONRPCError implements A2AError {

    public final static Integer DEFAULT_CODE = -32700;

    public JSONParseError() {
        this(null, null, null);
    }

    public JSONParseError(String message) {
        this(null, message, null);
    }

    @JsonCreator
    public JSONParseError(
            @JsonProperty("code") Integer code,
            @JsonProperty("message") String message,
            @JsonProperty("data") Object data) {
        super(
                defaultIfNull(code, DEFAULT_CODE),
                defaultIfNull(message, "Invalid JSON payload"),
                data);
    }
}
