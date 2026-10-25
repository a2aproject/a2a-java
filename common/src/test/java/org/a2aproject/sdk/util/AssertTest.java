package org.a2aproject.sdk.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class AssertTest {

    @Test
    public void testIsNullOrEmptyForNull() {
        assertTrue(Assert.isNullOrEmpty(null));
    }

    @Test
    public void testIsNullOrEmptyForEmptyString() {
        assertTrue(Assert.isNullOrEmpty(""));
    }

    @Test
    public void testIsNullOrEmptyForOrdinaryString() {
        assertFalse(Assert.isNullOrEmpty("value"));
    }

    @Test
    public void testIsNullOrEmptyForWhitespaceString() {
        assertTrue(Assert.isNullOrEmpty(" "));
    }
}
