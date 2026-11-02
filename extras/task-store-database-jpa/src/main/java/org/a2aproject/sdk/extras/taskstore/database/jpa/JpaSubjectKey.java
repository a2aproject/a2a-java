package org.a2aproject.sdk.extras.taskstore.database.jpa;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/** Exact UTF-8 subject encoding; never normalizes or truncates identity. */
public final class JpaSubjectKey {
    private JpaSubjectKey() {}

    public static byte[] encode(String subject) {
        try {
            ByteBuffer encoded =
                    StandardCharsets.UTF_8
                            .newEncoder()
                            .onMalformedInput(CodingErrorAction.REPORT)
                            .onUnmappableCharacter(CodingErrorAction.REPORT)
                            .encode(CharBuffer.wrap(subject));
            if (encoded.remaining() > 1024) {
                throw new IllegalArgumentException("Subject identity exceeds 1024 UTF-8 bytes");
            }
            byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            return bytes;
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("Malformed subject identity", e);
        }
    }
}
