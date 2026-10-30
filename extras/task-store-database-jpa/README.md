# A2A Java SDK - JPA Database TaskStore

This module provides a JPA-based implementation of the `TaskStore` interface that persists tasks to a relational database instead of keeping them in memory.

The persistence is done with the Jakarta Persistence API, so this should be suitable for any JPA 3.0+ provider and Jakarta EE application server.


> **NOTE:**
>
> The `Task` instances stored in the JPA Database `TaskStore` are simply serialized to JSON before storing in the database, and serialized from JSON when loading from the database. This is done according to the current A2A specification version's format of `Task`.
>
> Future versions of the specification might use a different format. This is not intended to be a long-term store; its usage is meant to be for the lifetime of the `Task` in order to have access to the `Task` when running in a load-balanced environment.
>
> If you wish to keep the `Task` instances stored between protocol versions, you might have to implement some migration of the stored data.


## Quick Start

### 1. Add Dependency

Add this module to your project's `pom.xml`:

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-extras-task-store-database-jpa</artifactId>
    <version>${a2a.version}</version>
</dependency>
```

The `JpaDatabaseTaskStore` is annotated in such a way that it should take precedence over the default `InMemoryTaskStore`. Hence, it is a drop-in replacement. 

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
# Named unit; schema generation is an application choice
quarkus.hibernate-orm."a2a-java".datasource=<default>
quarkus.hibernate-orm."a2a-java".packages=org.a2aproject.sdk.extras.taskstore.database.jpa
quarkus.hibernate-orm."a2a-java".database.generation=none
```

#### For WildFly/Jakarta EE Servers

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
            <!-- Change as required for your environment -->
            <property name="jakarta.persistence.schema-generation.database.action" value="none"/>
            <property name="hibernate.dialect" value="org.hibernate.dialect.PostgreSQLDialect"/>
        </properties>
    </persistence-unit>
</persistence>
```

### 3. Schema and production migrations

The application controls schema creation. Register `JpaTask` and `JpaTaskOwner` in the same `a2a-java` JTA persistence unit, including when unlisted classes are excluded. The Quarkus package setting above discovers both. Development schema generation creates the ownership entity even without selecting an authorization provider. Production upgrades require an application-managed migration before using the updated store, whose deletion path also cleans up ownership.

The task schema includes all of these columns. Use the database dialect's timestamp representation for Java `Instant`:

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

`subject_key` contains the exact UTF-8 encoding of the authenticated username. Malformed surrogates and identities exceeding **1,024 encoded bytes** are rejected before ownership access. The SDK does not normalize Unicode, change case, strip accents, trim spaces, truncate, pad, or replace identities with hashes. Case, accent, Unicode-normalization, and trailing-space distinctions all remain significant.

Verify the actual column and complete `(subject_key, task_id)` index. PostgreSQL must use `BYTEA`, not a large-object OID. MySQL must use `VARBINARY(1024)`, not padded `BINARY`, a character column with a binary collation, or a prefix index. Other databases need an equivalent variable-length binary type with exact byte comparison; encoding in Java alone cannot fix a lossy schema.

Backfill legacy tasks only from a trusted source preserving original subject keys. Audit any earlier case/accent/space-insensitive associations and validate lossless text-to-binary conversion. Changing the column cannot reconstruct identity distinctions already discarded. Migration tools must respect the byte limit without normalization or truncation. Legacy tasks without ownership remain denied.

### 4. Explicitly select authorization

Adding this module selects the task store, **not an authorization provider**. With no provider, task operations are denied by default. Deliberately providerless deployments may set `a2a.authorization.required=false`, or use the handler builder's `authorizationRequired(false)`. A configured provider remains authoritative even when that flag is false.

For Quarkus, select the supplied alternative explicitly:

```properties
quarkus.arc.selected-alternatives=org.a2aproject.sdk.extras.taskstore.database.jpa.JpaTaskAuthorizationProvider
```

For portable CDI, expose an ordinary application producer. Selecting an alternative in the application's `beans.xml` is archive-scoped and may not enable it for injection points in SDK archives.

```java
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

import org.a2aproject.sdk.extras.taskstore.database.jpa.JpaTaskAuthorizationProvider;
import org.a2aproject.sdk.extras.taskstore.database.jpa.JpaTaskOwnershipRepository;
import org.a2aproject.sdk.server.auth.TaskAuthorizationProvider;

