package org.a2aproject.sdk.transport.rest.handler;

import static org.a2aproject.sdk.common.MediaType.APPLICATION_JSON;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.inject.Instance;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.InvalidProtocolBufferException;

import org.a2aproject.sdk.server.AgentCardCacheMetadata;
import org.a2aproject.sdk.server.TestInstances;
import org.a2aproject.sdk.server.FixedInstance;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.UnauthenticatedUser;
import org.a2aproject.sdk.server.config.DefaultValuesConfigProvider;
import org.a2aproject.sdk.server.multitenancy.AgentCardRouter;
import org.a2aproject.sdk.server.requesthandlers.AbstractA2ARequestHandlerTest;
import org.a2aproject.sdk.server.requesthandlers.LogCaptureAssertions;
import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentExtension;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.Task;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;

@Timeout(value = 1, unit = TimeUnit.MINUTES)
public class RestHandlerTest extends AbstractA2ARequestHandlerTest {

    private final ServerCallContext callContext = new ServerCallContext(UnauthenticatedUser.INSTANCE, Map.of("foo", "bar"), new HashSet<>(), "1.0");

    private static AgentCardCacheMetadata createCacheMetadata() {
        return createCacheMetadata(CARD);
    }

    private static AgentCardCacheMetadata createCacheMetadata(AgentCard card) {
        return new AgentCardCacheMetadata(card, new DefaultValuesConfigProvider());
    }

    @Test
    public void testGetTaskSuccess() {
        RestHandler handler = new RestHandler(CARD, createCacheMetadata(), requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK, false);

        RestHandler.HTTPRestResponse response = handler.getTask(callContext, "", MINIMAL_TASK.id(), 0);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals(APPLICATION_JSON, response.getContentType());
        Assertions.assertTrue(response.getBody().contains(MINIMAL_TASK.id()));

        response = handler.getTask(callContext, "", MINIMAL_TASK.id(), 2);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals(APPLICATION_JSON, response.getContentType());
        Assertions.assertTrue(response.getBody().contains(MINIMAL_TASK.id()));
    }

    @Test
    public void testGetTaskNotFound() {
        RestHandler handler = new RestHandler(CARD, createCacheMetadata(), requestHandler, internalExecutor);

        RestHandler.HTTPRestResponse response = handler.getTask(callContext, "", "nonexistent", 0);

        assertProblemDetail(response, 404,
                "TASK_NOT_FOUND", "Task not found");
    }

    @Test
    public void testGetTaskNegativeHistoryLengthReturns422() {
        RestHandler handler = new RestHandler(CARD, createCacheMetadata(), requestHandler, internalExecutor);

        RestHandler.HTTPRestResponse response = handler.getTask(callContext, "", MINIMAL_TASK.id(), -1);

        assertProblemDetail(response, 422,
                "INVALID_PARAMS", "Invalid history length");
    }

    @Test
    public void testListTasksStatusWireString() {
        RestHandler handler = new RestHandler(CARD, createCacheMetadata(), requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK, false);

        RestHandler.HTTPRestResponse response = handler.listTasks(callContext, "", null, "TASK_STATE_SUBMITTED", null, null,
                null, null, null);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals(APPLICATION_JSON, response.getContentType());
        Assertions.assertTrue(response.getBody().contains(MINIMAL_TASK.id()));
    }

    @Test
    public void testListTasksInvalidStatus() {
        RestHandler handler = new RestHandler(CARD, createCacheMetadata(), requestHandler, internalExecutor);

        RestHandler.HTTPRestResponse response = handler.listTasks(callContext, "", null, "not-a-status", null, null,
                null, null, null);

        assertProblemDetail(response, 422,
                "INVALID_PARAMS", "Invalid params");
    }

