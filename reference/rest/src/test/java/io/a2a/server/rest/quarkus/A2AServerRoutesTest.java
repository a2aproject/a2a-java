package io.a2a.server.rest.quarkus;

import static io.a2a.transport.rest.context.RestContextKeys.METHOD_NAME_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.Executor;

import jakarta.enterprise.inject.Instance;

import io.a2a.server.ServerCallContext;
import io.a2a.spec.CancelTaskRequest;
import io.a2a.spec.DeleteTaskPushNotificationConfigRequest;
import io.a2a.spec.GetTaskPushNotificationConfigRequest;
import io.a2a.spec.GetTaskRequest;
import io.a2a.spec.ListTaskPushNotificationConfigRequest;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendStreamingMessageRequest;
import io.a2a.spec.SetTaskPushNotificationConfigRequest;
import io.a2a.spec.TaskResubscriptionRequest;
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
        when(mockRestHandler.sendMessage(anyString(), any(ServerCallContext.class))).thenReturn(mockHttpResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.sendMessage("{}", mockRoutingContext);

        // Assert
        verify(mockRestHandler).sendMessage(eq("{}"), contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(SendMessageRequest.METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
    }

    @Test
    public void testSendMessageStreaming_MethodNameSetInContext() {
        // Arrange
        HTTPRestResponse mockHttpResponse = mock(HTTPRestResponse.class);
        when(mockHttpResponse.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getContentType()).thenReturn("application/json");
        when(mockHttpResponse.getBody()).thenReturn("{}");
        when(mockRestHandler.sendStreamingMessage(anyString(), any(ServerCallContext.class)))
                .thenReturn(mockHttpResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.sendMessageStreaming("{}", mockRoutingContext);

        // Assert
        verify(mockRestHandler).sendStreamingMessage(eq("{}"), contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(SendStreamingMessageRequest.METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
    }

    @Test
    public void testGetTask_MethodNameSetInContext() {
        // Arrange
        when(mockRoutingContext.pathParam("id")).thenReturn("task123");
        HTTPRestResponse mockHttpResponse = mock(HTTPRestResponse.class);
        when(mockHttpResponse.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getContentType()).thenReturn("application/json");
        when(mockHttpResponse.getBody()).thenReturn("{test:value}");
        when(mockRestHandler.getTask(anyString(), any(), any(ServerCallContext.class))).thenReturn(mockHttpResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.getTask(mockRoutingContext);

        // Assert
        verify(mockRestHandler).getTask(eq("task123"), eq(null), contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(GetTaskRequest.METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
    }

    @Test
    public void testCancelTask_MethodNameSetInContext() {
        // Arrange
        when(mockRoutingContext.pathParam("param0")).thenReturn("task123");
        HTTPRestResponse mockHttpResponse = mock(HTTPRestResponse.class);
        when(mockHttpResponse.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getContentType()).thenReturn("application/json");
        when(mockHttpResponse.getBody()).thenReturn("{}");
        when(mockRestHandler.cancelTask(anyString(), any(ServerCallContext.class))).thenReturn(mockHttpResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.cancelTask(mockRoutingContext);

        // Assert
        verify(mockRestHandler).cancelTask(eq("task123"), contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(CancelTaskRequest.METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
    }

    @Test
    public void testResubscribeTask_MethodNameSetInContext() {
        // Arrange
        when(mockRoutingContext.pathParam("param0")).thenReturn("task123");
        HTTPRestResponse mockHttpResponse = mock(HTTPRestResponse.class);
        when(mockHttpResponse.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getContentType()).thenReturn("application/json");
        when(mockHttpResponse.getBody()).thenReturn("{}");
        when(mockRestHandler.resubscribeTask(anyString(), any(ServerCallContext.class)))
                .thenReturn(mockHttpResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.resubscribeTask(mockRoutingContext);

        // Assert
        verify(mockRestHandler).resubscribeTask(eq("task123"), contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(TaskResubscriptionRequest.METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
    }

    @Test
    public void testSetTaskPushNotificationConfiguration_MethodNameSetInContext() {
        // Arrange
        when(mockRoutingContext.pathParam("id")).thenReturn("task123");
        HTTPRestResponse mockHttpResponse = mock(HTTPRestResponse.class);
        when(mockHttpResponse.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getContentType()).thenReturn("application/json");
        when(mockHttpResponse.getBody()).thenReturn("{}");
        when(mockRestHandler.setTaskPushNotificationConfiguration(anyString(), anyString(),
                any(ServerCallContext.class))).thenReturn(mockHttpResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.setTaskPushNotificationConfiguration("{}", mockRoutingContext);

        // Assert
        verify(mockRestHandler).setTaskPushNotificationConfiguration(eq("task123"), eq("{}"), contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(SetTaskPushNotificationConfigRequest.METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
    }

    @Test
    public void testGetTaskPushNotificationConfiguration_MethodNameSetInContext() {
        // Arrange
        when(mockRoutingContext.pathParam("id")).thenReturn("task123");
        when(mockRoutingContext.pathParam("configId")).thenReturn("config456");
        HTTPRestResponse mockHttpResponse = mock(HTTPRestResponse.class);
        when(mockHttpResponse.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getContentType()).thenReturn("application/json");
        when(mockHttpResponse.getBody()).thenReturn("{}");
        when(mockRestHandler.getTaskPushNotificationConfiguration(anyString(), anyString(),
                any(ServerCallContext.class))).thenReturn(mockHttpResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.getTaskPushNotificationConfiguration(mockRoutingContext);

        // Assert
        verify(mockRestHandler).getTaskPushNotificationConfiguration(eq("task123"), eq("config456"),
                contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(GetTaskPushNotificationConfigRequest.METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
    }

    @Test
    public void testListTaskPushNotificationConfigurations_MethodNameSetInContext() {
        // Arrange
        when(mockRoutingContext.pathParam("id")).thenReturn("task123");
        HTTPRestResponse mockHttpResponse = mock(HTTPRestResponse.class);
        when(mockHttpResponse.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getContentType()).thenReturn("application/json");
        when(mockHttpResponse.getBody()).thenReturn("{}");
        when(mockRestHandler.listTaskPushNotificationConfigurations(anyString(), any(ServerCallContext.class)))
                .thenReturn(mockHttpResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.listTaskPushNotificationConfigurations(mockRoutingContext);

        // Assert
        verify(mockRestHandler).listTaskPushNotificationConfigurations(eq("task123"), contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(ListTaskPushNotificationConfigRequest.METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
    }

    @Test
    public void testDeleteTaskPushNotificationConfiguration_MethodNameSetInContext() {
        // Arrange
        when(mockRoutingContext.pathParam("id")).thenReturn("task123");
        when(mockRoutingContext.pathParam("configId")).thenReturn("config456");
        HTTPRestResponse mockHttpResponse = mock(HTTPRestResponse.class);
        when(mockHttpResponse.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getContentType()).thenReturn("application/json");
        when(mockHttpResponse.getBody()).thenReturn("{}");
        when(mockRestHandler.deleteTaskPushNotificationConfiguration(anyString(), anyString(),
                any(ServerCallContext.class))).thenReturn(mockHttpResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.deleteTaskPushNotificationConfiguration(mockRoutingContext);

        // Assert
        verify(mockRestHandler).deleteTaskPushNotificationConfiguration(eq("task123"), eq("config456"),
                contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(DeleteTaskPushNotificationConfigRequest.METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
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
