package io.a2a.client;

/**
 * Request and response messages used by the tests. These have been created following examples from
 * the <a href="https://google.github.io/A2A/specification/sample-messages">A2A sample messages</a>.
 */
public class JsonMessages {

    static final String AGENT_CARD = """
            {
                 "protocolVersion": "0.2.9",
                 "name": "GeoSpatial Route Planner Agent",
                 "description": "Provides advanced route planning, traffic analysis, and custom map generation services. This agent can calculate optimal routes, estimate travel times considering real-time traffic, and create personalized maps with points of interest.",
                 "url": "https://georoute-agent.example.com/a2a/v1",
                 "preferredTransport": "JSONRPC",
                 "additionalInterfaces" : [
                   {"url": "https://georoute-agent.example.com/a2a/v1", "transport": "JSONRPC"},
                   {"url": "https://georoute-agent.example.com/a2a/grpc", "transport": "GRPC"},
                   {"url": "https://georoute-agent.example.com/a2a/json", "transport": "HTTP+JSON"}
                 ],
                 "provider": {
                   "organization": "Example Geo Services Inc.",
                   "url": "https://www.examplegeoservices.com"
                 },
                 "iconUrl": "https://georoute-agent.example.com/icon.png",
                 "version": "1.2.0",
                 "documentationUrl": "https://docs.examplegeoservices.com/georoute-agent/api",
                 "capabilities": {
                   "streaming": true,
                   "pushNotifications": true,
                   "stateTransitionHistory": false
                 },
                 "securitySchemes": {
                   "google": {
                     "type": "openIdConnect",
                     "openIdConnectUrl": "https://accounts.google.com/.well-known/openid-configuration"
                   }
                 },
                 "security": [{ "google": ["openid", "profile", "email"] }],
                 "defaultInputModes": ["application/json", "text/plain"],
                 "defaultOutputModes": ["application/json", "image/png"],
                 "skills": [
                   {
                     "id": "route-optimizer-traffic",
                     "name": "Traffic-Aware Route Optimizer",
                     "description": "Calculates the optimal driving route between two or more locations, taking into account real-time traffic conditions, road closures, and user preferences (e.g., avoid tolls, prefer highways).",
                     "tags": ["maps", "routing", "navigation", "directions", "traffic"],
                     "examples": [
                       "Plan a route from '1600 Amphitheatre Parkway, Mountain View, CA' to 'San Francisco International Airport' avoiding tolls.",
                       "{\\"origin\\": {\\"lat\\": 37.422, \\"lng\\": -122.084}, \\"destination\\": {\\"lat\\": 37.7749, \\"lng\\": -122.4194}, \\"preferences\\": [\\"avoid_ferries\\"]}"
                     ],
                     "inputModes": ["application/json", "text/plain"],
                     "outputModes": [
                       "application/json",
                       "application/vnd.geo+json",
                       "text/html"
                     ]
                   },
                   {
                     "id": "custom-map-generator",
                     "name": "Personalized Map Generator",
                     "description": "Creates custom map images or interactive map views based on user-defined points of interest, routes, and style preferences. Can overlay data layers.",
                     "tags": ["maps", "customization", "visualization", "cartography"],
                     "examples": [
                       "Generate a map of my upcoming road trip with all planned stops highlighted.",
                       "Show me a map visualizing all coffee shops within a 1-mile radius of my current location."
                     ],
                     "inputModes": ["application/json"],
                     "outputModes": [
                       "image/png",
                       "image/jpeg",
                       "application/json",
                       "text/html"
                     ]
                   }
                 ],
                 "supportsAuthenticatedExtendedCard": true,
                 "signatures": [
                   {
                     "protected": "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpPU0UiLCJraWQiOiJrZXktMSIsImprdSI6Imh0dHBzOi8vZXhhbXBsZS5jb20vYWdlbnQvandrcy5qc29uIn0",
                     "signature": "QFdkNLNszlGj3z3u0YQGt_T9LixY3qtdQpZmsTdDHDe3fXV9y9-B3m2-XgCpzuhiLt8E0tV6HXoZKHv4GtHgKQ"
                   }
                 ]
               }""";

