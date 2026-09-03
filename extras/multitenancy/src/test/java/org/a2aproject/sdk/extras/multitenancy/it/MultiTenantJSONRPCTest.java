package org.a2aproject.sdk.extras.multitenancy.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.a2aproject.sdk.common.A2AHeaders;
import org.a2aproject.sdk.spec.AgentInterface;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class MultiTenantJSONRPCTest {

    @Test
    public void knownTenantRoutesToAcmeExecutor() {
        String response = rawPost(buildSendMessageRequest("acme"));
        assertArtifactText(response, "acme");
    }

    @Test
    public void secondTenantRoutesToBetaExecutor() {
        String response = rawPost(buildSendMessageRequest("beta"));
        assertArtifactText(response, "beta");
    }

    @Test
    public void unknownTenantFallsBackToDefault() {
        String response = rawPost(buildSendMessageRequest("unknown-corp"));
        assertArtifactText(response, "default");
    }

    @Test
    public void nullTenantUsesDefault() {
        String response = rawPost(buildSendMessageRequest(null));
        assertArtifactText(response, "default");
    }

    @Test
    public void getExtendedAgentCardWithAcmeTenant() {
        String response = rawPost(buildGetExtendedAgentCardRequest("acme"));
        JsonPath json = JsonPath.from(response);
        assertEquals("acme-extended", json.getString("result.name"));
    }

    @Test
    public void getExtendedAgentCardWithBetaTenant() {
        String response = rawPost(buildGetExtendedAgentCardRequest("beta"));
        JsonPath json = JsonPath.from(response);
        assertEquals("beta-extended", json.getString("result.name"));
    }

    @Test
    public void getExtendedAgentCardWithUnknownTenant() {
        String response = rawPost(buildGetExtendedAgentCardRequest("unknown"));
        JsonPath json = JsonPath.from(response);
        assertEquals("default-extended", json.getString("result.name"));
    }

    @Test
    public void getExtendedAgentCardWithoutTenant() {
        String response = rawPost(buildGetExtendedAgentCardRequest(null));
        JsonPath json = JsonPath.from(response);
        assertEquals("default-extended", json.getString("result.name"));
    }

    @Test
    public void streamingWithKnownTenant() {
        String msgId = UUID.randomUUID().toString();
        Response response = RestAssured.given()
                .header(A2AHeaders.A2A_VERSION, AgentInterface.CURRENT_PROTOCOL_VERSION)
                .contentType("application/json")
                .body(buildStreamingMessageRequest("acme", msgId))
                .when().post("/");

        assertEquals(200, response.getStatusCode());
        String body = response.getBody().asString();
        assertTrue(body.contains("\"text\":\"acme\""), "Stream should contain acme artifact: " + body);
    }

    @Test
    public void streamingWithUnknownTenantUsesDefault() {
        String msgId = UUID.randomUUID().toString();
        Response response = RestAssured.given()
                .header(A2AHeaders.A2A_VERSION, AgentInterface.CURRENT_PROTOCOL_VERSION)
                .contentType("application/json")
                .body(buildStreamingMessageRequest("unknown", msgId))
                .when().post("/");

        assertEquals(200, response.getStatusCode());
        String body = response.getBody().asString();
        assertTrue(body.contains("\"text\":\"default\""), "Stream should contain default artifact: " + body);
    }

    @Test
    public void getPublicAgentCardWithAcmeTenant() {
        String response = RestAssured.given()
                .when().get("/.well-known/acme/agent-card.json")
                .then().statusCode(200)
                .extract().asString();
        JsonPath json = JsonPath.from(response);
        assertEquals("Acme Agent", json.getString("name"));
    }

    @Test
    public void getPublicAgentCardWithBetaTenant() {
        String response = RestAssured.given()
                .when().get("/.well-known/beta/agent-card.json")
                .then().statusCode(200)
                .extract().asString();
        JsonPath json = JsonPath.from(response);
        assertEquals("Beta Agent", json.getString("name"));
    }

    @Test
    public void unknownTenantReturns404() {
        RestAssured.given()
                .when().get("/.well-known/unknown/agent-card.json")
                .then().statusCode(404);
    }

    @Test
    public void getPublicAgentCardWithoutTenantReturnsDefault() {
        String response = RestAssured.given()
                .when().get("/.well-known/agent-card.json")
                .then().statusCode(200)
                .extract().asString();
        JsonPath json = JsonPath.from(response);
        assertEquals("Multi-Tenant Test Agent", json.getString("name"));
    }

    @Test
    public void concurrentRequestsForDifferentTenants() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(3);
        try {
            CompletableFuture<String> acmeFuture = CompletableFuture.supplyAsync(
                    () -> rawPost(buildSendMessageRequest("acme", UUID.randomUUID().toString())), pool);
            CompletableFuture<String> betaFuture = CompletableFuture.supplyAsync(
                    () -> rawPost(buildSendMessageRequest("beta", UUID.randomUUID().toString())), pool);
            CompletableFuture<String> defaultFuture = CompletableFuture.supplyAsync(
                    () -> rawPost(buildSendMessageRequest(null, UUID.randomUUID().toString())), pool);

            assertArtifactText(acmeFuture.get(), "acme");
            assertArtifactText(betaFuture.get(), "beta");
            assertArtifactText(defaultFuture.get(), "default");
        } finally {
            pool.shutdown();
        }
    }

    private String rawPost(String body) {
        return RestAssured.given()
                .header(A2AHeaders.A2A_VERSION, AgentInterface.CURRENT_PROTOCOL_VERSION)
                .contentType("application/json")
                .body(body)
                .when().post("/")
                .then().statusCode(200)
                .extract().asString();
    }

    private void assertArtifactText(String response, String expected) {
        JsonPath json = JsonPath.from(response);
        assertNotNull(json.getString("result"), "Expected result in response: " + response);
        assertEquals(expected, json.getString("result.task.artifacts[0].parts[0].text"));
    }

    private static String buildSendMessageRequest(@Nullable String tenant) {
        return buildSendMessageRequest(tenant, "msg-1");
    }

    private static String buildSendMessageRequest(@Nullable String tenant, String messageId) {
        return """
                {
                  "jsonrpc": "2.0",
                  "id": "1",
                  "method": "SendMessage",
                  "params": {
                    "message": {
                      "messageId": "%s",
                      "role": "ROLE_USER",
                      "parts": [{"text": "hello"}]
                    }%s
                  }
                }""".formatted(messageId, buildTenantField(tenant));
    }

    private static String buildStreamingMessageRequest(@Nullable String tenant, String messageId) {
        return """
                {
                  "jsonrpc": "2.0",
                  "id": "1",
                  "method": "SendStreamingMessage",
                  "params": {
                    "message": {
                      "messageId": "%s",
                      "role": "ROLE_USER",
                      "parts": [{"text": "hello"}]
                    }%s
                  }
                }""".formatted(messageId, buildTenantField(tenant));
    }

    private static String buildTenantField(@Nullable String tenant) {
        return tenant != null && !tenant.isBlank()
                ? """
                , "tenant": "%s\"""".formatted(tenant)
                : "";
    }

    private static String buildGetExtendedAgentCardRequest(@Nullable String tenant) {
        String params = tenant != null
                ? """
                , "params": { "tenant": "%s" }""".formatted(tenant)
                : "";
        return """
                { "jsonrpc": "2.0", "method": "GetExtendedAgentCard", "id": "1"%s }""".formatted(params);
    }
}
