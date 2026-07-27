---
title: Getting Started
description: Get started with the A2A Java SDK — overview, guides, and next steps.
layout: page
---

# Getting Started

The A2A Java SDK is a multi-module Maven library for implementing the [Agent2Agent (A2A) Protocol](https://a2a-protocol.org/) in Java. It provides both client and server support for agent communication over JSON-RPC, gRPC, and REST transports.

## What You Can Build

- **A2A Servers** — Expose your Java agent as an A2A-compliant service that other agents and clients can discover and communicate with.
- **A2A Clients** — Connect to any A2A-compliant agent, with streaming, push notifications, and task management.

## Quick Start

Add the reference server dependency to your Maven project:

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-reference-jsonrpc</artifactId>
    <version>$\{org.a2aproject.sdk.version}</version>
</dependency>
```

Then implement an `AgentExecutor` and define an `AgentCard`. See the [Server Guide](server) for the full walkthrough.

## Guides

| Guide | Description |
|-------|-------------|
| [Server Guide](server) | Run your Java application as an A2A server |
| [Client Guide](client) | Communicate with A2A-compliant agents |
| [Configuration](configuration) | Config properties, MicroProfile Config, custom providers |
| [Task Authorization](authorization) | Per-user access control for multi-user deployments |
| [Backward Compatibility](compatibility) | Serve v1.0 and v0.3 clients simultaneously |
| [Extras](extras) | Optional add-ons: database storage, OpenTelemetry, HTTP clients |
| [BOMs](boms) | Dependency management with Bill of Materials |
| [Examples](examples) | Hello World walkthroughs and sample applications |

## Requirements

- Java 17+
- Maven 3.8+