    static final String AUTHENTICATION_EXTENDED_AGENT_CARD = """
            {
                "name": "GeoSpatial Route Planner Agent Extended",
                "description": "Extended description",
                "url": "https://georoute-agent.example.com/a2a/v1",
                "provider": {
                  "organization": "Example Geo Services Inc.",
                  "url": "https://www.examplegeoservices.com"
                },
                "iconUrl": "https://georoute-agent.example.com/icon.png",
                "version": "1.2.0",
                "documentationUrl": "https://docs.examplegeoservices.com/georoute-agent/api",
                "capabilities": {
                  "streaming": true,
                  "pushNotifications": true,
                  "stateTransitionHistory": false
                },
                "securitySchemes": {
                  "google": {
                    "type": "openIdConnect",
                    "openIdConnectUrl": "https://accounts.google.com/.well-known/openid-configuration"
                  }
                },
                "security": [{ "google": ["openid", "profile", "email"] }],
                "defaultInputModes": ["application/json", "text/plain"],
                "defaultOutputModes": ["application/json", "image/png"],
                "skills": [
                  {
                    "id": "route-optimizer-traffic",
                    "name": "Traffic-Aware Route Optimizer",
                    "description": "Calculates the optimal driving route between two or more locations, taking into account real-time traffic conditions, road closures, and user preferences (e.g., avoid tolls, prefer highways).",
                    "tags": ["maps", "routing", "navigation", "directions", "traffic"],
                    "examples": [
                      "Plan a route from '1600 Amphitheatre Parkway, Mountain View, CA' to 'San Francisco International Airport' avoiding tolls.",
                      "{\\"origin\\": {\\"lat\\": 37.422, \\"lng\\": -122.084}, \\"destination\\": {\\"lat\\": 37.7749, \\"lng\\": -122.4194}, \\"preferences\\": [\\"avoid_ferries\\"]}"
                    ],
                    "inputModes": ["application/json", "text/plain"],
                    "outputModes": [
                      "application/json",
                      "application/vnd.geo+json",
                      "text/html"
                    ]
                  },
                  {
                    "id": "custom-map-generator",
                    "name": "Personalized Map Generator",
                    "description": "Creates custom map images or interactive map views based on user-defined points of interest, routes, and style preferences. Can overlay data layers.",
                    "tags": ["maps", "customization", "visualization", "cartography"],
                    "examples": [
                      "Generate a map of my upcoming road trip with all planned stops highlighted.",
                      "Show me a map visualizing all coffee shops within a 1-mile radius of my current location."
                    ],
                    "inputModes": ["application/json"],
                    "outputModes": [
                      "image/png",
                      "image/jpeg",
                      "application/json",
                      "text/html"
                    ]
                  },
                  {
                    "id": "skill-extended",
                    "name": "Extended Skill",
                    "description": "This is an extended skill.",
                    "tags": ["extended"]
                  }
                ],
                "supportsAuthenticatedExtendedCard": true,
                "protocolVersion": "0.2.9",
                "signatures": [
                   {
                     "protected": "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpPU0UiLCJraWQiOiJrZXktMSIsImprdSI6Imh0dHBzOi8vZXhhbXBsZS5jb20vYWdlbnQvandrcy5qc29uIn0",
                     "signature": "QFdkNLNszlGj3z3u0YQGt_T9LixY3qtdQpZmsTdDHDe3fXV9y9-B3m2-XgCpzuhiLt8E0tV6HXoZKHv4GtHgKQ"
                   }
                 ]
              }""";


    static final String SEND_MESSAGE_TEST_REQUEST = """
            {
             "jsonrpc": "2.0",
             "id": "request-1234",
             "method": "message/send",
             "params": {
              "message": {
               "role": "user",
               "parts": [
                {
                 "kind": "text",
                 "text": "tell me a joke"
                }
               ],
               "messageId": "message-1234",
               "contextId": "context-1234",
               "kind": "message"
              },
              "configuration": {
                "acceptedOutputModes": ["text"],
                "blocking": true
              },
             }
            }""";

    static final String SEND_MESSAGE_TEST_RESPONSE = """
            {
             "jsonrpc": "2.0",
             "id": 1,
             "result": {
              "id": "de38c76d-d54c-436c-8b9f-4c2703648d64",
              "contextId": "c295ea44-7543-4f78-b524-7a38915ad6e4",
              "status": {
               "state": "completed"
              },
              "artifacts": [
               {
                "artifactId": "artifact-1",
                "name": "joke",
                "parts": [
                 {
                  "kind": "text",
                  "text": "Why did the chicken cross the road? To get to the other side!"
                 }
                ]
               }
              ],
              "metadata": {},
              "kind": "task"
             }
            }""";

    static final String SEND_MESSAGE_TEST_REQUEST_WITH_MESSAGE_RESPONSE = """
            {
             "jsonrpc": "2.0",
             "id": "request-1234-with-message-response",
             "method": "message/send",
             "params": {
              "message": {
               "role": "user",
               "parts": [
                {
                 "kind": "text",
                 "text": "tell me a joke"
                }
               ],
               "messageId": "message-1234",
               "contextId": "context-1234",
               "kind": "message"
              },
              "configuration": {
                "acceptedOutputModes": ["text"],
                "blocking": true
              },
             }
            }""";


