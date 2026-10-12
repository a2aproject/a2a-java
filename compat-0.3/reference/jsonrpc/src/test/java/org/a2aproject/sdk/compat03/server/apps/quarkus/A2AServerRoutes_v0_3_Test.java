package org.a2aproject.sdk.compat03.server.apps.quarkus;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static org.a2aproject.sdk.compat03.transport.jsonrpc.context.JSONRPCContextKeys_v0_3.METHOD_NAME_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.inject.Instance;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.smallrye.mutiny.Multi;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.a2aproject.sdk.compat03.conversion.Convert_v0_3_To10RequestHandler;
import org.a2aproject.sdk.compat03.spec.AgentCapabilities_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONRPCRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.SendStreamingMessageResponse_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskNotFoundError_v0_3;
import org.a2aproject.sdk.compat03.transport.jsonrpc.handler.JSONRPCHandler_v0_3;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.UnauthenticatedUser;
import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Characterizes the existing route decoder, including the separately recovered ID. */
public class A2AServerRoutes_v0_3_Test {
    private A2AServerRoutes_v0_3 routes;
    private JSONRPCHandler_v0_3 transport;
    private RoutingContext rc;
    private HttpServerResponse response;
    private MultiMap responseHeaders;

    @BeforeEach
    public void setUp() {
        routes = new A2AServerRoutes_v0_3();
        transport = mock(JSONRPCHandler_v0_3.class);
        routes.jsonRpcHandler = transport;
        routes.callContextFactory = mock(Instance.class);
        when(routes.callContextFactory.isUnsatisfied()).thenReturn(true);
        rc = mock(RoutingContext.class);
        HttpServerRequest request = mock(HttpServerRequest.class);
        response = mock(HttpServerResponse.class);
        responseHeaders = MultiMap.caseInsensitiveMultiMap();
        when(rc.request()).thenReturn(request);
        when(rc.response()).thenReturn(response);
        when(request.headers()).thenReturn(MultiMap.caseInsensitiveMultiMap());
        when(response.headers()).thenReturn(responseHeaders);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(response.putHeader(any(CharSequence.class), any(CharSequence.class))).thenReturn(response);
        when(response.end(anyString())).thenReturn(Future.succeededFuture());
        when(response.end()).thenReturn(Future.succeededFuture());
    }

    private JsonObject invoke(String body) {
        routes.invokeJSONRPCHandler(body, rc);
        return responseJson();
    }

    private JsonObject invokeNullBody() {
        routes.invokeJSONRPCHandler(null, rc);
        return responseJson();
    }

    private JsonObject responseJson() {
        verify(response).setStatusCode(200);
        verify(response).putHeader(CONTENT_TYPE, "application/json");
        ArgumentCaptor<String> output = ArgumentCaptor.forClass(String.class);
        verify(response).end(output.capture());
        verifyNoMoreInteractions(response);
        return JsonParser.parseString(output.getValue()).getAsJsonObject();
    }

    private void assertError(JsonObject actual, String id, int code, String message) {
        JsonObject expected = new JsonObject();
        expected.addProperty("jsonrpc", "2.0");
        if (id != null) {
            expected.add("id", JsonParser.parseString(id));
        }
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", message);
        expected.add("error", error);
        assertEquals(expected, actual);
    }

    @Test
    public void testInvalidJsonBeforeEnvelopeValidation() {
        String[][] cases = {
                {"{", "java.io.EOFException: End of input at line 1 column 2 path $."},
                {"[]", "Not a JSON Object: []"},
                {"null", "Not a JSON Object: null"},
                {"42", "Not a JSON Object: 42"},
                {"\"text\"", "Not a JSON Object: \"text\""},
                {"", "Not a JSON Object: null"}
        };
        for (String[] entry : cases) {
            setUp();
            assertError(invoke(entry[0]), null, -32700, entry[1]);
            verifyNoInteractions(transport);
        }
    }

    @Test
    public void testNullBodyIsJsonParseError() {
        assertError(invokeNullBody(), null, -32700,
                "Cannot invoke \"String.length()\" because \"s\" is null");
        verifyNoInteractions(transport);
    }

    @Test
    public void testInvalidIdBeforeVersionAndMethod() {
        for (String id : List.of("{}", "[]")) {
            setUp();
            assertError(invoke("{\"id\":" + id + ",\"jsonrpc\":false,\"method\":null}"), null, -32600,
                    "Invalid JSON-RPC request: 'id' must be a string, number, or null");
            verifyNoInteractions(transport);
        }
    }

