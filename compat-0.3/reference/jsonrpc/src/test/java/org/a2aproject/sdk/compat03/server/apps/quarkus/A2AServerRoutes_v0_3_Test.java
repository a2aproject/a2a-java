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
import java.util.stream.Stream;

import jakarta.enterprise.inject.Instance;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.smallrye.mutiny.Multi;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.jspecify.annotations.Nullable;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

    private void assertError(JsonObject actual, String id, int code, @Nullable String message) {
        JsonObject expected = new JsonObject();
        expected.addProperty("jsonrpc", "2.0");
        if (id != null) {
            expected.add("id", JsonParser.parseString(id));
        }
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        if (message != null) {
            error.addProperty("message", message);
        }
        if (message == null) {
            assertEquals("2.0", actual.get("jsonrpc").getAsString());
            if (id == null) {
                assertNull(actual.get("id"));
            } else {
                assertEquals(JsonParser.parseString(id), actual.get("id"));
            }
            JsonObject actualError = actual.getAsJsonObject("error");
            assertEquals(error.get("code"), actualError.get("code"));
            assertNotNull(actualError.get("message"));
        } else {
            expected.add("error", error);
            assertEquals(expected, actual);
        }
    }

    @ParameterizedTest(name = "body={0}")
    @MethodSource("invalidJsonBodies")
    public void testInvalidJsonBeforeEnvelopeValidation(String body, String message) {
        assertError(invoke(body), null, -32700, message);
        verifyNoInteractions(transport);
    }

    private static Stream<Arguments> invalidJsonBodies() {
        return Stream.of(
                Arguments.of("{", "java.io.EOFException: End of input at line 1 column 2 path $."),
                Arguments.of("[]", "Not a JSON Object: []"),
                Arguments.of("null", "Not a JSON Object: null"),
                Arguments.of("42", "Not a JSON Object: 42"),
                Arguments.of("\"text\"", "Not a JSON Object: \"text\""),
                Arguments.of("", "Not a JSON Object: null"));
    }

    @Test
    public void testNullBodyIsJsonParseError() {
        assertError(invokeNullBody(), null, -32700, null);
        verifyNoInteractions(transport);
    }

    @ParameterizedTest(name = "id={0}")
    @org.junit.jupiter.params.provider.ValueSource(strings = {"{}", "[]"})
    public void testInvalidIdBeforeVersionAndMethod(String id) {
        assertError(invoke("{\"id\":" + id + ",\"jsonrpc\":false,\"method\":null}"), null, -32600,
                "Invalid JSON-RPC request: 'id' must be a string, number, or null");
        verifyNoInteractions(transport);
    }

    @ParameterizedTest(name = "id={0}")
    @MethodSource("idCoercionCases")
    public void testExistingIdCoercion(@Nullable String rawId, @Nullable String responseId) {
        String id = rawId == null ? "" : "\"id\":" + rawId + ",";
        assertError(invoke("{" + id + "\"jsonrpc\":\"wrong\"}"), responseId, -32600,
                "Invalid JSON-RPC request: missing or invalid 'jsonrpc' field");
        verifyNoInteractions(transport);
    }

    private static Stream<Arguments> idCoercionCases() {
        return Stream.of(
                Arguments.of("\"request\"", "\"request\""),
                Arguments.of("7", "7"),
                Arguments.of("7.9", "7"),
                Arguments.of("true", "\"true\""),
                Arguments.of("null", null),
                Arguments.of((Object) null, null));
    }

    @ParameterizedTest(name = "jsonrpc={0}")
    @MethodSource("invalidVersions")
    public void testVersionFieldErrorRetainsId(@Nullable String version) {
        String field = version == null ? "" : ",\"jsonrpc\":" + version;
        assertError(invoke("{\"id\":\"original\",\"method\":{}" + field + "}"), "\"original\"", -32600,
                "Invalid JSON-RPC request: missing or invalid 'jsonrpc' field");
        verifyNoInteractions(transport);
    }

    private static Stream<Arguments> invalidVersions() {
        return Stream.of((String) null, "null", "{}", "[]", "true", "1", "\"1.0\"").map(Arguments::of);
    }

    @ParameterizedTest(name = "method={0}")
    @MethodSource("invalidMethods")
    public void testMethodFieldErrorRetainsId(@Nullable String method) {
        String field = method == null ? "" : ",\"method\":" + method;
        assertError(invoke("{\"jsonrpc\":\"2.0\",\"id\":17" + field + "}"), "17", -32600,
                "Invalid JSON-RPC request: missing or invalid 'method' field");
        verifyNoInteractions(transport);
    }

    private static Stream<Arguments> invalidMethods() {
        return Stream.of((String) null, "null", "{}", "[]").map(Arguments::of);
    }

    @ParameterizedTest(name = "method={0}")
    @org.junit.jupiter.params.provider.ValueSource(strings = {"\"unknown\"", "\"\""})
    public void testUnknownMethodBeforeParams(String method) {
        assertError(invoke("{\"jsonrpc\":\"2.0\",\"id\":\"unknown\",\"method\":" + method
                + ",\"params\":[]}"), "\"unknown\"", -32601, "Method not found");
        verifyNoInteractions(transport);
    }

    @ParameterizedTest(name = "method={0}")
    @org.junit.jupiter.params.provider.ValueSource(strings = {"true", "17"})
    public void testNonStringMethodBeforeParams(String method) {
        assertError(invoke("{\"jsonrpc\":\"2.0\",\"id\":\"unknown\",\"method\":" + method
                + ",\"params\":[]}"), "\"unknown\"", -32600,
                "Invalid JSON-RPC request: missing or invalid 'method' field");
        verifyNoInteractions(transport);
    }

    @ParameterizedTest(name = "params={0}")
    @MethodSource("invalidGetParams")
    public void testParamsErrorRetainsId(@Nullable String params) {
        JsonObject actual = invoke(request("tasks/get", params));
        assertError(actual, "\"original\"", -32602,
                "{\"historyLength\":-1}".equals(params) ? null : "Failed to parse JSON");
        verifyNoInteractions(transport);
    }

    private static Stream<Arguments> invalidGetParams() {
        return Stream.of(Arguments.of("[]"), Arguments.of("\"bad\""),
                Arguments.of("{\"historyLength\":-1}"),
                Arguments.of("{\"id\":\"task\",\"historyLength\":\"bad\"}"));
    }

    @ParameterizedTest(name = "params={0}")
    @MethodSource("emptyParams")
    public void testMissingMessageParamsIsInvalidParams(@Nullable String params) {
        useRealTransport();
        assertError(invoke(request("message/send", params)), "\"original\"", -32602,
                "Parameter 'params' may not be null");
    }

    @ParameterizedTest(name = "params={0}")
    @MethodSource("emptyParams")
    public void testMissingGetParamsReachTransport(@Nullable String params) {
        when(transport.onGetTask(any(), any())).thenThrow(new IllegalStateException("dispatch reached"));
        assertError(invoke(request("tasks/get", params)), "\"original\"", -32603, "dispatch reached");
        ArgumentCaptor<org.a2aproject.sdk.compat03.spec.GetTaskRequest_v0_3> decoded =
                ArgumentCaptor.forClass(org.a2aproject.sdk.compat03.spec.GetTaskRequest_v0_3.class);
        verify(transport).onGetTask(decoded.capture(), any());
        org.junit.jupiter.api.Assertions.assertNull(decoded.getValue().getParams());
    }

    private static Stream<Arguments> emptyParams() {
        return Stream.of(Arguments.of((Object) null), Arguments.of("null"));
    }

    @ParameterizedTest(name = "params={0}")
    @MethodSource("invalidMessageParams")
    public void testInvalidMessageParams(@Nullable String params) {
        assertError(invoke(request("message/send", params)), "\"original\"", -32602, "Failed to parse JSON");
        verifyNoInteractions(transport);
    }

    private static Stream<Arguments> invalidMessageParams() {
        return Stream.of(
                Arguments.of("{\"message\":{\"kind\":\"message\",\"messageId\":\"m\",\"role\":\"invalid\",\"parts\":[{\"kind\":\"text\",\"text\":\"hello\"}]}}"),
                Arguments.of("{\"message\":{\"kind\":\"message\",\"messageId\":\"m\",\"role\":\"user\",\"parts\":[{}]}}"),
                Arguments.of("{\"message\":{\"kind\":\"message\",\"messageId\":\"m\",\"role\":\"user\",\"parts\":[{\"kind\":\"file\",\"file\":{}}]}}"));
    }

    @ParameterizedTest(name = "method={0}")
    @org.junit.jupiter.params.provider.ValueSource(strings = {"message/stream", "tasks/resubscribe"})
    public void testStreamingDecodeFailureIsHttpJson(String method) {
        assertError(invoke(request(method, "[]")), "\"original\"", -32602, "Failed to parse JSON");
        verifyNoInteractions(transport);
    }

    @Test
    public void testStreamingDecodeFailureWithNullParamsIsHttpJson() {
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

    @ParameterizedTest(name = "method={0}")
    @MethodSource("supportedMethods")
    public void testSupportedMethodsDecode(String method, @Nullable String params, String dispatchMethod) {
        // A transport failure after decoding makes dispatch observable without implementing its dependencies.
        transport = mock(JSONRPCHandler_v0_3.class, invocation -> {
            throw new IllegalStateException("dispatch reached");
        });
        routes.jsonRpcHandler = transport;
        assertError(invoke(request(method, params)), "\"original\"", -32603, "dispatch reached");
        var invocation = mockingDetails(transport).getInvocations().iterator().next();
        assertEquals(dispatchMethod, invocation.getMethod().getName());
        assertEquals(1, mockingDetails(transport).getInvocations().size());
        JSONRPCRequest_v0_3<?> decoded = invocation.getArgument(0);
        assertEquals(method, decoded.getMethod());
        assertEquals("original", decoded.getId());
        ServerCallContext context = invocation.getArgument(1);
        assertEquals(method, context.getState().get(METHOD_NAME_KEY));
        if (params != null) {
            assertNotNull(decoded.getParams());
        }
    }

    private static Stream<Arguments> supportedMethods() {
        return Stream.of(
                Arguments.of("tasks/get", "{\"id\":\"task\"}", "onGetTask"),
                Arguments.of("tasks/cancel", "{\"id\":\"task\"}", "onCancelTask"),
                Arguments.of("message/send", "{\"message\":{\"kind\":\"message\",\"messageId\":\"m\",\"role\":\"user\",\"parts\":[{\"kind\":\"text\",\"text\":\"hello\"}]}}", "onMessageSend"),
                Arguments.of("tasks/pushNotificationConfig/set", "{\"taskId\":\"task\",\"pushNotificationConfig\":{\"url\":\"http://localhost/callback\"}}", "setPushNotificationConfig"),
                Arguments.of("tasks/pushNotificationConfig/get", "{\"id\":\"task\"}", "getPushNotificationConfig"),
                Arguments.of("tasks/pushNotificationConfig/list", "{\"id\":\"task\"}", "listPushNotificationConfig"),
                Arguments.of("tasks/pushNotificationConfig/delete", "{\"id\":\"task\",\"pushNotificationConfigId\":\"config\"}", "deletePushNotificationConfig"),
                Arguments.of("agent/getAuthenticatedExtendedCard", null, "onGetAuthenticatedExtendedCardRequest"));
    }

    @ParameterizedTest(name = "method={0}")
    @MethodSource("streamingMethods")
    public void testSupportedStreamingMethodsDecode(String method, String params) {
        var publisher = Multi.createFrom().item(new SendStreamingMessageResponse_v0_3("original", new TaskNotFoundError_v0_3()));
        when(transport.onMessageSendStream(any(), any())).thenReturn(publisher);
        when(transport.onResubscribeToTask(any(), any())).thenReturn(publisher);
        routes.invokeJSONRPCHandler(request(method, params), rc);
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

    private static Stream<Arguments> streamingMethods() {
        return Stream.of(
                Arguments.of("message/stream", "{\"message\":{\"kind\":\"message\",\"messageId\":\"m\",\"role\":\"user\",\"parts\":[{\"kind\":\"text\",\"text\":\"hello\"}]}}"),
                Arguments.of("tasks/resubscribe", "{\"id\":\"task\"}"));
    }

    @ParameterizedTest(name = "body={0}, recoveredMethod={1}")
    @MethodSource("contextRecoveryCases")
    public void testMethodContextRecoveryOnDecodeFailure(String body, @Nullable String expectedMethod) {
        ServerCallContext context = new ServerCallContext(UnauthenticatedUser.INSTANCE, new HashMap<>(), Set.of());
        CallContextFactory_v0_3 factory = mock(CallContextFactory_v0_3.class);
        when(routes.callContextFactory.isUnsatisfied()).thenReturn(false);
        when(routes.callContextFactory.get()).thenReturn(factory);
        when(factory.build(rc)).thenReturn(context);
        invoke(body);
        if (expectedMethod == null) {
            assertNull(context.getState().get(METHOD_NAME_KEY));
        } else {
            assertEquals(expectedMethod, context.getState().get(METHOD_NAME_KEY));
        }
        verifyNoInteractions(transport);
    }

    private static Stream<Arguments> contextRecoveryCases() {
        return Stream.of(
                Arguments.of("{", null),
                Arguments.of("{\"id\":{},\"method\":\"tasks/get\"}", null),
                Arguments.of("{\"id\":17,\"jsonrpc\":\"wrong\",\"method\":\"tasks/get\"}", null),
                Arguments.of("{\"id\":17,\"jsonrpc\":\"2.0\",\"method\":{}}", null),
                Arguments.of(request("unknown", "[]"), "unknown"),
                Arguments.of(request("tasks/get", "[]"), "tasks/get"),
                Arguments.of(request("message/stream", "[]"), "message/stream"));
    }

    @ParameterizedTest(name = "id={0}")
    @MethodSource("dispatchIds")
    public void testDispatchFailureRetainsRecoveredId(String rawId, String responseId, Object decodedId) {
        when(transport.onGetTask(any(), any())).thenThrow(new IllegalStateException("dispatch failed"));
        JsonObject body = JsonParser.parseString(request("tasks/get", "{\"id\":\"task\"}")).getAsJsonObject();
        body.add("id", JsonParser.parseString(rawId));
        assertError(invoke(body.toString()), responseId, -32603, "dispatch failed");
        ArgumentCaptor<org.a2aproject.sdk.compat03.spec.GetTaskRequest_v0_3> decoded =
                ArgumentCaptor.forClass(org.a2aproject.sdk.compat03.spec.GetTaskRequest_v0_3.class);
        verify(transport).onGetTask(decoded.capture(), any());
        assertEquals(decodedId, decoded.getValue().getId());
    }

    private static Stream<Arguments> dispatchIds() {
        return Stream.of(
                Arguments.of("\"original\"", "\"original\"", "original"),
                Arguments.of("7.9", "7", Double.valueOf(7.9)),
                Arguments.of("true", "\"true\"", Boolean.TRUE));
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
