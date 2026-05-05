package org.a2aproject.sdk.server.apps.common;

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
import org.a2aproject.sdk.client.Client;
import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.config.ClientConfig;
import org.a2aproject.sdk.jsonrpc.common.json.JsonUtil;
import org.a2aproject.sdk.server.apps.common.A2AGsonObjectMapper;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.A2AClientHTTPError;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.HTTPAuthSecurityScheme;
import org.a2aproject.sdk.spec.SecurityRequirement;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.junit.jupiter.api.Test;

/**
 * Abstract base class for authentication tests.
 * <p>
 * Provides minimal infrastructure for testing authentication without the full
 * server test setup. Tests verify that:
 * <ul>
 *   <li>{@code @Authenticated} annotations are enforced</li>
 *   <li>{@code @ActivateRequestContext} is present (prevents CDI context errors)</li>
 *   <li>Public endpoints remain accessible without credentials</li>
 * </ul>
 * <p>
 * Concrete test classes must implement:
 * <ul>
 *   <li>{@link #getTransportProtocol()} - protocol identifier</li>
 *   <li>{@link #getTransportUrl()} - server URL</li>
 *   <li>{@link #configureTransportWithAuth(ClientBuilder)} - add auth to transport</li>
 *   <li>{@link #configureTransport(ClientBuilder)} - transport without auth</li>
 * </ul>
 */
public abstract class AbstractA2AServerWithAuthTest {

    protected static final String TEST_USERNAME = "testuser";
    protected static final String TEST_PASSWORD = "testpass";
    protected static final String BASIC_AUTH_SCHEME_NAME = "basicAuth";

    /**
     * Get base64-encoded Basic Auth credentials.
     */
    protected static String getEncodedCredentials() {
        String credentials = TEST_USERNAME + ":" + TEST_PASSWORD;
        return Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    // Minimal test task
    protected static final Task MINIMAL_TASK = Task.builder()
            .id("task-123")
            .contextId("session-xyz")
            .status(new TaskStatus(TaskState.TASK_STATE_SUBMITTED))
            .build();

    protected final int serverPort;
    private Client authenticatedClient;
    private Client unauthenticatedClient;

    protected AbstractA2AServerWithAuthTest(int serverPort) {
        this.serverPort = serverPort;
    }

    /**
     * Get the transport protocol identifier.
     */
    protected abstract String getTransportProtocol();

    /**
     * Get the transport URL.
     */
    protected abstract String getTransportUrl();

    /**
     * Configure transport without authentication.
     */
    protected abstract void configureTransport(ClientBuilder builder);

    /**
     * Configure the transport with authentication credentials.
     * <p>
     * Subclasses implement this to add transport-specific authentication
     * (e.g., HTTP headers, gRPC call credentials).
     *
     * @param builder the client builder to configure
     */
    protected abstract void configureTransportWithAuth(ClientBuilder builder);

    /**
     * Get or create an authenticated client.
     */
    protected Client getAuthenticatedClient() throws A2AClientException {
        if (authenticatedClient == null) {
            authenticatedClient = createAuthenticatedClient();
        }
        return authenticatedClient;
    }

    /**
     * Get or create an unauthenticated client for testing auth failures.
     */
    protected Client getUnauthenticatedClient() throws A2AClientException {
        if (unauthenticatedClient == null) {
            unauthenticatedClient = createUnauthenticatedClient();
        }
        return unauthenticatedClient;
    }

    /**
     * Create an authenticated client.
     */
    private Client createAuthenticatedClient() throws A2AClientException {
        AgentCard agentCard = fetchAgentCardFromServer();
        ClientConfig clientConfig = new ClientConfig.Builder()
                .setStreaming(false)  // Non-streaming for simple tests
                .build();

        ClientBuilder clientBuilder = Client.builder(agentCard)
                .clientConfig(clientConfig);

        configureTransportWithAuth(clientBuilder);

        return clientBuilder.build();
    }

    /**
     * Create an unauthenticated client for testing authentication failures.
     */
    private Client createUnauthenticatedClient() throws A2AClientException {
        AgentCard agentCard = fetchAgentCardFromServer();
        ClientConfig clientConfig = new ClientConfig.Builder()
                .setStreaming(false)
                .build();

        ClientBuilder clientBuilder = Client.builder(agentCard)
                .clientConfig(clientConfig);

        configureTransport(clientBuilder);  // No auth

        return clientBuilder.build();
    }

    /**
     * Fetch the AgentCard from the server's /.well-known/agent-card.json endpoint.
     * Subclasses can override for transports that don't serve the agent card via HTTP.
     */
    protected AgentCard fetchAgentCardFromServer() {
        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + serverPort + "/.well-known/agent-card.json"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to fetch agent card: " + response.statusCode());
            }

            return JsonUtil.fromJson(response.body(), AgentCard.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch AgentCard from server", e);
        }
    }

