# ADR 0002: Refactor Domain classes to Java Records

## Status

Accepted

## Context

The `Task` class, `Message` class, and Part type implementations (`DataPart`, `FilePart`, `TextPart`) in the spec module were implemented as final classes with explicit field declarations, constructors, and getter methods. This resulted in significant boilerplate code (approximately 60+ lines for field declarations and accessors alone in Task, and similar patterns in Message and Part implementations).

Java 14 introduced records as a concise way to declare immutable data carriers. Records automatically provide:
- Canonical constructor
- Accessor methods (without `get` prefix)
- `equals()`, `hashCode()`, and `toString()` implementations
- Immutability guarantees

These classes are perfect candidates for record conversion as they:
- Are immutable by design
- Serve as data carriers for the A2A Protocol
- Contain only final fields with no mutable state
- Already follow value object semantics

Additionally, the Part type hierarchy benefits from Java's sealed interfaces (introduced in Java 17), allowing the `Part` interface to restrict which types can implement it while maintaining type safety and exhaustiveness checking.

## Decision

We have decided to refactor the `Task` class, `Message` class, and Part type implementations (`DataPart`, `FilePart`, `TextPart`) from final classes to Java records while maintaining full backward compatibility. The `Part` interface has been converted to a sealed interface.

### Key Changes - Task Class

1. **Record Declaration**: Converted the class to use Java record syntax with all fields declared in the compact header
2. **Compact Constructor**: Implemented validation and defensive copying logic in the compact canonical constructor
3. **Backward Compatibility**: Added deprecated getter methods (e.g., `getId()`, `getContextId()`) that delegate to record accessors to ensure existing code continues to work without modification
4. **Builder Pattern**: Updated the `Builder` class to use record accessor methods (`task.id()` instead of `task.id`)

### Key Changes - Message Class

1. **Record Declaration**: Converted the class to use Java record syntax with all fields declared in the compact header
2. **Compact Constructor**: Implemented validation and defensive copying logic in the compact canonical constructor
3. **Backward Compatibility**: Added deprecated getter methods (e.g., `getRole()`, `getParts()`, `getMessageId()`) that delegate to record accessors to ensure existing code continues to work without modification
4. **Builder Pattern**: Updated the `Builder` class to use record accessor methods (`message.role()` instead of `message.role`)
5. **Removed Setters**: The `setTaskId()` and `setContextId()` methods were removed as records are immutable. Code that requires mutation should use the builder pattern to create new instances.

### Key Changes - Part Types

1. **Sealed Interface**: Converted `Part` from an abstract base class to a `sealed interface` that permits only `DataPart`, `FilePart`, and `TextPart` implementations
2. **Record Implementations**: Converted `DataPart`, `FilePart`, and `TextPart` from final classes to records
3. **Compact Constructors**: Implemented validation and defensive copying in compact constructors for each Part type
4. **Backward Compatibility**: Added deprecated getter methods (e.g., `getData()`, `getFile()`, `getText()`) that delegate to record accessors
5. **Polymorphic Serialization**: Maintained Jackson's `@JsonTypeInfo` and `@JsonSubTypes` annotations for proper polymorphic JSON handling

### Implementation Details

- **Validation**: All validation logic (null checks, kind validation) is preserved in the compact constructors
- **Defensive Copying**:
  - Lists (`artifacts`, `history` in Task; `parts`, `referenceTaskIds`, `extensions` in Message) are defensively copied using `List.copyOf()` to maintain immutability
  - Maps (`metadata` in Task, Message, and Part types) are defensively copied using `Map.copyOf()` to prevent external modification
- **Jackson Compatibility**: Jackson annotations (`@JsonCreator`, `@JsonTypeName`, etc.) are maintained for proper serialization/deserialization
- **Deprecation**: Old getter methods are marked with `@Deprecated(since = "1.0")` to encourage migration to record accessors while maintaining compatibility
- **Sealed Types**: The sealed interface pattern ensures exhaustive pattern matching in switch expressions and prevents unauthorized implementations

## Consequences

### Positive

- **Reduced Boilerplate**: Eliminated ~30 lines of repetitive code per class (field declarations and basic accessor methods)
- **Improved Readability**: More concise and focused class definitions
- **Built-in Functionality**: Automatic `equals()`, `hashCode()`, and `toString()` implementations from record
- **Type Safety**: Records enforce immutability at compile time; sealed interfaces prevent unauthorized implementations
- **Pattern Matching**: Sealed interface enables exhaustive switch expressions over Part types
- **Modern Java**: Aligns with modern Java best practices and idioms (records + sealed types)
- **Zero Breaking Changes**: Existing code continues to work without any modifications

### Negative

- **Deprecation Warnings**: Code using old getter methods will see deprecation warnings
- **Migration Effort**: Teams should eventually migrate to use record accessor methods (`id()` instead of `getId()`)
- **Learning Curve**: Developers unfamiliar with records may need to learn the new syntax

### Neutral

- **Binary Compatibility**: The deprecated getters ensure binary compatibility with existing compiled code
- **Testing**: All existing tests pass without modification
- **Build**: Full project build succeeds without errors
- **Interface Change**: Part changed from abstract class to sealed interface, but the change is source and binary compatible due to deprecated methods

## Compliance

This change complies with:
- Java 17+ language features (records introduced in Java 14, standardized in Java 16; sealed types in Java 17)
- A2A Protocol specification (no protocol changes)
- Existing API contracts (through deprecated methods)
- JSON serialization requirements (via Jackson annotations)

## References

- [JEP 395: Records](https://openjdk.org/jeps/395)
- [JEP 409: Sealed Classes](https://openjdk.org/jeps/409)
- Task class location: `spec/src/main/java/io/a2a/spec/Task.java`
- Message class location: `spec/src/main/java/io/a2a/spec/Message.java`
- Part interface location: `spec/src/main/java/io/a2a/spec/Part.java`
- Part implementations: `spec/src/main/java/io/a2a/spec/{DataPart,FilePart,TextPart}.java`
- Related issue/PR: #507