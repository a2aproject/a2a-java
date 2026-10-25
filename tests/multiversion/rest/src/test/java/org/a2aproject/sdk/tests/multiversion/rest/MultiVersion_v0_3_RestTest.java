package org.a2aproject.sdk.tests.multiversion.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.JsonParser;
import io.quarkus.test.junit.QuarkusTest;
import org.a2aproject.sdk.compat03.client.ClientBuilder_v0_3;
import org.a2aproject.sdk.compat03.client.transport.rest.RestTransportConfigBuilder_v0_3;
import org.a2aproject.sdk.compat03.client.transport.rest.RestTransport_v0_3;
import org.a2aproject.sdk.compat03.conversion.AbstractA2AServerServerTest_v0_3;
import org.a2aproject.sdk.compat03.spec.TransportProtocol_v0_3;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class MultiVersion_v0_3_RestTest extends AbstractA2AServerServerTest_v0_3 {

    public MultiVersion_v0_3_RestTest() {
        super(8081);
    }

    @Override
    protected String getTransportProtocol() {
        return TransportProtocol_v0_3.HTTP_JSON.asString();
    }

    @Override
    protected String getTransportUrl() {
        return "http://localhost:8081";
    }

    @Override
    protected void configureTransport(ClientBuilder_v0_3 builder) {
        builder.withTransport(RestTransport_v0_3.class, new RestTransportConfigBuilder_v0_3());
    }
    @Test
    public void testSelectedVersionHistoryValidation() throws Exception {
        assertAliasConflict(historyResponse("0.3", "history_length=1&historyLength=bad"));
    }

    @Test
    public void testVersionSelectionBeforeHistoryValidation() throws Exception {
        assertAliasConflict(historyResponse(null, "history_length=1&historyLength=bad"));
        assertAliasConflict(historyResponse("0.3", "A2A-Version=1.0&history_length=1&historyLength=bad"));
        assertAliasConflict(historyResponse(null, "A2A-Version=0.3&history_length=1&historyLength=bad"));
        HttpResponse<String> selectedV1 = historyResponse("1.0", "A2A-Version=0.3&historyLength=bad");
        assertEquals(422, selectedV1.statusCode());
        assertEquals(JsonParser.parseString("""
                {"error":{"code":422,"status":"INVALID_ARGUMENT","message":"bad historyLength","details":[{
                "@type":"type.googleapis.com/google.rpc.ErrorInfo","reason":"INVALID_PARAMS",
                "domain":"a2a-protocol.org","metadata":{}}]}}
                """), JsonParser.parseString(selectedV1.body()));
    }

    private void assertAliasConflict(HttpResponse<String> response) {
        assertEquals(422, response.statusCode());
        assertEquals("{\"error\": \"org.a2aproject.sdk.compat03.spec.InvalidParamsError_v0_3\", "
                + "\"message\": \"Only one of 'history_length' or 'historyLength' may be specified\"}", response.body());
    }
    private HttpResponse<String> historyResponse(String header, String query) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/v1/tasks/missing-history-task?" + query)).GET();
        if (header != null) {
            request.header("A2A-Version", header);
        }
        HttpResponse<String> response = HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals("application/json", response.headers().firstValue("Content-Type").orElseThrow());
        return response;
    }

}
