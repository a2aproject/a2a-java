package org.a2aproject.sdk.server.events;

import org.junit.jupiter.api.Test;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EnhancedRunnable to verify callback registration enforcement.
 * These tests ensure that callbacks cannot be added after execution starts,
 * preventing race conditions in agent execution.
 */
public class EnhancedRunnableTest {

    @Test
    public void testCannotAddCallbackAfterStart() {
        EnhancedRunnable runnable = new EnhancedRunnable() {
            @Override
            public void run() {
                // Empty
            }
        };
        
        // Add callback before start - should succeed
        AtomicBoolean called = new AtomicBoolean(false);
        runnable.addDoneCallback((r) -> called.set(true));
        
        // Mark as started
        runnable.markStarted();
        
        // Try to add callback after start - should fail
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> runnable.addDoneCallback((r) -> {}));
        
        assertTrue(exception.getMessage().contains("Cannot add callback after runnable has started"));
    }
    
    @Test
    public void testCallbacksInvokedAfterCompletion() throws Exception {
        EnhancedRunnable runnable = new EnhancedRunnable() {
            @Override
            public void run() {
                // Empty
            }
        };
        
        AtomicBoolean callback1Called = new AtomicBoolean(false);
        AtomicBoolean callback2Called = new AtomicBoolean(false);
        
        runnable.addDoneCallback((r) -> callback1Called.set(true));
        runnable.addDoneCallback((r) -> callback2Called.set(true));
        runnable.markStarted();
        
        CompletableFuture.runAsync(runnable, Executors.newSingleThreadExecutor())
            .thenRun(runnable::invokeDoneCallbacks)
            .get();
        
        assertTrue(callback1Called.get());
        assertTrue(callback2Called.get());
    }

    @Test
    public void testMultipleCallbacksBeforeStart() {
        EnhancedRunnable runnable = new EnhancedRunnable() {
            @Override
            public void run() {
                // Empty
            }
        };
        
        // Should be able to add multiple callbacks before start
        assertDoesNotThrow(() -> {
            runnable.addDoneCallback((r) -> {});
            runnable.addDoneCallback((r) -> {});
            runnable.addDoneCallback((r) -> {});
        });
        
        // Mark as started
        runnable.markStarted();
        
        // Now adding should fail
        assertThrows(IllegalStateException.class,
            () -> runnable.addDoneCallback((r) -> {}));
    }

    @Test
    public void testErrorHandling() {
        EnhancedRunnable runnable = new EnhancedRunnable() {
            @Override
            public void run() {
                throw new RuntimeException("Test error");
            }
        };
        
        AtomicBoolean callbackInvoked = new AtomicBoolean(false);
        runnable.addDoneCallback((r) -> {
            callbackInvoked.set(true);
            assertNotNull(r.getError());
            assertEquals("Test error", r.getError().getMessage());
        });
        
        runnable.markStarted();
        
        try {
            runnable.run();
        } catch (RuntimeException e) {
            runnable.setError(e);
        }
        
        runnable.invokeDoneCallbacks();
        assertTrue(callbackInvoked.get());
    }

    @Test
    public void testMarkStartedIdempotent() {
        EnhancedRunnable runnable = new EnhancedRunnable() {
            @Override
            public void run() {
                // Empty
            }
        };
        
        // Should be able to call markStarted multiple times
        assertDoesNotThrow(() -> {
            runnable.markStarted();
            runnable.markStarted();
            runnable.markStarted();
        });
        
        // But callbacks should still be blocked
        assertThrows(IllegalStateException.class,
            () -> runnable.addDoneCallback((r) -> {}));
    }
}
