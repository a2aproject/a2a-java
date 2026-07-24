package org.a2aproject.sdk.compat03.spec;

/**
 * A fundamental unit with a Message or Artifact.
 * @param <T> the type of unit
 */
public sealed interface Part_v0_3<T> permits TextPart_v0_3, FilePart_v0_3, DataPart_v0_3 {
    enum Kind {
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
}
