# demo-adk-server

## Prerequisites

Before running the tests, you must build the `spring-boot-starter-a2a-jsonrpc` project first.

## Building the project

To build the project, run the following command:

```bash
mvn clean install
```

## Testing the project

1. Run the server:
   ```bash
   mvn spring-boot:run
   ```
2. Use the `demo-adk-client` project to test the server.