    static final String SEND_MESSAGE_TEST_RESPONSE_WITH_MESSAGE_RESPONSE = """
            {
             "jsonrpc": "2.0",
             "id": 1,
             "result": {
              "role": "agent",
                "parts": [
                 {
                  "kind": "text",
                  "text": "Why did the chicken cross the road? To get to the other side!"
                 }
                ],
                "messageId": "msg-456",
                "kind": "message"
             }
            }""";

    static final String SEND_MESSAGE_WITH_ERROR_TEST_REQUEST = """
            {
             "jsonrpc": "2.0",
             "id": "request-1234-with-error",
             "method": "message/send",
             "params": {
              "message": {
               "role": "user",
               "parts": [
                {
                 "kind": "text",
                 "text": "tell me a joke"
                }
               ],
               "messageId": "message-1234",
               "contextId": "context-1234",
               "kind": "message"
              },
              "configuration": {
                "acceptedOutputModes": ["text"],
                "blocking": true
              },
             }
            }""";

    static final String SEND_MESSAGE_ERROR_TEST_RESPONSE = """
            {
             "jsonrpc": "2.0",
             "error": {
                "code": -32702,
                "message": "Invalid parameters",
                "data": "Hello world"
             }
            }""";

    static final String GET_TASK_TEST_REQUEST = """
            {
             "jsonrpc": "2.0",
             "id": "request-1234",
             "method": "tasks/get",
             "params": {
              "id": "de38c76d-d54c-436c-8b9f-4c2703648d64",
              "historyLength": 10
             }
            }
            """;

    static final String GET_TASK_TEST_RESPONSE = """
            {
             "jsonrpc": "2.0",
             "id": 1,
             "result": {
              "id": "de38c76d-d54c-436c-8b9f-4c2703648d64",
              "contextId": "c295ea44-7543-4f78-b524-7a38915ad6e4",
              "status": {
               "state": "completed"
              },
              "artifacts": [
               {
                "artifactId": "artifact-1",
                "parts": [
                 {
                  "kind": "text",
                  "text": "Why did the chicken cross the road? To get to the other side!"
                 }
                ]
               }
              ],
              "history": [
               {
                "role": "user",
                "parts": [
                 {
                  "kind": "text",
                  "text": "tell me a joke"
                 },
                 {
                  "kind": "file",
                  "file": {
                     "uri": "file:///path/to/file.txt",
                     "mimeType": "text/plain"
                  }
                 },
                 {
                  "kind": "file",
                  "file": {
                     "bytes": "aGVsbG8=",
                     "name": "hello.txt"
                  }
                 }
                ],
                "messageId": "message-123",
                "kind": "message"
               }
              ],
              "metadata": {},
              "kind": "task"
             }
            }
            """;

    static final String CANCEL_TASK_TEST_REQUEST = """
            {
             "jsonrpc": "2.0",
             "id": "request-1234",
             "method": "tasks/cancel",
             "params": {
              "id": "de38c76d-d54c-436c-8b9f-4c2703648d64",
              "metadata": {}
             }
            }
            """;

    static final String CANCEL_TASK_TEST_RESPONSE = """
            {
             "jsonrpc": "2.0",
             "id": 1,
             "result": {
              "id": "de38c76d-d54c-436c-8b9f-4c2703648d64",
              "contextId": "c295ea44-7543-4f78-b524-7a38915ad6e4",
              "status": {
               "state": "canceled"
              },
              "metadata": {},
              "kind" : "task"
             }
            }
            """;

    static final String GET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_REQUEST = """
            {
             "jsonrpc": "2.0",
             "id": "1",
             "method": "tasks/pushNotificationConfig/get",
             "params": {
              "id": "de38c76d-d54c-436c-8b9f-4c2703648d64",
              "metadata": {},
             }
            }
            """;

    static final String GET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE = """
            {
             "jsonrpc": "2.0",
             "id": 1,
             "result": {
              "taskId": "de38c76d-d54c-436c-8b9f-4c2703648d64",
              "pushNotificationConfig": {
               "url": "https://example.com/callback",
               "authentication": {
                "schemes": ["jwt"]
               }
              }
             }
            }
            """;

    static final String SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_REQUEST = """
            {
             "jsonrpc": "2.0",
             "id": "1",
             "method": "tasks/pushNotificationConfig/set",
             "params": {
              "taskId": "de38c76d-d54c-436c-8b9f-4c2703648d64",
              "pushNotificationConfig": {
               "url": "https://example.com/callback",
               "authentication": {
                "schemes": ["jwt"]
               }
              }
             }
            }""";

    static final String SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE = """
            {
             "jsonrpc": "2.0",
             "id": 1,
             "result": {
              "taskId": "de38c76d-d54c-436c-8b9f-4c2703648d64",
              "pushNotificationConfig": {
               "url": "https://example.com/callback",
               "authentication": {
                "schemes": ["jwt"]
               }
              }
             }
            }
            """;

