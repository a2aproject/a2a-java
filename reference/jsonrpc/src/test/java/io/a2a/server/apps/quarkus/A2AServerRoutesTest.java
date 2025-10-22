package io.a2a.server.apps.quarkus;

import static io.a2a.transport.jsonrpc.context.JSONRPCContextKeys.METHOD_NAME_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

import jakarta.enterprise.inject.Instance;

import io.a2a.server.ServerCallContext;
import io.a2a.spec.CancelTaskRequest;
import io.a2a.spec.CancelTaskResponse;
import io.a2a.spec.DeleteTaskPushNotificationConfigRequest;
import io.a2a.spec.DeleteTaskPushNotificationConfigResponse;
import io.a2a.spec.GetAuthenticatedExtendedCardRequest;
import io.a2a.spec.GetAuthenticatedExtendedCardResponse;
import io.a2a.spec.GetTaskPushNotificationConfigRequest;
import io.a2a.spec.GetTaskPushNotificationConfigResponse;
import io.a2a.spec.GetTaskRequest;
import io.a2a.spec.GetTaskResponse;
import io.a2a.spec.ListTaskPushNotificationConfigRequest;
import io.a2a.spec.ListTaskPushNotificationConfigResponse;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendMessageResponse;
import io.a2a.spec.SendStreamingMessageRequest;
import io.a2a.spec.SendStreamingMessageResponse;
import io.a2a.spec.SetTaskPushNotificationConfigRequest;
import io.a2a.spec.SetTaskPushNotificationConfigResponse;
import io.a2a.spec.TaskResubscriptionRequest;
import io.a2a.transport.jsonrpc.handler.JSONRPCHandler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit test for JSON-RPC A2AServerRoutes that verifies the method names are properly set
 * in the ServerCallContext for all request types.
 */
public class A2AServerRoutesTest {

    private A2AServerRoutes routes;
    private JSONRPCHandler mockJsonRpcHandler;
    private Executor mockExecutor;
    private Instance<CallContextFactory> mockCallContextFactory;
    private RoutingContext mockRoutingContext;
    private HttpServerRequest mockRequest;
    private HttpServerResponse mockHttpResponse;
    private MultiMap mockHeaders;
    private RequestBody mockRequestBody;

    @BeforeEach
    public void setUp() {
        routes = new A2AServerRoutes();
        mockJsonRpcHandler = mock(JSONRPCHandler.class);
        mockExecutor = mock(Executor.class);
        mockCallContextFactory = mock(Instance.class);
        mockRoutingContext = mock(RoutingContext.class);
        mockRequest = mock(HttpServerRequest.class);
        mockHttpResponse = mock(HttpServerResponse.class);
        mockHeaders = MultiMap.caseInsensitiveMultiMap();
        mockRequestBody = mock(RequestBody.class);

        // Inject mocks via reflection since we can't use @InjectMocks
        setField(routes, "jsonRpcHandler", mockJsonRpcHandler);
        setField(routes, "executor", mockExecutor);
        setField(routes, "callContextFactory", mockCallContextFactory);

        // Setup common mock behavior
        when(mockCallContextFactory.isUnsatisfied()).thenReturn(true);
        when(mockRoutingContext.request()).thenReturn(mockRequest);
        when(mockRoutingContext.response()).thenReturn(mockHttpResponse);
        when(mockRoutingContext.user()).thenReturn(null);
        when(mockRequest.headers()).thenReturn(mockHeaders);
        when(mockRoutingContext.body()).thenReturn(mockRequestBody);

        // Chain the response methods properly
        when(mockHttpResponse.setStatusCode(any(Integer.class))).thenReturn(mockHttpResponse);
        when(mockHttpResponse.putHeader(any(CharSequence.class), any(CharSequence.class))).thenReturn(mockHttpResponse);
        when(mockHttpResponse.end(anyString())).thenReturn(null);
        when(mockHttpResponse.setChunked(any(Boolean.class))).thenReturn(mockHttpResponse);
        when(mockHttpResponse.headers()).thenReturn(mockHeaders);
    }

