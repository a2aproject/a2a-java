package io.a2a.examples.helloworld;

import static io.a2a.extras.opentelemetry.client.OpenTelemetryClientTransportWrapper.OTEL_TRACER_KEY;
import static io.a2a.extras.opentelemetry.client.propagation.OpenTelemetryClientPropagatorTransportWrapper.OTEL_OPEN_TELEMETRY_KEY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.a2a.A2A;

import io.a2a.client.Client;
import io.a2a.client.ClientBuilder;
import io.a2a.client.ClientEvent;
import io.a2a.client.MessageEvent;
import io.a2a.client.http.A2ACardResolver;
import io.a2a.client.transport.grpc.GrpcTransport;
import io.a2a.client.transport.grpc.GrpcTransportConfigBuilder;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfig;
import io.a2a.client.transport.rest.RestTransport;
import io.a2a.client.transport.rest.RestTransportConfig;
import io.a2a.client.transport.spi.ClientTransportConfig;
import io.a2a.jsonrpc.common.json.JsonUtil;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import java.util.function.Function;

/**
 * A simple example of using the A2A Java SDK to communicate with an A2A server.
 * This example is equivalent to the Python example provided in the A2A Python SDK.
 */
public class HelloWorldClient {

    private static final String SERVER_URL = "http://localhost:9999";
    private static final String MESSAGE_TEXT = "how much is 10 USD in INR?";

    public static void main(String[] args) {
        OpenTelemetrySdk openTelemetrySdk = null;
        try {
            AgentCard publicAgentCard = new A2ACardResolver("http://localhost:9999").getAgentCard();
            System.out.println("Successfully fetched public agent card:");
            System.out.println(JsonUtil.toJson(publicAgentCard));
            System.out.println("Using public agent card for client initialization (default).");
            AgentCard finalAgentCard = publicAgentCard;

            if (publicAgentCard.capabilities().extendedAgentCard()) {
                System.out.println("Public card supports authenticated extended card. Attempting to fetch from: " + SERVER_URL + "/ExtendedAgentCard");
                Map<String, String> authHeaders = new HashMap<>();
                authHeaders.put("Authorization", "Bearer dummy-token-for-extended-card");
                AgentCard extendedAgentCard = A2A.getAgentCard(SERVER_URL, "/ExtendedAgentCard", authHeaders);
                System.out.println("Successfully fetched authenticated extended agent card:");
                System.out.println(JsonUtil.toJson(extendedAgentCard));
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
                    if (responseMessage.parts() != null) {
                        for (Part<?> part : responseMessage.parts()) {
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

            if (Boolean.getBoolean("opentelemetry")) {
                openTelemetrySdk = initOpenTelemetry();
            }

            ClientBuilder clientBuilder = Client
                    .builder(finalAgentCard)
                    .addConsumers(consumers)
                    .streamingErrorHandler(streamingErrorHandler);
            configureTransport(clientBuilder, openTelemetrySdk);
             Client client = clientBuilder.build();

            Message message = A2A.toUserMessage(MESSAGE_TEXT); // the message ID will be automatically generated for you
            try {
                System.out.println("Sending message: " + MESSAGE_TEXT);
                client.sendMessage(message);
                System.out.println("Message sent successfully. Responses will be handled by the configured consumers.");

                String responseText = messageResponse.get();
                System.out.println("Response: " + responseText);
            } catch (Exception e) {
                System.err.println("Failed to get response: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Ensure OpenTelemetry SDK is properly shut down to export all pending spans
            if (openTelemetrySdk != null) {
                System.out.println("Shutting down OpenTelemetry SDK...");
                openTelemetrySdk.close();
                System.out.println("OpenTelemetry SDK shutdown complete.");
            }
        }
    }

    static OpenTelemetrySdk initOpenTelemetry() {
        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(
                        OtlpGrpcSpanExporter.builder()
                                .setEndpoint("http://localhost:5317")
                                .build()
                ).build())
                .setResource(Resource.getDefault().toBuilder()
                        .put("service.version", "1.0")
                        .put("service.name", "helloworld-client")
                        .build())
                .build();

        return OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();
    }
    private static void configureTransport(ClientBuilder clientBuilder, OpenTelemetrySdk openTelemetrySdk) {
        ClientTransportConfig transportConfig;
        switch(System.getProperty("quarkus.agentcard.protocol", "JSONRPC")) {
            case "GRPC":
                Function<String, Channel> channelFactory = url -> {
                    // Extract "localhost:9999" from "http://localhost:9999"
                    String target = url.replaceAll("^https?://", "");
                    return ManagedChannelBuilder.forTarget(target)
                            .usePlaintext() // No TLS
                            .build();
                };
                transportConfig = new GrpcTransportConfigBuilder().channelFactory(channelFactory).build();
                updateTransportConfig(transportConfig, openTelemetrySdk);
                clientBuilder.withTransport(GrpcTransport.class, transportConfig);
                break;
            case "HTTP+JSON":
                transportConfig = new RestTransportConfig();
                updateTransportConfig(transportConfig, openTelemetrySdk);
                clientBuilder.withTransport(RestTransport.class, transportConfig);
                break;
            case "JSONRPC":
            default:
                transportConfig = new JSONRPCTransportConfig();
                updateTransportConfig(transportConfig, openTelemetrySdk);
                clientBuilder.withTransport(JSONRPCTransport.class, transportConfig);
                break;
        }
    }

    private static void updateTransportConfig(ClientTransportConfig transportConfig, OpenTelemetrySdk openTelemetrySdk) {
        if (openTelemetrySdk != null) {
            Map<String, Object> parameters = new HashMap<>(transportConfig.getParameters());
            parameters.put(OTEL_TRACER_KEY, openTelemetrySdk.getTracer("helloworld-client"));
            parameters.put(OTEL_OPEN_TELEMETRY_KEY, openTelemetrySdk);
            transportConfig.setParameters(parameters);
        }
    }
}
