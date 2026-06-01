package org.a2aproject.sdk.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.net.URISyntaxException;

import org.a2aproject.sdk.spec.AgentInterface;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Utils} tenant-related methods.
 */
class UtilsTest {

    // ========== buildBaseUrl(AgentInterface, String) Tests ==========

    @Test
    void testBuildBaseUrl_withAgentInterface_noTenant() {
        AgentInterface iface = new AgentInterface("JSONRPC", "http://example.com", "");
        String url = Utils.buildBaseUrl(iface, null);
        assertEquals("http://example.com", url);
    }

    @Test
    void testBuildBaseUrl_withAgentInterface_withDefaultTenant() {
        AgentInterface iface = new AgentInterface("JSONRPC", "http://example.com", "/default-tenant");
        String url = Utils.buildBaseUrl(iface, null);
        assertEquals("http://example.com/default-tenant", url);
    }

    @Test
    void testBuildBaseUrl_withAgentInterface_withOverrideTenant() {
        AgentInterface iface = new AgentInterface("JSONRPC", "http://example.com", "/default-tenant");
        String url = Utils.buildBaseUrl(iface, "/custom-tenant");
        assertEquals("http://example.com/custom-tenant", url);
    }

    @Test
    void testBuildBaseUrl_withAgentInterface_urlWithTrailingSlash() {
        AgentInterface iface = new AgentInterface("JSONRPC", "http://example.com/", "/tenant");
        String url = Utils.buildBaseUrl(iface, null);
        assertEquals("http://example.com/tenant", url);
    }

    @Test
    void testBuildBaseUrl_withAgentInterface_urlWithoutTrailingSlash() {
        AgentInterface iface = new AgentInterface("JSONRPC", "http://example.com", "/tenant");
        String url = Utils.buildBaseUrl(iface, null);
        assertEquals("http://example.com/tenant", url);
    }

    @Test
    void testBuildBaseUrl_withAgentInterface_nullInterface_throws() {
        assertThrows(IllegalArgumentException.class, () -> {
            Utils.buildBaseUrl((AgentInterface) null, "/tenant");
        });
    }

    // ========== Security Validation Tests ==========

