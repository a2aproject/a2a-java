# Fix A2A TCK Compatibility Issue

## Description
Analyzes and fixes A2A Transport Compatibility Kit (TCK) issues by understanding the specification, reproducing the failure, implementing the fix, and validating it works.

## Triggers
- Issue references TCK, compatibility, or transport-specific behavior
- Keywords: "TCK", "compatibility", "HTTP+JSON", "gRPC", "JSON-RPC", "specification", "proto"
- Issue mentions transport layer discrepancies

## Input
- Issue number from a2aproject/a2a-java repository
- Optional: A2A spec reference (branch/tag/commit, defaults to `main`)

## Workflow

### 1. Fetch Issue Details
```bash
gh issue view <issue-number> --repo a2aproject/a2a-java --json title,body,labels,url
```

Parse issue to identify:
- Affected transport(s): HTTP+JSON, gRPC, JSON-RPC
- Expected behavior from specification
- Actual behavior (error message, reproducer)
- Specification section references

### 2. Read Specification (if needed)
Fetch from https://github.com/a2aproject/A2A with specified ref (default: main):
- `specification/grpc/a2a.proto` - for proto definitions and HTTP transcoding
- `docs/specification.md` - for detailed protocol requirements

Focus on sections referenced in the issue.

### 3. Analyze Code
Locate relevant code based on transport:
- **HTTP+JSON**: `transport/rest`
- **gRPC**: `transport/grpc`
- **JSON-RPC**: `transport/jsonrpc`

Identify root cause by comparing:
- What the spec says should happen
- What the code currently does
- Why they differ

**Optional**: If issue includes a curl/grpcurl reproducer, run it manually to validate the issue is genuine.

### 4. Determine Affected Transports
**CRITICAL**: If issue doesn't specify a single transport, you MUST create reproducers for ALL affected transports.

Issue mentions specific transport → Test that one only
Issue generic or mentions "all transports" → Test HTTP+JSON, gRPC, AND JSON-RPC

### 5. Create Temporary Reproducer(s)
Create test in appropriate module. Choose location based on test complexity:

