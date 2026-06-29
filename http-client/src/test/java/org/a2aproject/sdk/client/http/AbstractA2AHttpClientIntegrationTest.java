package org.a2aproject.sdk.client.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.a2aproject.sdk.common.A2AErrorMessages;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;

public abstract class AbstractA2AHttpClientIntegrationTest {

    private ClientAndServer mockServer;
    private A2AHttpClient client;

    protected abstract A2AHttpClient createClient();

    @BeforeEach
    public void setup() {
        mockServer = ClientAndServer.startClientAndServer(0);
        client = createClient();
    }

    @AfterEach
    public void teardown() {
        if (mockServer != null) {
            mockServer.stop();
        }
    }

    private String getBaseUrl() {
        return "http://localhost:" + mockServer.getPort();
    }

    @Test
    public void testGetRequestSuccess() throws Exception {
        mockServer
                .when(request().withMethod("GET").withPath("/test"))
                .respond(response().withStatusCode(200).withBody("success"));

        A2AHttpResponse response = client.createGet()
                .url(getBaseUrl() + "/test")
                .get();

        assertEquals(200, response.status());
        assertTrue(response.success());
        assertEquals("success", response.body());
    }

    @Test
    public void testPostRequestSuccess() throws Exception {
        mockServer
                .when(request()
                        .withMethod("POST")
                        .withPath("/test")
                        .withBody("{\"key\":\"value\"}"))
                .respond(response().withStatusCode(201).withBody("created"));

        A2AHttpResponse response = client.createPost()
                .url(getBaseUrl() + "/test")
                .body("{\"key\":\"value\"}")
                .post();

        assertEquals(201, response.status());
        assertTrue(response.success());
        assertEquals("created", response.body());
    }

    @Test
    public void testDeleteRequestSuccess() throws Exception {
        mockServer
                .when(request().withMethod("DELETE").withPath("/test"))
                .respond(response().withStatusCode(204));

        A2AHttpResponse response = client.createDelete()
                .url(getBaseUrl() + "/test")
                .delete();

        assertEquals(204, response.status());
        assertTrue(response.success());
    }

    @Test
    public void test401AuthenticationErrorOnGet() throws Exception {
        mockServer
                .when(request().withMethod("GET").withPath("/test"))
                .respond(response().withStatusCode(401));

        Exception exception = assertThrows(IOException.class, () -> {
            client.createGet()
                    .url(getBaseUrl() + "/test")
                    .get();
        });

        assertEquals(A2AErrorMessages.AUTHENTICATION_FAILED, exception.getMessage());
    }

    @Test
    public void test403AuthorizationErrorOnGet() throws Exception {
        mockServer
                .when(request().withMethod("GET").withPath("/test"))
                .respond(response().withStatusCode(403));

        Exception exception = assertThrows(IOException.class, () -> {
            client.createGet()
                    .url(getBaseUrl() + "/test")
                    .get();
        });

        assertEquals(A2AErrorMessages.AUTHORIZATION_FAILED, exception.getMessage());
    }

    @Test
    public void test401AuthenticationErrorOnPost() throws Exception {
        mockServer
                .when(request().withMethod("POST").withPath("/test"))
                .respond(response().withStatusCode(401));

        Exception exception = assertThrows(IOException.class, () -> {
            client.createPost()
                    .url(getBaseUrl() + "/test")
                    .body("{}")
                    .post();
        });

        assertEquals(A2AErrorMessages.AUTHENTICATION_FAILED, exception.getMessage());
    }

    @Test
    public void test403AuthorizationErrorOnPost() throws Exception {
        mockServer
                .when(request().withMethod("POST").withPath("/test"))
                .respond(response().withStatusCode(403));

        Exception exception = assertThrows(IOException.class, () -> {
            client.createPost()
                    .url(getBaseUrl() + "/test")
                    .body("{}")
                    .post();
        });

        assertEquals(A2AErrorMessages.AUTHORIZATION_FAILED, exception.getMessage());
    }

    @Test
    public void test401AuthenticationErrorOnDelete() throws Exception {
        mockServer
                .when(request().withMethod("DELETE").withPath("/test"))
                .respond(response().withStatusCode(401));

        Exception exception = assertThrows(IOException.class, () -> {
            client.createDelete()
                    .url(getBaseUrl() + "/test")
                    .delete();
        });

        assertEquals(A2AErrorMessages.AUTHENTICATION_FAILED, exception.getMessage());
    }

