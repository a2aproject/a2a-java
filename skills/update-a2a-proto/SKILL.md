---
name: update-a2a-proto
description: Update the A2A Protobuf file (a2a.proto) when the A2A protocol specification changes. Use when the user mentions updating the spec, syncing with upstream A2A, or when a new version of the A2A protocol is released.
compatibility: Requires curl
allowed-tools: Bash(mvn:*) Bash(curl:*) Read Edit Write Glob Grep
---

# Update a2a.proto

Update the A2A gRPC proto file from the upstream specification repository and regenerate Java sources.

## Step 1: Capture the current state

Before updating, record what we have now so we can diff later.

1. Read `spec-grpc/src/main/proto/a2a.proto` to note the current commit hash.
2. Save a copy of the file for diffing:

## Step 2: Download the updated specification

1. Ask the user if they want to update from the `main` branch or pass a `tag` or a `commit cheksum`
2. Download the latest `a2a.proto` from the upstream A2A repository at https://github.com/a2aproject/A2A/blob/main/specification/a2a.proto and save it to `spec-grpc/src/main/proto/a2a.proto`.
3. Update the `java_package` option in the downloaded proto file to:
   ```
   option java_package = "io.a2a.grpc";
   ```
4. Update the comment tracking the upstream commit hash (line starting with `// From commit`) to the commit hash / tag of the downloaded proto file.

## Step 3: Analyze specification changes

Compare the old and new versions of the specification to identify changes:

1. Diff `specification/a2a.proto` with the backup to find:
   - New or changed message types
   - New or changed RPC methods
   - New or changed fields

## Step 4: Regenerate gRPC classes

1. Regenerate gRPC Java classes by running:
   ```bash
   cd spec-grpc && mvn clean install -Dskip.protobuf.generate=false
   ```

## Step 5: Update the code

Based on the spec diff, update the relevant files

Summarize the changes for the user before proceeding.

## Step 6: Validate

Run the tests on all modules to verify the update

```bash
mvn clean install
```

Fix any issues before proceeding.

## Step 7: Audit changes

Before committing, audit all code changes.

Highlight any new, modified, or removed changes in the API in the client, common and spec modules

Verify the build succeeds for the full project:

   ```bash
   mvn clean install
   ```

## Step 8: Summarize

Present the user with a summary of:
- The old and new spec commit hashes
- What changed in the specification
- What was updated in the code
- Any areas that need manual review or additional test coverage
