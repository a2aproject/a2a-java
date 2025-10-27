#!/bin/bash
set -e

echo "========================================="
echo "A2A Cloud Deployment - Deployment Script for Minikube"
echo "========================================="
echo ""

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Parse command line arguments
CONTAINER_TOOL="${CONTAINER_TOOL:-docker}"  # Use env var or default to docker
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

# Check if Minikube is installed
if ! command -v minikube &> /dev/null; then
    echo -e "${RED}Error: Minikube is not installed${NC}"
    echo "Please install Minikube first: https://minikube.sigs.k8s.io/docs/start/"
    exit 1
fi

# Check if kubectl is installed
if ! command -v kubectl &> /dev/null; then
    echo -e "${RED}Error: kubectl is not installed${NC}"
    echo "Please install kubectl first: https://kubernetes.io/docs/tasks/tools/"
    exit 1
fi

# Start Minikube
echo "Starting Minikube..."

# Configure Minikube driver based on container tool
if [ "$CONTAINER_TOOL" = "podman" ]; then
    export MINIKUBE_DRIVER=podman
    echo "Configured Minikube to use Podman driver"

    # Enable rootless mode for Podman on Linux only (avoids sudo requirement)
    # macOS Podman runs in VM and doesn't support rootless mode
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        echo "Enabling rootless mode for Podman (Linux)..."
        minikube config set rootless true
    fi
fi

if ! minikube status &>/dev/null; then
    if [ "$CONTAINER_TOOL" = "podman" ]; then
        # For Podman on Linux, use containerd runtime for better rootless support
        if [[ "$OSTYPE" == "linux-gnu"* ]]; then
            minikube start --cpus=4 --memory=8192 --driver=podman --container-runtime=containerd
        else
            # macOS with Podman (runs in VM, doesn't need containerd flag)
            minikube start --cpus=4 --memory=8192 --driver=podman
        fi
    else
        minikube start --cpus=4 --memory=8192
    fi
    echo -e "${GREEN}✓ Minikube started${NC}"
else
    echo -e "${GREEN}✓ Minikube is already running${NC}"
fi

# Set up registry for Docker, skip for Podman (we'll use minikube image load)
if [ "$CONTAINER_TOOL" = "docker" ]; then
    # Enable Minikube registry addon if not already enabled
    echo ""
    echo "Checking Minikube registry addon..."
    if ! minikube addons list | grep -q "registry.*enabled"; then
        echo "Enabling Minikube registry addon..."
        REGISTRY_ENABLE_OUTPUT=$(minikube addons enable registry 2>&1)
        echo "$REGISTRY_ENABLE_OUTPUT"
        # Wait a bit for registry to start
        sleep 5
        echo -e "${GREEN}✓ Registry addon enabled${NC}"
    else
        echo -e "${GREEN}✓ Registry addon already enabled${NC}"
    fi

    # Wait for registry service to be created
    echo "Waiting for registry service to be created..."
    for i in {1..30}; do
        if kubectl get svc registry -n kube-system > /dev/null 2>&1; then
            echo "Registry service found"
            break
        fi
        if [ $i -eq 30 ]; then
            echo -e "${RED}ERROR: Registry service not found after 30 seconds${NC}"
            exit 1
        fi
        sleep 1
    done

    # Set up port forwarding to registry
    # This makes the registry accessible at localhost:5000
    echo ""
    echo "Setting up registry port forwarding..."

    # Detect OS
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS - use socat in container for port forwarding
        echo "macOS detected, using socat for port forwarding..."

        # Stop any existing port forwarder
        $CONTAINER_TOOL stop socat-registry 2>/dev/null || true
        $CONTAINER_TOOL rm socat-registry 2>/dev/null || true

        # Start socat container for port forwarding
        # Forward localhost:5000 to minikube-ip:5000 (registry internal port)
        echo "Setting up socat: localhost:5000 -> $(minikube ip):5000"
        $CONTAINER_TOOL run -d --name socat-registry --rm --network=host alpine \
            ash -c "apk add socat && socat TCP-LISTEN:5000,reuseaddr,fork TCP:$(minikube ip):5000" \
            > /dev/null 2>&1

        echo -e "${GREEN}✓ Port forward started (socat container: localhost:5000 -> $(minikube ip):5000)${NC}"
    else
        # Linux (including CI) - use kubectl port-forward
        echo "Linux/CI detected, using kubectl port-forward..."

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
else
    echo ""
    echo "Podman detected - skipping registry setup (will use 'minikube image load' instead)"
fi

# Build the project
echo ""
echo "Building the project..."
cd ../server
mvn clean package -DskipTests
echo -e "${GREEN}✓ Project built successfully${NC}"

