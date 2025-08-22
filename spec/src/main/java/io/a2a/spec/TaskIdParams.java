package io.a2a.spec;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.a2a.util.Assert;

/**
 * Defines parameters containing a task ID, used for simple task operations.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskIdParams(String id, Map<String, Object> metadata) {

    public TaskIdParams {
        Assert.checkNotNullParam("id", id);
    }

    public TaskIdParams(String id) {
        this(id, null);
    }
}
