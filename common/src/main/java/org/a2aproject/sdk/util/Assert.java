package org.a2aproject.sdk.util;

import org.jspecify.annotations.Nullable;

/** Parameter validation utilities. */
public final class Assert {

    private Assert() {
    }

    /**
     * Check that the named parameter is not {@code null}.  Use a standard exception message if it is.
     *
     * @param name the parameter name
     * @param value the parameter value
     * @param <T> the value type
     * @return the value that was passed in
     * @throws IllegalArgumentException if the value is {@code null}
     */
    public static <T> @NotNull T checkNotNullParam(String name, @Nullable T value) throws IllegalArgumentException {
        checkNotNullParamChecked("name", name);
        if (value == null) {
            throw new IllegalArgumentException("Parameter '" + name + "' may not be null");
        }
        return value;
    }

    private static <T> void checkNotNullParamChecked(final String name, final @Nullable T value) {
        if (value == null) {
            throw new IllegalArgumentException("Parameter '" + name + "' may not be null");
        }
    }

    /**
     * Validates that the given value is a legal JSON-RPC id ({@code null}, {@code String}, or {@code Number}).
     *
     * @param value the id value to validate
     * @throws IllegalArgumentException if the value is not a valid JSON-RPC id type
     */
    public static void isValidJsonRpcId(@Nullable Object value) {
        if (! (value == null || value instanceof String || value instanceof Number)) {
            throw new IllegalArgumentException("JSON-RPC id must be null, a String, or a Number");
        }
    }

}
