package org.a2aproject.sdk.compat03.conversion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.specification.RequestSpecification;
import jakarta.ws.rs.core.MediaType;
import org.a2aproject.sdk.compat03.client.Client_v0_3;
import org.a2aproject.sdk.compat03.client.ClientBuilder_v0_3;
import org.a2aproject.sdk.compat03.client.config.ClientConfig_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.domain.TaskMapper_v0_3;
import org.a2aproject.sdk.compat03.spec.A2AClientException_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCapabilities_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentInterface_v0_3;
import org.a2aproject.sdk.compat03.spec.HTTPAuthSecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskQueryParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskState_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskStatus_v0_3;
import org.a2aproject.sdk.jsonrpc.common.json.JsonUtil;
import org.junit.jupiter.api.Test;

/**
 * Abstract base class for v0.3 authentication tests.
 * <p>
 * Mirrors {@code AbstractA2AServerWithAuthTest} from v1.0 but uses v0.3 client types.
 * Tests verify that security enforcement works correctly through the v0.3 compatibility layer.
 */
public abstract class AbstractA2AServerWithAuthTest_v0_3 {

    protected static final String TEST_USERNAME = "testuser";
    protected static final String TEST_PASSWORD = "testpass";
    protected static final String BASIC_AUTH_SCHEME_NAME = "basicAuth";

    protected static String getEncodedCredentials() {
        String credentials = TEST_USERNAME + ":" + TEST_PASSWORD;
        return Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    protected static final Task_v0_3 MINIMAL_TASK = new Task_v0_3.Builder()
            .id("task-123")
            .contextId("session-xyz")
            .status(new TaskStatus_v0_3(TaskState_v0_3.SUBMITTED))
            .build();

    protected final int serverPort;
    private Client_v0_3 authenticatedClient;
    private Client_v0_3 unauthenticatedClient;

    protected AbstractA2AServerWithAuthTest_v0_3(int serverPort) {
        this.serverPort = serverPort;
    }

    protected abstract String getTransportProtocol();

    protected abstract String getTransportUrl();

    protected abstract void configureTransport(ClientBuilder_v0_3 builder);

    protected abstract void configureTransportWithAuth(ClientBuilder_v0_3 builder);

    protected Client_v0_3 getAuthenticatedClient() throws A2AClientException_v0_3 {
        if (authenticatedClient == null) {
            authenticatedClient = createAuthenticatedClient();
        }
        return authenticatedClient;
    }

    protected Client_v0_3 getUnauthenticatedClient() throws A2AClientException_v0_3 {
        if (unauthenticatedClient == null) {
            unauthenticatedClient = createUnauthenticatedClient();
        }
        return unauthenticatedClient;
    }

    private Client_v0_3 createAuthenticatedClient() throws A2AClientException_v0_3 {
        AgentCard_v0_3 agentCard = createTestAgentCard();
        ClientConfig_v0_3 clientConfig = new ClientConfig_v0_3.Builder()
                .setStreaming(false)
                .build();

        ClientBuilder_v0_3 clientBuilder = Client_v0_3.builder(agentCard)
                .clientConfig(clientConfig);

        configureTransportWithAuth(clientBuilder);

        return clientBuilder.build();
    }

    private Client_v0_3 createUnauthenticatedClient() throws A2AClientException_v0_3 {
        AgentCard_v0_3 agentCard = createTestAgentCard();
        ClientConfig_v0_3 clientConfig = new ClientConfig_v0_3.Builder()
                .setStreaming(false)
                .build();

        ClientBuilder_v0_3 clientBuilder = Client_v0_3.builder(agentCard)
                .clientConfig(clientConfig);

        configureTransport(clientBuilder);

        return clientBuilder.build();
    }

    private AgentCard_v0_3 createTestAgentCard() {
        return new AgentCard_v0_3.Builder()
                .name("test-card")
                .description("A test agent card")
                .url(getTransportUrl())
                .version("1.0")
                .preferredTransport(getTransportProtocol())
                .capabilities(new AgentCapabilities_v0_3.Builder()
                        .streaming(false)
                        .pushNotifications(false)
                        .stateTransitionHistory(false)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .additionalInterfaces(List.of(new AgentInterface_v0_3(getTransportProtocol(), getTransportUrl())))
                .securitySchemes(Map.of(
                        BASIC_AUTH_SCHEME_NAME,
                        new HTTPAuthSecurityScheme_v0_3.Builder()
                                .scheme("basic")
                                .description("HTTP Basic authentication")
                                .build()))
                .security(List.of(Map.of(BASIC_AUTH_SCHEME_NAME, List.of())))
                .build();
    }

    protected static RequestSpecification given() {
        return RestAssured.given()
                .config(RestAssured.config()
                        .objectMapperConfig(new ObjectMapperConfig(V10GsonObjectMapper_v0_3.INSTANCE)));
    }

    protected RequestSpecification givenAuthenticated() {
        return given()
                .auth().basic(TEST_USERNAME, TEST_PASSWORD);
    }

    protected RequestSpecification givenUnauthenticated() {
        return given();
    }

    protected void saveTaskInTaskStore(Task_v0_3 task) throws Exception {
        org.a2aproject.sdk.spec.Task v10Task = TaskMapper_v0_3.INSTANCE.toV10(task);

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/test/task"))
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.toJson(v10Task)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200 && response.statusCode() != 204) {
            throw new RuntimeException("Failed to save task: " + response.statusCode() + " " + response.body());
        }
    }

    protected void deleteTaskInTaskStore(String taskId) throws Exception {
        HttpClient httpClient = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/test/task/" + taskId))
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 && response.statusCode() != 204) {
            throw new RuntimeException("Failed to delete task: " + response.statusCode() + " " + response.body());
        }
    }

    @Test
    public void testGetTaskRequiresAuthenticationUnauthenticated() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);

        Client_v0_3 unauthClient = getUnauthenticatedClient();
        A2AClientException_v0_3 error = assertThrows(A2AClientException_v0_3.class, () -> {
            unauthClient.getTask(new TaskQueryParams_v0_3(MINIMAL_TASK.id()));
        });
        assertTrue(error.getMessage().contains("Authentication failed") ||
                   error.getMessage().contains("401") ||
                   error.getMessage().contains("Unauthorized"),
                "Expected authentication error, got: " + error.getMessage());

        deleteTaskInTaskStore(MINIMAL_TASK.id());
    }

    @Test
    public void testGetTaskWithAuthentication() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);

        Client_v0_3 client = getAuthenticatedClient();
        Task_v0_3 result = client.getTask(new TaskQueryParams_v0_3(MINIMAL_TASK.id()));
        assertNotNull(result);
        assertEquals(MINIMAL_TASK.id(), result.id());

        deleteTaskInTaskStore(MINIMAL_TASK.id());
    }

    @Test
    public void testGetAgentCardIsPublic() {
        givenUnauthenticated()
                .get("/.well-known/agent-card.json")
                .then()
                .statusCode(200);
    }

    @Test
    public void testBasicAuthWorksViaHttp() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);

        givenAuthenticated()
                .contentType("application/json")
                .body("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tasks/get\",\"params\":{\"id\":\"" + MINIMAL_TASK.id() + "\"}}")
                .post("/")
                .then()
                .statusCode(200);

        deleteTaskInTaskStore(MINIMAL_TASK.id());
    }
}
