package io.a2a.httprest.handler;

import java.util.Map;

import io.a2a.server.ServerCallContext;
import io.a2a.server.auth.UnauthenticatedUser;
import io.a2a.server.requesthandlers.AbstractA2ARequestHandlerTest;
import io.a2a.spec.AgentCard;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.InvalidRequestError;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.MethodNotFoundError;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.PushNotificationNotSupportedError;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskListParams;
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import io.a2a.server.tasks.TaskUpdater;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HTTPRestHandlerTest extends AbstractA2ARequestHandlerTest {

    private final ServerCallContext callContext = new ServerCallContext(UnauthenticatedUser.INSTANCE, Map.of("foo", "bar"));

    @Test
    public void testGetAgentCard() {
        HTTPRestHandler handler = new HTTPRestHandler(CARD, requestHandler);
        HTTPRestHandler.HTTPRestResponse response = handler.handleRequest("GET", "/v1/card", null, callContext);
        
        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertNotNull(response.getBody());
    }

    @Test
    public void testListTasks() {
        HTTPRestHandler handler = new HTTPRestHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);
        
        HTTPRestHandler.HTTPRestResponse response = handler.handleRequest("GET", "/v1/tasks", null, callContext);
        
        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertTrue(response.getBody().contains(MINIMAL_TASK.getId()));
    }

    @Test
    public void testGetTaskSuccess() {
        HTTPRestHandler handler = new HTTPRestHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);
        
        HTTPRestHandler.HTTPRestResponse response = handler.handleRequest("GET", "/v1/tasks/" + MINIMAL_TASK.getId(), null, callContext);
        
        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains(MINIMAL_TASK.getId()));
    }

    @Test
    public void testGetTaskNotFound() {
        HTTPRestHandler handler = new HTTPRestHandler(CARD, requestHandler);
        
        HTTPRestHandler.HTTPRestResponse response = handler.handleRequest("GET", "/v1/tasks/nonexistent", null, callContext);
        
        Assertions.assertEquals(404, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains("TaskNotFoundError"));
    }

    @Test
    public void testSendMessage() {
        HTTPRestHandler handler = new HTTPRestHandler(CARD, requestHandler);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getMessage());
        };

        String requestBody = """
            {
                "message": {
                    "role": "user",
                    "parts": [{"text": "Hello", "kind": "text"}],
                    "contextId": "ctx123",
                    "kind": "message"
                }
            }
            """;

        HTTPRestHandler.HTTPRestResponse response = handler.handleRequest("POST", "/v1/message:send", requestBody, callContext);
        
        
        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertNotNull(response.getBody());
    }

    @Test
    public void testSendMessageInvalidBody() {
        HTTPRestHandler handler = new HTTPRestHandler(CARD, requestHandler);
        
        String invalidBody = "invalid json";
        HTTPRestHandler.HTTPRestResponse response = handler.handleRequest("POST", "/v1/message:send", invalidBody, callContext);
        
        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains("InvalidParamsError"));
    }

    @Test
    public void testSendMessageEmptyBody() {
        HTTPRestHandler handler = new HTTPRestHandler(CARD, requestHandler);
        
        HTTPRestHandler.HTTPRestResponse response = handler.handleRequest("POST", "/v1/message:send", null, callContext);
        
        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains("InvalidParamsError"));
    }

    @Test
    public void testCancelTaskSuccess() {
        HTTPRestHandler handler = new HTTPRestHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);

        agentExecutorCancel = (context, eventQueue) -> {
            // We need to cancel the task or the EventConsumer never finds a 'final' event.
            // Looking at the Python implementation, they typically use AgentExecutors that
            // don't support cancellation. So my theory is the Agent updates the task to the CANCEL status
            Task task = context.getTask();
            TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);
            taskUpdater.cancel();
        };

        HTTPRestHandler.HTTPRestResponse response = handler.handleRequest("POST", "/v1/tasks/" + MINIMAL_TASK.getId() + ":cancel", null, callContext);
        
        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains(MINIMAL_TASK.getId()));
    }

    @Test
    public void testCancelTaskNotFound() {
        HTTPRestHandler handler = new HTTPRestHandler(CARD, requestHandler);
        
        HTTPRestHandler.HTTPRestResponse response = handler.handleRequest("POST", "/v1/tasks/nonexistent:cancel", null, callContext);
        
        Assertions.assertEquals(404, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains("TaskNotFoundError"));
    }

    @Test
    public void testSendStreamingMessageSuccess() {
        HTTPRestHandler handler = new HTTPRestHandler(CARD, requestHandler);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getMessage());
        };

        String requestBody = """
            {
                "message": {
                    "role": "user", 
                    "parts": [{"text": "Hello", "kind": "text"}],
                    "contextId": "ctx123",
                    "kind": "message"
                }
            }
            """;

        HTTPRestHandler.HTTPRestResponse response = handler.handleRequest("POST", "/v1/message:stream", requestBody, callContext);
        
        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertInstanceOf(HTTPRestHandler.HTTPRestStreamingResponse.class, response);
        HTTPRestHandler.HTTPRestStreamingResponse streamingResponse = (HTTPRestHandler.HTTPRestStreamingResponse) response;
        Assertions.assertNotNull(streamingResponse.getPublisher());
        Assertions.assertEquals("text/event-stream", streamingResponse.getContentType());
    }

    @Test
    public void testSendStreamingMessageNotSupported() {
        AgentCard card = createAgentCard(false, true, true);
        HTTPRestHandler handler = new HTTPRestHandler(card, requestHandler);

        String requestBody = """
            {
                "message": {
                    "role": "user",
                    "parts": [{"text": "Hello", "kind": "text"}],
                    "contextId": "ctx123",
                    "kind": "message"
                }
            }
            """;

        HTTPRestHandler.HTTPRestResponse response = handler.handleRequest("POST", "/v1/message:stream", requestBody, callContext);
        
        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertTrue(response.getBody().contains("InvalidRequestError"));
    }

    @Test
    public void testPushNotificationConfigSuccess() {
        HTTPRestHandler handler = new HTTPRestHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);

        String requestBody = """
            {
                "taskId": "%s",
                "pushNotificationConfig": {
                    "url": "http://example.com"
                }
            }
            """.formatted(MINIMAL_TASK.getId());

        HTTPRestHandler.HTTPRestResponse response = handler.handleRequest("POST", "/v1/tasks/" + MINIMAL_TASK.getId() + "/pushNotificationConfigs", requestBody, callContext);
        
        
        Assertions.assertEquals(201, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertNotNull(response.getBody());
    }

    @Test
    public void testPushNotificationConfigNotSupported() {
        AgentCard card = createAgentCard(true, false, true);
        HTTPRestHandler handler = new HTTPRestHandler(card, requestHandler);

        String requestBody = """
            {
                "taskId": "%s",
                "pushNotificationConfig": {
                    "url": "http://example.com"
                }
            }
            """.formatted(MINIMAL_TASK.getId());

        HTTPRestHandler.HTTPRestResponse response = handler.handleRequest("POST", "/v1/tasks/" + MINIMAL_TASK.getId() + "/pushNotificationConfigs", requestBody, callContext);
        
        Assertions.assertEquals(501, response.getStatusCode());
        Assertions.assertTrue(response.getBody().contains("PushNotificationNotSupportedError"));
    }

    @Test
    public void testGetPushNotificationConfig() {
        HTTPRestHandler handler = new HTTPRestHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);

        // First, create a push notification config
        String createRequestBody = """
            {
                "taskId": "%s",
                "pushNotificationConfig": {
                    "id": "default-config-id",
                    "url": "http://example.com"
                }
            }
            """.formatted(MINIMAL_TASK.getId());
        handler.handleRequest("POST", "/v1/tasks/" + MINIMAL_TASK.getId() + "/pushNotificationConfigs", createRequestBody, callContext);

        // Now get it
        HTTPRestHandler.HTTPRestResponse response = handler.handleRequest("GET", "/v1/tasks/" + MINIMAL_TASK.getId() + "/pushNotificationConfigs/default-config-id", null, callContext);
        
        
        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
    }

    @Test
    public void testDeletePushNotificationConfig() {
        HTTPRestHandler handler = new HTTPRestHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);

        HTTPRestHandler.HTTPRestResponse response = handler.handleRequest("DELETE", "/v1/tasks/" + MINIMAL_TASK.getId() + "/pushNotificationConfigs/default-config-id", null, callContext);
        
        Assertions.assertEquals(204, response.getStatusCode());
    }

    @Test
    public void testListPushNotificationConfigs() {
        HTTPRestHandler handler = new HTTPRestHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);

        HTTPRestHandler.HTTPRestResponse response = handler.handleRequest("GET", "/v1/tasks/" + MINIMAL_TASK.getId() + "/pushNotificationConfigs", null, callContext);
        
        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertNotNull(response.getBody());
    }

    @Test
    public void testMethodNotFound() {
        HTTPRestHandler handler = new HTTPRestHandler(CARD, requestHandler);
        
        HTTPRestHandler.HTTPRestResponse response = handler.handleRequest("GET", "/v1/unknown/endpoint", null, callContext);
        
        Assertions.assertEquals(404, response.getStatusCode());
        Assertions.assertTrue(response.getBody().contains("MethodNotFoundError"));
    }

    @Test
    public void testUnsupportedHttpMethod() {
        HTTPRestHandler handler = new HTTPRestHandler(CARD, requestHandler);
        
        HTTPRestHandler.HTTPRestResponse response = handler.handleRequest("PATCH", "/v1/card", null, callContext);
        
        Assertions.assertEquals(405, response.getStatusCode());
        Assertions.assertTrue(response.getBody().contains("MethodNotFoundError"));
    }

    @Test
    public void testHttpStatusCodeMapping() {
        HTTPRestHandler handler = new HTTPRestHandler(CARD, requestHandler);
        
        // Test 400 for invalid request
        HTTPRestHandler.HTTPRestResponse response = handler.handleRequest("POST", "/v1/message:send", null, callContext);
        Assertions.assertEquals(400, response.getStatusCode());
        
        // Test 404 for not found
        response = handler.handleRequest("GET", "/v1/tasks/nonexistent", null, callContext);
        Assertions.assertEquals(404, response.getStatusCode());
        
        // Test 405 for unsupported method
        response = handler.handleRequest("PATCH", "/v1/card", null, callContext);
        Assertions.assertEquals(405, response.getStatusCode());
    }
}