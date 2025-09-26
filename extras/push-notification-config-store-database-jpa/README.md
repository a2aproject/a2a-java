# A2A Java SDK - JPA Database PushNotificationConfigStore

This module provides a JPA-based implementation of the `PushNotificationConfigStore` interface that persists push notification configurations to a relational database instead of keeping them in memory.

The persistence is done with the Jakarta Persistence API, so this should be suitable for any JPA 3.0+ provider and Jakarta EE application server.

## Quick Start

### 1. Add Dependency

Add this module to your project's `pom.xml`:

```xml
<dependency>
    <groupId>io.github.a2asdk</groupId>
    <artifactId>a2a-java-extras-push-notification-config-store-database-jpa</artifactId>
    <version>${a2a.version}</version>
</dependency>
```

The `JpaDatabasePushNotificationConfigStore` is annotated in such a way that it should take precedence over the default `InMemoryPushNotificationConfigStore`. Hence, it is a drop-in replacement.

### 2. Configure Database

The following examples assume you are using PostgreSQL as your database. To use another database, adjust as needed for your environment.

#### For Quarkus Reference Servers

Add to your `application.properties`:

```properties
# Database configuration
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/a2a_db
quarkus.datasource.username=your_username
quarkus.datasource.password=your_password

# Hibernate configuration
quarkus.hibernate-orm.database.generation=update
```

#### For WildFly/Jakarta EE Servers

Create or update your `persistence.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<persistence xmlns="https://jakarta.ee/xml/ns/persistence" version="3.0">
    <persistence-unit name="a2a-java" transaction-type="JTA">
        <jta-data-source>java:jboss/datasources/A2ADataSource</jta-data-source>
        
        <class>io.a2a.extras.pushnotificationconfigstore.database.jpa.JpaPushNotificationConfig</class>
        <exclude-unlisted-classes>true</exclude-unlisted-classes>
        
        <properties>
            <!-- Change as required for your environment -->
            <property name="jakarta.persistence.schema-generation.database.action" value="create"/>
            <property name="hibernate.dialect" value="org.hibernate.dialect.PostgreSQLDialect"/>
        </properties>
    </persistence-unit>
</persistence>
```

### 3. Database Schema

The module will automatically create the required table, which uses a composite primary key:

```sql
CREATE TABLE a2a_push_notification_configs (
    task_id VARCHAR(255) NOT NULL,
    config_id VARCHAR(255) NOT NULL,
    task_data TEXT NOT NULL,
    PRIMARY KEY (task_id, config_id)
);
```

## Configuration Options

### Persistence Unit Name

The module uses the persistence unit name `"a2a-java"`. Ensure your `persistence.xml` defines a persistence unit with this name.