    /**
     * RestAssured helper configured with Gson object mapper.
     */
    protected static RequestSpecification given() {
        return RestAssured.given()
                .config(RestAssured.config()
                        .objectMapperConfig(new ObjectMapperConfig(A2AGsonObjectMapper.INSTANCE)));
    }

    /**
     * RestAssured helper with authentication.
     */
    protected RequestSpecification givenAuthenticated() {
        return given()
                .auth().basic(TEST_USERNAME, TEST_PASSWORD);
    }

    /**
     * RestAssured helper without authentication.
     */
    protected RequestSpecification givenUnauthenticated() {
        return given();
    }

    /**
     * Save a task in the test task store via HTTP.
     */
    protected void saveTaskInTaskStore(Task task) throws Exception {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/test/task"))
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.toJson(task)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200 && response.statusCode() != 204) {
            throw new RuntimeException("Failed to save task: " + response.statusCode() + " " + response.body());
        }
    }

    /**
     * Delete a task from the test task store via HTTP.
     */
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

    /**
     * Test that getTask() requires authentication.
     * <p>
     * The AuthTestProfile configures HTTP Basic Auth and an embedded user store.
     * This test verifies that unauthenticated requests fail.
     */
    @Test
    public void testGetTaskRequiresAuthenticationUnauthenticated() throws Exception {
        // Save task in store
        saveTaskInTaskStore(MINIMAL_TASK);

        // Test unauthenticated request fails
        Client unauthClient = getUnauthenticatedClient();
        A2AClientException error = assertThrows(A2AClientException.class, () -> {
            unauthClient.getTask(new TaskQueryParams(MINIMAL_TASK.id()));
        });
        // Verify it's an authentication failure
        assertTrue(error.getMessage().contains("Authentication failed") ||
                   error.getMessage().contains("401") ||
                   error.getMessage().contains("Unauthorized"),
                "Expected authentication error, got: " + error.getMessage());

        // Cleanup
        deleteTaskInTaskStore(MINIMAL_TASK.id());
    }

    /**
     * Test that getTask() succeeds with authentication.
     * <p>
     * The client sends Basic Auth headers matching the credentials in the embedded user store.
     */
    @Test
    public void testGetTaskWithAuthentication() throws Exception {
        // Save task in store
        saveTaskInTaskStore(MINIMAL_TASK);

        // Test authenticated request succeeds (client includes Basic Auth headers)
        Client client = getAuthenticatedClient();
        Task result = client.getTask(new TaskQueryParams(MINIMAL_TASK.id()));
        assertNotNull(result);

        // Cleanup
        deleteTaskInTaskStore(MINIMAL_TASK.id());
    }

    /**
     * Test that getAgentCard() is publicly accessible without authentication.
     * <p>
     * The /.well-known/agent-card.json endpoint should be accessible without
     * credentials to allow agent discovery, even when HTTP Basic Auth is enabled.
     */
    @Test
    public void testGetAgentCardIsPublic() {
        givenUnauthenticated()
                .get("/.well-known/agent-card.json")
                .then()
                .statusCode(200);
    }

    /**
     * Test that Basic Auth credentials actually work via direct HTTP.
     * <p>
     * This test uses RestAssured with Basic Auth to verify the embedded user store
     * is properly configured and accepts our test credentials.
     */
    @Test
    public void testBasicAuthWorksViaHttp() throws Exception {
        // Save task in store
        saveTaskInTaskStore(MINIMAL_TASK);

        // Test with valid credentials via RestAssured
        givenAuthenticated()
                .contentType("application/json")
                .body("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"getTask\",\"params\":{\"id\":\"" + MINIMAL_TASK.id() + "\"}}")
                .post("/")
                .then()
                .statusCode(200);

        // Cleanup
        deleteTaskInTaskStore(MINIMAL_TASK.id());
    }
}