@ApplicationScoped
public class TaskAuthorizationConfiguration {
    @Produces
    @Dependent
    TaskAuthorizationProvider taskAuthorization(JpaTaskOwnershipRepository ownership) {
        return new JpaTaskAuthorizationProvider(ownership);
    }
}
```

The injected repository invokes `JpaTaskOwnershipTransactions` through CDI/JTA interception. Configure a transaction-scoped persistence context and JTA manager. Do not construct the transaction helper directly; use the factory below outside CDI. Choose either alternative selection or the producer to avoid ambiguous providers.

For programmatic handler construction, use the application's JTA entity-manager factory and transaction manager:

```java
var ownership = JpaTaskOwnershipRepository.create(entityManagerFactory, transactionManager);
var authorization = new JpaTaskAuthorizationProvider(ownership);
var handler = DefaultRequestHandler.builder()
        .agentExecutor(agentExecutor)
        .taskStore(taskStore)
        .authorizationProvider(authorization)
        .build();
```

The factory requires a JTA factory for `a2a-java` and the manager governing its transactions. Insertion suspends the caller and uses a fresh persistence context in a new transaction. On failure, insertion rolls back before a **second new transaction** confirms committed ownership. This preserves first-writer-wins without poisoning unrelated caller work or consulting its stale repeatable-read snapshot. Failures propagate if committed ownership cannot be confirmed.

Provide an authenticated `User` in `ServerCallContext`. Its username must uniquely identify the subject across your deployment. Multiple issuers or tenant-local usernames require stable namespaced identities or a custom provider. Anonymous users and unknown ownership are denied. Creation requires authentication; recording is idempotent and never transfers an existing owner.

An ownership row may precede its task on asynchronous paths, so no immediate task foreign key is required. Access remains denied between task persistence and ownership recording. Replicas must use the same ownership database; recording or replication delays never grant access. Deleting through `JpaDatabaseTaskStore` removes ownership in the same transaction, even when the task row is absent. Direct maintenance must clean up both tables.

## Listing and capabilities

The core `TaskAuthorizationProvider.getCapability(Class<T>)` defaults to `Optional.empty()` and has no JPA dependency. The configured provider may expose `JpaTaskAuthorizationCapability.readScope(context, operation)`, returning `JpaTaskReadScope.OwnedBy(subject)`, `All`, or `None`.

For a supported capability the store resolves `LIST_TASKS` scope once and applies the same restriction to database `COUNT` and paginated `SELECT`. The exact authorized total covers every matching page; the cursor restricts only the page query. Owner scopes use correlated ownership `EXISTS` and binary parameters. `All` includes unowned tasks; `None` returns no tasks. The handler's unconditional list gate and final task checks remain enforced. Two statements do not guarantee a stronger snapshot than the configured database isolation.

Custom allow-all providers may expose `All` only when their individual checks agree. Capabilities must represent the provider's **complete operation-specific read policy**. Wrappers adding restrictions must not forward a broader capability. Unsupported capabilities use fallback; lookup or execution exceptions fail the request without changing policy.

The JPA fallback checks candidates in Java to obtain an exact count. `a2a.max-scanned-tasks` (default 10,000) bounds this scan; `a2a.authorization-scan-batch-size` (default 100) controls fetch size. Both must be positive. Exceeding the ceiling fails explicitly instead of returning a partial total. These settings apply **only to fallback**, not optimized queries, which still incur database execution cost.

Generic stores inheriting `TaskStore.listAuthorized` have separate fallback limits: 20,000 distinct candidates per phase, 300 count queries, 300 normal page/lookahead queries, and up to 100 additional queries only for opaque token recovery within the fetched final batch. Unsupported policies over larger datasets can still fail; matching backend capabilities enable database filtering and counting.

## Tests

H2 tests provide fast coverage. Real PostgreSQL 16 and MySQL 8 tests verify the actual binary column/full index, identity isolation, and duplicate-ownership recovery. PostgreSQL covers default Read Committed and explicit Repeatable Read; MySQL adds default text-collation and Repeatable Read coverage. MySQL support does not change the cloud example's PostgreSQL database.

With a working Docker/Podman engine:

```bash
mvn -pl server-common,extras/task-store-database-jpa -am test
```

`-DskipDockerTests=true` is an explicit local skip. Skipped database tests do not validate identity equality or transaction recovery.
