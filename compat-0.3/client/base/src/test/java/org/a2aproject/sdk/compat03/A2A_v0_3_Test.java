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
        
        assertEquals(Message_v0_3.Role.USER, message.role());
        assertEquals(1, message.parts().size());
        assertEquals(text, ((TextPart_v0_3) message.parts().get(0)).text());
        assertNotNull(message.messageId());
        assertNull(message.contextId());
        assertNull(message.taskId());
    }

    @Test
    public void testToUserMessageWithId() {
        String text = "Hello, world!";
        String messageId = "test-message-id";
        Message_v0_3 message = A2A_v0_3.toUserMessage(text, messageId);
        
        assertEquals(Message_v0_3.Role.USER, message.role());
        assertEquals(messageId, message.messageId());
    }

    @Test
    public void testToAgentMessage() {
        String text = "Hello, I'm an agent!";
        Message_v0_3 message = A2A_v0_3.toAgentMessage(text);
        
        assertEquals(Message_v0_3.Role.AGENT, message.role());
        assertEquals(1, message.parts().size());
        assertEquals(text, ((TextPart_v0_3) message.parts().get(0)).text());
        assertNotNull(message.messageId());
    }

    @Test
    public void testToAgentMessageWithId() {
        String text = "Hello, I'm an agent!";
        String messageId = "agent-message-id";
        Message_v0_3 message = A2A_v0_3.toAgentMessage(text, messageId);
        
        assertEquals(Message_v0_3.Role.AGENT, message.role());
        assertEquals(messageId, message.messageId());
    }

    @Test
    public void testCreateUserTextMessage() {
        String text = "User message with context";
        String contextId = "context-123";
        String taskId = "task-456";
        
        Message_v0_3 message = A2A_v0_3.createUserTextMessage(text, contextId, taskId);
        
        assertEquals(Message_v0_3.Role.USER, message.role());
        assertEquals(contextId, message.contextId());
        assertEquals(taskId, message.taskId());
        assertEquals(1, message.parts().size());
        assertEquals(text, ((TextPart_v0_3) message.parts().get(0)).text());
        assertNotNull(message.messageId());
        assertNull(message.metadata());
        assertNull(message.referenceTaskIds());
    }

    @Test
    public void testCreateUserTextMessageWithNullParams() {
        String text = "Simple user message";
        
        Message_v0_3 message = A2A_v0_3.createUserTextMessage(text, null, null);
        
        assertEquals(Message_v0_3.Role.USER, message.role());
        assertNull(message.contextId());
        assertNull(message.taskId());
        assertEquals(1, message.parts().size());
        assertEquals(text, ((TextPart_v0_3) message.parts().get(0)).text());
    }

    @Test
    public void testCreateAgentTextMessage() {
        String text = "Agent message with context";
        String contextId = "context-789";
        String taskId = "task-012";
        
        Message_v0_3 message = A2A_v0_3.createAgentTextMessage(text, contextId, taskId);
        
        assertEquals(Message_v0_3.Role.AGENT, message.role());
        assertEquals(contextId, message.contextId());
        assertEquals(taskId, message.taskId());
        assertEquals(1, message.parts().size());
        assertEquals(text, ((TextPart_v0_3) message.parts().get(0)).text());
        assertNotNull(message.messageId());
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
        
        assertEquals(Message_v0_3.Role.AGENT, message.role());
        assertEquals(contextId, message.contextId());
        assertEquals(taskId, message.taskId());
        assertEquals(2, message.parts().size());
        assertEquals("Part 1", ((TextPart_v0_3) message.parts().get(0)).text());
        assertEquals("Part 2", ((TextPart_v0_3) message.parts().get(1)).text());
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