package org.a2aproject.sdk.server.tasks;

import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.events.EventQueue;
import org.a2aproject.sdk.spec.UnsupportedOperationError;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Concurrency tests for AgentEmitter to verify thread-safety of terminal state management.
 * These tests ensure that the AtomicBoolean-based terminal state flag prevents race conditions
 * when multiple threads attempt to set terminal states concurrently.
 */
public class AgentEmitterConcurrencyTest {

    @Test
    public void testConcurrentTerminalStateUpdates() throws InterruptedException {
        // Setup
        RequestContext context = mock(RequestContext.class);
        when(context.getTaskId()).thenReturn("test-task-123");
        when(context.getContextId()).thenReturn("test-context-456");
        
        EventQueue eventQueue = mock(EventQueue.class);
        AgentEmitter emitter = new AgentEmitter(context, eventQueue);
        
        // Test concurrent completion attempts
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    emitter.complete();
                    successCount.incrementAndGet();
                } catch (IllegalStateException e) {
                    failureCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        
        startLatch.countDown(); // Start all threads simultaneously
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "All threads should complete");
        
        // Verify: exactly one success, rest failures
        assertEquals(1, successCount.get(), "Exactly one thread should succeed");
        assertEquals(threadCount - 1, failureCount.get(), "All other threads should fail");
        
        // Verify: only one event was enqueued
        verify(eventQueue, times(1)).enqueueEvent(any());
        
        executor.shutdown();
    }
    
    @Test
    public void testConcurrentMixedTerminalStates() throws InterruptedException {
        // Setup
        RequestContext context = mock(RequestContext.class);
        when(context.getTaskId()).thenReturn("test-task-123");
        when(context.getContextId()).thenReturn("test-context-456");
        
        EventQueue eventQueue = mock(EventQueue.class);
        AgentEmitter emitter = new AgentEmitter(context, eventQueue);
        
        // Test concurrent different terminal state attempts
        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(3);
        AtomicInteger successCount = new AtomicInteger(0);
        
        // Thread 1: complete
        executor.submit(() -> {
            try {
                startLatch.await();
                emitter.complete();
                successCount.incrementAndGet();
            } catch (Exception e) {
                // Expected for 2 out of 3 threads
            } finally {
                doneLatch.countDown();
            }
        });
        
        // Thread 2: fail
        executor.submit(() -> {
            try {
                startLatch.await();
                emitter.fail();
                successCount.incrementAndGet();
            } catch (Exception e) {
                // Expected for 2 out of 3 threads
            } finally {
                doneLatch.countDown();
            }
        });
        
        // Thread 3: cancel
        executor.submit(() -> {
            try {
                startLatch.await();
                emitter.cancel();
                successCount.incrementAndGet();
            } catch (Exception e) {
                // Expected for 2 out of 3 threads
            } finally {
                doneLatch.countDown();
            }
        });
        
        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        
        // Verify: exactly one success
        assertEquals(1, successCount.get(), "Exactly one terminal state should succeed");
        verify(eventQueue, times(1)).enqueueEvent(any());
        
        executor.shutdown();
    }

    @Test
    public void testConcurrentFailWithErrorAndComplete() throws InterruptedException {
        // Setup
        RequestContext context = mock(RequestContext.class);
        when(context.getTaskId()).thenReturn("test-task-123");
        when(context.getContextId()).thenReturn("test-context-456");
        
        EventQueue eventQueue = mock(EventQueue.class);
        AgentEmitter emitter = new AgentEmitter(context, eventQueue);
        
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);
        
        executor.submit(() -> {
            try {
                startLatch.await();
                emitter.fail(new UnsupportedOperationError());
                successCount.incrementAndGet();
            } catch (Exception e) {
                // Expected for one thread
            } finally {
                doneLatch.countDown();
            }
        });
        
        executor.submit(() -> {
            try {
                startLatch.await();
                emitter.complete();
                successCount.incrementAndGet();
            } catch (Exception e) {
                // Expected for one thread
            } finally {
                doneLatch.countDown();
            }
        });
        
        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        assertEquals(1, successCount.get(), "Exactly one terminal operation should succeed");
        
        executor.shutdown();
    }

    @Test
    public void testFailWithErrorSetsTerminalState() {
        // Setup
        RequestContext context = mock(RequestContext.class);
        when(context.getTaskId()).thenReturn("test-task-123");
        when(context.getContextId()).thenReturn("test-context-456");
        
        EventQueue eventQueue = mock(EventQueue.class);
        AgentEmitter emitter = new AgentEmitter(context, eventQueue);
        
        // Call fail with error
        emitter.fail(new UnsupportedOperationError());
        
        // Verify terminal state is set - subsequent calls should throw
        IllegalStateException exception = assertThrows(IllegalStateException.class, 
            () -> emitter.complete());
        assertEquals("Cannot update task status - terminal state already reached", 
            exception.getMessage());
    }

    @Test
    public void testFailWithErrorThenFailWithMessage() {
        // Setup
        RequestContext context = mock(RequestContext.class);
        when(context.getTaskId()).thenReturn("test-task-123");
        when(context.getContextId()).thenReturn("test-context-456");
        
        EventQueue eventQueue = mock(EventQueue.class);
        AgentEmitter emitter = new AgentEmitter(context, eventQueue);
        
        // Call fail with error
        emitter.fail(new UnsupportedOperationError());
        
        // Second fail should throw
        IllegalStateException exception = assertThrows(IllegalStateException.class, 
            () -> emitter.fail());
        assertEquals("Cannot update task status - terminal state already reached", 
            exception.getMessage());
    }

    @Test
    public void testNonTerminalThenTerminalState() throws InterruptedException {
        // Setup
        RequestContext context = mock(RequestContext.class);
        when(context.getTaskId()).thenReturn("test-task-123");
        when(context.getContextId()).thenReturn("test-context-456");
        
        EventQueue eventQueue = mock(EventQueue.class);
        AgentEmitter emitter = new AgentEmitter(context, eventQueue);
        
        // Non-terminal states should work
        emitter.submit();
        emitter.startWork();
        
        // Terminal state should work
        emitter.complete();
        
        // Verify events were enqueued
        verify(eventQueue, times(3)).enqueueEvent(any());
        
        // Further updates should fail
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> emitter.startWork());
        assertEquals("Cannot update task status - terminal state already reached",
            exception.getMessage());
    }
}
