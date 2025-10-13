package io.a2a;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.a2a.client.http.A2ACardResolver;
import io.a2a.client.http.A2AHttpClient;
import io.a2a.client.http.JdkA2AHttpClient;
import io.a2a.spec.A2AClientError;
import io.a2a.spec.A2AClientJSONError;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;


/**
 * Constants and utility methods related to the A2A protocol.
 */
public class A2A {

    /**
     * Convert the given text to a user message.
     *
     * @param text the message text
     * @return the user message
     */
    public static Message toUserMessage(String text) {
        return toMessage(text, Message.Role.USER, null);
    }

    /**
     * Convert the given text to a user message.
     *
     * @param text the message text
     * @param messageId the message ID to use
     * @return the user message
     */
    public static Message toUserMessage(String text, String messageId) {
        return toMessage(text, Message.Role.USER, messageId);
    }

    /**
     * Convert the given text to an agent message.
     *
     * @param text the message text
     * @return the agent message
     */
    public static Message toAgentMessage(String text) {
        return toMessage(text, Message.Role.AGENT, null);
    }

    /**
     * Convert the given text to an agent message.
     *
     * @param text the message text
     * @param messageId the message ID to use
     * @return the agent message
     */
    public static Message toAgentMessage(String text, String messageId) {
        return toMessage(text, Message.Role.AGENT, messageId);
    }

    /**
     * Create a user message with text content and optional context and task IDs.
     *
     * @param text the message text (required)
     * @param contextId the context ID to use (optional)
     * @param taskId the task ID to use (optional)
     * @return the user message
     */
    public static Message createUserTextMessage(String text, String contextId, String taskId) {
        return toMessage(text, Message.Role.USER, null, contextId, taskId);
    }

    /**
     * Create an agent message with text content and optional context and task IDs.
     *
     * @param text the message text (required)
     * @param contextId the context ID to use (optional)
     * @param taskId the task ID to use (optional)
     * @return the agent message
     */
    public static Message createAgentTextMessage(String text, String contextId, String taskId) {
        return toMessage(text, Message.Role.AGENT, null, contextId, taskId);
    }

    /**
     * Create an agent message with custom parts and optional context and task IDs.
     *
     * @param parts the message parts (required)
     * @param contextId the context ID to use (optional)
     * @param taskId the task ID to use (optional)
     * @return the agent message
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
        Message.Builder messageBuilder = new Message.Builder()
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
        Message.Builder messageBuilder = new Message.Builder()
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
     * Get the agent card for an A2A agent.
     *
     * @param agentUrl the base URL for the agent whose agent card we want to retrieve
     * @return the agent card
     * @throws A2AClientError If an HTTP error occurs fetching the card
     * @throws A2AClientJSONError If the response body cannot be decoded as JSON or validated against the AgentCard schema
     */
    public static AgentCard getAgentCard(String agentUrl) throws A2AClientError, A2AClientJSONError {
        return getAgentCard(new JdkA2AHttpClient(), agentUrl);
    }

    /**
     * Get the agent card for an A2A agent.
     *
     * @param httpClient the http client to use
     * @param agentUrl the base URL for the agent whose agent card we want to retrieve
     * @return the agent card
     * @throws A2AClientError If an HTTP error occurs fetching the card
     * @throws A2AClientJSONError If the response body cannot be decoded as JSON or validated against the AgentCard schema
     */
    public static AgentCard getAgentCard(A2AHttpClient httpClient, String agentUrl) throws A2AClientError, A2AClientJSONError  {
        return getAgentCard(httpClient, agentUrl, null, null);
    }

    /**
     * Get the agent card for an A2A agent.
     *
     * @param agentUrl the base URL for the agent whose agent card we want to retrieve
     * @param relativeCardPath optional path to the agent card endpoint relative to the base
     *                         agent URL, defaults to ".well-known/agent-card.json"
     * @param authHeaders the HTTP authentication headers to use
     * @return the agent card
     * @throws A2AClientError If an HTTP error occurs fetching the card
     * @throws A2AClientJSONError If the response body cannot be decoded as JSON or validated against the AgentCard schema
     */
    public static AgentCard getAgentCard(String agentUrl, String relativeCardPath, Map<String, String> authHeaders) throws A2AClientError, A2AClientJSONError {
        return getAgentCard(new JdkA2AHttpClient(), agentUrl, relativeCardPath, authHeaders);
    }

    /**
     * Get the agent card for an A2A agent.
     *
     * @param httpClient the http client to use
     * @param agentUrl the base URL for the agent whose agent card we want to retrieve
     * @param relativeCardPath optional path to the agent card endpoint relative to the base
     *                         agent URL, defaults to ".well-known/agent-card.json"
     * @param authHeaders the HTTP authentication headers to use
     * @return the agent card
     * @throws A2AClientError If an HTTP error occurs fetching the card
     * @throws A2AClientJSONError If the response body cannot be decoded as JSON or validated against the AgentCard schema
     */
    public static AgentCard getAgentCard(A2AHttpClient httpClient, String agentUrl, String relativeCardPath, Map<String, String> authHeaders) throws A2AClientError, A2AClientJSONError  {
        A2ACardResolver resolver = new A2ACardResolver(httpClient, agentUrl, relativeCardPath, authHeaders);
        return resolver.getAgentCard();
    }
}
