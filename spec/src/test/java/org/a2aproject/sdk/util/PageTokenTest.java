package org.a2aproject.sdk.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.stream.Stream;

import org.a2aproject.sdk.spec.InvalidParamsError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

/**
 * Unit tests for {@link PageToken}.
 */
class PageTokenTest {

    // ========== Valid Token Parsing Tests ==========

    /**
     * Provides test data for valid token parsing tests.
     * Format: tokenString, expectedTimestampMillis, expectedId
     */
    static Stream<Arguments> validTokens() {
        return Stream.of(
            Arguments.of("1640000000000:user123", 1640000000000L, "user123"),
            Arguments.of("1640000000000:user:123:extra", 1640000000000L, "user:123:extra"),
            Arguments.of("0:user123", 0L, "user123"),
            Arguments.of("-1000:user123", -1000L, "user123"),
            Arguments.of(Long.MAX_VALUE + ":user123", Long.MAX_VALUE, "user123"),
            Arguments.of(Long.MIN_VALUE + ":user123", Long.MIN_VALUE, "user123"),
            Arguments.of("1640000000000:" + "a".repeat(1000), 1640000000000L, "a".repeat(1000)),
            Arguments.of("1640000000000:user-123_test@example.com", 1640000000000L, "user-123_test@example.com")
        );
    }

    /**
     * Verifies that various valid pageToken strings are correctly parsed into PageToken objects.
     */
    @ParameterizedTest
    @MethodSource("validTokens")
    void testFromString_validTokens_parseCorrectly(String tokenStr, long expectedTimestampMillis, String expectedId) {
        PageToken token = PageToken.fromString(tokenStr);

        assertNotNull(token);
        assertEquals(Instant.ofEpochMilli(expectedTimestampMillis), token.timestamp());
        assertEquals(expectedId, token.id());
    }

    // ========== Null and Empty Input Tests ==========

    /**
     * Verifies that null or empty strings return null instead of throwing an exception.
     */
    @ParameterizedTest
    @NullAndEmptySource
    void testFromString_nullOrEmpty_returnsNull(String tokenStr) {
        PageToken token = PageToken.fromString(tokenStr);
        assertNull(token);
    }

    // ========== Invalid Format Tests ==========

    /**
     * Provides test data for invalid token format tests.
     * Format: tokenString, expectedErrorMessage
     */
    static Stream<Arguments> invalidTokenFormats() {
        return Stream.of(
            Arguments.of("1640000000000user123", "Invalid pageToken format: expected 'timestamp:id'"),
            Arguments.of("1640000000000", "Invalid pageToken format: expected 'timestamp:id'"),
            Arguments.of("1640000000000:", "Invalid pageToken format: id part cannot be empty"),
            Arguments.of(":", "Invalid pageToken format: timestamp must be numeric milliseconds"),
            Arguments.of("notanumber:user123", "Invalid pageToken format: timestamp must be numeric milliseconds"),
            Arguments.of("1640000000.123:user123", "Invalid pageToken format: timestamp must be numeric milliseconds"),
            Arguments.of("1640 000 000:user123", "Invalid pageToken format: timestamp must be numeric milliseconds")
        );
    }

    /**
     * Verifies that various invalid token formats throw InvalidParamsError with appropriate messages.
     */
    @ParameterizedTest
    @MethodSource("invalidTokenFormats")
    void testFromString_invalidFormats_throwsInvalidParamsError(String tokenStr, String expectedErrorMessage) {
        InvalidParamsError ex = assertThrows(InvalidParamsError.class, () -> {
            PageToken.fromString(tokenStr);
        });
        assertNotNull(ex.getMessage());
        assertEquals(expectedErrorMessage, ex.getMessage());
    }

    // ========== toString Tests ==========

    /**
     * Provides test data for toString tests.
     * Format: timestampMillis, id, expectedString
     */
    static Stream<Arguments> toStringTestData() {
        return Stream.of(
            Arguments.of(1640000000000L, "user123", "1640000000000:user123"),
            Arguments.of(1640000000000L, "user:123", "1640000000000:user:123"),
            Arguments.of(0L, "user123", "0:user123"),
            Arguments.of(-1000L, "user123", "-1000:user123")
        );
    }

    /**
     * Verifies that toString formats PageTokens correctly as "timestamp_millis:id".
     */
    @ParameterizedTest
    @MethodSource("toStringTestData")
    void testToString_formatsCorrectly(long timestampMillis, String id, String expectedString) {
        PageToken token = new PageToken(Instant.ofEpochMilli(timestampMillis), id);
        String result = token.toString();
        assertEquals(expectedString, result);
    }

    // ========== Round-Trip Conversion Tests ==========

    /**
     * Provides test data for round-trip conversion tests.
     */
    static Stream<String> roundTripTokens() {
        return Stream.of(
            "1640000000000:user123",
            "1640000000000:user:123:extra",
            "0:user123",
            "-1000:user123"
        );
    }

    /**
     * Verifies that converting from string to PageToken and back to string preserves the original value.
     */
    @ParameterizedTest
    @MethodSource("roundTripTokens")
    void testRoundTrip_fromStringToString_preservesValue(String original) {
        PageToken token = PageToken.fromString(original);
        String result = token.toString();
        assertEquals(original, result);
    }

    /**
     * Verifies that converting from PageToken to string and back to PageToken preserves the original value.
     */
    @Test
    void testRoundTrip_toStringFromString_preservesValue() {
        PageToken original = new PageToken(Instant.ofEpochMilli(1640000000000L), "user123");
        String tokenStr = original.toString();
        PageToken result = PageToken.fromString(tokenStr);

        assertEquals(original.timestamp(), result.timestamp());
        assertEquals(original.id(), result.id());
    }
}
