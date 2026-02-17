package io.a2a;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;
import org.junit.jupiter.api.Test;

public class A2ATest {

    @Test
    public void testToUserMessage() {
        String text = "Hello, world!";
        Message message = A2A.toUserMessage(text);
        
        assertEquals(Message.Role.ROLE_USER, message.role());
        assertEquals(1, message.parts().size());
        assertEquals(text, ((TextPart) message.parts().get(0)).text());
        assertNotNull(message.messageId());
        assertNull(message.contextId());
        assertNull(message.taskId());
    }

    @Test
    public void testToUserMessageWithId() {
        String text = "Hello, world!";
        String messageId = "test-message-id";
        Message message = A2A.toUserMessage(text, messageId);
        
        assertEquals(Message.Role.ROLE_USER, message.role());
        assertEquals(messageId, message.messageId());
    }

    @Test
    public void testToAgentMessage() {
        String text = "Hello, I'm an agent!";
        Message message = A2A.toAgentMessage(text);
        
        assertEquals(Message.Role.ROLE_AGENT, message.role());
        assertEquals(1, message.parts().size());
        assertEquals(text, ((TextPart) message.parts().get(0)).text());
        assertNotNull(message.messageId());
    }

    @Test
    public void testToAgentMessageWithId() {
        String text = "Hello, I'm an agent!";
        String messageId = "agent-message-id";
        Message message = A2A.toAgentMessage(text, messageId);
        
        assertEquals(Message.Role.ROLE_AGENT, message.role());
        assertEquals(messageId, message.messageId());
    }

    @Test
    public void testCreateUserTextMessage() {
        String text = "User message with context";
        String contextId = "context-123";
        String taskId = "task-456";
        
        Message message = A2A.createUserTextMessage(text, contextId, taskId);
        
        assertEquals(Message.Role.ROLE_USER, message.role());
        assertEquals(contextId, message.contextId());
        assertEquals(taskId, message.taskId());
        assertEquals(1, message.parts().size());
        assertEquals(text, ((TextPart) message.parts().get(0)).text());
        assertNotNull(message.messageId());
        assertNull(message.metadata());
        assertNull(message.referenceTaskIds());
    }

    @Test
    public void testCreateUserTextMessageWithNullParams() {
        String text = "Simple user message";
        
        Message message = A2A.createUserTextMessage(text, null, null);
        
        assertEquals(Message.Role.ROLE_USER, message.role());
        assertNull(message.contextId());
        assertNull(message.taskId());
        assertEquals(1, message.parts().size());
        assertEquals(text, ((TextPart) message.parts().get(0)).text());
    }

    @Test
    public void testCreateAgentTextMessage() {
        String text = "Agent message with context";
        String contextId = "context-789";
        String taskId = "task-012";
        
        Message message = A2A.createAgentTextMessage(text, contextId, taskId);
        
        assertEquals(Message.Role.ROLE_AGENT, message.role());
        assertEquals(contextId, message.contextId());
        assertEquals(taskId, message.taskId());
        assertEquals(1, message.parts().size());
        assertEquals(text, ((TextPart) message.parts().get(0)).text());
        assertNotNull(message.messageId());
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
        
        assertEquals(Message.Role.ROLE_AGENT, message.role());
        assertEquals(contextId, message.contextId());
        assertEquals(taskId, message.taskId());
        assertEquals(2, message.parts().size());
        assertEquals("Part 1", ((TextPart) message.parts().get(0)).text());
        assertEquals("Part 2", ((TextPart) message.parts().get(1)).text());
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