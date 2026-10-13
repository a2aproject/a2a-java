package org.a2aproject.sdk.compat03.transport.jsonrpc.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.stream.Stream;

import org.a2aproject.sdk.compat03.spec.CancelTaskRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.GetAuthenticatedExtendedCardRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.InvalidParamsError_v0_3;
import org.a2aproject.sdk.compat03.spec.InvalidRequestError_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONParseError_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONRPCError_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONRPCRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.ListTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.MethodNotFoundError_v0_3;
import org.a2aproject.sdk.compat03.spec.NonStreamingJSONRPCRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.SendMessageRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.SendStreamingMessageRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.SetTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.StreamingJSONRPCRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskResubscriptionRequest_v0_3;
import org.a2aproject.sdk.compat03.transport.jsonrpc.handler.JSONRPCRequestDecoder_v0_3.DecodeFailure;
import org.a2aproject.sdk.compat03.transport.jsonrpc.handler.JSONRPCRequestDecoder_v0_3.DecodedRequest;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Decoder parity with the reference route's captured wire responses. */
public class JSONRPCRequestDecoder_v0_3_Test {

    private static final String MESSAGE_PARAMS = "{\"message\":{\"kind\":\"message\",\"messageId\":\"m\","
            + "\"role\":\"user\",\"parts\":[{\"kind\":\"text\",\"text\":\"hello\"}]}}";

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
            assertFailure(entry[0], null, null, JSONParseError_v0_3.class, -32700, entry[1]);
        }
    }

    @Test
    public void testNullBodyIsJsonParseError() {
        DecodeFailure failure = assertInstanceOf(DecodeFailure.class, JSONRPCRequestDecoder_v0_3.decode(null));
        assertInstanceOf(JSONParseError_v0_3.class, failure.error());
        assertEquals(-32700, failure.error().getCode());
        assertEquals("Cannot invoke \"String.length()\" because \"s\" is null", failure.error().getMessage());
    }

    @Test
    public void testInvalidIdBeforeVersionAndMethod() {
        for (String id : List.of("{}", "[]")) {
            assertFailure("{\"id\":" + id + ",\"jsonrpc\":false,\"method\":null}", null, null,
                    InvalidRequestError_v0_3.class, -32600,
                    "Invalid JSON-RPC request: 'id' must be a string, number, or null");
        }
    }

    @Test
    public void testVersionFieldErrorRetainsId() {
        for (String id : List.of("\"original\"", "17")) {
            for (String version : new String[] {null, "null", "{}", "[]", "true", "1", "\"1.0\""}) {
                String field = version == null ? "" : ",\"jsonrpc\":" + version;
                assertFailure("{\"id\":" + id + ",\"method\":{}" + field + "}",
                        id.equals("17") ? Long.valueOf(17) : "original", null,
                        InvalidRequestError_v0_3.class, -32600,
                        "Invalid JSON-RPC request: missing or invalid 'jsonrpc' field");
            }
        }
    }

    @Test
    public void testMethodFieldErrorRetainsId() {
        for (String method : new String[] {null, "null", "{}", "[]"}) {
            String field = method == null ? "" : ",\"method\":" + method;
            assertFailure("{\"jsonrpc\":\"2.0\",\"id\":17" + field + "}", 17L, null,
                    InvalidRequestError_v0_3.class, -32600,
                    "Invalid JSON-RPC request: missing or invalid 'method' field");
        }
    }

    @Test
    public void testUnknownMethodBeforeParams() {
        String[][] cases = {{"\"unknown\"", "unknown"}, {"\"\"", ""}, {"true", "true"}, {"17", "17"}};
        for (String[] entry : cases) {
            assertFailure("{\"jsonrpc\":\"2.0\",\"id\":\"unknown\",\"method\":" + entry[0]
                    + ",\"params\":[]}", "unknown", entry[1], MethodNotFoundError_v0_3.class, -32601, "Method not found");
        }
    }

    @Test
    public void testParamsErrorRetainsId() {
        for (String params : List.of("[]", "\"bad\"", "{\"historyLength\":-1}",
                "{\"id\":\"task\",\"historyLength\":\"bad\"}")) {
            assertFailure(request("tasks/get", params), "original", "tasks/get", InvalidParamsError_v0_3.class, -32602,
                    params.equals("{\"historyLength\":-1}")
                            ? "Failed to invoke constructor 'org.a2aproject.sdk.compat03.spec.TaskQueryParams_v0_3(String, int, Map)' with args [null, -1, null]"
                            : "Failed to parse JSON");
        }
        for (String params : new String[] {null, "null"}) {
            for (String method : List.of("tasks/get", "message/send", "message/stream", "tasks/resubscribe")) {
                DecodedRequest decoded = assertInstanceOf(DecodedRequest.class,
                        JSONRPCRequestDecoder_v0_3.decode(request(method, params)));
                assertEquals("original", decoded.requestId());
                assertEquals(method, decoded.methodName());
                assertEquals(method, decoded.request().getMethod());
                assertNull(decoded.request().getParams());
            }
        }
        for (String params : List.of("{\"message\":{\"kind\":\"message\",\"messageId\":\"m\",\"role\":\"invalid\",\"parts\":[{\"kind\":\"text\",\"text\":\"hello\"}]}}",
                "{\"message\":{\"kind\":\"message\",\"messageId\":\"m\",\"role\":\"user\",\"parts\":[{}]}}",
                "{\"message\":{\"kind\":\"message\",\"messageId\":\"m\",\"role\":\"user\",\"parts\":[{\"kind\":\"file\",\"file\":{}}]}}")) {
            assertFailure(request("message/send", params), "original", "message/send", InvalidParamsError_v0_3.class,
                    -32602, "Failed to parse JSON");
        }
    }

    @Test
    public void testStreamingDecodeFailureIsHttpJson() {
        for (String method : List.of("message/stream", "tasks/resubscribe")) {
            assertFailure(request(method, "[]"), "original", method, InvalidParamsError_v0_3.class,
                    -32602, "Failed to parse JSON");
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("supportedMethods")
    public void testSupportedMethodsDecode(String method, @Nullable String params,
            Class<? extends JSONRPCRequest_v0_3<?>> requestClass, boolean streaming) {
        DecodedRequest decoded = assertInstanceOf(DecodedRequest.class,
                JSONRPCRequestDecoder_v0_3.decode(request(method, params)));
        assertEquals("original", decoded.requestId());
        assertEquals(method, decoded.methodName());
        assertEquals(requestClass, decoded.request().getClass());
        assertEquals("original", decoded.request().getId());
        assertEquals(method, decoded.request().getMethod());
        assertEquals("2.0", decoded.request().getJsonrpc());
        if (streaming) {
            assertInstanceOf(StreamingJSONRPCRequest_v0_3.class, decoded.request());
        } else {
            assertInstanceOf(NonStreamingJSONRPCRequest_v0_3.class, decoded.request());
        }
        if (params != null) {
            assertNotNull(decoded.request().getParams());
        } else {
            assertNull(decoded.request().getParams());
        }
    }

    private static Stream<Arguments> supportedMethods() {
        return Stream.of(
                Arguments.of("tasks/get", "{\"id\":\"task\"}", GetTaskRequest_v0_3.class, false),
                Arguments.of("tasks/cancel", "{\"id\":\"task\"}", CancelTaskRequest_v0_3.class, false),
                Arguments.of("message/send", MESSAGE_PARAMS, SendMessageRequest_v0_3.class, false),
                Arguments.of("tasks/pushNotificationConfig/set", "{\"taskId\":\"task\",\"pushNotificationConfig\":{\"url\":\"http://localhost/callback\"}}", SetTaskPushNotificationConfigRequest_v0_3.class, false),
                Arguments.of("tasks/pushNotificationConfig/get", "{\"id\":\"task\"}", GetTaskPushNotificationConfigRequest_v0_3.class, false),
                Arguments.of("tasks/pushNotificationConfig/list", "{\"id\":\"task\"}", ListTaskPushNotificationConfigRequest_v0_3.class, false),
                Arguments.of("tasks/pushNotificationConfig/delete", "{\"id\":\"task\",\"pushNotificationConfigId\":\"config\"}", DeleteTaskPushNotificationConfigRequest_v0_3.class, false),
                Arguments.of("agent/getAuthenticatedExtendedCard", null, GetAuthenticatedExtendedCardRequest_v0_3.class, false),
                Arguments.of("message/stream", MESSAGE_PARAMS, SendStreamingMessageRequest_v0_3.class, true),
                Arguments.of("tasks/resubscribe", "{\"id\":\"task\"}", TaskResubscriptionRequest_v0_3.class, true));
    }

    @Test
    public void testExistingIdCoercion() {
        Object[][] cases = {{"\"request\"", "request", "request"}, {"7", 7L, 7L}, {"7.9", 7L, 7.9},
                {"true", "true", true}, {"null", null, null}, {null, null, null}};
        for (Object[] entry : cases) {
            String id = entry[0] == null ? "" : "\"id\":" + entry[0] + ",";
            assertFailure("{" + id + "\"jsonrpc\":\"wrong\"}", entry[1], null,
                    InvalidRequestError_v0_3.class, -32600,
                    "Invalid JSON-RPC request: missing or invalid 'jsonrpc' field");
            DecodedRequest decoded = assertInstanceOf(DecodedRequest.class, JSONRPCRequestDecoder_v0_3.decode(
                    "{" + id + "\"jsonrpc\":\"2.0\",\"method\":\"tasks/get\",\"params\":{\"id\":\"task\"}}"));
            assertEquals(entry[1], decoded.requestId());
            assertEquals(entry[2], decoded.request().getId());
        }
    }

    private static String request(String method, @Nullable String params) {
        return "{\"jsonrpc\":\"2.0\",\"id\":\"original\",\"method\":\"" + method + "\""
                + (params == null ? "" : ",\"params\":" + params) + "}";
    }

    private static void assertFailure(String body, @Nullable Object id, @Nullable String method,
            Class<? extends JSONRPCError_v0_3> errorClass, int code, String message) {
        DecodeFailure failure = assertInstanceOf(DecodeFailure.class, JSONRPCRequestDecoder_v0_3.decode(body));
        assertEquals(id, failure.requestId());
        assertEquals(method, failure.methodName());
        assertEquals(errorClass, failure.error().getClass());
        assertEquals(code, failure.error().getCode());
        assertEquals(message, failure.error().getMessage());
        assertNull(failure.error().getData());
    }
}
