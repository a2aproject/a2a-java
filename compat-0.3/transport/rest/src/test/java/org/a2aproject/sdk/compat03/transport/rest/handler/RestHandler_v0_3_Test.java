package org.a2aproject.sdk.compat03.transport.rest.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Map;

import org.a2aproject.sdk.compat03.conversion.A2AProtocol_v0_3;
import org.a2aproject.sdk.compat03.conversion.AbstractA2ARequestHandlerTest_v0_3;
import org.a2aproject.sdk.compat03.conversion.Convert_v0_3_To10RequestHandler;
import org.a2aproject.sdk.compat03.conversion.mappers.domain.TaskMapper_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCapabilities_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.UnauthenticatedUser;
import org.a2aproject.sdk.server.requesthandlers.LogCaptureAssertions;
import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.spec.GetTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.InternalError;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;

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
            UnauthenticatedUser.INSTANCE, Map.of("foo", "bar"), new HashSet<>(), A2AProtocol_v0_3.PROTOCOL_VERSION);

    // ========================================
    // GetTask Tests
    // ========================================

    @Test
    public void testGetTaskSuccess() {
        RestHandler_v0_3 handler = new RestHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

        // Save v0.3 task by converting to v1.0
        taskStore.save(TaskMapper_v0_3.INSTANCE.toV10(MINIMAL_TASK), false);

        RestHandler_v0_3.HTTPRestResponse response = handler.getTask(MINIMAL_TASK.id(), null, null, callContext);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains(MINIMAL_TASK.id()));

        // Test with different version parameter
        response = handler.getTask(MINIMAL_TASK.id(), "2", null, callContext);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains(MINIMAL_TASK.id()));
    }

    @Test
    public void testGetTaskNotFound() {
        RestHandler_v0_3 handler = new RestHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

        RestHandler_v0_3.HTTPRestResponse response = handler.getTask("nonexistent", null, null, callContext);

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

        RestHandler_v0_3.HTTPRestResponse response = handler.cancelTask(MINIMAL_TASK.id(), callContext);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains(MINIMAL_TASK.id()));
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
            }""".formatted(MINIMAL_TASK.id(), MINIMAL_TASK.id());

        RestHandler_v0_3.HTTPRestResponse response = handler.setTaskPushNotificationConfiguration(MINIMAL_TASK.id(), requestBody, callContext);

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
            """.formatted(MINIMAL_TASK.id());

        RestHandler_v0_3.HTTPRestResponse response = handler.setTaskPushNotificationConfiguration(MINIMAL_TASK.id(), requestBody, callContext);

        assertEquals(501, response.getStatusCode());
        assertTrue(response.getBody().contains("PushNotificationNotSupportedError"));
    }

    @Test
    public void testSetPushConfigUsesUrlTaskIdAndExplicitConfigId() {
        for (String body : new String[] {
                pushConfigBody("\"parent\":\"tasks/task\",\"configId\":\"chosen\",",
                        "tasks/task/pushNotificationConfigs/chosen"),
                pushConfigBody("\"configId\":\"chosen\",", ""),
                pushConfigBody("", "tasks/task/pushNotificationConfigs/chosen")}) {
            RequestHandler backend = mock(RequestHandler.class);
            when(backend.onCreateTaskPushNotificationConfig(any(), any())).thenThrow(new InternalError("delegated"));
            assertWireError(validationHandler(true, backend).setTaskPushNotificationConfiguration("task", body, callContext),
                    500, "InternalError", "delegated");
            ArgumentCaptor<TaskPushNotificationConfig> config = ArgumentCaptor.forClass(TaskPushNotificationConfig.class);
            verify(backend).onCreateTaskPushNotificationConfig(config.capture(), any());
            assertEquals("task", config.getValue().taskId());
            assertEquals("chosen", config.getValue().id());
        }
    }

    @Test
    public void testSetPushConfigDefaultsAbsentConfigIdToUrlTaskId() {
        for (String configId : new String[] {null, "", "   "}) {
            for (String name : new String[] {"", "tasks/task/pushNotificationConfigs", "tasks/task/pushNotificationConfigs/"}) {
                String fields = configId == null ? "" : "\"configId\":\"" + configId + "\",";
                RequestHandler backend = mock(RequestHandler.class);
                when(backend.onCreateTaskPushNotificationConfig(any(), any())).thenThrow(new InternalError("delegated"));
                assertWireError(validationHandler(true, backend).setTaskPushNotificationConfiguration("task",
                        pushConfigBody(fields, name), callContext), 500, "InternalError", "delegated");
                ArgumentCaptor<TaskPushNotificationConfig> config = ArgumentCaptor.forClass(TaskPushNotificationConfig.class);
                verify(backend).onCreateTaskPushNotificationConfig(config.capture(), any());
                assertEquals("task", config.getValue().taskId());
                assertEquals("task", config.getValue().id());
            }
        }
    }

    @Test
    public void testSetPushConfigRejectsConflictingAndMalformedResourceNames() {
        for (String body : new String[] {
                pushConfigBody("\"parent\":\"tasks/other\",", "tasks/task/pushNotificationConfigs/"),
                pushConfigBody("\"parent\":\"tasks/task/extra\",", "tasks/task/pushNotificationConfigs/"),
                pushConfigBody("", "tasks/other/pushNotificationConfigs/chosen"),
                pushConfigBody("", "tasks/task/pushNotificationConfigs/chosen/extra"),
                pushConfigBody("", "tasks/task/notPushNotificationConfigs/chosen"),
                pushConfigBody("\"configId\":\"other\",", "tasks/task/pushNotificationConfigs/chosen")}) {
            RequestHandler backend = mock(RequestHandler.class);
            RestHandler_v0_3.HTTPRestResponse response = validationHandler(true, backend)
                    .setTaskPushNotificationConfiguration("task", body, callContext);
            assertEquals(422, response.getStatusCode(), body);
            assertTrue(response.getBody().contains("InvalidParamsError"), response.getBody());
            verifyNoInteractions(backend);
        }
    }

    @Test
    public void testSetPushConfigCapabilityPreflightPrecedesBodyValidation() {
        for (String body : new String[] {"{broken", pushConfigBody("\"parent\":\"tasks/other\",", "bad/name")}) {
            RequestHandler backend = mock(RequestHandler.class);
            RestHandler_v0_3.HTTPRestResponse response = validationHandler(false, backend)
                    .setTaskPushNotificationConfiguration("task", body, callContext);
            assertEquals(501, response.getStatusCode());
            assertTrue(response.getBody().contains("PushNotificationNotSupportedError"), response.getBody());
            verifyNoInteractions(backend);
        }
    }

    private String pushConfigBody(String fields, String name) {
        return "{" + fields + "\"config\":{\"name\":\"" + name
                + "\",\"pushNotificationConfig\":{\"url\":\"https://example.com/callback\"}}}";
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
            }""".formatted(MINIMAL_TASK.id(), MINIMAL_TASK.id());
        RestHandler_v0_3.HTTPRestResponse response = handler.setTaskPushNotificationConfiguration(MINIMAL_TASK.id(), createRequestBody, callContext);
        assertEquals(201, response.getStatusCode(), response.toString());
        assertEquals("application/json", response.getContentType());

        // Now get it (using taskId as configId since that's the default)
        response = handler.getTaskPushNotificationConfiguration(MINIMAL_TASK.id(), MINIMAL_TASK.id(), callContext);
        assertEquals(200, response.getStatusCode(), response.toString());
        assertEquals("application/json", response.getContentType());
    }

    @Test
    public void testDeletePushNotificationConfig() {
        RestHandler_v0_3 handler = new RestHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

        // Save task to v1.0 backend
        taskStore.save(TaskMapper_v0_3.INSTANCE.toV10(MINIMAL_TASK), false);

        RestHandler_v0_3.HTTPRestResponse response = handler.deleteTaskPushNotificationConfiguration(MINIMAL_TASK.id(), MINIMAL_TASK.id(), callContext);
        assertEquals(204, response.getStatusCode());
    }

    @Test
    public void testListPushNotificationConfigs() {
        RestHandler_v0_3 handler = new RestHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

        // Save task to v1.0 backend
        taskStore.save(TaskMapper_v0_3.INSTANCE.toV10(MINIMAL_TASK), false);

        RestHandler_v0_3.HTTPRestResponse response = handler.listTaskPushNotificationConfigurations(MINIMAL_TASK.id(), callContext);

        assertEquals(200, response.getStatusCode());
        assertEquals("application/json", response.getContentType());
    }

    @Test
    public void testPushNotificationConfigParseError_DoesNotLogSensitiveData() {
        RestHandler_v0_3 handler = new RestHandler_v0_3(CARD, internalExecutor, convert03To10Handler);
        taskStore.save(TaskMapper_v0_3.INSTANCE.toV10(MINIMAL_TASK), false);

        String requestBody = """
            {
              "name": "tasks/%s/pushNotificationConfigs/",
              "pushNotificationConfig": {
                "url": "https://webhook.example.com",
                "token": "secret-token-12345",
                "authentication": {
                  "scheme": "Bearer",
                  "credentials": "oauth-secret-67890"
                },
                "unknownField": "trigger-parse-error"
              }
            }""".formatted(MINIMAL_TASK.id());

        // Request body with sensitive data and an unknown field to trigger parse error
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(RestHandler_v0_3.class.getName());
        LogCaptureAssertions.assertSensitiveDataNotLogged(logger,
                () -> assertEquals(422,
                        handler.setTaskPushNotificationConfiguration(MINIMAL_TASK.id(), requestBody, callContext)
                                .getStatusCode()),
                "secret-token-12345", "oauth-secret-67890");
    }

    @Test
    public void testSharedErrorConversionPreservesWireMessage() {
        RestHandler_v0_3 handler = new RestHandler_v0_3(CARD, internalExecutor, convert03To10Handler);
        RestHandler_v0_3.HTTPRestResponse response = handler.getTask("missing-conversion-task", null, null, callContext);
        assertEquals(404, response.getStatusCode());
        assertEquals("application/json", response.getContentType());
        assertEquals(Map.of(), response.getHeaders());
        assertEquals("{\"error\": \"org.a2aproject.sdk.compat03.spec.TaskNotFoundError_v0_3\", "
                + "\"message\": \"Task not found\"}", response.getBody());
        response = handler.getTask("task", "-1", null, callContext);
        assertEquals(422, response.getStatusCode());
        assertEquals("application/json", response.getContentType());
        assertEquals("{\"error\": \"org.a2aproject.sdk.compat03.spec.InvalidParamsError_v0_3\", "
                + "\"message\": \"bad history_length or historyLength\"}", response.getBody());
    }

    private RestHandler_v0_3 validationHandler(boolean capabilities, RequestHandler backend) {
        AgentCard_v0_3 card = new AgentCard_v0_3.Builder(CARD)
                .capabilities(new AgentCapabilities_v0_3(capabilities, capabilities, false, null)).build();
        return new RestHandler_v0_3(card, internalExecutor, new Convert_v0_3_To10RequestHandler(backend));
    }

    private void assertWireError(RestHandler_v0_3.HTTPRestResponse response, int status, String type, String message) {
        assertEquals(RestHandler_v0_3.HTTPRestResponse.class, response.getClass());
        assertEquals(status, response.getStatusCode());
        assertEquals("application/json", response.getContentType());
        assertEquals(Map.of(), response.getHeaders());
        assertEquals("{\"error\": \"org.a2aproject.sdk.compat03.spec." + type
                + "_v0_3\", \"message\": \"" + message + "\"}", response.getBody());
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testGetTaskRawAliasValidationOrder(String taskId) {
        RequestHandler backend = mock(RequestHandler.class);
        RestHandler_v0_3 handler = validationHandler(true, backend);
        assertWireError(handler.getTask(taskId, "bad", "bad", callContext), 422, "InvalidParamsError", "bad task id");
        assertWireError(handler.getTask(taskId, "bad", null, callContext), 422, "InvalidParamsError", "bad task id");
        assertWireError(handler.getTask(taskId, null, "bad", callContext), 422, "InvalidParamsError", "bad task id");
        verifyNoInteractions(backend);
    }

    @Test
    public void testGetTaskRawAliases() {
        RequestHandler backend = mock(RequestHandler.class);
        RestHandler_v0_3 handler = validationHandler(true, backend);
        for (String snake : new String[] {"", "bad", "2"}) {
            assertWireError(handler.getTask("task", snake, "bad", callContext), 422, "InvalidParamsError",
                    "Only one of 'history_length' or 'historyLength' may be specified");
        }
        for (String value : new String[] {"", "bad", "2147483648", "-1"}) {
            assertWireError(handler.getTask("task", value, null, callContext), 422, "InvalidParamsError",
                    "bad history_length or historyLength");
            assertWireError(handler.getTask("task", null, value, callContext), 422, "InvalidParamsError",
                    "bad history_length or historyLength");
        }
        verifyNoInteractions(backend);
        for (String[] aliases : new String[][] {{null, null}, {"2", null}, {null, "2"}}) {
            RequestHandler successfulBackend = mock(RequestHandler.class);
            RestHandler_v0_3 successfulHandler = validationHandler(true, successfulBackend);
            when(successfulBackend.onGetTask(any(), any())).thenReturn(TaskMapper_v0_3.INSTANCE.toV10(MINIMAL_TASK));
            RestHandler_v0_3.HTTPRestResponse response = successfulHandler.getTask("task", aliases[0], aliases[1], callContext);
            assertEquals(200, response.getStatusCode());
            assertEquals("application/json", response.getContentType());
            assertTrue(response.getBody().contains(MINIMAL_TASK.id()));
            ArgumentCaptor<TaskQueryParams> query = ArgumentCaptor.forClass(TaskQueryParams.class);
            verify(successfulBackend).onGetTask(query.capture(), any());
            assertEquals(aliases[0] == null && aliases[1] == null ? null : Integer.valueOf(2), query.getValue().historyLength());
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testCancelTaskPathValidationOrder(String taskId) {
        RequestHandler backend = mock(RequestHandler.class);
        assertWireError(validationHandler(true, backend).cancelTask(taskId, callContext),
                422, "InvalidParamsError", "bad task id");
        verifyNoInteractions(backend);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testResubscribeTaskPathValidationOrder(String taskId) {
        RequestHandler backend = mock(RequestHandler.class);
        for (boolean capabilities : new boolean[] {true, false}) {
            assertWireError(validationHandler(capabilities, backend).resubscribeTask(taskId, callContext),
                    422, "InvalidParamsError", "bad task id");
        }
        verifyNoInteractions(backend);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testSetPushConfigPathValidationOrder(String taskId) {
        RequestHandler backend = mock(RequestHandler.class);
        for (boolean capabilities : new boolean[] {true, false}) {
            assertWireError(validationHandler(capabilities, backend).setTaskPushNotificationConfiguration(taskId, "{broken", callContext),
                    422, "InvalidParamsError", "bad task id");
        }
        verifyNoInteractions(backend);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testGetPushConfigPathValidationOrder(String taskId) {
        RequestHandler backend = mock(RequestHandler.class);
        for (boolean capabilities : new boolean[] {true, false}) {
            assertWireError(validationHandler(capabilities, backend).getTaskPushNotificationConfiguration(taskId, null, callContext),
                    422, "InvalidParamsError", "bad task id");
        }
        verifyNoInteractions(backend);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testGetPushConfigPermitsMissingConfigId(String configId) {
        RequestHandler backend = mock(RequestHandler.class);
        when(backend.onGetTaskPushNotificationConfig(any(), any())).thenThrow(new InternalError("delegated"));
        assertWireError(validationHandler(true, backend).getTaskPushNotificationConfiguration("task", configId, callContext),
                500, "InternalError", "delegated");
        ArgumentCaptor<GetTaskPushNotificationConfigParams> query = ArgumentCaptor.forClass(GetTaskPushNotificationConfigParams.class);
        verify(backend).onGetTaskPushNotificationConfig(query.capture(), any());
        assertEquals(configId == null ? "task" : "", query.getValue().id());
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testListPushConfigsPathValidationOrder(String taskId) {
        RequestHandler backend = mock(RequestHandler.class);
        for (boolean capabilities : new boolean[] {true, false}) {
            assertWireError(validationHandler(capabilities, backend).listTaskPushNotificationConfigurations(taskId, callContext),
                    422, "InvalidParamsError", "bad task id");
        }
        verifyNoInteractions(backend);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testDeletePushConfigPathValidationOrder(String id) {
        RequestHandler backend = mock(RequestHandler.class);
        for (boolean capabilities : new boolean[] {true, false}) {
            RestHandler_v0_3 handler = validationHandler(capabilities, backend);
            assertWireError(handler.deleteTaskPushNotificationConfiguration(id, id, callContext),
                    422, "InvalidParamsError", "bad task id");
            assertWireError(handler.deleteTaskPushNotificationConfiguration("task", id, callContext),
                    422, "InvalidParamsError", "bad config id");
        }
        verifyNoInteractions(backend);
    }

}
