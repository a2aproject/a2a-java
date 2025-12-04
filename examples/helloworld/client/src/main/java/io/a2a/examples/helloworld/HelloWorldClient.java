package io.a2a.examples.helloworld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.a2a.A2A;

import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.MessageEvent;
import io.a2a.client.http.A2ACardResolver;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfig;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;

/**
 * A simple example of using the A2A Java SDK to communicate with an A2A server.
 * This example is equivalent to the Python example provided in the A2A Python SDK.
 */
public class HelloWorldClient {

    private static final String SERVER_URL = "http://localhost:9999";
    private static final String MESSAGE_TEXT = "how much is 10 USD in INR?";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void main(String[] args) {
        try {
            AgentCard finalAgentCard = null;
            AgentCard publicAgentCard = new A2ACardResolver("http://localhost:9999").getAgentCard();
            System.out.println("Successfully fetched public agent card:");
            System.out.println(OBJECT_MAPPER.writeValueAsString(publicAgentCard));
            System.out.println("Using public agent card for client initialization (default).");
            finalAgentCard = publicAgentCard;

            if (publicAgentCard.supportsAuthenticatedExtendedCard()) {
                System.out.println("Public card supports authenticated extended card. Attempting to fetch from: " + SERVER_URL + "/agent/authenticatedExtendedCard");
                Map<String, String> authHeaders = new HashMap<>();
                authHeaders.put("Authorization", "Bearer dummy-token-for-extended-card");
                AgentCard extendedAgentCard = A2A.getAgentCard(SERVER_URL, "/agent/authenticatedExtendedCard", authHeaders);
                System.out.println("Successfully fetched authenticated extended agent card:");
                System.out.println(OBJECT_MAPPER.writeValueAsString(extendedAgentCard));
                System.out.println("Using AUTHENTICATED EXTENDED agent card for client initialization.");
                finalAgentCard = extendedAgentCard;
            } else {
                System.out.println("Public card does not indicate support for an extended card. Using public card.");
            }

            final CompletableFuture<String> messageResponse = new CompletableFuture<>();

            // Create consumers list for handling client events
            List<BiConsumer<ClientEvent, AgentCard>> consumers = new ArrayList<>();
            consumers.add((event, agentCard) -> {
                if (event instanceof MessageEvent messageEvent) {
                    Message responseMessage = messageEvent.getMessage();
                    StringBuilder textBuilder = new StringBuilder();
                    if (responseMessage.getParts() != null) {
                        for (Part<?> part : responseMessage.getParts()) {
                            if (part instanceof TextPart textPart) {
                                textBuilder.append(textPart.text());
                            }
                        }
                    }
                    messageResponse.complete(textBuilder.toString());
                } else {
                    System.out.println("Received client event: " + event.getClass().getSimpleName());
                }
            });

            // Create error handler for streaming errors
            Consumer<Throwable> streamingErrorHandler = (error) -> {
                System.err.println("Streaming error occurred: " + error.getMessage());
                error.printStackTrace();
                messageResponse.completeExceptionally(error);
            };

            Client client = Client
                    .builder(finalAgentCard)
                    .addConsumers(consumers)
                    .streamingErrorHandler(streamingErrorHandler)
                    .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
                    .build();

            Message message = A2A.toUserMessage(MESSAGE_TEXT); // the message ID will be automatically generated for you
            
            System.out.println("Sending message: " + MESSAGE_TEXT);
            client.sendMessage(message);
            System.out.println("Message sent successfully. Responses will be handled by the configured consumers.");

            try {
                String responseText = messageResponse.get();
                System.out.println("Response: " + responseText);
            } catch (Exception e) {
                System.err.println("Failed to get response: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

} 