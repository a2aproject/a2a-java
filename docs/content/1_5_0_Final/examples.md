---
title: Examples
description: Hello World examples for the A2A Java SDK — server and client walkthroughs with transport selection and OpenTelemetry.
layout: page
---

# Examples

## Prerequisites

- Java 17 or higher
- Python 3.8 or higher
- [uv](https://github.com/astral-sh/uv) (recommended Python package installer)
- Git

## Hello World Server

This example runs a Java A2A server that a Python client can talk to.

### Start the Java Server

```bash
cd examples/helloworld/server
mvn quarkus:dev
```

### Transport Protocol Selection

Select the transport protocol via the `quarkus.agentcard.protocol` property:

```bash
# JSON-RPC (default)
mvn quarkus:dev

# gRPC
mvn quarkus:dev -Dquarkus.agentcard.protocol=GRPC

# HTTP+JSON/REST
mvn quarkus:dev -Dquarkus.agentcard.protocol=HTTP+JSON
```

You can also set the default in `src/main/resources/application.properties`:
```properties
quarkus.agentcard.protocol=HTTP+JSON
```

### Run the Python Client

The Python client is part of the [a2a-samples](https://github.com/a2aproject/a2a-samples) project:

```bash
git clone https://github.com/a2aproject/a2a-samples.git
cd a2a-samples/samples/python/agents/helloworld

uv venv
source .venv/bin/activate
uv pip install -e .
uv run test_client.py
```

The client connects to `http://localhost:9999`, fetches the agent card, sends a regular message, then sends the same message as a streaming request.

## Hello World Client

This example runs a Java A2A client that talks to a Python server.

### Start the Python Server

```bash
git clone https://github.com/a2aproject/a2a-samples.git
cd a2a-samples/samples/python/agents/helloworld

uv venv
source .venv/bin/activate
uv pip install -e .
uv run .
```

The server starts on `http://localhost:9999`. You can also use the [Java server example](#hello-world-server) instead.

### Run the Java Client

First build the SDK, then run the client:

```bash
# From the a2a-java root
mvn clean install

# Run the client
cd examples/helloworld/client
mvn exec:java
```

#### Transport Protocol Selection

```bash
# JSON-RPC (default)
mvn exec:java

# gRPC
mvn exec:java -Dquarkus.agentcard.protocol=GRPC

# HTTP+JSON/REST
mvn exec:java -Dquarkus.agentcard.protocol=HTTP+JSON
```

The protocol you select on the client must match the protocol configured on the server.

#### Using JBang

Alternatively, run the client with [JBang](https://www.jbang.dev/) (no Maven required):

```bash
jbang examples/helloworld/client/src/main/java/org/a2aproject/sdk/examples/helloworld/HelloWorldRunner.java
```

Pass transport and OpenTelemetry flags the same way:
```bash
jbang examples/helloworld/client/src/main/java/org/a2aproject/sdk/examples/helloworld/HelloWorldRunner.java \
    -Dquarkus.agentcard.protocol=GRPC -Dopentelemetry=true
```

## OpenTelemetry (Optional)

Both the server and client support distributed tracing with OpenTelemetry.

### Server with OpenTelemetry

```bash
cd examples/helloworld/server
mvn quarkus:dev -Popentelemetry
```

Quarkus Dev Services automatically starts a Grafana observability stack. Open Grafana at `http://localhost:3001` (credentials: admin/admin) and view traces in the "Explore" section using the Tempo data source.

### Client with OpenTelemetry

```bash
cd examples/helloworld/client
mvn exec:java -Dopentelemetry=true
```

The client expects an OpenTelemetry collector on port 5317. The easiest way is to run the Java server with `-Popentelemetry` (which starts the collector automatically), then run the client with `-Dopentelemetry=true` for end-to-end traces.

For more information, see the [OpenTelemetry extras module](../extra/opentelemetry).

## Stream Lifecycle Hook

This example demonstrates the `TaskStreamLifecycleHook` — a CDI-discoverable hook that observes stream lifecycle events and can close all active streams on demand.

The server implements a hook that closes all subscriber streams when 3 clients connect to the same task. The client creates 3 subscribers sequentially, sending messages while the first two are active. When the third subscriber connects, the hook fires and all streams close gracefully.

### Start the Server

```bash
cd examples/stream-lifecycle/server
mvn quarkus:dev
```

### Run the Client

```bash
cd examples/stream-lifecycle/client
mvn exec:java
```

The client logs each event received by each subscriber, showing events flowing to subscribers 1 and 2 before the hook triggers.

#### Transport Protocol Selection

```bash
# JSON-RPC (default)
mvn exec:java

# gRPC
mvn exec:java -Dquarkus.agentcard.protocol=GRPC

# HTTP+JSON/REST
mvn exec:java -Dquarkus.agentcard.protocol=HTTP+JSON
```

Select the same protocol on both server and client.

For implementation details, see the [Stream Lifecycle Hook section](server#6-stream-lifecycle-hook-optional) in the Server Guide.

## More Examples

- [a2a-samples repository](https://github.com/a2aproject/a2a-samples/tree/main/samples/java/agents) — Additional agent examples in Java and other languages
