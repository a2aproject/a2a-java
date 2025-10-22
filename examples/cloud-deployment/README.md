# A2A Cloud Deployment Example

This example demonstrates deploying an A2A agent to Kubernetes with:
- **Multiple pods** (2 replicas) for load balancing
- **PostgreSQL database** for persistent task storage
- **Kafka event replication** for cross-pod event streaming
- **JSON-RPC transport** for client-server communication

## Architecture

```
┌──────────────────────────────────────────────────────┐
│  Kubernetes Cluster (Minikube)                       │
│                                                      │
│  ┌─────────────┐    ┌─────────────┐                  │
│  │  A2A Agent  │    │  A2A Agent  │                  │
│  │   Pod 1     │    │   Pod 2     │                  │
│  └──────┬──────┘    └──────┬──────┘                  │
│         │                   │                        │
│         └─────────┬─────────┘                        │
│                   │                                  │
│         ┌─────────▼─────────┐                        │
│         │  Service (LB)      │                       │
│         │  Round-Robin       │                       │
│         └─────────┬──────────┘                       │
│                   │                                  │
│         ┌─────────▼─────────┐                        │
│         │  PostgreSQL DB     │◄─── Task Persistence  │
│         └────────────────────┘                       │
│                                                      │
│         ┌────────────────────┐                       │
│         │  Kafka (Strimzi)   │◄─── Event Replication │
│         └────────────────────┘                       │
└──────────────────────────────────────────────────────┘
           ▲
           │
    ┌──────┴──────┐
    │   Client    │
    │  (External) │
    └─────────────┘
```

## What This Example Demonstrates

1. **Load Balancing**: Messages sent to the service are distributed across pods via round-robin
2. **Event Replication**: Events from one pod are replicated to other pods via Kafka
3. **Task Persistence**: Task state is stored in PostgreSQL and shared across all pods
4. **Streaming Subscriptions**: Clients can subscribe to task updates and receive events from any pod
5. **Fire-and-Forget Pattern**: Tasks remain in WORKING state until explicitly completed
6. **Command-Based Protocol**: Simple message protocol ("start", "process", "complete")

## Prerequisites

- **Minikube** (v1.30+)
- **kubectl** (v1.27+)
- **Maven** (3.8+)
- **Java** 17+
- **Container runtime**: Docker, Podman, or other OCI-compatible tool

## Quick Start

### 1. Start Minikube

This example uses a local container registry on the host machine. Minikube must be configured to allow pulling from this insecure (HTTP) registry.

**With Docker:**
```bash
minikube start --cpus=4 --memory=8192 --insecure-registry=192.168.49.1:5001
```

**With Podman:**
```bash
minikube start --cpus=4 --memory=8192 --driver=podman --insecure-registry=192.168.49.1:5001
```

**Note**: The IP address `192.168.49.1` is the default host IP from Minikube's perspective. If your Minikube uses a different network range, adjust accordingly.

### 2. Deploy the Stack

```bash
cd scripts
./deploy.sh
```

**If using a container tool other than `docker` (e.g., Podman without alias):**
```bash
./deploy.sh --container-tool podman
```

This script will:
- Install Strimzi Kafka operator
- Deploy PostgreSQL
- Deploy Kafka cluster
- Build and deploy the A2A agent (2 pods)

### 3. Verify Deployment

```bash
./verify.sh
```

Expected output:
```
✓ Namespace 'a2a-demo' exists
✓ PostgreSQL is running
✓ Kafka is ready
✓ Agent pods are running (2/2 ready)
✓ Agent service exists
```

### 4. Test Multi-Pod Behavior

#### Start Minikube Tunnel

The LoadBalancer service requires `minikube tunnel` to expose an external IP address. This demonstrates true load balancing across pods, as each new HTTP connection is round-robin distributed by the Kubernetes service.

In one terminal, start the tunnel (requires sudo/admin privileges):
```bash
minikube tunnel
```

This will:
- Assign an external IP (typically `127.0.0.1`) to the LoadBalancer service
- Route traffic from your local machine to the Kubernetes service
- Enable proper round-robin load balancing across agent pods
- Keep running in the foreground (Ctrl+C to stop)

**Note**: Unlike `kubectl port-forward` (which maintains session affinity), `minikube tunnel` allows the LoadBalancer to distribute each new connection across different pods, clearly demonstrating multi-pod load balancing.

#### Run the Test Client

In another terminal, run the test client:
```bash
cd ../server
mvn test-compile exec:java -Dexec.mainClass="io.a2a.examples.cloud.A2ACloudExampleClient" -Dexec.classpathScope=test
```

