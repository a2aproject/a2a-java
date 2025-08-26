package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An A2A-specific error indicating an incompatibility between the requested
 * content types and the agent's capabilities.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentTypeNotSupportedError extends JSONRPCError {

    public final static Integer DEFAULT_CODE = -32005;

    @JsonCreator
    public ContentTypeNotSupportedError(
            @JsonProperty("code") Integer code,
            @JsonProperty("message") String message,
            @JsonProperty("data") Object data) {
        super(
                defaultIfNull(code, DEFAULT_CODE),
                defaultIfNull(message, "Incompatible content types"),
                data);
    }
}
