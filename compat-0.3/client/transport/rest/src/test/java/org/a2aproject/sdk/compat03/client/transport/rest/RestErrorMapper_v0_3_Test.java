package org.a2aproject.sdk.compat03.client.transport.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.a2aproject.sdk.client.http.A2AHttpHeaders;
import org.a2aproject.sdk.client.http.A2AHttpResponse;
import org.a2aproject.sdk.compat03.spec.A2AClientHTTPError_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskNotFoundError_v0_3;
import org.junit.jupiter.api.Test;

class RestErrorMapper_v0_3_Test {
    @Test
    void preservesStatusRawBodyAndHeadersForUnrecognizedResponses() {
        for (String body : List.of("", "<html>rate limited</html>", "null", "[]", "{\"message\":\"rate limited\"}",
                "{\"error\":\"UNKNOWN\",\"message\":\"rate limited\"}")) {
            var exception = RestErrorMapper_v0_3.mapRestError(response(429, body,
                    Map.of("Retry-After", List.of("30"), "X-Request-Id", List.of("request"))));
            var cause = assertInstanceOf(A2AClientHTTPError_v0_3.class, exception.getCause());
            assertEquals(429, cause.getCode());
            assertEquals(body, cause.getResponseBody());
            assertEquals(List.of("30"), cause.getResponseHeaders().get("retry-after"));
            assertTrue(exception.getMessage().contains("429"));
        }
        var unauthorized = RestErrorMapper_v0_3.mapRestError(response(401, "",
                Map.of("WWW-Authenticate", List.of("Bearer"))));
        var cause = assertInstanceOf(A2AClientHTTPError_v0_3.class, unauthorized.getCause());
        assertEquals(401, cause.getCode());
        assertEquals(List.of("Bearer"), cause.getResponseHeaders().get("www-authenticate"));
    }

    @Test
    void retainsRecognizedProtocolErrors() {
        var exception = RestErrorMapper_v0_3.mapRestError(response(404,
                "{\"error\":\"org.a2aproject.sdk.compat03.spec.TaskNotFoundError_v0_3\",\"message\":\"missing\"}", Map.of()));
        assertInstanceOf(TaskNotFoundError_v0_3.class, exception.getCause());
        assertEquals("missing", exception.getMessage());
    }

    @Test
    void bodyAndClassNameOverloadsRetainHttpStatus() {
        assertEquals(503, assertInstanceOf(A2AClientHTTPError_v0_3.class,
                RestErrorMapper_v0_3.mapRestError("unavailable", 503).getCause()).getCode());
        assertEquals(401, assertInstanceOf(A2AClientHTTPError_v0_3.class,
                RestErrorMapper_v0_3.mapRestError("unknown", "unauthorized", 401).getCause()).getCode());
    }

    private static A2AHttpResponse response(int status, String body, Map<String, List<String>> headers) {
        return new A2AHttpResponse() {
            @Override public int status() { return status; }
            @Override public boolean success() { return false; }
            @Override public String body() { return body; }
            @Override public A2AHttpHeaders headers() { return A2AHttpHeaders.of(headers); }
        };
    }
}
