package io.a2a;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.a2a.client.http.A2ACardResolver;
import io.a2a.client.http.A2AHttpClient;
import io.a2a.client.http.A2AHttpClientFactory;
import io.a2a.spec.A2AClientError;
import io.a2a.spec.A2AClientJSONError;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;


/**
 * Utility class providing convenience methods for working with the A2A Protocol.
 * <p>
 * This class offers static helper methods for common A2A operations:
 * <ul>
 *   <li><b>Message creation:</b> Simplified construction of user and agent messages</li>
 *   <li><b>Agent card retrieval:</b> Fetching agent metadata from URLs</li>
 * </ul>
 * <p>
 * These utilities simplify client code by providing concise alternatives to the builder
 * APIs for routine operations.
 * <p>
 * <b>Example usage:</b>
 * <pre>{@code
 * // Get agent card
 * AgentCard card = A2A.getAgentCard("http://localhost:9999");
 *
 * // Create and send a user message
 * Message userMsg = A2A.toUserMessage("What's the weather today?");
 * client.sendMessage(userMsg);
 *
 * // Create a message with context and task IDs
 * Message contextMsg = A2A.createUserTextMessage(
 *     "Continue the conversation",
 *     "session-123",  // contextId
 *     "task-456"      // taskId
 * );
 * client.sendMessage(contextMsg);
 * }</pre>
 *
 * @see Message
 * @see AgentCard
 * @see io.a2a.client.Client
 */
public class A2A {

    /**
     * Create a simple user message from text.
     * <p>
     * This is the most common way to create messages when sending requests to agents.
     * The message will have:
     * <ul>
     *   <li>role: USER</li>
     *   <li>parts: Single {@link io.a2a.spec.TextPart} with the provided text</li>
     *   <li>Auto-generated message ID</li>
     * </ul>
     * <p>
     * Example:
     * <pre>{@code
     * Message msg = A2A.toUserMessage("Tell me a joke");
     * client.sendMessage(msg);
     * }</pre>
     *
     * @param text the message text (required)
     * @return a user message with the specified text
     * @see #toUserMessage(String, String)
     * @see #createUserTextMessage(String, String, String)
     */
    public static Message toUserMessage(String text) {
        return toMessage(text, Message.Role.USER, null);
    }

    /**
     * Create a user message from text with a specific message ID.
     * <p>
     * Use this when you need to control the message ID for tracking or correlation purposes.
     * <p>
     * Example:
     * <pre>{@code
     * String messageId = UUID.randomUUID().toString();
     * Message msg = A2A.toUserMessage("Process this request", messageId);
     * // Store messageId for later correlation
     * client.sendMessage(msg);
     * }</pre>
     *
     * @param text the message text (required)
     * @param messageId the message ID to use
     * @return a user message with the specified text and ID
     * @see #toUserMessage(String)
     */
    public static Message toUserMessage(String text, String messageId) {
        return toMessage(text, Message.Role.USER, messageId);
    }

    /**
     * Create a simple agent message from text.
     * <p>
     * This is typically used in testing or when constructing agent responses programmatically.
     * Most client applications receive agent messages via {@link io.a2a.client.MessageEvent}
     * rather than creating them manually.
     * <p>
     * Example:
     * <pre>{@code
     * // Testing scenario
     * Message agentResponse = A2A.toAgentMessage("Here's the answer: 42");
     * }</pre>
     *
     * @param text the message text (required)
     * @return an agent message with the specified text
     * @see #toAgentMessage(String, String)
     */
    public static Message toAgentMessage(String text) {
        return toMessage(text, Message.Role.AGENT, null);
    }

    /**
     * Create an agent message from text with a specific message ID.
     * <p>
     * Example:
     * <pre>{@code
     * Message agentResponse = A2A.toAgentMessage("Processing complete", "msg-789");
     * }</pre>
     *
     * @param text the message text (required)
     * @param messageId the message ID to use
     * @return an agent message with the specified text and ID
     */
    public static Message toAgentMessage(String text, String messageId) {
        return toMessage(text, Message.Role.AGENT, messageId);
    }

