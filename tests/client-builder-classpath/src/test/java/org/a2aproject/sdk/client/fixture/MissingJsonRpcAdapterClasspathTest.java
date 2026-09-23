package org.a2aproject.sdk.client.fixture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class MissingJsonRpcAdapterClasspathTest {

    @Test
    void reportsMissingJsonRpcAdapterWithCoreAdapterOnClasspath() throws Exception {
        String[] entries = System.getProperty("java.class.path").split(java.util.regex.Pattern.quote(File.pathSeparator));
        assertTrue(Arrays.stream(entries).noneMatch(entry -> entry.contains("compat-0.3-client-adapter-jsonrpc")),
                () -> "The fixture must not depend on the versioned JSON-RPC adapter: " + Arrays.toString(entries));

        Path probeDirectory = Files.createTempDirectory("a2a-client-builder-probe");
        Path probeClass = probeDirectory.resolve("org/a2aproject/sdk/client/fixture/MissingJsonRpcAdapterProbe.class");
        Files.createDirectories(probeClass.getParent());
        Files.copy(Path.of("target/test-classes/org/a2aproject/sdk/client/fixture/MissingJsonRpcAdapterProbe.class"),
                probeClass);

        String productionClassPath = Arrays.stream(entries)
                .filter(entry -> !entry.endsWith("target/test-classes"))
                .collect(Collectors.joining(File.pathSeparator));
        Process process = null;
        try {
            process = new ProcessBuilder(
                    Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                    "-cp", probeDirectory + File.pathSeparator + productionClassPath,
                    MissingJsonRpcAdapterProbe.class.getName())
                    .redirectErrorStream(true)
                    .start();
            String output;
            try (var input = process.getInputStream()) {
                output = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }

            assertEquals(0, process.waitFor(), output);
            assertTrue(output.contains("a2a-java-sdk-compat-0.3-client-adapter-jsonrpc"), output);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            try (var paths = Files.walk(probeDirectory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }
}
