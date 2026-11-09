package org.a2aproject.sdk.tests.multiversion.jsonrpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import org.a2aproject.sdk.compat03.transport.jsonrpc.handler.JSONRPCHandler_v0_3;
import org.a2aproject.sdk.transport.jsonrpc.handler.JSONRPCHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Exercises the multiversion reference routes when their transport fails unexpectedly. */
@QuarkusTest
class InternalErrorResponseTest {
    private static final String INTERNAL_DETAIL = "detail: /var/lib/internal/config.db";

    @InjectMock
    JSONRPCHandler_v0_3 compatTransport;

    @InjectMock
    JSONRPCHandler transport;

    @TestHTTPResource("/")
    URI endpoint;

    @BeforeEach
    void setUp() {
        when(compatTransport.onGetTask(any(), any())).thenThrow(new IllegalStateException(INTERNAL_DETAIL));
        when(transport.onGetTask(any(), any())).thenThrow(new IllegalStateException(INTERNAL_DETAIL));
    }

    @ParameterizedTest
    @CsvSource({"0.3, tasks/get", "1.0, GetTask"})
    void unexpectedFailureUsesGenericMessage(String version, String method) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("A2A-Version", version)
                .POST(HttpRequest.BodyPublishers.ofString("{\"jsonrpc\":\"2.0\",\"id\":\"request\","
                        + "\"method\":\"" + method + "\",\"params\":{\"id\":\"task\"}}"))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("content-type").orElse("").startsWith("application/json"));
        JsonObject error = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonObject("error");
        assertEquals(-32603, error.get("code").getAsInt());
        assertEquals("Internal error", error.get("message").getAsString());
        assertFalse(response.body().contains(INTERNAL_DETAIL));
    }

}
