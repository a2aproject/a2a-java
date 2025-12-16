package io.a2a.transport.rest.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.HashSet;
import java.util.Map;

import io.a2a.server.ServerCallContext;
import io.a2a.server.auth.UnauthenticatedUser;
import io.a2a.server.requesthandlers.AbstractA2ARequestHandlerTest;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Task;
import io.a2a.server.tasks.TaskUpdater;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RestHandlerTest extends AbstractA2ARequestHandlerTest {

    private final ServerCallContext callContext = new ServerCallContext(UnauthenticatedUser.INSTANCE, Map.of("foo", "bar"), new HashSet<>());

    @Test
    public void testGetTaskSuccess() {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK);

        RestHandler.HTTPRestResponse response = handler.getTask(MINIMAL_TASK.getId(), 0, "", callContext);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains(MINIMAL_TASK.getId()));

        response = handler.getTask(MINIMAL_TASK.getId(),2 , "",callContext);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains(MINIMAL_TASK.getId()));
    }

    @Test
    public void testGetTaskNotFound() {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);

        RestHandler.HTTPRestResponse response = handler.getTask("nonexistent", 0, "",callContext);

        Assertions.assertEquals(404, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains("TaskNotFoundError"));
    }

    @Test
    public void testSendMessage() throws InvalidProtocolBufferException {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);
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
                  "parts": [{
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

        RestHandler.HTTPRestResponse response = handler.sendMessage(requestBody, "", callContext);
        Assertions.assertEquals(200, response.getStatusCode(), response.toString());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertNotNull(response.getBody());
    }

    @Test
    public void testSendMessageInvalidBody() {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);

        String invalidBody = "invalid json";
        RestHandler.HTTPRestResponse response = handler.sendMessage(invalidBody, "", callContext);

        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains("JSONParseError"),response.getBody());
    }

    @Test
    public void testSendMessageWrongValueBody() {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);
        String requestBody = """
                    {
                      "message":
                        {
                          "messageId": "message-1234",
                          "contextId": "context-1234",
                          "role": "user",
                          "parts": [{
                            "text": "tell me a joke"
                          }],
                          "metadata": {
                          }
                      }
                    }""";
        RestHandler.HTTPRestResponse response = handler.sendMessage(requestBody, "", callContext);

        Assertions.assertEquals(422, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains("InvalidParamsError"));
    }

    @Test
    public void testSendMessageEmptyBody() {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);

        RestHandler.HTTPRestResponse response = handler.sendMessage("", "", callContext);

        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains("InvalidRequestError"));
    }

    @Test
    public void testCancelTaskSuccess() {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK);

        agentExecutorCancel = (context, eventQueue) -> {
            // We need to cancel the task or the EventConsumer never finds a 'final' event.
            // Looking at the Python implementation, they typically use AgentExecutors that
            // don't support cancellation. So my theory is the Agent updates the task to the CANCEL status
            Task task = context.getTask();
            TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);
            taskUpdater.cancel();
        };

        RestHandler.HTTPRestResponse response = handler.cancelTask(MINIMAL_TASK.getId(), "", callContext);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains(MINIMAL_TASK.getId()));
    }

    @Test
    public void testCancelTaskNotFound() {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);

        RestHandler.HTTPRestResponse response = handler.cancelTask("nonexistent", "", callContext);

        Assertions.assertEquals(404, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains("TaskNotFoundError"));
    }

    @Test
    public void testSendStreamingMessageSuccess() {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getMessage());
        };
        String requestBody = """
            {
              "message": {
                "role": "ROLE_USER",
                "parts": [
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

        RestHandler.HTTPRestResponse response = handler.sendStreamingMessage(requestBody, "", callContext);
        Assertions.assertEquals(200, response.getStatusCode(), response.toString());
        Assertions.assertInstanceOf(RestHandler.HTTPRestStreamingResponse.class, response);
        RestHandler.HTTPRestStreamingResponse streamingResponse = (RestHandler.HTTPRestStreamingResponse) response;
        Assertions.assertNotNull(streamingResponse.getPublisher());
        Assertions.assertEquals("text/event-stream", streamingResponse.getContentType());
    }

    @Test
    public void testSendStreamingMessageNotSupported() {
        AgentCard card = createAgentCard(false, true, true);
        RestHandler handler = new RestHandler(card, requestHandler, internalExecutor);

        String requestBody = """
            {
                "contextId": "ctx123",
                "role": "ROLE_USER",
                "parts": [{
                    "text": "Hello"
                }]
            }
            """;

        RestHandler.HTTPRestResponse response = handler.sendStreamingMessage(requestBody, "", callContext);

        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertTrue(response.getBody().contains("InvalidRequestError"));
    }

    @Test
    public void testPushNotificationConfigSuccess() {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK);

        String requestBody = """
            {
              "parent": "tasks/%s",
              "config": {
                "name": "tasks/%s/pushNotificationConfigs/default-config-id",
                "pushNotificationConfig": {
                  "id":"default-config-id",
                  "url": "https://example.com/callback",
                  "authentication": {
                    "schemes": ["jwt"]
                  }
                }
              }
            }""".formatted(MINIMAL_TASK.getId(), MINIMAL_TASK.getId());

        RestHandler.HTTPRestResponse response = handler.setTaskPushNotificationConfiguration( MINIMAL_TASK.getId(), requestBody, "", callContext);

        Assertions.assertEquals(201, response.getStatusCode(), response.toString());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertNotNull(response.getBody());
    }

    @Test
    public void testPushNotificationConfigNotSupported() {
        AgentCard card = createAgentCard(true, false, true);
        RestHandler handler = new RestHandler(card, requestHandler, internalExecutor);

        String requestBody = """
            {
                "taskId": "%s",
                "pushNotificationConfig": {
                    "url": "http://example.com"
                }
            }
            """.formatted(MINIMAL_TASK.getId());

        RestHandler.HTTPRestResponse response = handler.setTaskPushNotificationConfiguration(MINIMAL_TASK.getId(), requestBody, "", callContext);

        Assertions.assertEquals(501, response.getStatusCode());
        Assertions.assertTrue(response.getBody().contains("PushNotificationNotSupportedError"));
    }

    @Test
    public void testGetPushNotificationConfig() {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK);

        // First, create a push notification config
        String createRequestBody = """
            {
              "parent": "tasks/%s",
              "config": {
                "name": "tasks/%s/pushNotificationConfigs/default-config-id",
                "pushNotificationConfig": {
                  "id":"default-config-id",
                  "url": "https://example.com/callback",
                  "authentication": {
                    "schemes": ["jwt"]
                  }
                }
              }
            }""".formatted(MINIMAL_TASK.getId(), MINIMAL_TASK.getId());
        RestHandler.HTTPRestResponse response = handler.setTaskPushNotificationConfiguration(MINIMAL_TASK.getId(), createRequestBody, "", callContext);
        Assertions.assertEquals(201, response.getStatusCode(), response.toString());
        Assertions.assertEquals("application/json", response.getContentType());
        // Now get it
        response = handler.getTaskPushNotificationConfiguration(MINIMAL_TASK.getId(), "default-config-id", "", callContext);
        Assertions.assertEquals(200, response.getStatusCode(), response.toString());
        Assertions.assertEquals("application/json", response.getContentType());
    }

    @Test
    public void testDeletePushNotificationConfig() {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK);
        RestHandler.HTTPRestResponse response = handler.deleteTaskPushNotificationConfiguration(MINIMAL_TASK.getId(), "default-config-id", "", callContext);
        Assertions.assertEquals(204, response.getStatusCode());
    }

    @Test
    public void testListPushNotificationConfigs() {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK);

        RestHandler.HTTPRestResponse response = handler.listTaskPushNotificationConfigurations(MINIMAL_TASK.getId(), "", callContext);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertNotNull(response.getBody());
    }

    @Test
    public void testHttpStatusCodeMapping() {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);

        // Test 400 for invalid request
        RestHandler.HTTPRestResponse response = handler.sendMessage("", "", callContext);
        Assertions.assertEquals(400, response.getStatusCode());

        // Test 404 for not found
        response = handler.getTask("nonexistent", 0, "", callContext);
        Assertions.assertEquals(404, response.getStatusCode());
    }

    @Test
    public void testStreamingDoesNotBlockMainThread() throws Exception {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);

        // Track if the main thread gets blocked during streaming
        AtomicBoolean eventReceived = new AtomicBoolean(false);
        CountDownLatch streamStarted = new CountDownLatch(1);
        CountDownLatch eventProcessed = new CountDownLatch(1);
        agentExecutorExecute = (context, eventQueue) -> {
            // Wait a bit to ensure the main thread continues
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            eventQueue.enqueueEvent(context.getMessage());
        };

        String requestBody = """
            {
              "message": {
                "role": "ROLE_USER",
                "parts": [
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

        // Start streaming
        RestHandler.HTTPRestResponse response = handler.sendStreamingMessage(requestBody, "", callContext);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertInstanceOf(RestHandler.HTTPRestStreamingResponse.class, response);

        RestHandler.HTTPRestStreamingResponse streamingResponse = (RestHandler.HTTPRestStreamingResponse) response;
        Flow.Publisher<String> publisher = streamingResponse.getPublisher();
        publisher.subscribe(new Flow.Subscriber<String>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                streamStarted.countDown();
                subscription.request(1);
            }

            @Override
            public void onNext(String item) {
                eventReceived.set(true);
                eventProcessed.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                eventProcessed.countDown();
            }

            @Override
            public void onComplete() {
                eventProcessed.countDown();
            }
        });

        // The main thread should not be blocked - we should be able to continue immediately
        Assertions.assertTrue(streamStarted.await(100, TimeUnit.MILLISECONDS),
                "Streaming subscription should start quickly without blocking main thread");

        // This proves the main thread is not blocked - we can do other work
        // Simulate main thread doing other work
        Thread.sleep(50);

        // Wait for the actual event processing to complete
        Assertions.assertTrue(eventProcessed.await(2, TimeUnit.SECONDS),
                "Event should be processed within reasonable time");

        // Verify we received the event
        Assertions.assertTrue(eventReceived.get(), "Should have received streaming event");
    }
}
