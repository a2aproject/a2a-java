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

echo -e "${YELLOW}This will delete all resources in the a2a-demo namespace and the Kind cluster${NC}"
read -p "Are you sure you want to continue? (y/N) " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Cleanup cancelled"
    exit 0
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
echo "Deleting Kind cluster..."
kind delete cluster

echo ""
echo "Stopping and removing registry container..."
$CONTAINER_TOOL stop kind-registry > /dev/null 2>&1 || true
$CONTAINER_TOOL rm kind-registry > /dev/null 2>&1 || true

echo ""
echo -e "${GREEN}Cleanup completed${NC}"
echo ""
echo -e "${YELLOW}Note: Strimzi operator was not removed${NC}"
echo "To remove Strimzi operator, run:"
echo "  kubectl delete namespace kafka"
