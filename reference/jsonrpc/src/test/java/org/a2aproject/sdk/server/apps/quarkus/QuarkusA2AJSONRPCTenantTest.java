package org.a2aproject.sdk.server.apps.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;

import jakarta.ws.rs.core.MediaType;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.a2aproject.sdk.common.A2AHeaders;
import org.a2aproject.sdk.spec.AgentInterface;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for tenant resolution in the JSON-RPC reference server.
 *
 * <p>Verifies the two-source tenant resolution end-to-end:
 * <ol>
 *   <li>Tenant in the JSON-RPC {@code params} body → executor sees that value</li>
 *   <li>Tenant absent from body, present in URL path → path value is used as fallback</li>
 *   <li>Tenant in body AND in URL path → they must match or {@code InvalidParamsError} is returned</li>
 *   <li>No tenant anywhere → executor sees empty string</li>
 * </ol>
 *
 * <p>The test executor responds to {@code "tenant-echo:"} messages by emitting an artifact
 * whose text is the tenant value from
 * {@link org.a2aproject.sdk.server.agentexecution.RequestContext#getTenant()}.
 */
@QuarkusTest
public class QuarkusA2AJSONRPCTenantTest {

    // --- body-tenant tests (spec-compliant primary source) ---

    @Test
    public void testBodyTenant_executorReceivesIt() {
        // Tenant in params body → executor sees "my-tenant"
        String response = postTo("/", "my-tenant");
        assertArtifactText(response, "my-tenant");
    }

    @Test
    public void testBodyTenant_multiSegmentValue() {
        // Multi-segment tenant value in body is preserved as-is
        String response = postTo("/", "org/team");
        assertArtifactText(response, "org/team");
    }

    @Test
    public void testBodyTenant_specialCharacters() {
        // Hyphens, underscores, dots are preserved in tenant value
        String response = postTo("/", "org-name_v1.0");
        assertArtifactText(response, "org-name_v1.0");
    }

    // --- path-tenant fallback tests (SDK extension to the spec) ---

    @Test
    public void testPathTenant_singleSegment_usedWhenBodyOmitsTenant() {
        // POST /acme with no body tenant → executor sees "acme" (from path)
        String response = postWithoutBodyTenantTo("/acme");
        assertArtifactText(response, "acme");
    }

    @Test
    public void testPathTenant_multiSegment_usedWhenBodyOmitsTenant() {
        // POST /org/team with no body tenant → executor sees "org/team" (from path)
        String response = postWithoutBodyTenantTo("/org/team");
        assertArtifactText(response, "org/team");
    }

    @Test
    public void testMismatchedTenants_returnsInvalidParamsError() {
        // POST /path-tenant with body tenant "body-tenant" → tenants differ → JSON-RPC error
        Response response = RestAssured.given()
                .header(A2AHeaders.A2A_VERSION, AgentInterface.CURRENT_PROTOCOL_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildRequest("body-tenant"))
                .when()
                .post("/path-tenant")
                .then()
                .statusCode(200)
                .extract()
                .response();
        JsonPath json = JsonPath.from(response.asString());
        assertNotNull(json.getString("error"), "Expected JSON-RPC error but got: " + response.asString());
        assertEquals(-32602, json.getInt("error.code"), "Expected InvalidParams error code -32602");
    }

    @Test
    public void testMatchingTenants_urlAndBodySame_success() {
        // POST /acme with body tenant "acme" → tenants match → executor sees "acme"
        String response = postTo("/acme", "acme");
        assertArtifactText(response, "acme");
    }

    @Test
    public void testBlankBodyTenant_fallsBackToPathTenant() {
        // POST /acme with empty body tenant "" → falls back to path tenant "acme"
        String response = postWithBlankBodyTenantTo("/acme");
        assertArtifactText(response, "acme");
    }

    @Test
    public void testRootPath_noTenant() {
        // POST / with no body tenant → executor sees empty string (no tenant)
        String response = postWithoutBodyTenantTo("/");
        assertArtifactText(response, "");
    }

    // --- helpers ---

    private String postTo(String path, String bodyTenant) {
        return rawPost(path, buildRequest(bodyTenant));
    }

    private String postWithoutBodyTenantTo(String path) {
        return rawPost(path, buildRequestNoTenant());
    }

    private String postWithBlankBodyTenantTo(String path) {
        return rawPost(path, buildRequest(""));
    }

    private String rawPost(String path, String body) {
        Response response = RestAssured.given()
                .header(A2AHeaders.A2A_VERSION, AgentInterface.CURRENT_PROTOCOL_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .when()
                .post(path)
                .then()
                .statusCode(200)
                .extract()
                .response();
        return response.asString();
    }

    private void assertArtifactText(String responseBody, String expected) {
        JsonPath json = JsonPath.from(responseBody);
        String actual = json.getString("result.task.artifacts[0].parts[0].text");
        assertNotNull(actual, "Artifact text was null in response: " + responseBody);
        assertEquals(expected, actual,
                "Expected tenant '" + expected + "' in artifact but got '" + actual + "'");
    }

    private String buildRequest(String tenant) {
        return """
            {
              "jsonrpc": "2.0",
              "id": "%s",
              "method": "SendMessage",
              "params": {
                "tenant": "%s",
                "message": {
                  "messageId": "%s",
                  "contextId": "ctx-tenant-test",
                  "role": "ROLE_USER",
                  "parts": [{"text": "tenant-echo:test"}]
                }
              }
            }
            """.formatted(UUID.randomUUID(), tenant, UUID.randomUUID());
    }

    private String buildRequestNoTenant() {
        return """
            {
              "jsonrpc": "2.0",
              "id": "%s",
              "method": "SendMessage",
              "params": {
                "message": {
                  "messageId": "%s",
                  "contextId": "ctx-tenant-test",
                  "role": "ROLE_USER",
                  "parts": [{"text": "tenant-echo:test"}]
                }
              }
            }
            """.formatted(UUID.randomUUID(), UUID.randomUUID());
    }
}
