package org.a2aproject.sdk.compat03.transport.rest.handler;

import java.util.HashSet;
import java.util.Map;

import org.a2aproject.sdk.compat03.conversion.AbstractA2ARequestHandlerTest_v0_3;
import org.a2aproject.sdk.compat03.conversion.Convert_v0_3_To10RequestHandler;
import org.a2aproject.sdk.compat03.conversion.mappers.domain.TaskMapper_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCapabilities_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.UnauthenticatedUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test suite for v0.3 RestHandler with v1.0 backend.
 * <p>
 * Tests verify that v0.3 REST clients can successfully communicate with the v1.0 backend
 * via the {@link Convert_v0_3_To10RequestHandler} conversion layer.
 * </p>
 * <p>
 * <b>Phase 3 Focus:</b> Core non-streaming tests (GetTask, SendMessage, CancelTask).
 * Streaming tests are deferred to Phase 4.
 * </p>
 */
public class RestHandler_v0_3_Test extends AbstractA2ARequestHandlerTest_v0_3 {

    private final ServerCallContext callContext = new ServerCallContext(
            UnauthenticatedUser.INSTANCE, Map.of("foo", "bar"), new HashSet<>());

    // ========================================
    // GetTask Tests
    // ========================================

    @Test
    public void testGetTaskSuccess() {
        RestHandler_v0_3 handler = new RestHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

        // Save v0.3 task by converting to v1.0
        taskStore.save(TaskMapper_v0_3.INSTANCE.toV10(MINIMAL_TASK), false);

        RestHandler_v0_3.HTTPRestResponse response = handler.getTask(MINIMAL_TASK.getId(), 0, callContext);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains(MINIMAL_TASK.getId()));

