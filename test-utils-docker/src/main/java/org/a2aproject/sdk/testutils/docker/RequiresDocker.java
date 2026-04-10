package org.a2aproject.sdk.testutils.docker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Annotation to mark test classes that require Docker or Podman to be available.
 * <p>
 * Checks for both Docker and Podman container engines.
 * <p>
 * When neither Docker nor Podman is available:
 * <ul>
 *   <li>If -DskipDockerTests=true is set: tests are skipped</li>
 *   <li>If -DskipDockerTests is not set: tests fail with a clear error message</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * @QuarkusTest
 * @RequiresDocker
 * public class MyDockerTest {
 *     // Tests that require Docker or Podman
 * }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DockerRequiredExtension.class)
public @interface RequiresDocker {
}
