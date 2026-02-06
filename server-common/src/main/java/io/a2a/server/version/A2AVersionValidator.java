package io.a2a.server.version;

import java.util.List;
import java.util.stream.Collectors;

import io.a2a.server.ServerCallContext;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.VersionNotSupportedError;

/**
 * Utility class for validating A2A protocol version compatibility between clients and agents.
 *
 * <p>Version validation follows semantic versioning rules:
 * <ul>
 *   <li>Major versions must match exactly (1.x can only talk to 1.x)</li>
 *   <li>Minor versions are compatible (1.0 client can talk to 1.1 server and vice versa)</li>
 * </ul>
 *
 * <p>If the client does not specify a version, the current protocol version
 * ({@link AgentInterface#CURRENT_PROTOCOL_VERSION}) is assumed as the default.
 */
public class A2AVersionValidator {

    /**
     * Validates that the client's requested protocol version is compatible with the agent's
     * supported versions across all interfaces.
     *
     * @param agentCard the agent card containing the supported interfaces with their protocol versions
     * @param context the server call context containing the requested protocol version
     * @throws VersionNotSupportedError if the versions are incompatible
     */
    public static void validateProtocolVersion(AgentCard agentCard, ServerCallContext context)
            throws VersionNotSupportedError {
        String requestedVersion = context.getRequestedProtocolVersion();

        // If client didn't specify a version, default to current version
        if (requestedVersion == null || requestedVersion.trim().isEmpty()) {
            requestedVersion = AgentInterface.CURRENT_PROTOCOL_VERSION;
        }

        // Collect all unique protocol versions from all supported interfaces
        List<String> supportedVersions = agentCard.supportedInterfaces().stream()
                .map(AgentInterface::protocolVersion)
                .distinct()
                .collect(Collectors.toList());

        if (!isVersionCompatible(supportedVersions, requestedVersion)) {
            throw new VersionNotSupportedError(
                null,
                "Protocol version '" + requestedVersion + "' is not supported. " +
                "Supported versions: " + supportedVersions,
                null);
        }
    }

    /**
     * Checks if the requested version is compatible with the supported version.
     *
     * <p>Compatibility rules:
     * <ul>
     *   <li>Major versions must match exactly</li>
     *   <li>Minor versions are compatible (any x.Y works with x.Z)</li>
     * </ul>
     *
     * @param supportedVersions the version supported by the agent (e.g., ["1.0", "1.1"])
     * @param requestedVersion the version requested by the client (e.g., "1.1")
     * @return true if versions are compatible, false otherwise
     */
    static boolean isVersionCompatible(List<String> supportedVersions, String requestedVersion) {
        if (supportedVersions == null) {
            return false;
        }
        for (String supportedVersion : supportedVersions) {
            try {
                VersionParts supportedParts = parseVersion(supportedVersion);
                VersionParts requestedParts = parseVersion(requestedVersion);

                // Major versions must match exactly
                if (supportedParts.major == requestedParts.major) {
                    return true;
                }
                // Minor versions are compatible - any 1.x can talk to any 1.y
            } catch (IllegalArgumentException e) {
                // If we can't parse the version, consider it incompatible
                return false;
            }
        }
        return false;
    }

    /**
     * Parses a version string into major and minor components.
     *
     * @param version the version string (e.g., "1.0")
     * @return the parsed version parts
     * @throws IllegalArgumentException if the version format is invalid
     */
    private static VersionParts parseVersion(String version) {
        if (version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("Version cannot be null or empty");
        }

        String[] parts = version.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Version must have at least major.minor format: " + version);
        }

        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            return new VersionParts(major, minor);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid version format: " + version, e);
        }
    }

    /**
     * Simple record to hold version components.
     */
    private record VersionParts(int major, int minor) {}
}
