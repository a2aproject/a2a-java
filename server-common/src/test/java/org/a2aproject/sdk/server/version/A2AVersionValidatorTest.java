package org.a2aproject.sdk.server.version;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.UnauthenticatedUser;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.VersionNotSupportedError;
import org.junit.jupiter.api.Test;

public class A2AVersionValidatorTest {

    @Test
    public void testIsVersionCompatible_SameMajorMinor() {
        assertTrue(A2AVersionValidator.isVersionCompatible(List.of("1.0"), "1.0"));
    }

    @Test
    public void testIsVersionCompatible_SameMajorDifferentMinor() {
        // A server interface supports only the exact advertised major/minor version.
        assertFalse(A2AVersionValidator.isVersionCompatible(List.of("1.0"), "1.1"));
        assertFalse(A2AVersionValidator.isVersionCompatible(List.of("1.1"), "1.0"));
        assertFalse(A2AVersionValidator.isVersionCompatible(List.of("1.5"), "1.9"));
    }

    @Test
    public void testIsVersionCompatible_DifferentMajor() {
        // Major versions must match exactly
        assertFalse(A2AVersionValidator.isVersionCompatible(List.of("1.0"), "2.0"));
        assertFalse(A2AVersionValidator.isVersionCompatible(List.of("2.0"), "1.0"));
        assertFalse(A2AVersionValidator.isVersionCompatible(List.of("1.5"), "2.5"));
    }

    @Test
    public void testIsVersionCompatible_PatchVersionDoesNotAffectCompatibility() {
        assertTrue(A2AVersionValidator.isVersionCompatible(List.of("1.0"), "1.0.7"));
        assertTrue(A2AVersionValidator.isVersionCompatible(List.of("1.0.3"), "1.0"));
        assertFalse(A2AVersionValidator.isVersionCompatible(List.of("1.0"), "1.1.7"));
    }

    @Test
    public void testIsVersionCompatible_InvalidFormat() {
        // Invalid version formats should return false
        assertFalse(A2AVersionValidator.isVersionCompatible(List.of("1.0"), "invalid"));
        assertFalse(A2AVersionValidator.isVersionCompatible(List.of("invalid"), "1.0"));
        assertFalse(A2AVersionValidator.isVersionCompatible(List.of("1"), "1.0"));
        assertFalse(A2AVersionValidator.isVersionCompatible(List.of("1.0"), ""));
        assertFalse(A2AVersionValidator.isVersionCompatible(List.of(""), "1.0"));
        assertFalse(A2AVersionValidator.isVersionCompatible(List.of("1.0"), null));
        assertFalse(A2AVersionValidator.isVersionCompatible(null, "1.0"));
    }

    @Test
    public void testValidateProtocolVersion_NoVersionProvided_DefaultsTo0_3() {
        // Per spec Section 3.6.2: missing A2A-Version defaults to 0.3
        // A v1.0-only server should reject this
        AgentCard agentCard = createAgentCard("1.0");
        ServerCallContext context = createContext(null);

        VersionNotSupportedError error = assertThrows(VersionNotSupportedError.class,
                () -> A2AVersionValidator.validateProtocolVersion(agentCard, context));
        assertTrue(error.getMessage().contains("0.3"));
        assertTrue(error.getMessage().contains("not supported"));
    }

    @Test
    public void testValidateProtocolVersion_EmptyVersionProvided_DefaultsTo0_3() {
        // Per spec Section 3.6.2: empty A2A-Version defaults to 0.3
        // A v1.0-only server should reject this
        AgentCard agentCard = createAgentCard("1.0");
        ServerCallContext context = createContext("");

        VersionNotSupportedError error = assertThrows(VersionNotSupportedError.class,
                () -> A2AVersionValidator.validateProtocolVersion(agentCard, context));
        assertTrue(error.getMessage().contains("0.3"));
        assertTrue(error.getMessage().contains("not supported"));
    }

    @Test
    public void testValidateProtocolVersion_NoVersionProvided_MultiVersionServer() {
        // A server supporting both 1.0 and 0.3 should accept missing version (defaults to 0.3)
        AgentCard agentCard = createAgentCardWithVersions("1.0", "0.3");
        ServerCallContext context = createContext(null);

        assertDoesNotThrow(() -> A2AVersionValidator.validateProtocolVersion(agentCard, context));
    }

    @Test
    public void testValidateProtocolVersion_NoVersionProvided_V03OnlyServer() {
        // A v0.3-only server should accept missing version (defaults to 0.3)
        AgentCard agentCard = createAgentCard("0.3");
        ServerCallContext context = createContext(null);

        assertDoesNotThrow(() -> A2AVersionValidator.validateProtocolVersion(agentCard, context));
    }

