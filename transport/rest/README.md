# A2A REST Transport

REST transport implementation for the A2A Protocol, providing HTTP-based (`HTTP+JSON` protocol binding) communication between agents and clients.

## Overview

This module implements the REST transport layer for A2A protocol operations. All request and response bodies use [Protobuf JSON](https://protobuf.dev/programming-guides/proto3/#json) serialization (camelCase field names).

## API Endpoints

The `{tenant}` path prefix is optional on all endpoints. When omitted, the leading slash is also omitted (e.g., `/message:send` instead of `/{tenant}/message:send`).

### Send Message

Sends a message to the agent and blocks until the agent reaches a terminal or interrupted state, or returns immediately if `returnImmediately` is set.

```
POST /{tenant}/message:send
Content-Type: application/json
```

**Request body** (`SendMessageRequest`):
```json
{
  "message": {
    "messageId": "msg-1",
    "role": "ROLE_USER",
    "parts": [
      {"text": "Hello, what can you do?"}
    ],
    "contextId": "ctx-1"
  },
  "configuration": {
    "historyLength": 10,
    "returnImmediately": false,
    "acceptedOutputModes": ["text/plain"]
  }
}
```

**Response** — one of:
- `{"task": { ... }}` — a `Task` object when the agent creates/updates a task
- `{"message": { ... }}` — a `Message` object when the agent replies without a task

---

### Send Streaming Message

Sends a message and streams task updates as Server-Sent Events (SSE). Requires `capabilities.streaming = true` in the agent card.

```
POST /{tenant}/message:stream
Content-Type: application/json
Accept: text/event-stream
```

Request body is identical to `message:send`. Response is a stream of SSE events:

```
: SSE stream started

id: 0
data: {"statusUpdate":{"taskId":"task-1","contextId":"ctx-1","status":{"state":"TASK_STATE_WORKING"}}}

id: 1
data: {"artifactUpdate":{"taskId":"task-1","contextId":"ctx-1","artifact":{"artifactId":"a-1","parts":[{"text":"Hello!"}]}}}

id: 2
data: {"statusUpdate":{"taskId":"task-1","contextId":"ctx-1","status":{"state":"TASK_STATE_COMPLETED"}}}
```

Each `data` field contains a JSON-serialized `StreamResponse` with one of the following fields set:
- `task` — full `Task` snapshot
- `message` — a `Message` from the agent
- `statusUpdate` — a `TaskStatusUpdateEvent`
- `artifactUpdate` — a `TaskArtifactUpdateEvent`

---

### Get Task

Retrieves the current state of a task by ID.

```
GET /{tenant}/tasks/{taskId}?historyLength=10
```

| Query parameter | Type    | Description                                                                 |
|-----------------|---------|-----------------------------------------------------------------------------|
| `historyLength` | integer | Maximum number of history messages to include. Omit for no limit; `0` for none. |

**Response** — a `Task` object:
```json
{
  "id": "task-1",
  "contextId": "ctx-1",
  "status": {
    "state": "TASK_STATE_COMPLETED",
    "timestamp": "2023-10-27T10:00:00Z"
  },
  "artifacts": [],
  "history": []
}
```

---

### List Tasks

Lists tasks with optional filtering and pagination.

```
GET /{tenant}/tasks
```

| Query parameter        | Type    | Description                                                                                |
|------------------------|---------|--------------------------------------------------------------------------------------------|
| `contextId`            | string  | Filter by context ID.                                                                      |
| `status`               | string  | Filter by task state. One of the `TaskState` enum values (e.g. `TASK_STATE_COMPLETED`).    |
| `pageSize`             | integer | Maximum number of tasks to return (server default: 50, max: 100).                          |
| `pageToken`            | string  | Pagination token from a previous `ListTasks` response.                                     |
| `historyLength`        | integer | Maximum history messages to include per task.                                               |
| `statusTimestampAfter` | string  | ISO-8601 timestamp. Only return tasks whose status was updated at or after this time.      |
| `includeArtifacts`     | boolean | Whether to include artifacts in results. Defaults to `false`.                               |

**Response** (`ListTasksResponse`):
```json
{
  "tasks": [ ... ],
  "nextPageToken": "",
  "pageSize": 50,
  "totalSize": 3
}
```

**`TaskState` values:**

| Value                         | Description                                      |
|-------------------------------|--------------------------------------------------|
| `TASK_STATE_SUBMITTED`        | Task acknowledged, not yet processing.           |
| `TASK_STATE_WORKING`          | Task is actively being processed.                |
| `TASK_STATE_COMPLETED`        | Task finished successfully (terminal).           |
| `TASK_STATE_FAILED`           | Task finished with an error (terminal).          |
| `TASK_STATE_CANCELED`         | Task was canceled (terminal).                    |
| `TASK_STATE_REJECTED`         | Agent declined to perform the task (terminal).   |
| `TASK_STATE_INPUT_REQUIRED`   | Agent needs additional input (interrupted).      |
| `TASK_STATE_AUTH_REQUIRED`    | Authentication is required to proceed (interrupted). |

---

### Cancel Task

Requests cancellation of a running task. The agent should transition the task to `TASK_STATE_CANCELED`.

```
POST /{tenant}/tasks/{taskId}:cancel
Content-Type: application/json
```

Request body is optional (may be empty or contain a `metadata` field).

**Response** — the updated `Task` object on success.

---

### Subscribe to Task

Opens an SSE stream to receive real-time updates for an existing task. Requires `capabilities.streaming = true`. Returns `UnsupportedOperationError` if the task is already in a terminal state.

```
POST /{tenant}/tasks/{taskId}:subscribe
Accept: text/event-stream
```

Response is an SSE stream with the same event format as `message:stream`.

---

### Create Push Notification Config

Creates a webhook configuration for push notifications on a task.

```
POST /{tenant}/tasks/{taskId}/pushNotificationConfigs
Content-Type: application/json
```

**Request body** (`TaskPushNotificationConfig`):
```json
{
  "url": "https://example.com/webhook",
  "token": "optional-token",
  "authentication": {
    "scheme": "Bearer",
    "credentials": "my-token"
  }
}
```

**Response** — the created `TaskPushNotificationConfig` with its generated `id`. HTTP 201.

---

### Get Push Notification Config

```
GET /{tenant}/tasks/{taskId}/pushNotificationConfigs/{configId}
```

**Response** — the `TaskPushNotificationConfig` object.

---

### List Push Notification Configs

```
GET /{tenant}/tasks/{taskId}/pushNotificationConfigs
```

| Query parameter | Type    | Description                        |
|-----------------|---------|------------------------------------|
| `pageSize`      | integer | Maximum configurations to return.  |
| `pageToken`     | string  | Pagination token.                  |

**Response** (`ListTaskPushNotificationConfigsResponse`):
```json
{
  "configs": [ ... ],
  "nextPageToken": ""
}
```

---

### Delete Push Notification Config

```
DELETE /{tenant}/tasks/{taskId}/pushNotificationConfigs/{configId}
```

**Response** — HTTP 204 No Content on success.

---

### Get Agent Card

Public discovery endpoint. Returns the agent's self-describing manifest. No authentication required.

```
GET /.well-known/agent-card.json
```

**Response** — an `AgentCard` object:
```json
{
  "name": "My Agent",
  "description": "An example agent",
  "version": "1.0.0",
  "supportedInterfaces": [ ... ],
  "capabilities": {
    "streaming": true,
    "pushNotifications": false
  },
  "skills": [ ... ],
  "defaultInputModes": ["text/plain"],
  "defaultOutputModes": ["text/plain"]
}
```

---

### Get Extended Agent Card

Returns additional agent metadata for authenticated clients. Requires `capabilities.extendedAgentCard = true` in the public agent card.

```
GET /{tenant}/extendedAgentCard
```

**Response** — an `AgentCard` object (same structure as the public card, potentially with additional fields).

---

## Request Headers

| Header             | Description                                                                                     |
|--------------------|-------------------------------------------------------------------------------------------------|
| `X-A2A-Version`    | Requested A2A protocol version (e.g., `1.0`). Validated against the agent's supported versions. |
| `X-A2A-Extensions` | Comma-separated list of extension URIs the client supports. Required when the agent declares required extensions. |

---

## Error Handling

All error responses use [RFC 7807 Problem Details](https://tools.ietf.org/html/rfc7807) with `Content-Type: application/problem+json`.

```json
{
  "type": "https://a2a-protocol.org/errors/task-not-found",
  "title": "Task not found",
  "status": 404,
  "details": ""
}
```

| Field     | Type    | Description                                 |
|-----------|---------|---------------------------------------------|
| `type`    | string  | URI identifying the error type.             |
| `title`   | string  | Human-readable summary of the error.        |
| `status`  | integer | HTTP status code.                           |
| `details` | string  | Additional error context (may be empty).    |

### Error Types

| `type` URI                                                     | HTTP Status | Description                                                |
|----------------------------------------------------------------|-------------|------------------------------------------------------------|
| `https://a2a-protocol.org/errors/task-not-found`               | 404         | The requested task does not exist.                         |
| `https://a2a-protocol.org/errors/method-not-found`             | 404         | The endpoint does not exist.                               |
| `https://a2a-protocol.org/errors/invalid-request`              | 400         | Malformed request, missing required fields, or JSON parse error. |
| `https://a2a-protocol.org/errors/invalid-params`               | 422         | Invalid parameter values (e.g., negative `historyLength`). |
| `https://a2a-protocol.org/errors/extension-support-required`   | 400         | The agent requires an extension the client did not declare.|
| `https://a2a-protocol.org/errors/task-not-cancelable`          | 409         | The task cannot be canceled in its current state.          |
| `https://a2a-protocol.org/errors/content-type-not-supported`   | 415         | The requested content type is not supported.               |
| `https://a2a-protocol.org/errors/push-notification-not-supported` | 501      | Push notifications are not configured for this agent.      |
| `https://a2a-protocol.org/errors/unsupported-operation`        | 501         | The operation is not implemented or not applicable (e.g., subscribing to a finalized task). |
| `https://a2a-protocol.org/errors/version-not-supported`        | 400         | The requested protocol version is not supported.           |
| `https://a2a-protocol.org/errors/invalid-agent-response`       | 502         | The agent produced an invalid response.                    |
| `https://a2a-protocol.org/errors/extended-agent-card-not-configured` | 400   | The agent does not have an extended agent card configured. |
| `https://a2a-protocol.org/errors/internal-error`               | 500         | An unexpected server-side error occurred.                  |

---

## Client Integration

The REST client (`client/transport/rest`) automatically maps error responses to typed A2A exceptions. It supports both the current RFC 7807 format and the legacy `{"error": "...", "message": "..."}` format for backward compatibility.

```java
try {
    Task task = client.getTask(new TaskQueryParams("task-123"));
} catch (A2AClientException e) {
    if (e.getCause() instanceof TaskNotFoundError) {
        // Handle task not found
    } else if (e.getCause() instanceof UnsupportedOperationError) {
        // Handle unsupported operation
    }
}
```

---

## See Also

- [A2A Protocol Specification](https://a2a-protocol.org/)
- [RFC 7807 Problem Details](https://tools.ietf.org/html/rfc7807)
- [Protobuf JSON Encoding](https://protobuf.dev/programming-guides/proto3/#json)
