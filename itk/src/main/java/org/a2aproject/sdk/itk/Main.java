package org.a2aproject.sdk.itk;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        String httpPort = "10102";
        String grpcPort = "11002";

        for (int i = 0; i < args.length; i++) {
            if ("--httpPort".equals(args[i]) && i + 1 < args.length) {
                httpPort = args[++i];
            } else if ("--grpcPort".equals(args[i]) && i + 1 < args.length) {
                grpcPort = args[++i];
            }
        }

        Path jarPath = Path.of("target", "quarkus-app", "quarkus-run.jar");
        if (!jarPath.toFile().exists()) {
            System.err.println("quarkus-run.jar not found at " + jarPath.toAbsolutePath());
            System.exit(1);
        }

        List<String> command = new ArrayList<>();
        command.add(ProcessHandle.current().info().command().orElse("java"));
        command.add("-Dquarkus.http.port=" + httpPort);
        command.add("-Dquarkus.grpc.server.port=" + grpcPort);
        command.add("-jar");
        command.add(jarPath.toString());

        Process process = new ProcessBuilder(command)
                .inheritIO()
                .directory(new File("."))
                .start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> process.destroyForcibly()));

        System.exit(process.waitFor());
    }
}
