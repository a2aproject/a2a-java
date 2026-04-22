package io.a2a.util;

public final class Assert {

    /**
     * Check that the named parameter is not {@code null}.  Use a standard exception message if it is.
     *
     * @param name the parameter name
     * @param value the parameter value
     * @param <T> the value type
     * @return the value that was passed in
     * @throws IllegalArgumentException if the value is {@code null}
     */
    @NotNull
    public static <T> T checkNotNullParam(String name, T value) throws IllegalArgumentException {
        checkNotNullParamChecked("name", name);
        checkNotNullParamChecked(name, value);
        return value;
    }

    private static <T> void checkNotNullParamChecked(final String name, final T value) {
        if (value == null) throw new IllegalArgumentException("Parameter '" + name + "' may not be null");
    }

    /**
     * Validates that a value is a valid JSON-RPC ID (null, String, Integer, or Long).
     *
     * @param value the value to validate
     * @throws IllegalArgumentException if the value is not a valid JSON-RPC ID type
     */
    public static void isValidJsonRpcId(Object value) {
        if (! (value == null || value instanceof String || value instanceof Integer || value instanceof Long)) {
            throw new IllegalArgumentException("Id must be null, a String, an Integer, or a Long");
        }
    }

}