    @Test
    public void testExistingIdCoercion() {
        String[][] cases = {{"\"request\"", "\"request\""}, {"7", "7"}, {"7.9", "7"},
                {"true", "\"true\""}, {"null", null}, {null, null}};
        for (String[] entry : cases) {
            setUp();
            String id = entry[0] == null ? "" : "\"id\":" + entry[0] + ",";
            assertError(invoke("{" + id + "\"jsonrpc\":\"wrong\"}"), entry[1], -32600,
                    "Invalid JSON-RPC request: missing or invalid 'jsonrpc' field");
            verifyNoInteractions(transport);
        }
    }

    @Test
    public void testVersionFieldErrorRetainsId() {
        for (String version : new String[] {null, "null", "{}", "[]", "true", "1", "\"1.0\""}) {
            setUp();
            String field = version == null ? "" : ",\"jsonrpc\":" + version;
            assertError(invoke("{\"id\":\"original\",\"method\":{}" + field + "}"), "\"original\"", -32600,
                    "Invalid JSON-RPC request: missing or invalid 'jsonrpc' field");
            verifyNoInteractions(transport);
        }
    }

    @Test
    public void testMethodFieldErrorRetainsId() {
        for (String method : new String[] {null, "null", "{}", "[]"}) {
            setUp();
            String field = method == null ? "" : ",\"method\":" + method;
            assertError(invoke("{\"jsonrpc\":\"2.0\",\"id\":17" + field + "}"), "17", -32600,
                    "Invalid JSON-RPC request: missing or invalid 'method' field");
            verifyNoInteractions(transport);
        }
    }

    @Test
    public void testUnknownMethodBeforeParams() {
        for (String method : List.of("\"unknown\"", "\"\"", "true", "17")) {
            setUp();
            assertError(invoke("{\"jsonrpc\":\"2.0\",\"id\":\"unknown\",\"method\":" + method
                    + ",\"params\":[]}"), "\"unknown\"", -32601, "Method not found");
            verifyNoInteractions(transport);
        }
    }

    @Test
    public void testParamsErrorRetainsId() {
        for (String params : List.of("[]", "\"bad\"", "{\"historyLength\":-1}",
                "{\"id\":\"task\",\"historyLength\":\"bad\"}")) {
            setUp();
            JsonObject actual = invoke(request("tasks/get", params));
            assertError(actual, "\"original\"", -32602, params.equals("{\"historyLength\":-1}")
                    ? "Failed to invoke constructor 'org.a2aproject.sdk.compat03.spec.TaskQueryParams_v0_3(String, int, Map)' with args [null, -1, null]"
                    : "Failed to parse JSON");
            verifyNoInteractions(transport);
        }
        for (String params : new String[] {null, "null"}) {
            setUp();
            useRealTransport();
            JsonObject actual = invoke(request("message/send", params));
            assertError(actual, "\"original\"", -32602, "Parameter 'params' may not be null");
            setUp();
            when(transport.onGetTask(any(), any())).thenThrow(new IllegalStateException("dispatch reached"));
            assertError(invoke(request("tasks/get", params)), "\"original\"", -32603, "dispatch reached");
            ArgumentCaptor<org.a2aproject.sdk.compat03.spec.GetTaskRequest_v0_3> decoded =
                    ArgumentCaptor.forClass(org.a2aproject.sdk.compat03.spec.GetTaskRequest_v0_3.class);
            verify(transport).onGetTask(decoded.capture(), any());
            org.junit.jupiter.api.Assertions.assertNull(decoded.getValue().getParams());
        }
        for (String params : List.of("{\"message\":{\"kind\":\"message\",\"messageId\":\"m\",\"role\":\"invalid\",\"parts\":[{\"kind\":\"text\",\"text\":\"hello\"}]}}",
                "{\"message\":{\"kind\":\"message\",\"messageId\":\"m\",\"role\":\"user\",\"parts\":[{}]}}",
                "{\"message\":{\"kind\":\"message\",\"messageId\":\"m\",\"role\":\"user\",\"parts\":[{\"kind\":\"file\",\"file\":{}}]}}")) {
            setUp();
            assertError(invoke(request("message/send", params)), "\"original\"", -32602, "Failed to parse JSON");
            verifyNoInteractions(transport);
        }
    }

