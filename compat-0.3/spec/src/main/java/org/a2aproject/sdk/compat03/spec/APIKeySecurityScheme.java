package org.a2aproject.sdk.compat03.spec;

import org.a2aproject.sdk.util.Assert;

import static org.a2aproject.sdk.compat03.spec.APIKeySecurityScheme.API_KEY;

/**
 * Defines a security scheme using an API key.
 */
public final class APIKeySecurityScheme implements SecurityScheme {

    public static final String API_KEY = "apiKey";
    private final String in;
    private final String name;
    private final String type;
    private final String description;

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

    public APIKeySecurityScheme(String in, String name, String description) {
        this(in, name, description, API_KEY);
    }

    public APIKeySecurityScheme(String in, String name,
                                String description, String type) {
        Assert.checkNotNullParam("in", in);
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("type", type);
        if (! type.equals(API_KEY)) {
            throw new IllegalArgumentException("Invalid type for APIKeySecurityScheme");
        }
        this.in = in;
        this.name = name;
        this.description = description;
        this.type = type;
    }

    @Override
    public String getDescription() {
        return description;
    }


    public String getIn() {
        return in;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
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

        public APIKeySecurityScheme build() {
            return new APIKeySecurityScheme(in, name, description);
        }
    }
}
