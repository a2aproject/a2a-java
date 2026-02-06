package io.a2a.client.transport.jsonrpc;

import static io.a2a.client.transport.jsonrpc.JsonMessages.CANCEL_TASK_TEST_REQUEST;
import static io.a2a.client.transport.jsonrpc.JsonMessages.CANCEL_TASK_TEST_RESPONSE;
import static io.a2a.client.transport.jsonrpc.JsonMessages.GET_AUTHENTICATED_EXTENDED_AGENT_CARD_REQUEST;
import static io.a2a.client.transport.jsonrpc.JsonMessages.GET_AUTHENTICATED_EXTENDED_AGENT_CARD_RESPONSE;
import static io.a2a.client.transport.jsonrpc.JsonMessages.GET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_REQUEST;
import static io.a2a.client.transport.jsonrpc.JsonMessages.GET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE;
import static io.a2a.client.transport.jsonrpc.JsonMessages.GET_TASK_TEST_REQUEST;
import static io.a2a.client.transport.jsonrpc.JsonMessages.GET_TASK_TEST_RESPONSE;
import static io.a2a.client.transport.jsonrpc.JsonMessages.SEND_MESSAGE_ERROR_TEST_RESPONSE;
import static io.a2a.client.transport.jsonrpc.JsonMessages.SEND_MESSAGE_TEST_REQUEST;
import static io.a2a.client.transport.jsonrpc.JsonMessages.SEND_MESSAGE_TEST_REQUEST_WITH_MESSAGE_RESPONSE;
import static io.a2a.client.transport.jsonrpc.JsonMessages.SEND_MESSAGE_TEST_RESPONSE;
import static io.a2a.client.transport.jsonrpc.JsonMessages.SEND_MESSAGE_TEST_RESPONSE_WITH_MESSAGE_RESPONSE;
import static io.a2a.client.transport.jsonrpc.JsonMessages.SEND_MESSAGE_WITH_DATA_PART_TEST_REQUEST;
import static io.a2a.client.transport.jsonrpc.JsonMessages.SEND_MESSAGE_WITH_DATA_PART_TEST_RESPONSE;
import static io.a2a.client.transport.jsonrpc.JsonMessages.SEND_MESSAGE_WITH_ERROR_TEST_REQUEST;
import static io.a2a.client.transport.jsonrpc.JsonMessages.SEND_MESSAGE_WITH_FILE_PART_TEST_REQUEST;
import static io.a2a.client.transport.jsonrpc.JsonMessages.SEND_MESSAGE_WITH_FILE_PART_TEST_RESPONSE;
import static io.a2a.client.transport.jsonrpc.JsonMessages.SEND_MESSAGE_WITH_MIXED_PARTS_TEST_REQUEST;
import static io.a2a.client.transport.jsonrpc.JsonMessages.SEND_MESSAGE_WITH_MIXED_PARTS_TEST_RESPONSE;
import static io.a2a.client.transport.jsonrpc.JsonMessages.SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_REQUEST;
import static io.a2a.client.transport.jsonrpc.JsonMessages.SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE;
import static io.a2a.spec.AgentInterface.CURRENT_PROTOCOL_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.ExtensionSupportRequiredError;
import io.a2a.spec.VersionNotSupportedError;
import io.a2a.spec.AgentSkill;
import io.a2a.spec.Artifact;
import io.a2a.spec.AuthenticationInfo;
import io.a2a.spec.DataPart;
import io.a2a.spec.EventKind;
import io.a2a.spec.FileContent;
import io.a2a.spec.FilePart;
import io.a2a.spec.FileWithBytes;
import io.a2a.spec.FileWithUri;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendConfiguration;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.OpenIdConnectSecurityScheme;
import io.a2a.spec.Part;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.SecurityScheme;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import io.a2a.spec.TaskState;
import io.a2a.spec.TextPart;
import io.a2a.util.Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.MatchType;
import org.mockserver.model.JsonBody;

public class JSONRPCTransportTest {

    private ClientAndServer server;

    @BeforeEach
    public void setUp() {
        server = new ClientAndServer(4001);
    }

