# A2A Java SDK

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

<!-- markdownlint-disable no-inline-html -->

<html>
   <h2 align="center">
   <img src="https://raw.githubusercontent.com/google-a2a/A2A/refs/heads/main/docs/assets/a2a-logo-black.svg" width="256" alt="A2A Logo"/>
   </h2>
   <h3 align="center">A Java library that helps run agentic applications as A2AServers following the <a href="https://google-a2a.github.io/A2A">Agent2Agent (A2A) Protocol</a>.</h3>
</html>

## Installation

You can build the A2A Java SDK using `mvn`:

```bash
mvn clean install
```

### Regeneration of gRPC files
We copy https://github.com/a2aproject/A2A/blob/main/specification/grpc/a2a.proto to the [`spec-grpc/`](./spec-grpc) project, and adjust the `java_package` option to be as follows:
```
option java_package = "io.a2a.grpc";
```
Then build the `spec-grpc` module with `mvn clean install -Pproto-compile` to regenerate the gRPC classes in the `io.a2a.grpc` package.

## Examples

You can find examples of how to use the A2A Java SDK in the [a2a-samples repository](https://github.com/a2aproject/a2a-samples/tree/main/samples/java/agents).

More examples will be added soon.

## A2A Server

The A2A Java SDK provides a Java server implementation of the [Agent2Agent (A2A) Protocol](https://google-a2a.github.io/A2A). To run your agentic Java application as an A2A server, simply follow the steps below.

- [Add an A2A Java SDK Server Maven dependency to your project](#1-add-an-a2a-java-sdk-server-maven-dependency-to-your-project)
- [Add a class that creates an A2A Agent Card](#2-add-a-class-that-creates-an-a2a-agent-card)
- [Add a class that creates an A2A Agent Executor](#3-add-a-class-that-creates-an-a2a-agent-executor)

### 1. Add an A2A Java SDK Server Maven dependency to your project

Adding a dependency on an A2A Java SDK Server will provide access to the core classes
that make up the A2A specification and allow you to run your agentic Java application as an A2A server agent.

The A2A Java SDK provides [reference A2A server implementations](reference) based on [Quarkus](https://quarkus.io) for use with our tests and examples. However, the project is designed in such a way that it is trivial to integrate with various Java runtimes.

[Server Integrations](#server-integrations) contains a list of community contributed integrations of the server with various runtimes. You might be able to use one of these for your target runtime, or you can use them as inspiration to create your own.

#### Server Transports 
The A2A Java SDK Reference Server implementations support the following transports:

* JSON-RPC 2.0
* gRPC
* HTTP+JSON/REST

To use the reference implementation with the JSON-RPC protocol, add the following dependency to your project:

> *⚠️ The `io.github.a2asdk` `groupId` below is temporary and will likely change for future releases.*

```xml
<dependency>
    <groupId>io.github.a2asdk</groupId>
    <artifactId>a2a-java-sdk-reference-jsonrpc</artifactId>
    <!-- Use a released version from https://github.com/a2aproject/a2a-java/releases --> 
    <version>${io.a2a.sdk.version}</version>
</dependency>
```

To use the reference implementation with the gRPC protocol, add the following dependency to your project:

> *⚠️ The `io.github.a2asdk` `groupId` below is temporary and will likely change for future releases.*

```xml
<dependency>
    <groupId>io.github.a2asdk</groupId>
    <artifactId>a2a-java-sdk-reference-grpc</artifactId>
    <!-- Use a released version from https://github.com/a2aproject/a2a-java/releases --> 
    <version>${io.a2a.sdk.version}</version>
</dependency>
```

To use the reference implementation with the HTTP+JSON/REST protocol, add the following dependency to your project:

> *⚠️ The `io.github.a2asdk` `groupId` below is temporary and will likely change for future releases.*

```xml
<dependency>
    <groupId>io.github.a2asdk</groupId>
    <artifactId>a2a-java-sdk-reference-rest</artifactId>
    <!-- Use a released version from https://github.com/a2aproject/a2a-java/releases --> 
    <version>${io.a2a.sdk.version}</version>
</dependency>
```

Note that you can add more than one of the above dependencies to your project depending on the transports
you'd like to support.

### 2. Add a class that creates an A2A Agent Card

```java
import io.a2a.server.PublicAgentCard;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
...

@ApplicationScoped
public class WeatherAgentCardProducer {
    
    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {
        return new AgentCard.Builder()
                .name("Weather Agent")
                .description("Helps with weather")
                .url("http://localhost:10001")
                .version("1.0.0")
                .capabilities(new AgentCapabilities.Builder()
                        .streaming(true)
                        .pushNotifications(false)
                        .stateTransitionHistory(false)
                        .build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(Collections.singletonList(new AgentSkill.Builder()
                        .id("weather_search")
                        .name("Search weather")
                        .description("Helps with weather in cities or states")
                        .tags(Collections.singletonList("weather"))
                        .examples(List.of("weather in LA, CA"))
                        .build()))
                .protocolVersion("0.3.0")
                .build();
    }
}
```

### 3. Add a class that creates an A2A Agent Executor

```java
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TaskNotCancelableError;
import io.a2a.spec.TaskState;
import io.a2a.spec.TextPart;
...

@ApplicationScoped
public class WeatherAgentExecutorProducer {

    @Inject
    WeatherAgent weatherAgent;

    @Produces
    public AgentExecutor agentExecutor() {
        return new WeatherAgentExecutor(weatherAgent);
    }

    private static class WeatherAgentExecutor implements AgentExecutor {

        private final WeatherAgent weatherAgent;

        public WeatherAgentExecutor(WeatherAgent weatherAgent) {
            this.weatherAgent = weatherAgent;
        }

        @Override
        public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
            TaskUpdater updater = new TaskUpdater(context, eventQueue);

            // mark the task as submitted and start working on it
            if (context.getTask() == null) {
                updater.submit();
            }
            updater.startWork();

            // extract the text from the message
            String userMessage = extractTextFromMessage(context.getMessage());

            // call the weather agent with the user's message
            String response = weatherAgent.chat(userMessage);

            // create the response part
            TextPart responsePart = new TextPart(response, null);
            List<Part<?>> parts = List.of(responsePart);

            // add the response as an artifact and complete the task
            updater.addArtifact(parts, null, null, null);
            updater.complete();
        }

        @Override
        public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
            Task task = context.getTask();

            if (task.getStatus().state() == TaskState.CANCELED) {
                // task already cancelled
                throw new TaskNotCancelableError();
            }

            if (task.getStatus().state() == TaskState.COMPLETED) {
                // task already completed
                throw new TaskNotCancelableError();
            }

            // cancel the task
            TaskUpdater updater = new TaskUpdater(context, eventQueue);
            updater.cancel();
        }

        private String extractTextFromMessage(Message message) {
            StringBuilder textBuilder = new StringBuilder();
            if (message.getParts() != null) {
                for (Part part : message.getParts()) {
                    if (part instanceof TextPart textPart) {
                        textBuilder.append(textPart.getText());
                    }
                }
            }
            return textBuilder.toString();
        }
    }
}
```

### 4. Configuration System

The A2A Java SDK uses a flexible configuration system that works across different frameworks.

**Default behavior:** Configuration values come from `META-INF/a2a-defaults.properties` files on the classpath (provided by core modules and extras). These defaults work out of the box without any additional setup.

**Customizing configuration:**
- **Quarkus/MicroProfile Config users**: Add the [`microprofile-config`](integrations/microprofile-config/README.md) integration to override defaults via `application.properties`, environment variables, or system properties
- **Spring/other frameworks**: See the [integration module README](integrations/microprofile-config/README.md#custom-config-providers) for how to implement a custom `A2AConfigProvider`
- **Reference implementations**: Already include the MicroProfile Config integration

#### Configuration Properties

**Executor Settings** (Optional)

The SDK uses a dedicated executor for async operations like streaming. Default: 5 core threads, 50 max threads.

```properties
# Core thread pool size for the @Internal executor (default: 5)
a2a.executor.core-pool-size=5

# Maximum thread pool size (default: 50)
a2a.executor.max-pool-size=50

# Thread keep-alive time in seconds (default: 60)
a2a.executor.keep-alive-seconds=60
```

**Blocking Call Timeouts** (Optional)

```properties
# Timeout for agent execution in blocking calls (default: 30 seconds)
a2a.blocking.agent.timeout.seconds=30

# Timeout for event consumption in blocking calls (default: 5 seconds)
a2a.blocking.consumption.timeout.seconds=5
```

**Why this matters:**
- **Streaming Performance**: The executor handles streaming subscriptions. Too few threads can cause timeouts under concurrent load.
- **Resource Management**: The dedicated executor prevents streaming operations from competing with the ForkJoinPool.
- **Concurrency**: In production with high concurrent streaming, increase pool sizes accordingly.
- **Agent Timeouts**: LLM-based agents may need longer timeouts (60-120s) compared to simple agents.

**Note:** The reference server implementations (Quarkus-based) automatically include the MicroProfile Config integration, so properties work out of the box in `application.properties`.

## A2A Client

The A2A Java SDK provides a Java client implementation of the [Agent2Agent (A2A) Protocol](https://google-a2a.github.io/A2A), allowing communication with A2A servers. The Java client implementation supports the following transports:

* JSON-RPC 2.0
* gRPC
* HTTP+JSON/REST

To make use of the Java `Client`:

### 1. Add the A2A Java SDK Client dependency to your project

Adding a dependency on `a2a-java-sdk-client` will provide access to a `ClientBuilder`
that you can use to create your A2A `Client`.

----
> *⚠️ The `io.github.a2asdk` `groupId` below is temporary and will likely change for future releases.*
----

```xml
<dependency>
    <groupId>io.github.a2asdk</groupId>
    <artifactId>a2a-java-sdk-client</artifactId>
    <!-- Use a released version from https://github.com/a2aproject/a2a-java/releases -->
    <version>${io.a2a.sdk.version}</version>
</dependency>
```

### 2. Add one or more dependencies on the A2A Java SDK Client Transport(s) you'd like to use

By default, the `sdk-client` artifact includes the JSONRPC transport dependency. However, you must still explicitly configure this transport when building the `Client` as described in the [JSON-RPC Transport section](#json-rpc-transport-configuration).


If you want to use the gRPC transport, you'll need to add a relevant dependency:

----
> *⚠️ The `io.github.a2asdk` `groupId` below is temporary and will likely change for future releases.*
----

```xml
<dependency>
    <groupId>io.github.a2asdk</groupId>
    <artifactId>a2a-java-sdk-client-transport-grpc</artifactId>
    <!-- Use a released version from https://github.com/a2aproject/a2a-java/releases -->
    <version>${io.a2a.sdk.version}</version>
</dependency>
```


If you want to use the HTTP+JSON/REST transport, you'll need to add a relevant dependency:

----
> *⚠️ The `io.github.a2asdk` `groupId` below is temporary and will likely change for future releases.*
----

```xml
<dependency>
    <groupId>io.github.a2asdk</groupId>
    <artifactId>a2a-java-sdk-client-transport-rest</artifactId>
    <!-- Use a released version from https://github.com/a2aproject/a2a-java/releases -->
    <version>${io.a2a.sdk.version}</version>
</dependency>
```

### Sample Usage

#### Create a Client using the ClientBuilder

```java
// First, get the agent card for the A2A server agent you want to connect to
AgentCard agentCard = new A2ACardResolver("http://localhost:1234").getAgentCard();

// Specify configuration for the ClientBuilder
ClientConfig clientConfig = new ClientConfig.Builder()
        .setAcceptedOutputModes(List.of("text"))
        .build();

// Create event consumers to handle responses that will be received from the A2A server
// (these consumers will be used for both streaming and non-streaming responses)
List<BiConsumer<ClientEvent, AgentCard>> consumers = List.of(
    (event, card) -> {
        if (event instanceof MessageEvent messageEvent) {
            // handle the messageEvent.getMessage()
            ...
        } else if (event instanceof TaskEvent taskEvent) {
            // handle the taskEvent.getTask()
            ...
        } else if (event instanceof TaskUpdateEvent updateEvent) {
            // handle the updateEvent.getTask()
            ...
        }
    }
);

// Create a handler that will be used for any errors that occur during streaming
Consumer<Throwable> errorHandler = error -> {
    // handle the error.getMessage()
    ...
};

// Create the client using the builder
Client client = Client
        .builder(agentCard)
        .clientConfig(clientConfig)
        .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
        .addConsumers(consumers)
        .streamingErrorHandler(errorHandler)
        .build();
```

#### Configuring Transport-Specific Settings

Different transport protocols can be configured with specific settings using specific `ClientTransportConfig` implementations. The A2A Java SDK provides `JSONRPCTransportConfig` for the JSON-RPC transport and `GrpcTransportConfig` for the gRPC transport.

##### JSON-RPC Transport Configuration

For the JSON-RPC transport, to use the default `JdkA2AHttpClient`, provide a `JSONRPCTransportConfig` created with its default constructor.

To use a custom HTTP client implementation, simply create a `JSONRPCTransportConfig` as follows:

```java
// Create a custom HTTP client
A2AHttpClient customHttpClient = ...

// Configure the client settings
ClientConfig clientConfig = new ClientConfig.Builder()
        .setAcceptedOutputModes(List.of("text"))
        .build();

Client client = Client
        .builder(agentCard)
        .clientConfig(clientConfig)
        .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig(customHttpClient))
        .build();
```

##### gRPC Transport Configuration

For the gRPC transport, you must configure a channel factory:

```java
// Create a channel factory function that takes the agent URL and returns a Channel
Function<String, Channel> channelFactory = agentUrl -> {
    return ManagedChannelBuilder.forTarget(agentUrl)
            ...
            .build();
};

// Configure the client with transport-specific settings
ClientConfig clientConfig = new ClientConfig.Builder()
        .setAcceptedOutputModes(List.of("text"))
        .build();

Client client = Client
        .builder(agentCard)
        .clientConfig(clientConfig)
        .withTransport(GrpcTransport.class, new GrpcTransportConfig(channelFactory))
        .build();
```


##### HTTP+JSON/REST Transport Configuration

For the HTTP+JSON/REST transport, if you'd like to use the default `JdkA2AHttpClient`, provide a `RestTransportConfig` created with its default constructor.

To use a custom HTTP client implementation, simply create a `RestTransportConfig` as follows:

```java
// Create a custom HTTP client
A2AHttpClient customHttpClient = ...

// Configure the client settings
ClientConfig clientConfig = new ClientConfig.Builder()
        .setAcceptedOutputModes(List.of("text"))
        .build();

Client client = Client
        .builder(agentCard)
        .clientConfig(clientConfig)
        .withTransport(RestTransport.class, new RestTransportConfig(customHttpClient))
        .build();
```

##### Multiple Transport Configurations

You can specify configuration for multiple transports, the appropriate configuration
will be used based on the selected transport:

```java
// Configure both JSON-RPC and gRPC transports
Client client = Client
                .builder(agentCard)
                .withTransport(GrpcTransport.class, new GrpcTransportConfig(channelFactory))
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
                .withTransport(RestTransport.class, new RestTransportConfig())
                .build();
```

#### Send a message to the A2A server agent

```java
// Send a text message to the A2A server agent
Message message = A2A.toUserMessage("tell me a joke");

// Send the message (uses configured consumers to handle responses)
// Streaming will automatically be used if supported by both client and server,
// otherwise the non-streaming send message method will be used automatically
client.sendMessage(message);

// You can also optionally specify a ClientCallContext with call-specific config to use
client.sendMessage(message, clientCallContext);
```

#### Send a message with custom event handling

```java
// Create custom consumers for this specific message
List<BiConsumer<ClientEvent, AgentCard>> customConsumers = List.of(
    (event, card) -> {
        // handle this specific message's responses
        ...
    }
);

// Create custom error handler
Consumer<Throwable> customErrorHandler = error -> {
    // handle the error
    ...
};

Message message = A2A.toUserMessage("tell me a joke");
client.sendMessage(message, customConsumers, customErrorHandler);
```

#### Get the current state of a task

```java
// Retrieve the task with id "task-1234"
Task task = client.getTask(new TaskQueryParams("task-1234"));

// You can also specify the maximum number of history items for the task
// to include in the response 
Task task = client.getTask(new TaskQueryParams("task-1234", 10));

// You can also optionally specify a ClientCallContext with call-specific config to use
Task task = client.getTask(new TaskQueryParams("task-1234"), clientCallContext);
```

#### Cancel an ongoing task

```java
// Cancel the task we previously submitted with id "task-1234"
Task cancelledTask = client.cancelTask(new TaskIdParams("task-1234"));

// You can also specify additional properties using a map
Map<String, Object> metadata = Map.of("reason", "user_requested");
Task cancelledTask = client.cancelTask(new TaskIdParams("task-1234", metadata));

// You can also optionally specify a ClientCallContext with call-specific config to use
Task cancelledTask = client.cancelTask(new TaskIdParams("task-1234"), clientCallContext);
```

#### Get a push notification configuration for a task

```java
// Get task push notification configuration
TaskPushNotificationConfig config = client.getTaskPushNotificationConfiguration(
    new GetTaskPushNotificationConfigParams("task-1234"));

// The push notification configuration ID can also be optionally specified
TaskPushNotificationConfig config = client.getTaskPushNotificationConfiguration(
    new GetTaskPushNotificationConfigParams("task-1234", "config-4567"));

// Additional properties can be specified using a map
Map<String, Object> metadata = Map.of("source", "client");
TaskPushNotificationConfig config = client.getTaskPushNotificationConfiguration(
    new GetTaskPushNotificationConfigParams("task-1234", "config-1234", metadata));

// You can also optionally specify a ClientCallContext with call-specific config to use
TaskPushNotificationConfig config = client.getTaskPushNotificationConfiguration(
        new GetTaskPushNotificationConfigParams("task-1234"), clientCallContext);
```

#### Set a push notification configuration for a task

```java
// Set task push notification configuration
PushNotificationConfig pushNotificationConfig = new PushNotificationConfig.Builder()
        .url("https://example.com/callback")
        .authenticationInfo(new AuthenticationInfo(Collections.singletonList("jwt"), null))
        .build();

TaskPushNotificationConfig taskConfig = new TaskPushNotificationConfig.Builder()
        .taskId("task-1234")
        .pushNotificationConfig(pushNotificationConfig)
        .build();

TaskPushNotificationConfig result = client.setTaskPushNotificationConfiguration(taskConfig);

// You can also optionally specify a ClientCallContext with call-specific config to use
TaskPushNotificationConfig result = client.setTaskPushNotificationConfiguration(taskConfig, clientCallContext);
```

#### List the push notification configurations for a task

```java
List<TaskPushNotificationConfig> configs = client.listTaskPushNotificationConfigurations(
    new ListTaskPushNotificationConfigParams("task-1234"));

// Additional properties can be specified using a map
Map<String, Object> metadata = Map.of("filter", "active");
List<TaskPushNotificationConfig> configs = client.listTaskPushNotificationConfigurations(
    new ListTaskPushNotificationConfigParams("task-1234", metadata));

// You can also optionally specify a ClientCallContext with call-specific config to use
List<TaskPushNotificationConfig> configs = client.listTaskPushNotificationConfigurations(
        new ListTaskPushNotificationConfigParams("task-1234"), clientCallContext);
```

#### Delete a push notification configuration for a task

```java
client.deleteTaskPushNotificationConfigurations(
    new DeleteTaskPushNotificationConfigParams("task-1234", "config-4567"));

// Additional properties can be specified using a map
Map<String, Object> metadata = Map.of("reason", "cleanup");
client.deleteTaskPushNotificationConfigurations(
    new DeleteTaskPushNotificationConfigParams("task-1234", "config-4567", metadata));

// You can also optionally specify a ClientCallContext with call-specific config to use
client.deleteTaskPushNotificationConfigurations(
    new DeleteTaskPushNotificationConfigParams("task-1234", "config-4567", clientCallContext);
```

#### Resubscribe to a task

```java
// Resubscribe to an ongoing task with id "task-1234" using configured consumers
TaskIdParams taskIdParams = new TaskIdParams("task-1234");
client.resubscribe(taskIdParams);

// Or resubscribe with custom consumers and error handler
List<BiConsumer<ClientEvent, AgentCard>> customConsumers = List.of(
    (event, card) -> System.out.println("Resubscribe event: " + event)
);
Consumer<Throwable> customErrorHandler = error -> 
    System.err.println("Resubscribe error: " + error.getMessage());

client.resubscribe(taskIdParams, customConsumers, customErrorHandler);

// You can also optionally specify a ClientCallContext with call-specific config to use
client.resubscribe(taskIdParams, clientCallContext);
```

#### Retrieve details about the server agent that this client agent is communicating with
```java
AgentCard serverAgentCard = client.getAgentCard();
```

## Additional Examples

### Hello World Client Example

A complete example of a Java A2A client communicating with a Python A2A server is available in the [examples/helloworld/client](examples/helloworld/client/README.md) directory. This example demonstrates:

- Setting up and using the A2A Java client
- Sending regular and streaming messages to a Python A2A server
- Receiving and processing responses from the Python A2A server

The example includes detailed instructions on how to run the Python A2A server and how to run the Java A2A client using JBang.

Check out the [example's README](examples/helloworld/client/README.md) for more information.

### Hello World Server Example

A complete example of a Python A2A client communicating with a Java A2A server is available in the [examples/helloworld/server](examples/helloworld/server/README.md) directory. This example demonstrates:

- A sample `AgentCard` producer
- A sample `AgentExecutor` producer
- A Java A2A server receiving regular and streaming messages from a Python A2A client

Check out the [example's README](examples/helloworld/server/README.md) for more information.

## Community Articles

See [COMMUNITY_ARTICLES.md](COMMUNITY_ARTICLES.md) for a list of community articles and videos.

## License

This project is licensed under the terms of the [Apache 2.0 License](LICENSE).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines.

## Server Integrations
The following list contains community contributed integrations with various Java Runtimes.

To contribute an integration, please see [CONTRIBUTING_INTEGRATIONS.md](CONTRIBUTING_INTEGRATIONS.md).

* [reference/jsonrpc/README.md](reference/jsonrpc/README.md) - JSON-RPC 2.0 Reference implementation, based on Quarkus.
* [reference/grpc/README.md](reference/grpc/README.md) - gRPC Reference implementation, based on Quarkus.
* https://github.com/wildfly-extras/a2a-java-sdk-server-jakarta - This integration is based on Jakarta EE, and should work in all runtimes supporting the [Jakarta EE Web Profile](https://jakarta.ee/specifications/webprofile/).

# Extras
See the [`extras`](./extras/README.md) folder for extra functionality not provided by the SDK itself!

