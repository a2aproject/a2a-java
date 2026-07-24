package org.a2aproject.sdk.compat03.spec;

import org.a2aproject.sdk.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * Defines a security scheme using an API key.
 */
public record APIKeySecurityScheme_v0_3(String in, String name, @Nullable String description, String type) implements SecurityScheme_v0_3 {

    public static final String TYPE = "apiKey";

    /**
     * Represents the location of the API key.
     */
    public enum Location {
        COOKIE("cookie"),
        HEADER("header"),
        QUERY("query");

        private final String location;

        Location(String location) {
            this.location = location;
        }

        public String asString() {
            return location;
        }

        public static Location fromString(String location) {
            switch (location) {
                case "cookie" -> {
                    return COOKIE;
                }
                case "header" -> {
                    return HEADER;
                }
                case "query" -> {
                    return QUERY;
                }
                default -> throw new IllegalArgumentException("Invalid API key location: " + location);
            }
        }
    }

    public APIKeySecurityScheme_v0_3(String in, String name, @Nullable String description, @Nullable String type) {
        Assert.checkNotNullParam("in", in);
        Assert.checkNotNullParam("name", name);
        if (type != null && !type.equals(TYPE)) {
            throw new IllegalArgumentException("Invalid type for APIKeySecurityScheme");
        }
        this.in = in;
        this.name = name;
        this.description = description;
        this.type = TYPE;
    }

    public APIKeySecurityScheme_v0_3(String in, String name, @Nullable String description) {
        this(in, name, description, TYPE);
    }

    public static class Builder {
        private String in;
        private String name;
        private String description;

        public Builder in(String in) {
            this.in = in;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public APIKeySecurityScheme_v0_3 build() {
            return new APIKeySecurityScheme_v0_3(in, name, description);
        }
    }
}