    @AfterEach
    public void tearDown() {
        server.stop();
    }

    @Test
    public void testA2AClientSendMessage() throws Exception {
        this.server.when(
                        request()
                                .withMethod("POST")
                                .withPath("/")
                                .withBody(JsonBody.json(SEND_MESSAGE_TEST_REQUEST, MatchType.ONLY_MATCHING_FIELDS))

                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(SEND_MESSAGE_TEST_RESPONSE)
                );

        JSONRPCTransport client = new JSONRPCTransport("http://localhost:4001");
        Message message = Message.builder()
                .role(Message.Role.USER)
                .parts(Collections.singletonList(new TextPart("tell me a joke")))
                .contextId("context-1234")
                .messageId("message-1234")
                .build();
        MessageSendConfiguration configuration = MessageSendConfiguration.builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(true)
                .build();
        MessageSendParams params = MessageSendParams.builder()
                .message(message)
                .configuration(configuration)
                .build();

        EventKind result = client.sendMessage(params, null);
        assertInstanceOf(Task.class, result);
        Task task = (Task) result;
        assertEquals("de38c76d-d54c-436c-8b9f-4c2703648d64", task.id());
        assertNotNull(task.contextId());
        assertEquals(TaskState.COMPLETED,task.status().state());
        assertEquals(1, task.artifacts().size());
        Artifact artifact = task.artifacts().get(0);
        assertEquals("artifact-1", artifact.artifactId());
        assertEquals("joke", artifact.name());
        assertEquals(1, artifact.parts().size());
        Part<?> part = artifact.parts().get(0);
        assertTrue(part instanceof TextPart);
        assertEquals("Why did the chicken cross the road? To get to the other side!", ((TextPart) part).text());
        assertTrue(task.metadata().isEmpty());
    }

    @Test
    public void testA2AClientSendMessageWithMessageResponse() throws Exception {
        this.server.when(
                        request()
                                .withMethod("POST")
                                .withPath("/")
                                .withBody(JsonBody.json(SEND_MESSAGE_TEST_REQUEST_WITH_MESSAGE_RESPONSE, MatchType.ONLY_MATCHING_FIELDS))

                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(SEND_MESSAGE_TEST_RESPONSE_WITH_MESSAGE_RESPONSE)
                );

        JSONRPCTransport client = new JSONRPCTransport("http://localhost:4001");
        Message message = Message.builder()
                .role(Message.Role.USER)
                .parts(Collections.singletonList(new TextPart("tell me a joke")))
                .contextId("context-1234")
                .messageId("message-1234")
                .build();
        MessageSendConfiguration configuration = MessageSendConfiguration.builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(true)
                .build();
        MessageSendParams params = MessageSendParams.builder()
                .message(message)
                .configuration(configuration)
                .build();

        EventKind result = client.sendMessage(params, null);
        assertInstanceOf(Message.class, result);
        Message agentMessage = (Message) result;
        assertEquals(Message.Role.AGENT, agentMessage.role());
        Part<?> part = agentMessage.parts().get(0);
        assertTrue(part instanceof TextPart);
        assertEquals("Why did the chicken cross the road? To get to the other side!", ((TextPart) part).text());
        assertEquals("msg-456", agentMessage.messageId());
    }


    @Test
    public void testA2AClientSendMessageWithError() throws Exception {
        this.server.when(
                        request()
                                .withMethod("POST")
                                .withPath("/")
                                .withBody(JsonBody.json(SEND_MESSAGE_WITH_ERROR_TEST_REQUEST, MatchType.ONLY_MATCHING_FIELDS))

                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(SEND_MESSAGE_ERROR_TEST_RESPONSE)
                );

        JSONRPCTransport client = new JSONRPCTransport("http://localhost:4001");
        Message message = Message.builder()
                .role(Message.Role.USER)
                .parts(Collections.singletonList(new TextPart("tell me a joke")))
                .contextId("context-1234")
                .messageId("message-1234")
                .build();
        MessageSendConfiguration configuration = MessageSendConfiguration.builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(true)
                .build();
        MessageSendParams params = MessageSendParams.builder()
                .message(message)
                .configuration(configuration)
                .build();

        try {
            client.sendMessage(params, null);
            fail(); // should not reach here
        } catch (A2AClientException e) {
            assertTrue(e.getMessage().contains("Invalid parameters: \"Hello world\""),e.getMessage());
        }
    }