    static final String SEND_MESSAGE_WITH_FILE_PART_TEST_REQUEST = """
            {
             "jsonrpc": "2.0",
             "id": "request-1234-with-file",
             "method": "message/send",
             "params": {
              "message": {
               "role": "user",
               "parts": [
                {
                 "kind": "text",
                 "text": "analyze this image"
                },
                {
                 "kind": "file",
                 "file": {
                  "uri": "file:///path/to/image.jpg",
                  "mimeType": "image/jpeg"
                 }
                }
               ],
               "messageId": "message-1234-with-file",
               "contextId": "context-1234",
               "kind": "message"
              },
              "configuration": {
                "acceptedOutputModes": ["text"],
                "blocking": true
              }
             }
            }""";

    static final String SEND_MESSAGE_WITH_FILE_PART_TEST_RESPONSE = """
            {
             "jsonrpc": "2.0",
             "id": 1,
             "result": {
              "id": "de38c76d-d54c-436c-8b9f-4c2703648d64",
              "contextId": "c295ea44-7543-4f78-b524-7a38915ad6e4",
              "status": {
               "state": "completed"
              },
              "artifacts": [
               {
                "artifactId": "artifact-1",
                "name": "image-analysis",
                "parts": [
                 {
                  "kind": "text",
                  "text": "This is an image of a cat sitting on a windowsill."
                 }
                ]
               }
              ],
              "metadata": {},
              "kind": "task"
             }
            }""";

    static final String SEND_MESSAGE_WITH_DATA_PART_TEST_REQUEST = """
            {
             "jsonrpc": "2.0",
             "id": "request-1234-with-data",
             "method": "message/send",
             "params": {
              "message": {
               "role": "user",
               "parts": [
                {
                 "kind": "text",
                 "text": "process this data"
                },
                {
                 "kind": "data",
                 "data": {
                  "temperature": 25.5,
                  "humidity": 60.2,
                  "location": "San Francisco",
                  "timestamp": "2024-01-15T10:30:00Z"
                 }
                }
               ],
               "messageId": "message-1234-with-data",
               "contextId": "context-1234",
               "kind": "message"
              },
              "configuration": {
                "acceptedOutputModes": ["text"],
                "blocking": true
              }
             }
            }""";

    static final String SEND_MESSAGE_WITH_DATA_PART_TEST_RESPONSE = """
            {
             "jsonrpc": "2.0",
             "id": 1,
             "result": {
              "id": "de38c76d-d54c-436c-8b9f-4c2703648d64",
              "contextId": "c295ea44-7543-4f78-b524-7a38915ad6e4",
              "status": {
               "state": "completed"
              },
              "artifacts": [
               {
                "artifactId": "artifact-1",
                "name": "data-analysis",
                "parts": [
                 {
                  "kind": "text",
                  "text": "Processed weather data: Temperature is 25.5°C, humidity is 60.2% in San Francisco."
                 }
                ]
               }
              ],
              "metadata": {},
              "kind": "task"
             }
            }""";

    static final String SEND_MESSAGE_WITH_MIXED_PARTS_TEST_REQUEST = """
            {
             "jsonrpc": "2.0",
             "id": "request-1234-with-mixed",
             "method": "message/send",
             "params": {
              "message": {
               "role": "user",
               "parts": [
                {
                 "kind": "text",
                 "text": "analyze this data and image"
                },
                {
                 "kind": "file",
                 "file": {
                  "bytes": "aGVsbG8=",
                  "name": "chart.png",
                  "mimeType": "image/png"
                 }
                },
                {
                 "kind": "data",
                 "data": {
                  "chartType": "bar",
                  "dataPoints": [10, 20, 30, 40],
                  "labels": ["Q1", "Q2", "Q3", "Q4"]
                 }
                }
               ],
               "messageId": "message-1234-with-mixed",
               "contextId": "context-1234",
               "kind": "message"
              },
              "configuration": {
                "acceptedOutputModes": ["text"],
                "blocking": true
              }
             }
            }""";

    static final String SEND_MESSAGE_WITH_MIXED_PARTS_TEST_RESPONSE = """
            {
             "jsonrpc": "2.0",
             "id": 1,
             "result": {
              "id": "de38c76d-d54c-436c-8b9f-4c2703648d64",
              "contextId": "c295ea44-7543-4f78-b524-7a38915ad6e4",
              "status": {
               "state": "completed"
              },
              "artifacts": [
               {
                "artifactId": "artifact-1",
                "name": "mixed-analysis",
                "parts": [
                 {
                  "kind": "text",
                  "text": "Analyzed chart image and data: Bar chart showing quarterly data with values [10, 20, 30, 40]."
                 }
                ]
               }
              ],
              "metadata": {},
              "kind": "task"
             }
            }""";

}
