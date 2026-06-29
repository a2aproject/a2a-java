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

}
