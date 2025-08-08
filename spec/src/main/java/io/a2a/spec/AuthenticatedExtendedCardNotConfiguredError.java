package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An A2A-specific error indicating that the agent does not have an
 * Authenticated Extended Card configured
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthenticatedExtendedCardNotConfiguredError extends JSONRPCError {

    public final static Integer DEFAULT_CODE = -32007;

    @JsonCreator
    public AuthenticatedExtendedCardNotConfiguredError(
            @JsonProperty("code") Integer code,
            @JsonProperty("message") String message,
            @JsonProperty("data") Object data) {
        super(
                defaultIfNull(code, DEFAULT_CODE),
                defaultIfNull(message, "Authenticated Extended Card not configured"),
                data);
    }

    public AuthenticatedExtendedCardNotConfiguredError() {
        this(null, null, null);
    }

}
