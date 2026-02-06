package io.a2a.client.transport.jsonrpc;

import static io.a2a.spec.AgentInterface.CURRENT_PROTOCOL_VERSION;

/**
 * Request and response messages used by the tests. These have been created following examples from
 * the <a href="https://google.github.io/A2A/specification/sample-messages">A2A sample messages</a>.
 */
public class JsonMessages {

    static final String AGENT_CARD = """
            {
                 "name": "GeoSpatial Route Planner Agent",
                 "description": "Provides advanced route planning, traffic analysis, and custom map generation services. This agent can calculate optimal routes, estimate travel times considering real-time traffic, and create personalized maps with points of interest.",
                 "supportedInterfaces" : [
                   {"url": "https://georoute-agent.example.com/a2a/v1", "protocolBinding": "JSONRPC", "tenant": ""},
                   {"url": "https://georoute-agent.example.com/a2a/grpc", "protocolBinding": "GRPC", "tenant": ""},
                   {"url": "https://georoute-agent.example.com/a2a/json", "protocolBinding": "HTTP+JSON", "tenant": ""}
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
                   "extendedAgentCard": false
                 },
                 "securitySchemes": {
                   "google": {
                     "openIdConnectSecurityScheme": {
                       "openIdConnectUrl": "https://accounts.google.com/.well-known/openid-configuration"
                     }
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
                 "signatures": [
                   {
                     "protected": "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpPU0UiLCJraWQiOiJrZXktMSIsImprdSI6Imh0dHBzOi8vZXhhbXBsZS5jb20vYWdlbnQvandrcy5qc29uIn0",
                     "signature": "QFdkNLNszlGj3z3u0YQGt_T9LixY3qtdQpZmsTdDHDe3fXV9y9-B3m2-XgCpzuhiLt8E0tV6HXoZKHv4GtHgKQ"
                   }
                 ]
               }""";

    static final String SEND_MESSAGE_TEST_REQUEST = """
           {
              "jsonrpc":"2.0",
              "method":"SendMessage",
              "params":{
                "message":{
                  "messageId":"message-1234",
                  "contextId":"context-1234",
                  "role":"ROLE_USER",
                  "parts":[
                    {
                      "text":"tell me a joke"
                    }
                  ],
                  "metadata":{
                    
                  }
                }
              }
            }""";

    static final String SEND_MESSAGE_TEST_RESPONSE = """
           {
              "jsonrpc":"2.0",
              "id": "cd4c76de-d54c-436c-8b9f-4c2703648d64",
              "result":{
                "task":{
                  "id":"de38c76d-d54c-436c-8b9f-4c2703648d64",
                  "contextId":"c295ea44-7543-4f78-b524-7a38915ad6e4",
                  "status":{
                    "state":"TASK_STATE_COMPLETED"
                  },
                  "artifacts":[
                    {
                      "artifactId":"artifact-1",
                      "name":"joke",
                      "parts":[
                        {
                          "text":"Why did the chicken cross the road? To get to the other side!"
                        }
                      ]
                    }
                  ],
                  "metadata":{
                    
                  }
                }
              }
            }""";

    static final String SEND_MESSAGE_TEST_REQUEST_WITH_MESSAGE_RESPONSE = """
        {
          "jsonrpc":"2.0",
          "method":"SendMessage",
          "params":{
            "message":{
              "messageId":"message-1234",
              "contextId":"context-1234",
              "role":"ROLE_USER",
              "parts":[
                {
                  "text":"tell me a joke"
                }
              ],
              "metadata":{
              }
            },
            "configuration":{
              "acceptedOutputModes":[
                "text"
              ],
              "blocking":true
            },
            "metadata":{

            }
          }
        }""";

    static final String SEND_MESSAGE_TEST_RESPONSE_WITH_MESSAGE_RESPONSE = """
            {
              "jsonrpc":"2.0",
              "id":1,
              "result":{
                "message": {
                  "messageId":"msg-456",
                  "contextId":"context-1234",
                  "role":"ROLE_AGENT",
                  "parts":[
                    {
                      "text":"Why did the chicken cross the road? To get to the other side!"
                    }
                  ],
                  "metadata":{
                  }
                }
              }
            }""";

