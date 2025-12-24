package io.a2a.client;

import io.a2a.spec.Message;

/**
 * A client event containing an agent's message response.
 * <p>
 * MessageEvent represents a complete message from the agent, typically containing text, images,
 * or other content parts. This event type is used in two scenarios:
 * <ol>
 *   <li><b>Simple blocking responses:</b> When the agent completes a request immediately and
 *       returns a message without task tracking</li>
 *   <li><b>Legacy protocol support:</b> Older agents may return messages instead of task updates</li>
 * </ol>
 * <p>
 * <b>Example usage:</b>
 * <pre>{@code
 * client.addConsumer((event, agentCard) -> {
 *     if (event instanceof MessageEvent me) {
 *         Message msg = me.getMessage();
 *         
 *         // Extract text content
 *         String text = msg.parts().stream()
 *             .filter(p -> p instanceof TextPart)
 *             .map(p -> ((TextPart) p).text())
 *             .collect(Collectors.joining());
 *         
 *         System.out.println("Agent response: " + text);
 *         
 *         // Check for images
 *         msg.parts().stream()
 *             .filter(p -> p instanceof ImagePart)
 *             .forEach(p -> System.out.println("Image: " + ((ImagePart) p).url()));
 *     }
 * });
 * }</pre>
 * <p>
 * <b>Message structure:</b> The contained {@link Message} includes:
 * <ul>
 *   <li><b>role:</b> AGENT (indicating it's from the agent)</li>
 *   <li><b>parts:</b> List of content parts (text, images, files, etc.)</li>
 *   <li><b>contextId:</b> Optional session identifier</li>
 *   <li><b>taskId:</b> Optional associated task ID</li>
 *   <li><b>metadata:</b> Optional custom metadata from the agent</li>
 * </ul>
 * <p>
 * <b>Streaming vs blocking:</b> In streaming mode with task tracking, you're more likely to
 * receive {@link TaskUpdateEvent} instances instead of MessageEvent. MessageEvent is primarily
 * used for simple, synchronous request-response interactions.
 *
 * @see ClientEvent
 * @see Message
 * @see io.a2a.spec.Part
 * @see io.a2a.spec.TextPart
 */
public final class MessageEvent implements ClientEvent {

    private final Message message;

    /**
     * Create a message event.
     *
     * @param message the message received from the agent (required)
     */
    public MessageEvent(Message message) {
        this.message = message;
    }

    /**
     * Get the message contained in this event.
     *
     * @return the agent's message
     */
    public Message getMessage() {
        return message;
    }

    @Override
    public String toString() {
        String messageAsString = "{"
                + "role=" + message.role()
                + ", parts=" + message.parts()
                + ", messageId=" + message.messageId()
                + ", contextId=" + message.contextId()
                + ", taskId=" + message.taskId()
                + ", metadata=" + message.metadata()
                + ", kind=" + message.kind()
                + ", referenceTaskIds=" + message.referenceTaskIds()
                + ", extensions=" + message.extensions() + '}';
        return "MessageEvent{" + "message=" + messageAsString + '}';
    }
}
