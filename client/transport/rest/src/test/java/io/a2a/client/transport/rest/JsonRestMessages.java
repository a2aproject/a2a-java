package io.a2a.client.transport.rest;

/**
 * Request and response messages used by the tests. These have been created following examples from
 * the <a href="https://google.github.io/A2A/specification/sample-messages">A2A sample messages</a>.
 */
public class JsonRestMessages {

    static final String SEND_MESSAGE_TEST_REQUEST = """
            {
              "message":
                {
                  "messageId": "message-1234",
                  "contextId": "context-1234",
                  "role": "ROLE_USER",
                  "parts": [{
                    "text": "tell me a joke"
                  }],
                  "metadata": {
                  }
              }
            }""";

    static final String SEND_MESSAGE_TEST_RESPONSE = """
            {
              "task": {
                "id": "9b511af4-b27c-47fa-aecf-2a93c08a44f8",
                "contextId": "context-1234",
                "status": {
                  "state": "TASK_STATE_SUBMITTED"
                },
                "history": [
                  {
                    "messageId": "message-1234",
                    "contextId": "context-1234",
                    "taskId": "9b511af4-b27c-47fa-aecf-2a93c08a44f8",
                    "role": "ROLE_USER",
                    "parts": [
                      {
                        "text": "tell me a joke"
                      }
                    ],
                    "metadata": {}
                  }
                ]
              }
            }""";

    static final String CANCEL_TASK_TEST_REQUEST = """
            {
              "name": "tasks/de38c76d-d54c-436c-8b9f-4c2703648d64"
            }""";

    static final String CANCEL_TASK_TEST_RESPONSE = """
            {
                "id": "de38c76d-d54c-436c-8b9f-4c2703648d64",
                "contextId": "c295ea44-7543-4f78-b524-7a38915ad6e4",
                "status": {
                  "state": "TASK_STATE_CANCELLED"
                },
                "metadata": {}
            }""";

    static final String GET_TASK_TEST_RESPONSE = """
            {
              "id": "de38c76d-d54c-436c-8b9f-4c2703648d64",
              "contextId": "c295ea44-7543-4f78-b524-7a38915ad6e4",
              "status": {
               "state": "TASK_STATE_COMPLETED"
              },
              "artifacts": [
               {
                "artifactId": "artifact-1",
                "parts": [
                 {
                  "text": "Why did the chicken cross the road? To get to the other side!"
                 }
                ]
               }
              ],
              "history": [
               {
                "role": "ROLE_USER",
                "parts": [
                 {
                  "text": "tell me a joke"
                 },
                 {
                  "file": {
                     "file_with_uri": "file:///path/to/file.txt",
                     "mimeType": "text/plain"
                  }
                 },
                 {
                  "file": {
                     "file_with_bytes": "aGVsbG8=",
                     "mimeType": "text/plain"
                  }
                 }
                ],
                "messageId": "message-123"
               }
              ],
              "metadata": {}
            }
            """;

    static final String AGENT_CARD = """
            {
                "name": "GeoSpatial Route Planner Agent",
                "description": "Provides advanced route planning, traffic analysis, and custom map generation services. This agent can calculate optimal routes, estimate travel times considering real-time traffic, and create personalized maps with points of interest.",
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
                  }
                ],
                "supportsAuthenticatedExtendedCard": false,
                "protocolVersion": "0.2.5"
              }""";

    static final String AGENT_CARD_SUPPORTS_EXTENDED = """
            {
                "name": "GeoSpatial Route Planner Agent",
                "description": "Provides advanced route planning, traffic analysis, and custom map generation services. This agent can calculate optimal routes, estimate travel times considering real-time traffic, and create personalized maps with points of interest.",
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
                  }
                ],
                "supportsAuthenticatedExtendedCard": true,
                "protocolVersion": "0.2.5"
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
                "protocolVersion": "0.2.5"
              }""";

    static final String SEND_MESSAGE_TEST_REQUEST_WITH_MESSAGE_RESPONSE = """
            {
             "jsonrpc": "2.0",
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

    static final String GET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE = """
            {
              "name": "tasks/de38c76d-d54c-436c-8b9f-4c2703648d64/pushNotificationConfigs/10",
              "pushNotificationConfig": {
                "url": "https://example.com/callback",
                "authentication": {
                  "schemes": ["jwt"]
                }
              }
            }""";
    static final String LIST_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE = """
            {
              "configs":[
                {
                  "name": "tasks/de38c76d-d54c-436c-8b9f-4c2703648d64/pushNotificationConfigs/10",
                  "pushNotificationConfig": {
                    "url": "https://example.com/callback",
                    "authentication": {
                      "schemes": ["jwt"]
                    }
                  }
                },
                {
                  "name": "tasks/de38c76d-d54c-436c-8b9f-4c2703648d64/pushNotificationConfigs/5",
                  "pushNotificationConfig": {
                    "url": "https://test.com/callback"
                  }
                }
              ]
            }""";


    static final String SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_REQUEST = """
            {
              "parent": "tasks/de38c76d-d54c-436c-8b9f-4c2703648d64",
              "config": {
                "name": "tasks/de38c76d-d54c-436c-8b9f-4c2703648d64/pushNotificationConfigs",
                "pushNotificationConfig": {
                  "url": "https://example.com/callback",
                  "authentication": {
                    "schemes": [ "jwt" ]
                  }
                }
              }
            }""";

    static final String SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE = """
            {
              "name": "tasks/de38c76d-d54c-436c-8b9f-4c2703648d64/pushNotificationConfigs/10",
              "pushNotificationConfig": {
                "url": "https://example.com/callback",
                "authentication": {
                  "schemes": ["jwt"]
                }
              }
            }""";

    static final String SEND_MESSAGE_WITH_FILE_PART_TEST_REQUEST = """
            {
             "jsonrpc": "2.0",
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

    public static final String SEND_MESSAGE_STREAMING_TEST_REQUEST = """
            {
              "message": {
                "role": "ROLE_USER",
                "parts": [
                  {
                    "text": "tell me some jokes"
                  }
                ],
                "messageId": "message-1234",
                "contextId": "context-1234"
              },
              "configuration": {
                "acceptedOutputModes": ["text"]
              }
            }""";
    static final String SEND_MESSAGE_STREAMING_TEST_RESPONSE
            = "event: message\n"
            + "data: {\"task\":{\"id\":\"2\",\"contextId\":\"context-1234\",\"status\":{\"state\":\"TASK_STATE_SUBMITTED\"},\"artifacts\":[{\"artifactId\":\"artifact-1\",\"name\":\"joke\",\"parts\":[{\"text\":\"Why did the chicken cross the road? To get to the other side!\"}]}],\"metadata\":{}}}\n\n";

    static final String TASK_RESUBSCRIPTION_REQUEST_TEST_RESPONSE
            = "event: message\n"
            + "data: {\"task\":{\"id\":\"2\",\"contextId\":\"context-1234\",\"status\":{\"state\":\"TASK_STATE_COMPLETED\"},\"artifacts\":[{\"artifactId\":\"artifact-1\",\"name\":\"joke\",\"parts\":[{\"text\":\"Why did the chicken cross the road? To get to the other side!\"}]}],\"metadata\":{}}}\n\n";
    public static final String TASK_RESUBSCRIPTION_TEST_REQUEST = """
            {
             "jsonrpc": "2.0",
             "method": "tasks/resubscribe",
             "params": {
                "id": "task-1234"
             }
            }""";
}
