---
title: Bill of Materials (BOM)
description: Dependency management BOMs for the A2A Java SDK — SDK, Extras, and Reference BOMs.
layout: page
---

# Bill of Materials (BOM)

The A2A Java SDK provides three BOMs for different use cases, so you can manage dependency versions in one place.

## BOM Modules

### SDK BOM

**Artifact:** `org.a2aproject.sdk:a2a-java-sdk-bom`

Includes all A2A SDK core modules (spec, server, client, transport), core third-party dependencies, Jakarta APIs, and test utilities.

**Use this BOM when:** Building A2A agents with any framework (Quarkus, Spring Boot, vanilla Java, etc.)

### Extras BOM

**Artifact:** `org.a2aproject.sdk:a2a-java-sdk-extras-bom`

Includes everything from the SDK BOM plus server-side enhancement modules (database persistence, distributed queue management, etc.).

**Use this BOM when:** Building production A2A servers needing advanced server-side features beyond the core SDK.

### Reference BOM

**Artifact:** `org.a2aproject.sdk:a2a-java-sdk-reference-bom`

Includes everything from the SDK BOM plus the Quarkus BOM (complete Quarkus platform), A2A reference implementation modules, and the TCK module for testing.

**Use this BOM when:** Building Quarkus-based A2A agents or reference implementations.

## Usage

### SDK BOM (Any Framework)

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.a2aproject.sdk</groupId>
            <artifactId>a2a-java-sdk-bom</artifactId>
            <version>$\{org.a2aproject.sdk.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- No version needed - managed by BOM -->
    <dependency>
        <groupId>org.a2aproject.sdk</groupId>
        <artifactId>a2a-java-sdk-server-common</artifactId>
    </dependency>
    <dependency>
        <groupId>org.a2aproject.sdk</groupId>
        <artifactId>a2a-java-sdk-transport-jsonrpc</artifactId>
    </dependency>
</dependencies>
```

### Extras BOM (Database Persistence, Distributed Deployments)

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.a2aproject.sdk</groupId>
            <artifactId>a2a-java-sdk-extras-bom</artifactId>
            <version>$\{org.a2aproject.sdk.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>org.a2aproject.sdk</groupId>
        <artifactId>a2a-java-sdk-server-common</artifactId>
    </dependency>
    <dependency>
        <groupId>org.a2aproject.sdk</groupId>
        <artifactId>a2a-java-extras-task-store-database-jpa</artifactId>
    </dependency>
</dependencies>
```

### Reference BOM (Quarkus)

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.a2aproject.sdk</groupId>
            <artifactId>a2a-java-sdk-reference-bom</artifactId>
            <version>$\{org.a2aproject.sdk.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- A2A SDK and Quarkus versions both managed -->
    <dependency>
        <groupId>org.a2aproject.sdk</groupId>
        <artifactId>a2a-java-sdk-reference-jsonrpc</artifactId>
    </dependency>
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-arc</artifactId>
    </dependency>
</dependencies>
```