    static final String SEND_MESSAGE_WITH_ERROR_TEST_REQUEST = """
            {
              "jsonrpc":"2.0",
              "method":"SendMessage",
              "params":{
                "message":{
                  "messageId":"message-1234",
                  "contextId":"context-1234",
                  "role":"ROLE_USER",
                  "parts":[
                    {
                      "text":"tell me a joke"
                    }
                  ],
                  "metadata":{
                    
                  }
                },
                "configuration":{
                  "acceptedOutputModes":[
                    "text"
                  ],
                  "blocking":true
                },
                "metadata":{
                  
                }
              }
            }""";

    static final String SEND_MESSAGE_ERROR_TEST_RESPONSE = """
            {
             "jsonrpc": "2.0",
              "id": "cd4c76de-d54c-436c-8b9f-4c2703648d64",
             "error": {
                "code": -32702,
                "message": "Invalid parameters",
                "data": "Hello world"
             }
            }""";

    static final String GET_TASK_TEST_REQUEST = """
            {
              "jsonrpc":"2.0",
              "method":"GetTask",
              "params":{
                "id":"de38c76d-d54c-436c-8b9f-4c2703648d64",
                "historyLength":10
              }
            }
            """;

    static final String GET_TASK_TEST_RESPONSE = """
            {
              "jsonrpc":"2.0",
              "id": "cd4c76de-d54c-436c-8b9f-4c2703648d64",
              "result":{
                "id":"de38c76d-d54c-436c-8b9f-4c2703648d64",
                "contextId":"c295ea44-7543-4f78-b524-7a38915ad6e4",
                "status":{
                  "state":"TASK_STATE_COMPLETED"
                },
                "artifacts":[
                  {
                    "artifactId":"artifact-1",
                    "parts":[
                      {
                        "text":"Why did the chicken cross the road? To get to the other side!"
                      }
                    ]
                  }
                ],
                "history":[
                  {
                    "role":"ROLE_USER",
                    "parts":[
                      {
                        "text":"tell me a joke"
                      },
                      {
                        "url":"file:///path/to/file.txt",
                        "mediaType":"text/plain"
                      },
                      {
                        "raw":"aGVsbG8=",
                        "filename":"hello.txt"
                      }
                    ],
                    "messageId":"message-123"
                  }
                ],
                "metadata":{
                  
                }
              }
            }
            """;

    static final String CANCEL_TASK_TEST_REQUEST = """
            {
              "jsonrpc":"2.0",
              "method":"CancelTask",
              "params":{
                "id":"de38c76d-d54c-436c-8b9f-4c2703648d64"
              }
            }
            """;

    static final String CANCEL_TASK_TEST_RESPONSE = """
            {
              "jsonrpc":"2.0",
              "id": "cd4c76de-d54c-436c-8b9f-4c2703648d64",
              "result":{
                  "id":"de38c76d-d54c-436c-8b9f-4c2703648d64",
                  "contextId":"c295ea44-7543-4f78-b524-7a38915ad6e4",
                  "status":{
                    "state":"TASK_STATE_CANCELED"
                  },
                  "metadata":{
                    
                  }
              }
            }
            """;

    static final String GET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_REQUEST = """
            {
              "jsonrpc":"2.0",
              "method":"GetTaskPushNotificationConfig",
              "params":{
                "taskId":"de38c76d-d54c-436c-8b9f-4c2703648d64",
                "id":"c295ea44-7543-4f78-b524-7a38915ad6e4"
              }
            }""";

    static final String GET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE = """
            {
             "jsonrpc": "2.0",
              "id": "cd4c76de-d54c-436c-8b9f-4c2703648d64",
             "result": {
              "taskId": "de38c76d-d54c-436c-8b9f-4c2703648d64",
              "id": "c295ea44-7543-4f78-b524-7a38915ad6e4",
              "pushNotificationConfig": {
               "url": "https://example.com/callback",
               "authentication": {
                "scheme": "jwt"
               }
              }
             }
            }
            """;