    @Test
    public void testHeaderPropagation() throws Exception {
        mockServer
                .when(request()
                        .withMethod("GET")
                        .withPath("/test")
                        .withHeader("Authorization", "Bearer token")
                        .withHeader("X-Custom-Header", "custom-value"))
                .respond(response().withStatusCode(200).withBody("ok"));

        A2AHttpResponse response = client.createGet()
                .url(getBaseUrl() + "/test")
                .addHeader("Authorization", "Bearer token")
                .addHeader("X-Custom-Header", "custom-value")
                .get();

        assertEquals(200, response.status());
        assertEquals("ok", response.body());
    }

    @Test
    public void testNonSuccessStatusCode() throws Exception {
        mockServer
                .when(request().withMethod("GET").withPath("/test"))
                .respond(response().withStatusCode(500).withBody("Internal Server Error"));

        A2AHttpResponse response = client.createGet()
                .url(getBaseUrl() + "/test")
                .get();

        assertEquals(500, response.status());
        assertFalse(response.success());
        assertEquals("Internal Server Error", response.body());
    }

    @Test
    public void test404NotFound() throws Exception {
        mockServer
                .when(request().withMethod("GET").withPath("/test"))
                .respond(response().withStatusCode(404).withBody("Not Found"));

        A2AHttpResponse response = client.createGet()
                .url(getBaseUrl() + "/test")
                .get();

        assertEquals(404, response.status());
        assertFalse(response.success());
        assertEquals("Not Found", response.body());
    }

    @Test
    public void testResponseHeadersOnSuccess() throws Exception {
        mockServer
                .when(request().withMethod("GET").withPath("/test"))
                .respond(response()
                        .withStatusCode(200)
                        .withHeader("X-Custom-Header", "custom-value")
                        .withHeader("X-Request-Id", "abc-123")
                        .withBody("ok"));

        A2AHttpResponse response = client.createGet()
                .url(getBaseUrl() + "/test")
                .get();

        assertEquals(200, response.status());
        A2AHttpHeaders headers = response.headers();
        assertNotNull(headers);
        assertEquals("custom-value", headers.firstValue("X-Custom-Header"));
        assertEquals("abc-123", headers.firstValue("X-Request-Id"));
    }

    @Test
    public void testResponseHeadersCaseInsensitive() throws Exception {
        mockServer
                .when(request().withMethod("GET").withPath("/test"))
                .respond(response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}"));

        A2AHttpResponse response = client.createGet()
                .url(getBaseUrl() + "/test")
                .get();

        A2AHttpHeaders headers = response.headers();
        assertEquals(headers.firstValue("Content-Type"), headers.firstValue("content-type"));
    }

    @Test
    public void testResponseHeadersOnErrorStatus() throws Exception {
        mockServer
                .when(request().withMethod("POST").withPath("/test"))
                .respond(response()
                        .withStatusCode(429)
                        .withHeader("Retry-After", "120")
                        .withBody("Too Many Requests"));

        A2AHttpResponse response = client.createPost()
                .url(getBaseUrl() + "/test")
                .body("{}")
                .post();

        assertEquals(429, response.status());
        assertFalse(response.success());
        A2AHttpHeaders headers = response.headers();
        assertEquals("120", headers.firstValue("Retry-After"));
    }

    @Test
    public void testResponseHeadersMissingHeader() throws Exception {
        mockServer
                .when(request().withMethod("GET").withPath("/test"))
                .respond(response().withStatusCode(200).withBody("ok"));

        A2AHttpResponse response = client.createGet()
                .url(getBaseUrl() + "/test")
                .get();

        A2AHttpHeaders headers = response.headers();
        assertNull(headers.firstValue("X-Nonexistent"));
        assertEquals(List.of(), headers.allValues("X-Nonexistent"));
    }

    @Test
    public void testResponseHeadersToMap() throws Exception {
        mockServer
                .when(request().withMethod("GET").withPath("/test"))
                .respond(response()
                        .withStatusCode(200)
                        .withHeader("X-Test", "value1")
                        .withBody("ok"));

        A2AHttpResponse response = client.createGet()
                .url(getBaseUrl() + "/test")
                .get();

        Map<String, List<String>> headerMap = response.headers().toMap();
        assertNotNull(headerMap);
        assertFalse(headerMap.isEmpty());
        assertTrue(headerMap.containsKey("X-Test") || headerMap.containsKey("x-test"));
    }
}
