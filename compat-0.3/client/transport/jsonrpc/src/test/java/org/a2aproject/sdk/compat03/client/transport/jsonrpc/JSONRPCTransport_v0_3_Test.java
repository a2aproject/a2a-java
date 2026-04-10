package org.a2aproject.sdk.compat03.client.transport.jsonrpc;

import static org.a2aproject.sdk.compat03.client.transport.jsonrpc.JsonMessages_v0_3.AGENT_CARD;
import static org.a2aproject.sdk.compat03.client.transport.jsonrpc.JsonMessages_v0_3.AGENT_CARD_SUPPORTS_EXTENDED;
import static org.a2aproject.sdk.compat03.client.transport.jsonrpc.JsonMessages_v0_3.CANCEL_TASK_TEST_REQUEST;
import static org.a2aproject.sdk.compat03.client.transport.jsonrpc.JsonMessages_v0_3.CANCEL_TASK_TEST_RESPONSE;
import static org.a2aproject.sdk.compat03.client.transport.jsonrpc.JsonMessages_v0_3.GET_AUTHENTICATED_EXTENDED_AGENT_CARD_REQUEST;
import static org.a2aproject.sdk.compat03.client.transport.jsonrpc.JsonMessages_v0_3.GET_AUTHENTICATED_EXTENDED_AGENT_CARD_RESPONSE;
import static org.a2aproject.sdk.compat03.client.transport.jsonrpc.JsonMessages_v0_3.GET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_REQUEST;
import static org.a2aproject.sdk.compat03.client.transport.jsonrpc.JsonMessages_v0_3.GET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE;
import static org.a2aproject.sdk.compat03.client.transport.jsonrpc.JsonMessages_v0_3.GET_TASK_TEST_REQUEST;
import static org.a2aproject.sdk.compat03.client.transport.jsonrpc.JsonMessages_v0_3.GET_TASK_TEST_RESPONSE;
import static org.a2aproject.sdk.compat03.client.transport.jsonrpc.JsonMessages_v0_3.SEND_MESSAGE_ERROR_TEST_RESPONSE;
import static org.a2aproject.sdk.compat03.client.transport.jsonrpc.JsonMessages_v0_3.SEND_MESSAGE_TEST_REQUEST;
import static org.a2aproject.sdk.compat03.client.transport.jsonrpc.JsonMessages_v0_3.SEND_MESSAGE_TEST_REQUEST_WITH_MESSAGE_RESPONSE;
import static org.a2aproject.sdk.compat03.client.transport.jsonrpc.JsonMessages_v0_3.SEND_MESSAGE_TEST_RESPONSE;
import static org.a2aproject.sdk.compat03.client.transport.jsonrpc.JsonMessages_v0_3.SEND_MESSAGE_TEST_RESPONSE_WITH_MESSAGE_RESPONSE;
import static org.a2aproject.sdk.compat03.client.transport.jsonrpc.JsonMessages_v0_3.SEND_MESSAGE_WITH_DATA_PART_TEST_REQUEST;
import static org.a2aproject.sdk.compat03.client.transport.jsonrpc.JsonMessages_v0_3.SEND_MESSAGE_WITH_DATA_PART_TEST_RESPONSE;
import static org.a2aproject.sdk.compat03.client.transport.jsonrpc.JsonMessages_v0_3.SEND_MESSAGE_WITH_ERROR_TEST_REQUEST;
import static org.a2aproject.sdk.compat03.client.transport.jsonrpc.JsonMessages_v0_3.SEND_MESSAGE_WITH_FILE_PART_TEST_REQUEST;
import static org.a2aproject.sdk.compat03.client.transport.jsonrpc.JsonMessages_v0_3.SEND_MESSAGE_WITH_FILE_PART_TEST_RESPONSE;
import static org.a2aproject.sdk.compat03.client.transport.jsonrpc.JsonMessages_v0_3.SEND_MESSAGE_WITH_MIXED_PARTS_TEST_REQUEST;
import static org.a2aproject.sdk.compat03.client.transport.jsonrpc.JsonMessages_v0_3.SEND_MESSAGE_WITH_MIXED_PARTS_TEST_RESPONSE;
import static org.a2aproject.sdk.compat03.client.transport.jsonrpc.JsonMessages_v0_3.SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_REQUEST;
import static org.a2aproject.sdk.compat03.client.transport.jsonrpc.JsonMessages_v0_3.SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE;
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