    @Test
    public void testSendMessage_MethodNameSetInContext() {
        // Arrange - using valid JSON from JsonMessages test file
        String jsonRpcRequest = """
            {
             "jsonrpc": "2.0",
             "method": "message/send",
             "params": {
              "message": {
               "role": "user",
               "parts": [
                {
                 "kind": "text",
                 "text": "tell me a joke"
                }
               ],
               "messageId": "message-1234",
               "contextId": "context-1234",
               "kind": "message"
              },
              "configuration": {
                "acceptedOutputModes": ["text"],
                "blocking": true
              }
             }
            }""";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        SendMessageResponse mockResponse = mock(SendMessageResponse.class);
        when(mockJsonRpcHandler.onMessageSend(any(SendMessageRequest.class), any(ServerCallContext.class)))
                .thenReturn(mockResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        // Assert
        verify(mockJsonRpcHandler).onMessageSend(any(SendMessageRequest.class), contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(SendMessageRequest.METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
    }

    @Test
    public void testSendStreamingMessage_MethodNameSetInContext() {
        // Arrange - using the same valid format as testSendMessage
        String jsonRpcRequest = """
            {
             "jsonrpc": "2.0",
             "method": "message/stream",
             "params": {
              "message": {
               "role": "user",
               "parts": [
                {
                 "kind": "text",
                 "text": "tell me a joke"
                }
               ],
               "messageId": "message-1234",
               "contextId": "context-1234",
               "kind": "message"
              },
              "configuration": {
                "acceptedOutputModes": ["text"],
                "blocking": true
              }
             }
            }""";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        @SuppressWarnings("unchecked")
        Flow.Publisher<SendStreamingMessageResponse> mockPublisher = mock(Flow.Publisher.class);
        when(mockJsonRpcHandler.onMessageSendStream(any(SendStreamingMessageRequest.class),
                any(ServerCallContext.class))).thenReturn(mockPublisher);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        // Assert
        verify(mockJsonRpcHandler).onMessageSendStream(any(SendStreamingMessageRequest.class),
                contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(SendStreamingMessageRequest.METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
    }

    @Test
    public void testGetTask_MethodNameSetInContext() {
        // Arrange - based on GET_TASK_TEST_REQUEST from JsonMessages
        String jsonRpcRequest = """
            {
             "jsonrpc": "2.0",
             "method": "tasks/get",
             "params": {
              "id": "de38c76d-d54c-436c-8b9f-4c2703648d64",
              "historyLength": 10
             }
            }""";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        GetTaskResponse mockResponse = mock(GetTaskResponse.class);
        when(mockJsonRpcHandler.onGetTask(any(GetTaskRequest.class), any(ServerCallContext.class)))
                .thenReturn(mockResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        // Assert
        verify(mockJsonRpcHandler).onGetTask(any(GetTaskRequest.class), contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(GetTaskRequest.METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
    }

    @Test
    public void testCancelTask_MethodNameSetInContext() {
        // Arrange - based on CANCEL_TASK_TEST_REQUEST from JsonMessages
        String jsonRpcRequest = """
            {
             "jsonrpc": "2.0",
             "method": "tasks/cancel",
             "params": {
              "id": "de38c76d-d54c-436c-8b9f-4c2703648d64",
              "metadata": {}
             }
            }""";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        CancelTaskResponse mockResponse = mock(CancelTaskResponse.class);
        when(mockJsonRpcHandler.onCancelTask(any(CancelTaskRequest.class), any(ServerCallContext.class)))
                .thenReturn(mockResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        // Assert
        verify(mockJsonRpcHandler).onCancelTask(any(CancelTaskRequest.class), contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(CancelTaskRequest.METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
    }

    @Test
    public void testTaskResubscription_MethodNameSetInContext() {
        // Arrange - minimal valid JSON for task resubscription
        String jsonRpcRequest = """
            {
             "jsonrpc": "2.0",
             "method": "tasks/resubscribe",
             "params": {
              "id": "de38c76d-d54c-436c-8b9f-4c2703648d64"
             }
            }""";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        @SuppressWarnings("unchecked")
        Flow.Publisher<SendStreamingMessageResponse> mockPublisher = mock(Flow.Publisher.class);
        when(mockJsonRpcHandler.onResubscribeToTask(any(TaskResubscriptionRequest.class),
                any(ServerCallContext.class))).thenReturn(mockPublisher);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        // Assert
        verify(mockJsonRpcHandler).onResubscribeToTask(any(TaskResubscriptionRequest.class),
                contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(TaskResubscriptionRequest.METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
    }

    @Test
    public void testSetTaskPushNotificationConfig_MethodNameSetInContext() {
        // Arrange - based on SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_REQUEST from JsonMessages
        String jsonRpcRequest = """
            {
             "jsonrpc": "2.0",
             "method": "tasks/pushNotificationConfig/set",
             "params": {
              "taskId": "de38c76d-d54c-436c-8b9f-4c2703648d64",
              "pushNotificationConfig": {
               "url": "https://example.com/callback",
               "authentication": {
                "schemes": ["jwt"]
               }
              }
             }
            }""";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        SetTaskPushNotificationConfigResponse mockResponse = mock(SetTaskPushNotificationConfigResponse.class);
        when(mockJsonRpcHandler.setPushNotificationConfig(any(SetTaskPushNotificationConfigRequest.class),
                any(ServerCallContext.class))).thenReturn(mockResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        // Assert
        verify(mockJsonRpcHandler).setPushNotificationConfig(any(SetTaskPushNotificationConfigRequest.class),
                contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(SetTaskPushNotificationConfigRequest.METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
    }

    @Test
    public void testGetTaskPushNotificationConfig_MethodNameSetInContext() {
        // Arrange - based on GET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_REQUEST from JsonMessages
        String jsonRpcRequest = """
            {
             "jsonrpc": "2.0",
             "method": "tasks/pushNotificationConfig/get",
             "params": {
              "id": "de38c76d-d54c-436c-8b9f-4c2703648d64",
              "metadata": {}
             }
            }""";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        GetTaskPushNotificationConfigResponse mockResponse = mock(GetTaskPushNotificationConfigResponse.class);
        when(mockJsonRpcHandler.getPushNotificationConfig(any(GetTaskPushNotificationConfigRequest.class),
                any(ServerCallContext.class))).thenReturn(mockResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        // Assert
        verify(mockJsonRpcHandler).getPushNotificationConfig(any(GetTaskPushNotificationConfigRequest.class),
                contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(GetTaskPushNotificationConfigRequest.METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
    }

    @Test
    public void testListTaskPushNotificationConfig_MethodNameSetInContext() {
        // Arrange - minimal valid JSON for list task push notification config
        String jsonRpcRequest = """
            {
             "jsonrpc": "2.0",
             "method": "tasks/pushNotificationConfig/list",
             "params": {
              "id": "de38c76d-d54c-436c-8b9f-4c2703648d64"
             }
            }""";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        ListTaskPushNotificationConfigResponse mockResponse = mock(ListTaskPushNotificationConfigResponse.class);
        when(mockJsonRpcHandler.listPushNotificationConfig(any(ListTaskPushNotificationConfigRequest.class),
                any(ServerCallContext.class))).thenReturn(mockResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        // Assert
        verify(mockJsonRpcHandler).listPushNotificationConfig(any(ListTaskPushNotificationConfigRequest.class),
                contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(ListTaskPushNotificationConfigRequest.METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
    }

    @Test
    public void testDeleteTaskPushNotificationConfig_MethodNameSetInContext() {
        // Arrange - minimal valid JSON for delete task push notification config
        String jsonRpcRequest = """
            {
             "jsonrpc": "2.0",
             "method": "tasks/pushNotificationConfig/delete",
             "params": {
              "id": "de38c76d-d54c-436c-8b9f-4c2703648d64",
              "pushNotificationConfigId": "config-456"
             }
            }""";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        DeleteTaskPushNotificationConfigResponse mockResponse = mock(DeleteTaskPushNotificationConfigResponse.class);
        when(mockJsonRpcHandler.deletePushNotificationConfig(any(DeleteTaskPushNotificationConfigRequest.class),
                any(ServerCallContext.class))).thenReturn(mockResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        // Assert
        verify(mockJsonRpcHandler).deletePushNotificationConfig(any(DeleteTaskPushNotificationConfigRequest.class),
                contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(DeleteTaskPushNotificationConfigRequest.METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
    }

    @Test
    public void testGetAuthenticatedExtendedCard_MethodNameSetInContext() {
        // Arrange
        String jsonRpcRequest = "{\"jsonrpc\":\"2.0\",\"method\":\"" + GetAuthenticatedExtendedCardRequest.METHOD
                + "\",\"id\":1}";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        GetAuthenticatedExtendedCardResponse mockResponse = mock(GetAuthenticatedExtendedCardResponse.class);
        when(mockJsonRpcHandler.onGetAuthenticatedExtendedCardRequest(
                any(GetAuthenticatedExtendedCardRequest.class), any(ServerCallContext.class)))
                .thenReturn(mockResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        // Assert
        verify(mockJsonRpcHandler).onGetAuthenticatedExtendedCardRequest(
                any(GetAuthenticatedExtendedCardRequest.class), contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(GetAuthenticatedExtendedCardRequest.METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
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
}
