package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.a2a.util.Assert;

/**
 * Represents a JSON-RPC 2.0 Error object, included in an error response.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonDeserialize(using = JSONRPCErrorDeserializer.class)
@JsonSerialize(using = JSONRPCErrorSerializer.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JSONRPCError extends Error implements Event, A2AError {

    private final Integer code;
    private final Object data;

    @JsonCreator
    public JSONRPCError(
            @JsonProperty("code") Integer code,
            @JsonProperty("message") String message,
            @JsonProperty("data") Object data) {
        super(message);
        Assert.checkNotNullParam("code", code);
        Assert.checkNotNullParam("message", message);
        this.code = code;
        this.data = data;
    }

    /**
     * Gets the error code
     *
     * @return the error code
     */
    public Integer getCode() {
        return code;
    }

    /**
     * Gets the data associated with the error.
     *
     * @return the data. May be {@code null}
     */
    public Object getData() {
        return data;
    }
}
