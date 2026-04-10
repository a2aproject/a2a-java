package org.a2aproject.sdk.util;

import java.time.Instant;

import org.a2aproject.sdk.spec.InvalidParamsError;
import org.jspecify.annotations.Nullable;

/**
 * Represents a pagination token for keyset-based pagination.
 * <p>
 * PageTokens use the format {@code "timestamp_millis:id"} where:
 * <ul>
 *   <li>{@code timestamp_millis} - Unix timestamp in milliseconds (numeric)</li>
 *   <li>{@code id} - The entity identifier (String)</li>
 * </ul>
 * This format enables efficient keyset pagination by allowing queries to resume
 * at a specific point in a timestamp-sorted, ID-secondary-sorted result set.
 *
 * @param timestamp The timestamp component of the page token
 * @param id The identifier component of the page token
 */
public record PageToken(Instant timestamp, String id) {

    /**
     * Parses a pageToken string into a PageToken record.
     * <p>
     * Expected format: {@code "timestamp_millis:id"}
     *
     * @param tokenStr The pageToken string to parse, may be null or empty
     * @return A PageToken instance, or null if tokenStr is null or empty
     * @throws InvalidParamsError if the token format is invalid or timestamp is not numeric
     */
    public static @Nullable PageToken fromString(@Nullable String tokenStr) {
        if (tokenStr == null || tokenStr.isEmpty()) {
            return null;
        }

        String[] tokenParts = tokenStr.split(":", 2);
        if (tokenParts.length != 2) {
            throw new InvalidParamsError(null,
                "Invalid pageToken format: expected 'timestamp:id'", null);
        }

        try {
            long timestampMillis = Long.parseLong(tokenParts[0]);
            String id = tokenParts[1];
            if (id.isEmpty()) {
                throw new InvalidParamsError(null, "Invalid pageToken format: id part cannot be empty", null);
            }
            return new PageToken(Instant.ofEpochMilli(timestampMillis), id);
        } catch (NumberFormatException e) {
            throw new InvalidParamsError(null,
                "Invalid pageToken format: timestamp must be numeric milliseconds", null);
        }
    }

    /**
     * Converts this PageToken to its string representation.
     * <p>
     * Format: {@code "timestamp_millis:id"}
     *
     * @return The pageToken string
     */
    @Override
    public String toString() {
        return timestamp.toEpochMilli() + ":" + id;
    }
}
