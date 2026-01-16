package io.a2a.jsonrpc.common.json;

import static io.a2a.spec.APIKeySecurityScheme.Location.COOKIE;
import static io.a2a.spec.APIKeySecurityScheme.Location.HEADER;
import static io.a2a.spec.APIKeySecurityScheme.Location.QUERY;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.a2a.spec.APIKeySecurityScheme;
import io.a2a.spec.HTTPAuthSecurityScheme;
import io.a2a.spec.MutualTLSSecurityScheme;
import io.a2a.spec.OAuth2SecurityScheme;
import io.a2a.spec.OAuthFlows;
import io.a2a.spec.OpenIdConnectSecurityScheme;
import io.a2a.spec.PasswordOAuthFlow;
import io.a2a.spec.SecurityScheme;

/**
 * Tests for SecurityScheme serialization and deserialization using Gson.
 */
class SecuritySchemeSerializationTest {

    @Test
    void testHTTPAuthSecuritySchemeSerialization() throws JsonProcessingException {
        SecurityScheme scheme = HTTPAuthSecurityScheme.builder()
                .scheme("basic")
                .description("Basic HTTP authentication")
                .build();

        doTestSecuritySchemeSerialization(scheme, HTTPAuthSecurityScheme.TYPE, Map.of("scheme", "basic"));

    }

    @Test
    void testHTTPAuthSecuritySchemeDeserialization() throws JsonProcessingException {
        String json = """
                {
                  "httpAuthSecurityScheme" : {
                    "scheme": "basic"
                  }
                }""";
        SecurityScheme securityScheme = JsonUtil.fromJson(json, SecurityScheme.class);
        assertInstanceOf(HTTPAuthSecurityScheme.class, securityScheme);
        HTTPAuthSecurityScheme scheme = (HTTPAuthSecurityScheme) securityScheme;
        assertEquals("basic", scheme.scheme());
        assertNull(scheme.bearerFormat());
    }

    @Test
    void testHTTPAuthSecuritySchemeWithBearerFormatSerialization() throws JsonProcessingException {
        SecurityScheme scheme = HTTPAuthSecurityScheme.builder()
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT bearer token authentication")
                .build();

        doTestSecuritySchemeSerialization(scheme, HTTPAuthSecurityScheme.TYPE,
                Map.of("scheme", "bearer",
                        "bearerFormat", "JWT",
                        "description", "JWT bearer token authentication"));
    }

    @Test
    void testHTTPAuthSecuritySchemeWithBearerFormatDeserialization() throws JsonProcessingException {
        String json = """
                {
                  "httpAuthSecurityScheme" : {
                    "scheme": "bearer",
                    "bearerFormat": "JWT",
                    "description": "JWT authentication"
                  }
                }""";
        SecurityScheme securityScheme = JsonUtil.fromJson(json, SecurityScheme.class);
        assertInstanceOf(HTTPAuthSecurityScheme.class, securityScheme);
        HTTPAuthSecurityScheme scheme = (HTTPAuthSecurityScheme) securityScheme;
        assertEquals("bearer", scheme.scheme());
        assertEquals("JWT", scheme.bearerFormat());
        assertEquals("JWT authentication", scheme.description());
    }


    @Test
    void testAPIKeySecuritySchemeSerialization() throws JsonProcessingException {
        SecurityScheme scheme = APIKeySecurityScheme.builder()
                .location(HEADER)
                .name("bar")
                .build();

        doTestSecuritySchemeSerialization(scheme, APIKeySecurityScheme.TYPE, Map.of("location", "header",
                "name", "bar"));
    }

    @Test
    void testAPIKeySecuritySchemeDeserialization() throws JsonProcessingException {
        String json = """
                {
                  "apiKeySecurityScheme" : {
                    "location": "cookie",
                    "name": "bar"
                  }
                }""";
        SecurityScheme securityScheme = JsonUtil.fromJson(json, SecurityScheme.class);
        assertInstanceOf(APIKeySecurityScheme.class, securityScheme);
        APIKeySecurityScheme scheme = (APIKeySecurityScheme) securityScheme;
        assertEquals(COOKIE, scheme.location());
        assertEquals("bar", scheme.name());
    }

    @Test
    void testAPIKeySecuritySchemeWithDescriptionSerialization() throws JsonProcessingException {
        SecurityScheme scheme = APIKeySecurityScheme.builder()
                .location(QUERY)
                .name("api_key")
                .description("API key authentication via query parameter")
                .build();

        doTestSecuritySchemeSerialization(scheme, APIKeySecurityScheme.TYPE,
                Map.of("location", "query",
                        "name", "api_key",
                        "description", "API key authentication via query parameter"));
    }

    @Test
    void testOAuth2SecuritySchemeSerialization() throws JsonProcessingException {
        PasswordOAuthFlow passwordFlow = new PasswordOAuthFlow(
                "https://example.com/oauth/refresh",
                Map.of("read", "Read access", "write", "Write access"),
                "https://example.com/oauth/token"
        );

        OAuthFlows flows = OAuthFlows.builder()
                .password(passwordFlow)
                .build();

        SecurityScheme scheme = OAuth2SecurityScheme.builder()
                .flows(flows)
                .description("OAuth 2.0 password flow")
                .oauth2MetadataUrl("https://example.com/.well-known/oauth-authorization-server")
                .build();

        // Verify serialization with nested OAuth flow fields
        String json = JsonUtil.toJson(scheme);
        assertNotNull(json);
        assertTrue(json.contains(OAuth2SecurityScheme.TYPE));
        assertTrue(json.contains("\"description\":\"OAuth 2.0 password flow\""));
        assertTrue(json.contains("\"oauth2MetadataUrl\":\"https://example.com/.well-known/oauth-authorization-server\""));
        assertTrue(json.contains("\"tokenUrl\":\"https://example.com/oauth/token\""));
        assertTrue(json.contains("\"read\":\"Read access\""));

        SecurityScheme deserialized = JsonUtil.fromJson(json, SecurityScheme.class);
        assertEquals(scheme, deserialized);
    }

