package org.a2aproject.sdk.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.a2aproject.sdk.server.auth.User;
import org.junit.jupiter.api.Test;

class ServerCallContextTest {

    @Test
    void testDefaultConstructor() {
        User user = new TestUser();
        Map<String, Object> state = new HashMap<>();
        
        ServerCallContext context = new ServerCallContext(user, state, new HashSet<>());
        
        assertEquals(user, context.getUser());
        assertEquals(state, context.getState());
        assertTrue(context.getRequestedExtensions().isEmpty());
        assertTrue(context.getActivatedExtensions().isEmpty());
    }

    @Test
    void testConstructorWithRequestedExtensions() {
        User user = new TestUser();
        Map<String, Object> state = new HashMap<>();
        Set<String> requestedExtensions = Set.of("foo", "bar");
        
        ServerCallContext context = new ServerCallContext(user, state, requestedExtensions);
        
        assertEquals(user, context.getUser());
        assertEquals(state, context.getState());
        assertEquals(requestedExtensions, context.getRequestedExtensions());
        assertTrue(context.getActivatedExtensions().isEmpty());
    }

    @Test
    void testConstructorWithRequestedAndActivatedExtensions() {
        User user = new TestUser();
        Map<String, Object> state = new HashMap<>();
        Set<String> requestedExtensions = Set.of("foo", "bar");
        ServerCallContext context = new ServerCallContext(user, state, requestedExtensions);
        
        // Manually activate extensions since they start empty
        context.activateExtension("foo");
        
        assertEquals(user, context.getUser());
        assertEquals(state, context.getState());
        assertEquals(requestedExtensions, context.getRequestedExtensions());
        assertEquals(Set.of("foo"), context.getActivatedExtensions());
    }

    @Test
    void testExtensionActivation() {
        User user = new TestUser();
        Map<String, Object> state = new HashMap<>();
        Set<String> requestedExtensions = Set.of("foo", "bar");
        
        ServerCallContext context = new ServerCallContext(user, state, requestedExtensions);
        
        // Initially no extensions are activated
        assertFalse(context.isExtensionActivated("foo"));
        assertFalse(context.isExtensionActivated("bar"));
        
        // Activate an extension
        context.activateExtension("foo");
        assertTrue(context.isExtensionActivated("foo"));
        assertFalse(context.isExtensionActivated("bar"));
        
        // Activate another extension
        context.activateExtension("bar");
        assertTrue(context.isExtensionActivated("foo"));
        assertTrue(context.isExtensionActivated("bar"));
        
        // Deactivate an extension
        context.deactivateExtension("foo");
        assertFalse(context.isExtensionActivated("foo"));
        assertTrue(context.isExtensionActivated("bar"));
    }

    @Test
    void testExtensionRequested() {
        User user = new TestUser();
        Map<String, Object> state = new HashMap<>();
        Set<String> requestedExtensions = Set.of("foo", "bar");
        
        ServerCallContext context = new ServerCallContext(user, state, requestedExtensions);
        
        assertTrue(context.isExtensionRequested("foo"));
        assertTrue(context.isExtensionRequested("bar"));
        assertFalse(context.isExtensionRequested("baz"));
    }

    @Test
    void testExtensionCollectionsAreDefensiveCopies() {
        User user = new TestUser();
        Map<String, Object> state = new HashMap<>();
        Set<String> requestedExtensions = Set.of("foo", "bar");
        
        ServerCallContext context = new ServerCallContext(user, state, requestedExtensions);
        
        // Modifying returned sets should not affect the context
        Set<String> returnedRequested = context.getRequestedExtensions();
        returnedRequested.add("baz");
        assertFalse(context.isExtensionRequested("baz"));
        
        context.activateExtension("foo");
        Set<String> returnedActivated = context.getActivatedExtensions();
        returnedActivated.add("bar");
        assertFalse(context.isExtensionActivated("bar"));
    }

    // Simple test implementation of User interface
    private static class TestUser implements User {
        @Override
        public boolean isAuthenticated() {
            return true;
        }

        @Override
        public String getUsername() {
            return "test-user";
        }
    }
}
