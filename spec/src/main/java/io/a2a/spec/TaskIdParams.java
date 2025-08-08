package io.a2a.spec;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.a2a.util.Assert;

/**
 * Represents task identifier parameters in the A2A protocol.
 * <p>
 * This record encapsulates a task ID along with optional metadata.
 * It is commonly used in API requests and responses that need to
 * reference a specific task, such as task queries, updates, or
 * status requests.
 * </p>
 *
 * @param id the unique identifier of the task
 * @param metadata optional metadata associated with the task identifier
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskIdParams(String id, Map<String, Object> metadata) {

    /**
     * Compact constructor that validates the task ID parameters.
     * <p>
     * This constructor ensures that the task ID is not null, as it is
     * required to uniquely identify the task.
     * </p>
     *
     * @throws IllegalArgumentException if id is null
     */
    public TaskIdParams {
        Assert.checkNotNullParam("id", id);
    }

    /**
     * Convenience constructor for creating TaskIdParams with only an ID.
     *
     * @param id the unique identifier of the task
     */
    public TaskIdParams(String id) {
        this(id, null);
    }
}