    @Test
    public void testValidateProtocolVersion_EmptyVersionProvided_V03OnlyServer() {
        AgentCard agentCard = createAgentCard("0.3");
        ServerCallContext context = createContext("");

        assertDoesNotThrow(() -> A2AVersionValidator.validateProtocolVersion(agentCard, context));
    }

    @Test
    public void testValidateProtocolVersion_MatchingVersion() {
        // When version matches exactly, should succeed
        AgentCard agentCard = createAgentCard("1.0");
        ServerCallContext context = createContext("1.0");

        // Should not throw - versions match
        assertDoesNotThrow(() -> A2AVersionValidator.validateProtocolVersion(agentCard, context));
    }

    @Test
    public void testValidateProtocolVersion_PatchVersionDoesNotAffectCompatibility() {
        AgentCard agentCard = createAgentCard("1.0");
        ServerCallContext context = createContext("1.0.7");

        assertDoesNotThrow(() -> A2AVersionValidator.validateProtocolVersion(agentCard, context));
    }

    @Test
    public void testValidateProtocolVersion_MultipleSupportedMinorVersions() {
        AgentCard agentCard = createAgentCardWithVersions("1.0", "1.1");

        assertDoesNotThrow(() -> A2AVersionValidator.validateProtocolVersion(agentCard, createContext("1.1")));
        assertThrows(VersionNotSupportedError.class,
                () -> A2AVersionValidator.validateProtocolVersion(agentCard, createContext("1.2")));
    }

    @Test
    public void testValidateProtocolVersion_UnsupportedMinorVersion() {
        // A matching major is insufficient when the server advertises a different minor.
        AgentCard agentCard = createAgentCard("1.0");
        ServerCallContext context = createContext("1.1");

        assertThrows(VersionNotSupportedError.class,
                () -> A2AVersionValidator.validateProtocolVersion(agentCard, context));
    }

    @Test
    public void testValidateProtocolVersion_UnsupportedMinorVersion_Reverse() {
        // The server advertising 1.1 does not support a request for 1.0 unless it lists both.
        AgentCard agentCard = createAgentCard("1.1");
        ServerCallContext context = createContext("1.0");

        assertThrows(VersionNotSupportedError.class,
                () -> A2AVersionValidator.validateProtocolVersion(agentCard, context));
    }

    @Test
    public void testValidateProtocolVersion_IncompatibleMajorVersion() {
        // When major version differs, should throw VersionNotSupportedError
        AgentCard agentCard = createAgentCard("1.0");
        ServerCallContext context = createContext("2.0");

        VersionNotSupportedError error = assertThrows(VersionNotSupportedError.class,
                () -> A2AVersionValidator.validateProtocolVersion(agentCard, context));

        assertTrue(error.getMessage().contains("2.0"));
        assertTrue(error.getMessage().contains("1.0"));
        assertTrue(error.getMessage().contains("not supported"));
    }

    @Test
    public void testValidateProtocolVersion_IncompatibleMajorVersion_Reverse() {
        // When major version differs (reverse), should throw VersionNotSupportedError
        AgentCard agentCard = createAgentCard("2.0");
        ServerCallContext context = createContext("1.0");

        VersionNotSupportedError error = assertThrows(VersionNotSupportedError.class,
                () -> A2AVersionValidator.validateProtocolVersion(agentCard, context));

        assertTrue(error.getMessage().contains("1.0"));
        assertTrue(error.getMessage().contains("2.0"));
        assertTrue(error.getMessage().contains("not supported"));
    }

    @Test
    public void testValidateProtocolVersion_InvalidVersionFormat() {
        // When invalid version is provided, should throw VersionNotSupportedError
        AgentCard agentCard = createAgentCard("1.0");
        ServerCallContext context = createContext("invalid");

        VersionNotSupportedError error = assertThrows(VersionNotSupportedError.class,
                () -> A2AVersionValidator.validateProtocolVersion(agentCard, context));

        assertTrue(error.getMessage().contains("invalid"));
        assertTrue(error.getMessage().contains("not supported"));
    }

    private AgentCard createAgentCard(String protocolVersion) {
        return createAgentCardWithVersions(protocolVersion);
    }

    private AgentCard createAgentCardWithVersions(String... protocolVersions) {
        List<AgentInterface> interfaces = new java.util.ArrayList<>();
        for (String version : protocolVersions) {
            interfaces.add(new AgentInterface("GRPC", "http://localhost:9999", "", version));
        }
        return AgentCard.builder()
                .name("test-card")
                .description("Test card")
                .supportedInterfaces(interfaces)
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(false)
                        .pushNotifications(false)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(Collections.emptyList())
                .build();
    }

    private ServerCallContext createContext(String requestedProtocolVersion) {
        return new ServerCallContext(
                UnauthenticatedUser.INSTANCE,
                Collections.emptyMap(),
                new HashSet<>(),
                requestedProtocolVersion
        );
    }
}