    @Test
    void testValidateTenant_pathTraversal_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            AgentInterface iface = new AgentInterface("JSONRPC", "http://example.com", "");
            Utils.buildBaseUrl(iface, "../../admin");
        });
        assertNotNull(ex.getMessage());
        assertEquals("Tenant path contains invalid '..' sequence (path traversal attempt)", ex.getMessage());
    }

    @Test
    void testValidateTenant_pathTraversalWithSlash_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            AgentInterface iface = new AgentInterface("JSONRPC", "http://example.com", "");
            Utils.buildBaseUrl(iface, "/../admin");
        });
        assertNotNull(ex.getMessage());
        assertEquals("Tenant path contains invalid '..' sequence (path traversal attempt)", ex.getMessage());
    }

    @Test
    void testValidateTenant_tooLong_throws() {
        String longTenant = "/" + "a".repeat(256);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            AgentInterface iface = new AgentInterface("JSONRPC", "http://example.com", "");
            Utils.buildBaseUrl(iface, longTenant);
        });
        assertNotNull(ex.getMessage());
        assertEquals("Tenant path exceeds maximum length of 256 characters", ex.getMessage());
    }

    @Test
    void testValidateTenant_maxLengthAllowed_succeeds() {
        // 256 characters total (including leading slash)
        AgentInterface iface = new AgentInterface("JSONRPC", "http://example.com", "");
        String maxTenant = "/" + "a".repeat(255);
        String url = Utils.buildBaseUrl(iface, maxTenant);
        assertNotNull(url);
        assertEquals("http://example.com/" + "a".repeat(255), url);
    }

    @Test
    void testValidateTenant_invalidCharactersSpace_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            AgentInterface iface = new AgentInterface("JSONRPC", "http://example.com", "");
            Utils.buildBaseUrl(iface, "/tenant with spaces");
        });
        assertNotNull(ex.getMessage());
        assertEquals("Tenant path contains invalid characters. Only /a-zA-Z0-9_-. are allowed", ex.getMessage());
    }

    @Test
    void testValidateTenant_invalidCharactersSpecial_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            AgentInterface iface = new AgentInterface("JSONRPC", "http://example.com", "");
            Utils.buildBaseUrl(iface, "/tenant@123");
        });
        assertNotNull(ex.getMessage());
        assertEquals("Tenant path contains invalid characters. Only /a-zA-Z0-9_-. are allowed", ex.getMessage());
    }

    @Test
    void testValidateTenant_invalidCharactersQuery_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            AgentInterface iface = new AgentInterface("JSONRPC", "http://example.com", "");
            Utils.buildBaseUrl(iface, "/tenant?param=value");
        });
        assertNotNull(ex.getMessage());
        assertEquals("Tenant path contains invalid characters. Only /a-zA-Z0-9_-. are allowed", ex.getMessage());
    }

    @Test
    void testValidateTenant_validCharacters_succeeds() {
        // Test all allowed characters: /a-zA-Z0-9_-.
        AgentInterface iface = new AgentInterface("JSONRPC", "http://example.com", "");
        String url1 = Utils.buildBaseUrl(iface, "/tenant-name");
        assertEquals("http://example.com/tenant-name", url1);

        String url2 = Utils.buildBaseUrl(iface, "/tenant_name");
        assertEquals("http://example.com/tenant_name", url2);

        String url3 = Utils.buildBaseUrl(iface, "/Tenant123");
        assertEquals("http://example.com/Tenant123", url3);

        String url4 = Utils.buildBaseUrl(iface, "/multi/level/tenant");
        assertEquals("http://example.com/multi/level/tenant", url4);

        String url5 = Utils.buildBaseUrl(iface, "/tenant.v1");
        assertEquals("http://example.com/tenant.v1", url5);

        String url6 = Utils.buildBaseUrl(iface, "/.well-known");
        assertEquals("http://example.com/.well-known", url6);
    }

    @Test
    void testValidateTenant_emptyString_succeeds() {
        // Empty string is valid (no tenant)
        AgentInterface iface = new AgentInterface("JSONRPC", "http://example.com", "");
        String url = Utils.buildBaseUrl(iface, "");
        assertEquals("http://example.com", url);
    }

    @Test
    void testValidateTenant_multiLevelTenant_succeeds() {
        AgentInterface iface = new AgentInterface("JSONRPC", "http://example.com", "");
        String url = Utils.buildBaseUrl(iface, "/org/team/tenant");
        assertEquals("http://example.com/org/team/tenant", url);
    }

    // ========== Edge Case Tests ==========

    @Test
    void testBuildBaseUrl_complexScenario() {
        // Base URL with trailing slash, default tenant, custom override
        AgentInterface iface = new AgentInterface("JSONRPC", "http://example.com:8080/api/", "/default");
        String url = Utils.buildBaseUrl(iface, "custom-tenant");
        assertEquals("http://example.com:8080/api/custom-tenant", url);
    }

    @Test
    void testBuildBaseUrl_urlWithPort() {
        AgentInterface iface = new AgentInterface("JSONRPC", "http://example.com:9999", "/tenant");
        String url = Utils.buildBaseUrl(iface, null);
        assertEquals("http://example.com:9999/tenant", url);
    }

    @Test
    void testBuildBaseUrl_httpsUrl() {
        AgentInterface iface = new AgentInterface("JSONRPC", "https://secure.example.com", "/tenant");
        String url = Utils.buildBaseUrl(iface, null);
        assertEquals("https://secure.example.com/tenant", url);
    }

    // ========== buildBaseUrl(String, String) Tests ==========

    @Test
    void testBuildBaseUrl_string_noTenant() {
        assertEquals("http://example.com", Utils.buildBaseUrl("http://example.com", null));
    }

    @Test
    void testBuildBaseUrl_string_trailingSlashStripped() {
        assertEquals("http://example.com", Utils.buildBaseUrl("http://example.com/", null));
    }

    @Test
    void testBuildBaseUrl_string_withTenant() {
        assertEquals("http://example.com/my-tenant", Utils.buildBaseUrl("http://example.com", "my-tenant"));
    }

    @Test
    void testBuildBaseUrl_string_withTenantLeadingSlash() {
        assertEquals("http://example.com/my-tenant", Utils.buildBaseUrl("http://example.com", "/my-tenant"));
    }

    @Test
    void testBuildBaseUrl_string_withSubPath() {
        assertEquals("http://example.com/spec03/my-tenant", Utils.buildBaseUrl("http://example.com/spec03", "my-tenant"));
    }

    @Test
    void testBuildBaseUrl_string_nullBaseUrl_throws() {
        assertThrows(IllegalArgumentException.class, () -> Utils.buildBaseUrl((String) null, null));
    }

    // ========== validateAbsoluteUrl Tests ==========

    @Test
    void testValidateAbsoluteUrl_valid() {
        assertDoesNotThrow(() -> Utils.validateAbsoluteUrl("http://example.com"));
        assertDoesNotThrow(() -> Utils.validateAbsoluteUrl("https://example.com/path"));
        assertDoesNotThrow(() -> Utils.validateAbsoluteUrl("http://example.com:8080/path"));
    }

    @Test
    void testValidateAbsoluteUrl_relative_throws() {
        assertThrows(URISyntaxException.class, () -> Utils.validateAbsoluteUrl("/relative/path"));
    }

    @Test
    void testValidateAbsoluteUrl_malformed_throws() {
        assertThrows(URISyntaxException.class, () -> Utils.validateAbsoluteUrl("not a url"));
    }

    // ========== buildCardUrl Tests ==========

    @Test
    void testBuildCardUrl_simple() {
        assertEquals("http://example.com/.well-known/agent-card.json",
                Utils.buildCardUrl("http://example.com", "/.well-known/agent-card.json"));
    }

    @Test
    void testBuildCardUrl_baseTrailingSlash() {
        assertEquals("http://example.com/.well-known/agent-card.json",
                Utils.buildCardUrl("http://example.com/", "/.well-known/agent-card.json"));
    }

    @Test
    void testBuildCardUrl_pathWithoutLeadingSlash() {
        assertEquals("http://example.com/.well-known/agent-card.json",
                Utils.buildCardUrl("http://example.com", ".well-known/agent-card.json"));
    }

    @Test
    void testBuildCardUrl_preservesSubPath() {
        assertEquals("http://example.com/spec03/.well-known/agent-card.json",
                Utils.buildCardUrl("http://example.com/spec03", "/.well-known/agent-card.json"));
    }

    @Test
    void testBuildCardUrl_noDoubleSlash() {
        assertEquals("http://example.com/custom/agent.json",
                Utils.buildCardUrl("http://example.com/", "/custom/agent.json"));
    }

    // ========== stripWellKnownSuffix Tests ==========

    @Test
    void testStripWellKnownSuffix_noSuffix() {
        assertEquals("http://example.com", Utils.stripWellKnownSuffix("http://example.com"));
    }

    @Test
    void testStripWellKnownSuffix_withSuffix() {
        assertEquals("http://example.com",
                Utils.stripWellKnownSuffix("http://example.com/.well-known/agent-card.json"));
    }

    @Test
    void testStripWellKnownSuffix_withSubPathAndSuffix() {
        assertEquals("http://example.com/spec03",
                Utils.stripWellKnownSuffix("http://example.com/spec03/.well-known/agent-card.json"));
    }

    @Test
    void testStripWellKnownSuffix_trailingSlash() {
        assertEquals("http://example.com", Utils.stripWellKnownSuffix("http://example.com/"));
    }

    @Test
    void testStripWellKnownSuffix_unrelatedPath() {
        assertEquals("http://example.com/custom/agent.json",
                Utils.stripWellKnownSuffix("http://example.com/custom/agent.json"));
    }
}
