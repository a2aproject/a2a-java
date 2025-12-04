# ADR 0002: Refactor Task Class to Java Record

## Status

Accepted

## Context

The `Task` class in the spec module was implemented as a final class with explicit field declarations, constructors, and getter methods. This resulted in significant boilerplate code (approximately 60+ lines for field declarations and accessors alone).

Java 14 introduced records as a concise way to declare immutable data carriers. Records automatically provide:
- Canonical constructor
- Accessor methods (without `get` prefix)
- `equals()`, `hashCode()`, and `toString()` implementations
- Immutability guarantees

The `Task` class is a perfect candidate for record conversion as it:
- Is immutable by design
- Serves as a data carrier for the A2A Protocol
- Contains only final fields with no mutable state
- Already follows value object semantics

## Decision

We have decided to refactor the `Task` class from a final class to a Java record while maintaining full backward compatibility.

### Key Changes

1. **Record Declaration**: Converted the class to use Java record syntax with all fields declared in the compact header
2. **Compact Constructor**: Implemented validation and defensive copying logic in the compact canonical constructor
3. **Backward Compatibility**: Added deprecated getter methods (e.g., `getId()`, `getContextId()`) that delegate to record accessors to ensure existing code continues to work without modification
4. **Builder Pattern**: Updated the `Builder` class to use record accessor methods (`task.id()` instead of `task.id`)

### Implementation Details

- **Validation**: All validation logic (null checks, kind validation) is preserved in the compact constructor
- **Defensive Copying**:
  - Lists (`artifacts`, `history`) are defensively copied using `List.copyOf()` to maintain immutability
  - Map (`metadata`) is defensively copied using `Map.copyOf()` to prevent external modification
- **Jackson Compatibility**: Jackson annotations (`@JsonCreator`, etc.) are maintained for proper serialization/deserialization
- **Deprecation**: Old getter methods are marked with `@Deprecated(since = "1.0")` to encourage migration to record accessors while maintaining compatibility

## Consequences

### Positive

- **Reduced Boilerplate**: Eliminated ~30 lines of repetitive code (field declarations and basic accessor methods)
- **Improved Readability**: More concise and focused class definition
- **Built-in Functionality**: Automatic `equals()`, `hashCode()`, and `toString()` implementations from record
- **Type Safety**: Records enforce immutability at compile time
- **Modern Java**: Aligns with modern Java best practices and idioms
- **Zero Breaking Changes**: Existing code continues to work without any modifications

### Negative

- **Deprecation Warnings**: Code using old getter methods will see deprecation warnings
- **Migration Effort**: Teams should eventually migrate to use record accessor methods (`id()` instead of `getId()`)
- **Learning Curve**: Developers unfamiliar with records may need to learn the new syntax

### Neutral

- **Binary Compatibility**: The deprecated getters ensure binary compatibility with existing compiled code
- **Testing**: All existing tests pass without modification (22 tests in spec module)
- **Build**: Full project build succeeds without errors

## Compliance

This change complies with:
- Java 17+ language features (records introduced in Java 14, standardized in Java 16)
- A2A Protocol specification (no protocol changes)
- Existing API contracts (through deprecated methods)
- JSON serialization requirements (via Jackson annotations)

## References

- [JEP 395: Records](https://openjdk.org/jeps/395)
- Task class location: `spec/src/main/java/io/a2a/spec/Task.java`
- Related issue/PR: #507