package org.a2aproject.sdk.compat03.spec;

import java.util.Map;

/**
 * A fundamental unit with a Message or Artifact.
 * @param <T> the type of unit
 */
public abstract class Part_v0_3<T> {
    public enum Kind {
        TEXT("text"),
        FILE("file"),
        DATA("data");

        private final String kind;

        Kind(String kind) {
            this.kind = kind;
        }

        /**
         * Returns the string representation of the kind for JSON serialization.
         *
         * @return the kind as a string
         */
        public String asString() {
            return this.kind;
        }
    }

    public abstract Kind getKind();

    public abstract Map<String, Object> getMetadata();

}