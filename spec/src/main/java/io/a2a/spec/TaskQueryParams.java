package io.a2a.spec;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.a2a.util.Assert;

/**
 * Defines parameters for querying a task, with an option to limit history length.
 *
 * @param id the ID for the task to be queried
 * @param historyLength the maximum number of items of history for the task to include in the response
 * @param metadata additional properties
 */

@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskQueryParams(String id, Integer historyLength, Map<String, Object> metadata) {

    public TaskQueryParams {
        Assert.checkNotNullParam("id", id);
        if (historyLength != null && historyLength < 0) {
            throw new IllegalArgumentException("Invalid history length");
        }
    }

    public TaskQueryParams(String id) {
        this(id, null, null);
    }

    public TaskQueryParams(String id, Integer historyLength) {
        this(id, historyLength, null);
    }
}
