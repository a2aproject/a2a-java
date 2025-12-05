# ADR 0001: Task State Management Refactoring

## Status

Accepted

## Context

The original implementation of task state management had a significant architectural issue:

**Multiple Persistence Operations**: Task state changes were being persisted multiple times during event propagation. The `ResultAggregator` would save task state for each event processed, resulting in redundant writes to `TaskStore` for a single request. This created unnecessary I/O load and coupling between event processing and persistence.

## Decision

We refactored the task state management to follow a two-phase approach with proper lifecycle management:

### Separate State Building from Persistence

**Introduced `TaskStateProcessor` for In-Memory State Management**:
- Created per `DefaultRequestHandler` instance to maintain request handler's in-flight tasks
- Maintains task state in memory during event processing
- Provides methods to build task state from events without persisting
- Includes `removeTask()` method for explicit cleanup

**Modified Task Lifecycle**:
- Events are processed to build state in `TaskStateProcessor` without immediate persistence
- State is persisted **once** to `TaskStore` at appropriate lifecycle points (completion, cancellation, etc.)
- Tasks are explicitly removed from `TaskStateProcessor` after final persistence

### Task Cleanup Strategy

Tasks are removed from the state processor when they reach their final state:

1. **Blocking Message Sends**: After all events are processed and final state is persisted
2. **Task Cancellations**: After the canceled task state is persisted
3. **Non-blocking/Background Operations**: After background consumption completes and final state is persisted

### Component Architecture

**TaskStateProcessor** (new component):
- Instance created per `DefaultRequestHandler` to manage its in-flight tasks
- Provides thread-safe access via `ConcurrentHashMap`
- Separates state building from persistence concerns
- Enables explicit lifecycle management with `removeTask()`

**DefaultRequestHandler**:
- Creates and manages its own `TaskStateProcessor` instance
- Ensures tasks are removed after final persistence
- Passes state processor to components that need it

**ResultAggregator**:
- Uses `TaskStateProcessor` to build state during event consumption
- No longer performs persistence during event processing
- Removes tasks after background consumption completes

**TaskManager**:
- Delegates state building to `TaskStateProcessor`
- Coordinates between state processor and persistent store
- Supports dynamic task ID assignment for new tasks

## Consequences

### Positive

1. **Reduced I/O Operations**: Task state is persisted once per request lifecycle instead of multiple times during event propagation, significantly reducing database/storage load
2. **No Memory Leaks**: Tasks are explicitly removed from in-memory state after completion, ensuring memory usage scales with concurrent tasks rather than total tasks processed
3. **Better Test Isolation**: Each test creates its own state processor instance, providing natural isolation
4. **Clear Separation of Concerns**: State building logic is separate from persistence logic, improving maintainability
5. **Thread-Safe Design**: Uses concurrent data structures for safe access from multiple threads

### Negative

1. **Increased Complexity**: More components involved in task lifecycle management
2. **Lifecycle Management Responsibility**: Must ensure cleanup is called at all task completion points
3. **Constructor Changes**: All components creating `TaskManager` and `ResultAggregator` need updates to pass `TaskStateProcessor`

### Test Impact

Test infrastructure was updated to create `TaskStateProcessor` instances:
- Test utilities updated to create and pass `TaskStateProcessor` instances
- Each test creates its own state processor for proper isolation
- Test helper methods updated to handle non-existent tasks gracefully

## Impacts

### Performance
- **Improved**: Significantly reduced database/storage operations

### Memory
- **Bounded**: Memory usage scales with concurrent tasks, not total tasks processed
- **Predictable**: Tasks are removed from memory after completion

### Reliability
- **Improved**: Test isolation ensures reproducible test results
- **Improved**: Clearer task lifecycle reduces potential for bugs

## Outstanding Considerations

### Streaming Task Lifecycle

For streaming responses where clients disconnect mid-stream, background consumption handles cleanup. Tasks remain in memory until background processing completes, creating a brief retention window.

**Impact**: Low - tasks are eventually cleaned up, retention is temporary

### Error Handling Edge Cases

If catastrophic failures occur during event processing before final persistence, tasks might remain orphaned in `TaskStateProcessor`.

**Mitigation**: Most error paths persist task state (including error information), triggering cleanup

**Recommendation**: Consider adding periodic sweep of old tasks or timeout-based cleanup

### Concurrent Access Patterns

The `TaskStateProcessor` ensures thread-safe access via concurrent data structures. Event ordering is maintained by the underlying `EventQueue` system.

**Impact**: None - existing event ordering guarantees are preserved

## Future Enhancements

1. **Observability**: Add metrics for in-flight task count to monitor system health
2. **Cleanup Monitoring**: Add logging/metrics when tasks are removed for debugging
3. **Timeout Cleanup**: Implement periodic sweep of tasks exceeding age threshold
4. **Retention Policies**: Consider configurable retention for debugging (e.g., keep recent tasks for N minutes)

## Verification

All tests passing with the refactoring:
- server-common: 223 tests
- QuarkusA2AJSONRPCTest: 42 tests
- QuarkusA2AGrpcTest: 42 tests

Recommended manual testing:
- Long-running tasks to verify no memory growth
- Streaming scenarios with client disconnects
- Error scenarios to verify cleanup
- Concurrent task processing

## Files Changed

Core implementation:
- `server-common/src/main/java/io/a2a/server/tasks/TaskStateProcessor.java` (new)
- `server-common/src/main/java/io/a2a/server/requesthandlers/DefaultRequestHandler.java`
- `server-common/src/main/java/io/a2a/server/tasks/ResultAggregator.java`
- `server-common/src/main/java/io/a2a/server/tasks/TaskManager.java`

Test infrastructure:
- `server-common/src/test/java/io/a2a/server/tasks/TaskStateProcessorTest.java` (new)
- `tests/server-common/src/test/java/io/a2a/server/apps/common/AbstractA2AServerTest.java`
- All test files using `TaskManager` and `ResultAggregator`