# Build and deploy container image
echo ""
if [ "$CONTAINER_TOOL" = "podman" ]; then
    # Podman: Build locally and load into Minikube (no registry needed)
    echo "Building container image for Podman..."
    $CONTAINER_TOOL build -t a2a-cloud-deployment:latest .
    echo -e "${GREEN}✓ Container image built${NC}"

    echo "Saving image to tarball..."
    $CONTAINER_TOOL save a2a-cloud-deployment:latest -o /tmp/a2a-cloud-deployment.tar
    echo -e "${GREEN}✓ Image saved to /tmp/a2a-cloud-deployment.tar${NC}"

    echo "Loading image into Minikube from tarball..."
    minikube image load /tmp/a2a-cloud-deployment.tar
    echo -e "${GREEN}✓ Image loaded into Minikube${NC}"

    # Clean up tarball
    rm -f /tmp/a2a-cloud-deployment.tar
else
    # Docker: Build and push to registry
    REGISTRY="localhost:5000"
    echo "Building container image for Docker..."
    $CONTAINER_TOOL build -t ${REGISTRY}/a2a-cloud-deployment:latest .
    echo -e "${GREEN}✓ Container image built${NC}"

    echo "Pushing image to Minikube registry..."
    # Retry push a few times as port-forward can be flaky
    MAX_RETRIES=3
    for attempt in $(seq 1 $MAX_RETRIES); do
        echo "Push attempt $attempt/$MAX_RETRIES..."
        if $CONTAINER_TOOL push ${REGISTRY}/a2a-cloud-deployment:latest 2>&1 | tee /tmp/push.log; then
            echo -e "${GREEN}✓ Image pushed to registry${NC}"
            rm -f /tmp/push.log
            break
        else
            if [ $attempt -eq $MAX_RETRIES ]; then
                echo -e "${RED}ERROR: Failed to push image after $MAX_RETRIES attempts${NC}"
                cat /tmp/push.log
                rm -f /tmp/push.log
                exit 1
            fi
            echo -e "${YELLOW}Push failed, retrying in 2 seconds...${NC}"
            sleep 2
        fi
    done
fi

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

# On Linux + Podman, the entity-operator has compatibility issues with containerd
# so we check the broker directly instead of the full Kafka resource
if [[ "$OSTYPE" == "linux-gnu"* ]] && [ "$CONTAINER_TOOL" = "podman" ]; then
    echo "Using Linux + Podman - checking broker pod directly (entity-operator may not be ready, this is expected)"

    # Wait for broker pod to be ready
    for i in {1..60}; do
        echo "Checking Kafka broker status (attempt $i/60)..."
        kubectl get pods -n kafka -l strimzi.io/cluster=a2a-kafka 2>/dev/null || true

        # Check specifically for the broker pod (not entity-operator)
        if kubectl wait --for=condition=Ready pod/a2a-kafka-broker-0 -n kafka --timeout=5s 2>/dev/null; then
            echo -e "${GREEN}✓ Kafka broker pod is ready${NC}"
            echo -e "${YELLOW}⚠ Note: Entity operator may not be ready due to Podman+containerd compatibility issues${NC}"
            echo "  This is expected on Linux + Podman and does not affect the demo functionality."
            break
        fi

        if [ $i -eq 60 ]; then
            echo -e "${RED}ERROR: Timeout waiting for Kafka broker${NC}"
            kubectl get pods -n kafka -l strimzi.io/cluster=a2a-kafka
            kubectl describe pod a2a-kafka-broker-0 -n kafka 2>/dev/null || true
            exit 1
        fi

        # Sleep before next attempt
        sleep 5
    done
else
    # Mac + Podman or any Docker: Wait for full Kafka resource to be ready
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

# For Linux + Podman, skip topic readiness check (topic-operator may not be ready)
if [[ "$OSTYPE" == "linux-gnu"* ]] && [ "$CONTAINER_TOOL" = "podman" ]; then
    echo -e "${GREEN}✓ Kafka topic applied${NC}"
    echo -e "${YELLOW}⚠ Topic operator may not be ready - topic will be created when operator starts${NC}"
    echo "  This is expected on Linux + Podman and does not affect the demo functionality."

    # Wait for topic to actually exist in Kafka broker (not just CRD)
    echo "Waiting for topic to be created in Kafka broker. This can take minutes on Linux and Podman, please be patient..."
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
    kubectl wait --for=condition=Ready kafkatopic/a2a-replicated-events -n kafka --timeout=60s
    echo -e "${GREEN}✓ Kafka topic created${NC}"
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

    # Set image reference based on container tool
    if [ "$CONTAINER_TOOL" = "podman" ]; then
        # Podman: Use local image with localhost prefix (Podman adds this automatically)
        IMAGE_REF="localhost/a2a-cloud-deployment:latest"
    else
        # Docker: Use registry image
        IMAGE_REF="localhost:5000/a2a-cloud-deployment:latest"
    fi

    # Apply deployment and patch image reference
    kubectl apply -f ../k8s/05-agent-deployment.yaml
    kubectl set image deployment/a2a-agent a2a-agent=${IMAGE_REF} -n a2a-demo

    # For Podman, set imagePullPolicy to Never (image is loaded locally, not in registry)
    if [ "$CONTAINER_TOOL" = "podman" ]; then
        echo "Setting imagePullPolicy to Never for local Podman image..."
        kubectl patch deployment a2a-agent -n a2a-demo -p '{"spec":{"template":{"spec":{"containers":[{"name":"a2a-agent","imagePullPolicy":"Never"}]}}}}'
    fi

    echo "Waiting for Agent deployment rollout to complete..."
    kubectl rollout status deployment/a2a-agent -n a2a-demo --timeout=120s
    echo -e "${GREEN}✓ Agent deployed with image: ${IMAGE_REF}${NC}"
