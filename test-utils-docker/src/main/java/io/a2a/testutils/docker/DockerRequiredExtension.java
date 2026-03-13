package io.a2a.testutils.docker;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.opentest4j.TestAbortedException;

/**
 * JUnit 5 extension that conditionally executes tests based on Docker/Podman availability.
 * <p>
 * Checks for both Docker and Podman container engines.
 * <p>
 * Behavior:
 * <ul>
 *   <li>If -DskipDockerTests=true: tests are always skipped (regardless of Docker availability)</li>
 *   <li>If -DskipDockerTests is NOT set and Docker/Podman is available: tests run normally</li>
 *   <li>If -DskipDockerTests is NOT set and neither is available: tests abort with clear error message</li>
 * </ul>
 */
public class DockerRequiredExtension implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        boolean skipDockerTests = DockerAvailability.shouldSkipDockerTests();

        // Check skip flag first - if set, always skip regardless of Docker availability
        if (skipDockerTests) {
            return ConditionEvaluationResult.disabled(
                    "Docker tests skipped due to -DskipDockerTests=true");
        }

        boolean dockerAvailable = DockerAvailability.isDockerAvailable();
        if (!dockerAvailable) {
            // Docker/Podman not available and skip NOT requested - ABORT test
            throw new TestAbortedException(
                    "Docker/Podman is not available. Use -DskipDockerTests=true to skip these tests.");
        }

        // Docker/Podman available and skip not requested - RUN test
        return ConditionEvaluationResult.enabled("Docker/Podman is available");
    }
}