**Option A: transport/* modules (Unit Tests)** - Use when:
- Testing handler logic directly
- Need custom AgentCard configuration (e.g., capability flags)
- Simpler to set up specific test conditions
- **HTTP+JSON** → `transport/rest/src/test/java/io/a2a/transport/rest/handler/RestHandlerTest.java`
- **gRPC** → `transport/grpc/src/test/java/io/a2a/transport/grpc/handler/GrpcHandlerTest.java`
- **JSON-RPC** → `transport/jsonrpc/src/test/java/io/a2a/transport/jsonrpc/handler/JSONRPCHandlerTest.java`

**Option B: reference/* modules (Integration Tests)** - Use when:
- Testing full request/response cycle
- Need real server behavior
- Testing with standard agent configuration
- **HTTP+JSON** → `reference/rest/src/test/java/.../`
- **gRPC** → `reference/grpc/src/test/java/.../`
- **JSON-RPC** → `reference/jsonrpc/src/test/java/.../`

Reproducer requirements:
- Follow the exact scenario from the issue
- Use request format per specification (e.g., NO taskId in body for HTTP+JSON)
- Be named clearly: `test_Issue<number>_Reproducer()`
- Assert the WRONG behavior that issue reports (should fail)

Example:
```java
@Test
public void test_Issue732_Reproducer() {
    // Per spec: taskId should NOT be in request body for HTTP+JSON
    String requestBody = """
        {
          "id": "my-config-001",
          "url": "https://example.com/webhook"
        }""";

    HTTPRestResponse response = handler.createTaskPushNotificationConfiguration(
        context, "", requestBody, taskId);

    assertEquals(201, response.getStatusCode());
}
```

### 6. Run Reproducer(s) - Confirm Failure
**CRITICAL**: You MUST run the reproducer and see it FAIL before proceeding to fix.

For transport/* modules:
```bash
mvn test -Dtest=<TestClass>#test_Issue<number>_Reproducer -pl transport/<transport>
```

For reference/* modules:
```bash
mvn test -Dtest=<TestClass>#test_Issue<number>_Reproducer -pl reference/<transport>
```

**Required verification (DO NOT SKIP)**:
- ❌ Test MUST fail with the exact error mentioned in the issue
- ❌ Error message, status code, or exception type MUST match issue description
- ❌ If testing multiple transports, ALL reproducers must fail

**If reproducer doesn't fail as expected**:
- STOP - Do not proceed to fix
- Reassess understanding of the issue
- Check if test conditions match issue scenario
- Verify you're testing the right transport/endpoint

**Only proceed to step 7 after confirming all reproducers fail correctly.**

### 7. Implement Fix
Make minimal code changes to fix the root cause.

**If multiple transports are affected**: Fix ALL of them before proceeding to verification.

Common patterns:
- **Missing path parameter extraction**: Add `builder.setFieldName(pathParam)`
- **Wrong validation**: Adjust validation logic to match spec
- **Incorrect mapping**: Fix proto/domain conversion
- **Wrong error type**: Return correct error based on failure reason

### 8. Run Reproducer(s) - Confirm Fix
Run ALL reproducers you created in step 5.

For transport/* modules:
```bash
mvn test -Dtest=<TestClass>#test_Issue<number>_Reproducer -pl transport/<transport>
```

For reference/* modules:
```bash
mvn test -Dtest=<TestClass>#test_Issue<number>_Reproducer -pl reference/<transport>
```

**Required verification**:
- ✅ ALL reproducers must now PASS
- ✅ Test output shows expected behavior (correct status, no error, etc.)

If any reproducer still fails, debug and refine the fix.

### 9. Verify Backward Compatibility
Run full test suite for ALL modified transport modules to ensure no regressions:

```bash
mvn test -pl transport/rest,transport/jsonrpc,transport/grpc
```

If you also modified reference modules or only created reproducers there:
```bash
mvn test -pl reference/rest,reference/jsonrpc,reference/grpc
```

All existing tests must pass.

### 10. Delete Temporary Reproducer(s)
Remove ALL test methods or files created in step 5.

If you added a method to existing test class:
- Delete just the `test_Issue<number>_Reproducer()` method

If you created a new test file:
- Delete the entire file (e.g., `Issue733ReproducerTest.java`)

### 11. Commit
Add only the impacted files (NOT the temporary reproducers):
```bash
git add <changed-files>
git commit -m "fix: <concise description>

<explanation of spec requirement and how code was fixed>

Applied to <list transports if multiple>.

Fixes #<issue-number>"
```

Example for multi-transport fix:
```bash
git add transport/rest/src/main/java/io/a2a/transport/rest/handler/RestHandler.java \
        transport/jsonrpc/src/main/java/io/a2a/transport/jsonrpc/handler/JSONRPCHandler.java \
        transport/grpc/src/main/java/io/a2a/transport/grpc/handler/GrpcHandler.java
git commit -m "fix: Return UnsupportedOperationError when capability is disabled

Applied to all three transports: HTTP+JSON, JSON-RPC, and gRPC.

Fixes #733"
```

## Common Root Causes

### HTTP+JSON with `body: "*"`
Proto definition with path parameters and `body: "*"` means:
- Path parameters extracted from URL
- Body contains remaining fields only
- Handler must set path params into builder

Example proto:
```protobuf
rpc CreateTaskPushNotificationConfig(...) {
  option (google.api.http) = {
    post: "/tasks/{task_id}/pushNotificationConfigs"
    body: "*"
  };
}
```

Fix pattern:
```java
// Extract from URL path and set in builder
builder.setTaskId(taskId);
```

### Field Validation Errors
"X is required" errors often mean:
- Field should come from URL path, not body
- Handler isn't setting the path parameter
- Check proto's HTTP annotation

## Test Location Decision Guide

### transport/* modules (Unit Tests)
**Best for**:
- Testing handler logic with custom configurations
- Issues requiring specific AgentCard capabilities (e.g., streaming=false, extendedAgentCard=false)
- Faster test execution
- More control over test setup

**Test file locations**:
- `transport/rest/src/test/java/io/a2a/transport/rest/handler/RestHandlerTest.java`
- `transport/grpc/src/test/java/io/a2a/transport/grpc/handler/GrpcHandlerTest.java`
- `transport/jsonrpc/src/test/java/io/a2a/transport/jsonrpc/handler/JSONRPCHandlerTest.java`

### reference/* modules (Integration Tests)
**Best for**:
- Testing full request/response cycles
- Issues related to server behavior
- Testing with standard agent configuration
- Real-world scenario validation

**Structure**:
```
reference/
├── rest/          # HTTP+JSON integration tests
├── grpc/          # gRPC integration tests
└── jsonrpc/       # JSON-RPC integration tests
```

## Examples

### Example 1: Single Transport Issue
Issue #732: CreateTaskPushNotificationConfig required taskId in body (HTTP+JSON only)

1. ✅ Fetched issue - HTTP+JSON transport, expects taskId from URL
2. ✅ Read spec - `body: "*"` means taskId from path
3. ✅ Found root cause - RestHandler wasn't setting taskId from path param
4. ✅ Issue specifies HTTP+JSON only - test only that transport
5. ✅ Created reproducer in reference/rest without taskId in body
6. ✅ **Ran reproducer - CONFIRMED FAILURE with 422 status**
7. ✅ Fixed - added `builder.setTaskId(taskId)` to RestHandler
8. ✅ **Ran reproducer - CONFIRMED PASS**
9. ✅ Ran full test suite - all tests pass
10. ✅ Deleted reproducer
11. ✅ Committed (RestHandler.java only)

### Example 2: Multi-Transport Issue
Issue #733: GetExtendedAgentCard returns wrong error when capability disabled

1. ✅ Fetched issue - affects all transports (not specified which)
2. ✅ Read spec - should return UnsupportedOperationError when capability=false
3. ✅ Found root cause - handlers check config before capability
4. ✅ **Issue affects all transports - must test all three**
5. ✅ Created reproducers in transport/rest, transport/jsonrpc, transport/grpc
6. ✅ **Ran all reproducers - CONFIRMED all fail (wrong error type)**
7. ✅ Fixed all three handlers - check capability before config
8. ✅ **Ran all reproducers - CONFIRMED all pass**
9. ✅ Ran full test suite - all tests pass
10. ✅ Deleted all three reproducers
11. ✅ Committed (all three handler files)

## Critical Success Factors

### Must Do
- ✅ **ALWAYS run reproducer BEFORE fixing** - Confirms you understand the issue
- ✅ **Test ALL affected transports** - Don't assume single transport unless issue specifies
- ✅ **Confirm exact failure** - Error type, status code, message must match issue
- ✅ **Verify fix with reproducers** - All must pass before proceeding
- ✅ **Delete all reproducers** - Keep test suites clean

### Best Practices
- a2a.proto and spec are source of truth for compatibility
- Reproducers prove understanding before fixing
- Minimal, targeted fixes are better than broad changes
- Choose test location (transport/* vs reference/*) based on requirements
- Run full test suite to ensure no regressions
- Commit only code changes, never temporary reproducers

### Common Pitfalls
- ❌ Fixing without running reproducer first
- ❌ Testing only one transport when issue affects multiple
- ❌ Not confirming exact error before proceeding
- ❌ Committing temporary reproducer tests
- ❌ Skipping backward compatibility verification