import org.a2aproject.sdk.compat03.spec.A2AClientException_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentInterface_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentSkill_v0_3;
import org.a2aproject.sdk.compat03.spec.Artifact_v0_3;
import org.a2aproject.sdk.compat03.spec.DataPart_v0_3;
import org.a2aproject.sdk.compat03.spec.EventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.FileContent_v0_3;
import org.a2aproject.sdk.compat03.spec.FilePart_v0_3;
import org.a2aproject.sdk.compat03.spec.FileWithBytes_v0_3;
import org.a2aproject.sdk.compat03.spec.FileWithUri_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.compat03.spec.MessageSendConfiguration_v0_3;
import org.a2aproject.sdk.compat03.spec.MessageSendParams_v0_3;
import org.a2aproject.sdk.compat03.spec.OpenIdConnectSecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.Part_v0_3;
import org.a2aproject.sdk.compat03.spec.PushNotificationAuthenticationInfo_v0_3;
import org.a2aproject.sdk.compat03.spec.PushNotificationConfig_v0_3;
import org.a2aproject.sdk.compat03.spec.SecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskIdParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskPushNotificationConfig_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskQueryParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskState_v0_3;
import org.a2aproject.sdk.compat03.spec.TextPart_v0_3;
import org.a2aproject.sdk.compat03.spec.TransportProtocol_v0_3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.MatchType;
import org.mockserver.model.JsonBody;

public class JSONRPCTransport_v0_3_Test {

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

        JSONRPCTransport_v0_3 client = new JSONRPCTransport_v0_3("http://localhost:4001");
        Message_v0_3 message = new Message_v0_3.Builder()
                .role(Message_v0_3.Role.USER)
                .parts(Collections.singletonList(new TextPart_v0_3("tell me a joke")))
                .contextId("context-1234")
                .messageId("message-1234")
                .build();
        MessageSendConfiguration_v0_3 configuration = new MessageSendConfiguration_v0_3.Builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(true)
                .build();
        MessageSendParams_v0_3 params = new MessageSendParams_v0_3.Builder()
                .message(message)
                .configuration(configuration)
                .build();

        EventKind_v0_3 result = client.sendMessage(params, null);
        assertInstanceOf(Task_v0_3.class, result);
        Task_v0_3 task = (Task_v0_3) result;
        assertEquals("de38c76d-d54c-436c-8b9f-4c2703648d64", task.getId());
        assertNotNull(task.getContextId());
        assertEquals(TaskState_v0_3.COMPLETED,task.getStatus().state());
        assertEquals(1, task.getArtifacts().size());
        Artifact_v0_3 artifact = task.getArtifacts().get(0);
        assertEquals("artifact-1", artifact.artifactId());
        assertEquals("joke", artifact.name());
        assertEquals(1, artifact.parts().size());
        Part_v0_3<?> part = artifact.parts().get(0);
        assertEquals(Part_v0_3.Kind.TEXT, part.getKind());
        assertEquals("Why did the chicken cross the road? To get to the other side!", ((TextPart_v0_3) part).getText());
        assertTrue(task.getMetadata().isEmpty());
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

