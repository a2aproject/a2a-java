#!/bin/bash
set -e

echo "========================================="
echo "A2A Cloud Deployment - Deployment Script"
echo "========================================="
echo ""

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Parse command line arguments
CONTAINER_TOOL="docker"
while [[ $# -gt 0 ]]; do
    case $1 in
        --container-tool)
            CONTAINER_TOOL="$2"
            shift 2
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            echo "Usage: $0 [--container-tool docker|podman]"
            exit 1
            ;;
    esac
done

echo "Container tool: $CONTAINER_TOOL"
echo ""

# Configure Kind to use podman if specified
if [ "$CONTAINER_TOOL" = "podman" ]; then
    export KIND_EXPERIMENTAL_PROVIDER=podman
    echo "Configured Kind to use podman provider"
    echo ""
fi

# Check if Kind is installed
if ! command -v kind &> /dev/null; then
    echo -e "${RED}Error: Kind is not installed${NC}"
    echo "Please install Kind first: https://kind.sigs.k8s.io/docs/user/quick-start/#installation"
    exit 1
fi

# Check if kubectl is installed
if ! command -v kubectl &> /dev/null; then
    echo -e "${RED}Error: kubectl is not installed${NC}"
    echo "Please install kubectl first: https://kubernetes.io/docs/tasks/tools/"
    exit 1
fi

# Setup local registry
echo "Setting up local registry..."
REG_NAME='kind-registry'
REG_PORT='5001'

# Create registry container if it doesn't exist
if [ "$($CONTAINER_TOOL inspect -f '{{.State.Running}}' "${REG_NAME}" 2>/dev/null || true)" != 'true' ]; then
    echo "Creating registry container..."
    $CONTAINER_TOOL run \
        -d --restart=always -p "127.0.0.1:${REG_PORT}:5000" --network bridge --name "${REG_NAME}" \
        mirror.gcr.io/library/registry:2
    echo -e "${GREEN}✓ Registry container created${NC}"
else
    echo -e "${GREEN}✓ Registry container already running${NC}"
fi

# Create Kind cluster if it doesn't exist
echo ""
if ! kind get clusters 2>/dev/null | grep -q '^kind$'; then
    echo "Creating Kind cluster..."
    kind create cluster --config=../kind-config.yaml
    echo -e "${GREEN}✓ Kind cluster created${NC}"
else
    # Check if cluster is healthy by trying to get nodes
    if ! kubectl get nodes &>/dev/null; then
        echo -e "${RED}Error: Existing Kind cluster is not healthy${NC}"
        echo ""
        echo "The cluster exists but is not responding. This usually means:"
        echo "  - The cluster containers are stopped"
        echo "  - The cluster is in a corrupted state"
        echo ""
        echo "To fix this, delete the cluster and re-run this script:"
        echo "  kind delete cluster"
        echo "  ./deploy.sh"
        echo ""
        exit 1
    else
        echo -e "${GREEN}✓ Kind cluster already exists and is healthy${NC}"
    fi
fi

# Configure registry on cluster nodes
echo ""
echo "Configuring registry on cluster nodes..."
REGISTRY_DIR="/etc/containerd/certs.d/localhost:${REG_PORT}"
for node in $(kind get nodes); do
    $CONTAINER_TOOL exec "${node}" mkdir -p "${REGISTRY_DIR}"
    cat <<EOF | $CONTAINER_TOOL exec -i "${node}" cp /dev/stdin "${REGISTRY_DIR}/hosts.toml"
[host."http://${REG_NAME}:5000"]
EOF
done
echo -e "${GREEN}✓ Registry configured on nodes${NC}"

# Connect registry to cluster network
echo ""
echo "Connecting registry to cluster network..."
if [ "$($CONTAINER_TOOL inspect -f='{{json .NetworkSettings.Networks.kind}}' "${REG_NAME}")" = 'null' ]; then
    $CONTAINER_TOOL network connect "kind" "${REG_NAME}"
    echo -e "${GREEN}✓ Registry connected to cluster network${NC}"
else
    echo -e "${GREEN}✓ Registry already connected${NC}"
fi

# Create ConfigMap to document local registry
echo ""
echo "Creating registry ConfigMap..."
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: ConfigMap
metadata:
  name: local-registry-hosting
  namespace: kube-public
data:
  localRegistryHosting.v1: |
    host: "localhost:${REG_PORT}"
    help: "https://kind.sigs.k8s.io/docs/user/local-registry/"
EOF
echo -e "${GREEN}✓ Registry ConfigMap created${NC}"

# Verify registry is accessible
echo ""
echo "Verifying registry is accessible..."
if curl -s http://localhost:${REG_PORT}/v2/ > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Registry accessible at localhost:${REG_PORT}${NC}"
else
    echo -e "${RED}ERROR: Registry not accessible${NC}"
    exit 1
fi

# Build the project
echo ""
echo "Building the project..."
cd ../server
mvn clean package -DskipTests
echo -e "${GREEN}✓ Project built successfully${NC}"

# Build and push container image to local registry
REGISTRY="localhost:${REG_PORT}"
echo ""
echo "Building container image..."
$CONTAINER_TOOL build -t ${REGISTRY}/a2a-cloud-deployment:latest .
echo -e "${GREEN}✓ Container image built${NC}"

echo "Pushing image to local registry..."
if [ "$CONTAINER_TOOL" = "podman" ]; then
    $CONTAINER_TOOL push --tls-verify=false ${REGISTRY}/a2a-cloud-deployment:latest
else
    $CONTAINER_TOOL push ${REGISTRY}/a2a-cloud-deployment:latest
fi
echo -e "${GREEN}✓ Image pushed to registry${NC}"

# Go back to scripts directory
cd ../scripts

# Install Strimzi operator if not already installed
echo ""
echo "Checking for Strimzi operator..."

# Ensure kafka namespace exists
if ! kubectl get namespace kafka > /dev/null 2>&1; then
    echo "Creating kafka namespace..."
    kubectl create namespace kafka
fi

if ! kubectl get crd kafkas.kafka.strimzi.io > /dev/null 2>&1; then
    echo "Installing Strimzi operator..."
    kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka

    echo "Waiting for Strimzi operator deployment to be created..."
    for i in {1..30}; do
        if kubectl get deployment strimzi-cluster-operator -n kafka > /dev/null 2>&1; then
            echo "Deployment found"
            break
        fi
        if [ $i -eq 30 ]; then
            echo -e "${RED}ERROR: Deployment not found after 30 seconds${NC}"
            exit 1
        fi
        sleep 1
    done

    echo "Waiting for Strimzi operator to be ready..."
    kubectl wait --for=condition=Available deployment/strimzi-cluster-operator -n kafka --timeout=300s
    kubectl wait --for=condition=Ready pod -l name=strimzi-cluster-operator -n kafka --timeout=300s
    echo -e "${GREEN}✓ Strimzi operator installed${NC}"
else
    echo -e "${GREEN}✓ Strimzi operator already installed${NC}"
fi

# Create namespace
echo ""
echo "Creating namespace..."
kubectl apply -f ../k8s/00-namespace.yaml
echo -e "${GREEN}✓ Namespace created${NC}"

# Deploy PostgreSQL
echo ""
echo "Deploying PostgreSQL..."
kubectl apply -f ../k8s/01-postgres.yaml
echo "Waiting for PostgreSQL to be ready..."

# Wait for pod to be created (StatefulSet takes time to create pod)
for i in {1..30}; do
    if kubectl get pod -l app=postgres -n a2a-demo 2>/dev/null | grep -q postgres; then
        echo "PostgreSQL pod found, waiting for ready state..."
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "${RED}ERROR: PostgreSQL pod not created after 30 seconds${NC}"
        kubectl get statefulset -n a2a-demo
        exit 1
    fi
    sleep 1
done

# Now wait for pod to be ready
kubectl wait --for=condition=Ready pod -l app=postgres -n a2a-demo --timeout=120s
echo -e "${GREEN}✓ PostgreSQL deployed${NC}"

# Deploy Kafka
echo ""
echo "Deploying Kafka..."
kubectl apply -f ../k8s/02-kafka.yaml
echo "Waiting for Kafka to be ready (using KRaft mode, typically 2-3 minutes. Timeout is 10 minutes)..."

# Check if we should skip entity operator wait (workaround for some environments)
if [ "${SKIP_ENTITY_OPERATOR_WAIT}" = "true" ]; then
    echo -e "${YELLOW}⚠ SKIP_ENTITY_OPERATOR_WAIT is set - checking broker pod only${NC}"

    # Wait for broker pod to be ready (skip entity operator check)
    for i in {1..60}; do
        echo "Checking Kafka broker status (attempt $i/60)..."
        kubectl get pods -n kafka -l strimzi.io/cluster=a2a-kafka 2>/dev/null || true

        if kubectl wait --for=condition=Ready pod/a2a-kafka-broker-0 -n kafka --timeout=5s 2>/dev/null; then
            echo -e "${GREEN}✓ Kafka broker pod is ready${NC}"
            echo -e "${YELLOW}⚠ Entity operator may not be ready, but this does not affect functionality${NC}"
            break
        fi

        if [ $i -eq 60 ]; then
            echo -e "${RED}ERROR: Timeout waiting for Kafka broker${NC}"
            kubectl get pods -n kafka -l strimzi.io/cluster=a2a-kafka
            kubectl describe pod a2a-kafka-broker-0 -n kafka 2>/dev/null || true
            exit 1
        fi

        sleep 5
    done
else
    echo -e "${YELLOW} If waiting for Kafka times out, run ./cleanup.sh, and retry having set 'SKIP_ENTITY_OPERATOR_WAIT=true'${NC}"
    # Standard wait for full Kafka resource (includes entity operator)
    for i in {1..60}; do
        echo "Checking Kafka status (attempt $i/60)..."
        kubectl get kafka -n kafka -o wide 2>/dev/null || true
        kubectl get pods -n kafka -l strimzi.io/cluster=a2a-kafka 2>/dev/null || true

        if kubectl wait --for=condition=Ready kafka/a2a-kafka -n kafka --timeout=10s 2>/dev/null; then
            echo -e "${GREEN}✓ Kafka deployed${NC}"
            break
        fi

        if [ $i -eq 60 ]; then
            echo -e "${RED}ERROR: Timeout waiting for Kafka${NC}"
            kubectl describe kafka/a2a-kafka -n kafka
            kubectl get events -n kafka --sort-by='.lastTimestamp'
            exit 1
        fi
    done
fi

# Create Kafka Topic for event replication
echo ""
echo "Creating Kafka topic for event replication..."
kubectl apply -f ../k8s/03-kafka-topic.yaml

if [ "${SKIP_ENTITY_OPERATOR_WAIT}" = "true" ]; then
    echo -e "${YELLOW}⚠ SKIP_ENTITY_OPERATOR_WAIT is set - polling Kafka broker for topic${NC}"
    echo "  Topic operator may not be ready, waiting for broker to create topic. This check can take several minutes..."

    # Wait for topic to actually exist in Kafka broker (not just CRD)
    for i in {1..30}; do
        if kubectl exec a2a-kafka-broker-0 -n kafka -- \
            /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092 2>/dev/null | \
            grep -q "a2a-replicated-events"; then
            echo -e "${GREEN}✓ Topic exists in Kafka broker${NC}"
            break
        fi
        if [ $i -eq 30 ]; then
            echo -e "${RED}ERROR: Topic not found in broker after 30 attempts${NC}"
            exit 1
        fi
        sleep 2
    done
else
    echo "Waiting for Kafka topic to be ready..."
    if kubectl wait --for=condition=Ready kafkatopic/a2a-replicated-events -n kafka --timeout=60s; then
        echo -e "${GREEN}✓ Kafka topic created${NC}"
    else
        echo -e "${RED}ERROR: Timeout waiting for Kafka topic${NC}"
        echo -e "${YELLOW}The topic operator may not be ready in this environment.${NC}"
        echo -e "${YELLOW}Run ./cleanup.sh, then retry with: export SKIP_ENTITY_OPERATOR_WAIT=true${NC}"
        exit 1
    fi
fi

# Deploy Agent ConfigMap
echo ""
echo "Deploying Agent ConfigMap..."
kubectl apply -f ../k8s/04-agent-configmap.yaml
echo -e "${GREEN}✓ ConfigMap deployed${NC}"

# Deploy Agent
if [ "${SKIP_AGENT_DEPLOY}" != "true" ]; then
    echo ""
    echo "Deploying A2A Agent..."
    kubectl apply -f ../k8s/05-agent-deployment.yaml

    echo "Waiting for Agent pods to be ready..."
    kubectl wait --for=condition=Ready pod -l app=a2a-agent -n a2a-demo --timeout=120s
    echo -e "${GREEN}✓ Agent deployed${NC}"
else
    echo ""
    echo -e "${YELLOW}⚠ Skipping agent deployment (SKIP_AGENT_DEPLOY=true)${NC}"
    echo "  ConfigMap has been deployed, you can manually deploy the agent with:"
    echo "    kubectl apply -f ../k8s/05-agent-deployment.yaml"
fi

echo ""
echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}Deployment completed successfully!${NC}"
echo -e "${GREEN}=========================================${NC}"
echo ""
echo "To verify the deployment, run:"
echo "  ./verify.sh"
echo ""
echo "To access the agent (via NodePort):"
echo "  curl http://localhost:8080/.well-known/agent-card.json"
echo ""
echo "To run the test client (demonstrating load balancing):"
echo "  cd ../server"
echo "  mvn test-compile exec:java -Dexec.classpathScope=test \\"
echo "    -Dexec.mainClass=\"io.a2a.examples.cloud.A2ACloudExampleClient\" \\"
echo "    -Dagent.url=\"http://localhost:8080\""