The client will connect to `http://127.0.0.1:8080` (the tunnel-exposed external IP).

Expected output:
```
=============================================
A2A Cloud Deployment Example Client
=============================================

Agent URL: http://127.0.0.1:8080
Process messages: 8
Message interval: 1500ms

Fetching agent card...
✓ Agent: Cloud Deployment Demo Agent
✓ Description: Demonstrates A2A multi-pod deployment with Kafka event replication, PostgreSQL persistence, and round-robin load balancing across Kubernetes pods

Client task ID: cloud-test-1234567890

Step 1: Sending 'start' to create task...
✓ Task created: <server-task-id>
  State: WORKING

Step 2: Subscribing to task for streaming updates...
✓ Subscribed to task updates
  Artifact #1: Started by a2a-agent-7b8f9c-abc12
    → Pod: a2a-agent-7b8f9c-abc12 (Total unique pods: 1)

Step 3: Sending 8 'process' messages (interval: 1500ms)...
--------------------------------------------
✓ Process message 1 sent
  Artifact #2: Processed by a2a-agent-7b8f9c-xyz34
    → Pod: a2a-agent-7b8f9c-xyz34 (Total unique pods: 2)
✓ Process message 2 sent
  Artifact #3: Processed by a2a-agent-7b8f9c-abc12
    → Pod: a2a-agent-7b8f9c-abc12 (Total unique pods: 2)
✓ Process message 3 sent
  Artifact #4: Processed by a2a-agent-7b8f9c-xyz34
    → Pod: a2a-agent-7b8f9c-xyz34 (Total unique pods: 2)
...

Waiting for process artifacts to arrive...

Step 4: Sending 'complete' to finalize task...
✓ Complete message sent, task state: COMPLETED
  Artifact #10: Completed by a2a-agent-7b8f9c-abc12

Waiting for task to complete...
  Task reached final state: COMPLETED

=============================================
Test Results
=============================================
Total artifacts received: 10
Unique pods observed: 2
Pod names: [a2a-agent-7b8f9c-abc12, a2a-agent-7b8f9c-xyz34]

✓ TEST PASSED - Successfully demonstrated multi-pod processing!
  Messages were handled by 2 different pods.
  This proves that:
    - Load balancing is working (round-robin across pods)
    - Event replication is working (subscriber sees events from all pods)
    - Database persistence is working (task state shared across pods)
```

## How It Works

### Agent Implementation

The agent (`CloudAgentExecutorProducer`) implements a command-based protocol:

```java
@Override
public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
    TaskUpdater updater = new TaskUpdater(context, eventQueue);
    String messageText = extractTextFromMessage(context.getMessage()).trim().toLowerCase();

    // Get pod name from Kubernetes downward API
    String podName = System.getenv("POD_NAME");

    if ("complete".equals(messageText)) {
        // Completion trigger - add final artifact and complete
        String artifactText = "Completed by " + podName;
        List<Part<?>> parts = List.of(new TextPart(artifactText, null));
        updater.addArtifact(parts);
        updater.complete();  // Transition to COMPLETED state
    } else if (context.getTask() == null) {
        // Initial "start" message - create task in SUBMITTED → WORKING state
        updater.submit();
        updater.startWork();
        String artifactText = "Started by " + podName;
        List<Part<?>> parts = List.of(new TextPart(artifactText, null));
        updater.addArtifact(parts);
    } else {
        // Subsequent "process" messages - add artifacts (fire-and-forget, stays WORKING)
        String artifactText = "Processed by " + podName;
        List<Part<?>> parts = List.of(new TextPart(artifactText, null));
        updater.addArtifact(parts);
    }
}
```

**Message Protocol**:
- `"start"`: Initialize task (SUBMITTED → WORKING), adds "Started by {pod-name}"
- `"process"`: Add artifact "Processed by {pod-name}" (fire-and-forget, stays WORKING)
- `"complete"`: Add artifact "Completed by {pod-name}" and transition to COMPLETED

### Cloud-Native Components

1. **Database Persistence** (`JpaDatabaseTaskStore`):
   - Tasks are stored in PostgreSQL
   - All pods read/write to the same database
   - Ensures task state is consistent across pods

2. **Event Replication** (`ReplicatedQueueManager` + `ReactiveMessagingReplicationStrategy`):
   - Events are published to Kafka topic `a2a-replicated-events`
   - All pods subscribe to the same topic
   - Events from pod A are replicated to pod B's queue

3. **Configuration** (`application.properties`):
   - Database URL, credentials
   - Kafka bootstrap servers
   - Reactive Messaging channel configuration

### Load Balancing Flow

