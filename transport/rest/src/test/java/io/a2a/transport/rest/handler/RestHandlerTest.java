package io.a2a.transport.rest.handler;


import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.protobuf.InvalidProtocolBufferException;
import io.a2a.server.ServerCallContext;
import io.a2a.server.auth.UnauthenticatedUser;
import io.a2a.server.requesthandlers.AbstractA2ARequestHandlerTest;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentExtension;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.Task;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(value = 1, unit = TimeUnit.MINUTES)
public class RestHandlerTest extends AbstractA2ARequestHandlerTest {

    private final ServerCallContext callContext = new ServerCallContext(UnauthenticatedUser.INSTANCE, Map.of("foo", "bar"), new HashSet<>());

    @Test
    public void testGetTaskSuccess() {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK, false);

        RestHandler.HTTPRestResponse response = handler.getTask(callContext, "", MINIMAL_TASK.id(), 0);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains(MINIMAL_TASK.id()));

        response = handler.getTask(callContext, "", MINIMAL_TASK.id(), 2);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains(MINIMAL_TASK.id()));
    }

    @Test
    public void testGetTaskNotFound() {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);

        RestHandler.HTTPRestResponse response = handler.getTask(callContext, "", "nonexistent", 0);

        Assertions.assertEquals(404, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains("TaskNotFoundError"));
    }

    @Test
    public void testListTasksStatusWireString() {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK, false);

        RestHandler.HTTPRestResponse response = handler.listTasks(callContext, "", null, "submitted", null, null,
                null, null, null);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains(MINIMAL_TASK.id()));
    }

    @Test
    public void testListTasksInvalidStatus() {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);

        RestHandler.HTTPRestResponse response = handler.listTasks(callContext, "", null, "not-a-status", null, null,
                null, null, null);

        Assertions.assertEquals(422, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains("InvalidParamsError"));
    }

    @Test
    public void testSendMessage() throws InvalidProtocolBufferException {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);
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
                  "blocking": true
              }
            }""";

        RestHandler.HTTPRestResponse response = handler.sendMessage(callContext, "", requestBody);
        Assertions.assertEquals(200, response.getStatusCode(), response.toString());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertNotNull(response.getBody());
    }

    @Test
    public void testSendMessageInvalidBody() {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);

        String invalidBody = "invalid json";
        RestHandler.HTTPRestResponse response = handler.sendMessage(callContext, "", invalidBody);

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
        RestHandler.HTTPRestResponse response = handler.sendMessage(callContext, "", requestBody);

        Assertions.assertEquals(422, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains("InvalidParamsError"));
    }

    @Test
    public void testSendMessageEmptyBody() {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);

        RestHandler.HTTPRestResponse response = handler.sendMessage(callContext, "", "");

        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains("InvalidRequestError"));
    }

    @Test
    public void testCancelTaskSuccess() {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK, false);

        agentExecutorCancel = (context, agentEmitter) -> {
            // We need to cancel the task or the EventConsumer never finds a 'final' event.
            // Looking at the Python implementation, they typically use AgentExecutors that
            // don't support cancellation. So my theory is the Agent updates the task to the CANCEL status
            Task task = context.getTask();
            agentEmitter.cancel();
        };

        RestHandler.HTTPRestResponse response = handler.cancelTask(callContext, "", MINIMAL_TASK.id());

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains(MINIMAL_TASK.id()));
    }

    @Test
    public void testCancelTaskNotFound() {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);

        RestHandler.HTTPRestResponse response = handler.cancelTask(callContext, "", "nonexistent");

        Assertions.assertEquals(404, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains("TaskNotFoundError"));
    }

    @Test
    public void testSendStreamingMessageSuccess() {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);
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

        RestHandler.HTTPRestResponse response = handler.sendStreamingMessage(callContext, "", requestBody);

        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertTrue(response.getBody().contains("InvalidRequestError"));
    }

    @Test
    public void testPushNotificationConfigSuccess() {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK, false);

        String requestBody = """
            {
              "taskId": "%s",
              "configId": "default-config-id",
              "config": {
                "url": "https://example.com/callback",
                "authentication": {
                  "scheme": "jwt"
                }
              }
            }""".formatted(MINIMAL_TASK.id());

        RestHandler.HTTPRestResponse response = handler.createTaskPushNotificationConfiguration(callContext, "", requestBody, MINIMAL_TASK.id());

        Assertions.assertEquals(201, response.getStatusCode(), response.toString());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertNotNull(response.getBody());
    }

    @Test
    public void testPushNotificationConfigNotSupported() {
        AgentCard card = createAgentCard(true, false);
        RestHandler handler = new RestHandler(card, requestHandler, internalExecutor);

        String requestBody = """
            {
                "taskId": "%s",
                "pushNotificationConfig": {
                    "url": "http://example.com"
                }
            }
            """.formatted(MINIMAL_TASK.id());

        RestHandler.HTTPRestResponse response = handler.createTaskPushNotificationConfiguration(callContext, "", requestBody, MINIMAL_TASK.id());

        Assertions.assertEquals(501, response.getStatusCode());
        Assertions.assertTrue(response.getBody().contains("PushNotificationNotSupportedError"));
    }

    @Test
    public void testGetPushNotificationConfig() {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK, false);

        // First, create a push notification config
        String createRequestBody = """
            {
              "taskId": "%s",
              "configId": "default-config-id",
              "config": {
                "url": "https://example.com/callback",
                "authentication": {
                  "scheme": "jwt"
                }
              }
            }""".formatted(MINIMAL_TASK.id());
        RestHandler.HTTPRestResponse response = handler.createTaskPushNotificationConfiguration(callContext, "", createRequestBody, MINIMAL_TASK.id());
        Assertions.assertEquals(201, response.getStatusCode(), response.toString());
        Assertions.assertEquals("application/json", response.getContentType());
        // Now get it
        response = handler.getTaskPushNotificationConfiguration(callContext, "", MINIMAL_TASK.id(), "default-config-id");
        Assertions.assertEquals(200, response.getStatusCode(), response.toString());
        Assertions.assertEquals("application/json", response.getContentType());
    }

    @Test
    public void testDeletePushNotificationConfig() {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK, false);
        RestHandler.HTTPRestResponse response = handler.deleteTaskPushNotificationConfiguration(callContext, "", MINIMAL_TASK.id(), "default-config-id");
        Assertions.assertEquals(204, response.getStatusCode());
    }

    @Test
    public void testListPushNotificationConfigs() {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK, false);

        RestHandler.HTTPRestResponse response = handler.listTaskPushNotificationConfigurations(callContext, "", MINIMAL_TASK.id(), 0, "");

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertNotNull(response.getBody());
    }

    @Test
    public void testHttpStatusCodeMapping() {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);

        // Test 400 for invalid request
        RestHandler.HTTPRestResponse response = handler.sendMessage(callContext, "", "");
        Assertions.assertEquals(400, response.getStatusCode());

        // Test 404 for not found
        response = handler.getTask(callContext, "", "nonexistent", 0);
        Assertions.assertEquals(404, response.getStatusCode());
    }

    @Test
    public void testStreamingDoesNotBlockMainThread() throws Exception {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);

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
                .supportedInterfaces(Collections.singletonList(new AgentInterface("REST", "http://localhost:9999")))
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

        RestHandler handler = new RestHandler(cardWithExtension, requestHandler, internalExecutor);

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
                "blocking": true
              }
            }""";

        RestHandler.HTTPRestResponse response = handler.sendMessage(callContext, "", requestBody);

        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains("ExtensionSupportRequiredError"));
        Assertions.assertTrue(response.getBody().contains("https://example.com/test-extension"));
    }

    @Test
    public void testExtensionSupportRequiredErrorOnSendStreamingMessage() {
        // Create AgentCard with a required extension
        AgentCard cardWithExtension = AgentCard.builder()
                .name("test-card")
                .description("Test card with required extension")
                .supportedInterfaces(Collections.singletonList(new AgentInterface("REST", "http://localhost:9999")))
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

        RestHandler handler = new RestHandler(cardWithExtension, requestHandler, internalExecutor);

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
                if (item.contains("ExtensionSupportRequiredError") && 
                    item.contains("https://example.com/streaming-extension")) {
                    errorFound.set(true);
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
                .supportedInterfaces(Collections.singletonList(new AgentInterface("REST", "http://localhost:9999")))
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

        RestHandler handler = new RestHandler(cardWithExtension, requestHandler, internalExecutor);

        // Create context WITH the required extension
        Set<String> requestedExtensions = new HashSet<>();
        requestedExtensions.add("https://example.com/required-extension");
        ServerCallContext contextWithExtension = new ServerCallContext(
                UnauthenticatedUser.INSTANCE,
                Map.of("foo", "bar"),
                requestedExtensions
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
                "blocking": true
              }
            }""";

        RestHandler.HTTPRestResponse response = handler.sendMessage(contextWithExtension, "", requestBody);

        // Should succeed without error
        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertNotNull(response.getBody());
    }

    @Test
    public void testVersionNotSupportedErrorOnSendMessage() {
        // Create AgentCard with protocol version 1.0
        AgentCard agentCard = AgentCard.builder()
                .name("test-card")
                .description("Test card with version 1.0")
                .supportedInterfaces(Collections.singletonList(new AgentInterface("REST", "http://localhost:9999")))
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(false)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .build();

        RestHandler handler = new RestHandler(agentCard, requestHandler, internalExecutor);

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
                "blocking": true
              }
            }""";

        RestHandler.HTTPRestResponse response = handler.sendMessage(contextWithVersion, "", requestBody);

        Assertions.assertEquals(501, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains("VersionNotSupportedError"));
        Assertions.assertTrue(response.getBody().contains("2.0"));
    }

    @Test
    public void testVersionNotSupportedErrorOnSendStreamingMessage() {
        // Create AgentCard with protocol version 1.0
        AgentCard agentCard = AgentCard.builder()
                .name("test-card")
                .description("Test card with version 1.0")
                .supportedInterfaces(Collections.singletonList(new AgentInterface("REST", "http://localhost:9999")))
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(false)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .build();

        RestHandler handler = new RestHandler(agentCard, requestHandler, internalExecutor);

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
                if (item.contains("VersionNotSupportedError") &&
                    item.contains("2.0")) {
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
                .supportedInterfaces(Collections.singletonList(new AgentInterface("REST", "http://localhost:9999")))
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(false)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .build();

        RestHandler handler = new RestHandler(agentCard, requestHandler, internalExecutor);

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
                "blocking": true
              }
            }""";

        RestHandler.HTTPRestResponse response = handler.sendMessage(contextWithVersion, "", requestBody);

        // Should succeed without error
        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertNotNull(response.getBody());
    }

    @Test
    public void testNoVersionDefaultsToCurrentVersionSuccess() {
        // Create AgentCard with protocol version 1.0 (current version)
        AgentCard agentCard = AgentCard.builder()
                .name("test-card")
                .description("Test card with version 1.0")
                .supportedInterfaces(Collections.singletonList(new AgentInterface("REST", "http://localhost:9999")))
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(false)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .build();

        RestHandler handler = new RestHandler(agentCard, requestHandler, internalExecutor);

        // Use default callContext (no version - should default to 1.0)
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
                "blocking": true
              }
            }""";

        RestHandler.HTTPRestResponse response = handler.sendMessage(callContext, "", requestBody);

        // Should succeed without error (defaults to 1.0)
        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertNotNull(response.getBody());
    }

    @Test
    public void testListTasksNegativeTimestampReturns422() {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);

        // Negative timestamp should return 422 (Invalid params)
        RestHandler.HTTPRestResponse response = handler.listTasks(callContext, "", null, null, null, null,
                null, "-1", null);

        Assertions.assertEquals(422, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains("InvalidParamsError"));
    }

    @Test
    public void testListTasksUnixMillisecondsTimestamp() {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);
        // Unix milliseconds timestamp are no longer accepted
        RestHandler.HTTPRestResponse response = handler.listTasks(callContext, "", null, null, null, null,
                null, "1234567", null);
        Assertions.assertEquals(422, response.getStatusCode());
    }

    @Test
    public void testListTasksProtobufEnumStatus() {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK, false);

        // Protobuf enum format (TASK_STATE_SUBMITTED) should be accepted
        RestHandler.HTTPRestResponse response = handler.listTasks(callContext, "", null, "TASK_STATE_SUBMITTED", null, null,
                null, null, null);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains(MINIMAL_TASK.id()));
    }

    @Test
    public void testListTasksEnumConstantStatus() {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK, false);

        // Enum constant format (SUBMITTED) should be accepted
        RestHandler.HTTPRestResponse response = handler.listTasks(callContext, "", null, "SUBMITTED", null, null,
                null, null, null);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains(MINIMAL_TASK.id()));
    }

    @Test
    public void testListTasksEmptyResultIncludesAllFields() {
        RestHandler handler = new RestHandler(CARD, requestHandler, internalExecutor);

        // Query for a context that doesn't exist - should return empty result with all fields
        RestHandler.HTTPRestResponse response = handler.listTasks(callContext, "", "nonexistent-context-id", null, null, null,
                null, null, null);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());

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
}