else
    echo ""
    echo -e "${YELLOW}⚠ Skipping agent deployment (SKIP_AGENT_DEPLOY=true)${NC}"
    echo "  ConfigMap has been deployed, you can manually deploy the agent with:"
    echo "    kubectl apply -f ../k8s/05-agent-deployment.yaml"
fi


echo ""
echo "Getting service URL..."

# Get service URL - on macOS this starts a tunnel in the background
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS: Start tunnel in background and capture URL
    echo "Starting service tunnel in background (required for macOS)..."

    # Start tunnel and capture output to temp file
    TUNNEL_LOG="/tmp/minikube-tunnel-$$.log"
    minikube service a2a-agent-service -n a2a-demo --url > "$TUNNEL_LOG" 2>&1 &
    TUNNEL_PID=$!

    # Wait for URL to appear in log
    echo "Waiting for tunnel to start..."
    for i in {1..30}; do
        if grep -q "http://" "$TUNNEL_LOG" 2>/dev/null; then
            SERVICE_URL=$(grep -o 'http://[^[:space:]]*' "$TUNNEL_LOG" | head -1)
            break
        fi
        sleep 1
    done

    if [ -z "$SERVICE_URL" ]; then
        echo -e "${YELLOW}⚠ Could not detect service URL automatically${NC}"
        echo "Tunnel is running in background (PID: $TUNNEL_PID)"
        echo "Check the URL with: cat $TUNNEL_LOG"
        SERVICE_URL="<check-tunnel-log>"
    else
        echo -e "${GREEN}✓ Tunnel started (PID: $TUNNEL_PID)${NC}"
        echo "  To stop tunnel: kill $TUNNEL_PID"
    fi
else
    # Linux: Also needs tunnel for Podman, direct for Docker
    if [ "$CONTAINER_TOOL" = "podman" ]; then
        echo "Starting service tunnel in background (required for Podman)..."

        # Start tunnel and capture output to temp file
        TUNNEL_LOG="/tmp/minikube-tunnel-$$.log"
        minikube service a2a-agent-service -n a2a-demo --url > "$TUNNEL_LOG" 2>&1 &
        TUNNEL_PID=$!

        # Wait for URL to appear in log
        echo "Waiting for tunnel to start..."
        for i in {1..30}; do
            if grep -q "http://" "$TUNNEL_LOG" 2>/dev/null; then
                SERVICE_URL=$(grep -o 'http://[^[:space:]]*' "$TUNNEL_LOG" | head -1)
                break
            fi
            sleep 1
        done

        if [ -z "$SERVICE_URL" ]; then
            echo -e "${YELLOW}⚠ Could not detect service URL automatically${NC}"
            echo "Tunnel is running in background (PID: $TUNNEL_PID)"
            echo "Check the URL with: cat $TUNNEL_LOG"
            SERVICE_URL="<check-tunnel-log>"
        else
            echo -e "${GREEN}✓ Tunnel started (PID: $TUNNEL_PID)${NC}"
            echo "  To stop tunnel: kill $TUNNEL_PID"
        fi
    else
        # Linux with Docker: Direct service URL (should work without blocking)
        SERVICE_URL=$(minikube service a2a-agent-service -n a2a-demo --url)
    fi
fi

echo ""
echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}Deployment to Minikube completed successfully!${NC}"
echo -e "${GREEN}=========================================${NC}"
echo ""
echo -e "${GREEN}Agent is accessible at: ${SERVICE_URL}${NC}"
echo ""

echo "To run the test client (demonstrating load balancing):"
echo "  cd ../server"
echo "  mvn test-compile exec:java -Dexec.classpathScope=test \\"
echo "    -Dexec.mainClass=\"io.a2a.examples.cloud.A2ACloudExampleClient\" \\"
echo "    -Dagent.url=\"${SERVICE_URL}\""