    @Test
    void testOAuth2SecuritySchemeDeserialization() throws JsonProcessingException {
        String json = """
                {
                  "oauth2SecurityScheme" : {
                    "flows": {
                      "password": {
                        "tokenUrl": "https://example.com/oauth/token",
                        "refreshUrl": "https://example.com/oauth/refresh",
                        "scopes": {
                          "read": "Read access",
                          "write": "Write access"
                        }
                      }
                    },
                    "description": "OAuth 2.0 authentication"
                  }
                }""";

        SecurityScheme securityScheme = JsonUtil.fromJson(json, SecurityScheme.class);
        assertInstanceOf(OAuth2SecurityScheme.class, securityScheme);
        OAuth2SecurityScheme scheme = (OAuth2SecurityScheme) securityScheme;
        assertEquals("OAuth 2.0 authentication", scheme.description());
        assertNotNull(scheme.flows());
        assertNotNull(scheme.flows().password());
        assertEquals("https://example.com/oauth/token", scheme.flows().password().tokenUrl());
        assertEquals("https://example.com/oauth/refresh", scheme.flows().password().refreshUrl());
        assertEquals(2, scheme.flows().password().scopes().size());
        assertEquals("Read access", scheme.flows().password().scopes().get("read"));
    }

    @Test
    void testOpenIdConnectSecuritySchemeSerialization() throws JsonProcessingException {
        SecurityScheme scheme = OpenIdConnectSecurityScheme.builder()
                .openIdConnectUrl("https://example.com/.well-known/openid-configuration")
                .description("OpenID Connect authentication")
                .build();

        doTestSecuritySchemeSerialization(scheme, OpenIdConnectSecurityScheme.TYPE,
                Map.of("openIdConnectUrl", "https://example.com/.well-known/openid-configuration",
                        "description", "OpenID Connect authentication"));
    }

    @Test
    void testOpenIdConnectSecuritySchemeDeserialization() throws JsonProcessingException {
        String json = """
                {
                  "openIdConnectSecurityScheme" : {
                    "openIdConnectUrl": "https://example.com/.well-known/openid-configuration",
                    "description": "OIDC authentication"
                  }
                }""";

        SecurityScheme securityScheme = JsonUtil.fromJson(json, SecurityScheme.class);
        assertInstanceOf(OpenIdConnectSecurityScheme.class, securityScheme);
        OpenIdConnectSecurityScheme scheme = (OpenIdConnectSecurityScheme) securityScheme;
        assertEquals("https://example.com/.well-known/openid-configuration", scheme.openIdConnectUrl());
        assertEquals("OIDC authentication", scheme.description());
    }

    @Test
    void testMutualTLSSecuritySchemeSerialization() throws JsonProcessingException {
        SecurityScheme scheme = new MutualTLSSecurityScheme("Client certificate authentication required");

        doTestSecuritySchemeSerialization(scheme, MutualTLSSecurityScheme.TYPE,
                Map.of("description", "Client certificate authentication required"));
    }

    @Test
    void testMutualTLSSecuritySchemeDeserialization() throws JsonProcessingException {
        String json = """
                {
                  "mtlsSecurityScheme" : {
                    "description": "mTLS authentication"
                  }
                }""";

        SecurityScheme securityScheme = JsonUtil.fromJson(json, SecurityScheme.class);
        assertInstanceOf(MutualTLSSecurityScheme.class, securityScheme);
        MutualTLSSecurityScheme scheme = (MutualTLSSecurityScheme) securityScheme;
        assertEquals("mTLS authentication", scheme.description());
    }

    @Test
    void testMutualTLSSecuritySchemeWithNullDescriptionDeserialization() throws JsonProcessingException {
        String json = """
                {
                  "mtlsSecurityScheme" : {
                  }
                }""";

        SecurityScheme securityScheme = JsonUtil.fromJson(json, SecurityScheme.class);
        assertInstanceOf(MutualTLSSecurityScheme.class, securityScheme);
        MutualTLSSecurityScheme scheme = (MutualTLSSecurityScheme) securityScheme;
        assertNull(scheme.description());
    }

    void doTestSecuritySchemeSerialization(SecurityScheme scheme, String schemeType, Map<String, String> expectedFields) throws JsonProcessingException {
        // Serialize to JSON
        String json = JsonUtil.toJson(scheme);

        // Verify JSON contains expected fields
        assertNotNull(json);
        assertTrue(json.contains(schemeType));
        for (Map.Entry<String, String> entry : expectedFields.entrySet()) {
            String expectedField = format("\"%s\":\"%s\"", entry.getKey(), entry.getValue());
            assertTrue(json.contains(expectedField), expectedField + " not found in JSON");
        }

        // Deserialize back to Task
        SecurityScheme deserialized = JsonUtil.fromJson(json, SecurityScheme.class);

        assertEquals(scheme, deserialized);
    }
}