        JSONRPCTransport_v0_3 client = new JSONRPCTransport_v0_3("http://localhost:4001");
        Message_v0_3 message = new Message_v0_3.Builder()
                .role(Message_v0_3.Role.USER)
                .parts(Collections.singletonList(new TextPart_v0_3("tell me a joke")))
                .contextId("context-1234")
                .messageId("message-1234")
                .build();
        MessageSendConfiguration_v0_3 configuration = new MessageSendConfiguration_v0_3.Builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(true)
                .build();
        MessageSendParams_v0_3 params = new MessageSendParams_v0_3.Builder()
                .message(message)
                .configuration(configuration)
                .build();

        EventKind_v0_3 result = client.sendMessage(params, null);
        assertInstanceOf(Message_v0_3.class, result);
        Message_v0_3 agentMessage = (Message_v0_3) result;
        assertEquals(Message_v0_3.Role.AGENT, agentMessage.getRole());
        Part_v0_3<?> part = agentMessage.getParts().get(0);
        assertEquals(Part_v0_3.Kind.TEXT, part.getKind());
        assertEquals("Why did the chicken cross the road? To get to the other side!", ((TextPart_v0_3) part).getText());
        assertEquals("msg-456", agentMessage.getMessageId());
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

        JSONRPCTransport_v0_3 client = new JSONRPCTransport_v0_3("http://localhost:4001");
        Message_v0_3 message = new Message_v0_3.Builder()
                .role(Message_v0_3.Role.USER)
                .parts(Collections.singletonList(new TextPart_v0_3("tell me a joke")))
                .contextId("context-1234")
                .messageId("message-1234")
                .build();
        MessageSendConfiguration_v0_3 configuration = new MessageSendConfiguration_v0_3.Builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(true)
                .build();
        MessageSendParams_v0_3 params = new MessageSendParams_v0_3.Builder()
                .message(message)
                .configuration(configuration)
                .build();

        try {
            client.sendMessage(params, null);
            fail(); // should not reach here
        } catch (A2AClientException_v0_3 e) {
            assertTrue(e.getMessage().contains("Invalid parameters: Hello world"));
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

        JSONRPCTransport_v0_3 client = new JSONRPCTransport_v0_3("http://localhost:4001");
        Task_v0_3 task = client.getTask(new TaskQueryParams_v0_3("de38c76d-d54c-436c-8b9f-4c2703648d64",
                10), null);
        assertEquals("de38c76d-d54c-436c-8b9f-4c2703648d64", task.getId());
        assertEquals("c295ea44-7543-4f78-b524-7a38915ad6e4", task.getContextId());
        assertEquals(TaskState_v0_3.COMPLETED, task.getStatus().state());
        assertEquals(1, task.getArtifacts().size());
        Artifact_v0_3 artifact = task.getArtifacts().get(0);
        assertEquals(1, artifact.parts().size());
        assertEquals("artifact-1", artifact.artifactId());
        Part_v0_3<?> part = artifact.parts().get(0);
        assertEquals(Part_v0_3.Kind.TEXT, part.getKind());
        assertEquals("Why did the chicken cross the road? To get to the other side!", ((TextPart_v0_3) part).getText());
        assertTrue(task.getMetadata().isEmpty());
        List<Message_v0_3> history = task.getHistory();
        assertNotNull(history);
        assertEquals(1, history.size());
        Message_v0_3 message = history.get(0);
        assertEquals(Message_v0_3.Role.USER, message.getRole());
        List<Part_v0_3<?>> parts = message.getParts();
        assertNotNull(parts);
        assertEquals(3, parts.size());
        part = parts.get(0);
        assertEquals(Part_v0_3.Kind.TEXT, part.getKind());
        assertEquals("tell me a joke", ((TextPart_v0_3)part).getText());
        part = parts.get(1);
        assertEquals(Part_v0_3.Kind.FILE, part.getKind());
        FileContent_v0_3 filePart = ((FilePart_v0_3) part).getFile();
        assertEquals("file:///path/to/file.txt", ((FileWithUri_v0_3) filePart).uri());
        assertEquals("text/plain", filePart.mimeType());
        part = parts.get(2);
        assertEquals(Part_v0_3.Kind.FILE, part.getKind());
        filePart = ((FilePart_v0_3) part).getFile();
        assertEquals("aGVsbG8=", ((FileWithBytes_v0_3) filePart).bytes());
        assertEquals("hello.txt", filePart.name());
        assertTrue(task.getMetadata().isEmpty());
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

        JSONRPCTransport_v0_3 client = new JSONRPCTransport_v0_3("http://localhost:4001");
        Task_v0_3 task = client.cancelTask(new TaskIdParams_v0_3("de38c76d-d54c-436c-8b9f-4c2703648d64",
                new HashMap<>()), null);
        assertEquals("de38c76d-d54c-436c-8b9f-4c2703648d64", task.getId());
        assertEquals("c295ea44-7543-4f78-b524-7a38915ad6e4", task.getContextId());
        assertEquals(TaskState_v0_3.CANCELED, task.getStatus().state());
        assertTrue(task.getMetadata().isEmpty());
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

        JSONRPCTransport_v0_3 client = new JSONRPCTransport_v0_3("http://localhost:4001");
        TaskPushNotificationConfig_v0_3 taskPushNotificationConfig = client.getTaskPushNotificationConfiguration(
                new GetTaskPushNotificationConfigParams_v0_3("de38c76d-d54c-436c-8b9f-4c2703648d64", null,
                        new HashMap<>()), null);
        PushNotificationConfig_v0_3 pushNotificationConfig = taskPushNotificationConfig.pushNotificationConfig();
        assertNotNull(pushNotificationConfig);
        assertEquals("https://example.com/callback", pushNotificationConfig.url());
        PushNotificationAuthenticationInfo_v0_3 authenticationInfo = pushNotificationConfig.authentication();
        assertTrue(authenticationInfo.schemes().size() == 1);
        assertEquals("jwt", authenticationInfo.schemes().get(0));
    }

