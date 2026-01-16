package io.a2a.client.http;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import io.a2a.common.A2AErrorMessages;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;

public class VertxA2AHttpClientIntegrationTest {

    private ClientAndServer mockServer;
    private VertxA2AHttpClient client;

    @BeforeEach
    public void setup() {
        mockServer = ClientAndServer.startClientAndServer(0);  // Use random port
        client = new VertxA2AHttpClient();
    }

    @AfterEach
    public void teardown() {
        if (client != null) {
            client.close();
        }
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

        Exception exception = assertThrows(java.io.IOException.class, () -> {
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

        Exception exception = assertThrows(java.io.IOException.class, () -> {
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

        Exception exception = assertThrows(java.io.IOException.class, () -> {
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

        Exception exception = assertThrows(java.io.IOException.class, () -> {
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

        Exception exception = assertThrows(java.io.IOException.class, () -> {
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
}