    @Test
    public void testStreamingDecodeFailureIsHttpJson() {
        for (String method : List.of("message/stream", "tasks/resubscribe")) {
            setUp();
            assertError(invoke(request(method, "[]")), "\"original\"", -32602, "Failed to parse JSON");
            verifyNoInteractions(transport);
        }
        setUp();
        useRealTransport();
        routes.invokeJSONRPCHandler(request("message/stream", "null"), rc);
        assertEquals("text/event-stream", responseHeaders.get(CONTENT_TYPE));
        ArgumentCaptor<io.vertx.core.buffer.Buffer> event = ArgumentCaptor.forClass(io.vertx.core.buffer.Buffer.class);
        verify(response).write(event.capture(), any());
        String eventText = event.getValue().toString();
        assertTrue(eventText.startsWith(": SSE stream started\n\ndata: "));
        assertTrue(eventText.endsWith("\nid: 0\n\n"));
        assertError(JsonParser.parseString(eventText.substring(eventText.indexOf("data: ") + 6,
                eventText.indexOf("\nid: "))).getAsJsonObject(), "\"original\"", -32603,
                "Parameter 'params' may not be null");
    }

    @Test
    public void testSupportedMethodsDecode() {
        String[][] cases = {
                {"tasks/get", "{\"id\":\"task\"}", "onGetTask"},
                {"tasks/cancel", "{\"id\":\"task\"}", "onCancelTask"},
                {"message/send", "{\"message\":{\"kind\":\"message\",\"messageId\":\"m\",\"role\":\"user\",\"parts\":[{\"kind\":\"text\",\"text\":\"hello\"}]}}", "onMessageSend"},
                {"tasks/pushNotificationConfig/set", "{\"taskId\":\"task\",\"pushNotificationConfig\":{\"url\":\"http://localhost/callback\"}}", "setPushNotificationConfig"},
                {"tasks/pushNotificationConfig/get", "{\"id\":\"task\"}", "getPushNotificationConfig"},
                {"tasks/pushNotificationConfig/list", "{\"id\":\"task\"}", "listPushNotificationConfig"},
                {"tasks/pushNotificationConfig/delete", "{\"id\":\"task\",\"pushNotificationConfigId\":\"config\"}", "deletePushNotificationConfig"},
                {"agent/getAuthenticatedExtendedCard", null, "onGetAuthenticatedExtendedCardRequest"}
        };
        for (String[] entry : cases) {
            setUp();
            // A transport failure after decoding makes dispatch observable without implementing its dependencies.
            transport = mock(JSONRPCHandler_v0_3.class, invocation -> {
                throw new IllegalStateException("dispatch reached");
            });
            routes.jsonRpcHandler = transport;
            assertError(invoke(request(entry[0], entry[1])), "\"original\"", -32603, "dispatch reached");
            var invocation = mockingDetails(transport).getInvocations().iterator().next();
            assertEquals(entry[2], invocation.getMethod().getName());
            assertEquals(1, mockingDetails(transport).getInvocations().size());
            JSONRPCRequest_v0_3<?> decoded = invocation.getArgument(0);
            assertEquals(entry[0], decoded.getMethod());
            assertEquals("original", decoded.getId());
            ServerCallContext context = invocation.getArgument(1);
            assertEquals(entry[0], context.getState().get(METHOD_NAME_KEY));
            if (entry[1] != null) {
                assertNotNull(decoded.getParams());
            }
        }
        for (String method : List.of("message/stream", "tasks/resubscribe")) {
            setUp();
            var publisher = Multi.createFrom().item(new SendStreamingMessageResponse_v0_3("original", new TaskNotFoundError_v0_3()));
            when(transport.onMessageSendStream(any(), any())).thenReturn(publisher);
            when(transport.onResubscribeToTask(any(), any())).thenReturn(publisher);
            routes.invokeJSONRPCHandler(request(method, method.equals("tasks/resubscribe") ? "{\"id\":\"task\"}"
                    : "{\"message\":{\"kind\":\"message\",\"messageId\":\"m\",\"role\":\"user\",\"parts\":[{\"kind\":\"text\",\"text\":\"hello\"}]}}"), rc);
            if (method.equals("message/stream")) {
                verify(transport).onMessageSendStream(any(), any());
            } else {
                verify(transport).onResubscribeToTask(any(), any());
            }
            verifyNoMoreInteractions(transport);
            var invocation = mockingDetails(transport).getInvocations().iterator().next();
            JSONRPCRequest_v0_3<?> decoded = invocation.getArgument(0);
            assertEquals(method, decoded.getMethod());
            assertEquals("original", decoded.getId());
            assertNotNull(decoded.getParams());
            ServerCallContext context = invocation.getArgument(1);
            assertEquals(method, context.getState().get(METHOD_NAME_KEY));
            assertEquals("text/event-stream", responseHeaders.get(CONTENT_TYPE));
            assertEquals("no-cache", responseHeaders.get("Cache-Control"));
            assertEquals("no", responseHeaders.get("X-Accel-Buffering"));
            ArgumentCaptor<io.vertx.core.buffer.Buffer> event = ArgumentCaptor.forClass(io.vertx.core.buffer.Buffer.class);
            verify(response).write(event.capture(), any());
            assertEquals(": SSE stream started\n\ndata: {\"jsonrpc\":\"2.0\",\"id\":\"original\",\"error\":{\"code\":-32001,\"message\":\"Task not found\"}}\nid: 0\n\n", event.getValue().toString());
            verify(response).end();
        }
    }