    static final String SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_REQUEST = """
           {
              "jsonrpc":"2.0",
              "method":"CreateTaskPushNotificationConfig",
              "params":{
                "taskId":"de38c76d-d54c-436c-8b9f-4c2703648d64",
                "configId":"c295ea44-7543-4f78-b524-7a38915ad6e4",
                "config":{
                  "url":"https://example.com/callback",
                  "authentication":{
                    "scheme":"jwt"
                  }
                }
              }
            }""";

    static final String SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE = """
            {
             "jsonrpc": "2.0",
             "id": "cd4c76de-d54c-436c-8b9f-4c2703648d64",
             "result": {
              "taskId":"de38c76d-d54c-436c-8b9f-4c2703648d64",
              "id":"c295ea44-7543-4f78-b524-7a38915ad6e4",
              "pushNotificationConfig": {
               "url": "https://example.com/callback",
               "authentication": {
                "scheme": "jwt"
               }
              }
             }
            }
            """;

    static final String SEND_MESSAGE_WITH_FILE_PART_TEST_REQUEST = """
             {
               "jsonrpc":"2.0",
               "method":"SendMessage",
               "params":{
                 "message":{
                   "messageId":"message-1234-with-file",
                   "contextId":"context-1234",
                   "role":"ROLE_USER",
                   "parts":[
                     {
                       "text":"analyze this image"
                     },
                     {
                       "url":"file:///path/to/image.jpg",
                       "mediaType":"image/jpeg"
                     }
                   ],
                   "metadata":{
                     
                   }
                 },
                 "configuration":{
                   "acceptedOutputModes":[
                     "text"
                   ],
                   "blocking":true
                 },
                 "metadata":{
                   
                 }
               }
             }""";

    static final String SEND_MESSAGE_WITH_FILE_PART_TEST_RESPONSE = """
            {
              "jsonrpc":"2.0",
              "id": "cd4c76de-d54c-436c-8b9f-4c2703648d64",
              "result":{
                "task":{
                  "id":"de38c76d-d54c-436c-8b9f-4c2703648d64",
                  "contextId":"c295ea44-7543-4f78-b524-7a38915ad6e4",
                  "status":{
                    "state":"TASK_STATE_COMPLETED"
                  },
                  "artifacts":[
                    {
                      "artifactId":"artifact-1",
                      "name":"image-analysis",
                      "parts":[
                        {
                          "text":"This is an image of a cat sitting on a windowsill."
                        }
                      ]
                    }
                  ],
                  "metadata":{
                    "test":"metadata-test"
                  }
                }
              }
            }""";

    static final String SEND_MESSAGE_WITH_DATA_PART_TEST_REQUEST = """
            {
              "jsonrpc":"2.0",
              "method":"SendMessage",
              "params":{
                "message":{
                  "messageId":"message-1234-with-data",
                  "contextId":"context-1234",
                  "role":"ROLE_USER",
                  "parts":[
                    {
                      "text":"process this data"
                    },
                    {
                      "data":{
                        "temperature":25.5,
                        "humidity":60.2,
                        "location":"San Francisco",
                        "timestamp":"2024-01-15T10:30:00Z"
                      }
                    }
                  ],
                  "metadata":{
                    
                  }
                },
                "configuration":{
                  "acceptedOutputModes":[
                    "text"
                  ],
                  "blocking":true
                },
                "metadata":{
                  
                }
              }
            }""";

    static final String SEND_MESSAGE_WITH_DATA_PART_TEST_RESPONSE = """
            {
              "jsonrpc":"2.0",
              "id": "cd4c76de-d54c-436c-8b9f-4c2703648d64",
              "result":{
                "task":{
                  "id":"de38c76d-d54c-436c-8b9f-4c2703648d64",
                  "contextId":"c295ea44-7543-4f78-b524-7a38915ad6e4",
                  "status":{
                    "state":"TASK_STATE_COMPLETED"
                  },
                  "artifacts":[
                    {
                      "artifactId":"artifact-1",
                      "name":"data-analysis",
                      "parts":[
                        {
                          "text":"Processed weather data: Temperature is 25.5°C, humidity is 60.2% in San Francisco."
                        }
                      ]
                    }
                  ],
                  "metadata":{
                    
                  }
                }
              }
            }""";

