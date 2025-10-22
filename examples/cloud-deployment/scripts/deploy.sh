#!/bin/bash
set -e

echo "========================================="
echo "A2A Cloud Deployment - Deployment Script"
echo "========================================="
echo ""

# Color codes for output
RED='\033[0.31m'
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

# Check if Minikube is running
if ! minikube status > /dev/null 2>&1; then
    echo -e "${RED}Error: Minikube is not running${NC}"
    echo "Please start Minikube first with insecure registry configuration:"
    echo ""
    echo "With Docker:"
    echo "  minikube start --cpus=4 --memory=8192 --insecure-registry=192.168.49.1:5001"
    echo ""
    echo "With Podman:"
    echo "  minikube start --cpus=4 --memory=8192 --driver=podman --insecure-registry=192.168.49.1:5001"
    exit 1
fi

echo -e "${GREEN}✓ Minikube is running${NC}"

# Check if insecure registry is configured
echo "Checking insecure registry configuration..."
MINIKUBE_HOST_IP=$(minikube ssh "ip route | grep default | awk '{print \$3}'")
if ! minikube profile list -o json | grep -q "InsecureRegistry"; then
    echo -e "${RED}Error: Minikube is not configured for insecure registry${NC}"
    echo ""
    echo "Please delete and recreate Minikube with insecure registry support:"
    echo "  minikube delete"
    echo "  minikube start --cpus=4 --memory=8192 --driver=podman --insecure-registry=${MINIKUBE_HOST_IP}:5001"
    exit 1
fi
echo -e "${GREEN}✓ Insecure registry configured (${MINIKUBE_HOST_IP}:5001)${NC}"

# Set up local registry container on host
echo ""
echo "Setting up local registry container..."

# Check if registry container is already running
if $CONTAINER_TOOL ps --filter "name=kind-registry" --format '{{.Names}}' | grep -q kind-registry; then
    echo "Registry container already running"
else
    # Remove old container if it exists but is stopped
    $CONTAINER_TOOL rm -f kind-registry > /dev/null 2>&1 || true

    # Start registry container on host
    echo "Starting registry container on host (port 5001)..."
    $CONTAINER_TOOL run -d --restart=always -p "0.0.0.0:5001:5000" --name "kind-registry" registry:2
fi

# Verify registry is accessible
echo "Verifying registry is accessible at localhost:5001..."
for i in {1..10}; do
    if curl -s http://localhost:5001/v2/ > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Registry accessible at localhost:5001${NC}"
        break
    fi
    if [ $i -eq 10 ]; then
        echo -e "${RED}ERROR: Registry not accessible after 10 attempts${NC}"
        exit 1
    fi
    echo "Attempt $i/10..."
    sleep 1
done

# Registry will be accessed from Minikube using host.minikube.internal
REGISTRY="localhost:5001"
echo "Using registry: $REGISTRY (host), host.minikube.internal:5001 (from Minikube)"

# Build the project
echo ""
echo "Building the project..."
cd ../server
mvn clean package -DskipTests
echo -e "${GREEN}✓ Project built successfully${NC}"

# Build and push container image to Minikube registry
echo ""
echo "Building container image..."
$CONTAINER_TOOL build -t ${REGISTRY}/a2a-cloud-deployment:latest .
echo -e "${GREEN}✓ Container image built${NC}"

echo "Pushing image to Minikube registry..."
# Retry push a few times as it can be flaky with Podman
MAX_RETRIES=3
for attempt in $(seq 1 $MAX_RETRIES); do
    echo "Push attempt $attempt/$MAX_RETRIES..."
    if $CONTAINER_TOOL push ${REGISTRY}/a2a-cloud-deployment:latest --tls-verify=false --retry=2 2>&1 | tee /tmp/push.log; then
        echo -e "${GREEN}✓ Image pushed to registry${NC}"
        break
    else
        if [ $attempt -eq $MAX_RETRIES ]; then
            echo -e "${RED}ERROR: Failed to push image after $MAX_RETRIES attempts${NC}"
            cat /tmp/push.log
            exit 1
        fi
        echo -e "${YELLOW}Push failed, retrying in 2 seconds...${NC}"
        sleep 2
    fi
done

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
kubectl wait --for=condition=Ready pod -l app=postgres -n a2a-demo --timeout=120s
echo -e "${GREEN}✓ PostgreSQL deployed${NC}"

# Deploy Kafka
echo ""
echo "Deploying Kafka..."
kubectl apply -f ../k8s/02-kafka.yaml
echo "Waiting for Kafka to be ready (using KRaft mode, typically 2-3 minutes)..."

# Monitor progress while waiting
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

# Create Kafka Topic for event replication
echo ""
echo "Creating Kafka topic for event replication..."
kubectl apply -f ../k8s/02a-kafka-topic.yaml
echo "Waiting for Kafka topic to be ready..."
kubectl wait --for=condition=Ready kafkatopic/a2a-replicated-events -n kafka --timeout=60s
echo -e "${GREEN}✓ Kafka topic created${NC}"

# Deploy Agent ConfigMap
echo ""
echo "Deploying Agent ConfigMap..."
kubectl apply -f ../k8s/03-agent-configmap.yaml
echo -e "${GREEN}✓ ConfigMap deployed${NC}"

# Deploy Agent
if [ "${SKIP_AGENT_DEPLOY}" != "true" ]; then
    echo ""
    echo "Deploying A2A Agent..."
    kubectl apply -f ../k8s/04-agent-deployment.yaml

    echo "Waiting for Agent pods to be ready..."
    kubectl wait --for=condition=Ready pod -l app=a2a-agent -n a2a-demo --timeout=120s
    echo -e "${GREEN}✓ Agent deployed${NC}"
else
    echo ""
    echo -e "${YELLOW}⚠ Skipping agent deployment (SKIP_AGENT_DEPLOY=true)${NC}"
    echo "  ConfigMap has been deployed, you can manually deploy the agent with:"
    echo "    kubectl apply -f ../k8s/04-agent-deployment.yaml"
fi

echo ""
echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}Deployment completed successfully!${NC}"
echo -e "${GREEN}=========================================${NC}"
echo ""
echo "To verify the deployment, run:"
echo "  ./verify.sh"
echo ""
echo "To access the agent from outside the cluster:"
echo "  kubectl port-forward -n a2a-demo svc/a2a-agent-service 8080:8080"
echo ""
echo "Then access the agent at:"
echo "  http://localhost:8080/a2a/agent-card"