        // Test with different version parameter
        response = handler.getTask(MINIMAL_TASK.getId(), 2, callContext);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains(MINIMAL_TASK.getId()));
    }

    @Test
    public void testGetTaskNotFound() {
        RestHandler_v0_3 handler = new RestHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

        RestHandler_v0_3.HTTPRestResponse response = handler.getTask("nonexistent", 0, callContext);

        Assertions.assertEquals(404, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains("TaskNotFoundError"));
    }

    // ========================================
    // SendMessage Tests
    // ========================================

    @Test
    public void testSendMessage() {
        RestHandler_v0_3 handler = new RestHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

        // Configure agent to echo the message back
        agentExecutorExecute = (context, emitter) -> {
            emitter.emitEvent(context.getMessage());
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
              },
              "configuration":
                {
                  "blocking": true
              }
            }""";

        RestHandler_v0_3.HTTPRestResponse response = handler.sendMessage(requestBody, callContext);

        Assertions.assertEquals(200, response.getStatusCode(), response.toString());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertNotNull(response.getBody());
    }

    @Test
    public void testSendMessageInvalidBody() {
        RestHandler_v0_3 handler = new RestHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

        String invalidBody = "invalid json";
        RestHandler_v0_3.HTTPRestResponse response = handler.sendMessage(invalidBody, callContext);

        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains("JSONParseError"), response.getBody());
    }

    @Test
    public void testSendMessageWrongValueBody() {
        RestHandler_v0_3 handler = new RestHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

        // Invalid role value "user" instead of "ROLE_USER"
        String requestBody = """
                    {
                      "message":
                        {
                          "messageId": "message-1234",
                          "contextId": "context-1234",
                          "role": "user",
                          "content": [{
                            "text": "tell me a joke"
                          }],
                          "metadata": {
                          }
                      }
                    }""";

        RestHandler_v0_3.HTTPRestResponse response = handler.sendMessage(requestBody, callContext);

        Assertions.assertEquals(422, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains("InvalidParamsError"));
    }

    @Test
    public void testSendMessageEmptyBody() {
        RestHandler_v0_3 handler = new RestHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

        RestHandler_v0_3.HTTPRestResponse response = handler.sendMessage("", callContext);

        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains("InvalidRequestError"));
    }

    // ========================================
    // CancelTask Tests
    // ========================================

    @Test
    public void testCancelTaskSuccess() {
        RestHandler_v0_3 handler = new RestHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

        // Save v0.3 task by converting to v1.0
        taskStore.save(TaskMapper_v0_3.INSTANCE.toV10(MINIMAL_TASK), false);

        // Configure agent to cancel the task
        agentExecutorCancel = (context, emitter) -> {
            emitter.cancel();
        };

        RestHandler_v0_3.HTTPRestResponse response = handler.cancelTask(MINIMAL_TASK.getId(), callContext);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains(MINIMAL_TASK.getId()));
    }

    @Test
    public void testCancelTaskNotFound() {
        RestHandler_v0_3 handler = new RestHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

        RestHandler_v0_3.HTTPRestResponse response = handler.cancelTask("nonexistent", callContext);

        Assertions.assertEquals(404, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains("TaskNotFoundError"));
    }

    // ========================================
    // Streaming Tests
    // ========================================

    @Test
    public void testSendStreamingMessageSuccess() {
        RestHandler_v0_3 handler = new RestHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

        // Configure agent to emit the message back
        agentExecutorExecute = (context, emitter) -> {
            emitter.emitEvent(context.getMessage());
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

        RestHandler_v0_3.HTTPRestResponse response = handler.sendStreamingMessage(requestBody, callContext);

        // Verify streaming response
        assertEquals(200, response.getStatusCode(), response.toString());
        assertInstanceOf(RestHandler_v0_3.HTTPRestStreamingResponse.class, response,
                "Response should be HTTPRestStreamingResponse");

        RestHandler_v0_3.HTTPRestStreamingResponse streamingResponse =
                (RestHandler_v0_3.HTTPRestStreamingResponse) response;
        assertNotNull(streamingResponse.getPublisher(), "Publisher should not be null");
        assertEquals("text/event-stream", streamingResponse.getContentType(),
                "Content type should be text/event-stream for SSE");
    }

    @Test
    public void testSendStreamingMessageNotSupported() {
        // Create agent card with streaming disabled
        AgentCard_v0_3 nonStreamingCard = new AgentCard_v0_3.Builder(CARD)
                .capabilities(new AgentCapabilities_v0_3(false, true, false, null))
                .build();

        RestHandler_v0_3 handler = new RestHandler_v0_3(nonStreamingCard, internalExecutor, convert03To10Handler);

        String requestBody = """
            {
              "message": {
                "contextId": "ctx123",
                "role": "ROLE_USER",
                "content": [{
                    "text": "Hello"
                }]
              }
            }""";

        RestHandler_v0_3.HTTPRestResponse response = handler.sendStreamingMessage(requestBody, callContext);

        // Verify error response
        assertEquals(400, response.getStatusCode(), "Should return 400 for streaming not supported");
        assertTrue(response.getBody().contains("InvalidRequestError"),
                "Error should be InvalidRequestError");
        assertTrue(response.getBody().contains("Streaming is not supported by the agent"),
                "Error message should indicate streaming not supported");
    }

    // ========================================
    // Push Notification Tests
    // ========================================

    @Test
    public void testPushNotificationConfigSuccess() {
        RestHandler_v0_3 handler = new RestHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

        // Save task to v1.0 backend
        taskStore.save(TaskMapper_v0_3.INSTANCE.toV10(MINIMAL_TASK), false);

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
            }""".formatted(MINIMAL_TASK.getId(), MINIMAL_TASK.getId());

        RestHandler_v0_3.HTTPRestResponse response = handler.setTaskPushNotificationConfiguration(MINIMAL_TASK.getId(), requestBody, callContext);

        assertEquals(201, response.getStatusCode(), response.toString());
        assertEquals("application/json", response.getContentType());
        assertNotNull(response.getBody());
    }

    @Test
    public void testPushNotificationConfigNotSupported() {
        AgentCard_v0_3 card = createAgentCard(true, false, false);
        RestHandler_v0_3 handler = new RestHandler_v0_3(card, internalExecutor, convert03To10Handler);

        String requestBody = """
            {
                "taskId": "%s",
                "pushNotificationConfig": {
                    "url": "http://example.com"
                }
            }
            """.formatted(MINIMAL_TASK.getId());

        RestHandler_v0_3.HTTPRestResponse response = handler.setTaskPushNotificationConfiguration(MINIMAL_TASK.getId(), requestBody, callContext);

        assertEquals(501, response.getStatusCode());
        assertTrue(response.getBody().contains("PushNotificationNotSupportedError"));
    }

    @Test
    public void testGetPushNotificationConfig() {
        RestHandler_v0_3 handler = new RestHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

        // Save task to v1.0 backend
        taskStore.save(TaskMapper_v0_3.INSTANCE.toV10(MINIMAL_TASK), false);

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
            }""".formatted(MINIMAL_TASK.getId(), MINIMAL_TASK.getId());
        RestHandler_v0_3.HTTPRestResponse response = handler.setTaskPushNotificationConfiguration(MINIMAL_TASK.getId(), createRequestBody, callContext);
        assertEquals(201, response.getStatusCode(), response.toString());
        assertEquals("application/json", response.getContentType());

        // Now get it (using taskId as configId since that's the default)
        response = handler.getTaskPushNotificationConfiguration(MINIMAL_TASK.getId(), MINIMAL_TASK.getId(), callContext);
        assertEquals(200, response.getStatusCode(), response.toString());
        assertEquals("application/json", response.getContentType());
    }

    @Test
    public void testDeletePushNotificationConfig() {
        RestHandler_v0_3 handler = new RestHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

        // Save task to v1.0 backend
        taskStore.save(TaskMapper_v0_3.INSTANCE.toV10(MINIMAL_TASK), false);

        RestHandler_v0_3.HTTPRestResponse response = handler.deleteTaskPushNotificationConfiguration(MINIMAL_TASK.getId(), MINIMAL_TASK.getId(), callContext);
        assertEquals(204, response.getStatusCode());
    }

    @Test
    public void testListPushNotificationConfigs() {
        RestHandler_v0_3 handler = new RestHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

        // Save task to v1.0 backend
        taskStore.save(TaskMapper_v0_3.INSTANCE.toV10(MINIMAL_TASK), false);

        RestHandler_v0_3.HTTPRestResponse response = handler.listTaskPushNotificationConfigurations(MINIMAL_TASK.getId(), callContext);

        assertEquals(200, response.getStatusCode());
        assertEquals("application/json", response.getContentType());
    }
}
