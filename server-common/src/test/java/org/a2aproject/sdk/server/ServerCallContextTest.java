package org.a2aproject.sdk.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.a2aproject.sdk.server.auth.User;
import org.a2aproject.sdk.spec.AgentCard;
import org.junit.jupiter.api.Test;

class ServerCallContextTest {

    @Test
    void effectiveCardAttachmentNormalizesTenantAndRejectsConflictsWithoutReplacement() {
        ServerCallContext context = new ServerCallContext(new TestUser(), Map.of(), Set.of());
        AgentCard card = AgentCardResolverTest.card("public");
        assertNull(context.getEffectiveAgentCard());
        context.attachEffectiveAgentCard("/acme/", card);
        EffectiveAgentCard attached = context.getEffectiveAgentCard();
        context.attachEffectiveAgentCard("acme", card);
        assertSame(attached, context.getEffectiveAgentCard());
        assertEquals("acme", attached.tenant());
        assertSame(card, attached.card());
        assertThrows(IllegalStateException.class, () -> context.attachEffectiveAgentCard("other", card));
        assertThrows(IllegalStateException.class,
                () -> context.attachEffectiveAgentCard("acme", AgentCardResolverTest.card("public")));
        assertSame(attached, context.getEffectiveAgentCard());

        ServerCallContext defaults = new ServerCallContext(new TestUser(), Map.of(), Set.of());
        defaults.attachEffectiveAgentCard(null, card);
        EffectiveAgentCard defaultCard = defaults.getEffectiveAgentCard();
        for (String tenant : new String[] {"", " ", "/", "//"}) {
            defaults.attachEffectiveAgentCard(tenant, card);
            assertSame(defaultCard, defaults.getEffectiveAgentCard());
        }
    }

    @Test
    void concurrentSameCardAttachmentIsIdempotent() throws Exception {
        ServerCallContext context = new ServerCallContext(new TestUser(), Map.of(), Set.of());
        AgentCard card = AgentCardResolverTest.card("public");
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<EffectiveAgentCard> first = executor.submit(() -> {
                assertTrue(start.await(5, TimeUnit.SECONDS));
                context.attachEffectiveAgentCard("/acme/", card);
                return context.getEffectiveAgentCard();
            });
            Future<EffectiveAgentCard> second = executor.submit(() -> {
                assertTrue(start.await(5, TimeUnit.SECONDS));
                context.attachEffectiveAgentCard("acme", card);
                return context.getEffectiveAgentCard();
            });
            start.countDown();
            assertSame(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
            assertSame(card, context.getEffectiveAgentCard().card());
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentDifferentCardsInstallExactlyOneValueAndRejectTheOther() throws Exception {
        ServerCallContext context = new ServerCallContext(new TestUser(), Map.of(), Set.of());
        AgentCard firstCard = AgentCardResolverTest.card("first");
        AgentCard secondCard = AgentCardResolverTest.card("second");
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<?>> calls = new ArrayList<>();
            for (AgentCard card : List.of(firstCard, secondCard)) {
                calls.add(executor.submit(() -> {
                    try {
                        assertTrue(start.await(5, TimeUnit.SECONDS));
                        context.attachEffectiveAgentCard("acme", card);
                        successes.incrementAndGet();
                    } catch (IllegalStateException expected) {
                        conflicts.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(e);
                    }
                }));
            }
            start.countDown();
            for (Future<?> call : calls) {
                call.get(5, TimeUnit.SECONDS);
            }
            assertEquals(1, successes.get());
            assertEquals(1, conflicts.get());
            EffectiveAgentCard attached = context.getEffectiveAgentCard();
            AgentCard loser = attached.card() == firstCard ? secondCard : firstCard;
            assertThrows(IllegalStateException.class, () -> context.attachEffectiveAgentCard("acme", loser));
            assertSame(attached, context.getEffectiveAgentCard());
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

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
