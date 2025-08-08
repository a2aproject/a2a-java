package io.a2a.spec;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.a2a.util.Assert;

/**
 * Represents parameters for querying task information in the A2A protocol.
 * <p>
 * This record encapsulates the parameters needed to query a specific task,
 * including options to control the amount of historical data returned and
 * additional metadata for the query.
 * </p>
 *
 * @param id the unique identifier of the task to be queried
 * @param historyLength the maximum number of historical items to include in the response (null for default)
 * @param metadata additional properties and metadata for the query
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskQueryParams(String id, Integer historyLength, Map<String, Object> metadata) {

    /**
     * Compact constructor that validates the task query parameters.
     * <p>
     * This constructor ensures that the task ID is not null and that
     * the history length, if specified, is not negative.
     * </p>
     *
     * @throws IllegalArgumentException if id is null or historyLength is negative
     */
    public TaskQueryParams {
        Assert.checkNotNullParam("id", id);
        if (historyLength != null && historyLength < 0) {
            throw new IllegalArgumentException("Invalid history length");
        }
    }

    /**
     * Convenience constructor for creating TaskQueryParams with only a task ID.
     *
     * @param id the unique identifier of the task to be queried
     */
    public TaskQueryParams(String id) {
        this(id, null, null);
    }

    /**
     * Convenience constructor for creating TaskQueryParams with ID and history length.
     *
     * @param id the unique identifier of the task to be queried
     * @param historyLength the maximum number of historical items to include in the response
     */
    public TaskQueryParams(String id, Integer historyLength) {
        this(id, historyLength, null);
    }
}
