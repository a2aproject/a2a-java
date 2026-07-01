# A2A Java SDK

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

<!-- markdownlint-disable no-inline-html -->

<html>
   <h2 align="center">
   <img src="https://raw.githubusercontent.com/google-a2a/A2A/refs/heads/main/docs/assets/a2a-logo-black.svg" width="256" alt="A2A Logo"/>
   </h2>
   <h3 align="center">A Java library that helps run agentic applications as A2A servers following the <a href="https://a2a-protocol.org/">Agent2Agent (A2A) Protocol</a>.</h3>
</html>

A multi-module Maven library providing client and server support for A2A agent communication over JSON-RPC, gRPC, and REST transports.

## Documentation

Full documentation is available at **[a2aproject.github.io/a2a-java](https://a2aproject.github.io/a2a-java/)**.

- [Server Guide](https://a2aproject.github.io/a2a-java/server) — Run your agentic Java application as an A2A server
- [Client Guide](https://a2aproject.github.io/a2a-java/client) — Communicate with any A2A-compliant agent
- [Community Articles](https://a2aproject.github.io/a2a-java/community) — Tutorials, blog posts, and videos
- [Announcements](https://a2aproject.github.io/a2a-java/announces) — Release announcements and project news
- [Contributing](https://a2aproject.github.io/a2a-java/contributing) — Developer guide for contributing

## Quick Start

Requires Java 17+.

Add the A2A Java SDK reference server for JSON-RPC to your Maven project:

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-reference-jsonrpc</artifactId>
    <!-- Use a released version from https://github.com/a2aproject/a2a-java/releases --> 
    <version>${org.a2aproject.sdk.version}</version>
</dependency>
```

See the [Server Guide](https://a2aproject.github.io/a2a-java/server) for full setup instructions.

## Build

```bash
mvn clean install
```

### Regeneration of gRPC files

We copy https://github.com/a2aproject/A2A/blob/main/specification/grpc/a2a.proto to the [`spec-grpc/`](./spec-grpc) project, and adjust the `java_package` option to be as follows:
```
option java_package = "org.a2aproject.sdk.grpc";
```
Then build the `spec-grpc` module with `mvn clean install -Dskip.protobuf.generate=false` to regenerate the gRPC classes in the `org.a2aproject.sdk.grpc` package.

## Examples

- [Hello World Server](examples/helloworld/server/README.md) — Java A2A server with a Python client
- [Hello World Client](examples/helloworld/client/README.md) — Java A2A client with a Python server
- [a2a-samples](https://github.com/a2aproject/a2a-samples/tree/main/samples/java/agents) — More agent examples

## Server Integrations

Community contributed integrations with various Java runtimes:

* **Quarkus** — This project contains the reference implementations for JSON-RPC, gRPC, and HTTP+JSON (REST) transports.
* **Jakarta EE** — [a2a-jakarta](https://github.com/wildfly-extras/a2a-jakarta) works with any runtime supporting the [Jakarta EE Web Profile](https://jakarta.ee/specifications/webprofile/).

To contribute an integration, see [CONTRIBUTING_INTEGRATIONS.md](CONTRIBUTING_INTEGRATIONS.md).

## Extras

See the [`extras`](./extras/README.md) folder for optional add-ons (OpenTelemetry, JPA-backed task store, Kafka-based replicated queue manager, Vert.x and Android HTTP clients).

## License

This project is licensed under the terms of the [Apache 2.0 License](LICENSE).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines.