```
Client sends "start"/"process"/"complete" message
    ↓
Kubernetes Service (round-robin)
    ↓
Pod A or Pod B (alternates)
    ↓
AgentExecutor processes command
    ↓
Enqueues artifact with pod name ("Started by"/"Processed by"/"Completed by")
    ↓
Event published to Kafka topic (a2a-replicated-events)
    ↓
All pods receive replicated event
    ↓
Streaming subscriber receives artifact (regardless of which pod sent it)
```

## Configuration

### Environment Variables

The following environment variables are configured via ConfigMap (`k8s/03-agent-configmap.yaml`):

| Variable | Description | Example |
|----------|-------------|---------|
| `POD_NAME` | Pod name (from downward API) | `a2a-agent-7b8f9c-abc12` |
| `DATABASE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://postgres.a2a-demo.svc.cluster.local:5432/a2a` |
| `DATABASE_USER` | Database username | `a2a` |
| `DATABASE_PASSWORD` | Database password | `a2a` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka brokers | `a2a-kafka-kafka-bootstrap.kafka.svc.cluster.local:9092` |
| `AGENT_URL` | Public agent URL | `http://a2a-agent-service.a2a-demo.svc.cluster.local:8080` |

### Scaling

To change the number of agent pods, edit `k8s/04-agent-deployment.yaml`:

```yaml
spec:
  replicas: 2  # Change to desired number
```

Then apply:
```bash
kubectl apply -f k8s/04-agent-deployment.yaml
```

## Troubleshooting

### Pods Not Starting

**Check pod status:**
```bash
kubectl get pods -n a2a-demo
kubectl describe pod <pod-name> -n a2a-demo
kubectl logs <pod-name> -n a2a-demo
```

**Common issues:**
- **ImagePullBackOff**: Image not built in Minikube's container environment
  - Solution with Docker: Run `eval $(minikube docker-env)` before building
  - Solution with Podman: Run `eval $(minikube podman-env)` before building
- **CrashLoopBackOff**: Application startup failure
  - Check logs: `kubectl logs <pod-name> -n a2a-demo`
  - Common causes: Database not ready, Kafka not ready

### Container Build Issues

**Image not found in Minikube:**

The container image must be built inside Minikube's environment.

**With Docker:**
```bash
eval $(minikube docker-env)
docker build -t a2a-cloud-deployment:latest .
```

**With Podman:**
```bash
eval $(minikube podman-env)
podman build -t a2a-cloud-deployment:latest .
```

**Verify image exists:**
```bash
# With Docker
minikube ssh docker images | grep a2a-cloud-deployment

# With Podman
minikube ssh podman images | grep a2a-cloud-deployment
```

### Database Connection Failures

**Check PostgreSQL status:**
```bash
kubectl get pods -n a2a-demo -l app=postgres
kubectl logs <postgres-pod-name> -n a2a-demo
```

**Test connection from agent pod:**
```bash
kubectl exec -it <agent-pod-name> -n a2a-demo -- bash
# Inside pod:
curl telnet://postgres.a2a-demo.svc.cluster.local:5432
```

**Common issues:**
- PostgreSQL pod not ready: Wait for it to become Ready
- Wrong credentials: Check ConfigMap values match PostgreSQL config

### Kafka Connection Failures

**Check Kafka status:**
```bash
kubectl get kafka -n a2a-demo
kubectl get pods -n a2a-demo -l strimzi.io/cluster=a2a-kafka
```

**Common issues:**
- Kafka not ready: Kafka takes 2-5 minutes to start fully
  - Wait for `kubectl wait --for=condition=Ready kafka/a2a-kafka -n a2a-demo`
- Topic not created: Kafka auto-creates topics on first publish

### Test Client Failures

**Minikube tunnel not working:**
```bash
# Check if tunnel is running
ps aux | grep "minikube tunnel"

# Check service has external IP
kubectl get svc a2a-agent-service -n a2a-demo

# Expected output should show EXTERNAL-IP as 127.0.0.1:
# NAME                 TYPE           CLUSTER-IP      EXTERNAL-IP   PORT(S)          AGE
# a2a-agent-service    LoadBalancer   10.96.123.45    127.0.0.1     8080:30123/TCP   5m

# If EXTERNAL-IP shows <pending>, tunnel is not running
# Start tunnel in a separate terminal:
minikube tunnel
```

**Connection refused errors:**
```bash
# Verify agent card is accessible via tunnel
curl http://127.0.0.1:8080/.well-known/agent-card.json

# If this fails, check:
# 1. Tunnel is running (should show "Tunnel successfully started")
# 2. Agent pods are ready (kubectl get pods -n a2a-demo)
# 3. Service exists (kubectl get svc -n a2a-demo)
```

