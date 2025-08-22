package io.a2a.client;

import static io.a2a.client.JsonMessages.AGENT_CARD;
import static io.a2a.client.JsonMessages.AUTHENTICATION_EXTENDED_AGENT_CARD;
import static io.a2a.client.JsonMessages.CANCEL_TASK_TEST_REQUEST;
import static io.a2a.client.JsonMessages.CANCEL_TASK_TEST_RESPONSE;
import static io.a2a.client.JsonMessages.GET_AUTHENTICATED_EXTENDED_AGENT_CARD_REQUEST;
import static io.a2a.client.JsonMessages.GET_AUTHENTICATED_EXTENDED_AGENT_CARD_RESPONSE;
import static io.a2a.client.JsonMessages.GET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_REQUEST;
import static io.a2a.client.JsonMessages.GET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE;
import static io.a2a.client.JsonMessages.GET_TASK_TEST_REQUEST;
import static io.a2a.client.JsonMessages.GET_TASK_TEST_RESPONSE;
import static io.a2a.client.JsonMessages.SEND_MESSAGE_ERROR_TEST_RESPONSE;
import static io.a2a.client.JsonMessages.SEND_MESSAGE_TEST_REQUEST;
import static io.a2a.client.JsonMessages.SEND_MESSAGE_TEST_REQUEST_WITH_MESSAGE_RESPONSE;
import static io.a2a.client.JsonMessages.SEND_MESSAGE_TEST_RESPONSE;
import static io.a2a.client.JsonMessages.SEND_MESSAGE_TEST_RESPONSE_WITH_MESSAGE_RESPONSE;
import static io.a2a.client.JsonMessages.SEND_MESSAGE_WITH_ERROR_TEST_REQUEST;
import static io.a2a.client.JsonMessages.SEND_MESSAGE_WITH_FILE_PART_TEST_REQUEST;
import static io.a2a.client.JsonMessages.SEND_MESSAGE_WITH_FILE_PART_TEST_RESPONSE;
import static io.a2a.client.JsonMessages.SEND_MESSAGE_WITH_DATA_PART_TEST_REQUEST;
import static io.a2a.client.JsonMessages.SEND_MESSAGE_WITH_DATA_PART_TEST_RESPONSE;
import static io.a2a.client.JsonMessages.SEND_MESSAGE_WITH_MIXED_PARTS_TEST_REQUEST;
import static io.a2a.client.JsonMessages.SEND_MESSAGE_WITH_MIXED_PARTS_TEST_RESPONSE;
import static io.a2a.client.JsonMessages.SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_REQUEST;
import static io.a2a.client.JsonMessages.SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE;
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

import io.a2a.spec.A2AServerException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentCardSignature;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.AgentSkill;
import io.a2a.spec.Artifact;
import io.a2a.spec.CancelTaskResponse;
import io.a2a.spec.DataPart;
import io.a2a.spec.FileContent;
import io.a2a.spec.FilePart;
import io.a2a.spec.FileWithBytes;
import io.a2a.spec.FileWithUri;
import io.a2a.spec.GetAuthenticatedExtendedCardResponse;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.GetTaskPushNotificationConfigResponse;
import io.a2a.spec.GetTaskResponse;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendConfiguration;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.OpenIdConnectSecurityScheme;
import io.a2a.spec.Part;
import io.a2a.spec.PushNotificationAuthenticationInfo;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.SecurityScheme;
import io.a2a.spec.SendMessageResponse;
import io.a2a.spec.SetTaskPushNotificationConfigResponse;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import io.a2a.spec.TaskState;
import io.a2a.spec.TextPart;
import io.a2a.spec.TransportProtocol;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.MatchType;
import org.mockserver.model.JsonBody;