    @Test
    public void testA2AClientSetTaskPushNotificationConfig() throws Exception {
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

        JSONRPCTransport_v0_3 client = new JSONRPCTransport_v0_3("http://localhost:4001");
        TaskPushNotificationConfig_v0_3 taskPushNotificationConfig = client.setTaskPushNotificationConfiguration(
                new TaskPushNotificationConfig_v0_3("de38c76d-d54c-436c-8b9f-4c2703648d64",
                        new PushNotificationConfig_v0_3.Builder()
                                .url("https://example.com/callback")
                                .authenticationInfo(new PushNotificationAuthenticationInfo_v0_3(Collections.singletonList("jwt"),
                                        null))
                                .build()), null);
        PushNotificationConfig_v0_3 pushNotificationConfig = taskPushNotificationConfig.pushNotificationConfig();
        assertNotNull(pushNotificationConfig);
        assertEquals("https://example.com/callback", pushNotificationConfig.url());
        PushNotificationAuthenticationInfo_v0_3 authenticationInfo = pushNotificationConfig.authentication();
        assertEquals(1, authenticationInfo.schemes().size());
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

        JSONRPCTransport_v0_3 client = new JSONRPCTransport_v0_3("http://localhost:4001");
        AgentCard_v0_3 agentCard = client.getAgentCard(null);
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
        Map<String, SecurityScheme_v0_3> securitySchemes = agentCard.securitySchemes();
        assertNotNull(securitySchemes);
        OpenIdConnectSecurityScheme_v0_3 google = (OpenIdConnectSecurityScheme_v0_3) securitySchemes.get("google");
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
        List<AgentSkill_v0_3> skills = agentCard.skills();
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
        assertFalse(agentCard.supportsAuthenticatedExtendedCard());
        assertEquals("https://georoute-agent.example.com/icon.png", agentCard.iconUrl());
        assertEquals("0.2.9", agentCard.protocolVersion());
        assertEquals("JSONRPC", agentCard.preferredTransport());
        List<AgentInterface_v0_3> additionalInterfaces = agentCard.additionalInterfaces();
        assertEquals(3, additionalInterfaces.size());
        AgentInterface_v0_3 jsonrpc = new AgentInterface_v0_3(TransportProtocol_v0_3.JSONRPC.asString(), "https://georoute-agent.example.com/a2a/v1");
        AgentInterface_v0_3 grpc = new AgentInterface_v0_3(TransportProtocol_v0_3.GRPC.asString(), "https://georoute-agent.example.com/a2a/grpc");
        AgentInterface_v0_3 httpJson = new AgentInterface_v0_3(TransportProtocol_v0_3.HTTP_JSON.asString(), "https://georoute-agent.example.com/a2a/json");
        assertEquals(jsonrpc, additionalInterfaces.get(0));
        assertEquals(grpc, additionalInterfaces.get(1));
        assertEquals(httpJson, additionalInterfaces.get(2));
    }

