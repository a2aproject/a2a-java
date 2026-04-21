package org.a2aproject.sdk.compat03;

import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.compat03.spec.Part_v0_3;
import org.a2aproject.sdk.compat03.spec.TextPart_v0_3;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class A2A_v0_3_Test {

    @Test
    public void testToUserMessage() {
        String text = "Hello, world!";
        Message_v0_3 message = A2A_v0_3.toUserMessage(text);
        
        assertEquals(Message_v0_3.Role.USER, message.getRole());
        assertEquals(1, message.getParts().size());
        assertEquals(text, ((TextPart_v0_3) message.getParts().get(0)).getText());
        assertNotNull(message.getMessageId());
        assertNull(message.getContextId());
        assertNull(message.getTaskId());
    }

    @Test
    public void testToUserMessageWithId() {
        String text = "Hello, world!";
        String messageId = "test-message-id";
        Message_v0_3 message = A2A_v0_3.toUserMessage(text, messageId);
        
        assertEquals(Message_v0_3.Role.USER, message.getRole());
        assertEquals(messageId, message.getMessageId());
    }

    @Test
    public void testToAgentMessage() {
        String text = "Hello, I'm an agent!";
        Message_v0_3 message = A2A_v0_3.toAgentMessage(text);
        
        assertEquals(Message_v0_3.Role.AGENT, message.getRole());
        assertEquals(1, message.getParts().size());
        assertEquals(text, ((TextPart_v0_3) message.getParts().get(0)).getText());
        assertNotNull(message.getMessageId());
    }

    @Test
    public void testToAgentMessageWithId() {
        String text = "Hello, I'm an agent!";
        String messageId = "agent-message-id";
        Message_v0_3 message = A2A_v0_3.toAgentMessage(text, messageId);
        
        assertEquals(Message_v0_3.Role.AGENT, message.getRole());
        assertEquals(messageId, message.getMessageId());
    }

    @Test
    public void testCreateUserTextMessage() {
        String text = "User message with context";
        String contextId = "context-123";
        String taskId = "task-456";
        
        Message_v0_3 message = A2A_v0_3.createUserTextMessage(text, contextId, taskId);
        
        assertEquals(Message_v0_3.Role.USER, message.getRole());
        assertEquals(contextId, message.getContextId());
        assertEquals(taskId, message.getTaskId());
        assertEquals(1, message.getParts().size());
        assertEquals(text, ((TextPart_v0_3) message.getParts().get(0)).getText());
        assertNotNull(message.getMessageId());
        assertNull(message.getMetadata());
        assertNull(message.getReferenceTaskIds());
    }

    @Test
    public void testCreateUserTextMessageWithNullParams() {
        String text = "Simple user message";
        
        Message_v0_3 message = A2A_v0_3.createUserTextMessage(text, null, null);
        
        assertEquals(Message_v0_3.Role.USER, message.getRole());
        assertNull(message.getContextId());
        assertNull(message.getTaskId());
        assertEquals(1, message.getParts().size());
        assertEquals(text, ((TextPart_v0_3) message.getParts().get(0)).getText());
    }

    @Test
    public void testCreateAgentTextMessage() {
        String text = "Agent message with context";
        String contextId = "context-789";
        String taskId = "task-012";
        
        Message_v0_3 message = A2A_v0_3.createAgentTextMessage(text, contextId, taskId);
        
        assertEquals(Message_v0_3.Role.AGENT, message.getRole());
        assertEquals(contextId, message.getContextId());
        assertEquals(taskId, message.getTaskId());
        assertEquals(1, message.getParts().size());
        assertEquals(text, ((TextPart_v0_3) message.getParts().get(0)).getText());
        assertNotNull(message.getMessageId());
    }

    @Test
    public void testCreateAgentPartsMessage() {
        List<Part_v0_3<?>> parts = Arrays.asList(
            new TextPart_v0_3("Part 1"),
            new TextPart_v0_3("Part 2")
        );
        String contextId = "context-parts";
        String taskId = "task-parts";
        
        Message_v0_3 message = A2A_v0_3.createAgentPartsMessage(parts, contextId, taskId);
        
        assertEquals(Message_v0_3.Role.AGENT, message.getRole());
        assertEquals(contextId, message.getContextId());
        assertEquals(taskId, message.getTaskId());
        assertEquals(2, message.getParts().size());
        assertEquals("Part 1", ((TextPart_v0_3) message.getParts().get(0)).getText());
        assertEquals("Part 2", ((TextPart_v0_3) message.getParts().get(1)).getText());
    }

    @Test
    public void testCreateAgentPartsMessageWithNullParts() {
        try {
            A2A_v0_3.createAgentPartsMessage(null, "context", "task");
            org.junit.jupiter.api.Assertions.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Parts cannot be null or empty", e.getMessage());
        }
    }

    @Test
    public void testCreateAgentPartsMessageWithEmptyParts() {
        try {
            A2A_v0_3.createAgentPartsMessage(Collections.emptyList(), "context", "task");
            org.junit.jupiter.api.Assertions.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Parts cannot be null or empty", e.getMessage());
        }
    }
}