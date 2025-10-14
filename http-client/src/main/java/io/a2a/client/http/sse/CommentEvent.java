package io.a2a.client.http.sse;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents an SSE Comment
 * This is a line starting with a colon (:)
 */
public class CommentEvent extends Event {

    private final String comment;

    @Override
    Type getType() {
        return Type.COMMENT;
    }

    public CommentEvent(String comment) {
        this.comment = comment;
    }

    /**
     *
     * @return the contents of the last line starting with `:` (omitting the colon)
     */
    public String getComment() {
        return comment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CommentEvent that = (CommentEvent) o;
        return Objects.equals(comment, that.comment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(comment);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CommentEvent.class.getSimpleName() + "[", "]")
                .add("comment='" + comment + "'")
                .toString();
    }
}