    @Test
    public void testMethodContextRecoveryOnDecodeFailure() {
        String[][] cases = {
                {"{", null},
                {"{\"id\":{},\"method\":\"tasks/get\"}", null},
                {"{\"id\":17,\"jsonrpc\":\"wrong\",\"method\":\"tasks/get\"}", null},
                {"{\"id\":17,\"jsonrpc\":\"2.0\",\"method\":{}}", null},
                {request("unknown", "[]"), "unknown"},
                {request("tasks/get", "[]"), "tasks/get"},
                {request("message/stream", "[]"), "message/stream"}
        };
        for (String[] entry : cases) {
            setUp();
            ServerCallContext context = new ServerCallContext(UnauthenticatedUser.INSTANCE, new HashMap<>(), Set.of());
            CallContextFactory_v0_3 factory = mock(CallContextFactory_v0_3.class);
            when(routes.callContextFactory.isUnsatisfied()).thenReturn(false);
            when(routes.callContextFactory.get()).thenReturn(factory);
            when(factory.build(rc)).thenReturn(context);
            invoke(entry[0]);
            if (entry[1] == null) {
                assertNull(context.getState().get(METHOD_NAME_KEY));
            } else {
                assertEquals(entry[1], context.getState().get(METHOD_NAME_KEY));
            }
            verifyNoInteractions(transport);
        }
    }

    @Test
    public void testDispatchFailureRetainsRecoveredId() {
        for (String id : List.of("\"original\"", "7.9", "true")) {
            setUp();
            when(transport.onGetTask(any(), any())).thenThrow(new IllegalStateException("dispatch failed"));
            JsonObject body = JsonParser.parseString(request("tasks/get", "{\"id\":\"task\"}")).getAsJsonObject();
            body.add("id", JsonParser.parseString(id));
            assertError(invoke(body.toString()), id.equals("7.9") ? "7" : id.equals("true") ? "\"true\"" : id,
                    -32603, "dispatch failed");
            ArgumentCaptor<org.a2aproject.sdk.compat03.spec.GetTaskRequest_v0_3> decoded =
                    ArgumentCaptor.forClass(org.a2aproject.sdk.compat03.spec.GetTaskRequest_v0_3.class);
            verify(transport).onGetTask(decoded.capture(), any());
            assertEquals(id.equals("7.9") ? Double.valueOf(7.9) : id.equals("true") ? Boolean.TRUE : "original",
                    decoded.getValue().getId());
        }
    }

    private static String request(String method, String params) {
        return "{\"jsonrpc\":\"2.0\",\"id\":\"original\",\"method\":\"" + method + "\""
                + (params == null ? "" : ",\"params\":" + params) + "}";
    }

    private void useRealTransport() {
        AgentCard_v0_3 card = new AgentCard_v0_3.Builder().name("validation").description("Validation baseline")
                .url("http://localhost:9999").version("1").protocolVersion("0.3")
                .capabilities(new AgentCapabilities_v0_3(true, true, false, null))
                .defaultInputModes(List.of("text")).defaultOutputModes(List.of("text")).skills(List.of()).build();
        routes.jsonRpcHandler = new JSONRPCHandler_v0_3(card, Runnable::run,
                new Convert_v0_3_To10RequestHandler(mock(RequestHandler.class)));
    }
}
