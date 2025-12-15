package io.a2a.server.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentExtension;
import io.a2a.spec.AgentInterface;

class A2AExtensionsTest {

    @Test
    void testGetRequestedExtensions() {
        // Test empty list
        Set<String> result = A2AExtensions.getRequestedExtensions(Collections.emptyList());
        assertTrue(result.isEmpty());

        // Test single extension
        result = A2AExtensions.getRequestedExtensions(Arrays.asList("foo"));
        assertEquals(Set.of("foo"), result);

        // Test multiple extensions in separate values
        result = A2AExtensions.getRequestedExtensions(Arrays.asList("foo", "bar"));
        assertEquals(Set.of("foo", "bar"), result);

        // Test comma-separated extensions with space
        result = A2AExtensions.getRequestedExtensions(Arrays.asList("foo, bar"));
        assertEquals(Set.of("foo", "bar"), result);

        // Test comma-separated extensions without space
        result = A2AExtensions.getRequestedExtensions(Arrays.asList("foo,bar"));
        assertEquals(Set.of("foo", "bar"), result);

        // Test mixed format
        result = A2AExtensions.getRequestedExtensions(Arrays.asList("foo", "bar,baz"));
        assertEquals(Set.of("foo", "bar", "baz"), result);

        // Test with empty values and extra spaces
        result = A2AExtensions.getRequestedExtensions(Arrays.asList("foo,, bar", "baz"));
        assertEquals(Set.of("foo", "bar", "baz"), result);

        // Test with leading/trailing spaces
        result = A2AExtensions.getRequestedExtensions(Arrays.asList(" foo , bar ", "baz"));
        assertEquals(Set.of("foo", "bar", "baz"), result);

        // Test null list
        result = A2AExtensions.getRequestedExtensions(null);
        assertTrue(result.isEmpty());

        // Test list with null values
        List<String> listWithNulls = Arrays.asList("foo", null, "bar");
        result = A2AExtensions.getRequestedExtensions(listWithNulls);
        assertEquals(Set.of("foo", "bar"), result);
    }

    @Test
    void testFindExtensionByUri() {
        AgentExtension ext1 = AgentExtension.builder()
                .uri("foo")
                .description("The Foo extension")
                .build();
        AgentExtension ext2 = AgentExtension.builder()
                .uri("bar")
                .description("The Bar extension")
                .build();

        AgentCard card = AgentCard.builder()
                .name("Test Agent")
                .description("Test Agent Description")
                .version("1.0")
                .supportedInterfaces(Collections.singletonList(new AgentInterface("jsonrpc", "http://test.com")))
                .skills(Collections.emptyList())
                .defaultInputModes(Arrays.asList("text/plain"))
                .defaultOutputModes(Arrays.asList("text/plain"))
                .capabilities(AgentCapabilities.builder()
                        .extensions(Arrays.asList(ext1, ext2))
                        .build())
                .build();

        assertEquals(ext1, A2AExtensions.findExtensionByUri(card, "foo"));
        assertEquals(ext2, A2AExtensions.findExtensionByUri(card, "bar"));
        assertNull(A2AExtensions.findExtensionByUri(card, "baz"));
    }

    @Test
    void testFindExtensionByUriNoExtensions() {
        AgentCard card = AgentCard.builder()
                .name("Test Agent")
                .description("Test Agent Description")
                .version("1.0")
                .supportedInterfaces(Collections.singletonList(new AgentInterface("jsonrpc", "http://test.com")))
                .skills(Collections.emptyList())
                .defaultInputModes(Arrays.asList("text/plain"))
                .defaultOutputModes(Arrays.asList("text/plain"))
                .capabilities(AgentCapabilities.builder()
                        .extensions(null)
                        .build())
                .build();

        assertNull(A2AExtensions.findExtensionByUri(card, "foo"));
    }

    @Test
    void testFindExtensionByUriNoCapabilities() {
        // Test with empty capabilities (no extensions list)
        AgentCard card = AgentCard.builder()
                .name("Test Agent")
                .description("Test Agent Description")
                .version("1.0")
                .supportedInterfaces(Collections.singletonList(new AgentInterface("jsonrpc", "http://test.com")))
                .skills(Collections.emptyList())
                .defaultInputModes(Arrays.asList("text/plain"))
                .defaultOutputModes(Arrays.asList("text/plain"))
                .capabilities(AgentCapabilities.builder().build())
                .build();

        assertNull(A2AExtensions.findExtensionByUri(card, "foo"));
    }
}
