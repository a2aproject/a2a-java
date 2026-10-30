---
title: Storage & Persistence
description: JPA-backed TaskStore and PushNotificationConfigStore for the A2A Java SDK, providing database persistence for production and load-balanced deployments.
layout: page
---

# Storage & Persistence

The default SDK uses in-memory stores that are lost on restart. For production deployments — especially load-balanced environments — replace them with JPA-backed stores that persist data to a relational database.

Both JPA stores use the Jakarta Persistence API (JPA 3.0+), making them suitable for any JPA provider and Jakarta EE application server. They share the same persistence unit name (`a2a-java`) and database configuration, so they can share a single datasource.

> **Note:** Stored objects are serialized to JSON according to the current A2A specification version. Future specification versions may change the format. These stores are intended for the operational lifetime of tasks, not long-term archival. If you wish to keep objects stored between protocol versions, you may have to implement migration of the stored data.

## JPA Task Store

Replaces `InMemoryTaskStore` with a database-backed implementation. The `JpaDatabaseTaskStore` is annotated to take precedence over the default — it is a drop-in replacement.

### 1. Add Dependency

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-extras-task-store-database-jpa</artifactId>
</dependency>
```

### 2. Configure Database

#### For Quarkus Reference Servers

Add to your `application.properties`:

```properties
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/a2a_db
quarkus.datasource.username=your_username
quarkus.datasource.password=your_password
quarkus.hibernate-orm."a2a-java".datasource=<default>
quarkus.hibernate-orm."a2a-java".packages=org.a2aproject.sdk.extras.taskstore.database.jpa
# Application-managed production migrations; update is only for development
quarkus.hibernate-orm."a2a-java".database.generation=none
```

#### For WildFly/Jakarta EE Servers

> **Note:** On a Jakarta EE application server, the JPA dependencies are provided by the runtime. Mark them with `<scope>provided</scope>` in your POM to avoid packaging conflicts.

Create or update your `persistence.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<persistence xmlns="https://jakarta.ee/xml/ns/persistence" version="3.0">
    <persistence-unit name="a2a-java" transaction-type="JTA">
        <jta-data-source>java:jboss/datasources/A2ADataSource</jta-data-source>

        <class>org.a2aproject.sdk.extras.taskstore.database.jpa.JpaTask</class>
        <class>org.a2aproject.sdk.extras.taskstore.database.jpa.JpaTaskOwner</class>
        <exclude-unlisted-classes>true</exclude-unlisted-classes>

        <properties>
            <property name="jakarta.persistence.schema-generation.database.action" value="none"/>
            <property name="hibernate.dialect" value="org.hibernate.dialect.PostgreSQLDialect"/>
        </properties>
    </persistence-unit>
</persistence>
```

### 3. Database Schema

Schema creation is controlled by the application. Production upgrades require an application-managed migration for both task and ownership tables before using the updated store. Register both entities in the same `a2a-java` unit; package scanning discovers them in Quarkus. The task schema includes:

```sql
CREATE TABLE a2a_tasks (
    task_id VARCHAR(255) PRIMARY KEY,
    context_id VARCHAR(255),
    state VARCHAR(255),
    status_timestamp TIMESTAMP,
    task_data TEXT NOT NULL,
    finalized_at TIMESTAMP
);
```

### 4. Ownership schema and authorization selection

PostgreSQL 16 ownership DDL:

```sql
CREATE TABLE a2a_task_owners (
    task_id VARCHAR(255) PRIMARY KEY,
    subject_key BYTEA NOT NULL
);
CREATE INDEX a2a_task_owner_subject ON a2a_task_owners (subject_key, task_id);
```

MySQL 8 ownership DDL:

```sql
CREATE TABLE a2a_task_owners (
    task_id VARCHAR(255) PRIMARY KEY,
    subject_key VARBINARY(1024) NOT NULL
);
CREATE INDEX a2a_task_owner_subject ON a2a_task_owners (subject_key, task_id);
```

Verify the actual binary type and complete index. PostgreSQL must use `BYTEA`, not a large-object OID. MySQL requires variable-length `VARBINARY(1024)`, not padded `BINARY`, text with a binary collation, or a prefix index. Other databases need an equivalent exact variable-length binary mapping.

The SDK uses strict UTF-8 and rejects malformed surrogates or subjects exceeding 1,024 encoded bytes before ownership access. Case, accents, Unicode-normalization and trailing-space distinctions remain significant; there is no normalization, trimming, truncation, padding or lossy hashing. Backfill only from a trusted source preserving original subject keys. Audit legacy text-collation associations and lossless conversion; a migration cannot recover distinctions already discarded.

Adding the module selects the task store but does not enable an authorization provider. Explicitly select the supplied owner-only provider in Quarkus:

```properties
quarkus.arc.selected-alternatives=org.a2aproject.sdk.extras.taskstore.database.jpa.JpaTaskAuthorizationProvider
```

For portable CDI, expose an ordinary application producer instead of relying on archive-scoped `beans.xml` alternative selection:

```java
@ApplicationScoped
public class TaskAuthorizationConfiguration {
    @Produces
    @Dependent
    TaskAuthorizationProvider taskAuthorization(JpaTaskOwnershipRepository ownership) {
        return new JpaTaskAuthorizationProvider(ownership);
    }
}
```

Use the Jakarta CDI annotations and the JPA module's provider/repository types. Choose the producer or Quarkus alternative selection, avoiding ambiguous providers. The repository uses an intercepted JTA helper: insertion and duplicate confirmation each run in a separate `REQUIRES_NEW` transaction with a fresh persistence context. Confirmation starts after insertion rollback, so a stale repeatable-read caller cannot hide committed ownership or become rollback-only. Outside CDI, use `JpaTaskOwnershipRepository.create(entityManagerFactory, transactionManager)` and pass `new JpaTaskAuthorizationProvider(repository)` to `DefaultRequestHandler.builder().authorizationProvider(provider)`. The factory must be JTA-enabled for the same database/transaction manager. Do not directly instantiate the transaction helper.

Authenticate `ServerCallContext.getUser()` and supply a username unique across your identity domain. Multiple issuers or tenant-local names require stable namespaced keys or a custom policy. Anonymous users and unknown ownership are denied; recording is first-writer-wins across replicas. An owner row may precede its task, so there is no immediate task foreign key. A persisted task remains inaccessible until ownership is recorded. Store deletion removes ownership in the same transaction; direct database maintenance must clean up both tables. Replicas must share ownership storage.

With no provider, authorization is required by default. Only deliberate providerless deployments should set `a2a.authorization.required=false`. Configured providers remain authoritative with that setting. An explicitly configured allow-all provider may expose `JpaTaskReadScope.All`, including unowned tasks, if its individual checks agree.

A supported `JpaTaskAuthorizationCapability` restricts both database count and page queries. The exact authorized total excludes the cursor and is repeated on every page; the page query alone uses the cursor. The handler's list gate and final checks remain. Counts and pages follow configured database isolation, without an implied stronger snapshot guarantee.

Unsupported providers retain iterative checks in Java. Their exact count may fail at `a2a.max-scanned-tasks`; `a2a.authorization-scan-batch-size` controls fetch size. These limits do not apply to capability-based queries. Lookup or capability exceptions fail the request rather than switching policy. See [Task Authorization](../authorization.md) for capability contracts and [Configuration](../configuration.md) for fallback limits.

### Persistence Unit Name

The module uses the persistence unit name `"a2a-java"`. Ensure your `persistence.xml` defines a persistence unit with this name.

## JPA Push Notification Config Store

Replaces `InMemoryPushNotificationConfigStore` with a database-backed implementation, ensuring push notification subscriptions survive application restarts. The `JpaDatabasePushNotificationConfigStore` is annotated to take precedence over the default — it is a drop-in replacement.

### 1. Add Dependency

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-extras-push-notification-config-store-database-jpa</artifactId>
</dependency>
```

