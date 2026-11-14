package org.a2aproject.sdk.compat03.client.transport.jsonrpc;

import com.google.gson.JsonParser;
import java.util.List;
import java.util.Map;
import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallContext_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallInterceptor_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.PayloadAndHeaders_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.json.JsonUtil_v0_3;

/** Supplies deterministic request IDs to static protocol fixtures. */
final class FixedRequestId {
    private FixedRequestId() { }
    static JSONRPCTransport_v0_3 transport(A2AHttpClient httpClient) {
        var interceptor = new ClientCallInterceptor_v0_3() {
            @Override
            public PayloadAndHeaders_v0_3 intercept(String method, Object payload, Map<String, String> headers,
                    AgentCard_v0_3 card, ClientCallContext_v0_3 context) {
                try {
                    var json = JsonParser.parseString(JsonUtil_v0_3.toJson(payload)).getAsJsonObject();
                    json.addProperty("id", "test-request");
                    return new PayloadAndHeaders_v0_3(JsonUtil_v0_3.fromJson(json.toString(), payload.getClass()), headers);
                } catch (org.a2aproject.sdk.compat03.json.JsonProcessingException_v0_3 e) {
                    throw new AssertionError(e);
                }
            }
        };
        return new JSONRPCTransport_v0_3(httpClient, null, "http://localhost:4001", List.of(interceptor));
    }
    static String response(String json) {
        var object = JsonParser.parseString(json).getAsJsonObject();
        object.addProperty("id", "test-request");
        return object.toString();
    }
    static String sse(String data) {
        return data.replace("\"jsonrpc\":\"2.0\",\"id\":1", "\"jsonrpc\":\"2.0\",\"id\":\"test-request\"")
                .replace("\"id\": \"1234\"", "\"id\": \"test-request\"");
    }
}
