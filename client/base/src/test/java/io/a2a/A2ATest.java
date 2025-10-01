package io.a2a;

import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        assertEquals(text, ((TextPart) message.getParts().get(0)).getText());
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
        assertEquals(text, ((TextPart) message.getParts().get(0)).getText());
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
    public void testCreateUserMessage() {
        String text = "User message with context";
        String contextId = "context-123";
        String taskId = "task-456";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key", "value");
        List<String> referenceTaskIds = Arrays.asList("ref-task-1", "ref-task-2");
        
        Message message = A2A.createUserMessage(text, contextId, taskId, null, metadata, referenceTaskIds);
        
        assertEquals(Message.Role.USER, message.getRole());
        assertEquals(contextId, message.getContextId());
        assertEquals(taskId, message.getTaskId());
        assertEquals(metadata, message.getMetadata());
        assertEquals(referenceTaskIds, message.getReferenceTaskIds());
        assertEquals(1, message.getParts().size());
        assertEquals(text, ((TextPart) message.getParts().get(0)).getText());
    }

    @Test
    public void testCreateUserMessageWithCustomParts() {
        String text = "Not used";
        List<Part<?>> parts = Arrays.asList(
            new TextPart("Part 1"),
            new TextPart("Part 2")
        );
        
        Message message = A2A.createUserMessage(text, null, null, parts, null, null);
        
        assertEquals(Message.Role.USER, message.getRole());
        assertEquals(2, message.getParts().size());
        assertEquals("Part 1", ((TextPart) message.getParts().get(0)).getText());
        assertEquals("Part 2", ((TextPart) message.getParts().get(1)).getText());
    }

    @Test
    public void testCreateAgentMessage() {
        String text = "Agent message with context";
        String contextId = "context-789";
        String taskId = "task-012";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("agent", "true");
        List<String> referenceTaskIds = Collections.singletonList("ref-task-3");
        
        Message message = A2A.createAgentMessage(text, contextId, taskId, null, metadata, referenceTaskIds);
        
        assertEquals(Message.Role.AGENT, message.getRole());
        assertEquals(contextId, message.getContextId());
        assertEquals(taskId, message.getTaskId());
        assertEquals(metadata, message.getMetadata());
        assertEquals(referenceTaskIds, message.getReferenceTaskIds());
    }

    @Test
    public void testCreateAgentMessageWithId() {
        String text = "Agent message with ID";
        String messageId = "custom-message-id";
        
        Message message = A2A.createAgentMessage(text, messageId, null, null, null, null, null);
        
        assertEquals(Message.Role.AGENT, message.getRole());
        assertEquals(messageId, message.getMessageId());
    }

    @Test
    public void testCreateMessage() {
        String text = "Generic message";
        Message.Role role = Message.Role.USER;
        String messageId = "generic-id";
        String contextId = "generic-context";
        String taskId = "generic-task";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("generic", "metadata");
        List<String> referenceTaskIds = Arrays.asList("ref-1", "ref-2");
        
        Message message = A2A.createMessage(text, role, messageId, contextId, taskId, null, metadata, referenceTaskIds);
        
        assertEquals(role, message.getRole());
        assertEquals(messageId, message.getMessageId());
        assertEquals(contextId, message.getContextId());
        assertEquals(taskId, message.getTaskId());
        assertEquals(metadata, message.getMetadata());
        assertEquals(referenceTaskIds, message.getReferenceTaskIds());
    }
}