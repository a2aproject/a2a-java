# A2A Hello World Example

This example demonstrates how to use the A2A Java SDK to communicate with an A2A server. The example includes a Java client that sends both regular and streaming messages to a Python A2A server.

## Prerequisites

- Java 11 or higher
- [JBang](https://www.jbang.dev/documentation/guide/latest/installation.html) (see [INSTALL_JBANG.md](INSTALL_JBANG.md) for quick installation instructions)
- Python 3.8 or higher
- [uv](https://github.com/astral-sh/uv) (recommended) or pip
- Git

## Setup and Run the Python A2A Server

The Python A2A server is part of the [a2a-samples](https://github.com/google-a2a/a2a-samples) project. To set it up and run it:

1. Clone the a2a-samples repository:
   ```bash
   git clone https://github.com/google-a2a/a2a-samples.git
   cd a2a-samples/samples/python/agents/helloworld
   ```

2. **Recommended method**: Install dependencies using uv (much faster Python package installer):
   ```bash
   # Install uv if you don't have it already
   # On macOS and Linux
   curl -LsSf https://astral.sh/uv/install.sh | sh
   # On Windows
   powershell -c "irm https://astral.sh/uv/install.ps1 | iex"

   # Install the package using uv
   uv venv
   source .venv/bin/activate  # On Windows: .venv\Scripts\activate
   uv pip install -e .
   ```

4. Run the server with uv (recommended):
   ```bash
   uv run .
   ```

The server will start running on `http://localhost:9999`.

### Using the Java Server Instead

Alternatively, you can use the Java server example instead of the Python server. The Java server supports multiple transport protocols (JSONRPC, GRPC, and HTTP+JSON). See the [server README](../server/README.md) for details on starting the Java server with different transport protocols.

## Run the Java A2A Client

The Java client can be run using either Maven or JBang.

### Build the A2A Java SDK

First, ensure you have built the `a2a-java` project:

```bash
cd /path/to/a2a-java
mvn clean install
```

### Option 1: Using Maven (Recommended)

Run the client using Maven's exec plugin:

```bash
cd examples/helloworld/client
mvn exec:java
```

#### Transport Protocol Selection

The client supports multiple transport protocols. You can select which protocol to use via the `quarkus.agentcard.protocol` property:

**Using JSONRPC (default)**:
```bash
mvn exec:java
```

**Using GRPC**:
```bash
mvn exec:java -Dquarkus.agentcard.protocol=GRPC
```

**Using HTTP+JSON**:
```bash
mvn exec:java -Dquarkus.agentcard.protocol=HTTP+JSON
```

Available protocols:
- `JSONRPC` - Uses JSON-RPC for communication (default)
- `GRPC` - Uses gRPC for communication
- `HTTP+JSON` - Uses HTTP with JSON payloads

**Note**: The protocol you select on the client must match the protocol configured on the server.

#### Enabling OpenTelemetry

To enable OpenTelemetry with Maven:
```bash
mvn exec:java -Dopentelemetry=true
```

You can combine protocol selection with OpenTelemetry:
```bash
mvn exec:java -Dquarkus.agentcard.protocol=HTTP+JSON -Dopentelemetry=true
```

### Option 2: Using JBang

A JBang script is provided for running the client without Maven:

1. Make sure you have JBang installed. If not, follow the [JBang installation guide](https://www.jbang.dev/documentation/guide/latest/installation.html).

3. Run the client using the JBang script:
   ```bash
   jbang examples/helloworld/client/src/main/java/io/a2a/examples/helloworld/HelloWorldRunner.java
   ```

#### Transport Protocol Selection with JBang

Select the transport protocol using the same `-Dquarkus.agentcard.protocol` property:

**Using JSONRPC (default)**:
```bash
jbang examples/helloworld/client/src/main/java/io/a2a/examples/helloworld/HelloWorldRunner.java
```

**Using GRPC**:
```bash
jbang examples/helloworld/client/src/main/java/io/a2a/examples/helloworld/HelloWorldRunner.java -Dquarkus.agentcard.protocol=GRPC 
```

**Using HTTP+JSON**:
```bash
jbang examples/helloworld/client/src/main/java/io/a2a/examples/helloworld/HelloWorldRunner.java -Dquarkus.agentcard.protocol=HTTP+JSON
```

#### Enabling OpenTelemetry with JBang

To enable OpenTelemetry with JBang:
```bash
jbang examples/helloworld/client/src/main/java/io/a2a/examples/helloworld/HelloWorldRunner.java -Dopentelemetry=true
```

You can combine protocol selection with OpenTelemetry:
```bash
jbang examples/helloworld/client/src/main/java/io/a2a/examples/helloworld/HelloWorldRunner.java -Dquarkus.agentcard.protocol=GRPC -Dopentelemetry=true
```

## What the Example Does

The Java client (`HelloWorldClient.java`) performs the following actions:

1. Fetches the server's public agent card
2. Fetches the server's extended agent card
3. Creates a client using the extended agent card that connects to the Python server at `http://localhost:9999`.
4. Sends a regular message asking "how much is 10 USD in INR?".
5. Prints the server's response.
6. Sends the same message as a streaming request.
7. Prints each chunk of the server's streaming response as it arrives.

## Enable OpenTelemetry (Optional)

The client includes support for distributed tracing with OpenTelemetry. To enable it:

### Prerequisites

**IMPORTANT**: The client expects an OpenTelemetry collector to be ready and accepting traces. You have two options:

#### Option 1: Use the Java Server Example (Recommended)

Instead of the Python server, use the Java server example which has built-in OpenTelemetry support:

1. **Start the Java server with OpenTelemetry enabled**:
   ```bash
   mvn quarkus:dev -Popentelemetry -pl examples/helloworld/server/ -Dquarkus.agentcard.protocol=HTTP+JSON
   ```
   This will:
   - Start the server at `http://localhost:9999`
   - Launch Grafana at `http://localhost:3001`
   - Start OTLP collectors on ports 5317 (gRPC) and 5318 (HTTP)

2. **Run the client with OpenTelemetry**:

   Using Maven (from `examples/helloworld/client`):
   ```bash
   mvn exec:java -Dopentelemetry=true
   ```

   With specific protocol:
   ```bash
   mvn exec:java -Dquarkus.agentcard.protocol=HTTP+JSON -Dopentelemetry=true
   ```

   Or using JBang:
   ```bash
   jbang examples/helloworld/client/src/main/java/io/a2a/examples/helloworld/HelloWorldRunner.java -Dopentelemetry=true 
   ```

   With specific protocol:
   ```bash
   jbang examples/helloworld/client/src/main/java/io/a2a/examples/helloworld/HelloWorldRunner.java -Dquarkus.agentcard.protocol=HTTP+JSON -Dopentelemetry=true
   ```

3. **View traces in Grafana**:
   - Open `http://localhost:3001` (credentials: admin/admin)
   - Go to "Explore" → select "Tempo" data source
   - View distributed traces showing the full request flow from client to server

#### Option 2: Use External OpenTelemetry Collector

If you want to use the Python server with OpenTelemetry:

1. **Start an OpenTelemetry collector** on port 5317 (e.g., using Docker):
   ```bash
   docker run -p 5317:4317 otel/opentelemetry-collector
   ```

2. **Run the Python server** 

3. **Run the client with OpenTelemetry**:
   ```bash
   mvn exec:java -Dopentelemetry=true
   ```

   Or with JBang:
   ```bash
   jbang examples/helloworld/client/src/main/java/io/a2a/examples/helloworld/HelloWorldRunner.java -Dopentelemetry=true
   ```

   With specific protocol:
   ```bash
   mvn exec:java -Dquarkus.agentcard.protocol=HTTP+JSON -Dopentelemetry=true
   ```

### What Gets Traced

When OpenTelemetry is enabled, the client traces:
- Agent card fetching (public and extended)
- Message sending (blocking and streaming)
- Task operations (get, cancel, list)
- Push notification configuration operations
- Connection and transport layer operations

Client traces are automatically linked with server traces (when using the Java server), providing end-to-end visibility of the entire A2A protocol flow.

### Configuration

The client is configured to send traces to `http://localhost:5317` (OTLP gRPC endpoint). To use a different endpoint, modify the `initOpenTelemetry()` method in `HelloWorldClient.java`:

```java
OtlpGrpcSpanExporter.builder()
    .setEndpoint("http://your-collector:4317")
    .build()
```

## Notes

- Make sure the Python server is running before starting the Java client.
- The client will wait for 10 seconds to collect streaming responses before exiting.
- You can modify the message text or server URL in the `HelloWorldClient.java` file if needed. 
