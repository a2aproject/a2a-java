package org.a2aproject.sdk.server.rest.quarkus;

import static org.a2aproject.sdk.common.MediaType.APPLICATION_JSON;
import static org.a2aproject.sdk.spec.A2AMethods.CANCEL_TASK_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.GET_TASK_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.SEND_MESSAGE_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.SEND_STREAMING_MESSAGE_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.SUBSCRIBE_TO_TASK_METHOD;
import static org.a2aproject.sdk.transport.rest.context.RestContextKeys.METHOD_NAME_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import jakarta.enterprise.inject.Instance;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.server.AgentCardCacheMetadata;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.config.DefaultValuesConfigProvider;
import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentExtension;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.ContentTypeNotSupportedError;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsParams;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsResult;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.transport.rest.handler.RestHandler.HTTPRestResponse;
import org.a2aproject.sdk.transport.rest.handler.RestHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit test for A2AServerRoutes that verifies the method names are properly set
 * in the ServerCallContext for all route handlers.
 */
public class A2AServerRoutesTest {

    private A2AServerRoutes routes;
    private RestHandler mockRestHandler;
    private Executor mockExecutor;
    private Instance<CallContextFactory> mockCallContextFactory;
    private RoutingContext mockRoutingContext;
    private HttpServerRequest mockRequest;
    private HttpServerResponse mockResponse;
    private MultiMap mockHeaders;
    private MultiMap mockParams;
    private RequestBody mockRequestBody;

    @BeforeEach
    public void setUp() {
        routes = new A2AServerRoutes();
        mockRestHandler = mock(RestHandler.class);
        mockExecutor = mock(Executor.class);
        mockCallContextFactory = mock(Instance.class);
        mockRoutingContext = mock(RoutingContext.class);
        mockRequest = mock(HttpServerRequest.class);
        mockResponse = mock(HttpServerResponse.class);
        mockHeaders = MultiMap.caseInsensitiveMultiMap();
        mockParams = MultiMap.caseInsensitiveMultiMap();
        mockRequestBody = mock(RequestBody.class);

        // Inject mocks via reflection since we can't use @InjectMocks
        setField(routes, "jsonRestHandler", mockRestHandler);
        setField(routes, "executor", mockExecutor);
        setField(routes, "callContextFactory", mockCallContextFactory);

        // Setup common mock behavior
        when(mockCallContextFactory.isUnsatisfied()).thenReturn(true);
        when(mockRoutingContext.request()).thenReturn(mockRequest);
        when(mockRoutingContext.response()).thenReturn(mockResponse);
        when(mockRoutingContext.user()).thenReturn(null);
        when(mockRequest.headers()).thenReturn(mockHeaders);
        when(mockRequest.params()).thenReturn(mockParams);
        when(mockRequest.getHeader(any(CharSequence.class))).thenReturn(APPLICATION_JSON);
        when(mockRoutingContext.body()).thenReturn(mockRequestBody);
        when(mockRequestBody.asString()).thenReturn("{}");
        when(mockResponse.setStatusCode(any(Integer.class))).thenReturn(mockResponse);
        when(mockResponse.putHeader(any(CharSequence.class), any(CharSequence.class))).thenReturn(mockResponse);
        when(mockResponse.end()).thenReturn(Future.succeededFuture());
        when(mockResponse.end(anyString())).thenReturn(Future.succeededFuture());
    }

