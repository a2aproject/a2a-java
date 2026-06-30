package org.a2aproject.sdk.examples.springboot.rest.client;

import org.a2aproject.sdk.spec.AgentCard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/demo")
@Tag(name = "A2A Spring Boot REST Demo", description = "Scenario endpoints for exercising the A2A REST client against the example server")
public class SpringBootRestClientDemoController {

    private final SpringBootRestClientDemoService demoService;

    @GetMapping("/agent-card")
    @Operation(summary = "Fetch the remote agent card", description = "Calls the server example and returns its AgentCard as-is.")
    public ResponseEntity<AgentCard> fetchAgentCard() {
        return ResponseEntity.ok(demoService.fetchAgentCard());
    }

    @PostMapping("/blocking")
    @Operation(summary = "Run the blocking message flow", description = "Sends one blocking message and returns the observed events.")
    public ResponseEntity<SpringBootRestClientScenarioResponse> runBlocking(@RequestBody(required = false) SpringBootRestClientDemoRequest request) {
        log.info("Running blocking demo endpoint");
        return ResponseEntity.ok(demoService.runBlockingDemo(request));
    }

    @PostMapping("/streaming")
    @Operation(summary = "Run the streaming message flow", description = "Sends one streaming message and returns the observed task events.")
    public ResponseEntity<SpringBootRestClientScenarioResponse> runStreaming(@RequestBody(required = false) SpringBootRestClientDemoRequest request) {
        log.info("Running streaming demo endpoint");
        return ResponseEntity.ok(demoService.runStreamingDemo(request));
    }

    @PostMapping("/full-flow")
    @Operation(summary = "Run the full demo flow", description = "Fetches the card, runs a blocking call, then runs a streaming call and returns a combined report.")
    public ResponseEntity<SpringBootRestClientFullFlowResponse> runFullFlow(@RequestBody(required = false) SpringBootRestClientDemoRequest request) {
        log.info("Running full-flow demo endpoint");
        return ResponseEntity.ok(demoService.runFullFlow(request));
    }
}
