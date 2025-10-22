#!/bin/bash

echo "============================================"
echo "A2A Cloud Deployment - Cleanup Script"
echo "============================================"
echo ""

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}This will delete all resources in the a2a-demo namespace${NC}"
read -p "Are you sure you want to continue? (y/N) " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Cleanup cancelled"
    exit 0
fi

echo ""
echo "Deleting A2A Agent..."
kubectl delete -f ../k8s/04-agent-deployment.yaml --ignore-not-found=true

echo ""
echo "Deleting ConfigMap..."
kubectl delete -f ../k8s/03-agent-configmap.yaml --ignore-not-found=true

echo ""
echo "Deleting Kafka topic..."
kubectl delete -f ../k8s/02a-kafka-topic.yaml --ignore-not-found=true

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
echo "Stopping registry port forwarding..."
# Kill any port-forward processes
pkill -f "kubectl.*port-forward.*registry" > /dev/null 2>&1 || true

# Determine container tool for stopping socat container on macOS
CONTAINER_TOOL="docker"
if command -v podman &> /dev/null; then
    CONTAINER_TOOL="podman"
fi

# Stop socat container if running (macOS)
$CONTAINER_TOOL stop socat-registry > /dev/null 2>&1 || true
$CONTAINER_TOOL rm socat-registry > /dev/null 2>&1 || true

echo ""
echo -e "${GREEN}Cleanup completed${NC}"
echo ""
echo -e "${YELLOW}Note: Minikube registry addon and Strimzi operator were not removed${NC}"
echo "To disable the registry addon, run:"
echo "  minikube addons disable registry"
echo "To remove Strimzi operator, run:"
echo "  kubectl delete namespace kafka"
