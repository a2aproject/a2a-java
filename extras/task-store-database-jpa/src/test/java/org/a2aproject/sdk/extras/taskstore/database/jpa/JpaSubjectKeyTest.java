package org.a2aproject.sdk.extras.taskstore.database.jpa;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class JpaSubjectKeyTest {
    @Test
    void exactEncoding() {
        assertArrayEquals(new byte[] {0x41, 0x6c, 0x69, 0x63, 0x65}, JpaSubjectKey.encode("Alice"));
        for (String[] pair :
                new String[][] {
                    {"Alice", "alice"}, {"Jose", "José"}, {"é", "e\u0301"}, {"alice", "alice "}
                }) {
            assertFalse(
                    Arrays.equals(JpaSubjectKey.encode(pair[0]), JpaSubjectKey.encode(pair[1])));
        }
        assertArrayEquals("😀".getBytes(StandardCharsets.UTF_8), JpaSubjectKey.encode("😀"));
    }

    @Test
    void rejectsMalformedAndOversizedWithoutTruncation() {
        assertThrows(IllegalArgumentException.class, () -> JpaSubjectKey.encode("\ud800"));
        assertThrows(IllegalArgumentException.class, () -> JpaSubjectKey.encode("\udc00"));
        assertEquals(1024, JpaSubjectKey.encode("é".repeat(512)).length);
        assertThrows(IllegalArgumentException.class, () -> JpaSubjectKey.encode("é".repeat(513)));
    }
}
