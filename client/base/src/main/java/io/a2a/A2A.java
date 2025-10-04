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
     * Create a user message with additional configuration options.
     *
     * @param text the message text
     * @param contextId the context ID to use (optional)
     * @param taskId the task ID to use (optional)
     * @param parts the message parts (optional, defaults to a TextPart with the given text)
     * @param metadata the message metadata (optional)
     * @param referenceTaskIds the reference task IDs (optional)
     * @return the user message
     */
    public static Message createUserMessage(String text, String contextId, String taskId, 
                                           List<Part<?>> parts, Map<String, Object> metadata,
                                           List<String> referenceTaskIds) {
        return createMessage(text, Message.Role.USER, null, contextId, taskId, parts, metadata, referenceTaskIds);
    }

    /**
     * Create a user message with additional configuration options.
     *
     * @param text the message text
     * @param messageId the message ID to use
     * @param contextId the context ID to use (optional)
     * @param taskId the task ID to use (optional)
     * @param parts the message parts (optional, defaults to a TextPart with the given text)
     * @param metadata the message metadata (optional)
     * @param referenceTaskIds the reference task IDs (optional)
     * @return the user message
     */
    public static Message createUserMessage(String text, String messageId, String contextId, String taskId, 
                                           List<Part<?>> parts, Map<String, Object> metadata,
                                           List<String> referenceTaskIds) {
        return createMessage(text, Message.Role.USER, messageId, contextId, taskId, parts, metadata, referenceTaskIds);
    }

    /**
     * Create an agent message with additional configuration options.
     *
     * @param text the message text
     * @param contextId the context ID to use (optional)
     * @param taskId the task ID to use (optional)
     * @param parts the message parts (optional, defaults to a TextPart with the given text)
     * @param metadata the message metadata (optional)
     * @param referenceTaskIds the reference task IDs (optional)
     * @return the agent message
     */
    public static Message createAgentMessage(String text, String contextId, String taskId, 
                                            List<Part<?>> parts, Map<String, Object> metadata,
                                            List<String> referenceTaskIds) {
        return createMessage(text, Message.Role.AGENT, null, contextId, taskId, parts, metadata, referenceTaskIds);
    }

    /**
     * Create an agent message with additional configuration options.
     *
     * @param text the message text
     * @param messageId the message ID to use
     * @param contextId the context ID to use (optional)
     * @param taskId the task ID to use (optional)
     * @param parts the message parts (optional, defaults to a TextPart with the given text)
     * @param metadata the message metadata (optional)
     * @param referenceTaskIds the reference task IDs (optional)
     * @return the agent message
     */
    public static Message createAgentMessage(String text, String messageId, String contextId, String taskId, 
                                            List<Part<?>> parts, Map<String, Object> metadata,
                                            List<String> referenceTaskIds) {
        return createMessage(text, Message.Role.AGENT, messageId, contextId, taskId, parts, metadata, referenceTaskIds);
    }

    /**
     * Create a message with the specified role and additional configuration options.
     *
     * @param text the message text (used if parts is null)
     * @param role the message role
     * @param messageId the message ID to use (optional, generated if null)
     * @param contextId the context ID to use (optional)
     * @param taskId the task ID to use (optional)
     * @param parts the message parts (optional, defaults to a TextPart with the given text)
     * @param metadata the message metadata (optional)
     * @param referenceTaskIds the reference task IDs (optional)
     * @return the configured message
     */
    public static Message createMessage(String text, Message.Role role, String messageId, String contextId, 
                                       String taskId, List<Part<?>> parts, Map<String, Object> metadata,
                                       List<String> referenceTaskIds) {
        Message.Builder builder = new Message.Builder()
                .role(role)
                .messageId(messageId != null ? messageId : UUID.randomUUID().toString())
                .contextId(contextId)
                .taskId(taskId)
                .metadata(metadata)
                .referenceTaskIds(referenceTaskIds);
        
        if (parts != null && !parts.isEmpty()) {
            builder.parts(parts);
        } else {
            builder.parts(Collections.singletonList(new TextPart(text)));
        }
        
        return builder.build();
    }

    private static Message toMessage(String text, Message.Role role, String messageId) {
        Message.Builder messageBuilder = new Message.Builder()
                .role(role)
                .parts(Collections.singletonList(new TextPart(text)));
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
     * @throws A2AClientJSONError f the response body cannot be decoded as JSON or validated against the AgentCard schema
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
     * @throws A2AClientJSONError f the response body cannot be decoded as JSON or validated against the AgentCard schema
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
     * @throws A2AClientJSONError f the response body cannot be decoded as JSON or validated against the AgentCard schema
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
     * @throws A2AClientJSONError f the response body cannot be decoded as JSON or validated against the AgentCard schema
     */
    public static AgentCard getAgentCard(A2AHttpClient httpClient, String agentUrl, String relativeCardPath, Map<String, String> authHeaders) throws A2AClientError, A2AClientJSONError  {
        A2ACardResolver resolver = new A2ACardResolver(httpClient, agentUrl, relativeCardPath, authHeaders);
        return resolver.getAgentCard();
    }
}
