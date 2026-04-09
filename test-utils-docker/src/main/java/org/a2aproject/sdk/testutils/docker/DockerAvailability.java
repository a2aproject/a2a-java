package org.a2aproject.sdk.testutils.docker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for checking Docker/Podman availability and test skip settings.
 */
public final class DockerAvailability {

    private static final Logger LOG = LoggerFactory.getLogger(DockerAvailability.class);
    private static final String SKIP_DOCKER_TESTS_PROPERTY = "skipDockerTests";

    private static volatile Boolean dockerAvailable = null;

    private DockerAvailability() {
        // Utility class
    }

    /**
     * Checks if Docker or Podman is available in the current environment.
     * The result is cached after the first check.
     *
     * @return true if Docker or Podman is available, false otherwise
     */
    public static boolean isDockerAvailable() {
        if (dockerAvailable == null) {
            synchronized (DockerAvailability.class) {
                if (dockerAvailable == null) {
                    dockerAvailable = checkDockerAvailable();
                }
            }
        }
        return dockerAvailable;
    }

    /**
     * Checks if Docker tests should be skipped based on the system property.
     * Tests are skipped when the system property "skipDockerTests" is set to "true".
     *
     * @return true if Docker tests should be skipped, false otherwise
     */
    public static boolean shouldSkipDockerTests() {
        return Boolean.parseBoolean(System.getProperty(SKIP_DOCKER_TESTS_PROPERTY, "false"));
    }

    private static boolean checkDockerAvailable() {
        // Try docker first, then podman
        if (tryContainerCommand("docker")) {
            LOG.info("Docker is available");
            return true;
        }

        if (tryContainerCommand("podman")) {
            LOG.info("Podman is available");
            return true;
        }

        LOG.warn("Neither Docker nor Podman is available");
        return false;
    }

    private static boolean tryContainerCommand(String command) {
        try {
            Process process = new ProcessBuilder(command, "info")
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();

            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                LOG.debug("Timeout waiting for '{} info' command, destroying process", command);
                process.destroyForcibly();
                return false;
            }

            int exitCode = process.exitValue();
            return exitCode == 0;
        } catch (IOException e) {
            LOG.debug("Failed to execute '{} info' command", command, e);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.debug("Interrupted while waiting for '{} info' command", command, e);
            return false;
        }
    }
}
