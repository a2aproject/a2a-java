
///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS io.github.a2asdk:a2a-java-sdk-client:1.0.0.Alpha3
//DEPS io.github.a2asdk:a2a-java-sdk-client-transport-jsonrpc:1.0.0.Alpha3
//DEPS io.github.a2asdk:a2a-java-sdk-client-transport-grpc:1.0.0.Alpha3
//DEPS io.github.a2asdk:a2a-java-sdk-client-transport-rest:1.0.0.Alpha3
//DEPS io.github.a2asdk:a2a-java-sdk-opentelemetry-client:1.0.0.Alpha3
//DEPS io.github.a2asdk:a2a-java-sdk-opentelemetry-client-propagation:1.0.0.Alpha3
//DEPS io.opentelemetry:opentelemetry-sdk:1.55.0
//DEPS io.opentelemetry:opentelemetry-exporter-otlp:1.55.0
//DEPS io.opentelemetry:opentelemetry-exporter-logging:1.55.0
//DEPS io.grpc:grpc-netty:1.77.0
//SOURCES HelloWorldClient.java

/**
 * JBang script to run the A2A HelloWorldClient example.
 * This script automatically handles the dependencies and runs the client.
 * 
 * Prerequisites:
 * - JBang installed (see https://www.jbang.dev/documentation/guide/latest/installation.html)
 * - A running A2A server (see README.md for instructions on setting up the Python server)
 * 
 * Usage: 
 * $ jbang HelloWorldRunner.java
 * 
 * The script will communicate with the A2A server at http://localhost:9999
 */
public class HelloWorldRunner {

    public static void main(String[] args) {
        for (String arg : args) {
            if (arg != null && arg.startsWith("-D")) {
                int index = arg.indexOf('=');
                if (index > 0) {
                    System.setProperty(arg.substring(2, index), arg.substring(index + 1));
                }
            }
        }
        io.a2a.examples.helloworld.HelloWorldClient.main(args);
    }
}
