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
import org.a2aproject.sdk.spec.AgentInterface;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class MultiTenantRESTTest {

    @Test
    public void knownTenantRoutesToAcmeExecutor() {
        String response = postMessageSend("acme");
        assertArtifactText(response, "acme");
    }

    @Test
    public void secondTenantRoutesToBetaExecutor() {
        String response = postMessageSend("beta");
        assertArtifactText(response, "beta");
    }

    @Test
    public void unknownTenantFallsBackToDefault() {
        String response = postMessageSend("unknown-corp");
        assertArtifactText(response, "default");
    }

    @Test
    public void noTenantUsesDefault() {
        String response = postMessageSend(null);
        assertArtifactText(response, "default");
    }

    @Test
    public void getExtendedAgentCardWithAcmeTenant() {
        String response = RestAssured.given()
                .header("A2A-Version", AgentInterface.CURRENT_PROTOCOL_VERSION)
                .when().get("/acme/extendedAgentCard")
                .then().statusCode(200)
                .extract().asString();
        JsonPath json = JsonPath.from(response);
        assertEquals("acme-extended", json.getString("name"));
    }

    @Test
    public void getExtendedAgentCardWithBetaTenant() {
        String response = RestAssured.given()
                .header("A2A-Version", AgentInterface.CURRENT_PROTOCOL_VERSION)
                .when().get("/beta/extendedAgentCard")
                .then().statusCode(200)
                .extract().asString();
        JsonPath json = JsonPath.from(response);
        assertEquals("beta-extended", json.getString("name"));
    }

    @Test
    public void getExtendedAgentCardWithUnknownTenant() {
        String response = RestAssured.given()
                .header("A2A-Version", AgentInterface.CURRENT_PROTOCOL_VERSION)
                .when().get("/unknown/extendedAgentCard")
                .then().statusCode(200)
                .extract().asString();
        JsonPath json = JsonPath.from(response);
        assertEquals("default-extended", json.getString("name"));
    }

    @Test
    public void getExtendedAgentCardWithoutTenant() {
        String response = RestAssured.given()
                .header("A2A-Version", AgentInterface.CURRENT_PROTOCOL_VERSION)
                .when().get("/extendedAgentCard")
                .then().statusCode(200)
                .extract().asString();
        JsonPath json = JsonPath.from(response);
        assertEquals("default-extended", json.getString("name"));
    }

    @Test
    public void streamingWithKnownTenant() {
        Response response = RestAssured.given()
                .urlEncodingEnabled(false)
                .header("A2A-Version", AgentInterface.CURRENT_PROTOCOL_VERSION)
                .contentType("application/json")
                .body(buildMessageBody())
                .when().post("/acme/message:stream");

        assertEquals(200, response.getStatusCode());
        String body = response.getBody().asString();
        assertTrue(body.contains("\"text\":\"acme\""), "Stream should contain acme artifact: " + body);
    }

    @Test
    public void streamingWithUnknownTenantUsesDefault() {
        Response response = RestAssured.given()
                .urlEncodingEnabled(false)
                .header("A2A-Version", AgentInterface.CURRENT_PROTOCOL_VERSION)
                .contentType("application/json")
                .body(buildMessageBody())
                .when().post("/unknown/message:stream");

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
                    () -> postMessageSend("acme"), pool);
            CompletableFuture<String> betaFuture = CompletableFuture.supplyAsync(
                    () -> postMessageSend("beta"), pool);
            CompletableFuture<String> defaultFuture = CompletableFuture.supplyAsync(
                    () -> postMessageSend(null), pool);

            assertArtifactText(acmeFuture.get(), "acme");
            assertArtifactText(betaFuture.get(), "beta");
            assertArtifactText(defaultFuture.get(), "default");
        } finally {
            pool.shutdown();
        }
    }

    private String postMessageSend(@Nullable String tenant) {
        String path = tenant != null ? "/" + tenant + "/message:send" : "/message:send";
        return RestAssured.given()
                .urlEncodingEnabled(false)
                .header("A2A-Version", AgentInterface.CURRENT_PROTOCOL_VERSION)
                .contentType("application/json")
                .body(buildMessageBody())
                .when().post(path)
                .then().statusCode(200)
                .extract().asString();
    }

    private void assertArtifactText(String response, String expected) {
        JsonPath json = JsonPath.from(response);
        assertNotNull(json.getString("task"), "Expected task in response: " + response);
        assertEquals(expected, json.getString("task.artifacts[0].parts[0].text"));
    }

    private static String buildMessageBody() {
        return """
                {
                  "message": {
                    "messageId": "%s",
                    "role": "ROLE_USER",
                    "parts": [{"text": "hello"}]
                  }
                }""".formatted(UUID.randomUUID().toString());
    }
}
