package io.a2a;

import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class A2ATest {

    @Test
    public void testToUserMessage() {
        String text = "Hello, world!";
        Message message = A2A.toUserMessage(text);
        
        assertEquals(Message.Role.USER, message.getRole());
        assertEquals(1, message.getParts().size());
        assertEquals(text, ((TextPart) message.getParts().get(0)).text());
        assertNotNull(message.getMessageId());
        assertNull(message.getContextId());
        assertNull(message.getTaskId());
    }

    @Test
    public void testToUserMessageWithId() {
        String text = "Hello, world!";
        String messageId = "test-message-id";
        Message message = A2A.toUserMessage(text, messageId);
        
        assertEquals(Message.Role.USER, message.getRole());
        assertEquals(messageId, message.getMessageId());
    }

    @Test
    public void testToAgentMessage() {
        String text = "Hello, I'm an agent!";
        Message message = A2A.toAgentMessage(text);
        
        assertEquals(Message.Role.AGENT, message.getRole());
        assertEquals(1, message.getParts().size());
        assertEquals(text, ((TextPart) message.getParts().get(0)).text());
        assertNotNull(message.getMessageId());
    }

    @Test
    public void testToAgentMessageWithId() {
        String text = "Hello, I'm an agent!";
        String messageId = "agent-message-id";
        Message message = A2A.toAgentMessage(text, messageId);
        
        assertEquals(Message.Role.AGENT, message.getRole());
        assertEquals(messageId, message.getMessageId());
    }

    @Test
    public void testCreateUserTextMessage() {
        String text = "User message with context";
        String contextId = "context-123";
        String taskId = "task-456";
        
        Message message = A2A.createUserTextMessage(text, contextId, taskId);
        
        assertEquals(Message.Role.USER, message.getRole());
        assertEquals(contextId, message.getContextId());
        assertEquals(taskId, message.getTaskId());
        assertEquals(1, message.getParts().size());
        assertEquals(text, ((TextPart) message.getParts().get(0)).text());
        assertNotNull(message.getMessageId());
        assertNull(message.getMetadata());
        assertNull(message.getReferenceTaskIds());
    }

    @Test
    public void testCreateUserTextMessageWithNullParams() {
        String text = "Simple user message";
        
        Message message = A2A.createUserTextMessage(text, null, null);
        
        assertEquals(Message.Role.USER, message.getRole());
        assertNull(message.getContextId());
        assertNull(message.getTaskId());
        assertEquals(1, message.getParts().size());
        assertEquals(text, ((TextPart) message.getParts().get(0)).text());
    }

    @Test
    public void testCreateAgentTextMessage() {
        String text = "Agent message with context";
        String contextId = "context-789";
        String taskId = "task-012";
        
        Message message = A2A.createAgentTextMessage(text, contextId, taskId);
        
        assertEquals(Message.Role.AGENT, message.getRole());
        assertEquals(contextId, message.getContextId());
        assertEquals(taskId, message.getTaskId());
        assertEquals(1, message.getParts().size());
        assertEquals(text, ((TextPart) message.getParts().get(0)).text());
        assertNotNull(message.getMessageId());
    }

    @Test
    public void testCreateAgentPartsMessage() {
        List<Part<?>> parts = Arrays.asList(
            new TextPart("Part 1"),
            new TextPart("Part 2")
        );
        String contextId = "context-parts";
        String taskId = "task-parts";
        
        Message message = A2A.createAgentPartsMessage(parts, contextId, taskId);
        
        assertEquals(Message.Role.AGENT, message.getRole());
        assertEquals(contextId, message.getContextId());
        assertEquals(taskId, message.getTaskId());
        assertEquals(2, message.getParts().size());
        assertEquals("Part 1", ((TextPart) message.getParts().get(0)).text());
        assertEquals("Part 2", ((TextPart) message.getParts().get(1)).text());
    }

    @Test
    public void testCreateAgentPartsMessageWithNullParts() {
        try {
            A2A.createAgentPartsMessage(null, "context", "task");
            org.junit.jupiter.api.Assertions.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Parts cannot be null or empty", e.getMessage());
        }
    }

    @Test
    public void testCreateAgentPartsMessageWithEmptyParts() {
        try {
            A2A.createAgentPartsMessage(Collections.emptyList(), "context", "task");
            org.junit.jupiter.api.Assertions.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Parts cannot be null or empty", e.getMessage());
        }
    }
}