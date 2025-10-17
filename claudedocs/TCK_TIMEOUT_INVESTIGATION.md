# TCK Timeout Investigation Report

**Issue**: [#248](https://github.com/a2aproject/a2a-java/issues/248)
**Date**: 2025-10-13
**Status**: Root cause identified, fix ready to implement

## Executive Summary

TCK tests fail in CI with agents accumulating in the `runningAgents` map and never being cleaned up. The root cause is `ResultAggregator.consumeAndBreakOnInterrupt()` using `CompletableFuture.runAsync()` without an executor parameter, which defaults to `ForkJoinPool.commonPool` (only 3 threads on CI). During concurrent request bursts, the ForkJoinPool becomes saturated, preventing some consumer polling loops from starting. Vert.x worker threads remain blocked forever at `completionFuture.join()`, and cleanup finally blocks never execute.

## Evidence

### Run 2 Analysis (~/Downloads/Run_2)

**Thread Dump Issue**:
- Initially captured wrong process (Maven parent, PID 50387 instead of Quarkus app PID 50431)
- Showed only 12 JVM internal threads, no application threads
- Fixed by changing `pgrep -f "quarkus:dev"` to `pgrep -f "a2a-tck-server-dev.jar"`

**Log Analysis**:
```
2025-10-13 16:49:39.026 Agent execution completed for task a5a169bb-42ca-41e9-94e9-cdf81b87ebcf
2025-10-13 16:50:09.xxx Registered agent for task XXX, runningAgents.size() after: 1
2025-10-13 16:50:09.xxx Registered agent for task YYY, runningAgents.size() after: 2
... (burst of 6 concurrent tasks)
2025-10-13 16:50:09.xxx Registered agent for task ZZZ, runningAgents.size() after: 6
```

**Key Findings**:
- Agents accumulate starting at 16:50:09 with burst of 6 concurrent tasks
- Pattern: 0→1→3→4→5→6→7→8→9→10→11 agents at 30-second intervals
- First leaking task `a5a169bb` completes at 16:49:39 but **never removed**
- No "Removed agent for task a5a169bb" log entry
- ForkJoinPool threads last seen at 16:49:18, but task starts at 16:49:39 (21 seconds later)

### Run 3 Analysis (~/Downloads/Run_3)

**Thread Dump** (64K, 43 threads - correct process captured):
```
"vert.x-worker-thread-4" #59 prio=5 os_prio=0 cpu=854.96ms elapsed=295.47s
   java.lang.Thread.State: WAITING (parking)
	at jdk.internal.misc.Unsafe.park(Native Method)
	- parking to wait for  <0x000000071b687510> (a java.util.concurrent.CompletableFuture$Signaller)
	at java.util.concurrent.CompletableFuture.join(CompletableFuture.java:2117)
	at io.a2a.server.tasks.ResultAggregator.consumeAndBreakOnInterrupt(ResultAggregator.java:202)
	at io.a2a.server.requesthandlers.DefaultRequestHandler.onMessageSend(DefaultRequestHandler.java:203)
```

**Log Analysis**:
- Same pattern: Accumulation starts at 17:50:42 with 6 concurrent tasks
- Agents accumulate: 0→2→3→4→6→7→8→9→10→11
- Identical behavior to Run 2, confirming reproducible issue

## Root Cause Analysis

### The Problem Code

**File**: `server-common/src/main/java/io/a2a/server/tasks/ResultAggregator.java:105`

```java
public EventTypeAndInterrupt consumeAndBreakOnInterrupt(EventConsumer consumer, boolean blocking, Runnable eventCallback) throws JSONRPCError {
    Flow.Publisher<Event> all = consumer.consumeAll();
    // ... setup ...
    CompletableFuture<Void> completionFuture = new CompletableFuture<>();

    // PROBLEM: No executor parameter!
    CompletableFuture.runAsync(() -> {
        consumer(
            createTubeConfig(),
            all,
            (event) -> {
                // ... event processing that starts queue polling ...
            },
            throwable -> {
                if (throwable != null) {
                    errorRef.set(throwable);
                    completionFuture.completeExceptionally(throwable);
                } else {
                    completionFuture.complete(null);
                }
            }
        );
    }); // ← Uses ForkJoinPool.commonPool by default!

    // Vert.x worker thread blocks here forever if async task never runs
    try {
        completionFuture.join();
    } catch (CompletionException e) {
        // ... error handling ...
    }
```

### Why This Causes Agent Accumulation

**Sequential Execution**:
1. Client sends message → Vert.x assigns worker thread
2. Worker thread calls `DefaultRequestHandler.onMessageSend()`
3. Creates `ResultAggregator` and calls `consumeAndBreakOnInterrupt()`
4. Submits consumer subscription to ForkJoinPool.commonPool
5. Worker thread blocks at `completionFuture.join()` waiting for result
6. ForkJoinPool thread starts queue polling loop (blocks in `queue.dequeueEvent(500ms)`)
7. Agent completes → enqueues final event → queue polling loop processes it
8. CompletableFuture completes → worker thread unblocks
9. Finally block executes → agent removed from `runningAgents`

**Concurrent Execution (The Failure)**:
1. 6 concurrent requests arrive simultaneously
2. 6 Vert.x worker threads submit tasks to ForkJoinPool.commonPool
3. **ForkJoinPool only has 3 threads** (CPU count on CI)
4. First 3 tasks start their polling loops successfully
5. Remaining 3 tasks **queued in ForkJoinPool, never scheduled**
6. Corresponding Vert.x worker threads block forever at `completionFuture.join()`
7. Agents complete but polling never starts → completionFutures never complete
8. Finally blocks never execute → **agents leak in runningAgents map**
9. Each new concurrent burst adds more leaked agents
10. Eventually: 21 leaked agents, Vert.x worker pool exhausted, timeouts

### Why ForkJoinPool is Wrong Choice

**ForkJoinPool.commonPool characteristics**:
- Size = `Runtime.getRuntime().availableProcessors() - 1` (typically 3 on CI)
- Designed for CPU-bound divide-and-conquer tasks
- **Not suitable for I/O-bound blocking operations** like queue polling

**Our queue polling loop**:
```java
while (true) {
    event = queue.dequeueEvent(QUEUE_WAIT_MILLISECONDS); // Blocks for 500ms!
    // ... process event ...
}
```

This blocks the ForkJoinPool thread for extended periods, preventing other tasks from running.

## The Fix

### Solution: Use @Internal Executor

The `DefaultRequestHandler` already has access to the correct executor:

**File**: `server-common/src/main/java/io/a2a/server/requesthandlers/DefaultRequestHandler.java:78-87`
```java
@Inject
public DefaultRequestHandler(AgentExecutor agentExecutor, TaskStore taskStore,
                             QueueManager queueManager, PushNotificationConfigStore pushConfigStore,
                             PushNotificationSender pushSender, @Internal Executor executor) {
    // ...
    this.executor = executor;  // ← This is the correct executor to use!
```

**Configuration** (`tck/src/main/resources/application.properties`):
```properties
a2a.executor.core-pool-size=5
a2a.executor.max-pool-size=15
```

This executor has 15 threads and is designed for I/O-bound blocking operations.

### Implementation Plan

1. **Modify ResultAggregator constructor**:
```java
public class ResultAggregator {
    private final Executor executor;

    public ResultAggregator(TaskManager taskManager, Message message, Executor executor) {
        this.taskManager = taskManager;
        this.message = message;
        this.executor = executor;
    }
```

2. **Update runAsync() call**:
```java
// Before:
CompletableFuture.runAsync(() -> {
    consumer(...);
});

// After:
CompletableFuture.runAsync(() -> {
    consumer(...);
}, executor);
```

3. **Update all constructor calls in DefaultRequestHandler**:
```java
// Line 141, 183, 242, 425:
ResultAggregator resultAggregator = new ResultAggregator(mss.taskManager, null, executor);
```

## Expected Impact

**Before Fix**:
- ForkJoinPool.commonPool (3 threads) handles queue polling
- Concurrent bursts (6+ requests) saturate pool
- Some polling loops never start
- Vert.x workers block forever
- Agents accumulate indefinitely

**After Fix**:
- @Internal Executor (15 threads) handles queue polling
- Can handle 15 concurrent blocking operations
- All polling loops start successfully
- Vert.x workers complete normally
- Agents clean up in finally blocks

## Additional Fixes Applied

### Diagnostic Capture Fix

**Problem**: CI workflow captured Maven parent process (PID 50387) instead of Quarkus application (PID 50431)

**Fix**: Updated `.github/workflows/run-tck.yml`:
```yaml
# Before:
QUARKUS_PID=$(pgrep -f "quarkus:dev" || echo "")

# After:
QUARKUS_PID=$(pgrep -f "a2a-tck-server-dev.jar" || echo "")
```

**Verification**: Run 3 thread dump shows 64K (vs Run 2's 8K), with 43 application threads including blocked vert.x workers.

## Workarounds to Remove After Fix

Once the fix is verified, remove this workaround from `tck/src/main/resources/application.properties`:

```properties
# TEMPORARY WORKAROUND: Increase Vert.x worker thread blocking timeout
quarkus.vertx.max-worker-execute-time=300s
```

This was masking the symptom but not addressing the root cause.

## Testing Recommendations

1. Run TCK with concurrent request load (6+ simultaneous requests)
2. Monitor `runningAgents.size()` in logs - should remain at 0 or low numbers
3. Verify "Removed agent for task X" appears for every "Registered agent for task X"
4. Check thread dumps - no vert.x workers should be blocked at `completionFuture.join()`
5. Verify all tests pass without timeout failures

## References

- **Issue**: https://github.com/a2aproject/a2a-java/issues/248
- **Failing CI Run**: https://github.com/a2aproject/a2a-java/actions/runs/18472624510/job/52629881977?pr=333
- **Previous Fix**: Commit 3e93dd2 (removed `awaitQueuePollerStart()`)
- **Diagnostic Fix**: Commit fixing workflow process ID capture