**Only seeing 1 pod:**
- Check both pods are Running: `kubectl get pods -n a2a-demo -l app=a2a-agent`
- The test client creates fresh HTTP connections for each message to force load balancing
- If still seeing 1 pod, check service sessionAffinity is set to `None` (see `k8s/04-agent-deployment.yaml`)
- Try increasing PROCESS_MESSAGE_COUNT in test client for more samples

### Strimzi Installation Issues

**Strimzi operator not ready:**
```bash
kubectl get pods -n kafka
kubectl logs -n kafka <strimzi-operator-pod>
```

**CRD not found:**
```bash
# Check if Kafka CRD is installed
kubectl get crd kafkas.kafka.strimzi.io

# If missing, reinstall Strimzi
kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
```

### Minikube Resource Issues

**Insufficient resources:**
```bash
# Check Minikube resources
minikube status

# Increase resources (requires restart)
minikube delete
minikube start --cpus=6 --memory=12288
```

**Disk space:**
```bash
# Check disk usage
minikube ssh
df -h

# Clean up old images (adjust command for your container runtime)
minikube ssh
docker system prune -a   # or: podman system prune -a
```

## Cleanup

To remove all deployed resources:

```bash
cd scripts
./cleanup.sh
```

This will delete:
- A2A agent deployment and service
- Kafka cluster
- PostgreSQL
- Namespace `a2a-demo`

To also remove Strimzi operator:
```bash
kubectl delete namespace kafka
```

To stop Minikube:
```bash
minikube stop
```

## Project Structure

```
cloud-deployment/
├── server/
│   ├── src/main/java/io/a2a/examples/cloud/
│   │   ├── CloudAgentCardProducer.java       # Agent card configuration
│   │   └── CloudAgentExecutorProducer.java   # Agent business logic
│   ├── src/main/resources/
│   │   └── application.properties            # Application configuration
│   ├── src/test/java/io/a2a/examples/cloud/
│   │   └── A2ACloudExampleClient.java        # Test client
│   ├── pom.xml                               # Maven dependencies
│   └── Dockerfile                            # Container image
├── k8s/
│   ├── 00-namespace.yaml                     # Kubernetes namespace
│   ├── 01-postgres.yaml                      # PostgreSQL deployment
│   ├── 02-kafka.yaml                         # Strimzi Kafka cluster
│   ├── 03-agent-configmap.yaml               # Configuration
│   └── 04-agent-deployment.yaml              # Agent deployment + service
├── scripts/
│   ├── deploy.sh                             # Automated deployment
│   ├── verify.sh                             # Health checks
│   └── cleanup.sh                            # Resource cleanup
└── README.md                                 # This file
```

## Key Dependencies

From `pom.xml`:

```xml
<!-- A2A SDK with JSON-RPC transport -->
<dependency>
    <groupId>io.github.a2asdk</groupId>
    <artifactId>a2a-java-sdk-reference-jsonrpc</artifactId>
</dependency>

<!-- Database task storage -->
<dependency>
    <groupId>io.github.a2asdk</groupId>
    <artifactId>a2a-java-extras-task-store-database-jpa</artifactId>
</dependency>

<!-- Replicated event queue manager -->
<dependency>
    <groupId>io.github.a2asdk</groupId>
    <artifactId>a2a-java-queue-manager-replicated-core</artifactId>
</dependency>
<dependency>
    <groupId>io.github.a2asdk</groupId>
    <artifactId>a2a-java-queue-manager-replication-mp-reactive</artifactId>
</dependency>

<!-- Kafka connector -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-messaging-kafka</artifactId>
</dependency>
```

## Next Steps

- **Production deployment**: Replace Minikube with a real Kubernetes cluster (EKS, GKE, AKS)
- **Secrets management**: Use Kubernetes Secrets for credentials
- **Monitoring**: Add Prometheus metrics and Grafana dashboards
- **Autoscaling**: Configure Horizontal Pod Autoscaler based on CPU/memory
- **Persistent storage**: Use PersistentVolumes for PostgreSQL in production
- **TLS**: Enable TLS for Kafka and PostgreSQL connections
- **Resource limits**: Fine-tune CPU/memory requests and limits

## References

- [A2A Protocol Specification](https://github.com/a2aproject/a2a)
- [Strimzi Kafka Operator](https://strimzi.io/)
- [Quarkus Reactive Messaging](https://quarkus.io/guides/kafka)
- [Kubernetes Downward API](https://kubernetes.io/docs/concepts/workloads/pods/downward-api/)
- [Minikube Drivers](https://minikube.sigs.k8s.io/docs/drivers/)