### 2. Configure Database

Uses the same datasource and persistence unit (`a2a-java`) as the JPA Task Store.

#### For Quarkus Reference Servers

The same `application.properties` configuration as the JPA Task Store applies.

#### For WildFly/Jakarta EE Servers

Create or update your `persistence.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<persistence xmlns="https://jakarta.ee/xml/ns/persistence" version="3.0">
    <persistence-unit name="a2a-java" transaction-type="JTA">
        <jta-data-source>java:jboss/datasources/A2ADataSource</jta-data-source>

        <class>org.a2aproject.sdk.extras.pushnotificationconfigstore.database.jpa.JpaPushNotificationConfig</class>
        <exclude-unlisted-classes>true</exclude-unlisted-classes>

        <properties>
            <property name="jakarta.persistence.schema-generation.database.action" value="none"/>
            <property name="hibernate.dialect" value="org.hibernate.dialect.PostgreSQLDialect"/>
        </properties>
    </persistence-unit>
</persistence>
```

### 3. Database Schema

When application schema generation is enabled, it creates a table with a composite primary key. Otherwise apply the corresponding migration:

```sql
CREATE TABLE a2a_push_notification_configs (
    task_id VARCHAR(255) NOT NULL,
    config_id VARCHAR(255) NOT NULL,
    task_data TEXT NOT NULL,
    protocol_version VARCHAR(255),
    created_at TIMESTAMP,
    PRIMARY KEY (task_id, config_id)
);
```

## Using Both Stores Together

When using both JPA stores, combine their entity classes in a single `persistence.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<persistence xmlns="https://jakarta.ee/xml/ns/persistence" version="3.0">
    <persistence-unit name="a2a-java" transaction-type="JTA">
        <jta-data-source>java:jboss/datasources/A2ADataSource</jta-data-source>

        <class>org.a2aproject.sdk.extras.taskstore.database.jpa.JpaTask</class>
        <class>org.a2aproject.sdk.extras.taskstore.database.jpa.JpaTaskOwner</class>
        <class>org.a2aproject.sdk.extras.pushnotificationconfigstore.database.jpa.JpaPushNotificationConfig</class>
        <exclude-unlisted-classes>true</exclude-unlisted-classes>

        <properties>
            <property name="jakarta.persistence.schema-generation.database.action" value="none"/>
            <property name="hibernate.dialect" value="org.hibernate.dialect.PostgreSQLDialect"/>
        </properties>
    </persistence-unit>
</persistence>
```

For Quarkus, include both store packages in the named persistence unit:

```properties
quarkus.hibernate-orm."a2a-java".packages=org.a2aproject.sdk.extras.taskstore.database.jpa,org.a2aproject.sdk.extras.pushnotificationconfigstore.database.jpa
```

This discovers `JpaTask`, `JpaTaskOwner` and `JpaPushNotificationConfig`. Schema generation remains an application choice; production upgrades require migrations.
