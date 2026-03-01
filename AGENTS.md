# AGENTS.md - AI Agent Guide for A2A Java SDK

This document provides essential context for AI agents working with the A2A Java SDK codebase.

## Project Overview

**A2A Java SDK** is a comprehensive Java implementation of the [Agent2Agent (A2A) Protocol](https://a2a-protocol.org/), enabling Java applications to run as A2A servers and communicate with other A2A agents.

- **Language**: Java 17+
- **Build Tool**: Maven 3.x
- **Primary Framework**: Quarkus (for reference implementations)
- **License**: Apache 2.0
- **Repository**: https://github.com/a2aproject/a2a-java

## Quick Start Guide

This 5-minute guide shows you how to create your first A2A server and client using the helloworld example.

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- An IDE or text editor

### Build the SDK

First, clone and build the A2A Java SDK:

```bash
git clone https://github.com/a2aproject/a2a-java.git
cd a2a-java
mvn clean install -DskipTests=true
```

### Create a Simple A2A Server

The minimal A2A server requires two components:

**1. AgentCard Producer** (`AgentCardProducer.java`):

```java
@ApplicationScoped
public class AgentCardProducer {
    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {
        return AgentCard.builder()
            .name("Hello World Agent")
            .description("Just a hello world agent")
            .supportedInterfaces(Collections.singletonList(
                new AgentInterface("jsonrpc", "http://localhost:9999")))
            .version("1.0.0")
            .capabilities(AgentCapabilities.builder()
                .streaming(true)
                .build())
            .protocolVersion(AgentCard.CURRENT_PROTOCOL_VERSION)
            .build();
    }
}
```

**2. AgentExecutor Implementation** (`AgentExecutorProducer.java`):

```java
@ApplicationScoped
public class AgentExecutorProducer {
    @Produces
    public AgentExecutor agentExecutor() {
        return new AgentExecutor() {
            @Override
            public void execute(RequestContext context, EventQueue eventQueue)
                    throws A2AError {
                eventQueue.enqueueEvent(A2A.toAgentMessage("Hello World"));
            }

            @Override
            public void cancel(RequestContext context, EventQueue eventQueue)
                    throws A2AError {
                throw new UnsupportedOperationError();
            }
        };
    }
}
```

**Run the Server**:

```bash
cd examples/helloworld/server
mvn quarkus:dev
```

The server will start on `http://localhost:9999`.

### Create a Simple A2A Client

**Minimal Client Example** (`HelloWorldClient.java`):

```java
public class HelloWorldClient {
    public static void main(String[] args) throws Exception {
        // 1. Fetch the agent card from the server
        AgentCard agentCard = new A2ACardResolver("http://localhost:9999")
            .getAgentCard();

        // 2. Create event consumer to handle responses
        CompletableFuture<String> response = new CompletableFuture<>();
        List<BiConsumer<ClientEvent, AgentCard>> consumers = new ArrayList<>();
        consumers.add((event, card) -> {
            if (event instanceof MessageEvent messageEvent) {
                Message msg = messageEvent.getMessage();
                response.complete(extractText(msg));
            }
        });

        // 3. Build the client with JSON-RPC transport
        Client client = Client.builder(agentCard)
            .addConsumers(consumers)
            .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
            .build();

        // 4. Send a message
        Message message = A2A.toUserMessage("Hello!");
        client.sendMessage(message);

        // 5. Wait for and print response
        System.out.println("Response: " + response.get());
    }

    private static String extractText(Message msg) {
        StringBuilder text = new StringBuilder();
        if (msg.parts() != null) {
            for (Part<?> part : msg.parts()) {
                if (part instanceof TextPart textPart) {
                    text.append(textPart.text());
                }
            }
        }
        return text.toString();
    }
}
```

**Run the Client**:

```bash
cd examples/helloworld/client
# Using JBang (recommended for quick testing)
jbang src/main/java/io/a2a/examples/helloworld/HelloWorldRunner.java

# Or using Maven
mvn compile exec:java
```

### What Just Happened?

1. **Server Side**:
   - `AgentCardProducer` defines the agent's metadata and capabilities
   - `AgentExecutor` handles incoming messages and sends responses via `EventQueue`
   - Quarkus provides the runtime and automatic endpoint creation

2. **Client Side**:
   - Fetches the server's `AgentCard` to discover capabilities
   - Creates a `Client` with event consumers for handling responses
   - Sends messages and receives responses asynchronously

### Next Steps

- Explore the full example: `examples/helloworld/`
- Try different transports (gRPC, REST) instead of JSON-RPC
- Add task management with `TaskUpdater`
- Implement streaming responses
- Add skills and capabilities to your AgentCard

See the "Common Development Tasks" section below for more advanced patterns.

## Architecture

### Multi-Module Maven Project Structure

```
a2a-java/
├── spec/                           # Core A2A specification (POJOs, interfaces) - BASE MODULE
├── spec-grpc/                      # gRPC protocol definitions → spec
├── common/                         # Shared utilities → spec
├── jsonrpc-common/                 # JSON-RPC utilities → spec, common
├── http-client/                    # HTTP client abstraction → spec, common
├── client/                         # Client SDK
│   ├── base/                      # Core client → spec, common, http-client
│   └── transport/                 # Transport implementations
│       ├── spi/                   # Transport SPI → client/base
│       ├── grpc/                  # gRPC transport → client/base, spi, spec-grpc
│       ├── jsonrpc/               # JSON-RPC transport → client/base, spi, jsonrpc-common
│       └── rest/                  # REST transport → client/base, spi, http-client
├── server-common/                  # Server SDK core → spec, common
├── transport/                      # Server transport implementations
│   ├── grpc/                      # gRPC server → server-common, spec-grpc
│   ├── jsonrpc/                   # JSON-RPC server → server-common, jsonrpc-common
│   └── rest/                      # REST server → server-common, http-client
├── reference/                      # Reference implementations (Quarkus-based)
│   ├── common/                    # Common reference → server-common
│   ├── grpc/                      # gRPC reference → reference/common, transport/grpc
│   ├── jsonrpc/                   # JSON-RPC reference → reference/common, transport/jsonrpc
│   └── rest/                      # REST reference → reference/common, transport/rest
├── integrations/                   # Framework integrations
│   └── microprofile-config/       # MicroProfile Config → common
├── extras/                         # Optional extensions
│   ├── common/                    # Extras common → server-common
│   ├── task-store-database-jpa/   # JPA task store → extras/common
│   ├── push-notification-config-store-database-jpa/ # JPA push config → extras/common
│   └── queue-manager-replicated/  # Replicated queue → extras/common
├── boms/                           # Bill of Materials (dependency management only)
│   ├── test-utils/                # Test utilities BOM
│   ├── sdk/                       # SDK BOM → all core modules
│   ├── reference/                 # Reference BOM → sdk, reference modules
│   └── extras/                    # Extras BOM → sdk, extras modules
├── examples/                       # Example applications → reference implementations
├── tck/                           # Technology Compatibility Kit → spec, client, server
└── tests/                         # Test utilities
    └── server-common/             # Server test utils → server-common

Legend: → indicates "depends on"
```

**Key Dependency Flows:**

1. **Foundation Layer**: `spec` is the base - all modules depend on it
2. **Utilities Layer**: `common`, `jsonrpc-common`, `http-client` provide shared functionality
3. **Client Stack**: `client/base` → `transport/spi` → specific transports (grpc/jsonrpc/rest)
4. **Server Stack**: `server-common` → `transport/*` → `reference/*` implementations
5. **Extensions**: `extras/common` → specific extras (task-store, push-config, queue-manager)
6. **BOMs**: Aggregate dependencies without code, used for version management

### Key Concepts

1. **AgentCard**: Describes an agent's capabilities, skills, and metadata
2. **AgentExecutor**: Core interface for implementing agent logic
3. **Task**: Represents a unit of work with state management
4. **Message**: Communication unit with parts (text, data, files)
5. **Transport**: Protocol layer (JSON-RPC 2.0, gRPC, HTTP+JSON/REST)
6. **EventQueue**: Streaming event delivery mechanism

### Design Patterns

- **CDI/Jakarta EE**: Dependency injection using `@ApplicationScoped`, `@Produces`, `@Inject`
- **Builder Pattern**: Extensively used for creating immutable objects (AgentCard, Message, Task)
- **Strategy Pattern**: Transport implementations are pluggable
- **Observer Pattern**: Event-based streaming with consumers
- **Reactive Programming**: Uses Mutiny Zero for async operations

## Development Guidelines

### Building the Project

```bash
# Full build with tests
mvn clean install

# Skip tests
mvn clean install -DskipTests=true

# Build specific module
cd <module-directory>
mvn clean install
```

### Code Quality Standards

- **Null Safety**: Uses JSpecify annotations and NullAway for compile-time null checking
- **Error Handling**: Custom error types extending `JSONRPCError`
- **Logging**: SLF4J facade with appropriate log levels
- **Testing**: JUnit 5, Mockito, REST Assured

### Configuration System

The SDK uses a flexible configuration system:
- Default values in `META-INF/a2a-defaults.properties`
- Override via MicroProfile Config (Quarkus) or custom `A2AConfigProvider`
- Key properties:
  - `a2a.executor.core-pool-size`: Thread pool for async operations
  - `a2a.blocking.agent.timeout.seconds`: Agent execution timeout
  - `a2a.blocking.consumption.timeout.seconds`: Event consumption timeout

## Common Development Tasks

### Creating a New A2A Server

1. Add dependency on reference implementation (jsonrpc, grpc, or rest)
2. Create `AgentCard` producer with `@PublicAgentCard` qualifier
3. Implement `AgentExecutor` interface
4. Use `TaskUpdater` for task state management
5. Handle messages and produce artifacts

**Key Classes**:
- `io.a2a.spec.AgentCard`
- `io.a2a.server.agentexecution.AgentExecutor`
- `io.a2a.server.tasks.TaskUpdater`
- `io.a2a.server.events.EventQueue`

### Creating an A2A Client

1. Add `a2a-java-sdk-client` dependency
2. Add transport dependency (grpc, jsonrpc, or rest)
3. Get `AgentCard` from target server
4. Build `Client` with `ClientBuilder`
5. Configure event consumers and error handlers
6. Send messages and handle responses

**Key Classes**:
- `io.a2a.client.Client`
- `io.a2a.client.ClientBuilder`
- `io.a2a.client.ClientConfig`
- `io.a2a.client.transport.*TransportConfig`

### Adding a New Transport

1. Implement `ClientTransport` interface (client side)
2. Implement server-side transport endpoints
3. Add transport configuration class
4. Register in `ClientBuilder`
5. Add tests using TCK

### Working with Tasks

```java
// Server side - updating task state
TaskUpdater updater = new TaskUpdater(context, eventQueue);
updater.submit();           // Mark as submitted
updater.startWork();        // Begin processing
updater.addArtifact(...);   // Add results
updater.complete();         // Mark as complete
updater.cancel();           // Cancel task

// Client side - querying task
Task task = client.getTask(new TaskQueryParams(taskId));
Task cancelled = client.cancelTask(new TaskIdParams(taskId));
```

## Testing

### Test Structure

- **Unit Tests**: In each module's `src/test/java`
- **Integration Tests**: In `tests/server-common`
- **TCK**: Technology Compatibility Kit in `tck/` module
- **Examples**: Runnable examples in `examples/`

### Running Tests

```bash
# All tests
mvn test

# Specific module
cd <module>
mvn test

# Integration tests
cd tests/server-common
mvn verify
```

### Test Utilities

- `tests/server-common`: Shared test infrastructure
- Mock servers using MockServer library
- REST Assured for HTTP testing
- Quarkus test framework for reference implementations

## Important Files and Locations

### Documentation
- `README.md`: Main project documentation
- `CONTRIBUTING.md`: Contribution guidelines
- `RELEASE.md`: Release process
- `SECURITY.md`: Security policy
- `examples/*/README.md`: Example-specific documentation

### Configuration
- `pom.xml`: Root Maven configuration
- `*/pom.xml`: Module-specific configurations
- `META-INF/a2a-defaults.properties`: Default configuration values
- `.github/workflows/`: CI/CD pipelines

### Code Generation
- `spec-grpc/src/main/proto/a2a.proto`: Protocol definition (used for ALL transports: gRPC, JSON-RPC, REST)
  - **⚠️ CRITICAL: This file is the single source of truth for ALL payloads and must NEVER be altered by AI agents**
- Regenerate Java classes with: `mvn clean install -Dskip.protobuf.generate=false`

## Module Dependencies

### Core Dependencies
- `spec` → Base for all modules (core types)
- `common` → Shared utilities
- `server-common` → Base for server implementations
- `client/base` → Base for client implementations

### Transport Dependencies
- Client transports depend on `client/transport/spi`
- Server transports depend on `server-common`
- Reference implementations depend on respective transports

### BOM Structure
- `boms/sdk`: Core SDK dependencies
- `boms/reference`: Reference implementation dependencies
- `boms/extras`: Optional extras dependencies
- `boms/test-utils`: Test utilities

## Common Patterns in Codebase

### Error Handling
```java
// Protocol errors extend A2AProtocolError (which extends A2AError)
throw new TaskNotFoundError("Task not found: " + taskId);
throw new TaskNotCancelableError();
throw new ContentTypeNotSupportedError("Unsupported content type");

// Generic A2A errors extend A2AError directly
throw new InvalidRequestError("Invalid request format");
throw new InternalError("Internal processing error");
```

### Async Operations
```java
// Using Mutiny Zero for reactive streams (in Quarkus-based modules)
Flow.Publisher<ClientEvent> publisher = transport.streamingCall(...);
publisher.subscribe(new Flow.Subscriber<ClientEvent>() { ... });
```

### CDI Producers
```java
@ApplicationScoped
public class MyProducer {
    @Produces
    @PublicAgentCard
    public AgentCard agentCard() { ... }
    
    @Produces
    public AgentExecutor executor() { ... }
}
```

### Builder Pattern
```java
AgentCard card = AgentCard.builder()
    .name("My Agent")
    .description("Description")
    .url("http://localhost:8080")
    .capabilities(AgentCapabilities.builder()
        .streaming(true)
        .build())
    .build();
```

## Troubleshooting

### Common Issues

1. **gRPC Generation**: If gRPC classes are missing, rebuild `spec-grpc` with `-Dskip.protobuf.generate=false`
2. **Null Safety Warnings**: Ensure proper `@Nullable` annotations from JSpecify
3. **Timeout Issues**: Adjust `a2a.blocking.*.timeout.seconds` properties
4. **Thread Pool Exhaustion**: Increase `a2a.executor.max-pool-size`

### Debug Tips

- Enable debug logging: Set `quarkus.log.level=DEBUG` in `application.properties`
- Use `maven.test.redirectTestOutputToFile=false` to see test output in console
- Check `target/surefire-reports/` for test failure details

## Version Information

- **Java Version**: 17+ (21+ recommended for full null safety support)
- **Protocol Version**: Defined in `AgentCard.CURRENT_PROTOCOL_VERSION`
- **Current Versions**: Check `pom.xml` for current dependency versions (Quarkus, gRPC, etc.)

## External Resources

- [A2A Protocol Specification](https://a2a-protocol.org/)
- [A2A Samples Repository](https://github.com/a2aproject/a2a-samples)
- [Quarkus Documentation](https://quarkus.io)
- [gRPC Java Documentation](https://grpc.io/docs/languages/java/)

## Contributing

See `CONTRIBUTING.md` for:
- Fork and clone process
- Branch naming conventions
- PR requirements (tests, documentation, commit messages)
- Code review process

## Coding Principles

When writing or generating code for the A2A Java SDK, strictly adhere to these fundamental principles:

### KISS (Keep It Simple, Stupid)

- **Write simple, readable code** - Favor clarity over cleverness
- **Avoid over-engineering** - Don't add complexity for hypothetical future needs
- **One responsibility per class/method** - Each component should do one thing well
- **Minimize abstractions** - Only abstract when there's a clear, immediate benefit
- **Use straightforward solutions** - Choose the simplest approach that solves the problem
- **Avoid premature optimization** - Make it work first, optimize only when needed

**Example - Simple vs Complex:**
```java
// GOOD - Simple and clear
public String extractText(Message msg) {
    StringBuilder text = new StringBuilder();
    if (msg.parts() != null) {
        for (Part<?> part : msg.parts()) {
            if (part instanceof TextPart textPart) {
                text.append(textPart.text());
            }
        }
    }
    return text.toString();
}

// BAD - Over-engineered
public String extractText(Message msg) {
    return Optional.ofNullable(msg.parts())
        .orElse(Collections.emptyList())
        .stream()
        .filter(TextPart.class::isInstance)
        .map(TextPart.class::cast)
        .map(TextPart::text)
        .collect(Collectors.joining());
}
```

### DRY (Don't Repeat Yourself)

- **Extract common logic** - If code appears more than once, extract it into a reusable method/class
- **Use inheritance and composition** - Share behavior through proper OOP design
- **Leverage existing utilities** - Check `common/` module before creating new utilities
- **Create helper methods** - For repeated patterns, create well-named helper methods
- **Avoid copy-paste** - Never duplicate code; refactor to share logic
- **Single source of truth** - Each piece of knowledge should exist in exactly one place

**Example - Avoiding Repetition:**
```java
// GOOD - DRY approach
public class MessageUtils {
    public static String extractText(Message msg) {
        StringBuilder text = new StringBuilder();
        if (msg.parts() != null) {
            for (Part<?> part : msg.parts()) {
                if (part instanceof TextPart textPart) {
                    text.append(textPart.text());
                }
            }
        }
        return text.toString();
    }
}

// Use it everywhere
String text1 = MessageUtils.extractText(message1);
String text2 = MessageUtils.extractText(message2);

// BAD - Repeated code
String text1 = extractTextFromMessage1(); // duplicated logic
String text2 = extractTextFromMessage2(); // duplicated logic
```

### Additional Best Practices

- **Fail fast** - Validate inputs early and throw meaningful exceptions
- **Immutability** - Prefer immutable objects (use builders for complex objects)
- **Meaningful names** - Use descriptive names that reveal intent
- **Small methods** - Keep methods short and focused (ideally < 20 lines)
- **Consistent formatting** - Follow the project's code style
- **Comments for "why", not "what"** - Code should be self-documenting; comments explain reasoning

## Notes for AI Agents

1. **Always check module dependencies** before suggesting changes
2. **Follow existing patterns** for consistency (builders, CDI, error handling)
3. **Apply KISS and DRY principles** - Keep code simple and avoid duplication
4. **Consider null safety** - use JSpecify annotations
5. **Test thoroughly** - add unit tests for new functionality
6. **Update documentation** - README.md and relevant module READMEs
7. **Respect the architecture** - don't introduce circular dependencies
8. **Use appropriate transport** - understand differences between JSON-RPC, gRPC, and REST
9. **Configuration over code** - prefer configuration properties for behavior changes
10. **Async-first** - use reactive patterns for I/O operations
11. **Security-conscious** - validate inputs, handle errors gracefully
12. **Reuse before creating** - Check existing utilities in `common/` module before writing new code

---

*This document is maintained to help AI agents understand and work effectively with the A2A Java SDK codebase.*
