package io.a2a.client.transport.jsonrpc;

/**
 * Contains JSON strings for testing SSE streaming.
 */
public class JsonStreamingMessages {

    public static final String STREAMING_TASK_EVENT = """
            data: {
                  "jsonrpc": "2.0",
                  "id": "1234",
                  "result": {
                    "kind": "task",
                    "id": "task-123",
                    "contextId": "context-456",
                    "status": {
                      "state": "working"
                    }
                  }
            }
            """;


    public static final String STREAMING_MESSAGE_EVENT = """
            data: {
                  "jsonrpc": "2.0",
                  "id": "1234",
                  "result": {
                    "kind": "message",
                    "role": "agent",
                    "messageId": "msg-123",
                    "contextId": "context-456",
                    "parts": [
                      {
                        "kind": "text",
                        "text": "Hello, world!"
                      }
                    ]
                  }
            }""";

    public static final String STREAMING_STATUS_UPDATE_EVENT = """
            data: {
                  "jsonrpc": "2.0",
                  "id": "1234",
                  "result": {
                    "taskId": "1",
                    "contextId": "2",
                    "status": {
                        "state": "submitted"
                    },
                    "final": false,
                    "kind": "status-update"
                  }
            }""";

    public static final String STREAMING_STATUS_UPDATE_EVENT_FINAL = """
            data: {
                  "jsonrpc": "2.0",
                  "id": "1234",
                  "result": {
                    "taskId": "1",
                    "contextId": "2",
                    "status": {
                        "state": "completed"
                    },
                    "final": true,
                    "kind": "status-update"
                  }
            }""";

    public static final String STREAMING_ARTIFACT_UPDATE_EVENT = """
             data: {
                  "jsonrpc": "2.0",
                  "id": "1234",
                  "result": {
                    "kind": "artifact-update",
                    "taskId": "1",
                    "contextId": "2",
                    "append": false,
                    "lastChunk": true,
                    "artifact": {
                        "artifactId": "artifact-1",
                        "parts": [
                         {
                            "kind": "text",
                            "text": "Why did the chicken cross the road? To get to the other side!"
                         }
                        ]
                    }
                  }
               }
            }""";

    public static final String STREAMING_ERROR_EVENT = """
            data: {
                  "jsonrpc": "2.0",
                  "id": "1234",
                  "error": {
                    "code": -32602,
                    "message": "Invalid parameters",
                    "data": "Missing required field"
                  }
             }""";

    public static final String SEND_MESSAGE_STREAMING_TEST_REQUEST = """
            {
             "jsonrpc": "2.0",
             "method": "message/stream",
             "params": {
              "message": {
               "role": "user",
               "parts": [
                {
                 "kind": "text",
                 "text": "tell me some jokes"
                }
               ],
               "messageId": "message-1234",
               "contextId": "context-1234",
               "kind": "message"
              },
              "configuration": {
                "acceptedOutputModes": ["text"],
                "blocking": false
              },
             }
            }""";

    static final String SEND_MESSAGE_STREAMING_TEST_RESPONSE =
            "event: message\n" +
            "data: {\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"id\":\"2\",\"contextId\":\"context-1234\",\"status\":{\"state\":\"completed\"},\"artifacts\":[{\"artifactId\":\"artifact-1\",\"name\":\"joke\",\"parts\":[{\"kind\":\"text\",\"text\":\"Why did the chicken cross the road? To get to the other side!\"}]}],\"metadata\":{},\"kind\":\"task\"}}\n\n";

    static final String TASK_RESUBSCRIPTION_REQUEST_TEST_RESPONSE =
            "event: message\n" +
                    "data: {\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"id\":\"2\",\"contextId\":\"context-1234\",\"status\":{\"state\":\"completed\"},\"artifacts\":[{\"artifactId\":\"artifact-1\",\"name\":\"joke\",\"parts\":[{\"kind\":\"text\",\"text\":\"Why did the chicken cross the road? To get to the other side!\"}]}],\"metadata\":{},\"kind\":\"task\"}}\n\n";

    public static final String TASK_RESUBSCRIPTION_TEST_REQUEST = """
            {
             "jsonrpc": "2.0",
             "method": "tasks/resubscribe",
             "params": {
                "id": "task-1234"
             }
            }""";
}