#!/bin/bash

echo "============================================"
echo "A2A Cloud Deployment - Cleanup Script for Minikube"
echo "============================================"
echo ""

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Parse command line arguments
CONTAINER_TOOL="${CONTAINER_TOOL:-docker}"  # Use env var or default to docker
SKIP_CONFIRMATION=false
while [[ $# -gt 0 ]]; do
    case $1 in
        --container-tool)
            CONTAINER_TOOL="$2"
            shift 2
            ;;
        -y|--yes)
            SKIP_CONFIRMATION=true
            shift
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            echo "Usage: $0 [--container-tool docker|podman] [--yes]"
            exit 1
            ;;
    esac
done

# Configure Minikube driver based on container tool
if [ "$CONTAINER_TOOL" = "podman" ]; then
    export MINIKUBE_DRIVER=podman
fi

if [ "$SKIP_CONFIRMATION" != "true" ]; then
    echo -e "${YELLOW}This will delete all resources in the a2a-demo namespace and stop the Minikube cluster${NC}"
    read -p "Are you sure you want to continue? (y/N) " -n 1 -r
    echo ""

    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Cleanup cancelled"
        exit 0
    fi
fi

echo ""
echo "Deleting A2A Agent..."
kubectl delete -f ../k8s/05-agent-deployment.yaml --ignore-not-found=true

echo ""
echo "Deleting ConfigMap..."
kubectl delete -f ../k8s/04-agent-configmap.yaml --ignore-not-found=true

echo ""
echo "Deleting Kafka topic..."
kubectl delete -f ../k8s/03-kafka-topic.yaml --ignore-not-found=true

echo ""
echo "Deleting Kafka..."
kubectl delete -f ../k8s/02-kafka.yaml --ignore-not-found=true

echo ""
echo "Deleting PostgreSQL..."
kubectl delete -f ../k8s/01-postgres.yaml --ignore-not-found=true

echo ""
echo "Deleting namespace..."
kubectl delete -f ../k8s/00-namespace.yaml --ignore-not-found=true

echo ""
echo "Stopping registry port-forward..."
# Stop socat container (macOS)
$CONTAINER_TOOL stop socat-registry 2>/dev/null || true
$CONTAINER_TOOL rm socat-registry 2>/dev/null || true
# Kill kubectl port-forward (Linux)
pkill -f "kubectl.*port-forward.*registry" 2>/dev/null || true

echo ""
echo "Stopping Minikube..."
minikube stop

echo ""
echo -e "${GREEN}Cleanup completed${NC}"
echo ""
echo -e "${YELLOW}Note: Strimzi operator was not removed${NC}"
echo "To remove Strimzi operator, run:"
echo "  kubectl delete namespace kafka"
