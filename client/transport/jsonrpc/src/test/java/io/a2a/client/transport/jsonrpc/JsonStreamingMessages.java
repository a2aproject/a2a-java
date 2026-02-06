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
                    "task": {
                      "id": "task-123",
                      "contextId": "context-456",
                      "status": {
                        "state": "TASK_STATE_WORKING"
                      }
                    }
                  }
            }
            """;


    public static final String STREAMING_MESSAGE_EVENT = """
            data: {
                  "jsonrpc": "2.0",
                  "id": "1234",
                  "result": {
                    "message": {
                      "role": "ROLE_AGENT",
                      "messageId": "msg-123",
                      "contextId": "context-456",
                      "parts": [
                        {
                          "text": "Hello, world!"
                        }
                      ]
                    }
                  }
            }""";

    public static final String STREAMING_STATUS_UPDATE_EVENT = """
            data: {
                  "jsonrpc": "2.0",
                  "id": "1234",
                  "result": {
                    "statusUpdate": {
                      "taskId": "1",
                      "contextId": "2",
                      "status": {
                        "state": "TASK_STATE_SUBMITTED"
                      }
                    }
                  }
            }""";

    public static final String STREAMING_STATUS_UPDATE_EVENT_FINAL = """
            data: {
                  "jsonrpc": "2.0",
                  "id": "1234",
                  "result": {
                    "statusUpdate": {
                      "taskId": "1",
                      "contextId": "2",
                      "status": {
                        "state": "TASK_STATE_COMPLETED"
                      }
                    }
                  }
            }""";

    public static final String STREAMING_ARTIFACT_UPDATE_EVENT = """
             data: {
                  "jsonrpc": "2.0",
                  "id": "1234",
                  "result": {
                    "artifactUpdate": {
                      "taskId": "1",
                      "contextId": "2",
                      "append": false,
                      "lastChunk": true,
                      "artifact": {
                        "artifactId": "artifact-1",
                        "parts": [
                          {
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
              "jsonrpc":"2.0",
              "method":"SendStreamingMessage",
              "params":{
                "message":{
                  "messageId":"message-1234",
                  "contextId":"context-1234",
                  "role":"ROLE_USER",
                  "parts":[
                    {
                      "text":"tell me some jokes"
                    }
                  ],
                  "metadata":{
                    
                  }
                },
                "configuration":{
                  "acceptedOutputModes":[
                    "text"
                  ]
                },
                "metadata":{
                  
                }
              }
            }""";

    static final String SEND_MESSAGE_STREAMING_TEST_RESPONSE =
            """
            event: message
            data: {"jsonrpc":"2.0","id":1,"result":{"task":{"id":"2","contextId":"context-1234","status":{"state":"TASK_STATE_COMPLETED"},"artifacts":[{"artifactId":"artifact-1","name":"joke","parts":[{"text":"Why did the chicken cross the road? To get to the other side!"}]}],"metadata":{}}}}
            
            """;

    static final String TASK_SUBSCRIPTION_REQUEST_TEST_RESPONSE =
            """
            event: message
            data: {"jsonrpc":"2.0","id":1,"result":{"task":{"id":"2","contextId":"context-1234","status":{"state":"TASK_STATE_COMPLETED"},"artifacts":[{"artifactId":"artifact-1","name":"joke","parts":[{"text":"Why did the chicken cross the road? To get to the other side!"}]}],"metadata":{}}}}
            
            """;

    public static final String TASK_SUBSCRIPTION_TEST_REQUEST = """
            {
              "jsonrpc":"2.0",
              "method":"SubscribeToTask",
              "params":{
                "id":"task-1234"
              }
            }""";
}