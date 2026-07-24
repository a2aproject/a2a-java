package org.a2aproject.sdk.client.transport.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.a2aproject.sdk.client.http.A2AHttpHeaders;
import org.a2aproject.sdk.client.http.A2AHttpResponse;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.A2AClientHTTPError;
import org.a2aproject.sdk.spec.TaskNotFoundError;
import org.junit.jupiter.api.Test;

public class RestErrorMapperTest {

    @Test
    public void testEmptyBodyFallbackIncludesHeaders() {
        Map<String, List<String>> headers = Map.of("Retry-After", List.of("60"));
        A2AClientException ex = RestErrorMapper.mapRestError("", 429, headers);

        assertInstanceOf(A2AClientHTTPError.class, ex.getCause());
        A2AClientHTTPError httpError = (A2AClientHTTPError) ex.getCause();
        assertEquals(429, httpError.getCode());
        assertEquals(List.of("60"), httpError.getResponseHeaders().get("Retry-After"));
    }

    @Test
    public void testNullBodyFallbackIncludesHeaders() {
        Map<String, List<String>> headers = Map.of("WWW-Authenticate", List.of("Bearer"));
        A2AClientException ex = RestErrorMapper.mapRestError(null, 401, headers);

        assertInstanceOf(A2AClientHTTPError.class, ex.getCause());
        A2AClientHTTPError httpError = (A2AClientHTTPError) ex.getCause();
        assertEquals(401, httpError.getCode());
        assertEquals(List.of("Bearer"), httpError.getResponseHeaders().get("WWW-Authenticate"));
    }

    @Test
    public void testUnrecognizedGoogleErrorFormatIncludesHeaders() {
        String body = "{\"error\": {\"code\": 503, \"message\": \"Service Unavailable\"}}";
        Map<String, List<String>> headers = Map.of("Retry-After", List.of("30"));
        A2AClientException ex = RestErrorMapper.mapRestError(body, 503, headers);

        assertInstanceOf(A2AClientHTTPError.class, ex.getCause());
        A2AClientHTTPError httpError = (A2AClientHTTPError) ex.getCause();
        assertEquals(503, httpError.getCode());
        assertEquals(List.of("30"), httpError.getResponseHeaders().get("Retry-After"));
    }

    @Test
    public void testRecognizedA2AErrorDoesNotWrapInHttpError() {
        String body = "{\"error\": {\"code\": 404, \"status\": \"NOT_FOUND\", \"message\": \"Task not found\", " +
                "\"details\": [{\"reason\": \"TASK_NOT_FOUND\"}]}}";
        A2AClientException ex = RestErrorMapper.mapRestError(body, 404, Map.of());

        assertInstanceOf(TaskNotFoundError.class, ex.getCause());
    }

    @Test
    public void testMapRestErrorFromA2AHttpResponse() {
        Map<String, List<String>> headerMap = Map.of("X-Custom", List.of("value"));
        A2AHttpResponse response = new A2AHttpResponse() {
            @Override
            public int status() {
                return 500;
            }

            @Override
            public boolean success() {
                return false;
            }

            @Override
            public String body() {
                return "";
            }

            @Override
            public A2AHttpHeaders headers() {
                return new A2AHttpHeaders() {
                    @Override
                    public String firstValue(String name) {
                        List<String> values = headerMap.get(name);
                        return values != null && !values.isEmpty() ? values.get(0) : null;
                    }

                    @Override
                    public List<String> allValues(String name) {
                        return headerMap.getOrDefault(name, List.of());
                    }

                    @Override
                    public Map<String, List<String>> toMap() {
                        return headerMap;
                    }
                };
            }
        };

        A2AClientException ex = RestErrorMapper.mapRestError(response);
        assertInstanceOf(A2AClientHTTPError.class, ex.getCause());
        A2AClientHTTPError httpError = (A2AClientHTTPError) ex.getCause();
        assertEquals(500, httpError.getCode());
        assertEquals(List.of("value"), httpError.getResponseHeaders().get("X-Custom"));
    }

    @Test
    public void testHeaderLookupIsCaseInsensitive() {
        Map<String, List<String>> headers = Map.of("Retry-After", List.of("60"));
        A2AClientException ex = RestErrorMapper.mapRestError("", 429, headers);

        A2AClientHTTPError httpError = (A2AClientHTTPError) ex.getCause();
        assertEquals(List.of("60"), httpError.getResponseHeaders().get("retry-after"));
        assertEquals(List.of("60"), httpError.getResponseHeaders().get("RETRY-AFTER"));
    }

    @Test
    public void testTwoArgOverloadDefaultsToEmptyHeaders() {
        A2AClientException ex = RestErrorMapper.mapRestError("", 500);

        assertInstanceOf(A2AClientHTTPError.class, ex.getCause());
        A2AClientHTTPError httpError = (A2AClientHTTPError) ex.getCause();
        assertNotNull(httpError.getResponseHeaders());
        assertTrue(httpError.getResponseHeaders().isEmpty());
    }
}
