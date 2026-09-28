# Project Standards

This file documents the build tools, commands, and code style conventions for the a2a-java project.

- Build tool: Maven
- Build command: `mvn clean install`
- Test command: `mvn test`
- Test with coverage command: `mvn verify`
- Format command: _(none — checkstyle enforced at build time)_
- Module-specific build: yes (`mvn clean install -pl <module>`)
- Parallelized Maven: no
- Code style restrictions:
  - Java 17+ required
  - Import statements must be sorted; no star imports
  - Use `@Nullable` from `org.jspecify.annotations` for optional fields
  - NullAway + JSpecify enforced via Error Prone
  - Use Java `record` for immutable data types
  - Use `Assert.checkNotNullParam()` in compact constructors for required fields
  - Use `List.copyOf()` and `Map.copyOf()` for defensive copying
  - Apply Builder pattern for records with many fields
  - Do not split Java packages across Maven modules in production code
  - Serialization: Gson

## Version

788c6a703f406b4196cdcb5b30173bd971e1b6e0
