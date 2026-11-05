package org.a2aproject.sdk.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class HtmlEscapeUtilsTest {

    @Test
    public void removeHtmlEscaping_restoresAngleBrackets() {
        assertEquals("<event-topic>", HtmlEscapeUtils.removeHtmlEscaping("\\u003cevent-topic\\u003e"));
    }

    @Test
    public void removeHtmlEscaping_restoresAmpersand() {
        assertEquals("foo&bar", HtmlEscapeUtils.removeHtmlEscaping("foo\\u0026bar"));
    }

    @Test
    public void removeHtmlEscaping_restoresEquals() {
        assertEquals("a=b", HtmlEscapeUtils.removeHtmlEscaping("a\\u003db"));
    }

    @Test
    public void removeHtmlEscaping_restoresApostrophe() {
        assertEquals("it's", HtmlEscapeUtils.removeHtmlEscaping("it\\u0027s"));
    }

    @Test
    public void removeHtmlEscaping_handlesMultipleEscapes() {
        assertEquals("<tag>&</tag>",
                HtmlEscapeUtils.removeHtmlEscaping("\\u003ctag\\u003e\\u0026\\u003c/tag\\u003e"));
    }

    @Test
    public void removeHtmlEscaping_leavesRegularJsonUntouched() {
        String json = "{\"key\": \"value\", \"num\": 42}";
        assertEquals(json, HtmlEscapeUtils.removeHtmlEscaping(json));
    }

    @Test
    public void removeHtmlEscaping_handlesEmptyString() {
        assertEquals("", HtmlEscapeUtils.removeHtmlEscaping(""));
    }

    @Test
    public void removeHtmlEscaping_restoresUppercaseAngleBrackets() {
        assertEquals("<event-topic>", HtmlEscapeUtils.removeHtmlEscaping("\\u003Cevent-topic\\u003E"));
    }

    @Test
    public void removeHtmlEscaping_restoresUppercaseEquals() {
        assertEquals("a=b", HtmlEscapeUtils.removeHtmlEscaping("a\\u003Db"));
    }

    @Test
    public void removeHtmlEscaping_handlesMixedCase() {
        assertEquals("<tag>=value", HtmlEscapeUtils.removeHtmlEscaping("\\u003ctag\\u003E\\u003Dvalue"));
    }

    @Test
    public void removeHtmlEscaping_preservesEscapedBackslashes() {
        String[] escapes = {"003c", "003C", "003e", "003E", "0026", "003d", "003D", "0027"};
        String[] characters = {"<", "<", ">", ">", "&", "=", "=", "'"};
        for (int index = 0; index < escapes.length; index++) {
            for (int backslashes = 1; backslashes <= 6; backslashes++) {
                String prefix = "\\".repeat(backslashes);
                String input = prefix + "u" + escapes[index];
                String expected = backslashes % 2 == 0
                        ? input
                        : "\\".repeat(backslashes - 1) + characters[index];
                assertEquals(expected, HtmlEscapeUtils.removeHtmlEscaping(input),
                        "Escape " + escapes[index] + " preceded by " + backslashes + " backslashes");
            }
        }
    }

    @Test
    public void removeHtmlEscaping_preservesOtherEscapesAndIncompleteSequences() {
        String json = "{\"text\":\"\\\" \\\\ \\n \\t \\u0041 \\u2028 \\u2029 \\u003e\"}";
        String expected = "{\"text\":\"\\\" \\\\ \\n \\t \\u0041 \\u2028 \\u2029 >\"}";
        assertEquals(expected, HtmlEscapeUtils.removeHtmlEscaping(json));
        assertEquals("\\u00", HtmlEscapeUtils.removeHtmlEscaping("\\u00"));
        assertEquals("\\u003", HtmlEscapeUtils.removeHtmlEscaping("\\u003"));
        assertEquals(">\\", HtmlEscapeUtils.removeHtmlEscaping("\\u003e\\"));
    }

}
