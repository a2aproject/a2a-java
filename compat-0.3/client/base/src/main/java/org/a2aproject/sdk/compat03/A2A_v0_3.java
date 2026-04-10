package org.a2aproject.sdk.compat03;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.a2aproject.sdk.compat03.client.http.A2ACardResolver_v0_3;
import org.a2aproject.sdk.compat03.client.http.A2AHttpClient_v0_3;
import org.a2aproject.sdk.compat03.client.http.JdkA2AHttpClient_v0_3;
import org.a2aproject.sdk.compat03.spec.A2AClientError_v0_3;
import org.a2aproject.sdk.compat03.spec.A2AClientJSONError_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.compat03.spec.Part_v0_3;
import org.a2aproject.sdk.compat03.spec.TextPart_v0_3;


/**
 * Constants and utility methods related to the A2A protocol.
 */
public class A2A_v0_3 {

    /**
     * Convert the given text to a user message.
     *
     * @param text the message text
     * @return the user message
     */
    public static Message_v0_3 toUserMessage(String text) {
        return toMessage(text, Message_v0_3.Role.USER, null);
    }

    /**
     * Convert the given text to a user message.
     *
     * @param text the message text
     * @param messageId the message ID to use
     * @return the user message
     */
    public static Message_v0_3 toUserMessage(String text, String messageId) {
        return toMessage(text, Message_v0_3.Role.USER, messageId);
    }

    /**
     * Convert the given text to an agent message.
     *
     * @param text the message text
     * @return the agent message
     */
    public static Message_v0_3 toAgentMessage(String text) {
        return toMessage(text, Message_v0_3.Role.AGENT, null);
    }

    /**
     * Convert the given text to an agent message.
     *
     * @param text the message text
     * @param messageId the message ID to use
     * @return the agent message
     */
    public static Message_v0_3 toAgentMessage(String text, String messageId) {
        return toMessage(text, Message_v0_3.Role.AGENT, messageId);
    }

    /**
     * Create a user message with text content and optional context and task IDs.
     *
     * @param text the message text (required)
     * @param contextId the context ID to use (optional)
     * @param taskId the task ID to use (optional)
     * @return the user message
     */
    public static Message_v0_3 createUserTextMessage(String text, String contextId, String taskId) {
        return toMessage(text, Message_v0_3.Role.USER, null, contextId, taskId);
    }

    /**
     * Create an agent message with text content and optional context and task IDs.
     *
     * @param text the message text (required)
     * @param contextId the context ID to use (optional)
     * @param taskId the task ID to use (optional)
     * @return the agent message
     */
    public static Message_v0_3 createAgentTextMessage(String text, String contextId, String taskId) {
        return toMessage(text, Message_v0_3.Role.AGENT, null, contextId, taskId);
    }

    /**
     * Create an agent message with custom parts and optional context and task IDs.
     *
     * @param parts the message parts (required)
     * @param contextId the context ID to use (optional)
     * @param taskId the task ID to use (optional)
     * @return the agent message
     */
    public static Message_v0_3 createAgentPartsMessage(List<Part_v0_3<?>> parts, String contextId, String taskId) {
        if (parts == null || parts.isEmpty()) {
            throw new IllegalArgumentException("Parts cannot be null or empty");
        }
        return toMessage(parts, Message_v0_3.Role.AGENT, null, contextId, taskId);
    }

    private static Message_v0_3 toMessage(String text, Message_v0_3.Role role, String messageId) {
        return toMessage(text, role, messageId, null, null);
    }

    private static Message_v0_3 toMessage(String text, Message_v0_3.Role role, String messageId, String contextId, String taskId) {
        Message_v0_3.Builder messageBuilder = new Message_v0_3.Builder()
                .role(role)
                .parts(Collections.singletonList(new TextPart_v0_3(text)))
                .contextId(contextId)
                .taskId(taskId);
        if (messageId != null) {
            messageBuilder.messageId(messageId);
        }
        return messageBuilder.build();
    }

    private static Message_v0_3 toMessage(List<Part_v0_3<?>> parts, Message_v0_3.Role role, String messageId, String contextId, String taskId) {
        Message_v0_3.Builder messageBuilder = new Message_v0_3.Builder()
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
     * @throws A2AClientError_v0_3 If an HTTP error occurs fetching the card
     * @throws A2AClientJSONError_v0_3 If the response body cannot be decoded as JSON or validated against the AgentCard schema
     */
    public static AgentCard_v0_3 getAgentCard(String agentUrl) throws A2AClientError_v0_3, A2AClientJSONError_v0_3 {
        return getAgentCard(new JdkA2AHttpClient_v0_3(), agentUrl);
    }

    /**
     * Get the agent card for an A2A agent.
     *
     * @param httpClient the http client to use
     * @param agentUrl the base URL for the agent whose agent card we want to retrieve
     * @return the agent card
     * @throws A2AClientError_v0_3 If an HTTP error occurs fetching the card
     * @throws A2AClientJSONError_v0_3 If the response body cannot be decoded as JSON or validated against the AgentCard schema
     */
    public static AgentCard_v0_3 getAgentCard(A2AHttpClient_v0_3 httpClient, String agentUrl) throws A2AClientError_v0_3, A2AClientJSONError_v0_3 {
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
     * @throws A2AClientError_v0_3 If an HTTP error occurs fetching the card
     * @throws A2AClientJSONError_v0_3 If the response body cannot be decoded as JSON or validated against the AgentCard schema
     */
    public static AgentCard_v0_3 getAgentCard(String agentUrl, String relativeCardPath, Map<String, String> authHeaders) throws A2AClientError_v0_3, A2AClientJSONError_v0_3 {
        return getAgentCard(new JdkA2AHttpClient_v0_3(), agentUrl, relativeCardPath, authHeaders);
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
     * @throws A2AClientError_v0_3 If an HTTP error occurs fetching the card
     * @throws A2AClientJSONError_v0_3 If the response body cannot be decoded as JSON or validated against the AgentCard schema
     */
    public static AgentCard_v0_3 getAgentCard(A2AHttpClient_v0_3 httpClient, String agentUrl, String relativeCardPath, Map<String, String> authHeaders) throws A2AClientError_v0_3, A2AClientJSONError_v0_3 {
        A2ACardResolver_v0_3 resolver = new A2ACardResolver_v0_3(httpClient, agentUrl, relativeCardPath, authHeaders);
        return resolver.getAgentCard();
    }
}
