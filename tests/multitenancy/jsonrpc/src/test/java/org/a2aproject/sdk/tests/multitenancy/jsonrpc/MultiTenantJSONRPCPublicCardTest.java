package org.a2aproject.sdk.tests.multitenancy.jsonrpc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the JSON-RPC transport serves the public agent card endpoints
 * (/.well-known/agent-card.json and /.well-known/{tenant}/agent-card.json)
 * when the REST transport is NOT on the classpath.
 */
@QuarkusTest
public class MultiTenantJSONRPCPublicCardTest {

    @Test
    public void defaultPublicCardIsServed() {
        String response = RestAssured.given()
                .when().get("/.well-known/agent-card.json")
                .then().statusCode(200)
                .extract().asString();
        assertEquals("Default Agent", JsonPath.from(response).getString("name"));
    }

    @Test
    public void acmeTenantPublicCardIsServed() {
        String response = RestAssured.given()
                .when().get("/.well-known/acme/agent-card.json")
                .then().statusCode(200)
                .extract().asString();
        assertEquals("Acme Agent", JsonPath.from(response).getString("name"));
    }

    @Test
    public void betaTenantPublicCardIsServed() {
        String response = RestAssured.given()
                .when().get("/.well-known/beta/agent-card.json")
                .then().statusCode(200)
                .extract().asString();
        assertEquals("Beta Agent", JsonPath.from(response).getString("name"));
    }

    @Test
    public void unknownTenantReturns404() {
        RestAssured.given()
                .when().get("/.well-known/unknown-corp/agent-card.json")
                .then().statusCode(404);
    }
}
