package org.a2aproject.sdk.examples.springboot.rest.client;

import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(
        info = @Info(
                title = "A2A Spring Boot REST Client Demo",
                version = "1.0.0",
                description = """
                        Scenario-based demo app for the A2A Spring Boot REST integration.

                        Use the endpoints to fetch the remote agent card, run blocking and streaming
                        client flows, and inspect the end-to-end result as JSON.
                        """),
        servers = @Server(url = "http://localhost:18081"),
        tags = {
                @Tag(name = "A2A Spring Boot REST Demo", description = "Scenario endpoints for exercising the A2A REST client against the example server")
        })
public class SpringBootRestClientOpenApiConfig {
}
