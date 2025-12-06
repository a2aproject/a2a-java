# A2A JSON-RPC Spring Boot Starter

This starter helps you quickly set up an A2A (Agent-to-Agent) JSON-RPC server using Spring Boot. It provides auto-configuration for handling JSON-RPC requests and integrates with Spring WebFlux for reactive request processing.

## Features

- Auto-configures a JSON-RPC request handler.
- Reactive request processing with Project Reactor.
- Handles both streaming and non-streaming JSON-RPC methods.
- Integrated security configuration with Spring Security.
- Exposes a `/.well-known/agent-card.json` endpoint for agent discovery.

## Installation

Add the following dependency to your project's `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.a2a</groupId>
    <artifactId>spring-boot-starter-a2a-jsonrpc</artifactId>
    <version>4.0.0</version>
</dependency>
```

## Usage

To use this starter, you need to create a Spring `@Component` that implements the `io.a2a.transport.jsonrpc.handler.JSONRPCHandler` interface. The starter will automatically detect and use your implementation.

**Example:**

```java
import io.a2a.spec.AgentCard;
import io.a2a.transport.jsonrpc.handler.JSONRPCHandler;
import org.springframework.stereotype.Component;

// Other necessary imports for your specific implementation

@Component
public class MyAgentHandler implements JSONRPCHandler {

    @Override
    public AgentCard getAgentCard() {
        return new AgentCard(
            "My Test Agent",
            "An agent that can say hello.",
            "http://localhost:8080/agent"
            // ... other card properties
        );
    }

    // Implement other required methods from the JSONRPCHandler interface
    // (e.g., onGetTask, onMessageSend, onMessageSendStream, etc.)
    // ...
}
```

## Configuration

By default, the starter configures the following endpoints:

- **JSON-RPC Handler**: `POST /agent`
- **Agent Card**: `GET /.well-known/agent-card.json`

These paths are defined in the `A2AServerRouter` class and can be customized by providing your own `RouterFunction` bean.

## Security

For ease of testing, the starter is currently configured to permit all requests. This is done in the `SecurityConfiguration` class.

```java
@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .anyExchange()
                        .permitAll())
                .build();
    }
}
```

For production use, you should update this configuration to properly secure your endpoints.