    @Test
    public void testA2AClientGetTask() throws Exception {
        this.server.when(
                        request()
                                .withMethod("POST")
                                .withPath("/")
                                .withBody(JsonBody.json(GET_TASK_TEST_REQUEST, MatchType.ONLY_MATCHING_FIELDS))

                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(GET_TASK_TEST_RESPONSE)
                );

        JSONRPCTransport client = new JSONRPCTransport("http://localhost:4001");
        Task task = client.getTask(new TaskQueryParams("de38c76d-d54c-436c-8b9f-4c2703648d64",
                10), null);
        assertEquals("de38c76d-d54c-436c-8b9f-4c2703648d64", task.id());
        assertEquals("c295ea44-7543-4f78-b524-7a38915ad6e4", task.contextId());
        assertEquals(TaskState.COMPLETED, task.status().state());
        assertEquals(1, task.artifacts().size());
        Artifact artifact = task.artifacts().get(0);
        assertEquals(1, artifact.parts().size());
        assertEquals("artifact-1", artifact.artifactId());
        Part<?> part = artifact.parts().get(0);
        assertTrue(part instanceof TextPart);
        assertEquals("Why did the chicken cross the road? To get to the other side!", ((TextPart) part).text());
        assertTrue(task.metadata().isEmpty());
        List<Message> history = task.history();
        assertNotNull(history);
        assertEquals(1, history.size());
        Message message = history.get(0);
        assertEquals(Message.Role.USER, message.role());
        List<Part<?>> parts = message.parts();
        assertNotNull(parts);
        assertEquals(3, parts.size());
        part = parts.get(0);
        assertTrue(part instanceof TextPart);
        assertEquals("tell me a joke", ((TextPart)part).text());
        part = parts.get(1);
        assertTrue(part instanceof FilePart);
        FileContent filePart = ((FilePart) part).file();
        assertEquals("file:///path/to/file.txt", ((FileWithUri) filePart).uri());
        assertEquals("text/plain", filePart.mimeType());
        part = parts.get(2);
        assertTrue(part instanceof FilePart);
        filePart = ((FilePart) part).file();
        assertEquals("aGVsbG8=", ((FileWithBytes) filePart).bytes());
        assertEquals("hello.txt", filePart.name());
        assertTrue(task.metadata().isEmpty());
    }

    @Test
    public void testA2AClientCancelTask() throws Exception {
        this.server.when(
                        request()
                                .withMethod("POST")
                                .withPath("/")
                                .withBody(JsonBody.json(CANCEL_TASK_TEST_REQUEST, MatchType.ONLY_MATCHING_FIELDS))

                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(CANCEL_TASK_TEST_RESPONSE)
                );

        JSONRPCTransport client = new JSONRPCTransport("http://localhost:4001");
        Task task = client.cancelTask(new TaskIdParams("de38c76d-d54c-436c-8b9f-4c2703648d64"), null);
        assertEquals("de38c76d-d54c-436c-8b9f-4c2703648d64", task.id());
        assertEquals("c295ea44-7543-4f78-b524-7a38915ad6e4", task.contextId());
        assertEquals(TaskState.CANCELED, task.status().state());
        assertTrue(task.metadata().isEmpty());
    }

    @Test
    public void testA2AClientGetTaskPushNotificationConfig() throws Exception {
        this.server.when(
                        request()
                                .withMethod("POST")
                                .withPath("/")
                                .withBody(JsonBody.json(GET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_REQUEST, MatchType.ONLY_MATCHING_FIELDS))

                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(GET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE)
                );

        JSONRPCTransport client = new JSONRPCTransport("http://localhost:4001");
        TaskPushNotificationConfig taskPushNotificationConfig = client.getTaskPushNotificationConfiguration(
                new GetTaskPushNotificationConfigParams("de38c76d-d54c-436c-8b9f-4c2703648d64", "c295ea44-7543-4f78-b524-7a38915ad6e4"), null);
        PushNotificationConfig pushNotificationConfig = taskPushNotificationConfig.pushNotificationConfig();
        assertNotNull(pushNotificationConfig);
        assertEquals("https://example.com/callback", pushNotificationConfig.url());
        AuthenticationInfo authenticationInfo = pushNotificationConfig.authentication();
        assertEquals("jwt", authenticationInfo.scheme());
    }