public class A2AClientTest {

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
                                .withBody(JsonBody.json(SEND_MESSAGE_TEST_REQUEST, MatchType.STRICT))

                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(SEND_MESSAGE_TEST_RESPONSE)
                );

        A2AClient client = new A2AClient("http://localhost:4001");
        Message message = new Message.Builder()
                .role(Message.Role.USER)
                .parts(Collections.singletonList(new TextPart("tell me a joke")))
                .contextId("context-1234")
                .messageId("message-1234")
                .build();
        MessageSendConfiguration configuration = new MessageSendConfiguration.Builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(true)
                .build();
        MessageSendParams params = new MessageSendParams.Builder()
                .message(message)
                .configuration(configuration)
                .build();

        SendMessageResponse response = client.sendMessage("request-1234", params);

        assertEquals("2.0", response.getJsonrpc());
        assertNotNull(response.getId());
        Object result = response.getResult();
        assertInstanceOf(Task.class, result);
        Task task = (Task) result;
        assertEquals("de38c76d-d54c-436c-8b9f-4c2703648d64", task.getId());
        assertNotNull(task.getContextId());
        assertEquals(TaskState.COMPLETED,task.getStatus().state());
        assertEquals(1, task.getArtifacts().size());
        Artifact artifact = task.getArtifacts().get(0);
        assertEquals("artifact-1", artifact.artifactId());
        assertEquals("joke", artifact.name());
        assertEquals(1, artifact.parts().size());
        Part<?> part = artifact.parts().get(0);
        assertEquals(Part.Kind.TEXT, part.getKind());
        assertEquals("Why did the chicken cross the road? To get to the other side!", ((TextPart) part).getText());
        assertTrue(task.getMetadata().isEmpty());
    }

    @Test
    public void testA2AClientSendMessageWithMessageResponse() throws Exception {
        this.server.when(
                        request()
                                .withMethod("POST")
                                .withPath("/")
                                .withBody(JsonBody.json(SEND_MESSAGE_TEST_REQUEST_WITH_MESSAGE_RESPONSE, MatchType.STRICT))

                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(SEND_MESSAGE_TEST_RESPONSE_WITH_MESSAGE_RESPONSE)
                );

        A2AClient client = new A2AClient("http://localhost:4001");
        Message message = new Message.Builder()
                .role(Message.Role.USER)
                .parts(Collections.singletonList(new TextPart("tell me a joke")))
                .contextId("context-1234")
                .messageId("message-1234")
                .build();
        MessageSendConfiguration configuration = new MessageSendConfiguration.Builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(true)
                .build();
        MessageSendParams params = new MessageSendParams.Builder()
                .message(message)
                .configuration(configuration)
                .build();

        SendMessageResponse response = client.sendMessage("request-1234-with-message-response", params);

        assertEquals("2.0", response.getJsonrpc());
        assertNotNull(response.getId());
        Object result = response.getResult();
        assertInstanceOf(Message.class, result);
        Message agentMessage = (Message) result;
        assertEquals(Message.Role.AGENT, agentMessage.getRole());
        Part<?> part = agentMessage.getParts().get(0);
        assertEquals(Part.Kind.TEXT, part.getKind());
        assertEquals("Why did the chicken cross the road? To get to the other side!", ((TextPart) part).getText());
        assertEquals("msg-456", agentMessage.getMessageId());
    }


    @Test
    public void testA2AClientSendMessageWithError() throws Exception {
        this.server.when(
                        request()
                                .withMethod("POST")
                                .withPath("/")
                                .withBody(JsonBody.json(SEND_MESSAGE_WITH_ERROR_TEST_REQUEST, MatchType.STRICT))

                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(SEND_MESSAGE_ERROR_TEST_RESPONSE)
                );

        A2AClient client = new A2AClient("http://localhost:4001");
        Message message = new Message.Builder()
                .role(Message.Role.USER)
                .parts(Collections.singletonList(new TextPart("tell me a joke")))
                .contextId("context-1234")
                .messageId("message-1234")
                .build();
        MessageSendConfiguration configuration = new MessageSendConfiguration.Builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(true)
                .build();
        MessageSendParams params = new MessageSendParams.Builder()
                .message(message)
                .configuration(configuration)
                .build();

        try {
            client.sendMessage("request-1234-with-error", params);
            fail(); // should not reach here
        } catch (A2AServerException e) {
            assertTrue(e.getMessage().contains("Invalid parameters: Hello world"));
        }
    }

    @Test
    public void testA2AClientGetTask() throws Exception {
        this.server.when(
                        request()
                                .withMethod("POST")
                                .withPath("/")
                                .withBody(JsonBody.json(GET_TASK_TEST_REQUEST, MatchType.STRICT))

                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(GET_TASK_TEST_RESPONSE)
                );

        A2AClient client = new A2AClient("http://localhost:4001");
        GetTaskResponse response = client.getTask("request-1234",
                new TaskQueryParams("de38c76d-d54c-436c-8b9f-4c2703648d64", 10));

        assertEquals("2.0", response.getJsonrpc());
        assertEquals(1, response.getId());
        Object result = response.getResult();
        assertInstanceOf(Task.class, result);
        Task task = (Task) result;
        assertEquals("de38c76d-d54c-436c-8b9f-4c2703648d64", task.getId());
        assertEquals("c295ea44-7543-4f78-b524-7a38915ad6e4", task.getContextId());
        assertEquals(TaskState.COMPLETED, task.getStatus().state());
        assertEquals(1, task.getArtifacts().size());
        Artifact artifact = task.getArtifacts().get(0);
        assertEquals(1, artifact.parts().size());
        assertEquals("artifact-1", artifact.artifactId());
        Part<?> part = artifact.parts().get(0);
        assertEquals(Part.Kind.TEXT, part.getKind());
        assertEquals("Why did the chicken cross the road? To get to the other side!", ((TextPart) part).getText());
        assertTrue(task.getMetadata().isEmpty());
        List<Message> history = task.getHistory();
        assertNotNull(history);
        assertEquals(1, history.size());
        Message message = history.get(0);
        assertEquals(Message.Role.USER, message.getRole());
        List<Part<?>> parts = message.getParts();
        assertNotNull(parts);
        assertEquals(3, parts.size());
        part = parts.get(0);
        assertEquals(Part.Kind.TEXT, part.getKind());
        assertEquals("tell me a joke", ((TextPart)part).getText());
        part = parts.get(1);
        assertEquals(Part.Kind.FILE, part.getKind());
        FileContent filePart = ((FilePart) part).getFile();
        assertEquals("file:///path/to/file.txt", ((FileWithUri) filePart).uri());
        assertEquals("text/plain", filePart.mimeType());
        part = parts.get(2);
        assertEquals(Part.Kind.FILE, part.getKind());
        filePart = ((FilePart) part).getFile();
        assertEquals("aGVsbG8=", ((FileWithBytes) filePart).bytes());
        assertEquals("hello.txt", filePart.name());
        assertTrue(task.getMetadata().isEmpty());
    }

    @Test
    public void testA2AClientCancelTask() throws Exception {
        this.server.when(
                        request()
                                .withMethod("POST")
                                .withPath("/")
                                .withBody(JsonBody.json(CANCEL_TASK_TEST_REQUEST, MatchType.STRICT))

                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(CANCEL_TASK_TEST_RESPONSE)
                );

        A2AClient client = new A2AClient("http://localhost:4001");
        CancelTaskResponse response = client.cancelTask("request-1234",
                new TaskIdParams("de38c76d-d54c-436c-8b9f-4c2703648d64", new HashMap<>()));

        assertEquals("2.0", response.getJsonrpc());
        assertEquals(1, response.getId());
        Object result = response.getResult();
        assertInstanceOf(Task.class, result);
        Task task = (Task) result;
        assertEquals("de38c76d-d54c-436c-8b9f-4c2703648d64", task.getId());
        assertEquals("c295ea44-7543-4f78-b524-7a38915ad6e4", task.getContextId());
        assertEquals(TaskState.CANCELED, task.getStatus().state());
        assertTrue(task.getMetadata().isEmpty());
    }

    @Test
    public void testA2AClientGetTaskPushNotificationConfig() throws Exception {
        this.server.when(
                        request()
                                .withMethod("POST")
                                .withPath("/")
                                .withBody(JsonBody.json(GET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_REQUEST, MatchType.STRICT))

                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(GET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE)
                );

        A2AClient client = new A2AClient("http://localhost:4001");
        GetTaskPushNotificationConfigResponse response = client.getTaskPushNotificationConfig("1",
                new GetTaskPushNotificationConfigParams("de38c76d-d54c-436c-8b9f-4c2703648d64", null, new HashMap<>()));
        assertEquals("2.0", response.getJsonrpc());
        assertEquals(1, response.getId());
        assertInstanceOf(TaskPushNotificationConfig.class, response.getResult());
        TaskPushNotificationConfig taskPushNotificationConfig = (TaskPushNotificationConfig) response.getResult();
        PushNotificationConfig pushNotificationConfig = taskPushNotificationConfig.pushNotificationConfig();
        assertNotNull(pushNotificationConfig);
        assertEquals("https://example.com/callback", pushNotificationConfig.url());
        PushNotificationAuthenticationInfo authenticationInfo = pushNotificationConfig.authentication();
        assertTrue(authenticationInfo.schemes().size() == 1);
        assertEquals("jwt", authenticationInfo.schemes().get(0));
    }

    @Test
    public void testA2AClientSetTaskPushNotificationConfig() throws Exception {
        this.server.when(
                        request()
                                .withMethod("POST")
                                .withPath("/")
                                .withBody(JsonBody.json(SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_REQUEST, MatchType.STRICT))

                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE)
                );

        A2AClient client = new A2AClient("http://localhost:4001");
        SetTaskPushNotificationConfigResponse response = client.setTaskPushNotificationConfig("1",
                "de38c76d-d54c-436c-8b9f-4c2703648d64",
                new PushNotificationConfig.Builder()
                        .url("https://example.com/callback")
                        .authenticationInfo(new PushNotificationAuthenticationInfo(Collections.singletonList("jwt"), null))
                        .build());
        assertEquals("2.0", response.getJsonrpc());
        assertEquals(1, response.getId());
        assertInstanceOf(TaskPushNotificationConfig.class, response.getResult());
        TaskPushNotificationConfig taskPushNotificationConfig = (TaskPushNotificationConfig) response.getResult();
        PushNotificationConfig pushNotificationConfig = taskPushNotificationConfig.pushNotificationConfig();
        assertNotNull(pushNotificationConfig);
        assertEquals("https://example.com/callback", pushNotificationConfig.url());
        PushNotificationAuthenticationInfo authenticationInfo = pushNotificationConfig.authentication();
        assertTrue(authenticationInfo.schemes().size() == 1);
        assertEquals("jwt", authenticationInfo.schemes().get(0));
    }


    @Test
    public void testA2AClientGetAgentCard() throws Exception {
        this.server.when(
                        request()
                                .withMethod("GET")
                                .withPath("/.well-known/agent-card.json")
                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(AGENT_CARD)
                );

        A2AClient client = new A2AClient("http://localhost:4001");
        AgentCard agentCard = client.getAgentCard();
        assertEquals("GeoSpatial Route Planner Agent", agentCard.name());
        assertEquals("Provides advanced route planning, traffic analysis, and custom map generation services. This agent can calculate optimal routes, estimate travel times considering real-time traffic, and create personalized maps with points of interest.", agentCard.description());
        assertEquals("https://georoute-agent.example.com/a2a/v1", agentCard.url());
        assertEquals("Example Geo Services Inc.", agentCard.provider().organization());
        assertEquals("https://www.examplegeoservices.com", agentCard.provider().url());
        assertEquals("1.2.0", agentCard.version());
        assertEquals("https://docs.examplegeoservices.com/georoute-agent/api", agentCard.documentationUrl());
        assertTrue(agentCard.capabilities().streaming());
        assertTrue(agentCard.capabilities().pushNotifications());
        assertFalse(agentCard.capabilities().stateTransitionHistory());
        Map<String, SecurityScheme> securitySchemes = agentCard.securitySchemes();
        assertNotNull(securitySchemes);
        OpenIdConnectSecurityScheme google = (OpenIdConnectSecurityScheme) securitySchemes.get("google");
        assertEquals("openIdConnect", google.getType());
        assertEquals("https://accounts.google.com/.well-known/openid-configuration", google.getOpenIdConnectUrl());
        List<Map<String, List<String>>> security = agentCard.security();
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
        assertTrue(agentCard.supportsAuthenticatedExtendedCard());
        assertEquals("https://georoute-agent.example.com/icon.png", agentCard.iconUrl());
        assertEquals("0.2.9", agentCard.protocolVersion());
        assertEquals("JSONRPC", agentCard.preferredTransport());
        List<AgentInterface> additionalInterfaces = agentCard.additionalInterfaces();
        assertEquals(3, additionalInterfaces.size());
        AgentInterface jsonrpc = new AgentInterface(TransportProtocol.JSONRPC.asString(), "https://georoute-agent.example.com/a2a/v1");
        AgentInterface grpc = new AgentInterface(TransportProtocol.GRPC.asString(), "https://georoute-agent.example.com/a2a/grpc");
        AgentInterface httpJson = new AgentInterface(TransportProtocol.HTTP_JSON.asString(), "https://georoute-agent.example.com/a2a/json");
        assertEquals(jsonrpc, additionalInterfaces.get(0));
        assertEquals(grpc, additionalInterfaces.get(1));
        assertEquals(httpJson, additionalInterfaces.get(2));
    }

    @Test
    public void testA2AClientGetAuthenticatedExtendedAgentCard() throws Exception {
        this.server.when(
                        request()
                                .withMethod("POST")
                                .withPath("/")
                                .withBody(JsonBody.json(GET_AUTHENTICATED_EXTENDED_AGENT_CARD_REQUEST, MatchType.STRICT))

                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(GET_AUTHENTICATED_EXTENDED_AGENT_CARD_RESPONSE)
                );

        A2AClient client = new A2AClient("http://localhost:4001");
        GetAuthenticatedExtendedCardResponse response = client.getAuthenticatedExtendedCard("1", null);
        AgentCard agentCard = response.getResult();
        assertEquals("GeoSpatial Route Planner Agent Extended", agentCard.name());
        assertEquals("Extended description", agentCard.description());
        assertEquals("https://georoute-agent.example.com/a2a/v1", agentCard.url());
        assertEquals("Example Geo Services Inc.", agentCard.provider().organization());
        assertEquals("https://www.examplegeoservices.com", agentCard.provider().url());
        assertEquals("1.2.0", agentCard.version());
        assertEquals("https://docs.examplegeoservices.com/georoute-agent/api", agentCard.documentationUrl());
        assertTrue(agentCard.capabilities().streaming());
        assertTrue(agentCard.capabilities().pushNotifications());
        assertFalse(agentCard.capabilities().stateTransitionHistory());
        Map<String, SecurityScheme> securitySchemes = agentCard.securitySchemes();
        assertNotNull(securitySchemes);
        OpenIdConnectSecurityScheme google = (OpenIdConnectSecurityScheme) securitySchemes.get("google");
        assertEquals("openIdConnect", google.getType());
        assertEquals("https://accounts.google.com/.well-known/openid-configuration", google.getOpenIdConnectUrl());
        List<Map<String, List<String>>> security = agentCard.security();
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
        assertTrue(agentCard.supportsAuthenticatedExtendedCard());
        assertEquals("https://georoute-agent.example.com/icon.png", agentCard.iconUrl());
        assertEquals("0.2.9", agentCard.protocolVersion());
        List<AgentCardSignature> signatures = agentCard.signatures();
        assertEquals(1, signatures.size());
        AgentCardSignature signature = new AgentCardSignature(null,
                "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpPU0UiLCJraWQiOiJrZXktMSIsImprdSI6Imh0dHBzOi8vZXhhbXBsZS5jb20vYWdlbnQvandrcy5qc29uIn0",
                "QFdkNLNszlGj3z3u0YQGt_T9LixY3qtdQpZmsTdDHDe3fXV9y9-B3m2-XgCpzuhiLt8E0tV6HXoZKHv4GtHgKQ");
        assertEquals(signature, signatures.get(0));
    }

    @Test
    public void testA2AClientSendMessageWithFilePart() throws Exception {
        this.server.when(
                        request()
                                .withMethod("POST")
                                .withPath("/")
                                .withBody(JsonBody.json(SEND_MESSAGE_WITH_FILE_PART_TEST_REQUEST, MatchType.STRICT))

                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(SEND_MESSAGE_WITH_FILE_PART_TEST_RESPONSE)
                );

        A2AClient client = new A2AClient("http://localhost:4001");
        Message message = new Message.Builder()
                .role(Message.Role.USER)
                .parts(List.of(
                        new TextPart("analyze this image"),
                        new FilePart(new FileWithUri("image/jpeg", null, "file:///path/to/image.jpg"))
                ))
                .contextId("context-1234")
                .messageId("message-1234-with-file")
                .build();
        MessageSendConfiguration configuration = new MessageSendConfiguration.Builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(true)
                .build();
        MessageSendParams params = new MessageSendParams.Builder()
                .message(message)
                .configuration(configuration)
                .build();

        SendMessageResponse response = client.sendMessage("request-1234-with-file", params);

        assertEquals("2.0", response.getJsonrpc());
        assertNotNull(response.getId());
        Object result = response.getResult();
        assertInstanceOf(Task.class, result);
        Task task = (Task) result;
        assertEquals("de38c76d-d54c-436c-8b9f-4c2703648d64", task.getId());
        assertNotNull(task.getContextId());
        assertEquals(TaskState.COMPLETED, task.getStatus().state());
        assertEquals(1, task.getArtifacts().size());
        Artifact artifact = task.getArtifacts().get(0);
        assertEquals("artifact-1", artifact.artifactId());
        assertEquals("image-analysis", artifact.name());
        assertEquals(1, artifact.parts().size());
        Part<?> part = artifact.parts().get(0);
        assertEquals(Part.Kind.TEXT, part.getKind());
        assertEquals("This is an image of a cat sitting on a windowsill.", ((TextPart) part).getText());
        assertTrue(task.getMetadata().isEmpty());
    }

    @Test
    public void testA2AClientSendMessageWithDataPart() throws Exception {
        this.server.when(
                        request()
                                .withMethod("POST")
                                .withPath("/")
                                .withBody(JsonBody.json(SEND_MESSAGE_WITH_DATA_PART_TEST_REQUEST, MatchType.STRICT))

                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(SEND_MESSAGE_WITH_DATA_PART_TEST_RESPONSE)
                );

        A2AClient client = new A2AClient("http://localhost:4001");
        
        Map<String, Object> data = new HashMap<>();
        data.put("temperature", 25.5);
        data.put("humidity", 60.2);
        data.put("location", "San Francisco");
        data.put("timestamp", "2024-01-15T10:30:00Z");
        
        Message message = new Message.Builder()
                .role(Message.Role.USER)
                .parts(List.of(
                        new TextPart("process this data"),
                        new DataPart(data)
                ))
                .contextId("context-1234")
                .messageId("message-1234-with-data")
                .build();
        MessageSendConfiguration configuration = new MessageSendConfiguration.Builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(true)
                .build();
        MessageSendParams params = new MessageSendParams.Builder()
                .message(message)
                .configuration(configuration)
                .build();

        SendMessageResponse response = client.sendMessage("request-1234-with-data", params);

        assertEquals("2.0", response.getJsonrpc());
        assertNotNull(response.getId());
        Object result = response.getResult();
        assertInstanceOf(Task.class, result);
        Task task = (Task) result;
        assertEquals("de38c76d-d54c-436c-8b9f-4c2703648d64", task.getId());
        assertNotNull(task.getContextId());
        assertEquals(TaskState.COMPLETED, task.getStatus().state());
        assertEquals(1, task.getArtifacts().size());
        Artifact artifact = task.getArtifacts().get(0);
        assertEquals("artifact-1", artifact.artifactId());
        assertEquals("data-analysis", artifact.name());
        assertEquals(1, artifact.parts().size());
        Part<?> part = artifact.parts().get(0);
        assertEquals(Part.Kind.TEXT, part.getKind());
        assertEquals("Processed weather data: Temperature is 25.5°C, humidity is 60.2% in San Francisco.", ((TextPart) part).getText());
        assertTrue(task.getMetadata().isEmpty());
    }

    @Test
    public void testA2AClientSendMessageWithMixedParts() throws Exception {
        this.server.when(
                        request()
                                .withMethod("POST")
                                .withPath("/")
                                .withBody(JsonBody.json(SEND_MESSAGE_WITH_MIXED_PARTS_TEST_REQUEST, MatchType.STRICT))

                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(SEND_MESSAGE_WITH_MIXED_PARTS_TEST_RESPONSE)
                );

        A2AClient client = new A2AClient("http://localhost:4001");
        
        Map<String, Object> data = new HashMap<>();
        data.put("chartType", "bar");
        data.put("dataPoints", List.of(10, 20, 30, 40));
        data.put("labels", List.of("Q1", "Q2", "Q3", "Q4"));
        
        Message message = new Message.Builder()
                .role(Message.Role.USER)
                .parts(List.of(
                        new TextPart("analyze this data and image"),
                        new FilePart(new FileWithBytes("image/png", "chart.png", "aGVsbG8=")),
                        new DataPart(data)
                ))
                .contextId("context-1234")
                .messageId("message-1234-with-mixed")
                .build();
        MessageSendConfiguration configuration = new MessageSendConfiguration.Builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(true)
                .build();
        MessageSendParams params = new MessageSendParams.Builder()
                .message(message)
                .configuration(configuration)
                .build();

        SendMessageResponse response = client.sendMessage("request-1234-with-mixed", params);

        assertEquals("2.0", response.getJsonrpc());
        assertNotNull(response.getId());
        Object result = response.getResult();
        assertInstanceOf(Task.class, result);
        Task task = (Task) result;
        assertEquals("de38c76d-d54c-436c-8b9f-4c2703648d64", task.getId());
        assertNotNull(task.getContextId());
        assertEquals(TaskState.COMPLETED, task.getStatus().state());
        assertEquals(1, task.getArtifacts().size());
        Artifact artifact = task.getArtifacts().get(0);
        assertEquals("artifact-1", artifact.artifactId());
        assertEquals("mixed-analysis", artifact.name());
        assertEquals(1, artifact.parts().size());
        Part<?> part = artifact.parts().get(0);
        assertEquals(Part.Kind.TEXT, part.getKind());
        assertEquals("Analyzed chart image and data: Bar chart showing quarterly data with values [10, 20, 30, 40].", ((TextPart) part).getText());
        assertTrue(task.getMetadata().isEmpty());
    }
}