    /**
     * Create a user message with text content and optional context and task IDs.
     * <p>
     * This method is useful when continuing a conversation or working with a specific task:
     * <ul>
     *   <li><b>contextId:</b> Links message to a conversation session</li>
     *   <li><b>taskId:</b> Associates message with an existing task</li>
     * </ul>
     * <p>
     * Example - continuing a conversation:
     * <pre>{@code
     * // First message creates context
     * Message msg1 = A2A.toUserMessage("What's your name?");
     * client.sendMessage(msg1);
     * String contextId = ...; // Get from response
     *
     * // Follow-up message uses contextId
     * Message msg2 = A2A.createUserTextMessage(
     *     "What else can you do?",
     *     contextId,
     *     null  // no specific task
     * );
     * client.sendMessage(msg2);
     * }</pre>
     * <p>
     * Example - adding to an existing task:
     * <pre>{@code
     * Message msg = A2A.createUserTextMessage(
     *     "Add this information too",
     *     "session-123",
     *     "task-456"  // Continue working on this task
     * );
     * client.sendMessage(msg);
     * }</pre>
     *
     * @param text the message text (required)
     * @param contextId the context ID to use (optional)
     * @param taskId the task ID to use (optional)
     * @return a user message with the specified text, context, and task IDs
     * @see #createAgentTextMessage(String, String, String)
     * @see Message#contextId()
     * @see Message#taskId()
     */
    public static Message createUserTextMessage(String text, String contextId, String taskId) {
        return toMessage(text, Message.Role.USER, null, contextId, taskId);
    }

    /**
     * Create an agent message with text content and optional context and task IDs.
     * <p>
     * This is typically used in testing or when constructing agent responses programmatically.
     *
     * @param text the message text (required)
     * @param contextId the context ID to use (optional)
     * @param taskId the task ID to use (optional)
     * @return an agent message with the specified text, context, and task IDs
     * @see #createUserTextMessage(String, String, String)
     */
    public static Message createAgentTextMessage(String text, String contextId, String taskId) {
        return toMessage(text, Message.Role.AGENT, null, contextId, taskId);
    }

    /**
     * Create an agent message with custom parts and optional context and task IDs.
     * <p>
     * This method allows creating messages with multiple parts (text, images, files, etc.)
     * instead of just simple text. Useful for complex agent responses or testing.
     * <p>
     * Example - message with text and image:
     * <pre>{@code
     * List<Part<?>> parts = List.of(
     *     new TextPart("Here's a chart of the data:"),
     *     new ImagePart("https://example.com/chart.png", "Chart showing sales data")
     * );
     * Message msg = A2A.createAgentPartsMessage(parts, "session-123", "task-456");
     * }</pre>
     *
     * @param parts the message parts (required, must not be empty)
     * @param contextId the context ID to use (optional)
     * @param taskId the task ID to use (optional)
     * @return an agent message with the specified parts, context, and task IDs
     * @throws IllegalArgumentException if parts is null or empty
     * @see io.a2a.spec.Part
     * @see io.a2a.spec.TextPart
     */
    public static Message createAgentPartsMessage(List<Part<?>> parts, String contextId, String taskId) {
        if (parts == null || parts.isEmpty()) {
            throw new IllegalArgumentException("Parts cannot be null or empty");
        }
        return toMessage(parts, Message.Role.AGENT, null, contextId, taskId);
    }

    private static Message toMessage(String text, Message.Role role, String messageId) {
        return toMessage(text, role, messageId, null, null);
    }

    private static Message toMessage(String text, Message.Role role, String messageId, String contextId, String taskId) {
        Message.Builder messageBuilder = Message.builder()
                .role(role)
                .parts(Collections.singletonList(new TextPart(text)))
                .contextId(contextId)
                .taskId(taskId);
        if (messageId != null) {
            messageBuilder.messageId(messageId);
        }
        return messageBuilder.build();
    }

    private static Message toMessage(List<Part<?>> parts, Message.Role role, String messageId, String contextId, String taskId) {
        Message.Builder messageBuilder = Message.builder()
                .role(role)
                .parts(parts)
                .contextId(contextId)
                .taskId(taskId);
        if (messageId != null) {
            messageBuilder.messageId(messageId);
        }
        return messageBuilder.build();
    }

    /**
     * Retrieve the agent card for an A2A agent.
     * <p>
     * This is the standard way to discover an agent's capabilities before creating a client.
     * The agent card is fetched from the well-known endpoint: {@code <agentUrl>/.well-known/agent-card.json}
     * <p>
     * Example:
     * <pre>{@code
     * // Get agent card
     * AgentCard card = A2A.getAgentCard("http://localhost:9999");
     *
     * // Check capabilities
     * System.out.println("Agent: " + card.name());
     * System.out.println("Supports streaming: " + card.capabilities().streaming());
     *
     * // Create client
     * Client client = Client.builder(card)
     *     .withTransport(...)
     *     .build();
     * }</pre>
     *
     * @param agentUrl the base URL for the agent whose agent card we want to retrieve
     * @return the agent card
     * @throws io.a2a.spec.A2AClientError if an HTTP error occurs fetching the card
     * @throws io.a2a.spec.A2AClientJSONError if the response body cannot be decoded as JSON or validated against the AgentCard schema
     * @see #getAgentCard(A2AHttpClient, String)
     * @see #getAgentCard(String, String, java.util.Map)
     * @see AgentCard
     */
    public static AgentCard getAgentCard(String agentUrl) throws A2AClientError, A2AClientJSONError {
        return getAgentCard(A2AHttpClientFactory.create(), agentUrl);
    }

