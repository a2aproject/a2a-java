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
    echo "Please start Minikube first:"
    echo ""
    echo "With Docker:"
    echo "  minikube start --cpus=4 --memory=8192"
    echo ""
    echo "With Podman:"
    echo "  minikube start --cpus=4 --memory=8192 --driver=podman"
    echo ""
    echo "With Podman (rootless):"
    echo "  minikube start --cpus=4 --memory=8192 --driver=podman --container-runtime=containerd"
    exit 1
fi

echo -e "${GREEN}✓ Minikube is running${NC}"

# Enable Minikube registry addon if not already enabled
echo ""
echo "Checking Minikube registry addon..."
if ! minikube addons list | grep -q "registry.*enabled"; then
    echo "Enabling Minikube registry addon..."
    minikube addons enable registry
    # Wait a bit for registry to start
    sleep 5
    echo -e "${GREEN}✓ Registry addon enabled${NC}"
else
    echo -e "${GREEN}✓ Registry addon already enabled${NC}"
fi

# Set up port forwarding to registry
# This makes the registry accessible at localhost:5000
echo ""
echo "Setting up registry port forwarding..."

# Detect OS
if echo "$OSTYPE" | grep -q "^darwin"; then
    # macOS - use socat in container for port forwarding
    echo "macOS detected, using socat for port forwarding..."

    # Stop any existing port forwarder
    $CONTAINER_TOOL stop socat-registry 2>/dev/null || true
    $CONTAINER_TOOL rm socat-registry 2>/dev/null || true

    # Pull alpine if needed
    $CONTAINER_TOOL pull alpine 2>/dev/null || true

    # Start socat container for port forwarding
    $CONTAINER_TOOL run -d --name socat-registry --rm --network=host alpine \
        ash -c "apk add socat && socat TCP-LISTEN:5000,reuseaddr,fork TCP:$(minikube ip):5000" \
        > /dev/null 2>&1 &

    echo -e "${GREEN}✓ Port forward started (socat container)${NC}"
else
    # Linux - use kubectl port-forward
    echo "Linux detected, using kubectl port-forward..."

    # Kill any existing port-forward processes
    pkill -f "kubectl.*port-forward.*registry" || true

    # Start port forward in background
    kubectl port-forward --namespace kube-system service/registry 5000:80 > /dev/null 2>&1 &

    echo -e "${GREEN}✓ Port forward started (kubectl)${NC}"
fi

# Wait for registry to be accessible
echo "Waiting for registry to be accessible at localhost:5000..."
for i in {1..30}; do
    if curl -s http://localhost:5000/v2/ > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Registry accessible at localhost:5000${NC}"
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "${RED}ERROR: Registry not accessible after 30 attempts${NC}"
        exit 1
    fi
    sleep 1
done

# Build the project
echo ""
echo "Building the project..."
cd ../server
mvn clean package -DskipTests
echo -e "${GREEN}✓ Project built successfully${NC}"

# Build and push container image to Minikube registry
REGISTRY="localhost:5000"
echo ""
echo "Building container image..."
$CONTAINER_TOOL build -t ${REGISTRY}/a2a-cloud-deployment:latest .
echo -e "${GREEN}✓ Container image built${NC}"

echo "Pushing image to Minikube registry..."
# Retry push a few times as port-forward can be flaky
MAX_RETRIES=3
for attempt in $(seq 1 $MAX_RETRIES); do
    echo "Push attempt $attempt/$MAX_RETRIES..."
    if $CONTAINER_TOOL push ${REGISTRY}/a2a-cloud-deployment:latest 2>&1 | tee /tmp/push.log; then
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
echo "  http://localhost:8080/.well-known/agent-card.json"