    @Test
    public void testA2AClientCreateTaskPushNotificationConfig() throws Exception {
        this.server.when(
                        request()
                                .withMethod("POST")
                                .withPath("/")
                                .withBody(JsonBody.json(SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_REQUEST, MatchType.ONLY_MATCHING_FIELDS))

                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE)
                );

        JSONRPCTransport client = new JSONRPCTransport("http://localhost:4001");
        TaskPushNotificationConfig taskPushNotificationConfig = client.createTaskPushNotificationConfiguration(
                new TaskPushNotificationConfig("de38c76d-d54c-436c-8b9f-4c2703648d64",
                        PushNotificationConfig.builder()
                                .id("c295ea44-7543-4f78-b524-7a38915ad6e4")
                                .url("https://example.com/callback")
                                .authentication(new AuthenticationInfo("jwt", null))
                                .build(), ""), null);
        PushNotificationConfig pushNotificationConfig = taskPushNotificationConfig.pushNotificationConfig();
        assertNotNull(pushNotificationConfig);
        assertEquals("https://example.com/callback", pushNotificationConfig.url());
        AuthenticationInfo authenticationInfo = pushNotificationConfig.authentication();
        assertEquals("jwt", authenticationInfo.scheme());
    }

    @Test
    public void testA2AClientGetExtendedAgentCard() throws Exception {
        this.server.when(
                        request()
                                .withMethod("POST")
                                .withPath("/")
                                .withBody(JsonBody.json(GET_AUTHENTICATED_EXTENDED_AGENT_CARD_REQUEST, MatchType.ONLY_MATCHING_FIELDS))
                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(GET_AUTHENTICATED_EXTENDED_AGENT_CARD_RESPONSE)
                );

        JSONRPCTransport client = new JSONRPCTransport("http://localhost:4001");
        AgentCard agentCard = client.getExtendedAgentCard(null);
        assertEquals("GeoSpatial Route Planner Agent Extended", agentCard.name());
        assertEquals("Extended description", agentCard.description());
        assertEquals("https://georoute-agent.example.com/a2a/v1", Utils.getFavoriteInterface(agentCard).url());
        assertEquals("Example Geo Services Inc.", agentCard.provider().organization());
        assertEquals("https://www.examplegeoservices.com", agentCard.provider().url());
        assertEquals("1.2.0", agentCard.version());
        assertEquals("https://docs.examplegeoservices.com/georoute-agent/api", agentCard.documentationUrl());
        assertTrue(agentCard.capabilities().streaming());
        assertTrue(agentCard.capabilities().pushNotifications());
        assertTrue(agentCard.capabilities().extendedAgentCard());
        Map<String, SecurityScheme> securitySchemes = agentCard.securitySchemes();
        assertNotNull(securitySchemes);
        OpenIdConnectSecurityScheme google = (OpenIdConnectSecurityScheme) securitySchemes.get("google");
        assertEquals("https://accounts.google.com/.well-known/openid-configuration", google.openIdConnectUrl());
        List<Map<String, List<String>>> security = agentCard.securityRequirements();
        assertEquals(1, security.size());
        Map<String, List<String>> securityMap = security.get(0);
        List<String> scopes = securityMap.get("google");
        List<String> expectedScopes = List.of("openid", "profile", "email");
        assertEquals(expectedScopes, scopes);
        List<String> defaultInputModes = List.of("application/json", "text/plain");
        assertEquals(defaultInputModes, agentCard.defaultInputModes());
        List<String> defaultOutputModes = List.of("application/json", "image/png");
        assertEquals(defaultOutputModes, agentCard.defaultOutputModes());
        List<AgentSkill> skills = agentCard.skills();
        assertEquals("route-optimizer-traffic", skills.get(0).id());
        assertEquals("Traffic-Aware Route Optimizer", skills.get(0).name());
        assertEquals("Calculates the optimal driving route between two or more locations, taking into account real-time traffic conditions, road closures, and user preferences (e.g., avoid tolls, prefer highways).", skills.get(0).description());
        List<String> tags = List.of("maps", "routing", "navigation", "directions", "traffic");
        assertEquals(tags, skills.get(0).tags());
        List<String> examples = List.of("Plan a route from '1600 Amphitheatre Parkway, Mountain View, CA' to 'San Francisco International Airport' avoiding tolls.",
                "{\"origin\": {\"lat\": 37.422, \"lng\": -122.084}, \"destination\": {\"lat\": 37.7749, \"lng\": -122.4194}, \"preferences\": [\"avoid_ferries\"]}");
        assertEquals(examples, skills.get(0).examples());
        assertEquals(defaultInputModes, skills.get(0).inputModes());
        List<String> outputModes = List.of("application/json", "application/vnd.geo+json", "text/html");
        assertEquals(outputModes, skills.get(0).outputModes());
        assertEquals("custom-map-generator", skills.get(1).id());
        assertEquals("Personalized Map Generator", skills.get(1).name());
        assertEquals("Creates custom map images or interactive map views based on user-defined points of interest, routes, and style preferences. Can overlay data layers.", skills.get(1).description());
        tags = List.of("maps", "customization", "visualization", "cartography");
        assertEquals(tags, skills.get(1).tags());
        examples = List.of("Generate a map of my upcoming road trip with all planned stops highlighted.",
                "Show me a map visualizing all coffee shops within a 1-mile radius of my current location.");
        assertEquals(examples, skills.get(1).examples());
        List<String> inputModes = List.of("application/json");
        assertEquals(inputModes, skills.get(1).inputModes());
        outputModes = List.of("image/png", "image/jpeg", "application/json", "text/html");
        assertEquals(outputModes, skills.get(1).outputModes());
        assertEquals("skill-extended", skills.get(2).id());
        assertEquals("Extended Skill", skills.get(2).name());
        assertEquals("This is an extended skill.", skills.get(2).description());
        assertEquals(List.of("extended"), skills.get(2).tags());
        assertEquals("https://georoute-agent.example.com/icon.png", agentCard.iconUrl());
        assertEquals(CURRENT_PROTOCOL_VERSION, agentCard.supportedInterfaces().get(0).protocolVersion());
    }

    @Test
    public void testA2AClientSendMessageWithFilePart() throws Exception {
        this.server.when(
                        request()
                                .withMethod("POST")
                                .withPath("/")
                                .withBody(JsonBody.json(SEND_MESSAGE_WITH_FILE_PART_TEST_REQUEST, MatchType.ONLY_MATCHING_FIELDS))

                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(SEND_MESSAGE_WITH_FILE_PART_TEST_RESPONSE)
                );

        JSONRPCTransport client = new JSONRPCTransport("http://localhost:4001");
        Message message = Message.builder()
                .role(Message.Role.USER)
                .parts(List.of(
                        new TextPart("analyze this image"),
                        new FilePart(new FileWithUri("image/jpeg", null, "file:///path/to/image.jpg"))
                ))
                .contextId("context-1234")
                .messageId("message-1234-with-file")
                .build();
        MessageSendConfiguration configuration = MessageSendConfiguration.builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(true)
                .build();
        MessageSendParams params = MessageSendParams.builder()
                .message(message)
                .configuration(configuration)
                .build();

        EventKind result = client.sendMessage(params, null);
        assertInstanceOf(Task.class, result);
        Task task = (Task) result;
        assertEquals("de38c76d-d54c-436c-8b9f-4c2703648d64", task.id());
        assertNotNull(task.contextId());
        assertEquals(TaskState.COMPLETED, task.status().state());
        assertEquals(1, task.artifacts().size());
        Artifact artifact = task.artifacts().get(0);
        assertEquals("artifact-1", artifact.artifactId());
        assertEquals("image-analysis", artifact.name());
        assertEquals(1, artifact.parts().size());
        Part<?> part = artifact.parts().get(0);
        assertTrue(part instanceof TextPart);
        assertEquals("This is an image of a cat sitting on a windowsill.", ((TextPart) part).text());
        assertFalse(task.metadata().isEmpty());
        assertEquals(1, task.metadata().size());
        assertEquals("metadata-test", task.metadata().get("test"));
    }

    @Test
    public void testA2AClientSendMessageWithDataPart() throws Exception {
        this.server.when(
                        request()
                                .withMethod("POST")
                                .withPath("/")
                                .withBody(JsonBody.json(SEND_MESSAGE_WITH_DATA_PART_TEST_REQUEST, MatchType.ONLY_MATCHING_FIELDS))

                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(SEND_MESSAGE_WITH_DATA_PART_TEST_RESPONSE)
                );

        JSONRPCTransport client = new JSONRPCTransport("http://localhost:4001");

        Map<String, Object> data = new HashMap<>();
        data.put("temperature", 25.5);
        data.put("humidity", 60.2);
        data.put("location", "San Francisco");
        data.put("timestamp", "2024-01-15T10:30:00Z");

        Message message = Message.builder()
                .role(Message.Role.USER)
                .parts(List.of(
                        new TextPart("process this data"),
                        new DataPart(data)
                ))
                .contextId("context-1234")
                .messageId("message-1234-with-data")
                .build();
        MessageSendConfiguration configuration = MessageSendConfiguration.builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(true)
                .build();
        MessageSendParams params = MessageSendParams.builder()
                .message(message)
                .configuration(configuration)
                .build();

        EventKind result = client.sendMessage(params, null);
        assertInstanceOf(Task.class, result);
        Task task = (Task) result;
        assertEquals("de38c76d-d54c-436c-8b9f-4c2703648d64", task.id());
        assertNotNull(task.contextId());
        assertEquals(TaskState.COMPLETED, task.status().state());
        assertEquals(1, task.artifacts().size());
        Artifact artifact = task.artifacts().get(0);
        assertEquals("artifact-1", artifact.artifactId());
        assertEquals("data-analysis", artifact.name());
        assertEquals(1, artifact.parts().size());
        Part<?> part = artifact.parts().get(0);
        assertTrue(part instanceof TextPart);
        assertEquals("Processed weather data: Temperature is 25.5°C, humidity is 60.2% in San Francisco.", ((TextPart) part).text());
        assertTrue(task.metadata().isEmpty());
    }

    @Test
    public void testA2AClientSendMessageWithMixedParts() throws Exception {
        this.server.when(
                        request()
                                .withMethod("POST")
                                .withPath("/")
                                .withBody(JsonBody.json(SEND_MESSAGE_WITH_MIXED_PARTS_TEST_REQUEST, MatchType.ONLY_MATCHING_FIELDS))

                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(SEND_MESSAGE_WITH_MIXED_PARTS_TEST_RESPONSE)
                );

        JSONRPCTransport client = new JSONRPCTransport("http://localhost:4001");

        Map<String, Object> data = new HashMap<>();
        data.put("chartType", "bar");
        data.put("dataPoints", List.of(10, 20, 30, 40));
        data.put("labels", List.of("Q1", "Q2", "Q3", "Q4"));

        Message message = Message.builder()
                .role(Message.Role.USER)
                .parts(List.of(
                        new TextPart("analyze this data and image"),
                        new FilePart(new FileWithBytes("image/png", "chart.png", "aGVsbG8=")),
                        new DataPart(data)
                ))
                .contextId("context-1234")
                .messageId("message-1234-with-mixed")
                .build();
        MessageSendConfiguration configuration = MessageSendConfiguration.builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(true)
                .build();
        MessageSendParams params = MessageSendParams.builder()
                .message(message)
                .configuration(configuration)
                .build();

        EventKind result = client.sendMessage(params, null);
        assertInstanceOf(Task.class, result);
        Task task = (Task) result;
        assertEquals("de38c76d-d54c-436c-8b9f-4c2703648d64", task.id());
        assertNotNull(task.contextId());
        assertEquals(TaskState.COMPLETED, task.status().state());
        assertEquals(1, task.artifacts().size());
        Artifact artifact = task.artifacts().get(0);
        assertEquals("artifact-1", artifact.artifactId());
        assertEquals("mixed-analysis", artifact.name());
        assertEquals(1, artifact.parts().size());
        Part<?> part = artifact.parts().get(0);
        assertTrue(part instanceof TextPart);
        assertEquals("Analyzed chart image and data: Bar chart showing quarterly data with values [10, 20, 30, 40].", ((TextPart) part).text());
        assertTrue(task.metadata().isEmpty());
    }

    /**
     * Test that ExtensionSupportRequiredError is properly unmarshalled from JSON-RPC error response.
     */
    @Test
    public void testExtensionSupportRequiredErrorUnmarshalling() throws Exception {
        // Mock server returns JSON-RPC error with code -32008 (EXTENSION_SUPPORT_REQUIRED_ERROR)
        String errorResponseBody = """
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "error": {
                        "code": -32008,
                        "message": "Extension required: https://example.com/test-extension"
                    }
                }
                """;

        this.server.when(
                        request()
                                .withMethod("POST")
                                .withPath("/")
                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(errorResponseBody)
                );

        JSONRPCTransport client = new JSONRPCTransport("http://localhost:4001");
        Message message = Message.builder()
                .role(Message.Role.USER)
                .parts(Collections.singletonList(new TextPart("test message")))
                .contextId("context-test")
                .messageId("message-test")
                .build();
        MessageSendConfiguration configuration = MessageSendConfiguration.builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(true)
                .build();
        MessageSendParams params = MessageSendParams.builder()
                .message(message)
                .configuration(configuration)
                .build();

        // Should throw A2AClientException with ExtensionSupportRequiredError as cause
        try {
            client.sendMessage(params, null);
            fail("Expected A2AClientException to be thrown");
        } catch (A2AClientException e) {
            // Verify the cause is ExtensionSupportRequiredError
            assertInstanceOf(ExtensionSupportRequiredError.class, e.getCause());
            ExtensionSupportRequiredError extensionError = (ExtensionSupportRequiredError) e.getCause();
            assertTrue(extensionError.getMessage().contains("https://example.com/test-extension"));
        }
    }

    /**
     * Test that VersionNotSupportedError is properly unmarshalled from JSON-RPC error response.
     */
    @Test
    public void testVersionNotSupportedErrorUnmarshalling() throws Exception {
        // Mock server returns JSON-RPC error with code -32009 (VERSION_NOT_SUPPORTED_ERROR)
        String errorResponseBody = """
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "error": {
                        "code": -32009,
                        "message": "Protocol version 2.0 is not supported. This agent supports version 1.0"
                    }
                }
                """;

        this.server.when(
                        request()
                                .withMethod("POST")
                                .withPath("/")
                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(errorResponseBody)
                );

        JSONRPCTransport client = new JSONRPCTransport("http://localhost:4001");
        Message message = Message.builder()
                .role(Message.Role.USER)
                .parts(Collections.singletonList(new TextPart("test message")))
                .contextId("context-test")
                .messageId("message-test")
                .build();
        MessageSendConfiguration configuration = MessageSendConfiguration.builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(true)
                .build();
        MessageSendParams params = MessageSendParams.builder()
                .message(message)
                .configuration(configuration)
                .build();

        // Should throw A2AClientException with VersionNotSupportedError as cause
        try {
            client.sendMessage(params, null);
            fail("Expected A2AClientException to be thrown");
        } catch (A2AClientException e) {
            // Verify the cause is VersionNotSupportedError
            assertInstanceOf(VersionNotSupportedError.class, e.getCause());
            VersionNotSupportedError versionError = (VersionNotSupportedError) e.getCause();
            assertTrue(versionError.getMessage().contains("2.0"));
            assertTrue(versionError.getMessage().contains("1.0"));
        }
    }
}