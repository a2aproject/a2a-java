package io.a2a.server.version;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import io.a2a.server.ServerCallContext;
import io.a2a.server.auth.UnauthenticatedUser;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.VersionNotSupportedError;
import org.junit.jupiter.api.Test;

public class A2AVersionValidatorTest {

    @Test
    public void testIsVersionCompatible_SameMajorMinor() {
        assertTrue(A2AVersionValidator.isVersionCompatible(List.of("1.0"), "1.0"));
    }

    @Test
    public void testIsVersionCompatible_SameMajorDifferentMinor() {
        // Major versions match, minor versions can differ
        assertTrue(A2AVersionValidator.isVersionCompatible(List.of("1.0"), "1.1"));
        assertTrue(A2AVersionValidator.isVersionCompatible(List.of("1.1"), "1.0"));
        assertTrue(A2AVersionValidator.isVersionCompatible(List.of("1.5"), "1.9"));
    }

    @Test
    public void testIsVersionCompatible_DifferentMajor() {
        // Major versions must match exactly
        assertFalse(A2AVersionValidator.isVersionCompatible(List.of("1.0"), "2.0"));
        assertFalse(A2AVersionValidator.isVersionCompatible(List.of("2.0"), "1.0"));
        assertFalse(A2AVersionValidator.isVersionCompatible(List.of("1.5"), "2.5"));
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
    public void testValidateProtocolVersion_NoVersionProvided_DefaultsTo1_0() {
        // When no version is provided, should default to 1.0 and succeed
        AgentCard agentCard = createAgentCard("1.0");
        ServerCallContext context = createContext(null);

        // Should not throw - defaults to 1.0 which matches
        assertDoesNotThrow(() -> A2AVersionValidator.validateProtocolVersion(agentCard, context));
    }

    @Test
    public void testValidateProtocolVersion_EmptyVersionProvided_DefaultsTo1_0() {
        // When empty version is provided, should default to 1.0 and succeed
        AgentCard agentCard = createAgentCard("1.0");
        ServerCallContext context = createContext("");

        // Should not throw - defaults to 1.0 which matches
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
    public void testValidateProtocolVersion_CompatibleMinorVersions() {
        // When major version matches but minor differs, should succeed
        AgentCard agentCard = createAgentCard("1.0");
        ServerCallContext context = createContext("1.1");

        // Should not throw - same major version
        assertDoesNotThrow(() -> A2AVersionValidator.validateProtocolVersion(agentCard, context));
    }

    @Test
    public void testValidateProtocolVersion_CompatibleMinorVersions_Reverse() {
        // When major version matches but minor differs (reverse), should succeed
        AgentCard agentCard = createAgentCard("1.1");
        ServerCallContext context = createContext("1.0");

        // Should not throw - same major version
        assertDoesNotThrow(() -> A2AVersionValidator.validateProtocolVersion(agentCard, context));
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
        return AgentCard.builder()
                .name("test-card")
                .description("Test card")
                .supportedInterfaces(List.of(new AgentInterface("GRPC", "http://localhost:9999", "", protocolVersion)))
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
