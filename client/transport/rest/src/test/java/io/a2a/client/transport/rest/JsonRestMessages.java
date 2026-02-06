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
              "id": "de38c76d-d54c-436c-8b9f-4c2703648d64"
            }""";

    static final String CANCEL_TASK_TEST_RESPONSE = """
            {
                "id": "de38c76d-d54c-436c-8b9f-4c2703648d64",
                "contextId": "c295ea44-7543-4f78-b524-7a38915ad6e4",
                "status": {
                  "state": "TASK_STATE_CANCELED"
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
                  "url": "file:///path/to/file.txt",
                  "mediaType": "text/plain"
                 },
                 {
                  "raw": "aGVsbG8=",
                  "mediaType": "text/plain"
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
                "supportedInterfaces": [
                  {"url": "https://georoute-agent.example.com/a2a/v1", "protocolBinding": "HTTP+JSON"}
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
                  "pushNotifications": true
                },
                "securitySchemes": {
                  "google": {
                    "type": "openIdConnect",
                    "openIdConnectUrl": "https://accounts.google.com/.well-known/openid-configuration"
                  }
                },
                "securityRequirements": [{ "schemes": { "google": { "list": ["openid", "profile", "email"] } } }],
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
                "supportsExtendedAgentCard": false,
                "protocolVersion": "0.2.5"
              }""";

    static final String AGENT_CARD_SUPPORTS_EXTENDED = """
            {
                "name": "GeoSpatial Route Planner Agent",
                "description": "Provides advanced route planning, traffic analysis, and custom map generation services. This agent can calculate optimal routes, estimate travel times considering real-time traffic, and create personalized maps with points of interest.",
                "supportedInterfaces": [
                  {"url": "https://georoute-agent.example.com/a2a/v1", "protocolBinding": "HTTP+JSON"}
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
                  "pushNotifications": true
                },
                "securitySchemes": {
                  "google": {
                    "type": "openIdConnect",
                    "openIdConnectUrl": "https://accounts.google.com/.well-known/openid-configuration"
                  }
                },
                "securityRequirements": [{ "schemes": { "google": { "list": ["openid", "profile", "email"] } } }],
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
                "supportsExtendedAgentCard": true,
                "protocolVersion": "0.2.5"
              }""";

    static final String AUTHENTICATION_EXTENDED_AGENT_CARD = """
            {
                "name": "GeoSpatial Route Planner Agent Extended",
                "description": "Extended description",
                "supportedInterfaces": [
                  {"url": "https://georoute-agent.example.com/a2a/v1", "protocolBinding": "HTTP+JSON"}
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
                  "pushNotifications": true
                },
                "securitySchemes": {
                  "google": {
                    "type": "openIdConnect",
                    "openIdConnectUrl": "https://accounts.google.com/.well-known/openid-configuration"
                  }
                },
                "securityRequirements": [{ "schemes": { "google": { "list": ["openid", "profile", "email"] } } }],
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
                "supportsExtendedAgentCard": true,
                "protocolVersion": "0.2.5"
              }""";

    static final String GET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE = """
            {
              "taskId": "de38c76d-d54c-436c-8b9f-4c2703648d64",
              "id": "10",
              "pushNotificationConfig": {
                "url": "https://example.com/callback",
                "authentication": {
                  "scheme": "jwt"
                }
              }
            }""";
    static final String LIST_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE = """
            {
              "configs":[
                {
                  "taskId": "de38c76d-d54c-436c-8b9f-4c2703648d64",
                  "id": "10",
                  "pushNotificationConfig": {
                    "url": "https://example.com/callback",
                    "authentication": {
                      "scheme": "jwt"
                    }
                  }
                },
                {
                  "taskId": "de38c76d-d54c-436c-8b9f-4c2703648d64",
                  "id": "5",
                  "pushNotificationConfig": {
                    "url": "https://test.com/callback"
                  }
                }
              ]
            }""";


    static final String SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_REQUEST = """
            {
              "taskId": "de38c76d-d54c-436c-8b9f-4c2703648d64",
              "configId": "default-config-id",
              "config": {
                "url": "https://example.com/callback",
                "authentication": {
                  "scheme": "jwt"
                }
              }
            }""";

    static final String SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE = """
            {
              "taskId": "de38c76d-d54c-436c-8b9f-4c2703648d64",
              "id": "10",
              "pushNotificationConfig": {
                "url": "https://example.com/callback",
                "authentication": {
                  "scheme": "jwt"
                }
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
             "method": "SubscribeToTask",
             "params": {
                "id": "task-1234"
             }
            }""";
}
