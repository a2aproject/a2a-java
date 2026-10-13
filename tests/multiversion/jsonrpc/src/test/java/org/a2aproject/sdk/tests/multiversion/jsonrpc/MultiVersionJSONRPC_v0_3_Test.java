package org.a2aproject.sdk.tests.multiversion.jsonrpc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.JsonParser;
import io.quarkus.test.junit.QuarkusTest;
import org.a2aproject.sdk.compat03.client.ClientBuilder_v0_3;
import org.a2aproject.sdk.compat03.client.transport.jsonrpc.JSONRPCTransportConfigBuilder_v0_3;
import org.a2aproject.sdk.compat03.client.transport.jsonrpc.JSONRPCTransport_v0_3;
import org.a2aproject.sdk.compat03.conversion.AbstractA2AServerServerTest_v0_3;
import org.a2aproject.sdk.compat03.spec.TransportProtocol_v0_3;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class MultiVersionJSONRPC_v0_3_Test extends AbstractA2AServerServerTest_v0_3 {

    public MultiVersionJSONRPC_v0_3_Test() {
        super(8081);
    }

    @Override
    protected String getTransportProtocol() {
        return TransportProtocol_v0_3.JSONRPC.asString();
    }

    @Override
    protected String getTransportUrl() {
        return "http://localhost:8081";
    }

    @Override
    protected void configureTransport(ClientBuilder_v0_3 builder) {
        builder.withTransport(JSONRPCTransport_v0_3.class, new JSONRPCTransportConfigBuilder_v0_3());
    }
    @Test
    public void testSelectedVersionDecodeErrors() throws Exception {
        assertVersionFieldError(decodeResponse("0.3", "", "{\"id\":\"original\",\"method\":null}"));
        HttpResponse<String> response = decodeResponse("0.3", "", """
                {"jsonrpc":"2.0","id":"original","method":"message/stream","params":[]}
                """);
        assertEquals(JsonParser.parseString("""
                {"jsonrpc":"2.0","id":"original","error":{"code":-32602,"message":"Failed to parse JSON"}}
                """), JsonParser.parseString(response.body()));
    }

    @Test
    public void testVersionSelectionBeforeDecode() throws Exception {
        String body = "{\"id\":\"original\",\"method\":null}";
        assertVersionFieldError(decodeResponse(null, "", body));
        assertVersionFieldError(decodeResponse("0.3", "?A2A-Version=1.0", body));
        assertVersionFieldError(decodeResponse(null, "?A2A-Version=0.3", body));
        HttpResponse<String> response = decodeResponse("1.0", "?A2A-Version=0.3", body);
        assertEquals(JsonParser.parseString("""
                {"jsonrpc":"2.0","id":"original","error":{"code":-32600,
                "message":"Missing required 'jsonrpc' field. All requests must include 'jsonrpc': '2.0'",
                "data":[{"@type":"type.googleapis.com/google.rpc.ErrorInfo","reason":"INVALID_REQUEST",
                "domain":"a2a-protocol.org","metadata":{}}]}}
                """), JsonParser.parseString(response.body()));
    }

    private void assertVersionFieldError(HttpResponse<String> response) {
        assertEquals(JsonParser.parseString("""
                {"jsonrpc":"2.0","id":"original","error":{"code":-32600,
                "message":"Invalid JSON-RPC request: missing or invalid 'jsonrpc' field"}}
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