    @Test
    public void testSendMessage() throws InvalidProtocolBufferException {
        RestHandler handler = new RestHandler(CARD, createCacheMetadata(), requestHandler, internalExecutor);
        agentExecutorExecute = (context, agentEmitter) -> {
            agentEmitter.sendMessage(context.getMessage());
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
                  "returnImmediately": false
              }
            }""";

        RestHandler.HTTPRestResponse response = handler.sendMessage(callContext, "", requestBody);
        Assertions.assertEquals(200, response.getStatusCode(), response.toString());
        Assertions.assertEquals(APPLICATION_JSON, response.getContentType());
        Assertions.assertNotNull(response.getBody());
    }

    @Test
    public void testSendMessageInvalidBody() {
        RestHandler handler = new RestHandler(CARD, createCacheMetadata(), requestHandler, internalExecutor);

        String invalidBody = "invalid json";
        RestHandler.HTTPRestResponse response = handler.sendMessage(callContext, "", invalidBody);

        assertProblemDetail(response, 400,
                "JSON_PARSE", "Failed to parse json");
    }

    @Test
    public void testSendMessageWrongValueBody() {
        RestHandler handler = new RestHandler(CARD, createCacheMetadata(), requestHandler, internalExecutor);
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
        RestHandler.HTTPRestResponse response = handler.sendMessage(callContext, "", requestBody);

        Assertions.assertEquals(422, response.getStatusCode());
        Assertions.assertEquals(APPLICATION_JSON, response.getContentType());
        JsonObject body = JsonParser.parseString(response.getBody()).getAsJsonObject();
        JsonObject error = body.getAsJsonObject("error");
        Assertions.assertEquals(422, error.get("code").getAsInt());
        Assertions.assertEquals("INVALID_PARAMS", error.getAsJsonArray("details").get(0).getAsJsonObject().get("reason").getAsString());
        Assertions.assertTrue(error.get("message").getAsString().startsWith("Failed to parse request body:"),
                "message should indicate parse failure: " + error.get("message").getAsString());
    }

    @Test
    public void testSendMessageEmptyBody() {
        RestHandler handler = new RestHandler(CARD, createCacheMetadata(), requestHandler, internalExecutor);

        RestHandler.HTTPRestResponse response = handler.sendMessage(callContext, "", "");

        assertProblemDetail(response, 400,
                "INVALID_REQUEST", "Request body is required");
    }

    @Test
    public void testCancelTaskSuccess() {
        RestHandler handler = new RestHandler(CARD, createCacheMetadata(), requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK, false);

        agentExecutorCancel = (context, agentEmitter) -> {
            // We need to cancel the task or the EventConsumer never finds a 'final' event.
            // Looking at the Python implementation, they typically use AgentExecutors that
            // don't support cancellation. So my theory is the Agent updates the task to the CANCEL status
            Task task = context.getTask();
            agentEmitter.cancel();
        };

        String requestBody = String.format("{\"id\":\"%s\"}", MINIMAL_TASK.id());
        RestHandler.HTTPRestResponse response = handler.cancelTask(callContext, "", requestBody, MINIMAL_TASK.id());

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals(APPLICATION_JSON, response.getContentType());
        Assertions.assertTrue(response.getBody().contains(MINIMAL_TASK.id()));
    }

    @Test
    public void testCancelTaskNotFound() {
        RestHandler handler = new RestHandler(CARD, createCacheMetadata(), requestHandler, internalExecutor);

        String requestBody = "{\"id\":\"nonexistent\"}";
        RestHandler.HTTPRestResponse response = handler.cancelTask(callContext, "", requestBody, "nonexistent");

        assertProblemDetail(response, 404,
                "TASK_NOT_FOUND", "Task not found");
    }

    @Test
    public void testCancelTaskWithMetadata() {
        RestHandler handler = new RestHandler(CARD, createCacheMetadata(), requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK, false);

        agentExecutorCancel = (context, agentEmitter) -> {
            // Verify metadata is accessible in the context
            Task task = context.getTask();

            // Cancel the task so EventConsumer finds a final event
            agentEmitter.cancel();
        };

        // Request body with metadata
        String requestBody = """
            {
                "metadata": {
                    "reason": "user_requested",
                    "source": "web_ui",
                    "priority": "high"
                }
            }
            """;

        RestHandler.HTTPRestResponse response = handler.cancelTask(callContext, "", requestBody, MINIMAL_TASK.id());

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals(APPLICATION_JSON, response.getContentType());
        Assertions.assertTrue(response.getBody().contains(MINIMAL_TASK.id()));
    }

    @Test
    public void testCancelTaskWithEmptyMetadata() {
        RestHandler handler = new RestHandler(CARD, createCacheMetadata(), requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK, false);

        agentExecutorCancel = (context, agentEmitter) -> {
            Task task = context.getTask();
            agentEmitter.cancel();
        };

        // Request body with empty metadata object
        String requestBody = """
            {
                "metadata": {}
            }
            """;

        RestHandler.HTTPRestResponse response = handler.cancelTask(callContext, "", requestBody, MINIMAL_TASK.id());

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals(APPLICATION_JSON, response.getContentType());
        Assertions.assertTrue(response.getBody().contains(MINIMAL_TASK.id()));
    }

    @Test
    public void testCancelTaskWithNoMetadata() {
        RestHandler handler = new RestHandler(CARD, createCacheMetadata(), requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK, false);

        agentExecutorCancel = (context, agentEmitter) -> {
            Task task = context.getTask();
            agentEmitter.cancel();
        };

        // Request body without metadata field
        String requestBody = "{}";

        RestHandler.HTTPRestResponse response = handler.cancelTask(callContext, "", requestBody, MINIMAL_TASK.id());

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals(APPLICATION_JSON, response.getContentType());
        Assertions.assertTrue(response.getBody().contains(MINIMAL_TASK.id()));
    }

    @Test
    public void testCancelTaskWithNullBody() {
        RestHandler handler = new RestHandler(CARD, createCacheMetadata(), requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK, false);

        agentExecutorCancel = (context, agentEmitter) -> {
            Task task = context.getTask();
            agentEmitter.cancel();
        };

        // Null body should still work - metadata defaults to empty map
        RestHandler.HTTPRestResponse response = handler.cancelTask(callContext, "", null, MINIMAL_TASK.id());

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals(APPLICATION_JSON, response.getContentType());
        Assertions.assertTrue(response.getBody().contains(MINIMAL_TASK.id()));
    }

    @Test
    public void testSendStreamingMessageSuccess() {
        RestHandler handler = new RestHandler(CARD, createCacheMetadata(), requestHandler, internalExecutor);
        agentExecutorExecute = (context, agentEmitter) -> {
            agentEmitter.sendMessage(context.getMessage());
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

        RestHandler.HTTPRestResponse response = handler.sendStreamingMessage(callContext, "", requestBody);
        Assertions.assertEquals(200, response.getStatusCode(), response.toString());
        Assertions.assertInstanceOf(RestHandler.HTTPRestStreamingResponse.class, response);
        RestHandler.HTTPRestStreamingResponse streamingResponse = (RestHandler.HTTPRestStreamingResponse) response;
        Assertions.assertNotNull(streamingResponse.getPublisher());
        Assertions.assertEquals("text/event-stream", streamingResponse.getContentType());
    }

    @Test
    public void testSendStreamingMessageNotSupported() {
        AgentCard card = createAgentCard(false, true);
        RestHandler handler = new RestHandler(card, createCacheMetadata(card), requestHandler, internalExecutor);

        String requestBody = """
            {
                "contextId": "ctx123",
                "role": "ROLE_USER",
                "parts": [{
                    "text": "Hello"
                }]
            }
            """;

        RestHandler.HTTPRestResponse response = handler.sendStreamingMessage(callContext, "", requestBody);

        assertProblemDetail(response, 400,
                "UNSUPPORTED_OPERATION",
                "Streaming is not supported by the agent");
    }

    @Test
    public void testPushNotificationConfigSuccess() {
        RestHandler handler = new RestHandler(CARD, createCacheMetadata(), requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK, false);

        String requestBody = """
            {
              "id": "default-config-id",
              "taskId": "%s",
              "url": "https://example.com/callback",
              "authentication": {
                "scheme": "jwt"
              }
            }""".formatted(MINIMAL_TASK.id());

        RestHandler.HTTPRestResponse response = handler.createTaskPushNotificationConfiguration(callContext, "", requestBody, MINIMAL_TASK.id());

        Assertions.assertEquals(201, response.getStatusCode(), response.toString());
        Assertions.assertEquals(APPLICATION_JSON, response.getContentType());
        Assertions.assertNotNull(response.getBody());
    }

    @Test
    public void testPushNotificationConfigNotSupported() {
        AgentCard card = createAgentCard(true, false);
        RestHandler handler = new RestHandler(card, createCacheMetadata(card), requestHandler, internalExecutor);

        String requestBody = """
            {
                "id": "default-config-id",
                "taskId": "%s",
                "url": "http://example.com"
            }
            """.formatted(MINIMAL_TASK.id());

        RestHandler.HTTPRestResponse response = handler.createTaskPushNotificationConfiguration(callContext, "", requestBody, MINIMAL_TASK.id());

        assertProblemDetail(response, 400,
                "PUSH_NOTIFICATION_NOT_SUPPORTED",
                "Push Notification is not supported");
    }

    @Test
    public void testGetPushNotificationConfig() {
        RestHandler handler = new RestHandler(CARD, createCacheMetadata(), requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK, false);

        // First, create a push notification config
        String createRequestBody = """
            {
              "id": "default-config-id",
              "taskId": "%s",
              "url": "https://example.com/callback",
              "authentication": {
                "scheme": "jwt"
              }
            }""".formatted(MINIMAL_TASK.id());
        RestHandler.HTTPRestResponse response = handler.createTaskPushNotificationConfiguration(callContext, "", createRequestBody, MINIMAL_TASK.id());
        Assertions.assertEquals(201, response.getStatusCode(), response.toString());
        Assertions.assertEquals(APPLICATION_JSON, response.getContentType());
        // Now get it
        response = handler.getTaskPushNotificationConfiguration(callContext, "", MINIMAL_TASK.id(), "default-config-id");
        Assertions.assertEquals(200, response.getStatusCode(), response.toString());
        Assertions.assertEquals(APPLICATION_JSON, response.getContentType());
    }

    @Test
    public void testDeletePushNotificationConfig() {
        RestHandler handler = new RestHandler(CARD, createCacheMetadata(), requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK, false);
        RestHandler.HTTPRestResponse response = handler.deleteTaskPushNotificationConfiguration(callContext, "", MINIMAL_TASK.id(), "default-config-id");
        Assertions.assertEquals(204, response.getStatusCode());
    }

    @Test
    public void testListPushNotificationConfigs() {
        RestHandler handler = new RestHandler(CARD, createCacheMetadata(), requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK, false);

        RestHandler.HTTPRestResponse response = handler.listTaskPushNotificationConfigurations(callContext, "", MINIMAL_TASK.id(), 0, "");

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals(APPLICATION_JSON, response.getContentType());
        Assertions.assertNotNull(response.getBody());
    }

    @Test
    public void testPushNotificationConfigParseError_DoesNotLogSensitiveData() {
        RestHandler handler = new RestHandler(CARD, createCacheMetadata(), requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK, false);

        String requestBody = """
            {
              "id": "default-config-id",
              "taskId": "%s",
              "url": "https://webhook.example.com",
              "token": "secret-token-12345",
              "authentication": {
                "scheme": "Bearer",
                "credentials": "oauth-secret-67890"
              },
              "unknownField": "trigger-parse-error"
            }""".formatted(MINIMAL_TASK.id());

        // Request body with sensitive data and an unknown field to trigger parse error
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(RestHandler.class.getName());
        LogCaptureAssertions.assertSensitiveDataNotLogged(logger,
                () -> Assertions.assertEquals(422,
                        handler.createTaskPushNotificationConfiguration(callContext, "", requestBody, MINIMAL_TASK.id())
                                .getStatusCode()),
                "secret-token-12345", "oauth-secret-67890");
    }

    @Test
    public void testHttpStatusCodeMapping() {
        RestHandler handler = new RestHandler(CARD, createCacheMetadata(), requestHandler, internalExecutor);

        // Test 400 for invalid request
        RestHandler.HTTPRestResponse response = handler.sendMessage(callContext, "", "");
        Assertions.assertEquals(400, response.getStatusCode());

        // Test 404 for not found
        response = handler.getTask(callContext, "", "nonexistent", 0);
        Assertions.assertEquals(404, response.getStatusCode());
    }

    @Test
    public void testStreamingDoesNotBlockMainThread() throws Exception {
        RestHandler handler = new RestHandler(CARD, createCacheMetadata(), requestHandler, internalExecutor);

        // Track if the main thread gets blocked during streaming
        AtomicBoolean eventReceived = new AtomicBoolean(false);
        CountDownLatch streamStarted = new CountDownLatch(1);
        CountDownLatch eventProcessed = new CountDownLatch(1);
        agentExecutorExecute = (context, agentEmitter) -> {
            // Wait a bit to ensure the main thread continues
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            agentEmitter.sendMessage(context.getMessage());
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
        RestHandler.HTTPRestResponse response = handler.sendStreamingMessage(callContext, "", requestBody);

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

    @Test
    public void testExtensionSupportRequiredErrorOnSendMessage() {
        // Create AgentCard with a required extension
        AgentCard cardWithExtension = AgentCard.builder()
                .name("test-card")
                .description("Test card with required extension")
                .supportedInterfaces(Collections.singletonList(new AgentInterface("HTTP+JSON", "http://localhost:9999")))
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(true)
                        .extensions(List.of(
                                AgentExtension.builder()
                                        .uri("https://example.com/test-extension")
                                        .required(true)
                                        .build()
                        ))
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .build();

        RestHandler handler = new RestHandler(cardWithExtension, createCacheMetadata(cardWithExtension), requestHandler, internalExecutor);

        String requestBody = """
            {
              "message": {
                "messageId": "message-1234",
                "contextId": "context-1234",
                "role": "ROLE_USER",
                "parts": [{
                  "text": "tell me a joke"
                }],
                "metadata": {}
              },
              "configuration": {
                "returnImmediately": false
              }
            }""";

        RestHandler.HTTPRestResponse response = handler.sendMessage(callContext, "", requestBody);

        assertProblemDetail(response, 400,
                "EXTENSION_SUPPORT_REQUIRED",
                "Required extension 'https://example.com/test-extension' was not requested by the client");
    }

    @Test
    public void testExtensionSupportRequiredErrorOnSendStreamingMessage() {
        // Create AgentCard with a required extension
        AgentCard cardWithExtension = AgentCard.builder()
                .name("test-card")
                .description("Test card with required extension")
                .supportedInterfaces(Collections.singletonList(new AgentInterface("HTTP+JSON", "http://localhost:9999")))
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(true)
                        .extensions(List.of(
                                AgentExtension.builder()
                                        .uri("https://example.com/streaming-extension")
                                        .required(true)
                                        .build()
                        ))
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .build();

        RestHandler handler = new RestHandler(cardWithExtension, createCacheMetadata(cardWithExtension), requestHandler, internalExecutor);

        String requestBody = """
            {
              "message": {
                "role": "ROLE_USER",
                "parts": [{
                  "text": "tell me some jokes"
                }],
                "messageId": "message-1234",
                "contextId": "context-1234"
              },
              "configuration": {
                "acceptedOutputModes": ["text"]
              }
            }""";

        RestHandler.HTTPRestResponse response = handler.sendStreamingMessage(callContext, "", requestBody);

        // Streaming responses embed errors in the stream with status 200
        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertInstanceOf(RestHandler.HTTPRestStreamingResponse.class, response);
        
        // Subscribe to publisher and verify error in stream
        RestHandler.HTTPRestStreamingResponse streamingResponse = (RestHandler.HTTPRestStreamingResponse) response;
        Flow.Publisher<String> publisher = streamingResponse.getPublisher();
        
        AtomicBoolean errorFound = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);
        
        publisher.subscribe(new Flow.Subscriber<String>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(1);
            }

            @Override
            public void onNext(String item) {
                JsonObject body = JsonParser.parseString(item).getAsJsonObject();
                if (body.has("error")) {
                    JsonObject error = body.getAsJsonObject("error");
                    var details = error.has("details") ? error.getAsJsonArray("details") : null;
                    if (details != null && !details.isEmpty()) {
                        String reason = details.get(0).getAsJsonObject().get("reason").getAsString();
                        if ("EXTENSION_SUPPORT_REQUIRED".equals(reason) &&
                            item.contains("https://example.com/streaming-extension")) {
                            errorFound.set(true);
                        }
                    }
                }
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });

        try {
            Assertions.assertTrue(latch.await(1, TimeUnit.SECONDS));
            Assertions.assertTrue(errorFound.get(), "Error should be found in streaming response");
        } catch (InterruptedException e) {
            Assertions.fail("Test interrupted");
        }
    }

    @Test
    public void testRequiredExtensionProvidedSuccess() {
        // Create AgentCard with a required extension
        AgentCard cardWithExtension = AgentCard.builder()
                .name("test-card")
                .description("Test card with required extension")
                .supportedInterfaces(Collections.singletonList(new AgentInterface("HTTP+JSON", "http://localhost:9999")))
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(true)
                        .extensions(List.of(
                                AgentExtension.builder()
                                        .uri("https://example.com/required-extension")
                                        .required(true)
                                        .build()
                        ))
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .build();

        RestHandler handler = new RestHandler(cardWithExtension, createCacheMetadata(cardWithExtension), requestHandler, internalExecutor);

        // Create context WITH the required extension
        Set<String> requestedExtensions = new HashSet<>();
        requestedExtensions.add("https://example.com/required-extension");
        ServerCallContext contextWithExtension = new ServerCallContext(
                UnauthenticatedUser.INSTANCE,
                Map.of("foo", "bar"),
                requestedExtensions,
                "1.0"
        );

        agentExecutorExecute = (context, agentEmitter) -> {
            agentEmitter.sendMessage(context.getMessage());
        };

        String requestBody = """
            {
              "message": {
                "messageId": "message-1234",
                "contextId": "context-1234",
                "role": "ROLE_USER",
                "parts": [{
                  "text": "tell me a joke"
                }],
                "metadata": {}
              },
              "configuration": {
                "returnImmediately": false
              }
            }""";

        RestHandler.HTTPRestResponse response = handler.sendMessage(contextWithExtension, "", requestBody);

        // Should succeed without error
        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals(APPLICATION_JSON, response.getContentType());
        Assertions.assertNotNull(response.getBody());
    }

    @Test
    public void testVersionNotSupportedErrorOnSendMessage() {
        // Create AgentCard with protocol version 1.0
        AgentCard agentCard = AgentCard.builder()
                .name("test-card")
                .description("Test card with version 1.0")
                .supportedInterfaces(Collections.singletonList(new AgentInterface("HTTP+JSON", "http://localhost:9999")))
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(false)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .build();

        RestHandler handler = new RestHandler(agentCard, createCacheMetadata(agentCard), requestHandler, internalExecutor);

        // Create context with incompatible version 2.0
        ServerCallContext contextWithVersion = new ServerCallContext(
                UnauthenticatedUser.INSTANCE,
                Map.of("foo", "bar"),
                new HashSet<>(),
                "2.0"  // Incompatible version
        );

        String requestBody = """
            {
              "message": {
                "messageId": "message-1234",
                "contextId": "context-1234",
                "role": "ROLE_USER",
                "parts": [{
                  "text": "tell me a joke"
                }],
                "metadata": {}
              },
              "configuration": {
                "returnImmediately": false
              }
            }""";

        RestHandler.HTTPRestResponse response = handler.sendMessage(contextWithVersion, "", requestBody);

        assertProblemDetail(response, 400,
                "VERSION_NOT_SUPPORTED",
                "Protocol version '2.0' is not supported. Supported versions: [1.0]");
    }

    @Test
    public void testVersionNotSupportedErrorOnSendStreamingMessage() {
        // Create AgentCard with protocol version 1.0
        AgentCard agentCard = AgentCard.builder()
                .name("test-card")
                .description("Test card with version 1.0")
                .supportedInterfaces(Collections.singletonList(new AgentInterface("HTTP+JSON", "http://localhost:9999")))
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(false)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .build();

        RestHandler handler = new RestHandler(agentCard, createCacheMetadata(agentCard), requestHandler, internalExecutor);

        // Create context with incompatible version 2.0
        ServerCallContext contextWithVersion = new ServerCallContext(
                UnauthenticatedUser.INSTANCE,
                Map.of("foo", "bar"),
                new HashSet<>(),
                "2.0"  // Incompatible version
        );

        String requestBody = """
            {
              "message": {
                "role": "ROLE_USER",
                "parts": [{
                  "text": "tell me some jokes"
                }],
                "messageId": "message-1234",
                "contextId": "context-1234"
              },
              "configuration": {
                "acceptedOutputModes": ["text"]
              }
            }""";

        RestHandler.HTTPRestResponse response = handler.sendStreamingMessage(contextWithVersion, "", requestBody);

        // Streaming responses embed errors in the stream with status 200
        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertInstanceOf(RestHandler.HTTPRestStreamingResponse.class, response);

        // Subscribe to publisher and verify error in stream
        RestHandler.HTTPRestStreamingResponse streamingResponse = (RestHandler.HTTPRestStreamingResponse) response;
        Flow.Publisher<String> publisher = streamingResponse.getPublisher();

        AtomicBoolean errorFound = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        publisher.subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(String item) {
                JsonObject body = JsonParser.parseString(item).getAsJsonObject();
                if (body.has("error")) {
                    JsonObject error = body.getAsJsonObject("error");
                    var details = error.has("details") ? error.getAsJsonArray("details") : null;
                    if (details != null && !details.isEmpty()) {
                        String reason = details.get(0).getAsJsonObject().get("reason").getAsString();
                        if ("VERSION_NOT_SUPPORTED".equals(reason) &&
                            error.has("message") && error.get("message").getAsString().contains("2.0")) {
                            errorFound.set(true);
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });

        try {
            Assertions.assertTrue(latch.await(1, TimeUnit.SECONDS));
            Assertions.assertTrue(errorFound.get(), "Error should be found in streaming response");
        } catch (InterruptedException e) {
            Assertions.fail("Test interrupted");
        }
    }

    @Test
    public void testCompatibleVersionSuccess() {
        // Create AgentCard with protocol version 1.0
        AgentCard agentCard = AgentCard.builder()
                .name("test-card")
                .description("Test card with version 1.0")
                .supportedInterfaces(Collections.singletonList(new AgentInterface("HTTP+JSON", "http://localhost:9999")))
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(false)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .build();

        RestHandler handler = new RestHandler(agentCard, createCacheMetadata(agentCard), requestHandler, internalExecutor);

        // Create context with compatible version 1.1
        ServerCallContext contextWithVersion = new ServerCallContext(
                UnauthenticatedUser.INSTANCE,
                Map.of("foo", "bar"),
                new HashSet<>(),
                "1.1"  // Compatible version (same major version)
        );

        agentExecutorExecute = (context, agentEmitter) -> {
            agentEmitter.sendMessage(context.getMessage());
        };

        String requestBody = """
            {
              "message": {
                "messageId": "message-1234",
                "contextId": "context-1234",
                "role": "ROLE_USER",
                "parts": [{
                  "text": "tell me a joke"
                }],
                "metadata": {}
              },
              "configuration": {
                "returnImmediately": false
              }
            }""";

        RestHandler.HTTPRestResponse response = handler.sendMessage(contextWithVersion, "", requestBody);

        // Should succeed without error
        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals(APPLICATION_JSON, response.getContentType());
        Assertions.assertNotNull(response.getBody());
    }

    @Test
    public void testNoVersionDefaultsTo0_3_RejectedByV10OnlyServer() {
        // Per spec Section 3.6.2: missing A2A-Version defaults to 0.3
        AgentCard agentCard = AgentCard.builder()
                .name("test-card")
                .description("Test card with version 1.0")
                .supportedInterfaces(Collections.singletonList(new AgentInterface("HTTP+JSON", "http://localhost:9999")))
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(false)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .build();

        RestHandler handler = new RestHandler(agentCard, createCacheMetadata(agentCard), requestHandler, internalExecutor);

        agentExecutorExecute = (context, agentEmitter) -> {
            agentEmitter.sendMessage(context.getMessage());
        };

        // Context with no version — defaults to 0.3, incompatible with v1.0-only server
        ServerCallContext noVersionContext = new ServerCallContext(
                UnauthenticatedUser.INSTANCE, Map.of("foo", "bar"), new HashSet<>());

        String requestBody = """
            {
              "message": {
                "messageId": "message-1234",
                "contextId": "context-1234",
                "role": "ROLE_USER",
                "parts": [{
                  "text": "tell me a joke"
                }],
                "metadata": {}
              },
              "configuration": {
                "returnImmediately": false
              }
            }""";

        RestHandler.HTTPRestResponse response = handler.sendMessage(noVersionContext, "", requestBody);

        // Should return error (0.3 is not supported by v1.0-only server)
        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertTrue(response.getBody().contains("0.3"));
        Assertions.assertTrue(response.getBody().contains("not supported"));
    }

    @Test
    public void testListTasksNegativeTimestampReturns422() {
        RestHandler handler = new RestHandler(CARD, createCacheMetadata(), requestHandler, internalExecutor);

        // Negative timestamp should return 422 (Invalid params)
        RestHandler.HTTPRestResponse response = handler.listTasks(callContext, "", null, null, null, null,
                null, "-1", null);

        assertProblemDetail(response, 422,
                "INVALID_PARAMS", "Invalid params");
    }

    @Test
    public void testListTasksUnixMillisecondsTimestamp() {
        RestHandler handler = new RestHandler(CARD, createCacheMetadata(), requestHandler, internalExecutor);
        // Unix milliseconds timestamp are no longer accepted
        RestHandler.HTTPRestResponse response = handler.listTasks(callContext, "", null, null, null, null,
                null, "1234567", null);
        Assertions.assertEquals(422, response.getStatusCode());
    }

    @Test
    public void testListTasksProtobufEnumStatus() {
        RestHandler handler = new RestHandler(CARD, createCacheMetadata(), requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK, false);

        // Protobuf enum format (TASK_STATE_SUBMITTED) should be accepted
        RestHandler.HTTPRestResponse response = handler.listTasks(callContext, "", null, "TASK_STATE_SUBMITTED", null, null,
                null, null, null);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals(APPLICATION_JSON, response.getContentType());
        Assertions.assertTrue(response.getBody().contains(MINIMAL_TASK.id()));
    }

    @Test
    public void testListTasksEnumConstantStatus() {
        RestHandler handler = new RestHandler(CARD, createCacheMetadata(), requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK, false);

        // Enum constant format (TASK_STATE_SUBMITTED) should be accepted
        RestHandler.HTTPRestResponse response = handler.listTasks(callContext, "", null, "TASK_STATE_SUBMITTED", null, null,
                null, null, null);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals(APPLICATION_JSON, response.getContentType());
        Assertions.assertTrue(response.getBody().contains(MINIMAL_TASK.id()));
    }

    @Test
    public void testListTasksEmptyResultIncludesAllFields() {
        RestHandler handler = new RestHandler(CARD, createCacheMetadata(), requestHandler, internalExecutor);

        // Query for a context that doesn't exist - should return empty result with all fields
        RestHandler.HTTPRestResponse response = handler.listTasks(callContext, "", "nonexistent-context-id", null, null, null,
                null, null, null);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals(APPLICATION_JSON, response.getContentType());

        String body = response.getBody();
        // Verify all required fields are present (not missing)
        Assertions.assertTrue(body.contains("\"tasks\""), "Response should contain tasks field");
        Assertions.assertTrue(body.contains("\"totalSize\""), "Response should contain totalSize field");
        Assertions.assertTrue(body.contains("\"pageSize\""), "Response should contain pageSize field");
        Assertions.assertTrue(body.contains("\"nextPageToken\""), "Response should contain nextPageToken field");
        // Verify empty array, not null
        Assertions.assertTrue(body.contains("\"tasks\":[]") || body.contains("\"tasks\": []"),
                "tasks should be empty array");
    }

    @Test
    void constructorDoesNotResolveAgentCardInstances() {
        Instance<AgentCard> throwOnGet = TestInstances.throwOnGet();

        Assertions.assertDoesNotThrow(() -> new RestHandler(throwOnGet, throwOnGet,
                createCacheMetadata(), requestHandler, internalExecutor, null));
    }

    private static void assertProblemDetail(RestHandler.HTTPRestResponse response,
                                            int expectedStatus, String expectedReason, String expectedMessage) {
        Assertions.assertEquals(expectedStatus, response.getStatusCode());
        Assertions.assertEquals(APPLICATION_JSON, response.getContentType());
        JsonObject body = JsonParser.parseString(response.getBody()).getAsJsonObject();
        Assertions.assertTrue(body.has("error"), "error wrapper should be present");
        JsonObject error = body.getAsJsonObject("error");
        Assertions.assertEquals(expectedStatus, error.get("code").getAsInt(), "code field mismatch");
        Assertions.assertEquals(expectedMessage, error.get("message").getAsString(), "message field mismatch");
        Assertions.assertTrue(error.has("status"), "status field should be present");
        Assertions.assertTrue(error.has("details"), "details field should be present");
        var details = error.getAsJsonArray("details");
        Assertions.assertFalse(details.isEmpty(), "details array should not be empty");
        JsonObject detail = details.get(0).getAsJsonObject();
        Assertions.assertEquals("type.googleapis.com/google.rpc.ErrorInfo", detail.get("@type").getAsString(), "@type field mismatch");
        Assertions.assertEquals(expectedReason, detail.get("reason").getAsString(), "reason field mismatch");
        Assertions.assertEquals("a2a-protocol.org", detail.get("domain").getAsString(), "domain field mismatch");
    }

    @Test
    public void testSendMessageSanitizesInternalError() {
        // A non-A2AError exception must not leak its message to the client
        RequestHandler mocked = Mockito.mock(RequestHandler.class);
        Mockito.doThrow(new RuntimeException("sensitive detail: /var/lib/secret/config.db"))
                .when(mocked).onMessageSend(Mockito.any(), Mockito.any());

        RestHandler handler = new RestHandler(CARD, createCacheMetadata(), mocked, internalExecutor);
        String requestBody = """
                {
                  "message": {
                    "messageId": "message-1234",
                    "contextId": "context-1234",
                    "role": "ROLE_USER",
                    "parts": [{"text": "hello"}],
                    "metadata": {}
                  }
                }""";

        RestHandler.HTTPRestResponse response = handler.sendMessage(callContext, "", requestBody);

        JsonObject body = JsonParser.parseString(response.getBody()).getAsJsonObject();
        JsonObject error = body.getAsJsonObject("error");
        Assertions.assertEquals(500, error.get("code").getAsInt());
        Assertions.assertEquals("Internal error", error.get("message").getAsString());
        Assertions.assertFalse(error.get("message").getAsString().contains("sensitive"),
                "Internal exception message must not be leaked to the client");
    }

    @Test
    public void testVersionNotSupportedErrorOnGetTask() {
        RestHandler handler = versionTestHandler();
        taskStore.save(MINIMAL_TASK, false);

        assertVersionRejected(handler.getTask(incompatibleVersionContext(), "", MINIMAL_TASK.id(), null));
    }

    @Test
    public void testVersionNotSupportedErrorOnListTasks() {
        RestHandler handler = versionTestHandler();
        taskStore.save(MINIMAL_TASK, false);

        assertVersionRejected(handler.listTasks(incompatibleVersionContext(), "",
                MINIMAL_TASK.contextId(), null, null, null, null, null, null));
    }

    @Test
    public void testVersionNotSupportedErrorOnCancelTask() {
        RestHandler handler = versionTestHandler();
        taskStore.save(MINIMAL_TASK, false);

        // Without this the executor emits nothing, and a cancel that reaches the request handler
        // waits forever for a final event rather than failing the assertion.
        agentExecutorCancel = (context, agentEmitter) -> agentEmitter.cancel();

        assertVersionRejected(handler.cancelTask(incompatibleVersionContext(), "", "{}", MINIMAL_TASK.id()));
    }

    @Test
    public void testVersionNotSupportedErrorOnCreateTaskPushNotificationConfiguration() {
        RestHandler handler = versionTestHandler();
        taskStore.save(MINIMAL_TASK, false);

        String requestBody = """
            {
              "id": "config-1",
              "taskId": "%s",
              "url": "http://example.com"
            }""".formatted(MINIMAL_TASK.id());

        assertVersionRejected(handler.createTaskPushNotificationConfiguration(
                incompatibleVersionContext(), "", requestBody, MINIMAL_TASK.id()));
    }

    @Test
    public void testVersionNotSupportedErrorOnGetTaskPushNotificationConfiguration() {
        RestHandler handler = versionTestHandler();
        taskStore.save(MINIMAL_TASK, false);

        assertVersionRejected(handler.getTaskPushNotificationConfiguration(
                incompatibleVersionContext(), "", MINIMAL_TASK.id(), "config-1"));
    }

    @Test
    public void testVersionNotSupportedErrorOnListTaskPushNotificationConfigurations() {
        RestHandler handler = versionTestHandler();
        taskStore.save(MINIMAL_TASK, false);

        assertVersionRejected(handler.listTaskPushNotificationConfigurations(
                incompatibleVersionContext(), "", MINIMAL_TASK.id(), 10, ""));
    }

    @Test
    public void testVersionNotSupportedErrorOnDeleteTaskPushNotificationConfiguration() {
        RestHandler handler = versionTestHandler();
        taskStore.save(MINIMAL_TASK, false);

        assertVersionRejected(handler.deleteTaskPushNotificationConfiguration(
                incompatibleVersionContext(), "", MINIMAL_TASK.id(), "config-1"));
    }

    @Test
    public void testVersionNotSupportedErrorOnGetExtendedAgentCard() {
        AgentCard card = versionTestCard();
        AgentCard extended = AgentCard.builder(card).description("extended").build();
        Instance<AgentCard> extendedInstance = new FixedInstance<>(extended);

        RestHandler handler = new RestHandler(new FixedInstance<>(card), extendedInstance,
                createCacheMetadata(card), requestHandler, internalExecutor, null);

        assertVersionRejected(handler.getExtendedAgentCard(incompatibleVersionContext(), ""));
    }

    @Test
    public void testExtendedAgentCardWithRouterKnownTenant() {
        AgentCard cardWithExtCapability = AgentCard.builder(CARD)
                .capabilities(AgentCapabilities.builder().extendedAgentCard(true).build()).build();
        AgentCard tenantCard = AgentCard.builder(cardWithExtCapability).name("acme-card").build();
        AgentCardRouter router = tenant -> "acme".equals(tenant) ? tenantCard : cardWithExtCapability;

        RestHandler handler = new RestHandler(new FixedInstance<>(cardWithExtCapability), null,
                createCacheMetadata(cardWithExtCapability), requestHandler, internalExecutor,
                new FixedInstance<>(router));

        RestHandler.HTTPRestResponse response = handler.getExtendedAgentCard(callContext, "acme");

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals(APPLICATION_JSON, response.getContentType());
        Assertions.assertTrue(response.getBody().contains("acme-card"));
    }

    @Test
    public void testExtendedAgentCardWithRouterReturnsNull() {
        AgentCard cardWithExtCapability = AgentCard.builder(CARD)
                .capabilities(AgentCapabilities.builder().extendedAgentCard(true).build()).build();
        AgentCardRouter router = tenant -> null;

        RestHandler handler = new RestHandler(new FixedInstance<>(cardWithExtCapability), null,
                createCacheMetadata(cardWithExtCapability), requestHandler, internalExecutor,
                new FixedInstance<>(router));

        RestHandler.HTTPRestResponse response = handler.getExtendedAgentCard(callContext, "acme");

        assertProblemDetail(response, 400,
                "EXTENDED_AGENT_CARD_NOT_CONFIGURED", "Extended Card not configured");
    }

    @Test
    public void testExtendedAgentCardWithoutRouter() {
        AgentCard cardWithExtCapability = AgentCard.builder(CARD)
                .capabilities(AgentCapabilities.builder().extendedAgentCard(true).build()).build();
        AgentCard extended = AgentCard.builder(cardWithExtCapability).description("extended").build();
        Instance<AgentCard> extendedInstance = new FixedInstance<>(extended);

        RestHandler handler = new RestHandler(new FixedInstance<>(cardWithExtCapability), extendedInstance,
                createCacheMetadata(cardWithExtCapability), requestHandler, internalExecutor, null);

        RestHandler.HTTPRestResponse response = handler.getExtendedAgentCard(callContext, "acme");

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertTrue(response.getBody().contains("extended"));
    }

    // -------------------------------------------------------------------------
    // Public agent card — tenant routing
    // -------------------------------------------------------------------------

    @Test
    public void testGetPublicAgentCard_withRouterKnownTenant_returns200() {
        AgentCard tenantCard = AgentCard.builder(CARD).name("acme-public").build();
        AgentCardRouter router = new AgentCardRouter() {
            @Override public AgentCard resolveExtendedCard(String t) { return null; }
            @Override public AgentCard resolvePublicCard(String t) { return "acme".equals(t) ? tenantCard : null; }
        };

        RestHandler handler = new RestHandler(new FixedInstance<>(CARD), null,
                createCacheMetadata(CARD), requestHandler, internalExecutor, new FixedInstance<>(router));

        RestHandler.HTTPRestResponse response = handler.getAgentCard("acme");

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertTrue(response.getBody().contains("acme-public"));
    }

    @Test
    public void testGetPublicAgentCard_withRouterUnknownTenant_returns404() {
        AgentCardRouter router = new AgentCardRouter() {
            @Override public AgentCard resolveExtendedCard(String t) { return null; }
            @Override public AgentCard resolvePublicCard(String t) { return null; }
        };

        RestHandler handler = new RestHandler(new FixedInstance<>(CARD), null,
                createCacheMetadata(CARD), requestHandler, internalExecutor, new FixedInstance<>(router));

        RestHandler.HTTPRestResponse response = handler.getAgentCard("unknown");

        Assertions.assertEquals(404, response.getStatusCode());
    }

    @Test
    public void testGetPublicAgentCard_noRouterWithTenant_returnsDefaultCard() {
        // Single-tenant server: no AgentCardRouter configured; tenant segment is ignored.
        RestHandler handler = new RestHandler(new FixedInstance<>(CARD), null,
                createCacheMetadata(CARD), requestHandler, internalExecutor, null);

        RestHandler.HTTPRestResponse response = handler.getAgentCard("any-tenant");

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertTrue(response.getBody().contains(CARD.name()));
    }

    @Test
    public void testGetPublicAgentCard_noTenant_returnsDefaultCard() {
        RestHandler handler = new RestHandler(new FixedInstance<>(CARD), null,
                createCacheMetadata(CARD), requestHandler, internalExecutor, null);

        RestHandler.HTTPRestResponse response = handler.getAgentCard((String) null);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertTrue(response.getBody().contains(CARD.name()));
    }

    @Test
    public void testVersionNotSupportedErrorOnSubscribeToTask() throws Exception {
        RestHandler handler = versionTestHandler();
        taskStore.save(MINIMAL_TASK, false);

        RestHandler.HTTPRestResponse response =
                handler.subscribeToTask(incompatibleVersionContext(), "", MINIMAL_TASK.id());

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertInstanceOf(RestHandler.HTTPRestStreamingResponse.class, response);
        assertVersionRejectedInStream((RestHandler.HTTPRestStreamingResponse) response);
    }

    /**
     * A card whose sole interface speaks protocol version 1.0, with every capability enabled so
     * that each operation reaches the version check instead of stopping at a capability guard.
     */
    private static AgentCard versionTestCard() {
        return AgentCard.builder()
                .name("test-card")
                .description("Test card with version 1.0")
                .supportedInterfaces(Collections.singletonList(new AgentInterface("HTTP+JSON", "http://localhost:9999")))
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(true)
                        .extendedAgentCard(true)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .build();
    }

    private RestHandler versionTestHandler() {
        AgentCard card = versionTestCard();
        return new RestHandler(card, createCacheMetadata(card), requestHandler, internalExecutor);
    }

    /**
     * A context requesting version 2.0, whose major differs from the card built by
     * {@link #versionTestCard()} and is therefore incompatible under section 3.6.2.
     */
    private static ServerCallContext incompatibleVersionContext() {
        return new ServerCallContext(UnauthenticatedUser.INSTANCE, Map.of("foo", "bar"), new HashSet<>(), "2.0");
    }

    /**
     * Asserts that an operation answered with the problem detail carried by a version refusal,
     * rather than with a success status or an unrelated error.
     *
     * @param response the response an operation returned
     */
    private static void assertVersionRejected(RestHandler.HTTPRestResponse response) {
        assertProblemDetail(response, 400, "VERSION_NOT_SUPPORTED",
                "Protocol version '2.0' is not supported. Supported versions: [1.0]");
    }

    /**
     * Asserts the same refusal for a streaming operation, which answers 200 and embeds the error
     * as an event rather than surfacing it in the status line.
     *
     * @param response the streaming response under test
     * @throws InterruptedException if the wait for termination is interrupted
     */
    private static void assertVersionRejectedInStream(RestHandler.HTTPRestStreamingResponse response)
            throws InterruptedException {
        AtomicBoolean errorFound = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        response.getPublisher().subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(String item) {
                JsonObject body = JsonParser.parseString(item).getAsJsonObject();
                if (!body.has("error")) {
                    return;
                }
                JsonObject error = body.getAsJsonObject("error");
                var details = error.has("details") ? error.getAsJsonArray("details") : null;
                if (details != null && !details.isEmpty()
                        && "VERSION_NOT_SUPPORTED".equals(details.get(0).getAsJsonObject().get("reason").getAsString())
                        && error.get("message").getAsString().contains("2.0")) {
                    errorFound.set(true);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });

        Assertions.assertTrue(latch.await(2, TimeUnit.SECONDS), "Expected the stream to terminate within timeout");
        Assertions.assertTrue(errorFound.get(), "Expected a VERSION_NOT_SUPPORTED event in the stream");
    }
}