    @Test
    public void testSendMessage_MethodNameSetInContext() {
        // Arrange
        HTTPRestResponse mockHttpResponse = mock(HTTPRestResponse.class);
        when(mockHttpResponse.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getContentType()).thenReturn(APPLICATION_JSON);
        when(mockHttpResponse.getBody()).thenReturn("{}");
        when(mockRestHandler.sendMessage(any(ServerCallContext.class), anyString(), anyString())).thenReturn(mockHttpResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.sendMessage("{}", mockRoutingContext);

        // Assert
        verify(mockRestHandler).sendMessage(contextCaptor.capture(), anyString(), eq("{}"));
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(SEND_MESSAGE_METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
    }

    @Test
    public void testSendMessageStreaming_MethodNameSetInContext() {
        // Arrange
        HTTPRestResponse mockHttpResponse = mock(HTTPRestResponse.class);
        when(mockHttpResponse.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getContentType()).thenReturn(APPLICATION_JSON);
        when(mockHttpResponse.getBody()).thenReturn("{}");
        when(mockRestHandler.sendStreamingMessage(any(ServerCallContext.class), anyString(), anyString()))
                .thenReturn(mockHttpResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.sendMessageStreaming("{}", mockRoutingContext);

        // Assert
        verify(mockRestHandler).sendStreamingMessage(contextCaptor.capture(), anyString(), eq("{}"));
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(SEND_STREAMING_MESSAGE_METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
    }

    @Test
    public void testGetTask_MethodNameSetInContext() {
        // Arrange
        when(mockRoutingContext.pathParam("taskId")).thenReturn("task123");
        HTTPRestResponse mockHttpResponse = mock(HTTPRestResponse.class);
        when(mockHttpResponse.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getContentType()).thenReturn(APPLICATION_JSON);
        when(mockHttpResponse.getBody()).thenReturn("{test:value}");
        when(mockRestHandler.getTask(any(ServerCallContext.class), anyString(), anyString(), any())).thenReturn(mockHttpResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.getTask(mockRoutingContext);

        // Assert
        verify(mockRestHandler).getTask(contextCaptor.capture(), anyString(), eq("task123"), any());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(GET_TASK_METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
    }

    @Test
    public void testCancelTask_MethodNameSetInContext() {
        // Arrange
        when(mockRoutingContext.pathParam("taskId")).thenReturn("task123");
        HTTPRestResponse mockHttpResponse = mock(HTTPRestResponse.class);
        when(mockHttpResponse.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getContentType()).thenReturn(APPLICATION_JSON);
        when(mockHttpResponse.getBody()).thenReturn("{}");
        when(mockRestHandler.cancelTask(any(ServerCallContext.class), anyString(), anyString(), anyString())).thenReturn(mockHttpResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.cancelTask("{\"id\":\"task123\"}", mockRoutingContext);

        // Assert
        verify(mockRestHandler).cancelTask(contextCaptor.capture(), anyString(), eq("{\"id\":\"task123\"}"), eq("task123"));
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(CANCEL_TASK_METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
    }

    @Test
    public void testCancelTask_WithMetadata() {
        // Arrange
        when(mockRoutingContext.pathParam("taskId")).thenReturn("task456");
        HTTPRestResponse mockHttpResponse = mock(HTTPRestResponse.class);
        when(mockHttpResponse.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getContentType()).thenReturn(APPLICATION_JSON);
        when(mockHttpResponse.getBody()).thenReturn("{\"id\":\"task456\",\"status\":\"cancelled\"}");

        String requestBody = """
            {
                "metadata": {
                    "reason": "user_requested",
                    "source": "web_ui",
                    "priority": "high"
                }
            }
            """;

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        when(mockRestHandler.cancelTask(any(ServerCallContext.class), anyString(), anyString(), anyString()))
                .thenReturn(mockHttpResponse);

        // Act
        routes.cancelTask(requestBody, mockRoutingContext);

        // Assert
        verify(mockRestHandler).cancelTask(any(ServerCallContext.class), anyString(), bodyCaptor.capture(), eq("task456"));
        String capturedBody = bodyCaptor.getValue();
        assertNotNull(capturedBody);
        assertEquals(requestBody, capturedBody);
    }

    @Test
    public void testCancelTask_WithEmptyMetadata() {
        // Arrange
        when(mockRoutingContext.pathParam("taskId")).thenReturn("task789");
        HTTPRestResponse mockHttpResponse = mock(HTTPRestResponse.class);
        when(mockHttpResponse.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getContentType()).thenReturn(APPLICATION_JSON);
        when(mockHttpResponse.getBody()).thenReturn("{\"id\":\"task789\"}");

        String requestBody = """
            {
                "metadata": {}
            }
            """;

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        when(mockRestHandler.cancelTask(any(ServerCallContext.class), anyString(), anyString(), anyString()))
                .thenReturn(mockHttpResponse);

        // Act
        routes.cancelTask(requestBody, mockRoutingContext);

        // Assert
        verify(mockRestHandler).cancelTask(any(ServerCallContext.class), anyString(), bodyCaptor.capture(), eq("task789"));
        String capturedBody = bodyCaptor.getValue();
        assertNotNull(capturedBody);
        assertEquals(requestBody, capturedBody);
    }

    @Test
    public void testCancelTask_WithNoMetadataField() {
        // Arrange
        when(mockRoutingContext.pathParam("taskId")).thenReturn("task999");
        HTTPRestResponse mockHttpResponse = mock(HTTPRestResponse.class);
        when(mockHttpResponse.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getContentType()).thenReturn(APPLICATION_JSON);
        when(mockHttpResponse.getBody()).thenReturn("{\"id\":\"task999\"}");

        String requestBody = "{}";

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        when(mockRestHandler.cancelTask(any(ServerCallContext.class), anyString(), anyString(), anyString()))
                .thenReturn(mockHttpResponse);

        // Act
        routes.cancelTask(requestBody, mockRoutingContext);

        // Assert
        verify(mockRestHandler).cancelTask(any(ServerCallContext.class), anyString(), bodyCaptor.capture(), eq("task999"));
        String capturedBody = bodyCaptor.getValue();
        assertNotNull(capturedBody);
        assertEquals(requestBody, capturedBody);
    }

    @Test
    public void testCancelTask_WithNullBody() {
        // Arrange
        when(mockRoutingContext.pathParam("taskId")).thenReturn("task111");
        HTTPRestResponse mockHttpResponse = mock(HTTPRestResponse.class);
        when(mockHttpResponse.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getContentType()).thenReturn(APPLICATION_JSON);
        when(mockHttpResponse.getBody()).thenReturn("{\"id\":\"task111\"}");

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        when(mockRestHandler.cancelTask(any(ServerCallContext.class), anyString(), anyString(), anyString()))
                .thenReturn(mockHttpResponse);

        // Act
        routes.cancelTask(null, mockRoutingContext);

        // Assert
        verify(mockRestHandler).cancelTask(any(ServerCallContext.class), anyString(), bodyCaptor.capture(), eq("task111"));
        String capturedBody = bodyCaptor.getValue();
        assertNull(capturedBody);
    }

    @Test
    public void testCancelTask_WithComplexMetadata() {
        // Arrange
        when(mockRoutingContext.pathParam("taskId")).thenReturn("task222");
        HTTPRestResponse mockHttpResponse = mock(HTTPRestResponse.class);
        when(mockHttpResponse.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getContentType()).thenReturn(APPLICATION_JSON);
        when(mockHttpResponse.getBody()).thenReturn("{\"id\":\"task222\"}");

        String requestBody = """
            {
                "metadata": {
                    "cancellation_reason": "timeout",
                    "retry_count": 3,
                    "user_id": "user_abc123",
                    "timestamp": "2024-01-01T00:00:00Z",
                    "nested": {
                        "key1": "value1",
                        "key2": "value2"
                    }
                }
            }
            """;

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        when(mockRestHandler.cancelTask(any(ServerCallContext.class), anyString(), anyString(), anyString()))
                .thenReturn(mockHttpResponse);

        // Act
        routes.cancelTask(requestBody, mockRoutingContext);

        // Assert
        verify(mockRestHandler).cancelTask(any(ServerCallContext.class), anyString(), bodyCaptor.capture(), eq("task222"));
        String capturedBody = bodyCaptor.getValue();
        assertNotNull(capturedBody);
        assertEquals(requestBody, capturedBody);
    }

    @Test
    public void testSubscribeTask_MethodNameSetInContext() {
        // Arrange
        when(mockRoutingContext.pathParam("taskId")).thenReturn("task123");
        HTTPRestResponse mockHttpResponse = mock(HTTPRestResponse.class);
        when(mockHttpResponse.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getContentType()).thenReturn(APPLICATION_JSON);
        when(mockHttpResponse.getBody()).thenReturn("{}");
        when(mockRestHandler.subscribeToTask(any(ServerCallContext.class), anyString(), anyString()))
                .thenReturn(mockHttpResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.subscribeToTask(mockRoutingContext);

        // Assert
        verify(mockRestHandler).subscribeToTask(contextCaptor.capture(), anyString(), eq("task123"));
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(SUBSCRIBE_TO_TASK_METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
    }

    @Test
    public void testCreateTaskPushNotificationConfiguration_MethodNameSetInContext() {
        // Arrange
        when(mockRoutingContext.pathParam("taskId")).thenReturn("task123");
        HTTPRestResponse mockHttpResponse = mock(HTTPRestResponse.class);
        when(mockHttpResponse.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getContentType()).thenReturn(APPLICATION_JSON);
        when(mockHttpResponse.getBody()).thenReturn("{}");
        when(mockRestHandler.createTaskPushNotificationConfiguration(any(ServerCallContext.class), anyString(), anyString(), anyString())).thenReturn(mockHttpResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.createTaskPushNotificationConfiguration("{}", mockRoutingContext);

        // Assert
        verify(mockRestHandler).createTaskPushNotificationConfiguration(contextCaptor.capture(), anyString(), eq("{}"), eq("task123"));
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
    }

    @Test
    public void testGetTaskPushNotificationConfiguration_MethodNameSetInContext() {
        // Arrange
        when(mockRoutingContext.pathParam("taskId")).thenReturn("task123");
        when(mockRoutingContext.pathParam("configId")).thenReturn("config456");
        HTTPRestResponse mockHttpResponse = mock(HTTPRestResponse.class);
        when(mockHttpResponse.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getContentType()).thenReturn(APPLICATION_JSON);
        when(mockHttpResponse.getBody()).thenReturn("{}");
        when(mockRestHandler.getTaskPushNotificationConfiguration(any(ServerCallContext.class), anyString(), anyString(), anyString())).thenReturn(mockHttpResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.getTaskPushNotificationConfiguration(mockRoutingContext);

        // Assert
        verify(mockRestHandler).getTaskPushNotificationConfiguration(contextCaptor.capture(), anyString(), eq("task123"),
                eq("config456"));
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
    }

    @Test
    public void testListTaskPushNotificationConfigurations_MethodNameSetInContext() {
        // Arrange
        when(mockRoutingContext.pathParam("taskId")).thenReturn("task123");
        HTTPRestResponse mockHttpResponse = mock(HTTPRestResponse.class);
        when(mockHttpResponse.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getContentType()).thenReturn(APPLICATION_JSON);
        when(mockHttpResponse.getBody()).thenReturn("{}");
        when(mockRestHandler.listTaskPushNotificationConfigurations(any(ServerCallContext.class), anyString(), anyString(), any(), any()))
                .thenReturn(mockHttpResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.listTaskPushNotificationConfigurations(mockRoutingContext);

        // Assert
        verify(mockRestHandler).listTaskPushNotificationConfigurations(contextCaptor.capture(), anyString(), eq("task123"), any(), any());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
    }

    @Test
    public void testDeleteTaskPushNotificationConfiguration_MethodNameSetInContext() {
        // Arrange
        when(mockRoutingContext.pathParam("taskId")).thenReturn("task123");
        when(mockRoutingContext.pathParam("configId")).thenReturn("config456");
        HTTPRestResponse mockHttpResponse = mock(HTTPRestResponse.class);
        when(mockHttpResponse.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getContentType()).thenReturn(APPLICATION_JSON);
        when(mockHttpResponse.getBody()).thenReturn("{}");
        when(mockRestHandler.deleteTaskPushNotificationConfiguration(any(ServerCallContext.class), anyString(), anyString(), anyString())).thenReturn(mockHttpResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.deleteTaskPushNotificationConfiguration(mockRoutingContext);

        // Assert
        verify(mockRestHandler).deleteTaskPushNotificationConfiguration(contextCaptor.capture(), anyString(), eq("task123"),
                eq("config456"));
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
    }

    @Test
    public void testSendMessage_UnsupportedContentType_ReturnsContentTypeNotSupportedError() {
        // Arrange
        HTTPRestResponse mockErrorResponse = mock(HTTPRestResponse.class);
        when(mockErrorResponse.getStatusCode()).thenReturn(415);
        when(mockErrorResponse.getContentType()).thenReturn(APPLICATION_JSON);
        when(mockErrorResponse.getBody()).thenReturn("{\"error\":{\"code\":415,\"status\":\"INVALID_ARGUMENT\",\"message\":\"Incompatible content types\",\"details\":[{\"reason\":\"CONTENT_TYPE_NOT_SUPPORTED\",\"domain\":\"a2a-protocol.org\"}]}}");
        when(mockRestHandler.createErrorResponse(any(ContentTypeNotSupportedError.class))).thenReturn(mockErrorResponse);
        when(mockRequest.getHeader(any(CharSequence.class))).thenReturn("text/plain");

        // Act
        routes.sendMessage("{}", mockRoutingContext);

        // Assert: createErrorResponse called with ContentTypeNotSupportedError, sendMessage NOT called
        verify(mockRestHandler).createErrorResponse(any(ContentTypeNotSupportedError.class));
        verify(mockRestHandler, never()).sendMessage(any(ServerCallContext.class), anyString(), anyString());
    }

    @Test
    public void testSendMessageStreaming_UnsupportedContentType_ReturnsContentTypeNotSupportedError() {
        // Arrange
        HTTPRestResponse mockErrorResponse = mock(HTTPRestResponse.class);
        when(mockErrorResponse.getStatusCode()).thenReturn(415);
        when(mockErrorResponse.getContentType()).thenReturn(APPLICATION_JSON);
        when(mockErrorResponse.getBody()).thenReturn("{\"error\":{\"code\":415,\"status\":\"INVALID_ARGUMENT\",\"message\":\"Incompatible content types\",\"details\":[{\"reason\":\"CONTENT_TYPE_NOT_SUPPORTED\",\"domain\":\"a2a-protocol.org\"}]}}");
        when(mockRestHandler.createErrorResponse(any(ContentTypeNotSupportedError.class))).thenReturn(mockErrorResponse);
        when(mockRequest.getHeader(any(CharSequence.class))).thenReturn("text/plain");

        // Act
        routes.sendMessageStreaming("{}", mockRoutingContext);

        // Assert: createErrorResponse called with ContentTypeNotSupportedError, sendStreamingMessage NOT called
        verify(mockRestHandler).createErrorResponse(any(ContentTypeNotSupportedError.class));
        verify(mockRestHandler, never()).sendStreamingMessage(any(ServerCallContext.class), anyString(), anyString());
    }

    @Test
    public void testSendMessage_UnsupportedProtocolVersion_ReturnsVersionNotSupportedError() {
        // Arrange: content type is OK, but RestHandler returns a VersionNotSupportedError response
        HTTPRestResponse mockErrorResponse = mock(HTTPRestResponse.class);
        when(mockErrorResponse.getStatusCode()).thenReturn(400);
        when(mockErrorResponse.getContentType()).thenReturn(APPLICATION_JSON);
        when(mockErrorResponse.getBody()).thenReturn("{\"error\":{\"code\":400,\"status\":\"UNIMPLEMENTED\",\"message\":\"Protocol version not supported\",\"details\":[{\"reason\":\"VERSION_NOT_SUPPORTED\",\"domain\":\"a2a-protocol.org\"}]}}");
        when(mockRequest.getHeader(any(CharSequence.class))).thenReturn(APPLICATION_JSON);
        when(mockRestHandler.sendMessage(any(ServerCallContext.class), anyString(), anyString()))
                .thenReturn(mockErrorResponse);

        // Act
        routes.sendMessage("{}", mockRoutingContext);

        // Assert: sendMessage was called and error response forwarded
        verify(mockRestHandler).sendMessage(any(ServerCallContext.class), anyString(), eq("{}"));
        verify(mockResponse).setStatusCode(400);
    }

    /**
     * Helper method to set a field via reflection for testing purposes.
     */
    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
    private RequestHandler validationHandler;

    private void validationFixture(String version, boolean extension, boolean capabilities) {
        setUp();
        validationHandler = mock(RequestHandler.class);
        AgentCard card = AgentCard.builder().name("validation").description("Validation baseline").version("1")
                .supportedInterfaces(List.of(new AgentInterface("HTTP+JSON", "http://localhost:9999")))
                .capabilities(AgentCapabilities.builder().streaming(capabilities).pushNotifications(capabilities)
                        .extensions(extension ? List.of(AgentExtension.builder().uri("urn:test:required")
                                .required(true).build()) : List.of()).build())
                .defaultInputModes(List.of("text")).defaultOutputModes(List.of("text")).skills(List.of()).build();
        setField(routes, "jsonRestHandler", new RestHandler(card,
                new AgentCardCacheMetadata(card, new DefaultValuesConfigProvider()), validationHandler, Runnable::run));
        mockHeaders.set("A2A-Version", version);
        when(mockRequest.getHeader(anyString())).thenAnswer(invocation -> mockHeaders.get(invocation.getArgument(0).toString()));
        when(mockRoutingContext.pathParam("taskId")).thenReturn("task");
        when(mockRoutingContext.pathParam("configId")).thenReturn("config");
    }

    private void assertValidationError(int status, String reason, String message) {
        assertValidationError(status, reason, message, new JsonObject());
    }

    private void assertValidationError(int status, String reason, String message, JsonObject metadata) {
        verify(mockResponse).setStatusCode(status);
        verify(mockResponse).putHeader(io.vertx.core.http.HttpHeaders.CONTENT_TYPE, APPLICATION_JSON);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(mockResponse).end(body.capture());
        JsonObject expected = JsonParser.parseString("""
                {"error":{"code":422,"status":"INVALID_ARGUMENT","message":"", "details":[{
                "@type":"type.googleapis.com/google.rpc.ErrorInfo","reason":"INVALID_PARAMS",
                "domain":"a2a-protocol.org","metadata":{}}]}}
                """).getAsJsonObject();
        JsonObject error = expected.getAsJsonObject("error");
        error.addProperty("code", status);
        error.addProperty("status", switch (reason) {
            case "VERSION_NOT_SUPPORTED", "PUSH_NOTIFICATION_NOT_SUPPORTED", "UNSUPPORTED_OPERATION" -> "UNIMPLEMENTED";
            case "EXTENSION_SUPPORT_REQUIRED" -> "FAILED_PRECONDITION";
            default -> "INVALID_ARGUMENT";
        });
        error.addProperty("message", message);
        JsonObject info = error.getAsJsonArray("details").get(0).getAsJsonObject();
        info.addProperty("reason", reason);
        info.add("metadata", metadata);
        assertEquals(expected, JsonParser.parseString(body.getValue()));
        verifyNoMoreInteractions(mockResponse);
        verifyNoInteractions(validationHandler);
    }

    private void assertBadTaskPath(Consumer<RoutingContext> operation) {
        for (String taskId : new String[] {null, ""}) {
            for (int conflict = 0; conflict < 4; conflict++) {
                validationFixture(conflict == 1 ? "2.0" : "1.0", conflict == 2, conflict != 3);
                when(mockRoutingContext.pathParam("taskId")).thenReturn(taskId);
                when(mockRoutingContext.pathParam("configId")).thenReturn("");
                mockParams.set("historyLength", "bad").set("pageSize", "bad");
                operation.accept(mockRoutingContext);
                assertValidationError(422, "INVALID_PARAMS", "bad task id");
            }
        }
    }

    @Test
    public void testGetTaskRawInputValidationOrder() {
        assertBadTaskPath(rc -> routes.getTask(rc));
        for (String history : new String[] {"", "bad", "2147483648"}) {
            for (int conflict = 0; conflict < 3; conflict++) {
                validationFixture(conflict == 1 ? "2.0" : "1.0", conflict == 2, true);
                mockParams.set("historyLength", history);
                routes.getTask(mockRoutingContext);
                assertValidationError(422, "INVALID_PARAMS", "bad historyLength");
            }
        }
        validationFixture("2.0", false, true);
        mockParams.set("historyLength", "-1");
        routes.getTask(mockRoutingContext);
        assertValidationError(400, "VERSION_NOT_SUPPORTED",
                "Protocol version '2.0' is not supported. Supported versions: [1.0]");
        validationFixture("1.0", false, true);
        mockParams.set("historyLength", "-1");
        routes.getTask(mockRoutingContext);
        assertValidationError(422, "INVALID_PARAMS", "Invalid history length");
    }

    @Test
    public void testListTasksRawQueryValidationOrder() {
        for (String field : List.of("pageSize", "historyLength")) {
            for (int conflict = 0; conflict < 3; conflict++) {
                validationFixture(conflict == 1 ? "2.0" : "1.0", conflict == 2, true);
                mockParams.set(field, "bad").set("status", "invalid").set("statusTimestampAfter", "invalid");
                if (field.equals("pageSize")) {
                    mockParams.set("historyLength", "also-bad");
                }
                routes.listTasks(mockRoutingContext);
                assertValidationError(422, "INVALID_PARAMS", "Invalid number format in parameters");
            }
            validationFixture("2.0", false, true);
            mockParams.set(field, "-1");
            routes.listTasks(mockRoutingContext);
            assertValidationError(400, "VERSION_NOT_SUPPORTED",
                    "Protocol version '2.0' is not supported. Supported versions: [1.0]");
        }
        for (String raw : new String[] {null, "", "true", "TrUe", "false", "not-true", "1"}) {
            validationFixture("1.0", false, true);
            if (raw != null) {
                mockParams.set("includeArtifacts", raw).set("pageSize", "").set("historyLength", "");
            }
            mockParams.set("status", "task_state_submitted");
            when(validationHandler.onListTasks(any(), any())).thenReturn(new ListTasksResult(List.of()));
            routes.listTasks(mockRoutingContext);
            ArgumentCaptor<ListTasksParams> params = ArgumentCaptor.forClass(ListTasksParams.class);
            verify(validationHandler).onListTasks(params.capture(), any());
            assertEquals(TaskState.TASK_STATE_SUBMITTED, params.getValue().status());
            assertNull(params.getValue().pageSize());
            assertNull(params.getValue().historyLength());
            assertEquals(raw == null || raw.isEmpty() ? null : Boolean.valueOf(raw), params.getValue().includeArtifacts());
            verify(mockResponse).setStatusCode(200);
            verify(mockResponse).putHeader(io.vertx.core.http.HttpHeaders.CONTENT_TYPE, APPLICATION_JSON);
        }
        for (String field : List.of("status", "statusTimestampAfter")) {
            validationFixture("1.0", false, true);
            mockParams.set(field, "");
            routes.listTasks(mockRoutingContext);
            JsonObject metadata = new JsonObject();
            metadata.addProperty("parameter", field);
            metadata.addProperty("reason", field.equals("status")
                    ? "Must be one of: " + java.util.Arrays.stream(TaskState.values()).map(TaskState::name)
                            .collect(java.util.stream.Collectors.joining(", "))
                    : "Must be an ISO-8601 timestamp");
            assertValidationError(422, "INVALID_PARAMS", "Invalid params", metadata);
        }
    }

    @Test
    public void testCancelTaskPathValidationOrder() {
        assertBadTaskPath(rc -> routes.cancelTask("{broken", rc));
    }

    @Test
    public void testSubscribeToTaskPathValidationOrder() {
        assertBadTaskPath(rc -> routes.subscribeToTask(rc));
    }

    @Test
    public void testCreatePushConfigPathValidationOrder() {
        assertBadTaskPath(rc -> routes.createTaskPushNotificationConfiguration("{broken", rc));
    }

    @Test
    public void testGetPushConfigPathValidationOrder() {
        assertBadTaskPath(rc -> routes.getTaskPushNotificationConfiguration(rc));
        assertBadConfigPath(rc -> routes.getTaskPushNotificationConfiguration(rc), "bad configuration id");
    }

    @Test
    public void testDeletePushConfigPathValidationOrder() {
        assertBadTaskPath(rc -> routes.deleteTaskPushNotificationConfiguration(rc));
        assertBadConfigPath(rc -> routes.deleteTaskPushNotificationConfiguration(rc), "bad config id");
    }

    private void assertBadConfigPath(Consumer<RoutingContext> operation, String message) {
        for (String configId : new String[] {null, ""}) {
            for (int conflict = 0; conflict < 4; conflict++) {
                validationFixture(conflict == 1 ? "2.0" : "1.0", conflict == 2, conflict != 3);
                when(mockRoutingContext.pathParam("configId")).thenReturn(configId);
                operation.accept(mockRoutingContext);
                assertValidationError(422, "INVALID_PARAMS", message);
            }
        }
    }

    @Test
    public void testListPushConfigsRawInputValidationOrder() {
        assertBadTaskPath(rc -> routes.listTaskPushNotificationConfigurations(rc));
        for (String page : List.of("", "bad", "2147483648")) {
            for (int conflict = 0; conflict < 4; conflict++) {
                validationFixture(conflict == 1 ? "2.0" : "1.0", conflict == 2, conflict != 3);
                mockParams.set("pageSize", page);
                routes.listTaskPushNotificationConfigurations(mockRoutingContext);
                assertValidationError(422, "INVALID_PARAMS", "bad pageSize");
            }
        }
        for (String token : new String[] {null, ""}) {
            validationFixture("1.0", false, true);
            if (token != null) {
                mockParams.set("pageToken", token);
            }
            when(validationHandler.onListTaskPushNotificationConfigs(any(), any()))
                    .thenReturn(new ListTaskPushNotificationConfigsResult(List.of()));
            routes.listTaskPushNotificationConfigurations(mockRoutingContext);
            ArgumentCaptor<ListTaskPushNotificationConfigsParams> params =
                    ArgumentCaptor.forClass(ListTaskPushNotificationConfigsParams.class);
            verify(validationHandler).onListTaskPushNotificationConfigs(params.capture(), any());
            assertEquals(0, params.getValue().pageSize());
            assertEquals("", params.getValue().pageToken());
            verify(mockResponse).setStatusCode(200);
        }
    }

    @Test
    public void testCancelContentTypeBeforePathValidation() {
        validationFixture("2.0", true, false);
        when(mockRoutingContext.pathParam("taskId")).thenReturn("");
        when(mockRequest.getHeader(any(CharSequence.class))).thenReturn("text/plain");
        routes.cancelTask("{broken", mockRoutingContext);
        assertValidationError(415, "CONTENT_TYPE_NOT_SUPPORTED", "Incompatible content types");
    }

    @Test
    public void testCreatePushContentTypeBeforePathValidation() {
        validationFixture("2.0", true, false);
        when(mockRoutingContext.pathParam("taskId")).thenReturn("");
        when(mockRequest.getHeader(any(CharSequence.class))).thenReturn("text/plain");
        routes.createTaskPushNotificationConfiguration("{broken", mockRoutingContext);
        assertValidationError(415, "CONTENT_TYPE_NOT_SUPPORTED", "Incompatible content types");
    }

    @Test
    public void testRawPathAndQueryValuesForwardedForAllOperations() {
        for (String taskId : new String[] {null, "", "task"}) {
            for (String configId : new String[] {null, "", "config"}) {
                for (String raw : new String[] {null, "", "bad", "2"}) {
                    for (int operation = 0; operation < 8; operation++) {
                        setUp();
                        when(mockRoutingContext.pathParam("taskId")).thenReturn(taskId);
                        when(mockRoutingContext.pathParam("configId")).thenReturn(configId);
                        if (raw != null) {
                            mockParams.set("historyLength", raw).set("pageSize", raw).set("pageToken", raw)
                                    .set("status", "task_state_submitted").set("includeArtifacts", raw)
                                    .set("contextId", raw).set("statusTimestampAfter", raw);
                        }
                        HTTPRestResponse error = new HTTPRestResponse(422, APPLICATION_JSON, "{\"transportError\":true}");
                        switch (operation) {
                            case 0 -> {
                                when(mockRestHandler.getTask(any(), anyString(), eq(taskId), eq(raw))).thenReturn(error);
                                routes.getTask(mockRoutingContext);
                                verify(mockRestHandler).getTask(any(), anyString(), eq(taskId), eq(raw));
                            }
                            case 1 -> {
                                String status = raw == null ? null : "task_state_submitted";
                                when(mockRestHandler.listTasks(any(), anyString(), eq(raw), eq(status), eq(raw), eq(raw),
                                        eq(raw), eq(raw), eq(raw))).thenReturn(error);
                                routes.listTasks(mockRoutingContext);
                                verify(mockRestHandler).listTasks(any(), anyString(), eq(raw), eq(status), eq(raw), eq(raw),
                                        eq(raw), eq(raw), eq(raw));
                            }
                            case 2 -> {
                                when(mockRestHandler.cancelTask(any(), anyString(), eq("{broken"), eq(taskId))).thenReturn(error);
                                routes.cancelTask("{broken", mockRoutingContext);
                                verify(mockRestHandler).cancelTask(any(), anyString(), eq("{broken"), eq(taskId));
                            }
                            case 3 -> {
                                when(mockRestHandler.subscribeToTask(any(), anyString(), eq(taskId))).thenReturn(error);
                                routes.subscribeToTask(mockRoutingContext);
                                verify(mockRestHandler).subscribeToTask(any(), anyString(), eq(taskId));
                            }
                            case 4 -> {
                                when(mockRestHandler.createTaskPushNotificationConfiguration(any(), anyString(), eq("{broken"), eq(taskId)))
                                        .thenReturn(error);
                                routes.createTaskPushNotificationConfiguration("{broken", mockRoutingContext);
                                verify(mockRestHandler).createTaskPushNotificationConfiguration(any(), anyString(), eq("{broken"), eq(taskId));
                            }
                            case 5 -> {
                                when(mockRestHandler.getTaskPushNotificationConfiguration(any(), anyString(), eq(taskId), eq(configId)))
                                        .thenReturn(error);
                                routes.getTaskPushNotificationConfiguration(mockRoutingContext);
                                verify(mockRestHandler).getTaskPushNotificationConfiguration(any(), anyString(), eq(taskId), eq(configId));
                            }
                            case 6 -> {
                                when(mockRestHandler.listTaskPushNotificationConfigurations(any(), anyString(), eq(taskId), eq(raw), eq(raw)))
                                        .thenReturn(error);
                                routes.listTaskPushNotificationConfigurations(mockRoutingContext);
                                verify(mockRestHandler).listTaskPushNotificationConfigurations(any(), anyString(), eq(taskId), eq(raw), eq(raw));
                            }
                            case 7 -> {
                                when(mockRestHandler.deleteTaskPushNotificationConfiguration(any(), anyString(), eq(taskId), eq(configId)))
                                        .thenReturn(error);
                                routes.deleteTaskPushNotificationConfiguration(mockRoutingContext);
                                verify(mockRestHandler).deleteTaskPushNotificationConfiguration(any(), anyString(), eq(taskId), eq(configId));
                            }
                            default -> throw new AssertionError(operation);
                        }
                        verifyNoMoreInteractions(mockRestHandler);
                        verify(mockResponse).setStatusCode(422);
                        verify(mockResponse).putHeader(io.vertx.core.http.HttpHeaders.CONTENT_TYPE, APPLICATION_JSON);
                        verify(mockResponse).end(error.getBody());
                        verifyNoMoreInteractions(mockResponse);
                    }
                }
            }
        }
    }

}
