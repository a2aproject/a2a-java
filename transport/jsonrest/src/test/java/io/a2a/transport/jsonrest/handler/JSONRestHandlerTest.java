package io.a2a.transport.jsonrest.handler;

import io.a2a.transport.jsonrest.handler.JSONRestHandler;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Map;

import io.a2a.server.ServerCallContext;
import io.a2a.server.auth.UnauthenticatedUser;
import io.a2a.server.requesthandlers.AbstractA2ARequestHandlerTest;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Task;
import io.a2a.server.tasks.TaskUpdater;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JSONRestHandlerTest extends AbstractA2ARequestHandlerTest {

    private final ServerCallContext callContext = new ServerCallContext(UnauthenticatedUser.INSTANCE, Map.of("foo", "bar"));

    @Test
    public void testGetAgentCard() {
        JSONRestHandler handler = new JSONRestHandler(CARD, requestHandler);
        JSONRestHandler.HTTPRestResponse response = handler.handleRequest("GET", "/v1/card", null, callContext);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertNotNull(response.getBody());
    }

    @Test
    public void testGetTaskSuccess() {
        JSONRestHandler handler = new JSONRestHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);

        JSONRestHandler.HTTPRestResponse response = handler.handleRequest("GET", "/v1/tasks/" + MINIMAL_TASK.getId(), null, callContext);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains(MINIMAL_TASK.getId()));
    }

    @Test
    public void testGetTaskNotFound() {
        JSONRestHandler handler = new JSONRestHandler(CARD, requestHandler);

        JSONRestHandler.HTTPRestResponse response = handler.handleRequest("GET", "/v1/tasks/nonexistent", null, callContext);

        Assertions.assertEquals(404, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains("TaskNotFoundError"));
    }

    @Test
    public void testSendMessage() throws InvalidProtocolBufferException {
        JSONRestHandler handler = new JSONRestHandler(CARD, requestHandler);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getMessage());
        };
        String requestBody = """
            {
              "message":
                {
                  "messageId": "message-1234",
                  "contextId": "context-1234",
                  "role": "ROLE_USER",
                  "content": [{
                    "text": "tell me a joke"
                  }],
                  "metadata": {
                  }
              }
            }""";

        JSONRestHandler.HTTPRestResponse response = handler.handleRequest("POST", "/v1/message:send", requestBody, callContext);
        Assertions.assertEquals(200, response.getStatusCode(), response.toString());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertNotNull(response.getBody());
    }

    @Test
    public void testSendMessageInvalidBody() {
        JSONRestHandler handler = new JSONRestHandler(CARD, requestHandler);

        String invalidBody = "invalid json";
        JSONRestHandler.HTTPRestResponse response = handler.handleRequest("POST", "/v1/message:send", invalidBody, callContext);

        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains("InvalidParamsError"));
    }

    @Test
    public void testSendMessageEmptyBody() {
        JSONRestHandler handler = new JSONRestHandler(CARD, requestHandler);

        JSONRestHandler.HTTPRestResponse response = handler.handleRequest("POST", "/v1/message:send", null, callContext);

        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains("InvalidParamsError"));
    }

    @Test
    public void testCancelTaskSuccess() {
        JSONRestHandler handler = new JSONRestHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);

        agentExecutorCancel = (context, eventQueue) -> {
            // We need to cancel the task or the EventConsumer never finds a 'final' event.
            // Looking at the Python implementation, they typically use AgentExecutors that
            // don't support cancellation. So my theory is the Agent updates the task to the CANCEL status
            Task task = context.getTask();
            TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);
            taskUpdater.cancel();
        };

        JSONRestHandler.HTTPRestResponse response = handler.handleRequest("POST", "/v1/tasks/" + MINIMAL_TASK.getId() + ":cancel", null, callContext);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains(MINIMAL_TASK.getId()));
    }

    @Test
    public void testCancelTaskNotFound() {
        JSONRestHandler handler = new JSONRestHandler(CARD, requestHandler);

        JSONRestHandler.HTTPRestResponse response = handler.handleRequest("POST", "/v1/tasks/nonexistent:cancel", null, callContext);

        Assertions.assertEquals(404, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains("TaskNotFoundError"));
    }

    @Test
    public void testSendStreamingMessageSuccess() {
        JSONRestHandler handler = new JSONRestHandler(CARD, requestHandler);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getMessage());
        };
        String requestBody = """
            {
              "message": {
                "role": "ROLE_USER",
                "content": [
                  {
                    "text": "tell me some jokes"
                  }
                ],
                "messageId": "message-1234",
                "contextId": "context-1234"
              },
              "configuration": {
                "acceptedOutputModes": ["text"]
              }
            }""";

        JSONRestHandler.HTTPRestResponse response = handler.handleRequest("POST", "/v1/message:stream", requestBody, callContext);
        Assertions.assertEquals(200, response.getStatusCode(), response.toString());
        Assertions.assertInstanceOf(JSONRestHandler.HTTPRestStreamingResponse.class, response);
        JSONRestHandler.HTTPRestStreamingResponse streamingResponse = (JSONRestHandler.HTTPRestStreamingResponse) response;
        Assertions.assertNotNull(streamingResponse.getPublisher());
        Assertions.assertEquals("text/event-stream", streamingResponse.getContentType());
    }

    @Test
    public void testSendStreamingMessageNotSupported() {
        AgentCard card = createAgentCard(false, true, true);
        JSONRestHandler handler = new JSONRestHandler(card, requestHandler);

        String requestBody = """
            {
                "contextId": "ctx123",
                "role": "ROLE_USER",
                "content": [{
                    "text": "Hello"
                }]
            }
            """;

        JSONRestHandler.HTTPRestResponse response = handler.handleRequest("POST", "/v1/message:stream", requestBody, callContext);

        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertTrue(response.getBody().contains("InvalidRequestError"));
    }

    @Test
    public void testPushNotificationConfigSuccess() {
        JSONRestHandler handler = new JSONRestHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);

        String requestBody = """
            {
              "parent": "tasks/%s",
              "config": {
                "name": "tasks/%s/pushNotificationConfigs/",
                "pushNotificationConfig": {
                  "url": "https://example.com/callback",
                  "authentication": {
                    "schemes": ["jwt"]
                  }
                }
              }
            }""".formatted(MINIMAL_TASK.getId(),MINIMAL_TASK.getId());

        JSONRestHandler.HTTPRestResponse response = handler.handleRequest("POST", "/v1/tasks/" + MINIMAL_TASK.getId() + "/pushNotificationConfigs", requestBody, callContext);

        Assertions.assertEquals(201, response.getStatusCode(), response.toString());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertNotNull(response.getBody());
    }

    @Test
    public void testPushNotificationConfigNotSupported() {
        AgentCard card = createAgentCard(true, false, true);
        JSONRestHandler handler = new JSONRestHandler(card, requestHandler);

        String requestBody = """
            {
                "taskId": "%s",
                "pushNotificationConfig": {
                    "url": "http://example.com"
                }
            }
            """.formatted(MINIMAL_TASK.getId());

        JSONRestHandler.HTTPRestResponse response = handler.handleRequest("POST", "/v1/tasks/" + MINIMAL_TASK.getId() + "/pushNotificationConfigs", requestBody, callContext);

        Assertions.assertEquals(501, response.getStatusCode());
        Assertions.assertTrue(response.getBody().contains("PushNotificationNotSupportedError"));
    }

    @Test
    public void testGetPushNotificationConfig() {
        JSONRestHandler handler = new JSONRestHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);

        // First, create a push notification config
        String createRequestBody = """
            {
              "parent": "tasks/%s",
              "config": {
                "name": "tasks/%s/pushNotificationConfigs/",
                "pushNotificationConfig": {
                  "url": "https://example.com/callback",
                  "authentication": {
                    "schemes": ["jwt"]
                  }
                }
              }
            }""".formatted(MINIMAL_TASK.getId(),MINIMAL_TASK.getId());
        JSONRestHandler.HTTPRestResponse response = handler.handleRequest("POST", "/v1/tasks/" + MINIMAL_TASK.getId() + "/pushNotificationConfigs", createRequestBody, callContext);
        Assertions.assertEquals(201, response.getStatusCode(), response.toString());
        Assertions.assertEquals("application/json", response.getContentType());
        // Now get it
        response = handler.handleRequest("GET", "/v1/tasks/" + MINIMAL_TASK.getId() + "/pushNotificationConfigs/default-config-id", null, callContext);
        Assertions.assertEquals(200, response.getStatusCode(), response.toString());
        Assertions.assertEquals("application/json", response.getContentType());
    }

    @Test
    public void testDeletePushNotificationConfig() {
        JSONRestHandler handler = new JSONRestHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);

        JSONRestHandler.HTTPRestResponse response = handler.handleRequest("DELETE", "/v1/tasks/" + MINIMAL_TASK.getId() + "/pushNotificationConfigs/default-config-id", null, callContext);

        Assertions.assertEquals(204, response.getStatusCode());
    }

    @Test
    public void testListPushNotificationConfigs() {
        JSONRestHandler handler = new JSONRestHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);

        JSONRestHandler.HTTPRestResponse response = handler.handleRequest("GET", "/v1/tasks/" + MINIMAL_TASK.getId() + "/pushNotificationConfigs", null, callContext);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertNotNull(response.getBody());
    }

    @Test
    public void testMethodNotFound() {
        JSONRestHandler handler = new JSONRestHandler(CARD, requestHandler);

        JSONRestHandler.HTTPRestResponse response = handler.handleRequest("GET", "/v1/unknown/endpoint", null, callContext);

        Assertions.assertEquals(404, response.getStatusCode());
        Assertions.assertTrue(response.getBody().contains("MethodNotFoundError"));
    }

    @Test
    public void testUnsupportedHttpMethod() {
        JSONRestHandler handler = new JSONRestHandler(CARD, requestHandler);

        JSONRestHandler.HTTPRestResponse response = handler.handleRequest("PATCH", "/v1/card", null, callContext);

        Assertions.assertEquals(405, response.getStatusCode());
        Assertions.assertTrue(response.getBody().contains("MethodNotFoundError"));
    }

    @Test
    public void testHttpStatusCodeMapping() {
        JSONRestHandler handler = new JSONRestHandler(CARD, requestHandler);

        // Test 400 for invalid request
        JSONRestHandler.HTTPRestResponse response = handler.handleRequest("POST", "/v1/message:send", null, callContext);
        Assertions.assertEquals(400, response.getStatusCode());

        // Test 404 for not found
        response = handler.handleRequest("GET", "/v1/tasks/nonexistent", null, callContext);
        Assertions.assertEquals(404, response.getStatusCode());

        // Test 405 for unsupported method
        response = handler.handleRequest("PATCH", "/v1/card", null, callContext);
        Assertions.assertEquals(405, response.getStatusCode());
    }
}