    @Test
    public void testA2AClientGetAuthenticatedExtendedAgentCard() throws Exception {
        this.server.when(
                        request()
                                .withMethod("GET")
                                .withPath("/.well-known/agent-card.json")
                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(AGENT_CARD_SUPPORTS_EXTENDED)
                );
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

        JSONRPCTransport_v0_3 client = new JSONRPCTransport_v0_3("http://localhost:4001");
        AgentCard_v0_3 agentCard = client.getAgentCard(null);
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
        Map<String, SecurityScheme_v0_3> securitySchemes = agentCard.securitySchemes();
        assertNotNull(securitySchemes);
        OpenIdConnectSecurityScheme_v0_3 google = (OpenIdConnectSecurityScheme_v0_3) securitySchemes.get("google");
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
        List<AgentSkill_v0_3> skills = agentCard.skills();
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
        assertEquals("0.2.5", agentCard.protocolVersion());
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

        JSONRPCTransport_v0_3 client = new JSONRPCTransport_v0_3("http://localhost:4001");
        Message_v0_3 message = new Message_v0_3.Builder()
                .role(Message_v0_3.Role.USER)
                .parts(List.of(
                        new TextPart_v0_3("analyze this image"),
                        new FilePart_v0_3(new FileWithUri_v0_3("image/jpeg", null, "file:///path/to/image.jpg"))
                ))
                .contextId("context-1234")
                .messageId("message-1234-with-file")
                .build();
        MessageSendConfiguration_v0_3 configuration = new MessageSendConfiguration_v0_3.Builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(true)
                .build();
        MessageSendParams_v0_3 params = new MessageSendParams_v0_3.Builder()
                .message(message)
                .configuration(configuration)
                .build();

        EventKind_v0_3 result = client.sendMessage(params, null);
        assertInstanceOf(Task_v0_3.class, result);
        Task_v0_3 task = (Task_v0_3) result;
        assertEquals("de38c76d-d54c-436c-8b9f-4c2703648d64", task.getId());
        assertNotNull(task.getContextId());
        assertEquals(TaskState_v0_3.COMPLETED, task.getStatus().state());
        assertEquals(1, task.getArtifacts().size());
        Artifact_v0_3 artifact = task.getArtifacts().get(0);
        assertEquals("artifact-1", artifact.artifactId());
        assertEquals("image-analysis", artifact.name());
        assertEquals(1, artifact.parts().size());
        Part_v0_3<?> part = artifact.parts().get(0);
        assertEquals(Part_v0_3.Kind.TEXT, part.getKind());
        assertEquals("This is an image of a cat sitting on a windowsill.", ((TextPart_v0_3) part).getText());
        assertTrue(task.getMetadata().isEmpty());
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

        JSONRPCTransport_v0_3 client = new JSONRPCTransport_v0_3("http://localhost:4001");

        Map<String, Object> data = new HashMap<>();
        data.put("temperature", 25.5);
        data.put("humidity", 60.2);
        data.put("location", "San Francisco");
        data.put("timestamp", "2024-01-15T10:30:00Z");

        Message_v0_3 message = new Message_v0_3.Builder()
                .role(Message_v0_3.Role.USER)
                .parts(List.of(
                        new TextPart_v0_3("process this data"),
                        new DataPart_v0_3(data)
                ))
                .contextId("context-1234")
                .messageId("message-1234-with-data")
                .build();
        MessageSendConfiguration_v0_3 configuration = new MessageSendConfiguration_v0_3.Builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(true)
                .build();
        MessageSendParams_v0_3 params = new MessageSendParams_v0_3.Builder()
                .message(message)
                .configuration(configuration)
                .build();

        EventKind_v0_3 result = client.sendMessage(params, null);
        assertInstanceOf(Task_v0_3.class, result);
        Task_v0_3 task = (Task_v0_3) result;
        assertEquals("de38c76d-d54c-436c-8b9f-4c2703648d64", task.getId());
        assertNotNull(task.getContextId());
        assertEquals(TaskState_v0_3.COMPLETED, task.getStatus().state());
        assertEquals(1, task.getArtifacts().size());
        Artifact_v0_3 artifact = task.getArtifacts().get(0);
        assertEquals("artifact-1", artifact.artifactId());
        assertEquals("data-analysis", artifact.name());
        assertEquals(1, artifact.parts().size());
        Part_v0_3<?> part = artifact.parts().get(0);
        assertEquals(Part_v0_3.Kind.TEXT, part.getKind());
        assertEquals("Processed weather data: Temperature is 25.5°C, humidity is 60.2% in San Francisco.", ((TextPart_v0_3) part).getText());
        assertTrue(task.getMetadata().isEmpty());
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

        JSONRPCTransport_v0_3 client = new JSONRPCTransport_v0_3("http://localhost:4001");

        Map<String, Object> data = new HashMap<>();
        data.put("chartType", "bar");
        data.put("dataPoints", List.of(10, 20, 30, 40));
        data.put("labels", List.of("Q1", "Q2", "Q3", "Q4"));

        Message_v0_3 message = new Message_v0_3.Builder()
                .role(Message_v0_3.Role.USER)
                .parts(List.of(
                        new TextPart_v0_3("analyze this data and image"),
                        new FilePart_v0_3(new FileWithBytes_v0_3("image/png", "chart.png", "aGVsbG8=")),
                        new DataPart_v0_3(data)
                ))
                .contextId("context-1234")
                .messageId("message-1234-with-mixed")
                .build();
        MessageSendConfiguration_v0_3 configuration = new MessageSendConfiguration_v0_3.Builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(true)
                .build();
        MessageSendParams_v0_3 params = new MessageSendParams_v0_3.Builder()
                .message(message)
                .configuration(configuration)
                .build();

        EventKind_v0_3 result = client.sendMessage(params, null);
        assertInstanceOf(Task_v0_3.class, result);
        Task_v0_3 task = (Task_v0_3) result;
        assertEquals("de38c76d-d54c-436c-8b9f-4c2703648d64", task.getId());
        assertNotNull(task.getContextId());
        assertEquals(TaskState_v0_3.COMPLETED, task.getStatus().state());
        assertEquals(1, task.getArtifacts().size());
        Artifact_v0_3 artifact = task.getArtifacts().get(0);
        assertEquals("artifact-1", artifact.artifactId());
        assertEquals("mixed-analysis", artifact.name());
        assertEquals(1, artifact.parts().size());
        Part_v0_3<?> part = artifact.parts().get(0);
        assertEquals(Part_v0_3.Kind.TEXT, part.getKind());
        assertEquals("Analyzed chart image and data: Bar chart showing quarterly data with values [10, 20, 30, 40].", ((TextPart_v0_3) part).getText());
        assertTrue(task.getMetadata().isEmpty());
    }
}