    static final String SEND_MESSAGE_WITH_MIXED_PARTS_TEST_REQUEST = """
            {
              "jsonrpc":"2.0",
              "method":"SendMessage",
              "params":{
                "message":{
                  "messageId":"message-1234-with-mixed",
                  "contextId":"context-1234",
                  "role":"ROLE_USER",
                  "parts":[
                    {
                      "text":"analyze this data and image"
                    },
                    {
                      "raw":"aGVsbG8=",
                      "mediaType":"image/png",
                      "filename":"chart.png"
                    },
                    {
                      "data":{
                        "chartType":"bar",
                        "dataPoints":[10.0, 20.0, 30.0, 40.0],
                        "labels":["Q1", "Q2", "Q3", "Q4"]
                      }
                    }
                  ],
                  "metadata":{
                    
                  }
                },
                "configuration":{
                  "acceptedOutputModes":[
                    "text"
                  ],
                  "blocking":true
                },
                "metadata":{
                  
                }
              }
            }""";

    static final String SEND_MESSAGE_WITH_MIXED_PARTS_TEST_RESPONSE = """
           {
              "jsonrpc":"2.0",
              "id": "cd4c76de-d54c-436c-8b9f-4c2703648d64",
              "result":{
                "task":{
                  "id":"de38c76d-d54c-436c-8b9f-4c2703648d64",
                  "contextId":"c295ea44-7543-4f78-b524-7a38915ad6e4",
                  "status":{
                    "state":"TASK_STATE_COMPLETED"
                  },
                  "artifacts":[
                    {
                      "artifactId":"artifact-1",
                      "name":"mixed-analysis",
                      "parts":[
                        {
                          "text":"Analyzed chart image and data: Bar chart showing quarterly data with values [10, 20, 30, 40]."
                        }
                      ]
                    }
                  ],
                  "metadata":{
                    
                  }
                }
              }
            }""";

    static final String GET_AUTHENTICATED_EXTENDED_AGENT_CARD_REQUEST = """
            {
                "jsonrpc": "2.0",
                "method": "GetExtendedAgentCard"
            }
            """;

    static final String GET_AUTHENTICATED_EXTENDED_AGENT_CARD_RESPONSE = """
            {
                "jsonrpc": "2.0",
                "id": "1",
                "result": {
                    "name": "GeoSpatial Route Planner Agent Extended",
                    "description": "Extended description",
                    "supportedInterfaces": [
                      {"url": "https://georoute-agent.example.com/a2a/v1", "protocolBinding": "JSONRPC", "tenant": ""}
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
                      "extendedAgentCard": true
                    },
                    "securitySchemes": {
                      "google": {
                        "openIdConnectSecurityScheme": {
                          "openIdConnectUrl": "https://accounts.google.com/.well-known/openid-configuration"
                        }
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
                    "signatures": [
                       {
                         "protected": "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpPU0UiLCJraWQiOiJrZXktMSIsImprdUI6Imh0dHBzOi8vZXhhbXBsZS5jb20vYWdlbnQvandrcy5qc29uIn0",
                         "signature": "QFdkNLNszlGj3z3u0YQGt_T9LixY3qtdQpZmsTdDHDe3fXV9y9-B3m2-XgCpzuhiLt8E0tV6HXoZKHv4GtHgKQ"
                       }
                     ]
                }
            }""";

    static final String AGENT_CARD_SUPPORTS_EXTENDED = """
            {
                "name": "GeoSpatial Route Planner Agent",
                "description": "Provides advanced route planning, traffic analysis, and custom map generation services. This agent can calculate optimal routes, estimate travel times considering real-time traffic, and create personalized maps with points of interest.",
                "supportedInterfaces": [
                  {"url": "https://georoute-agent.example.com/a2a/v1", "protocolBinding": "JSONRPC"}
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
                  "extendedAgentCard": true
                },
                "securitySchemes": {
                  "google": {
                    "openIdConnectSecurityScheme": {
                      "openIdConnectUrl": "https://accounts.google.com/.well-known/openid-configuration"
                    }
                  }
                },
                "security": [{ "schemes": { "google": { "list": ["openid", "profile", "email"] } } }],
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
                ]
              }""";
}
