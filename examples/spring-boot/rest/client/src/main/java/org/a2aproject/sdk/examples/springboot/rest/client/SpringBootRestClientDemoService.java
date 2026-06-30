package org.a2aproject.sdk.examples.springboot.rest.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.a2aproject.sdk.A2A;
import org.a2aproject.sdk.client.Client;
import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.ClientEvent;
import org.a2aproject.sdk.client.MessageEvent;
import org.a2aproject.sdk.client.TaskEvent;
import org.a2aproject.sdk.client.TaskUpdateEvent;
import org.a2aproject.sdk.client.config.ClientConfig;
import org.a2aproject.sdk.client.transport.rest.RestTransport;
import org.a2aproject.sdk.client.transport.rest.RestTransportConfigBuilder;
import org.a2aproject.sdk.jsonrpc.common.json.JsonProcessingException;
import org.a2aproject.sdk.jsonrpc.common.json.JsonUtil;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.TextPart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpringBootRestClientDemoService {

    private final SpringBootRestClientExampleProperties properties;

    public AgentCard fetchAgentCard() {
        log.info("Fetching agent card from {}", properties.serverUrl());
        AgentCard agentCard = A2A.getAgentCard(properties.serverUrl());
        try {
            log.info("Fetched agent card:\n{}", JsonUtil.toJson(agentCard));
        } catch (JsonProcessingException e) {
            log.warn("Fetched agent card, but could not serialize it for logs", e);
        }
        return agentCard;
    }

    public SpringBootRestClientScenarioResponse runBlockingDemo(SpringBootRestClientDemoRequest request) {
        String messageText = resolveHelloMessage(request);
        try {
            AgentCard agentCard = fetchAgentCard();
            return runBlockingDemo(agentCard, messageText);
        } catch (Exception e) {
            log.error("Blocking demo failed", e);
            return SpringBootRestClientScenarioResponse.failure(
                    "blocking",
                    properties.serverUrl(),
                    messageText,
                    List.of(),
                    e.getMessage());
        }
    }

    public SpringBootRestClientScenarioResponse runStreamingDemo(SpringBootRestClientDemoRequest request) {
        String messageText = resolveStreamMessage(request);
        int timeoutSeconds = resolveStreamingTimeoutSeconds(request);
        try {
            AgentCard agentCard = fetchAgentCard();
            return runStreamingDemo(agentCard, messageText, timeoutSeconds);
        } catch (Exception e) {
            log.error("Streaming demo failed", e);
            return SpringBootRestClientScenarioResponse.failure(
                    "streaming",
                    properties.serverUrl(),
                    messageText,
                    List.of(),
                    e.getMessage());
        }
    }

    public SpringBootRestClientFullFlowResponse runFullFlow(SpringBootRestClientDemoRequest request) {
        try {
            AgentCard agentCard = fetchAgentCard();
            SpringBootRestClientScenarioResponse blocking = runBlockingDemo(agentCard, resolveHelloMessage(request));
            SpringBootRestClientScenarioResponse streaming = runStreamingDemo(
                    agentCard,
                    resolveStreamMessage(request),
                    resolveStreamingTimeoutSeconds(request));
            return SpringBootRestClientFullFlowResponse.success(properties.serverUrl(), agentCard, blocking, streaming);
        } catch (Exception e) {
            log.error("Full flow demo failed", e);
            return SpringBootRestClientFullFlowResponse.failure(properties.serverUrl(), e.getMessage());
        }
    }

    private SpringBootRestClientScenarioResponse runBlockingDemo(AgentCard agentCard, String messageText) throws Exception {
        List<String> events = new ArrayList<>();
        AtomicReference<String> receivedMessage = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        try (Client client = createClient(agentCard, false)) {
            Message message = A2A.toUserMessage(messageText);
            log.info("Running blocking demo with message: {}", messageText);
            client.sendMessage(message, List.of((event, card) -> {
                String eventDescription = describeEvent("blocking", event, card);
                events.add(eventDescription);
                log.info(eventDescription);
                if (event instanceof MessageEvent messageEvent) {
                    receivedMessage.set(extractText(messageEvent.getMessage()));
                }
            }), throwable -> {
                errorRef.set(throwable);
                log.error("Blocking transport callback reported an error", throwable);
            }, null);
        }

        if (errorRef.get() != null) {
            throw new IllegalStateException("Blocking demo failed", errorRef.get());
        }

        return SpringBootRestClientScenarioResponse.success(
                "blocking",
                properties.serverUrl(),
                agentCard,
                messageText,
                receivedMessage.get(),
                List.copyOf(events));
    }

    private SpringBootRestClientScenarioResponse runStreamingDemo(AgentCard agentCard, String messageText, int timeoutSeconds)
            throws Exception {
        List<String> events = new ArrayList<>();
        AtomicReference<String> receivedMessage = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch completionLatch = new CountDownLatch(1);

        try (Client client = createClient(agentCard, true)) {
            Message message = Message.builder()
                    .role(Message.Role.ROLE_USER)
                    .parts(List.of(new TextPart(messageText)))
                    .build();

            log.info("Running streaming demo with message: {}", messageText);
            client.sendMessage(message, List.of((event, card) -> {
                String eventDescription = describeEvent("streaming", event, card);
                events.add(eventDescription);
                log.info(eventDescription);
                if (event instanceof MessageEvent messageEvent) {
                    receivedMessage.set(extractText(messageEvent.getMessage()));
                    completionLatch.countDown();
                }
                if (event instanceof TaskEvent taskEvent && taskEvent.getTask().status().state().isFinal()) {
                    completionLatch.countDown();
                }
                if (event instanceof TaskUpdateEvent taskUpdateEvent
                        && taskUpdateEvent.getTask().status().state().isFinal()) {
                    completionLatch.countDown();
                }
            }), throwable -> {
                errorRef.set(throwable);
                completionLatch.countDown();
                log.error("Streaming transport callback reported an error", throwable);
            }, null);

            if (!completionLatch.await(timeoutSeconds, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for streaming response");
            }
        }

        if (errorRef.get() != null) {
            throw new IllegalStateException("Streaming demo failed", errorRef.get());
        }

        return SpringBootRestClientScenarioResponse.success(
                "streaming",
                properties.serverUrl(),
                agentCard,
                messageText,
                receivedMessage.get(),
                List.copyOf(events));
    }

    private Client createClient(AgentCard agentCard, boolean streaming) throws Exception {
        ClientConfig clientConfig = new ClientConfig.Builder()
                .setStreaming(streaming)
                .build();
        ClientBuilder builder = Client.builder(agentCard)
                .clientConfig(clientConfig)
                .withTransport(RestTransport.class, new RestTransportConfigBuilder());
        return builder.build();
    }

    private String resolveHelloMessage(SpringBootRestClientDemoRequest request) {
        if (request != null && request.helloMessage() != null && !request.helloMessage().isBlank()) {
            return request.helloMessage();
        }
        return properties.helloMessage();
    }

    private String resolveStreamMessage(SpringBootRestClientDemoRequest request) {
        if (request != null && request.streamMessage() != null && !request.streamMessage().isBlank()) {
            return request.streamMessage();
        }
        return properties.streamMessage();
    }

    private int resolveStreamingTimeoutSeconds(SpringBootRestClientDemoRequest request) {
        if (request != null && request.streamingTimeoutSeconds() != null && request.streamingTimeoutSeconds() > 0) {
            return request.streamingTimeoutSeconds();
        }
        return properties.streamingTimeoutSeconds();
    }

    private String describeEvent(String scenario, ClientEvent event, AgentCard agentCard) {
        if (event instanceof MessageEvent messageEvent) {
            return "[" + scenario + "] MessageEvent from " + agentCard.name() + ": " + extractText(messageEvent.getMessage());
        }
        if (event instanceof TaskEvent taskEvent) {
            return "[" + scenario + "] TaskEvent: taskId=" + taskEvent.getTask().id() + ", state=" + taskEvent.getTask().status().state();
        }
        if (event instanceof TaskUpdateEvent taskUpdateEvent) {
            return "[" + scenario + "] TaskUpdateEvent: taskId=" + taskUpdateEvent.getTask().id()
                    + ", state=" + taskUpdateEvent.getTask().status().state();
        }
        return "[" + scenario + "] Event received: " + event.getClass().getSimpleName();
    }

    private String extractText(Message message) {
        StringBuilder builder = new StringBuilder();
        if (message.parts() != null) {
            for (var part : message.parts()) {
                if (part instanceof TextPart textPart) {
                    builder.append(textPart.text());
                }
            }
        }
        return builder.toString();
    }
}
