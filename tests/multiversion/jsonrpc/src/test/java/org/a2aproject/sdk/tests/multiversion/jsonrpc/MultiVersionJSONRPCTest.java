package org.a2aproject.sdk.tests.multiversion.jsonrpc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import jakarta.inject.Inject;

import com.google.gson.JsonParser;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.Vertx;
import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.http.vertx.VertxA2AHttpClient;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import org.a2aproject.sdk.server.apps.common.AbstractA2AServerTest;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class MultiVersionJSONRPCTest extends AbstractA2AServerTest {

    @Inject
    Vertx vertx;

    public MultiVersionJSONRPCTest() {
        super(8081);
    }

    @Override
    protected String getTransportProtocol() {
        return TransportProtocol.JSONRPC.asString();
    }

    @Override
    protected String getTransportUrl() {
        return "http://localhost:8081";
    }

    @Override
    protected void configureTransport(ClientBuilder builder) {
        builder.withTransport(JSONRPCTransport.class,
                new JSONRPCTransportConfigBuilder()
                        .httpClient(new VertxA2AHttpClient(vertx)));
    }
    @Test
    public void testUnsupportedVersionBeforeBodyValidation() throws Exception {
        for (String body : new String[] {"{", "{\"id\":\"recoverable\",\"method\":false}"}) {
            HttpResponse<String> response = decodeResponse("2.0", "", body);
            assertEquals(JsonParser.parseString("""
                    {"jsonrpc":"2.0","id":null,"error":{"code":-32009,
                    "message":"Protocol version '2.0' is not supported. Supported versions: [1.0, 0.3]",
                    "data":[{"@type":"type.googleapis.com/google.rpc.ErrorInfo","reason":"VERSION_NOT_SUPPORTED",
                    "domain":"a2a-protocol.org","metadata":{}}]}}
                    """), JsonParser.parseString(response.body()));
        }
    }

    @Test
    public void testSelectedVersionDecodeErrors() throws Exception {
        HttpResponse<String> response = decodeResponse("1.0", "", """
                {"id":"original","method":"SendMessage","params":[]}
                """);
        assertEquals(JsonParser.parseString("""
                {"jsonrpc":"2.0","id":"original","error":{"code":-32600,
                "message":"Missing required 'jsonrpc' field. All requests must include 'jsonrpc': '2.0'",
                "data":[{"@type":"type.googleapis.com/google.rpc.ErrorInfo","reason":"INVALID_REQUEST",
                "domain":"a2a-protocol.org","metadata":{}}]}}
                """), JsonParser.parseString(response.body()));
        response = decodeResponse("1.0", "", """
                {"jsonrpc":"2.0","id":"original","method":"SendMessage","params":{
                 "message":{"role":"ROLE_USER","parts":[{"text":"hello"}]}}}
                """);
        assertEquals(JsonParser.parseString("""
                {"jsonrpc":"2.0","id":"original","error":{"code":-32602,
                "message":"Required field cannot be null or empty",
                "data":[{"@type":"type.googleapis.com/google.rpc.ErrorInfo","reason":"INVALID_PARAMS",
                "domain":"a2a-protocol.org","metadata":{}}]}}
                """), JsonParser.parseString(response.body()));
    }
    private HttpResponse<String> decodeResponse(String header, String query, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/" + query))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        if (header != null) {
            request.header("A2A-Version", header);
        }
        HttpResponse<String> response = HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals("application/json", response.headers().firstValue("Content-Type").orElseThrow());
        return response;
    }

}
