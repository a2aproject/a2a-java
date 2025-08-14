package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import static io.a2a.util.Utils.defaultIfNull;

/**
 * An A2A-specific error indicating that the requested task ID was not found.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskNotFoundError extends JSONRPCError {

    public final static Integer DEFAULT_CODE = -32001;

    public TaskNotFoundError() {
        this(null, null, null);
    }

    @JsonCreator

    public TaskNotFoundError(
            @JsonProperty("code") Integer code,
            @JsonProperty("message") String message,
            @JsonProperty("data") Object data) {
        super(
                defaultIfNull(code, DEFAULT_CODE),
                defaultIfNull(message, "Task not found"),
                data);
    }
}
