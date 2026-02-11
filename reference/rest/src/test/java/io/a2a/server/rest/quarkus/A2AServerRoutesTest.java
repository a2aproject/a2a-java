package io.a2a.server.rest.quarkus;

import static io.a2a.spec.A2AMethods.CANCEL_TASK_METHOD;
import static io.a2a.spec.A2AMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static io.a2a.spec.A2AMethods.GET_TASK_METHOD;
import static io.a2a.spec.A2AMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static io.a2a.spec.A2AMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static io.a2a.spec.A2AMethods.SEND_MESSAGE_METHOD;
import static io.a2a.spec.A2AMethods.SEND_STREAMING_MESSAGE_METHOD;
import static io.a2a.spec.A2AMethods.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static io.a2a.spec.A2AMethods.SUBSCRIBE_TO_TASK_METHOD;
import static io.a2a.transport.rest.context.RestContextKeys.METHOD_NAME_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.Executor;

import jakarta.enterprise.inject.Instance;

import io.a2a.server.ServerCallContext;
import io.a2a.transport.rest.handler.RestHandler;
import io.a2a.transport.rest.handler.RestHandler.HTTPRestResponse;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
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
        when(mockHttpResponse.getContentType()).thenReturn("application/json");
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
        when(mockHttpResponse.getContentType()).thenReturn("application/json");
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
        when(mockHttpResponse.getContentType()).thenReturn("application/json");
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
        when(mockHttpResponse.getContentType()).thenReturn("application/json");
        when(mockHttpResponse.getBody()).thenReturn("{}");
        when(mockRestHandler.cancelTask(any(ServerCallContext.class), anyString(), anyString())).thenReturn(mockHttpResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.cancelTask(mockRoutingContext);

        // Assert
        verify(mockRestHandler).cancelTask(contextCaptor.capture(), anyString(), eq("task123"));
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(CANCEL_TASK_METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
    }

    @Test
    public void testSubscribeTask_MethodNameSetInContext() {
        // Arrange
        when(mockRoutingContext.pathParam("taskId")).thenReturn("task123");
        HTTPRestResponse mockHttpResponse = mock(HTTPRestResponse.class);
        when(mockHttpResponse.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getContentType()).thenReturn("application/json");
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
        when(mockHttpResponse.getContentType()).thenReturn("application/json");
        when(mockHttpResponse.getBody()).thenReturn("{}");
        when(mockRestHandler.createTaskPushNotificationConfiguration(any(ServerCallContext.class), anyString(), anyString(), anyString())).thenReturn(mockHttpResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.CreateTaskPushNotificationConfiguration("{}", mockRoutingContext);

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
        when(mockHttpResponse.getContentType()).thenReturn("application/json");
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
        when(mockHttpResponse.getContentType()).thenReturn("application/json");
        when(mockHttpResponse.getBody()).thenReturn("{}");
        when(mockRestHandler.listTaskPushNotificationConfigurations(any(ServerCallContext.class), anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(mockHttpResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.listTaskPushNotificationConfigurations(mockRoutingContext);

        // Assert
        verify(mockRestHandler).listTaskPushNotificationConfigurations(contextCaptor.capture(), anyString(), eq("task123"), anyInt(), anyString());
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
        when(mockHttpResponse.getContentType()).thenReturn("application/json");
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