    /**
     * Retrieve the agent card using a custom HTTP client.
     * <p>
     * Use this variant when you need to customize HTTP behavior (timeouts, SSL configuration,
     * connection pooling, etc.).
     * <p>
     * Example:
     * <pre>{@code
     * A2AHttpClient customClient = new CustomHttpClient()
     *     .withTimeout(Duration.ofSeconds(10))
     *     .withSSLContext(mySSLContext);
     *
     * AgentCard card = A2A.getAgentCard(customClient, "https://secure-agent.com");
     * }</pre>
     *
     * @param httpClient the http client to use
     * @param agentUrl the base URL for the agent whose agent card we want to retrieve
     * @return the agent card
     * @throws io.a2a.spec.A2AClientError if an HTTP error occurs fetching the card
     * @throws io.a2a.spec.A2AClientJSONError if the response body cannot be decoded as JSON or validated against the AgentCard schema
     * @see io.a2a.client.http.A2AHttpClient
     */
    public static AgentCard getAgentCard(A2AHttpClient httpClient, String agentUrl) throws A2AClientError, A2AClientJSONError  {
        return getAgentCard(httpClient, agentUrl, null, null);
    }

    /**
     * Retrieve the agent card with custom path and authentication.
     * <p>
     * Use this variant when:
     * <ul>
     *   <li>The agent card is at a non-standard location</li>
     *   <li>Authentication is required to access the agent card</li>
     * </ul>
     * <p>
     * Example with authentication:
     * <pre>{@code
     * Map<String, String> authHeaders = Map.of(
     *     "Authorization", "Bearer my-api-token",
     *     "X-API-Key", "my-api-key"
     * );
     *
     * AgentCard card = A2A.getAgentCard(
     *     "https://secure-agent.com",
     *     null,  // Use default path
     *     authHeaders
     * );
     * }</pre>
     * <p>
     * Example with custom path:
     * <pre>{@code
     * AgentCard card = A2A.getAgentCard(
     *     "https://agent.com",
     *     "api/v2/agent-info",  // Custom path
     *     null  // No auth needed
     * );
     * // Fetches from: https://agent.com/api/v2/agent-info
     * }</pre>
     *
     * @param agentUrl the base URL for the agent whose agent card we want to retrieve
     * @param relativeCardPath optional path to the agent card endpoint relative to the base
     *                         agent URL, defaults to ".well-known/agent-card.json"
     * @param authHeaders the HTTP authentication headers to use
     * @return the agent card
     * @throws io.a2a.spec.A2AClientError if an HTTP error occurs fetching the card
     * @throws io.a2a.spec.A2AClientJSONError if the response body cannot be decoded as JSON or validated against the AgentCard schema
     */
    public static AgentCard getAgentCard(String agentUrl, String relativeCardPath, Map<String, String> authHeaders) throws A2AClientError, A2AClientJSONError {
        return getAgentCard(A2AHttpClientFactory.create(), agentUrl, relativeCardPath, authHeaders);
    }

    /**
     * Retrieve the agent card with full customization options.
     * <p>
     * This is the most flexible variant, allowing customization of:
     * <ul>
     *   <li>HTTP client implementation</li>
     *   <li>Agent card endpoint path</li>
     *   <li>Authentication headers</li>
     * </ul>
     * <p>
     * Example:
     * <pre>{@code
     * A2AHttpClient customClient = new CustomHttpClient();
     * Map<String, String> authHeaders = Map.of("Authorization", "Bearer token");
     *
     * AgentCard card = A2A.getAgentCard(
     *     customClient,
     *     "https://agent.com",
     *     "custom/agent-card",
     *     authHeaders
     * );
     * }</pre>
     *
     * @param httpClient the http client to use
     * @param agentUrl the base URL for the agent whose agent card we want to retrieve
     * @param relativeCardPath optional path to the agent card endpoint relative to the base
     *                         agent URL, defaults to ".well-known/agent-card.json"
     * @param authHeaders the HTTP authentication headers to use
     * @return the agent card
     * @throws io.a2a.spec.A2AClientError if an HTTP error occurs fetching the card
     * @throws io.a2a.spec.A2AClientJSONError if the response body cannot be decoded as JSON or validated against the AgentCard schema
     */
    public static AgentCard getAgentCard(A2AHttpClient httpClient, String agentUrl, String relativeCardPath, Map<String, String> authHeaders) throws A2AClientError, A2AClientJSONError  {
        A2ACardResolver resolver = new A2ACardResolver(httpClient, agentUrl, "", relativeCardPath, authHeaders);
        return resolver.getAgentCard();
    }
}
