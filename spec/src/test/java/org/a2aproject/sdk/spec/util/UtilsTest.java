package org.a2aproject.sdk.spec.util;

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
        assertEquals("Tenant contains invalid characters. Only a-zA-Z0-9_-. are allowed", ex.getMessage());
    }

    @Test
    void testValidateTenant_pathTraversalWithSlash_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            AgentInterface iface = new AgentInterface("JSONRPC", "http://example.com", "");
            Utils.buildBaseUrl(iface, "/../admin");
        });
        assertNotNull(ex.getMessage());
        assertEquals("Tenant contains invalid characters. Only a-zA-Z0-9_-. are allowed", ex.getMessage());
    }

    @Test
    void testValidateTenant_tooLong_throws() {
        String longTenant = "a".repeat(257);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            AgentInterface iface = new AgentInterface("JSONRPC", "http://example.com", "");
            Utils.buildBaseUrl(iface, longTenant);
        });
        assertNotNull(ex.getMessage());
        assertEquals("Tenant exceeds maximum length of 256 characters", ex.getMessage());
    }

    @Test
    void testValidateTenant_maxLengthAllowed_succeeds() {
        AgentInterface iface = new AgentInterface("JSONRPC", "http://example.com", "");
        String maxTenant = "a".repeat(256);
        String url = Utils.buildBaseUrl(iface, maxTenant);
        assertNotNull(url);
        assertEquals("http://example.com/" + "a".repeat(256), url);
    }

    @Test
    void testValidateTenant_invalidCharactersSpace_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            AgentInterface iface = new AgentInterface("JSONRPC", "http://example.com", "");
            Utils.buildBaseUrl(iface, "tenant with spaces");
        });
        assertNotNull(ex.getMessage());
        assertEquals("Tenant contains invalid characters. Only a-zA-Z0-9_-. are allowed", ex.getMessage());
    }

    @Test
    void testValidateTenant_invalidCharactersSpecial_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            AgentInterface iface = new AgentInterface("JSONRPC", "http://example.com", "");
            Utils.buildBaseUrl(iface, "tenant@123");
        });
        assertNotNull(ex.getMessage());
        assertEquals("Tenant contains invalid characters. Only a-zA-Z0-9_-. are allowed", ex.getMessage());
    }

    @Test
    void testValidateTenant_invalidCharactersQuery_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            AgentInterface iface = new AgentInterface("JSONRPC", "http://example.com", "");
            Utils.buildBaseUrl(iface, "tenant?param=value");
        });
        assertNotNull(ex.getMessage());
        assertEquals("Tenant contains invalid characters. Only a-zA-Z0-9_-. are allowed", ex.getMessage());
    }

    @Test
    void testValidateTenant_slashInTenant_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            AgentInterface iface = new AgentInterface("JSONRPC", "http://example.com", "");
            Utils.buildBaseUrl(iface, "multi/level/tenant");
        });
        assertNotNull(ex.getMessage());
        assertEquals("Tenant contains invalid characters. Only a-zA-Z0-9_-. are allowed", ex.getMessage());
    }

    @Test
    void testValidateTenant_leadingSlashMultiSegment_throws() {
        assertThrows(IllegalArgumentException.class, () -> {
            Utils.validateTenant("/multi/level");
        });
    }

    @Test
    void testValidateTenant_intermediateSegments_throws() {
        assertThrows(IllegalArgumentException.class, () -> {
            Utils.validateTenant("tenant/api/v1");
        });
    }

    @Test
    void testValidateTenant_trailingSlashMultiSegment_throws() {
        assertThrows(IllegalArgumentException.class, () -> {
            Utils.validateTenant("multi/level/");
        });
    }

    @Test
    void testValidateTenant_validCharacters_succeeds() {
        AgentInterface iface = new AgentInterface("JSONRPC", "http://example.com", "");
        String url1 = Utils.buildBaseUrl(iface, "tenant-name");
        assertEquals("http://example.com/tenant-name", url1);

        String url2 = Utils.buildBaseUrl(iface, "tenant_name");
        assertEquals("http://example.com/tenant_name", url2);

        String url3 = Utils.buildBaseUrl(iface, "Tenant123");
        assertEquals("http://example.com/Tenant123", url3);

        String url4 = Utils.buildBaseUrl(iface, "tenant.v1");
        assertEquals("http://example.com/tenant.v1", url4);

        String url5 = Utils.buildBaseUrl(iface, ".well-known");
        assertEquals("http://example.com/.well-known", url5);
    }

    @Test
    void testValidateTenant_emptyString_succeeds() {
        AgentInterface iface = new AgentInterface("JSONRPC", "http://example.com", "");
        String url = Utils.buildBaseUrl(iface, "");
        assertEquals("http://example.com", url);
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

    // ========== Tenant deduplication Tests ==========

    @Test
    void testBuildBaseUrl_urlAlreadyContainsTenant_noDoubling() {
        AgentInterface iface = new AgentInterface("HTTP+JSON", "http://example.com/acme");
        String url = Utils.buildBaseUrl(iface, "acme");
        assertEquals("http://example.com/acme", url);
    }

    @Test
    void testBuildBaseUrl_urlAlreadyContainsTenantWithSlash_noDoubling() {
        AgentInterface iface = new AgentInterface("HTTP+JSON", "http://example.com/acme");
        String url = Utils.buildBaseUrl(iface, "/acme");
        assertEquals("http://example.com/acme", url);
    }

    @Test
    void testBuildBaseUrl_urlAlreadyContainsTenantWithTrailingSlash_noDoubling() {
        AgentInterface iface = new AgentInterface("HTTP+JSON", "http://example.com/acme/");
        String url = Utils.buildBaseUrl(iface, "acme");
        assertEquals("http://example.com/acme", url);
    }

    @Test
    void testBuildBaseUrl_urlContainsDifferentTenant_appends() {
        AgentInterface iface = new AgentInterface("HTTP+JSON", "http://example.com/acme");
        String url = Utils.buildBaseUrl(iface, "beta");
        assertEquals("http://example.com/acme/beta", url);
    }

    @Test
    void testBuildBaseUrl_tenantSubstringOfPathSegment_noFalsePositive() {
        // Tenant "/lic" must not match the end of "/public" — extractTenant normalizes
        // the tenant to start with "/" so endsWith checks a full segment boundary.
        AgentInterface iface = new AgentInterface("HTTP+JSON", "http://example.com/public");
        String url = Utils.buildBaseUrl(iface, "lic");
        assertEquals("http://example.com/public/lic", url);
    }

    @Test
    void testBuildBaseUrl_string_urlAlreadyContainsTenant_noDoubling() {
        assertEquals("http://example.com/acme", Utils.buildBaseUrl("http://example.com/acme", "acme"));
    }

    @Test
    void testBuildBaseUrl_string_urlAlreadyContainsTenantWithTrailingSlash_noDoubling() {
        assertEquals("http://example.com/acme", Utils.buildBaseUrl("http://example.com/acme/", "acme"));
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

    @Test
    void testStripWellKnownSuffix_tenantSpecific() {
        assertEquals("http://example.com",
                Utils.stripWellKnownSuffix("http://example.com/.well-known/acme/agent-card.json"));
    }

    @Test
    void testStripWellKnownSuffix_tenantSpecificWithSubPath() {
        assertEquals("http://example.com/spec03",
                Utils.stripWellKnownSuffix("http://example.com/spec03/.well-known/acme/agent-card.json"));
    }

    @Test
    void testStripWellKnownSuffix_tenantSpecificWithTrailingSlash() {
        assertEquals("http://example.com",
                Utils.stripWellKnownSuffix("http://example.com/.well-known/acme/agent-card.json/"));
    }

    @Test
    void testStripWellKnownSuffix_multiLevelTenantNotStripped() {
        // Multi-level tenant paths (containing /) are not stripped
        assertEquals("http://example.com/.well-known/org/team/agent-card.json",
                Utils.stripWellKnownSuffix("http://example.com/.well-known/org/team/agent-card.json"));
    }

    @Test
    void testStripWellKnownSuffix_invalidTenantCharsNotStripped() {
        // Tenant with characters outside [a-zA-Z0-9_.-] must not be stripped
        assertEquals("http://example.com/.well-known/invalid tenant/agent-card.json",
                Utils.stripWellKnownSuffix("http://example.com/.well-known/invalid tenant/agent-card.json"));
        assertEquals("http://example.com/.well-known/bad@tenant/agent-card.json",
                Utils.stripWellKnownSuffix("http://example.com/.well-known/bad@tenant/agent-card.json"));
    }

    // -------------------------------------------------------------------------
    // buildTenantCardPath
    // -------------------------------------------------------------------------

    @Test
    void testBuildTenantCardPath_simple() {
        assertEquals("/.well-known/acme/agent-card.json", Utils.buildTenantCardPath("acme"));
    }

    @Test
    void testBuildTenantCardPath_stripsLeadingAndTrailingSlashes() {
        assertEquals("/.well-known/acme/agent-card.json", Utils.buildTenantCardPath("/acme/"));
    }
}
