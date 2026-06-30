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
quarkus.hibernate-orm.database.generation=update
```

#### For WildFly/Jakarta EE Servers

Create or update your `persistence.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<persistence xmlns="https://jakarta.ee/xml/ns/persistence" version="3.0">
    <persistence-unit name="a2a-java" transaction-type="JTA">
        <jta-data-source>java:jboss/datasources/A2ADataSource</jta-data-source>

        <class>org.a2aproject.sdk.extras.taskstore.database.jpa.JpaTask</class>
        <exclude-unlisted-classes>true</exclude-unlisted-classes>

        <properties>
            <property name="jakarta.persistence.schema-generation.database.action" value="create"/>
            <property name="hibernate.dialect" value="org.hibernate.dialect.PostgreSQLDialect"/>
        </properties>
    </persistence-unit>
</persistence>
```

### 3. Database Schema

The module automatically creates the required table:

```sql
CREATE TABLE a2a_tasks (
    task_id VARCHAR(255) PRIMARY KEY,
    task_data TEXT NOT NULL
);
```

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
            <property name="jakarta.persistence.schema-generation.database.action" value="create"/>
            <property name="hibernate.dialect" value="org.hibernate.dialect.PostgreSQLDialect"/>
        </properties>
    </persistence-unit>
</persistence>
```

### 3. Database Schema

The module automatically creates the required table with a composite primary key:

```sql
CREATE TABLE a2a_push_notification_configs (
    task_id VARCHAR(255) NOT NULL,
    config_id VARCHAR(255) NOT NULL,
    task_data TEXT NOT NULL,
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
        <class>org.a2aproject.sdk.extras.pushnotificationconfigstore.database.jpa.JpaPushNotificationConfig</class>
        <exclude-unlisted-classes>true</exclude-unlisted-classes>

        <properties>
            <property name="jakarta.persistence.schema-generation.database.action" value="create"/>
            <property name="hibernate.dialect" value="org.hibernate.dialect.PostgreSQLDialect"/>
        </properties>
    </persistence-unit>
</persistence>
```

For Quarkus, no additional configuration is needed — both entities are discovered automatically.
